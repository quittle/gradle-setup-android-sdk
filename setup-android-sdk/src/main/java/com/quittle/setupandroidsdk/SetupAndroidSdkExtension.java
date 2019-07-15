package com.quittle.setupandroidsdk;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides configuration for {@link SetupAndroidSdkPlugin}.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class SetupAndroidSdkExtension {
    /**
     * The default version of Android SDK Tools to download and use.
     * Currently is {@code Revision 26.1.1 (September 2017)}
     */
    public static final String DEFAULT_SDK_TOOLS_VERSION = "4333796";

    /**
     * The version of Android SDK Tools to download and use. Defaults to
     * {@link #DEFAULT_SDK_TOOLS_VERSION}.
     */
    private String sdkToolsVersion = DEFAULT_SDK_TOOLS_VERSION;

    /**
     * The Android SDK Manager packages to install.
     */
    private final Set<String> packages = new HashSet<>();

    /**
     * Gets the SDK Tools version.
     * @return The currently set SDK Tools version.
     */
    public String getSdkToolsVersion() {
        return sdkToolsVersion;
    }

    /**
     * Sets the SDK Tools version.
     * @param sdkToolsVersion The SDK Tools version to set.
     */
    public void setSdkToolsVersion(final String sdkToolsVersion) {
        this.sdkToolsVersion = sdkToolsVersion;
    }

    /**
     * Sets the SDK Tools version.
     * @param sdkToolsVersion The SDK Tools version to set.
     */
    public void sdkToolsVersion(final String sdkToolsVersion) {
        setSdkToolsVersion(sdkToolsVersion);
    }

    /**
     * Adds all the packages to the collection to install via the plugin
     * @param packages The package names used by the Android SDK Manager. e.g.
     *                 {@code add-ons;addon-google_apis-google-4} or
     *                 {@code system-images;android-25;google_apis;x86}.
     */
    public void packages(final Collection<String> packages) {
        this.packages.addAll(packages);
    }

    /**
     * Adds all the packages to the collection to install via the plugin
     * @param packages The package names used by the Android SDK Manager. e.g.
     *                 {@code add-ons;addon-google_apis-google-4} or
     *                 {@code system-images;android-25;google_apis;x86}.
     */
    public void packages(final String... packages) {
        Collections.addAll(this.packages, packages);
    }

    /**
     * Gets the packages to be installed
     * @return A collection of Android SDK Manager packages to be installed by the plugin.
     */
    public Collection<String> getPackages() {
        return new HashSet<>(this.packages);
    }
}
