// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab

import com.microsoft.hydralab.utils.HydraLabClientUtils
import org.gradle.api.Plugin
import org.gradle.api.Project


class ClientUtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.task("requestHydraLabTest") {
            doFirst {
                // try run with params:
                // -PappPath=path/to/app -PtestAppPath=path/to/app -PbuildFlavor=flavor -PtestSuiteName=SuiteFullName -PinstrumentationArgs="a=b,c=d"
                // to ignore a case use -PinstrumentationArgs="ignores=testA|testB"
                if (!project.hasProperty("appPath")
                        || !project.hasProperty("runningType")
                        || !project.hasProperty("pkgName")
                        || !project.hasProperty("deviceIdentifier")
                        || !project.hasProperty("runTimeOutSeconds")
                        || !project.hasProperty("authToken")
                ) {
                    throw new Exception('Required params not provided! Make sure the following params are all provided correctly: authToken, appPath, pkgName, runningType, deviceIdentifier, runTimeOutSeconds.')
                }
                def runningType = ""
                if (project.hasProperty('runningType')) {
                    runningType = project.runningType
                }
                def deviceIdentifierArg = null
                if (project.hasProperty('deviceIdentifier')) {
                    deviceIdentifierArg = project.deviceIdentifier
                }
                def queueTimeOutSeconds = project.runTimeOutSeconds
                if (project.hasProperty('queueTimeOutSeconds')) {
                    queueTimeOutSeconds = project.queueTimeOutSeconds
                }
                // running type specified params
                switch (runningType) {
                    case "INSTRUMENTATION":
                        if (!project.hasProperty("testAppPath")) {
                            throw new Exception('Required param testAppPath not provided!')
                        }
                        if (!project.hasProperty("testSuiteName")) {
                            throw new Exception('Required param testSuiteName not provided!')
                        }
                        if (!project.hasProperty("testPkgName")) {
                            throw new Exception('Required param testPkgName not provided!')
                        }
                        break
                    case "APPIUM":
                        if (!project.hasProperty("testAppPath")) {
                            throw new Exception('Required param testAppPath not provided!')
                        }
                        if (!project.hasProperty("testSuiteName")) {
                            throw new Exception('Required param testSuiteName not provided!')
                        }
                        break
                    case "APPIUM_CROSS":
                        if (!project.hasProperty("testAppPath")) {
                            throw new Exception('Required param testAppPath not provided!')
                        }
                        if (!project.hasProperty("testSuiteName")) {
                            throw new Exception('Required param testSuiteName not provided!')
                        }
                        break
                    case "SMART":
                        if (!project.hasProperty("maxStepCount")) {
                            throw new Exception('Required param maxStepCount not provided!')
                        }
                        if (!project.hasProperty("deviceTestCount")) {
                            throw new Exception('Required param deviceTestCount not provided!')
                        }
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
                def tag = null
                if (project.hasProperty('tag')) {
                    tag = project.tag
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
                } else {
                    if (!project.hasProperty('testSuiteName')) {
                        // set default testScope as TEST_APP
                        apiConfig.testScope = "TEST_APP"
                    }
                }
                def testSuiteName = ""
                if (project.hasProperty('testSuiteName')) {
                    testSuiteName = project.testSuiteName
                }

                // optional for APPIUM_CROSS, T2C_JSON
                if (project.hasProperty('needUninstall')) {
                    apiConfig.needUninstall = Boolean.parseBoolean(project.needUninstall)
                }
                if (project.hasProperty('needClearData')) {
                    apiConfig.needClearData = Boolean.parseBoolean(project.needClearData)
                }

                HydraLabClientUtils.runTestOnDeviceWithApp(
                        runningType, appPath, testAppPath, attachmentConfigPath,
                        testSuiteName, deviceIdentifierArg, Integer.parseInt(queueTimeOutSeconds), Integer.parseInt(project.runTimeOutSeconds),
                        reportDir.absolutePath, argsMap, extraArgsMap, tag,
                        apiConfig
                )
            }
        }.configure {
            group = "Test"
            description = "Run mobile/cross-platform test with specified params on Hydra Lab - see more in https://github.com/microsoft/HydraLab/wiki"
        }
    }

}
