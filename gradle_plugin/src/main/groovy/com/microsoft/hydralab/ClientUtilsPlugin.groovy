// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab

import com.microsoft.hydralab.utils.HydraLabClientUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project


class ClientUtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.task("requestHydraLabTest") {
            doFirst {
                def runningType = ""
                if (project.hasProperty('runningType')) {
                    runningType = project.runningType
                }
                def deviceIdentifier = ""
                if (project.hasProperty('deviceIdentifier')) {
                    deviceIdentifier = project.deviceIdentifier
                }
                def runTimeOutSeconds = ""
                if (project.hasProperty('runTimeOutSeconds')) {
                    runTimeOutSeconds = project.runTimeOutSeconds
                }
                def queueTimeOutSeconds = runTimeOutSeconds
                if (project.hasProperty('queueTimeOutSeconds')) {
                    queueTimeOutSeconds = project.queueTimeOutSeconds
                }
                def testSuiteName = ""
                if (project.hasProperty('testSuiteName')) {
                    testSuiteName = project.testSuiteName
                }

                def appPath = ""
                if (project.hasProperty('appPath')) {
                    def appFile = project.file(project.appPath)
                    println("Param appPath: ${project.appPath}")
                    if (!appFile.exists()) {
                        def exceptionMsg = "${project.appPath} file not exist!"
                        throw new Exception(exceptionMsg)
                    } else {
                        appPath = appFile.absolutePath
                    }
                }

                def testAppPath = ""
                if (project.hasProperty('testAppPath')) {
                    def testAppFile = project.file(project.testAppPath)
                    println("Param testAppPath: ${project.testAppPath}")
                    if (!testAppFile.exists()) {
                        def exceptionMsg = "${project.testAppPath} file not exist!"
                        throw new Exception(exceptionMsg)
                    } else {
                        testAppPath = testAppFile.absolutePath
                    }
                }

                def attachmentConfigPath = ""
                if (project.hasProperty('attachmentConfigPath')) {
                    def attachmentConfigFile = project.file(project.attachmentConfigPath)
                    println("Param attachmentConfigPath: ${project.attachmentConfigPath}")
                    if (!attachmentConfigFile.exists()) {
                        def exceptionMsg = "${project.attachmentConfigPath} file not exist!"
                        throw new Exception(exceptionMsg)
                    } else {
                        attachmentConfigPath = attachmentConfigFile.absolutePath
                    }
                }

                def reportDir = new File(project.buildDir, "testResult")
                if (!reportDir.exists()) reportDir.mkdirs()

                def argsMap = null
                if (project.hasProperty('instrumentationArgs')) {
                    argsMap = [:]
                    // quotation marks not support
                    def argLines = project.instrumentationArgs.replace("\"", "").split(",")
                    for (i in 0..<argLines.size()) {
                        String[] kv = argLines[i].split("=")
                        // use | to represent comma to avoid conflicts
                        argsMap.put(kv[0], kv[1].replace("|", ","))
                    }
                }

                def tag = null
                if (project.hasProperty('tag')) {
                    tag = project.tag
                }

                def extraArgsMap = null
                if (project.hasProperty('extraArgs')) {
                    extraArgsMap = [:]
                    // quotation marks not support
                    def argLines = project.extraArgs.replace("\"", "").split(",")
                    for (i in 0..<argLines.size()) {
                        String[] kv = argLines[i].split("=")
                        // use | to represent comma to avoid conflicts
                        extraArgsMap.put(kv[0], kv[1].replace("|", ","))
                    }
                }

                HydraLabClientUtils.HydraLabAPIConfig apiConfig = HydraLabClientUtils.HydraLabAPIConfig.defaultAPI()

                if (project.hasProperty('hydraLabAPISchema')) {
                    apiConfig.schema = project.hydraLabAPISchema
                }
                if (project.hasProperty('hydraLabAPIHost')) {
                    apiConfig.host = project.hydraLabAPIHost
                }
                if (project.hasProperty('authToken')) {
                    apiConfig.authToken = project.authToken
                }
                if (project.hasProperty('onlyAuthPost')) {
                    apiConfig.onlyAuthPost = Boolean.parseBoolean(project.onlyAuthPost)
                }
                if (project.hasProperty('pkgName')) {
                    apiConfig.pkgName = project.pkgName
                }
                if (project.hasProperty('testPkgName')) {
                    apiConfig.testPkgName = project.testPkgName
                }
                if (project.hasProperty('groupTestType')) {
                    apiConfig.groupTestType = project.groupTestType
                }
                if (project.hasProperty('frameworkType')) {
                    apiConfig.frameworkType = project.frameworkType
                }
                if (project.hasProperty('maxStepCount')) {
                    apiConfig.maxStepCount = Integer.parseInt(project.maxStepCount)
                }
                if (project.hasProperty('deviceTestCount')) {
                    apiConfig.deviceTestCount = Integer.parseInt(project.deviceTestCount)
                }
                if (project.hasProperty('teamName')) {
                    apiConfig.teamName = project.teamName
                }
                if (project.hasProperty('testRunnerName')) {
                    apiConfig.testRunnerName = project.testRunnerName
                }
                if (project.hasProperty('testScope')) {
                    apiConfig.testScope = project.testScope
                }
                if (project.hasProperty('needUninstall')) {
                    apiConfig.needUninstall = Boolean.parseBoolean(project.needUninstall)
                }
                if (project.hasProperty('needClearData')) {
                    apiConfig.needClearData = Boolean.parseBoolean(project.needClearData)
                }
                if (project.hasProperty('neededPermissions')) {
                    apiConfig.neededPermissions = project.neededPermissions.split(", +")
                }
                if (project.hasProperty('deviceActions')) {
                    // add quotes back as quotes in gradle plugins will be replaced by blanks
                    apiConfig.deviceActionsStr = project.deviceActions.replace("\\", "\"")
                }

                requiredParamCheck(runningType, appPath, testAppPath, deviceIdentifier, runTimeOutSeconds, testSuiteName, apiConfig)

                HydraLabClientUtils.runTestOnDeviceWithApp(
                        runningType, appPath, testAppPath, attachmentConfigPath,
                        testSuiteName, deviceIdentifier, Integer.parseInt(queueTimeOutSeconds), Integer.parseInt(runTimeOutSeconds),
                        reportDir.absolutePath, argsMap, extraArgsMap, tag,
                        apiConfig
                )
            }
        }.configure {
            group = "Test"
            description = "Run mobile/cross-platform test with specified params on Hydra Lab - see more in https://github.com/microsoft/HydraLab/wiki"
        }
    }

    private void requiredParamCheck(String runningType, String appPath, String testAppPath, String deviceIdentifier, String runTimeOutSeconds, String testSuiteName, HydraLabClientUtils.HydraLabAPIConfig apiConfig) {
        if (StringUtils.isBlank(runningType)
                || StringUtils.isBlank(appPath)
                || StringUtils.isBlank(apiConfig.pkgName)
                || StringUtils.isBlank(deviceIdentifier)
                || StringUtils.isBlank(runTimeOutSeconds)
                || StringUtils.isBlank(apiConfig.authToken)
        ) {
            throw new IllegalArgumentException('Required params not provided! Make sure the following params are all provided correctly: authToken, appPath, pkgName, runningType, deviceIdentifier, runTimeOutSeconds.')
        }

        // running type specified params
        switch (runningType) {
            case "INSTRUMENTATION":
                if (StringUtils.isBlank(testAppPath)) {
                    throw new IllegalArgumentException('Required param testAppPath not provided!')
                }
                if (StringUtils.isBlank(apiConfig.testPkgName)) {
                    throw new IllegalArgumentException('Required param testPkgName not provided!')
                }
                if (apiConfig.testScope != TestScope.PACKAGE && apiConfig.testScope != TestScope.CLASS) {
                    break
                }
                if (StringUtils.isBlank(testSuiteName)) {
                    throw new IllegalArgumentException('Required param testSuiteName not provided!')
                }
                break
            case "APPIUM":
                if (StringUtils.isBlank(testAppPath)) {
                    throw new IllegalArgumentException('Required param testAppPath not provided!')
                }
                if (StringUtils.isBlank(testSuiteName)) {
                    throw new IllegalArgumentException('Required param testSuiteName not provided!')
                }
                break
            case "APPIUM_CROSS":
                if (StringUtils.isBlank(testAppPath)) {
                    throw new IllegalArgumentException('Required param testAppPath not provided!')
                }
                if (StringUtils.isBlank(testSuiteName)) {
                    throw new IllegalArgumentException('Required param testSuiteName not provided!')
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
