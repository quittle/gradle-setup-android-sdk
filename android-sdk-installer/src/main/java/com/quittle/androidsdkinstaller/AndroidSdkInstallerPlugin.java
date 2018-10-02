package com.quittle.androidsdkinstaller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.gradle.api.AntBuilder;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.WriteProperties;
import com.android.build.gradle.BaseExtension;
import org.gradle.api.tasks.TaskInstantiationException;

/**
 * Automatically installs the Android SDK. Apply after the Android Gradle plugin.
 */
public class AndroidSdkInstallerPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        final Project rootProject = project.getRootProject();
        final File sdkDir = new File(rootProject.getBuildDir(), "android-sdk-root");
        final File sdkManager = new File(sdkDir, "tools/bin/sdkmanager");
        final File localProperties = rootProject.file("local.properties");

        if (!sdkManager.exists()) {
            downloadSdkTools(rootProject, sdkDir);
            sdkManager.setExecutable(true);
        }

        createCleanTask(rootProject, localProperties);
        if (!localProperties.exists()) {
            createLocalProperties(rootProject, sdkDir, localProperties);
        }

        rootProject.afterEvaluate(p -> {
            installSdk(p, sdkDir, sdkManager);
        });
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static void downloadSdkTools(final Project project, final File sdkRoot) {
        byte[] memoryRepresentation = null;
        try (final InputStream is = new URL(getSdkToolsUrl()).openStream();
                final ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final String fileName = entry.getName();
                project.getLogger().debug("Downloading " + fileName);
                final File curFile = new File(sdkRoot, fileName);
                if (entry.isDirectory()) {
                    curFile.mkdirs();
                } else {
                    curFile.getParentFile().mkdirs();
                    try (final OutputStream os = new FileOutputStream(curFile)) {
                        IOUtils.copy(zis, os);
                    }
                }
            }
        } catch (final IOException e) {
            throw new TaskInstantiationException("Unable read Android SDK tools zip", e);
        }

        project.getLogger().info("Done downloading sdkmanager");
    }

    private static void createCleanTask(final Project project, final File localProperties) {
        final Task deleteTask = project.getTasks().create("cleanLocalProperties", Delete.class, task -> {
            task.delete(localProperties);
        });

        project.afterEvaluate(p -> {
            final Task cleanTask = p.getTasks().findByName("clean");
            if (cleanTask != null) {
                cleanTask.dependsOn(deleteTask);
            } else {
                p.getLogger().debug("Unable to find clean task");
            }
        });
    }

    private static void createLocalProperties(final Project project, final File sdkDir, final File localProperties) {
        final Properties properties = new Properties();
        properties.setProperty("sdk.dir", sdkDir.getAbsolutePath());
        try (final OutputStream os = new FileOutputStream(localProperties)) {
            properties.store(os, null);
        } catch (final IOException e) {
            throw new TaskInstantiationException("Unable to create local.properties", e);
        }
    }

    private static String getSdkToolsUrl() {
        final String sdkToolsUrl = "https://dl.google.com/android/repository/sdk-tools-%s-%s.zip";
        final String version = "3859397";
        final String platform;

        if (Os.isFamily(Os.FAMILY_UNIX)) {
            platform = "linux";
        } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            platform = "windows";
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            platform = "darwin";
        } else {
            throw new TaskInstantiationException("Unsupported OS. File a bug report to get it added");
        }

        return String.format(sdkToolsUrl, platform, version);
    }

    private static void installSdk(final Project project, final File sdkRoot, final File sdkManager) {
        final BaseExtension android = project.getExtensions().findByType(BaseExtension.class);
        if (android == null) {
            throw new TaskInstantiationException("Unable to find android extension.");
        }

        final String compileSdkVersion = android.getCompileSdkVersion();
        if (compileSdkVersion == null) {
            throw new TaskInstantiationException("Android compile sdk version not set");
        }

        final String buildToolsVersion = android.getBuildToolsVersion();
        if (buildToolsVersion == null) {
            throw new TaskInstantiationException("Android build tools version not set");
        }

        final ProcessBuilder pb = new ProcessBuilder(sdkManager.getAbsolutePath(),
                "--sdk_root=" + sdkRoot.getAbsolutePath(),
                "platforms;" + compileSdkVersion,
                "build-tools;" + buildToolsVersion);
        pb.redirectErrorStream(true);
        final int exitCode;
        try {
            final Process process = pb.start();
            try (final OutputStream os = process.getOutputStream()) {
                os.write('y');
            }
            write(process.getInputStream(), project.getLogger()::debug);
            exitCode = process.waitFor();
        } catch (final IOException | InterruptedException e) {
            throw new TaskInstantiationException("sdkmanager failed to run successfully", e);
        }

        if (exitCode != 0 ) {
            throw new TaskInstantiationException("Unable to run sdkmanager successfully. Exit code:" + exitCode);
        }
    }

    private static void write(final InputStream is, final Consumer<String> consumer) throws IOException {
        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            bufferedReader.lines().forEach(consumer);
        }
    }
}