package com.quittle.setupandroidsdk;

/**
 * Provides configuration for {@link SetupAndroidSdkPlugin}.
 */
public class SetupAndroidSdkExtension {
    /** The default version of Android SDK Tools to download and use. */
    public static final String DEFAULT_SDK_TOOLS_VERSION = "4333796";

    /**
     * The version of Android SDK Tools to download and use. Defaults to
     * {@link #DEFAULT_SDK_TOOLS_VERSION}.
     */
    private String sdkToolsVersion = DEFAULT_SDK_TOOLS_VERSION;

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
}