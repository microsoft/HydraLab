// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab

import com.microsoft.hydralab.config.DeviceConfig
import com.microsoft.hydralab.config.HydraLabAPIConfig
import com.microsoft.hydralab.config.TestConfig
import com.microsoft.hydralab.utils.CommonUtils
import com.microsoft.hydralab.utils.HydraLabClientUtils
import com.microsoft.hydralab.utils.YamlParser
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project


class ClientUtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.task("requestHydraLabTest") {
            doFirst {
                HydraLabAPIConfig apiConfig = new HydraLabAPIConfig()
                TestConfig testConfig = new TestConfig()
                DeviceConfig deviceConfig = new DeviceConfig()
                def instrumentationArgsMap = null
                def reportDir = new File(project.buildDir, "testResult")
                if (!reportDir.exists()) {
                    reportDir.mkdirs()
                }

                // read config from yml
                if (project.hasProperty('ymlConfigFile')) {
                    YamlParser yamlParser = new YamlParser(project.ymlConfigFile)
                    apiConfig = yamlParser.parseAPIConfig()
                    testConfig = yamlParser.parseTestConfig()
                    deviceConfig = yamlParser.parseDeviceConfig()
                    instrumentationArgsMap = CommonUtils.parseArguments(yamlParser.getString("instrumentationArgs"))
                }

                if (project.hasProperty('appPath')) {
                    testConfig.appPath = project.appPath
                }
                if (project.hasProperty('testAppPath')) {
                    testConfig.testAppPath = project.testAppPath
                }
                if (project.hasProperty('attachmentConfigPath')) {
                    testConfig.attachmentConfigPath = project.attachmentConfigPath
                }
                // validate file path
                testConfig.appPath = CommonUtils.validateFile(testConfig.appPath, "appPath")
                testConfig.testAppPath = CommonUtils.validateFile(testConfig.testAppPath, "testAppPath")
                testConfig.attachmentConfigPath = CommonUtils.validateFile(testConfig.attachmentConfigPath, "attachmentConfigPath")

                if (project.hasProperty('instrumentationArgs')) {
                    instrumentationArgsMap = CommonUtils.parseArguments(project.instrumentationArgs)
                }

                if (project.hasProperty('hydraLabAPISchema')) {
                    apiConfig.schema = project.hydraLabAPISchema
                }
                if (project.hasProperty('hydraLabAPIHost')) {
                    apiConfig.host = project.hydraLabAPIHost
                }
                if (project.hasProperty('authToken')) {
                    apiConfig.authToken = project.authToken
                }

                if (project.hasProperty('deviceIdentifier')) {
                    deviceConfig.deviceIdentifier = project.deviceIdentifier
                }
                if (project.hasProperty('groupTestType')) {
                    deviceConfig.groupTestType = project.groupTestType
                }
                if (project.hasProperty('neededPermissions')) {
                    deviceConfig.neededPermissions = project.neededPermissions.split(", +")
                }
                if (project.hasProperty('deviceActions')) {
                    // add quotes back as quotes in gradle plugins will be replaced by blanks
                    deviceConfig.deviceActionsStr = project.deviceActions.replace("\\", "\"")
                }

                if (project.hasProperty('type')) {
                    testConfig.type = project.type
                }
                if (project.hasProperty('runningType')) {
                    testConfig.runningType = project.runningType
                }
                if (project.hasProperty('pkgName')) {
                    testConfig.pkgName = project.pkgName
                }
                if (project.hasProperty('testPkgName')) {
                    testConfig.testPkgName = project.testPkgName
                }
                if (project.hasProperty('teamName')) {
                    testConfig.teamName = project.teamName
                }
                if (project.hasProperty('testRunnerName')) {
                    testConfig.testRunnerName = project.testRunnerName
                }
                if (project.hasProperty('testScope')) {
                    testConfig.testScope = project.testScope
                }
                if (project.hasProperty('testSuiteName')) {
                    testConfig.testSuiteName = project.testSuiteName
                }
                if (project.hasProperty('frameworkType')) {
                    testConfig.frameworkType = project.frameworkType
                }
                if (project.hasProperty('runTimeOutSeconds')) {
                    testConfig.runTimeOutSeconds = Integer.parseInt(project.runTimeOutSeconds)
                }
                if (project.hasProperty('queueTimeOutSeconds')) {
                    testConfig.queueTimeOutSeconds = Integer.parseInt(project.queueTimeOutSeconds)
                } else {
                    if (!project.hasProperty('ymlConfigFile')) {
                        testConfig.queueTimeOutSeconds = testConfig.runTimeOutSeconds
                    }
                }
                if (project.hasProperty('maxStepCount')) {
                    testConfig.maxStepCount = Integer.parseInt(project.maxStepCount)
                }
                if (project.hasProperty('deviceTestCount')) {
                    testConfig.deviceTestCount = Integer.parseInt(project.deviceTestCount)
                }
                if (project.hasProperty('needUninstall')) {
                    testConfig.needUninstall = Boolean.parseBoolean(project.needUninstall)
                }
                if (project.hasProperty('needClearData')) {
                    testConfig.needClearData = Boolean.parseBoolean(project.needClearData)
                }
                if (project.hasProperty('tag')) {
                    testConfig.artifactTag = project.tag
                }

                requiredParamCheck(apiConfig, deviceConfig, testConfig)

                HydraLabClientUtils.runTestOnDeviceWithApp(
                        reportDir.absolutePath, instrumentationArgsMap,
                        apiConfig, deviceConfig, testConfig
                )
            }
        }.configure {
            group = "Test"
            description = "Run mobile/cross-platform test with specified params on Hydra Lab - see more in https://github.com/microsoft/HydraLab/wiki"
        }
    }

    void requiredParamCheck(HydraLabAPIConfig apiConfig, DeviceConfig deviceConfig, TestConfig testConfig) {
        if (StringUtils.isBlank(apiConfig.host)
                || StringUtils.isBlank(apiConfig.authToken)
                || StringUtils.isBlank(testConfig.appPath)
                || StringUtils.isBlank(testConfig.pkgName)
                || StringUtils.isBlank(testConfig.runningType)
                || testConfig.runTimeOutSeconds == 0
                || StringUtils.isBlank(deviceConfig.deviceIdentifier)
        ) {
            throw new IllegalArgumentException('Required params not provided! Make sure the following params are all provided correctly: hydraLabAPIhost, authToken, deviceIdentifier, appPath, pkgName, runningType, runTimeOutSeconds.')
        }

        // running type specified params
        switch (testConfig.runningType) {
            case "INSTRUMENTATION":
                if (StringUtils.isBlank(testConfig.testAppPath)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testAppPath not provided!')
                }
                if (StringUtils.isBlank(testConfig.testPkgName)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testPkgName not provided!')
                }
                if (testConfig.testScope != TestScope.PACKAGE && testConfig.testScope != TestScope.CLASS) {
                    break
                }
                if (StringUtils.isBlank(testConfig.testSuiteName)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testSuiteName not provided!')
                }
                break
            case "APPIUM":
                if (StringUtils.isBlank(testConfig.testAppPath)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testAppPath not provided!')
                }
                if (StringUtils.isBlank(testConfig.testSuiteName)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testSuiteName not provided!')
                }
                break
            case "APPIUM_CROSS":
                if (StringUtils.isBlank(testConfig.testAppPath)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testAppPath not provided!')
                }
                if (StringUtils.isBlank(testConfig.testSuiteName)) {
                    throw new IllegalArgumentException('Running type ' + testConfig.runningType + ' required param testSuiteName not provided!')
                }
                break
            case "SMART":
                break
            case "T2C_JSON":
                break
            case "APPIUM_MONKEY":
                break
            case "MONKEY":
                break
            default:
                break
        }
    }

    interface TestScope {
        String TEST_APP = "TEST_APP";
        String PACKAGE = "PACKAGE";
        String CLASS = "CLASS";
    }
}
