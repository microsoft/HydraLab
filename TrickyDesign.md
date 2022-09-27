# Tricky Design

## 1. Pull file repeatedly to make sure the video is intact
com.microsoft.launcher.devices.screen.PhoneAppScreenRecorder--finishRecording()--line 119

## 2. Should analysis inputStream and record time tag when running smart test 
com.microsoft.launcher.devices.runner.smart.SmartTestLog

## 3. The video tag logic is a huge pain and used a lot of tricky designs and the schema is hard to parse and understand.

We'd better create a new POJO to wrap the info.
Related references:
- com.microsoft.launcher.devices.runner.espresso.EspressoListener.testStarted
- com.microsoft.launcher.devices.entity.common.DeviceTestTask.addNewTimeTag
- com.microsoft.devices.network.center.controller.TestDetailController.deviceTaskInfo
- react/src/component/VideoNavView.jsx:110