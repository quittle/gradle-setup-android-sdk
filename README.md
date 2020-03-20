# Android SDK Installer [![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/quittle/setup-android-sdk/maven-metadata.xml.svg?label=Gradle+Plugin)](https://plugins.gradle.org/plugin/com.quittle.setup-android-sdk) [![Build Status](https://travis-ci.com/quittle/gradle-setup-android-sdk.svg?branch=master)](https://travis-ci.com/quittle/gradle-setup-android-sdk)

This plugin automatically installs the Android SDK and configures Gradle to consume it. This plugin
also automatically accepts all Android SDK licenses by default when downloading SDK packages. *Before
using the plugin or upgrading Android SDK versions, make sure you are okay accepting the licenses
for those versions or explicitly set `setupAndroidSdk.licensesDirectory` if not.*

## Consumption

The minimum requirement for consumption is to simply
[apply this plugin, `com.quittle.setup-android-sdk`](https://plugins.gradle.org/plugin/com.quittle.setup-android-sdk).
The latest version is listed above in the "Gradle Plugin" badge.

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
    id 'com.quittle.setup-android-sdk' version 'x.x.x'
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

    // Optional field to specify the licenses you previously accepted. If not set, all licenses are
    // automatically accepted.
    // See https://developer.android.com/studio/intro/update.html#download-with-gradle for more info
    // on backing up license agreements.
    licensesDirectory file('path/to/licenses')

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
    id 'com.quittle.setup-android-sdk' version 'x.x.x'
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

    // Optional field to specify the licenses you previously accepted. If not set, all licenses are
    // automatically accepted.
    // See https://developer.android.com/studio/intro/update.html#download-with-gradle for more info
    // on backing up license agreements.
    licensesDirectory file('path/to/licenses')

    // You can add additional packages to install like this
    packages 'ndk-bundle', 'emulator', 'system-images;android-28;default;x86'
}
```

### Note

This plugin performs the SDK installation as part of Gradle's configuration phase instead of being
a task because the Android plugin does verification of the SDK when it is applied. It checks if the
required version of the SDK was already installed, however, so it won't do any unnecessary
networking. This means post installation, the build should work fine without network access.

A way to ensure the licenses you accept remain stable is to do do a build once without specifying
`licensesDirectory` and copying the contents of `build/android-sdk-root/licenses` to a folder in
your version controlled directory and referencing that directory with `licensesDirectory` in your
`build.gradle`. If you check in this license directory and configure this plugin to use it, your
build will only succeed if no new licenses needed to be accepted when downloading SDKs. This process
is [recommended by Google](https://developer.android.com/studio/intro/update.html#download-with-gradle)
to prevent ensure you are always fully aware of what you are agreeing to.

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

Upon check-in to the `master` branch, Travis checks out, builds, and deploys the plugin. Version
numbers are determined by tag names.
