# HydraLab API Reference

Quick reference for all commonly used HydraLab REST API endpoints with `curl` examples for both Android and iOS platforms.

> **Base URL:** `http://localhost:9886` (default local deployment)

---

## 1. Health Check

```bash
curl -s "http://localhost:9886/api/center/isAlive" | python3 -m json.tool
```

---

## 2. Device Management

### 2.1 List All Devices

Returns all connected agents and their devices.

```bash
curl -s "http://localhost:9886/api/device/list" | python3 -m json.tool
```

### 2.2 List Online Devices (filtered by platform)

**Android:**
```bash
curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for agent in data.get('content', []):
    for d in agent.get('devices', []):
        if d.get('type') == 'ANDROID':
            print(f\"serial={d['serialNum']}  status={d['status']}  name={d['name']}  model={d['model']}\")"
```

**iOS:**
```bash
curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for agent in data.get('content', []):
    for d in agent.get('devices', []):
        if d.get('type') == 'IOS':
            print(f\"serial={d['serialNum']}  status={d['status']}  name={d['name']}  model={d['model']}\")"
```

### 2.3 List Runnable Devices/Groups

Returns devices, groups, and Appium agents available for testing.

```bash
curl -s "http://localhost:9886/api/device/runnable" | python3 -m json.tool
```

### 2.4 Update Device Scope (Private/Public)

```bash
curl -s -X POST "http://localhost:9886/api/device/updateDeviceScope" \
  -d "deviceSerial=<SERIAL_NUM>&isPrivate=false"
```

---

## 3. Package Management

### 3.1 Upload Package (App Only)

```bash
curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@/path/to/app.apk" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -m json.tool
```

### 3.2 Upload Package (App + Test APK) — Android

```bash
curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@/path/to/app.apk" \
  -F "testAppFile=@/path/to/app-androidTest.apk" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -m json.tool
```

### 3.3 Upload Package — iOS

```bash
curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@/path/to/test_bundle.zip" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -m json.tool
```

### 3.4 Get File Set ID from Upload Response

```bash
FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@/path/to/app.apk" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))")
echo "File Set ID: $FILE_SET_ID"
```

### 3.5 Get File Set Info

```bash
curl -s "http://localhost:9886/api/package/<FILE_SET_ID>" | python3 -m json.tool
```

### 3.6 List File Sets (Paginated)

```bash
curl -s -X POST "http://localhost:9886/api/package/list" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"pageSize":10}' | python3 -m json.tool
```

---

## 4. Test Task Execution

### 4.1 Run Android Instrumentation Test

> **Important:** Use `serialNum` (not `deviceId`) as the `deviceIdentifier`. Check via the device list API.

```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId": "<FILE_SET_ID>",
    "deviceIdentifier": "<SERIAL_NUM>",
    "runningType": "INSTRUMENTATION",
    "pkgName": "com.example.app",
    "testPkgName": "com.example.app.test",
    "testRunnerName": "androidx.test.runner.AndroidJUnitRunner",
    "testScope": "TEST_APP",
    "testTimeOutSec": 2700,
    "frameworkType": "JUNIT4",
    "disableRecording": false,
    "needUninstall": true
  }' | python3 -m json.tool
```

### 4.2 Run Android Test with File Pull (directories-to-pull equivalent)

Pulls `/sdcard/Documents/` from the device into the test result folder after completion.

```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId": "<FILE_SET_ID>",
    "deviceIdentifier": "<SERIAL_NUM>",
    "runningType": "INSTRUMENTATION",
    "pkgName": "com.example.app",
    "testPkgName": "com.example.app.test",
    "testRunnerName": "androidx.test.runner.AndroidJUnitRunner",
    "testScope": "CLASS",
    "testSuiteClass": "com.example.MainActivityTest",
    "testTimeOutSec": 2700,
    "frameworkType": "JUNIT4",
    "disableRecording": false,
    "needUninstall": true,
    "deviceActions": {
      "tearDown": [
        {
          "deviceType": "Android",
          "method": "pullFileFromDevice",
          "args": ["/sdcard/Documents/"]
        }
      ]
    }
  }' | python3 -m json.tool
```

### 4.3 Run iOS XCTest

```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId": "<FILE_SET_ID>",
    "deviceIdentifier": "<DEVICE_UDID>",
    "runningType": "XCTEST",
    "pkgName": "com.example.app",
    "testScope": "TEST_APP",
    "testTimeOutSec": 1800,
    "frameworkType": "XCTest",
    "disableRecording": false
  }' | python3 -m json.tool
```

### 4.4 Run iOS XCTest with File Pull (directories-to-pull equivalent)

Pulls app container `/Documents/` into the test result folder after completion.
Supports two argument formats:
- `bundleId:/path` — explicit bundle ID and path (recommended)
- `/path` — uses the task's `pkgName` as the bundle ID

```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId": "<FILE_SET_ID>",
    "deviceIdentifier": "<DEVICE_UDID>",
    "runningType": "XCTEST",
    "pkgName": "com.example.app",
    "testScope": "TEST_APP",
    "testTimeOutSec": 1800,
    "frameworkType": "XCTest",
    "disableRecording": false,
    "deviceActions": {
      "tearDown": [
        {
          "deviceType": "IOS",
          "method": "pullFileFromDevice",
          "args": ["com.example.app:/Documents/"]
        }
      ]
    }
  }' | python3 -m json.tool
```

### 4.5 Run Appium Test (Cross-Platform)

```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId": "<FILE_SET_ID>",
    "deviceIdentifier": "<SERIAL_NUM>",
    "runningType": "APPIUM",
    "pkgName": "com.example.app",
    "testScope": "TEST_APP",
    "testTimeOutSec": 1800,
    "frameworkType": "JUnit5",
    "disableRecording": false
  }' | python3 -m json.tool
```

### 4.6 Run Monkey Test — Android

```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId": "<FILE_SET_ID>",
    "deviceIdentifier": "<SERIAL_NUM>",
    "runningType": "MONKEY",
    "pkgName": "com.example.app",
    "testScope": "TEST_APP",
    "testTimeOutSec": 600,
    "maxStepCount": 500,
    "disableRecording": false
  }' | python3 -m json.tool
```

---

## 5. Test Task Monitoring

### 5.1 Get Task Status

```bash
curl -s "http://localhost:9886/api/test/task/<TASK_ID>" | python3 -m json.tool
```

### 5.2 Get Task Queue

```bash
curl -s "http://localhost:9886/api/test/task/queue" | python3 -m json.tool
```

### 5.3 List Tasks (Paginated)

```bash
curl -s -X POST "http://localhost:9886/api/test/task/list" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"pageSize":10}' | python3 -m json.tool
```

### 5.4 Cancel a Running/Queued Task

```bash
curl -s "http://localhost:9886/api/test/task/cancel/<TASK_ID>?reason=manual+cancel" | python3 -m json.tool
```

---

## 6. Test Results

### 6.1 Get Test Run Details

```bash
curl -s "http://localhost:9886/api/test/task/device/<DEVICE_TASK_ID>" | python3 -m json.tool
```

### 6.2 Get Test Case Detail

```bash
curl -s "http://localhost:9886/api/test/case/<TEST_CASE_ID>" | python3 -m json.tool
```

### 6.3 Get Crash Stack

```bash
curl -s "http://localhost:9886/api/test/crash/<CRASH_ID>" | python3 -m json.tool
```

### 6.4 Get Test Video

```bash
curl -s "http://localhost:9886/api/test/videos/<RESULT_ID>" | python3 -m json.tool
```

---

## 7. Device Actions Reference

Device actions can be attached to test tasks via the `deviceActions` field. They run at `setUp` (before test) or `tearDown` (after test).

### Supported Methods

| Method                  | Platforms      | Args                                          | Description                                    |
|-------------------------|----------------|-----------------------------------------------|------------------------------------------------|
| `setProperty`           | Android        | `[property, value]`                            | Set a system property                          |
| `setDefaultLauncher`    | Android        | `[packageName, defaultActivity]`               | Set default launcher app                       |
| `backToHome`            | Android, iOS   | `[]`                                           | Navigate to home screen                        |
| `changeGlobalSetting`   | Android        | `[setting, value]`                             | Change global settings                         |
| `changeSystemSetting`   | Android        | `[setting, value]`                             | Change system settings                         |
| `execCommandOnDevice`   | Android        | `[command]`                                    | Run shell command on device                    |
| `execCommandOnAgent`    | Android, iOS   | `[command]`                                    | Run shell command on agent host                |
| `pushFileToDevice`      | Android        | `[pathOnAgent, pathOnDevice]`                  | Push file from agent to device                 |
| `pullFileFromDevice`    | Android, iOS   | `[pathOnDevice]`                               | Pull files from device to result folder        |
| `addToBatteryWhiteList` | Android        | `[packageName]`                                | Add app to battery whitelist                   |

### pullFileFromDevice — Platform Differences

**Android:** `pathOnDevice` is an absolute path on the device filesystem.
```json
{"method": "pullFileFromDevice", "args": ["/sdcard/Documents/"]}
```

**iOS:** `pathOnDevice` uses `bundleId:/path` format to access the app's sandboxed container.
```json
{"method": "pullFileFromDevice", "args": ["com.example.app:/Documents/"]}
```

### Example: setUp + tearDown Actions

```json
{
  "deviceActions": {
    "setUp": [
      {
        "deviceType": "Android",
        "method": "addToBatteryWhiteList",
        "args": ["com.example.app"]
      }
    ],
    "tearDown": [
      {
        "deviceType": "Android",
        "method": "pullFileFromDevice",
        "args": ["/sdcard/Documents/"]
      }
    ]
  }
}
```

---

## 8. Complete Workflow Examples

### 8.1 Android: Upload + Run + Pull Artifacts

```bash
# Upload both APKs
FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@app.apk" \
  -F "testAppFile=@app-androidTest.apk" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))") && \
echo "File Set ID: $FILE_SET_ID" && \

# Find first online Android device
DEVICE_SERIAL=$(curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys,json
for a in json.load(sys.stdin).get('content',[]):
  for d in a.get('devices',[]):
    if d.get('type')=='ANDROID' and d.get('status')=='ONLINE':
      print(d.get('serialNum')); exit(0)") && \
echo "Device: $DEVICE_SERIAL" && \

# Run test with file pull
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId":"'"$FILE_SET_ID"'",
    "deviceIdentifier":"'"$DEVICE_SERIAL"'",
    "runningType":"INSTRUMENTATION",
    "pkgName":"com.example.app",
    "testPkgName":"com.example.app.test",
    "testRunnerName":"androidx.test.runner.AndroidJUnitRunner",
    "testScope":"TEST_APP",
    "testTimeOutSec":2700,
    "frameworkType":"JUNIT4",
    "disableRecording":false,
    "needUninstall":true,
    "deviceActions":{
      "tearDown":[{"deviceType":"Android","method":"pullFileFromDevice","args":["/sdcard/Documents/"]}]
    }
  }' | python3 -m json.tool
```

### 8.2 iOS: Upload + Run + Pull Artifacts

```bash
# Upload test bundle
FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@test_bundle.zip" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))") && \
echo "File Set ID: $FILE_SET_ID" && \

# Find first online iOS device
DEVICE_UDID=$(curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys,json
for a in json.load(sys.stdin).get('content',[]):
  for d in a.get('devices',[]):
    if d.get('type')=='IOS' and d.get('status')=='ONLINE':
      print(d.get('serialNum')); exit(0)") && \
echo "Device: $DEVICE_UDID" && \

# Run XCTest with file pull
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId":"'"$FILE_SET_ID"'",
    "deviceIdentifier":"'"$DEVICE_UDID"'",
    "runningType":"XCTEST",
    "pkgName":"com.example.app",
    "testScope":"TEST_APP",
    "testTimeOutSec":1800,
    "frameworkType":"XCTest",
    "disableRecording":false,
    "deviceActions":{
      "tearDown":[{"deviceType":"IOS","method":"pullFileFromDevice","args":["com.example.app:/Documents/"]}]
    }
  }' | python3 -m json.tool
```

---

## Notes

- **`deviceIdentifier`** must be the device's `serialNum` (not `deviceId`). The center server looks up devices by `serialNum` in its internal map. Use the device list API to find the correct value.
- **iOS `pullFileFromDevice`** uses `pymobiledevice3 apps pull` under the hood, which accesses the app's sandboxed container via the AFC (Apple File Conduit) protocol.
- **Android `pullFileFromDevice`** uses `adb pull` and can access any path on the device filesystem.
- All pulled files are saved into the test run's result directory and included in the uploaded test artifacts.
- **Subfolder organization**: Pulled files are placed in a subfolder named after the last component of the remote path (e.g. `/Documents/` → `Documents/`, `/Library/Caches/` → `Caches/`). If the remote path is just `/`, the subfolder defaults to `pulled_files/`. This keeps pulled files separate from HydraLab-generated artifacts (crash reports, videos, logs) in the result folder.
