import tl = require('azure-pipelines-task-lib/task')
import * as path from 'path';
import * as fs from 'fs';
import FormData = require('form-data');

async function run() {
    try {
        tl.setResourcePath(path.join( __dirname, 'task.json'));

        let taskSuccess: boolean = true;

        const HydraLabAPIConfig: any = {};

        const Path: object = {
            'getCenterInfo': "/api/center/info/",
            'uploadAPKAPIPath': "/api/package/add/",
            'generateDeviceGroupAccessKey': "/api/deviceGroup/generate?deviceIdentifier=",
            'runTestAPIPath': "/api/test/task/run/",
            'testStatusAPIPath': "/api/test/task/",
            'testPortalTaskInfoPath': "/portal/index.html?redirectUrl=/info/task/",
            'testPortalTaskDeviceVideoPath': "/portal/index.html?redirectUrl=/info/videos/"
        };

        Object.assign(HydraLabAPIConfig, { 'Path': Path });

        const serviceEndpointId: string | undefined = tl.getInput('serviceEndpoint', true);
        
        if (!serviceEndpointId) {
            throw new Error('EndpointId Not Found');
        }

        const serviceEndpointUrl: string | undefined = tl.getEndpointUrl(serviceEndpointId, true);
        if (!serviceEndpointUrl) {
            throw new Error('EndpointUrl Not Found');
        }
        Object.assign(HydraLabAPIConfig, { 'serviceEndpointUrl': serviceEndpointUrl });

        const serviceEndpoint: tl.EndpointAuthorization | undefined = tl.getEndpointAuthorization(serviceEndpointId, true);
        if (!serviceEndpoint) {
            throw new Error('Endpoint Not Found');
        }
        const authToken: string = serviceEndpoint.parameters['authToken'].replace(/\\n/g, '\n');
        Object.assign(HydraLabAPIConfig, { 'authToken': authToken });

        let runningType: string | undefined = tl.getInput('runningType', false);
        if (!runningType) {
            throw new Error('Running Type is not set');
        }
        console.log('##[section]runningType: %s', runningType);

        const apkPath: string | undefined = tl.getPathInput('pkgPath', true, true);
        if (!apkPath) {
            throw new Error('Package Path is not set');
        }
        console.log('##[section]apkPath: %s', apkPath);

        const buildFlavor: string | undefined = tl.getInput('buildFlavor', false) ? tl.getInput('buildFlavor', false) : 'UNKNOWN';
        console.log('##[section]buildFlavor: %s', buildFlavor);

        let testApkPath: string = apkPath
        if (runningType === 'INSTRUMENTATION' || runningType === 'APPIUM') {
            const testApkPathInput: string | undefined = tl.getPathInput('testPkgPath', true, true);
            if (!testApkPathInput) {
                throw new Error('Test Package Path is not set');
            }
            testApkPath = testApkPathInput    
        }
        console.log('##[section]testApkPath: %s', testApkPath);

        let frameworkType: string | undefined = undefined
        if (runningType === 'APPIUM') {
            frameworkType = tl.getInput('frameworkType', true);
            if (!frameworkType) {
                throw new Error('Frame Work Type is not set');
            }
            Object.assign(HydraLabAPIConfig, { 'frameworkType': frameworkType });
        }
        const pkgName: string | undefined = tl.getInput('pkgName', true);
        if (!pkgName) {
            throw new Error('APK Package Name is not set');
        }
        Object.assign(HydraLabAPIConfig, { 'pkgName': pkgName });

        if (runningType === 'INSTRUMENTATION') {
            const testPkgName: string | undefined = tl.getInput('testPkgName', true);
            if (!testPkgName) {
                throw new Error('Test APK Package Name is not set');
            }
            Object.assign(HydraLabAPIConfig, { 'testPkgName': testPkgName });
        }

        let testSuiteClass: string | undefined = undefined;
        if (runningType === 'INSTRUMENTATION' || runningType === 'APPIUM') {
            testSuiteClass = tl.getInput('testSuiteClass', true);
            if (!testSuiteClass) {
                throw new Error('Test Suite Name is not set');
            }
            console.log('##[section]testSuiteClass: %s', testSuiteClass);
        }

        let maxStepCount: number | undefined = undefined;
        if (runningType === 'SMART') {
            const maxStepCountInput: string | undefined = tl.getInput('maxStepCount', true);
            if (!maxStepCountInput) {
                throw new Error('Max Step Count is not set');
            }
            maxStepCount = Number(maxStepCountInput);
            console.log('##[section]maxStepCount: %s', maxStepCount);
        }

        let deviceTestCount: number | undefined = undefined;
        if (runningType === 'SMART') {
            const deviceTestCountInput: string | undefined = tl.getInput('deviceTestCount', true);
            if (!deviceTestCountInput) {
                throw new Error('Device Test Count is not set');
            }
            deviceTestCount = Number(deviceTestCountInput);
            console.log('##[section]deviceTestCount: %s', deviceTestCount);
        }

        const deviceIdentifier: string | undefined = tl.getInput('deviceIdentifier', true);
        if (!deviceIdentifier) {
            throw new Error('Device Identifier is not set');
        }
        console.log('##[section]deviceIdentifier: %s', deviceIdentifier);

        let groupTestType: string | undefined = undefined;
        if (deviceIdentifier.startsWith('G.')) {
            groupTestType = tl.getInput('groupTestType', true);
            if (!groupTestType) {
                throw new Error('Group Test Type is not set');
            }
            Object.assign(HydraLabAPIConfig, { 'groupTestType': groupTestType });
            console.log('##[section]groupTestType: %s', groupTestType);
        }


        const build_reason: string | undefined = tl.getVariable('BUILD_REASON');
        if (build_reason) {
            Object.assign(HydraLabAPIConfig, { 'build_reason': build_reason });
            console.log('##[section]type: %s', build_reason);
        }

        const timeoutSecInput: string | undefined = tl.getInput('timeoutSec', true);
        if (!timeoutSecInput) {
            throw new Error('Timeout Sec is not set');
        }
        const timeoutSec: number = Number(timeoutSecInput);
        console.log('##[section]timeoutSec: %s', timeoutSec);


        const instrumentationArgsInput: string[] | undefined = tl.getDelimitedInput('instrumentationArgs', ',', false);
        let instrumentationArgs: object = {};
        if (instrumentationArgsInput && instrumentationArgsInput.length > 0) {
            for (const instrumentationArg of instrumentationArgsInput) {
                const instrumentationArgSplit: string[] = instrumentationArg.split('=');
                if (instrumentationArgSplit.length === 2) {
                    Object.assign(instrumentationArgs, JSON.parse(`{"${instrumentationArgSplit[0]}": "${instrumentationArgSplit[1].replace(/|/g, ',')}"}`))
                } else {
                    throw new Error('Illegal Instrumentation Args');
                }
            } 
        }
        console.log('##[section]instrumentationArgs: %s', JSON.stringify(instrumentationArgs));

        // let extraArgs: object[] = [{}];
        // const extraArgsInput: string[] | undefined = tl.getDelimitedInput('extraArgs', ',', false);
        // if (extraArgsInput && extraArgsInput.length > 0) {
        //     for (const extraArg of extraArgsInput) {
        //         const extraArgSplit: string[] = extraArg.split('=');
        //         if (extraArgSplit.length === 2) {
        //             extraArgs.push(JSON.parse(`{"${extraArgSplit[0]}": "${extraArgSplit[1]}"}`))
        //         } else {
        //             throw new Error('Illegal Extra Args');
        //         }
        //     } 
        // }
        // console.log('##[section]extraArgs: %s', JSON.stringify(extraArgs));

        let runningInfo: string | undefined = tl.getInput('runningInfo', false);
        if (!runningInfo) {
            throw new Error('Running Info is not set');
        }
        console.log('##[section]runningInfo: %s', runningType);

        let pipelineLink: string | undefined = undefined; 
        let commitId: string | undefined = undefined; 
        let commitCount: string | undefined = undefined; 
        let commitMsg: string | undefined = undefined; 
        let reportAudience: string | undefined = undefined; 

        if (runningInfo == 'manually') {
            pipelineLink = tl.getInput('pipelineLink', false);
            commitId = tl.getInput('commitId', false);
            commitCount = tl.getInput('commitCount', false);
            commitMsg = tl.getInput('commitMsg', false);
            reportAudience = tl.getInput('reportAudience', false);
        }
        if (pipelineLink === undefined) {
            if (tl.getVariable('SYSTEM_TEAMFOUNDATIONCOLLECTIONURI') && tl.getVariable('SYSTEM_TEAMPROJECT')  && tl.getVariable('BUILD_BUILDID')) {
                pipelineLink = `${tl.getVariable('SYSTEM_TEAMFOUNDATIONCOLLECTIONURI')}${tl.getVariable('SYSTEM_TEAMPROJECT')}/_build/results?buildId=${tl.getVariable('BUILD_BUILDID')}`;
                pipelineLink.replace(/\s+/g, '%20');
            }
            else {
                pipelineLink = 'UNKNOWN';
            }
        }
        if (commitId === undefined) {
            commitId = (tl.getVariable('BUILD_SOURCEVERSION')) ? tl.getVariable('BUILD_SOURCEVERSION') : 'COMMIT_ID_RETRIEVE_FAIL';
        }
        if (commitCount === undefined) {
            commitCount = '-1';
        }
        if (commitMsg === undefined) {
            const build_source_version_message: string | undefined = tl.getVariable('BUILD_SOURCEVERSIONMESSAGE')
            if (build_source_version_message) {
                commitMsg = build_source_version_message.split('\n')[0]
            } 
            else {
                commitMsg = 'UNKNOWN';
            }
        }
        if (reportAudience === undefined) {
            reportAudience = 'TestLabOwner';
        }

        console.log('##[section]pipelineLink: %s', pipelineLink);
        console.log('##[section]commitId: %s', commitId);
        console.log('##[section]commitCount: %s', commitCount);
        console.log('##[section]commitMsg: %s', commitMsg);
        console.log('##[section]reportAudience: %s', reportAudience);

        let dir: string | undefined = tl.getVariable('BUILD_ARTIFACTSTAGINGDIRECTORY');

        if (!dir) {
            dir = __dirname
        }

        const reportFolderPath: string = path.join(dir, `/outputs/androidTest-results/connected/flavors/${buildFlavor}`);
        console.log('##[section]reportFolderPath: %s', reportFolderPath);

        fs.mkdir(reportFolderPath, { recursive: true }, (err) => {
            if (err) throw err;
            console.log('##[section]Report Folder created successfully!'); 
        });

        const uploadAPKResult: any = await uploadAPK(HydraLabAPIConfig, commitId, commitCount, commitMsg, buildFlavor, apkPath, testApkPath);

        if (uploadAPKResult['status']) {
            console.log(`##[section]Uploaded APK set id: ${uploadAPKResult.apkSetId}`)
        }
        else {
            throw new Error(uploadAPKResult['message']);
        }

        let accessKey: string | undefined = undefined;
        const generateDeviceGroupAccessKeyResult: any = await generateDeviceGroupAccessKey(HydraLabAPIConfig, deviceIdentifier);

        if (generateDeviceGroupAccessKeyResult['status']) {
            accessKey = generateDeviceGroupAccessKeyResult.accessKey;
            console.log('##[section]Generate Group AccessKey Successfully!')
        }
        else {
            console.log('##[section]Generate Group AccessKey Failed!')
        }
    

        let triggerTestRunResult: any = await triggerTestRun(HydraLabAPIConfig, runningType, deviceIdentifier, uploadAPKResult.apkSetId, testSuiteClass, timeoutSec, maxStepCount, deviceTestCount, pipelineLink, accessKey, frameworkType, reportAudience, instrumentationArgs)

        if (triggerTestRunResult['status']) {
            console.log(`##[section]Triggered test task id: ${triggerTestRunResult['testTaskId']} successful!`);
        }
        else {
            throw new Error(triggerTestRunResult['message']);
        }
        
        let sleepSecond: number = Math.round(timeoutSec / 3);
        let minWaitSec: number = 15;
        let totalWaitSecond: number = 0;
        let finished: boolean = false;
        let runningTest: any;
        let HydraRetryTime = 0;
        while (!finished) {
            if (totalWaitSecond > timeoutSec) {
                break;
            }
            console.log("Get test status after waiting for %d seconds", totalWaitSecond);

            runningTest = await getTestStatus(HydraLabAPIConfig, triggerTestRunResult['testTaskId']);
            // console.log("Current running test info: %s", JSON.stringify(runningTest.TestTask, null, 2));

            if (!runningTest['status']) {
                throw new Error(runningTest['message']);
            }

            if (HydraRetryTime != runningTest.TestTask.retryTime) {
                HydraRetryTime = runningTest.TestTask.retryTime;
                console.log("##[command]Retrying to run task again, waitSecond will be reset. current retryTime is : %d", HydraRetryTime);
                totalWaitSecond = 0;
                sleepSecond = timeoutSec / 3;
            }

            if (runningTest.TestTask.status === 'waiting') {
                console.log(`##[command]Start waiting: 30 seconds, ${runningTest.TestTask.message}`);
                await delay(30000);
            } else {
                console.log(`##[command]Running test on ${runningTest.TestTask.testDevicesCount} devices, status for now: ${runningTest.TestTask.status}`);
                if (runningTest.TestTask.status === 'canceled') {
                    throw new Error(`The test task is canceled`);
                }
                if (runningTest.TestTask.status === 'error') {
                    throw new Error(`The test task is error`);
                }
                finished = (runningTest.TestTask.status === 'finished');

                if(finished){
                    break;
                }

                console.log(`##[command]Start waiting: ${sleepSecond} seconds`);
                await delay(Math.round(sleepSecond * 1000));
                totalWaitSecond += sleepSecond;
                sleepSecond = Math.round(Math.max(sleepSecond / 2, minWaitSec));
            }
        }
        
        if (!finished) {
            throw new Error(`Time out after waiting for ${timeoutSec} seconds! Test id ${runningTest.TestTask.id}`);
        }

        if (!runningTest.TestTask.deviceTestResults) {
            throw new Error(`No deviceTestResults! Test id ${runningTest.TestTask.id}`);
        }

        const testReportUrl: string = HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.testPortalTaskInfoPath + runningTest.TestTask.id;

        const StringBuilder = require("string-builder");
        const mdBuilder: any = new StringBuilder();

        mdBuilder.append("# Hydra Lab Test Result Details\n\n\n");
        mdBuilder.appendFormat("### [Link to full report]({0})\n\n\n", testReportUrl);
        mdBuilder.appendFormat("### Statistic: total test case count: {0}, failed: {1}\n\n", runningTest.TestTask.totalTestCount, runningTest.TestTask.totalFailCount);
        if (runningTest.TestTask.totalFailCount > 0 && runningTest.TestTask.reportImagePath != null) {
            console.log("##[warning] %d cases failed during the test", runningTest.TestTask.totalFailCount);
        }

        if (runningTest.TestTask.totalFailCount > 0 || runningTest.TestTask.totalTestCount == 0) {
            taskSuccess = false;
        }

        console.log("##vso[task.setprogress value=90;]Almost Done with testing");

        for (let index in runningTest.TestTask.deviceTestResults) {
            let deviceSerialNumber: string = runningTest.TestTask.deviceTestResults[index].deviceSerialNumber
            console.log(">>>>>>\n Device %s, failed cases count: %d, total cases: %d", deviceSerialNumber, runningTest.TestTask.deviceTestResults[index].failCount, runningTest.TestTask.deviceTestResults[index].totalCount);
            if (runningTest.TestTask.deviceTestResults[index].failCount > 0 || runningTest.TestTask.deviceTestResults[index].totalCount == 0) {
                if (runningTest.TestTask.deviceTestResults[index].crashStack != null && runningTest.TestTask.deviceTestResults[index].crashStack.length > 0) {
                    console.log("##[error]Fatal error during test on device %s, stack:\n%s", deviceSerialNumber, runningTest.TestTask.deviceTestResults[index].crashStack);
                } else {
                    console.log("##[error]Fatal error during test on device %s, crash stack is null.", deviceSerialNumber);
                }
                taskSuccess = false;
            }

            mdBuilder.appendFormat("- On device {0} (SN: {1}), total case count: {2}, failed: {3}\n", runningTest.TestTask.deviceTestResults[index].deviceName, deviceSerialNumber, runningTest.TestTask.deviceTestResults[index].totalCount, runningTest.TestTask.deviceTestResults[index].failCount)
            
            if (runningTest.TestTask.deviceTestResults[index].attachments.length == 0) {
                continue;
            }

            fs.mkdir(path.join(reportFolderPath, deviceSerialNumber), { recursive: true }, (err) => {
                if (err) throw err;
                console.log('##[section]Report Subfolder created successfully!'); 
            });

            for (let attachmentIndex in runningTest.TestTask.deviceTestResults[index].attachments) {
                let attachmentUrl: string = runningTest.TestTask.deviceTestResults[index].attachments[attachmentIndex].blobUrl;
                let attachmentFileName: string = runningTest.TestTask.deviceTestResults[index].attachments[attachmentIndex].fileName;
                let attachmentPath: string = path.join(reportFolderPath, deviceSerialNumber, attachmentFileName);

                console.log("Start downloading attachment for device: %s, device name: %s, filename: %s, link: %s", deviceSerialNumber, runningTest.TestTask.deviceTestResults[index].deviceName, attachmentFileName, attachmentUrl);
                await downloadToFile(attachmentUrl, attachmentPath);
                console.log("Finish downloading attachment for device %s", deviceSerialNumber);
            }
        }

        // use the https://docs.microsoft.com/en-us/azure/devops/pipelines/scripts/logging-commands?view=azure-devops&tabs=powershell#build-commands
        // to upload the report
        console.log("##vso[artifact.upload artifactname=testResult;]%s", path.resolve(reportFolderPath));

        console.log("##[section]All done, overall failed cases count: %d, total count: %d, devices count: %d", runningTest.TestTask.totalFailCount, runningTest.TestTask.totalTestCount, runningTest.TestTask.testDevicesCount);
        console.log("##[section]Test task report link:");
        console.log(testReportUrl);
        console.log("##vso[task.setvariable variable=TestTaskReportLink;]%s", testReportUrl);

        let TestLabSummaryPath: string = path.join(reportFolderPath, "TestLabSummary.md")

        await fs.writeFile(TestLabSummaryPath, mdBuilder.toString(), (err) => {
            if (err) throw err;
            console.log("##vso[task.uploadsummary]%s", path.resolve(TestLabSummaryPath));
        });
        
        if (taskSuccess) {
            tl.setResult(tl.TaskResult.Succeeded, 'Success');
        } else {
            tl.setResult(tl.TaskResult.Failed, 'Test Failed');
        }   
    }
    catch (err: any) {
        console.log(err)
        tl.setResult(tl.TaskResult.Failed, err.message);
    }
}

async function getCenterInfo(HydraLabAPIConfig: any): Promise<object> {
    const axios = require('axios').default;

    let result: object = { 'status': false };

    await axios({
        method: "get",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.getCenterInfo,
        headers: { "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    })
    .then(function (response: any) {
        if (response.data.code == 200){
            Object.assign(result, { 
                'status': true,
                'message': `The request was made and the server responded with a status code: ${response.status}`,
                'centerInfo': response.data.content
            });
        } else {
            Object.assign(result, { 
                'status': false,
                'message': `Error Code from center: ${response.data.code}, message: ${response.data.message}`,
            });
        }
    })
    .catch(function (error: any) {
        console.log(error);
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            Object.assign(result, { 
                'status': false,
                'message': `The request was made and the server responded with a status code: ${error.response.status}`
            });
          } else if (error.request) {
            // The request was made but no response was received
            // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
            // http.ClientRequest in node.js
            Object.assign(result, { 
                'status': false,
                'message': `The request was made but no response was received`
            });
          } else {
            // Something happened in setting up the request that triggered an Error
            Object.assign(result, { 
                'status': false,
                'message': `Something happened in setting up the request that triggered an Error`
            });
          }
    });

    return result;
}

async function uploadAPK(HydraLabAPIConfig: any, commitId: string | undefined, commitCount: string | undefined, commitMsg: string | undefined, buildFlavor: string | undefined, apkPath: string, testApkPath: string): Promise<object> {
    const axios = require('axios').default;
    const form = new FormData();

    form.append('commitId', commitId);
    form.append('commitCount', commitCount);
    form.append('commitMessage', commitMsg);
    form.append('buildFlavor', buildFlavor);
    form.append('apkFile', fs.createReadStream(apkPath), { filename: path.basename(apkPath), contentType: 'application/vnd.android.package-archive' });
    form.append('testApkFile', fs.createReadStream(testApkPath), { filename: path.basename(testApkPath), contentType: 'application/vnd.android.package-archive' });
    
    let result: object = { 'status': false };

    let centerInfo: any = await getCenterInfo(HydraLabAPIConfig);
    if (centerInfo.status) {
        Object.assign(result, { 'centerInfo': centerInfo.centerInfo });
    } else {
        Object.assign(result, {
            'status': false,
            'message': `[GetCenterInfo] Center is not alive, message: ${centerInfo.message}`
        });
        return result
    }

    await axios({
        method: "post",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.uploadAPKAPIPath,
        data: form,
        maxContentLength: Infinity,
        maxBodyLength: Infinity,        
        headers: { "Content-Type": `multipart/form-data; boundary=${form.getBoundary()}`, "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    })
    .then(function (response: any) {
        if (response.data.code == 200){
            Object.assign(result, { 
                'status': true,
                'message': `[UploadAPK] The request was made and the server responded with a status code: ${response.status}`,
                'apkSetId': response.data.content.id
            });
        } else {
            Object.assign(result, { 
                'status': false,
                'message': `[UploadAPK] Error Code from center: ${response.data.code}, message: ${response.data.message}`,
            });
        }

    })
    .catch(function (error: any) {
        console.log(error);
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            Object.assign(result, { 
                'status': false,
                'message': `[UploadAPK] The request was made and the server responded with a status code: ${error.response.status}`
            });
          } else if (error.request) {
            // The request was made but no response was received
            // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
            // http.ClientRequest in node.js
            Object.assign(result, { 
                'status': false,
                'message': `[UploadAPK] The request was made but no response was received`
            });
          } else {
            // Something happened in setting up the request that triggered an Error
            Object.assign(result, { 
                'status': false,
                'message': `[UploadAPK] Something happened in setting up the request that triggered an Error`
            });
          }
    });
    
    return result
}


async function generateDeviceGroupAccessKey(HydraLabAPIConfig: any, deviceIdentifier: string): Promise<object> {
    const axios = require('axios').default;

    let result: object = { 'status': false };

    let centerInfo: any = await getCenterInfo(HydraLabAPIConfig);
    if (centerInfo.status) {
        Object.assign(result, { 'centerInfo': centerInfo.centerInfo });
    } else {
        Object.assign(result, { 
            'status': false,
            'message': `[GetCenterInfo] Center is not alive, message: ${centerInfo.message}`
        });
        return result
    }

    await axios({
        method: "get",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.generateDeviceGroupAccessKey + deviceIdentifier,
        headers: { "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    })
    .then(function (response: any) {
        if (response.data.code == 200){
            Object.assign(result, { 
                'status': true,
                'message': `[GenerateDeviceGroupAccessKey] The request was made and the server responded with a status code: ${response.status}`,
                'accessKey': response.data.content.key
            });
        } else {
            Object.assign(result, { 
                'status': false,
                'message': `[GenerateDeviceGroupAccessKey] Error Code from center: ${response.data.code}, message: ${response.data.message}`,
            });
        }
    })
    .catch(function (error: any) {
        console.log(error);
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            Object.assign(result, { 
                'status': false,
                'message': `[GenerateDeviceGroupAccessKey] The request was made and the server responded with a status code: ${error.response.status}`
            });
          } else if (error.request) {
            // The request was made but no response was received
            // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
            // http.ClientRequest in node.js
            Object.assign(result, { 
                'status': false,
                'message': `[GenerateDeviceGroupAccessKey] The request was made but no response was received`
            });
          } else {
            // Something happened in setting up the request that triggered an Error
            Object.assign(result, { 
                'status': false,
                'message': `[GenerateDeviceGroupAccessKey] Something happened in setting up the request that triggered an Error`
            });
          }
    });

    return result;
}

async function triggerTestRun(HydraLabAPIConfig: any, runningType: string, deviceIdentifier: string, apkSetId: string, testSuiteClass: string | undefined, timeoutSec: number, maxStepCount: number | undefined, deviceTestCount: number | undefined, pipelineLink: string | undefined, accessKey: string | undefined, frameworkType: string | undefined, reportAudience?: string, instrumentationArgs?: object): Promise<object> {
    const axios = require('axios').default;
    let json: object = {};

    Object.assign(json, { 'runningType': runningType });
    Object.assign(json, { "deviceIdentifier": deviceIdentifier });
    if (HydraLabAPIConfig.hasOwnProperty('groupTestType')) {
        Object.assign(json, { "groupTestType": HydraLabAPIConfig.groupTestType });
    }
    if (HydraLabAPIConfig.hasOwnProperty('build_reason')) {
        Object.assign(json, { "type": HydraLabAPIConfig.build_reason });
    }
    Object.assign(json, { 'apkSetId': apkSetId });
    if (HydraLabAPIConfig.hasOwnProperty('pkgName')) {
        Object.assign(json, { "pkgName": HydraLabAPIConfig.pkgName });
    }
    if (HydraLabAPIConfig.hasOwnProperty('testPkgName')) {
        Object.assign(json, { "testPkgName": HydraLabAPIConfig.testPkgName });
    }
    if (testSuiteClass) {
        Object.assign(json, { 'testSuiteClass': testSuiteClass });
    }
    if (instrumentationArgs) {
        Object.assign(json, { "instrumentationArgs": instrumentationArgs });
    }
    if (maxStepCount) {
        Object.assign(json, { "maxStepCount": maxStepCount });
    }
    if (deviceTestCount) {
        Object.assign(json, { "deviceTestCount": deviceTestCount });
    }
    Object.assign(json, { 'pipelineLink': pipelineLink });
    if (accessKey) {
        Object.assign(json, { "accessKey": accessKey });
    }
    if (timeoutSec) {
        Object.assign(json, { "testTimeOutSec": timeoutSec });
    }
    if (frameworkType) {
        Object.assign(json, { 'frameworkType': frameworkType });
    }
    else {
        Object.assign(json, { 'frameworkType': 'JUnit4' });
    }


    console.log('##[section]TriggerTestRun Post Json: \n %s', JSON.stringify(json, null, 2));

    let result: object = { 'status': false };

    let centerInfo: any = await getCenterInfo(HydraLabAPIConfig);
    if (centerInfo.status) {
        Object.assign(result, { 'centerInfo': centerInfo.centerInfo });
    } else {
        Object.assign(result, { 
            'status': false,
            'message': `[GetCenterInfo] Center is not alive, message: ${centerInfo.message}`
        });
        return result
    }
    
    await axios({
        method: "post",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.runTestAPIPath,
        data: json,
        headers: { "Content-Type": 'application/json; ; charset=utf-8', "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    })
    .then(function (response: any) {
        if (response.data.code == 200){
            Object.assign(result, { 
                'status': true,
                'message': `[TriggerTestRun] The request was made and the server responded with a status code: ${response.status}`,
                'testTaskId': response.data.content.testTaskId
            });
        } else {
            Object.assign(result, { 
                'status': false,
                'message': `[TriggerTestRun] Error code from center: ${response.data.code}, message: ${response.data.message}`,
            });
        }
    })
    .catch(function (error: any) {
        console.log(error);
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            Object.assign(result, { 
                'status': false,
                'message': `[TriggerTestRun] The request was made and the server responded with a status code: ${error.response.status}`
            });
          } else if (error.request) {
            // The request was made but no response was received
            // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
            // http.ClientRequest in node.js
            Object.assign(result, { 
                'status': false,
                'message': `[TriggerTestRun] The request was made but no response was received`
            });
          } else {
            // Something happened in setting up the request that triggered an Error
            Object.assign(result, { 
                'status': false,
                'message': `[TriggerTestRun] Something happened in setting up the request that triggered an Error`
            });
          }
    });
    return result;
}

async function getTestStatus(HydraLabAPIConfig: any, testTaskId: string): Promise<object> {
    const axios = require('axios').default;

    let result: object = { 'status': false };

    let centerInfo: any = await getCenterInfo(HydraLabAPIConfig);
    if (centerInfo.status) {
        Object.assign(result, { 'centerInfo': centerInfo.centerInfo });
    } else {
        Object.assign(result, { 
            'status': false,
            'message': `[GetCenterInfo] Center is not alive, message: ${centerInfo.message}`
        });
        return result
    }

    await axios({
        method: "get",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.testStatusAPIPath + testTaskId,
        headers: { "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    })
    .then(function (response: any) {
        if (response.data.code == 200){
            Object.assign(result, { 
                'status': true,
                'message': `[GetTestStatus] The request was made and the server responded with a status code: ${response.status}`,
                'TestTask': response.data.content
            });
        } else {
            Object.assign(result, { 
                'status': false,
                'message': `[GetTestStatus] Error Code from center: ${response.data.code}, message: ${response.data.message}`,
            });
        }
    })
    .catch(function (error: any) {
        console.log(error);
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            Object.assign(result, { 
                'status': false,
                'message': `[GetTestStatus]The request was made and the server responded with a status code: ${error.response.status}`
            });
          } else if (error.request) {
            // The request was made but no response was received
            // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
            // http.ClientRequest in node.js
            Object.assign(result, { 
                'status': false,
                'message': `[GetTestStatus]The request was made but no response was received`
            });
          } else {
            // Something happened in setting up the request that triggered an Error
            Object.assign(result, { 
                'status': false,
                'message': `[GetTestStatus]Something happened in setting up the request that triggered an Error`
            });
          }
    });

    return result;
}

async function downloadToFile(url: string, fileName: string) {
    const axios = require('axios').default;

    await axios({
        method: 'get',
        url: url,
        responseType: 'stream'
    })
    .then(async function (response: any) {
        await response.data.pipe(fs.createWriteStream(fileName))
    });
}

function delay(ms: number) {
    return new Promise( resolve => setTimeout(resolve, ms) );
}


run();