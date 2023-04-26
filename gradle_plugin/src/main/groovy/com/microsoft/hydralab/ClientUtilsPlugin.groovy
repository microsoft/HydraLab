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

                def reportDir = new File(project.buildDir, "testResult")
                if (!reportDir.exists()) {
                    reportDir.mkdirs()
                }

                // read config from yml
                if (project.hasProperty('ymlConfigFile')) {
                    YamlParser yamlParser = new YamlParser(project.ymlConfigFile)
                    apiConfig = yamlParser.parseAPIConfig()
                    testConfig = yamlParser.parseTestConfig()
                }

                if (project.hasProperty('appPath')) {
                    testConfig.appPath = CommonUtils.validateAndReturnFilePath(project.appPath, "appPath")
                }
                if (project.hasProperty('testAppPath')) {
                    testConfig.testAppPath = CommonUtils.validateAndReturnFilePath(project.testAppPath, "testAppPath")
                }
                if (project.hasProperty('attachmentConfigPath')) {
                    testConfig.attachmentConfigPath = CommonUtils.validateAndReturnFilePath(project.attachmentConfigPath, "attachmentConfigPath")
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

                if (testConfig.deviceConfig == null) {
                    testConfig.deviceConfig = new DeviceConfig()
                }
                if (project.hasProperty('deviceIdentifier')) {
                    testConfig.deviceConfig.deviceIdentifier = project.deviceIdentifier
                }
                if (project.hasProperty('groupTestType')) {
                    testConfig.deviceConfig.groupTestType = project.groupTestType
                }
                if (project.hasProperty('deviceActions')) {
                    // add quotes back as quotes in gradle plugins will be replaced by blanks
                    testConfig.deviceConfig.deviceActionsStr = project.deviceActions.replace("\\", "\"")
                }

                if (project.hasProperty('triggerType')) {
                    testConfig.triggerType = project.triggerType
                }
                // @Deprecated
                else if (project.hasProperty('type')) {
                    testConfig.triggerType = project.type
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
                if (project.hasProperty('appVersion')) {
                    testConfig.appVersion = project.appVersion
                }
                if (project.hasProperty('needInstall')) {
                    testConfig.needInstall = Boolean.parseBoolean(project.needInstall)
                }
                if (project.hasProperty('needUninstall')) {
                    testConfig.needUninstall = Boolean.parseBoolean(project.needUninstall)
                }
                if (project.hasProperty('needClearData')) {
                    testConfig.needClearData = Boolean.parseBoolean(project.needClearData)
                }
                if (project.hasProperty('neededPermissions')) {
                    testConfig.neededPermissions = project.neededPermissions.split(", +")
                }
                if (project.hasProperty('artifactTag')) {
                    testConfig.artifactTag = project.artifactTag
                }
                // @Deprecated
                else if (project.hasProperty('tag')) {
                    testConfig.artifactTag = project.tag
                }
                if (project.hasProperty('testRunArgs')) {
                    testConfig.testRunArgs = CommonUtils.parseArguments(project.testRunArgs)
                }
                // @Deprecated
                else if (project.hasProperty('instrumentationArgs')) {
                    testConfig.testRunArgs = CommonUtils.parseArguments(project.instrumentationArgs)
                }
                if (project.hasProperty('maxStepCount')) {
                    testConfig.maxStepCount = Integer.parseInt(project.maxStepCount)
                }
                if (project.hasProperty('testRound')) {
                    testConfig.testRound = Integer.parseInt(project.testRound)
                }
                // @Deprecated
                else if (project.hasProperty('deviceTestCount')) {
                    testConfig.testRound = Integer.parseInt(project.deviceTestCount)
                }
                if (project.hasProperty('inspectionStrategiesStr')) {
                    // add quotes back as quotes in gradle plugins will be replaced by blanks
                    testConfig.inspectionStrategiesStr = project.inspectionStrategiesStr.replace("\\", "\"")
                }
                if (project.hasProperty('enableFailingTask')) {
                    // add quotes back as quotes in gradle plugins will be replaced by blanks
                    testConfig.enableFailingTask = Boolean.parseBoolean(project.enableFailingTask)
                }

                requiredParamCheck(apiConfig, testConfig)

                HydraLabClientUtils.runTestOnDeviceWithApp(reportDir.absolutePath, apiConfig, testConfig)
            }
        }.configure {
            group = "Test"
            description = "Run mobile/cross-platform test with specified params on Hydra Lab - see more in https://github.com/microsoft/HydraLab/wiki"
        }
    }

    void requiredParamCheck(HydraLabAPIConfig apiConfig, TestConfig testConfig) {
        if (StringUtils.isBlank(apiConfig.host)
                || StringUtils.isBlank(apiConfig.authToken)
                || StringUtils.isBlank(testConfig.appPath)
                || StringUtils.isBlank(testConfig.pkgName)
                || StringUtils.isBlank(testConfig.runningType)
                || testConfig.runTimeOutSeconds == 0
                || StringUtils.isBlank(testConfig.deviceConfig.deviceIdentifier)
        ) {
            throw new IllegalArgumentException('Required params not provided! Make sure the following params are all provided correctly: hydraLabAPIHost, authToken, deviceIdentifier, appPath, pkgName, runningType, runTimeOutSeconds.')
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
