package com.quittle.setupandroidsdk;

import com.android.build.gradle.BaseExtension;
import com.android.builder.core.AndroidBuilder;
import com.android.repository.Revision;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.gradle.api.AntBuilder;
import org.gradle.api.logging.Logger;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.WriteProperties;
import org.gradle.api.tasks.TaskInstantiationException;

/**
 * Automatically installs the Android SDK. Apply after the Android Gradle plugin.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SetupAndroidSdkPlugin implements Plugin<Project> {
    private static final String SDK_TOOLS_URL_FORMAT =
            "https://dl.google.com/android/repository/sdk-tools-%s-%s.zip";
    @Override
    public void apply(final Project project) {
        final Project rootProject = project.getRootProject();
        final Logger logger = rootProject.getLogger();
        final File sdkDir = new File(rootProject.getBuildDir(), "android-sdk-root");
        final File sdkToolsVersionFile = new File(sdkDir, "sdkToolsVersion.txt");
        final File sdkManager = new File(sdkDir, "tools/bin/sdkmanager");
        final File localProperties = rootProject.file("local.properties");

        final SetupAndroidSdkExtension extension =
                project.getExtensions().create("setupAndroidSdk", SetupAndroidSdkExtension.class);


        createCleanTask(rootProject, localProperties);
        if (!localProperties.exists()) {
            createLocalProperties(sdkDir, localProperties);
        }

        rootProject.afterEvaluate(p -> {
            installSdkManager(logger, sdkToolsVersionFile, extension.getSdkToolsVersion(), sdkDir, sdkManager);
        });

        project.allprojects(p -> {
            p.afterEvaluate(pp -> {
                final Set<String> packages = new HashSet<>();
                packages.addAll(getDefaultPackagesToInstall(pp));
                packages.addAll(extension.getPackages());
                installSdk(logger, sdkDir, sdkManager, packages);
            });
        });
    }

    /**
     * Installs {@code sdkmanager}. There is a file storing what the version downloaded was to
     * avoid downloading every time and detecting version changes.
     */
    private static void installSdkManager(final Logger logger, final File sdkToolsVersionFile,
            final String desiredSdkToolsVersion, final File sdkDir, final File sdkManager) {
        final String currentSdkToolsVersion = getCurrentSdkToolsVersion(logger, sdkToolsVersionFile);
        if (!sdkManager.exists() || !Objects.equals(desiredSdkToolsVersion, currentSdkToolsVersion)) {
            downloadSdkTools(logger, sdkDir, desiredSdkToolsVersion);
            sdkManager.setExecutable(true);
            try {
                FileUtils.writeStringToFile(sdkToolsVersionFile, desiredSdkToolsVersion, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                throw new TaskInstantiationException("Unable to save sdk tools version", e);
            }
        }
    }

    private static String getCurrentSdkToolsVersion(final Logger logger, final File sdkToolsVersionFile) {
        if (!sdkToolsVersionFile.exists()) {
            return null;
        } else {
            try {
                return FileUtils.readFileToString(sdkToolsVersionFile, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                logger.error(
                        "Unable to read sdk tools version file: " + sdkToolsVersionFile.getAbsolutePath());
                throw new TaskInstantiationException("Unable to read sdk tools version file", e);
            }
        }
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    @SuppressWarnings("PMD.AssignmentInOperand")
    private static void downloadSdkTools(final Logger logger, final File sdkRoot, final String sdkToolsVersion) {
        byte[] memoryRepresentation = null;
        try (final InputStream is = new URL(getSdkToolsUrl(sdkToolsVersion)).openStream();
                final ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final String fileName = entry.getName();
                logger.debug("Downloading " + fileName);
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

        logger.info("Done downloading sdkmanager");
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

    private static void createLocalProperties(final File sdkDir, final File localProperties) {
        final Properties properties = new Properties();
        properties.setProperty("sdk.dir", sdkDir.getAbsolutePath());
        try (final OutputStream os = new FileOutputStream(localProperties)) {
            properties.store(os, null);
        } catch (final IOException e) {
            throw new TaskInstantiationException("Unable to create local.properties", e);
        }
    }

    private static String getSdkToolsUrl(final String sdkToolsVersion) {
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

        return String.format(SDK_TOOLS_URL_FORMAT, platform, sdkToolsVersion);
    }

    private static Collection<String> getDefaultPackagesToInstall(final Project project) {
        final BaseExtension android = project.getExtensions().findByType(BaseExtension.class);
        if (android == null) {
            project.getLogger().debug("Unable to find android extension for project " + project.getName() + ". Skipping...");
            return Collections.emptyList();
        }

        final String compileSdkVersion = android.getCompileSdkVersion();
        if (compileSdkVersion == null) {
            throw new TaskInstantiationException("Android compile sdk version not set");
        }

        final String buildToolsVersion = getBuildToolsVersion(android);

        return Arrays.asList("platforms;" + compileSdkVersion, "build-tools;" + buildToolsVersion);
    }

    private static void installSdk(final Logger logger, final File sdkRoot, final File sdkManager, final Collection<String> packages) {
        final List<String> command = new ArrayList<>();
        command.add(sdkManager.getAbsolutePath());
        command.add("--sdk_root=" + sdkRoot.getAbsolutePath());
        command.addAll(packages);
        logger.debug("Installing SDK with command: " + command);
        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        final int exitCode;
        try {
            final Process process = pb.start();
            try (final OutputStream os = process.getOutputStream()) {
                os.write('y');
            }
            write(process.getInputStream(), logger::debug);
            exitCode = process.waitFor();
        } catch (final IOException | InterruptedException e) {
            throw new TaskInstantiationException("sdkmanager failed to run successfully", e);
        }

        if (exitCode != 0) {
            throw new TaskInstantiationException("Unable to run sdkmanager successfully. Exit code: " + exitCode);
        }
    }

    /**
     * If the android extension does not specify a build tools version explicitly, then a default version as chosen by
     * the plugin is used. If the build tools version added is lower than this default version, it considered
     * unsupported and rather than failing the build, the plugin helpfully ignores what was specified, logs a warning,
     * and instead uses the min version hardcoded in the plugin.
     */
    private static String getBuildToolsVersion(final BaseExtension android) {
        final String explicitVersion = android.getBuildToolsVersion();
        final Revision determinedVersion;
        if (explicitVersion == null) {
            determinedVersion = AndroidBuilder.MIN_BUILD_TOOLS_REV;
        } else {
            final Revision explicitFullRevision = Revision.parseRevision(explicitVersion);
            if (explicitFullRevision.compareTo(AndroidBuilder.MIN_BUILD_TOOLS_REV) < 0) {
                determinedVersion = AndroidBuilder.MIN_BUILD_TOOLS_REV;
            } else {
                determinedVersion = explicitFullRevision;
            }
        }
        return determinedVersion.toString().replace(' ', '-');
    }

    /**
     * Reads {@link is} as a stream of text and passes each line to the {@link consumer}.
     * @param is The stream to read from
     * @param consumer Called with each line from the input stream as it is read.
     */
    private static void write(final InputStream is, final Consumer<String> consumer) throws IOException {
        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            bufferedReader.lines().forEach(consumer);
        }
    }
}
