// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

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
            "addAttachmentAPIPath": "/api/package/addAttachment",
            'generateDeviceGroupAccessKey': "/api/deviceGroup/generate?deviceIdentifier=",
            'runTestAPIPath': "/api/test/task/run/",
            'testStatusAPIPath': "/api/test/task/",
            'testPortalTaskInfoPath': "/portal/index.html?redirectUrl=/info/task/",
            'testPortalTaskDeviceVideoPath': "/portal/index.html?redirectUrl=/info/videos/"
        };

        Object.assign(HydraLabAPIConfig, { 'Path': Path });

        const serviceEndpointId: string = getRequiredInput('serviceEndpoint');
        
        const serviceEndpointUrl: string | undefined = tl.getEndpointUrl(serviceEndpointId, true);
        if (!serviceEndpointUrl) {
            throw new Error('EndpointUrl Not Found');
        }
        Object.assign(HydraLabAPIConfig, { 'serviceEndpointUrl': serviceEndpointUrl });

        const serviceEndpoint: tl.EndpointAuthorization | undefined = tl.getEndpointAuthorization(serviceEndpointId, true);
        if (!serviceEndpoint) {
            throw new Error('Endpoint Not Found');
        }
        const authToken: string = serviceEndpoint.parameters['apitoken'].replace(/\\n/g, '\n');
        Object.assign(HydraLabAPIConfig, { 'authToken': authToken });

        let teamName: string = getRequiredInput('teamName')

        let runningType: string = getRequiredInput('runningType')

        const apkPath: string | undefined = tl.getPathInput('pkgPath', true, true);
        if (!apkPath) {
            throw new Error('Package Path is not set');
        }
        console.log('##[section]apkPath: %s', apkPath);

        const buildFlavor: string | undefined = tl.getInput('buildFlavor', false) ? tl.getInput('buildFlavor', false) : 'UNKNOWN';
        console.log('##[section]buildFlavor: %s', buildFlavor);

        let testApkPath: string | undefined = undefined
        if (runningType === 'INSTRUMENTATION' || runningType === 'APPIUM' || runningType === 'APPIUM_CROSS' || runningType === 'T2C_JSON') {
            const testApkPathInput: string | undefined = tl.getPathInput('testPkgPath', true, true);
            if (!testApkPathInput) {
                throw new Error('Test Package Path is not set');
            }
            testApkPath = testApkPathInput    
        }
        console.log('##[section]testApkPath: %s', testApkPath);

        let frameworkType: string | undefined = undefined
        if (runningType === 'APPIUM' || runningType === 'APPIUM_CROSS') {
            frameworkType = tl.getInput('frameworkType', true);
            if (!frameworkType) {
                throw new Error('Frame Work Type is not set');
            }
            Object.assign(HydraLabAPIConfig, { 'frameworkType': frameworkType });
        }

        const pkgName: string = getRequiredInput('pkgName');
        Object.assign(HydraLabAPIConfig, { 'pkgName': pkgName });

        if (runningType === 'INSTRUMENTATION') {
            const testPkgName: string = getRequiredInput('testPkgName');
            Object.assign(HydraLabAPIConfig, { 'testPkgName': testPkgName });
        }

        let testSuiteClass: string | undefined = undefined;
        if (runningType === 'INSTRUMENTATION' || runningType === 'APPIUM' || runningType === 'APPIUM_CROSS') {
            testSuiteClass = getRequiredInput('testSuiteClass');
        }

        let attachmentsInput: string | undefined = tl.getInput('attachmentsInfo', false);
        let attachments: any = undefined;
        if (attachmentsInput && attachmentsInput.length > 0) {
            try {
                attachments = JSON.parse(attachmentsInput)
            }
            catch {
                console.log('##[section]AttachmentsInfo Input: %s', attachmentsInput);
                throw new Error('AttachmentsInfo not a Json string'); 
            }
        }
        console.log('##[section]AttachmentsInfo: %s', JSON.stringify(attachments));

        let maxStepCount: number | undefined = undefined;
        if (runningType === 'SMART' || runningType === 'APPIUM_MONKEY') {
            const maxStepCountInput: string | undefined = tl.getInput('maxStepCount', false);
            if (maxStepCountInput) {
                maxStepCount = Number(maxStepCountInput);
                console.log('##[section]maxStepCount: %s', maxStepCount);
            }
            else if (runningType === 'SMART') {
                maxStepCount = 100
                console.log('##[section]maxStepCount: %s(default)', maxStepCount);
            }
        }

        let deviceTestCount: number | undefined = undefined;
        if (runningType === 'SMART') {
            const deviceTestCountInput: string = getRequiredInput('deviceTestCount');
            deviceTestCount = Number(deviceTestCountInput);
        }

        const deviceIdentifier: string = getRequiredInput('deviceIdentifier');

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

        const timeoutSecInput: string = getRequiredInput('timeoutSec');
        const timeoutSec: number = Number(timeoutSecInput);
        
        let needUninstall: boolean | undefined = undefined;
        let needClearData: boolean | undefined = undefined;

        if (runningType === 'APPIUM_CROSS' || runningType === 'T2C_JSON') {
            needUninstall = tl.getBoolInput('needUninstall', true);
            console.log('##[section]needUninstall: %s', needUninstall);
            needClearData = tl.getBoolInput('needClearData', true);
            console.log('##[section]needClearData: %s', needClearData);
        }

        const instrumentationArgsInput: string[] | undefined = tl.getDelimitedInput('instrumentationArgs', ';', false);
        let instrumentationArgs: object = {};
        if (instrumentationArgsInput && instrumentationArgsInput.length > 0) {
            for (const instrumentationArg of instrumentationArgsInput) {
                const instrumentationArgSplit: string[] = instrumentationArg.replace(/\s+/g,'').split('=');
                if (instrumentationArgSplit.length === 2) {
                    Object.assign(instrumentationArgs, JSON.parse(`{"${instrumentationArgSplit[0]}": "${instrumentationArgSplit[1]}"}`))
                } else {
                    throw new Error('Illegal Instrumentation Args');
                }
            } 
        }
        console.log('##[section]instrumentationArgs: %s', JSON.stringify(instrumentationArgs));

        let runningInfo: string = getRequiredInput('runningInfo');

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

        const reportFolderPath: string = path.join(dir, `report`, runningType);
        console.log('##[section]reportFolderPath: %s', reportFolderPath);

        fs.mkdir(reportFolderPath, { recursive: true }, (err) => {
            if (err) throw err;
            console.log('##[section]Report Folder created successfully!'); 
        });

        const fileSetId: any = await uploadAPP(HydraLabAPIConfig, teamName, commitId, commitCount, commitMsg, buildFlavor, apkPath, testApkPath);
        console.log(`##[section]Uploaded APK set id: ${fileSetId}`)

        await uploadAttachments(HydraLabAPIConfig, fileSetId, attachments)
        
        let accessKey = undefined
        try {
            accessKey = await generateDeviceGroupAccessKey(HydraLabAPIConfig, deviceIdentifier);
            console.log('##[section]Generate Group AccessKey Successfully!')
        }
        catch {
            console.log('##[section]Generate Group AccessKey Failed!')
        }

        let testTaskId: any = await triggerTestRun(HydraLabAPIConfig, runningType, deviceIdentifier, fileSetId, testSuiteClass, timeoutSec, maxStepCount, deviceTestCount, pipelineLink, accessKey, frameworkType, reportAudience, instrumentationArgs, needUninstall, needClearData)
        console.log(`##[section]Triggered test task id: ${testTaskId} successful!`);
        
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

            runningTest = await getTestStatus(HydraLabAPIConfig, testTaskId);
            // console.log("Current running test info: %s", JSON.stringify(runningTest, null, 2));

            if (HydraRetryTime != runningTest.retryTime) {
                HydraRetryTime = runningTest.retryTime;
                console.log("##[command]Retrying to run task again, waitSecond will be reset. current retryTime is : %d", HydraRetryTime);
                totalWaitSecond = 0;
                sleepSecond = timeoutSec / 3;
            }

            if (runningTest.status === 'waiting') {
                console.log(`##[command]Start waiting: 30 seconds, ${runningTest.message}`);
                await delay(30000);
            } else {
                console.log(`##[command]Running test on ${runningTest.testDevicesCount} devices, status for now: ${runningTest.status}`);
                if (runningTest.status === 'canceled') {
                    throw new Error(`The test task is canceled`);
                }
                if (runningTest.status === 'error') {
                    throw new Error(`The test task is error`);
                }
                finished = (runningTest.status === 'finished');

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
            throw new Error(`Time out after waiting for ${timeoutSec} seconds! Test id ${runningTest.id}`);
        }

        if (!runningTest.deviceTestResults) {
            throw new Error(`No deviceTestResults! Test id ${runningTest.id}`);
        }

        const testReportUrl: string = HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.testPortalTaskInfoPath + runningTest.id;

        const StringBuilder = require("string-builder");
        const mdBuilder: any = new StringBuilder();

        mdBuilder.append("# Hydra Lab Test Result Details\n\n\n");
        mdBuilder.appendFormat("### [Link to full report]({0})\n\n\n", testReportUrl);
        mdBuilder.appendFormat("### Statistic: total test case count: {0}, failed: {1}\n\n", runningTest.totalTestCount, runningTest.totalFailCount);
        if (runningTest.totalFailCount > 0 && runningTest.reportImagePath != null) {
            console.log("##[warning] %d cases failed during the test", runningTest.totalFailCount);
        }

        if (runningTest.totalFailCount > 0 || runningTest.totalTestCount == 0) {
            taskSuccess = false;
        }

        console.log("##vso[task.setprogress value=90;]Almost Done with testing");

        for (let index in runningTest.deviceTestResults) {
            let deviceSerialNumber: string = runningTest.deviceTestResults[index].deviceSerialNumber
            console.log(">>>>>>\n Device %s, failed cases count: %d, total cases: %d", deviceSerialNumber, runningTest.deviceTestResults[index].failCount, runningTest.deviceTestResults[index].totalCount);
            if (runningTest.deviceTestResults[index].failCount > 0 || runningTest.deviceTestResults[index].totalCount == 0) {
                if (runningTest.deviceTestResults[index].crashStack != null && runningTest.deviceTestResults[index].crashStack.length > 0) {
                    console.log("##[error]Fatal error during test on device %s, stack:\n%s", deviceSerialNumber, runningTest.deviceTestResults[index].crashStack);
                } else {
                    console.log("##[error]Fatal error during test on device %s, crash stack is null.", deviceSerialNumber);
                }
                taskSuccess = false;
            }

            mdBuilder.appendFormat("- On device {0} (SN: {1}), total case count: {2}, failed: {3}\n", runningTest.deviceTestResults[index].deviceName, deviceSerialNumber, runningTest.deviceTestResults[index].totalCount, runningTest.deviceTestResults[index].failCount)
            
            if (runningTest.deviceTestResults[index].attachments.length == 0) {
                continue;
            }

            fs.mkdir(path.join(reportFolderPath, deviceSerialNumber), { recursive: true }, (err) => {
                if (err) throw err;
                console.log('##[section]Report Subfolder created successfully!'); 
            });

            for (let attachmentIndex in runningTest.deviceTestResults[index].attachments) {
                let attachmentUrl: string = runningTest.deviceTestResults[index].attachments[attachmentIndex].blobUrl;
                let attachmentFileName: string = runningTest.deviceTestResults[index].attachments[attachmentIndex].fileName;
                let attachmentPath: string = path.join(reportFolderPath, deviceSerialNumber, attachmentFileName);

                console.log("Start downloading attachment for device: %s, device name: %s, filename: %s, link: %s", deviceSerialNumber, runningTest.deviceTestResults[index].deviceName, attachmentFileName, attachmentUrl);
                await downloadToFile(attachmentUrl, attachmentPath);
                console.log("Finish downloading attachment for device %s", deviceSerialNumber);
            }
        }

        // use the https://docs.microsoft.com/en-us/azure/devops/pipelines/scripts/logging-commands?view=azure-devops&tabs=powershell#build-commands
        // to upload the report
        console.log("##vso[artifact.upload artifactname=testResult;]%s", path.resolve(reportFolderPath));

        console.log("##[section]All done, overall failed cases count: %d, total count: %d, devices count: %d", runningTest.totalFailCount, runningTest.totalTestCount, runningTest.testDevicesCount);
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

async function uploadAPP(HydraLabAPIConfig: any, teamName: string, commitId: string | undefined, commitCount: string | undefined, commitMsg: string | undefined, buildFlavor: string | undefined, apkPath: string, testApkPath: string | undefined): Promise<object> {
    const form = new FormData();
    
    form.append('teamName', teamName);
    form.append('commitId', commitId);
    form.append('commitCount', commitCount);
    form.append('commitMessage', commitMsg);
    form.append('buildType', buildFlavor);
    form.append('appFile', fs.createReadStream(apkPath), { filename: path.basename(apkPath), contentType: 'application/vnd.android.package-archive' });
    if (testApkPath) {
        form.append('testAppFile', fs.createReadStream(testApkPath), { filename: path.basename(testApkPath), contentType: 'application/vnd.android.package-archive' });
    }

    let requestParameters = {
            method: "post",
            url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.uploadAPKAPIPath,
            data: form,
            maxContentLength: Infinity,
            maxBodyLength: Infinity,        
            headers: { "Content-Type": `multipart/form-data; boundary=${form.getBoundary()}`, "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    }

    let responseContent: any =  await requestHydraLabAfterCheckCenterAlive(HydraLabAPIConfig, 'UploadPackage', requestParameters);

    return responseContent.id
}

async function uploadAttachments(HydraLabAPIConfig: any, fileSetId: string, attachments: any[]): Promise<void> {
    for (let attachment of attachments) {
        let form = new FormData();

        if (!fs.existsSync(attachment.filePath)) {
            throw new Error(`No such file ${attachment.filePath}`)
        }

        console.log(`##[section]Adding attachment: ${attachment.fileName}`)
    
        form.append('fileSetId', fileSetId);
        form.append('fileType', attachment.fileType);
        form.append('loadType', attachment.loadType);
        form.append('loadDir', attachment.loadDir);
        form.append('attachment', fs.createReadStream(attachment.filePath), { filename: path.basename(attachment.filePath), contentType: 'application/vnd.android.package-archive' });

        let requestParameters = {
            method: "post",
            url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.addAttachmentAPIPath,
            data: form,
            maxContentLength: Infinity,
            maxBodyLength: Infinity,        
            headers: { "Content-Type": `multipart/form-data; boundary=${form.getBoundary()}`, "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
        }

        let responseContent: any = await requestHydraLabAfterCheckCenterAlive(HydraLabAPIConfig, 'AddAttachment', requestParameters);

        console.log(`##[section]Add Attachment Success. Current Attachment Count: ${responseContent.attachments.length}`)
    }
}

async function generateDeviceGroupAccessKey(HydraLabAPIConfig: any, deviceIdentifier: string): Promise<object> {
    let requestParameters = {
        method: "get",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.generateDeviceGroupAccessKey + deviceIdentifier,
        headers: { "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    }

    let responseContent: any =  await requestHydraLabAfterCheckCenterAlive(HydraLabAPIConfig, 'GenerateDeviceGroupAccessKey', requestParameters);

    return responseContent.key
}

async function triggerTestRun(HydraLabAPIConfig: any, runningType: string, deviceIdentifier: string, fileSetId: string, testSuiteClass: string | undefined, timeoutSec: number, maxStepCount: number | undefined, deviceTestCount: number | undefined, pipelineLink: string | undefined, accessKey: any | undefined, frameworkType: string | undefined, reportAudience?: string, instrumentationArgs?: object, needUninstall?: boolean, needClearData?: boolean): Promise<object> {
    let json: object = {};

    Object.assign(json, { 'runningType': runningType });
    Object.assign(json, { "deviceIdentifier": deviceIdentifier });
    if (HydraLabAPIConfig.hasOwnProperty('groupTestType')) {
        Object.assign(json, { "groupTestType": HydraLabAPIConfig.groupTestType });
    }
    if (HydraLabAPIConfig.hasOwnProperty('build_reason')) {
        Object.assign(json, { "type": HydraLabAPIConfig.build_reason });
    }
    Object.assign(json, { 'fileSetId': fileSetId });
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
    Object.assign(json, { "needUninstall": needUninstall });
    Object.assign(json, { "needClearData": needClearData });

    let requestParameters = {
        method: "post",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.runTestAPIPath,
        data: json,
        headers: { "Content-Type": 'application/json; ; charset=utf-8', "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    }

    let responseContent: any =  await requestHydraLabAfterCheckCenterAlive(HydraLabAPIConfig, 'TriggerTestRun', requestParameters);

    return responseContent.testTaskId
}

async function getTestStatus(HydraLabAPIConfig: any, testTaskId: string): Promise<object> {    
    let requestParameters = {
        method: "get",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.testStatusAPIPath + testTaskId,
        headers: { "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    }

    let responseContent: any =  await requestHydraLabAfterCheckCenterAlive(HydraLabAPIConfig, 'GetTestStatus', requestParameters);

    return responseContent
}

function getRequiredInput(name: string): string {
    let variable: string | undefined = tl.getInput(name, true);

    if (!variable) {
        throw new Error(`Input: ${name} is not set`);
    }

    console.log(`##[section]${name}: ${variable}`);

    return variable;
}

async function requestHydraLab(APIName: string, requestParameters: any): Promise<any> {
    const axios = require('axios').default;

    let responseContent = {
        "success": false,
        "content": undefined
    }

    await axios(requestParameters)
    .then(function (response: any) {
        if (response.data.code == 200){
            if (response.data.content) {
                Object.assign(responseContent, {
                    "success": true,
                    "content": response.data.content
                })    
            }
        } else {
            Object.assign(responseContent, {
                "success": false,
                "content": `[${APIName}] Error Code from center: ${response.data.code}, message: ${response.data.message}`
            })
        }
    })
    .catch(function (error: any) {
        // console.log(error);
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            throw new Error(`[${APIName}] The request was made and the server responded with a status code: ${error.response.status}, error: ${error}`);
        } else if (error.request) {
            // The request was made but no response was received
            // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
            // http.ClientRequest in node.js
            throw new Error(`[${APIName}] The request was made but no response was received, error: ${error}`);
        } else {
            // Something happened in setting up the request that triggered an Error
            throw new Error(`[${APIName}] Something happened in setting up the request that triggered an Error, error: ${error}`);
        }
    });

    if (!responseContent.success || !responseContent.content) {
        throw new Error(`${responseContent.content ? responseContent.content : "Response Content Undefined"}`);
    }

    return responseContent.content
}

async function requestHydraLabAfterCheckCenterAlive(HydraLabAPIConfig: any, APIName: string, requestParameters: any): Promise<object> {
    let checkCenterRequestParameters = {
        method: "get",
        url: HydraLabAPIConfig.serviceEndpointUrl + HydraLabAPIConfig.Path.getCenterInfo,
        headers: { "Authorization": `Bearer ${HydraLabAPIConfig['authToken']}` }
    }

    let centerInfo = await requestHydraLab('GetCenterInfo', checkCenterRequestParameters);

    console.log(`##[section][${APIName}] Center Version: ${centerInfo.version}`)

    let responseContent: any =  await requestHydraLab(APIName, requestParameters);

    return responseContent;
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