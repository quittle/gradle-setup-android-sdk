package com.quittle.setupandroidsdk;

import com.android.build.gradle.BaseExtension;
import com.android.repository.Revision;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectState;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskInstantiationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.quittle.setupandroidsdk.Utils.getConstantViaReflection;

/**
 * Automatically installs the Android SDK. Apply after the Android Gradle plugin.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SetupAndroidSdkPlugin implements Plugin<Project> {
    private static final String SDK_TOOLS_URL_FORMAT =
            "https://dl.google.com/android/repository/commandlinetools-%s-%s.zip";

    @Override
    public void apply(final Project project) {
        final Project rootProject = project.getRootProject();
        final Logger logger = rootProject.getLogger();
        final File sdkDir = new File(rootProject.getBuildDir(), "android-sdk-root");
        final File sdkToolsVersionFile = new File(sdkDir, "sdkToolsVersion.txt");
        final File sdkManager = getSdkManager(sdkDir);
        final File localProperties = rootProject.file("local.properties");

        final SetupAndroidSdkExtension extension =
                project.getExtensions().create("setupAndroidSdk", SetupAndroidSdkExtension.class);


        createCleanTask(rootProject, localProperties);
        if (!localProperties.exists()) {
            createLocalProperties(sdkDir, localProperties);
        }

        // afterEvaluate required for consumer to configure extension
        project.afterEvaluate(p -> {
            setupLicences(logger, extension.getLicensesDirectory(), sdkDir);
            installSdkManager(logger, sdkToolsVersionFile, extension.getSdkToolsVersion(), sdkDir, sdkManager);
        });

        final Action<Project> installPackagesForProject = p -> {
            final boolean shouldAutoAcceptLicenses = extension.getLicensesDirectory() == null;
            final Set<String> packages = new HashSet<>();
            packages.addAll(getDefaultPackagesToInstall(p));
            packages.addAll(extension.getPackages());
            installSdk(logger, sdkDir, sdkManager, packages, shouldAutoAcceptLicenses);
        };

        project.allprojects(p -> {
            final ProjectState state = p.getState();
            // If applied to a root project that uses evaluationDependsOnChildren, afterEvaluate actions won't be
            // triggered so checking the current state of that project is required and must be scheduled for after the
            // current project is evaluated to ensure the sdkmanager installation is completed first.
            if (state.getExecuted()) {
                project.afterEvaluate(installPackagesForProject);
            } else {
                p.afterEvaluate(installPackagesForProject);
            }
        });
    }

    /**
     * Gets the location of the sdkmanager executable
     * @param sdkDir The SDK directory to find the sdkmanager in
     * @return The OS-dependent location of the sdkmanager tool
     */
    private static File getSdkManager(final File sdkDir) {
        if (Os.isFamily(Os.FAMILY_UNIX) || Os.isFamily(Os.FAMILY_MAC)) {
            return new File(sdkDir, "tools/bin/sdkmanager");
        } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return new File(sdkDir, "tools/bin/sdkmanager.bat");
        } else {
            throw new TaskInstantiationException("Unsupported OS. File a bug report to get it added");
        }
    }

    private static void setupLicences(
            final Logger logger, final File licenseDirectory, final File sdkDir) {
        if (licenseDirectory == null) {
            logger.debug("No license directory specified, accepting all licenses automatically");
            return;
        }
        final File sdkLicensesDirectory = new File(sdkDir, "licenses");
        try {
            FileUtils.deleteDirectory(sdkLicensesDirectory);
            FileUtils.copyDirectory(licenseDirectory, sdkLicensesDirectory);
        } catch (final IOException e) {
            throw new TaskInstantiationException("Unable to synchronize licenses directory", e);
        }
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

    @SuppressFBWarnings(
            value = "OBL_UNSATISFIED_OBLIGATION",
            justification = "https://github.com/spotbugs/spotbugs/issues/432")
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
            platform = "win";
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            platform = "mac";
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

    private static void installSdk(final Logger logger,
                                   final File sdkRoot,
                                   final File sdkManager,
                                   final Collection<String> packages,
                                   final boolean shouldAutoAcceptLicenses) {
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
            if (shouldAutoAcceptLicenses) {
                try (final OutputStream os = process.getOutputStream()) {
                    os.write('y');
                }
            } else {
                process.getOutputStream().close();
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
    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "Older versions of the plugin don't guarantee getBuildToolsVersion won't return null.")
    private static String getBuildToolsVersion(final BaseExtension android) {
        final String explicitVersion = android.getBuildToolsVersion();
        final Revision minBuildToolsVersion = getMinBuildToolsRev();
        final Revision determinedVersion;
        if (explicitVersion == null) {
            determinedVersion = minBuildToolsVersion;
        } else {
            final Revision explicitFullRevision = Revision.parseRevision(explicitVersion);
            if (explicitFullRevision.compareTo(minBuildToolsVersion) < 0) {
                determinedVersion = minBuildToolsVersion;
            } else {
                determinedVersion = explicitFullRevision;
            }
        }
        return determinedVersion.toString().replace(' ', '-');
    }

    /**
     * Determines the minimum build tools version from the plugin
     * @return The minimum version.
     * @throws TaskInstantiationException if unable to determine the version
     */
    private static Revision getMinBuildToolsRev() {
        return Stream.of(
                    getConstantViaReflection("com.android.builder.core.AndroidBuilder", "MIN_BUILD_TOOLS_REV", Revision.class),
                    getConstantViaReflection("com.android.builder.core.ToolsRevisionUtils", "MIN_BUILD_TOOLS_REV", Revision.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new TaskInstantiationException("Unable to determine min build tools version"));
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
