package com.microsoft.hydralab.common.appcenter.entity;

import lombok.Data;

@Data
public class Device {
    /**
     * Name of the SDK.
     */
    private String sdkName;

    /**
     * Version of the SDK.
     */
    private String sdkVersion;

    /**
     * Device model (example: iPad2,3).
     */
    private String model;

    /**
     * Device manufacturer (example: HTC).
     */
    private String oemName;

    /**
     * OS name (example: iOS).
     */
    private String osName;

    /**
     * OS version (example: 9.3.0).
     */
    private String osVersion;

    /**
     * OS build code (example: LMY47X).
     */
    private String osBuild;

    /**
     * API level when applicable like in Android (example: 15).
     */
    private Integer osApiLevel;

    /**
     * Language code (example: en_US).
     */
    private String locale;

    /**
     * The offset in minutes from UTC for the device time zone, including
     * daylight savings time.
     */
    private Integer timeZoneOffset;

    /**
     * Screen size of the device in pixels (example: 640x480).
     */
    private String screenSize;

    /**
     * Application version name.
     */
    private String appVersion;

    /**
     * Carrier name (for mobile devices).
     */
    private String carrierName;

    /**
     * Carrier country code (for mobile devices).
     */
    private String carrierCountry;

    /**
     * The app's build number, e.g. 42.
     */
    private String appBuild;

    /**
     * The bundle identifier, package identifier, or namespace, depending on
     * what the individual platforms use,  .e.g com.microsoft.example.
     */
    private String appNamespace;
}
