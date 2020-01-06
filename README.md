# Android SDK Installer [![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/quittle/setup-android-sdk/maven-metadata.xml.svg?label=Gradle+Plugin)](https://plugins.gradle.org/plugin/com.quittle.setup-android-sdk) [![Build Status](https://travis-ci.com/quittle/gradle-setup-android-sdk.svg?branch=master)](https://travis-ci.com/quittle/gradle-setup-android-sdk)

This plugin automatically installs the Android SDK and configures Gradle to consume it. This plugin
will automatically accept all Android SDK licenses when installing them. Before using the plugin or
upgrading Android SDK versions, make sure you are okay accepting the licenses for those versions.

## Consumption

The minimum requirement for consumption is to simply
[apply this plugin](https://plugins.gradle.org/plugin/com.quittle.setup-android-sdk)

### Multi-Project
If you have a multi-project setup, e.g. you have two `build.gradle`s, one at `/build.gradle` and one at `/app/build.gradle`,
follow this setup, modifying only the root `build.gradle`.

#### build.gradle
```groovy
buildscript {
    repositories {
        // Make sure to have Google as a buildscript dependency for the plugin
        google()
    }
    
    dependencies {
        // Keep whatever build tools you have like the
        // Android Gradle plugin (com.android.tools.build:gradle)
    }
}

plugins {
    // Apply the plugin
    id 'com.quittle.setup-android-sdk' version '1.3.1'
}

// The rest of the file can remain as it is
allprojects {
    repositories {
        // ...
    }
}

// Optional configuration
setupAndroidSdk {
    // This is the suffix found in the downloads for command line tool zips.
    // See https://developer.android.com/studio/#command-tools for the latest version available.
    // If not specified, defaults to the version baked into the plugin.
    sdkToolsVersion '4333796'

    // You can add additional packages to install like this
    packages 'ndk-bundle', 'emulator', 'system-images;android-28;default;x86'
}
```

### Single-Project Setup

If you only have a single-project setup, i.e. one `build.gradle` in the root of you project, follow this setup. Make sure
to apply the plugin *before* the `android` one.

#### build.gradle
```groovy
// Consume from Gradle plugin respository. This is the only required step.
plugins {
    id 'com.quittle.setup-android-sdk' version '1.3.1'
}

// Consume android plugin as usual.
apply plugin: 'android'

android {
    // Fill out normally
}

// Optional configuration
setupAndroidSdk {
    // This is the suffix found in the downloads for command line tool zips.
    // See https://developer.android.com/studio/#command-tools for the latest version available.
    // If not specified, defaults to the version baked into the plugin.
    sdkToolsVersion '4333796'

    // You can add additional packages to install like this
    packages 'ndk-bundle', 'emulator', 'system-images;android-28;default;x86'
}
```

## Development

In general, perform builds in the context of each folder, rather than as a multi-project Gradle
build. This is necessary because the `example-android-project` will fail to configure without the
plugin being locally available so the `android-sdk-installer` project must be built and deployed
locally first.

In general, to build and test locally.
```
$ ./gradlew -p setup-android-sdk # This runs all the default tasks
$ ./gradlew -p setup-android-sdk publishToMavenLocal # Publishes it for the example-android-project to consume
$ ./validate_plugin # Integration test to validate the plugin works
```

## Deployment
This package is deployed via [Travis CI](https://travis-ci.com/quittle/gradle-setup-android-sdk).
See `.travis.yml` for the CI/CD setup.

In the configuration for the build on Travis, `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` are
injected as secret environment variables.

Upon check-in to the `master` branch, Travis checks out, builds, and deploys the plugin.
