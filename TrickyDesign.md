# Tricky Design

## 1. Pull file repeatedly to make sure the video is intact
com.microsoft.hydralab.common.screen.PhoneAppScreenRecorder--finishRecording()--line 119

## 2. Should analysis inputStream and record time tag when running smart test 
com.microsoft.hydralab.agent.runner.smart.SmartTestLog

## 3. The video tag logic is a huge pain and used a lot of tricky designs and the schema is hard to parse and understand.

We'd better create a new POJO to wrap the info.
Related references:
- com.microsoft.hydralab.agent.runner.espresso.testStarted
- com.microsoft.hydralab.common.entity.common.TestRun.addNewTimeTag
- com.microsoft.hydralab.center.controller.TestDetailController.deviceTaskInfo
- react/src/component/VideoNavView.jsx:110