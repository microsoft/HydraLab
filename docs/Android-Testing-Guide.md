# Android Testing Guide for HydraLab

## Table of Contents
- [Prerequisites](#prerequisites)
- [Onboarding a New Android Device](#onboarding-a-new-android-device)
- [Running Android Tests](#running-android-tests)
- [Screen Recording](#screen-recording)
- [Pull Files from Device](#pull-files-from-device)
- [Device Actions Reference](#device-actions-reference)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)
- [Known Issues & Solutions](#known-issues--solutions)

---

## Prerequisites

### Required Tools
- **Android SDK**: `ANDROID_HOME` environment variable must be set
- **ADB**: Version 1.x or higher (located at `$ANDROID_HOME/platform-tools/adb`)
- **Java**: JDK 11 or higher (to run the HydraLab agent)
- **ffmpeg**: Required for video merging (multiple screen recording segments)

### Environment Variables
```bash
# Required
export ANDROID_HOME=/path/to/android/sdk

# Verify ADB is accessible
$ANDROID_HOME/platform-tools/adb version
```

### Device Requirements
- USB Debugging enabled (Settings → Developer options → USB debugging)
- Developer options unlocked (Settings → About phone → tap Build number 7 times)
- Device connected via USB
- USB debugging authorization accepted ("Allow USB debugging?" prompt)

### Verify Setup
```bash
# Check ADB sees the device
adb devices
# Expected: <serial>    device

# Check HydraLab agent is running
curl -s http://localhost:9886/api/center/isAlive | python3 -m json.tool

# Verify device appears in HydraLab
curl -s http://localhost:9886/api/device/list | python3 -c "
import sys, json
for agent in json.load(sys.stdin).get('content', []):
    for d in agent.get('devices', []):
        if d.get('type') == 'ANDROID':
            print(f\"serial={d['serialNum']}  status={d['status']}  name={d['name']}  model={d['model']}\")"
```

---

## Onboarding a New Android Device

### Step 1: Physical Setup
1. **Connect the device** via USB to the machine running HydraLab agent
2. **Unlock the device** and accept the USB debugging authorization prompt
3. For USB-C devices, prefer a direct connection (avoid hubs if possible)

### Step 2: Enable Developer Options & USB Debugging
```bash
# On the device:
# 1. Settings → About phone → Tap "Build number" 7 times
# 2. Settings → Developer options → Enable "USB debugging"
# 3. Settings → Developer options → (Optional) Disable "Verify apps over USB"
```

### Step 3: Verify ADB Connection
```bash
# List connected devices
adb devices

# Expected output:
# List of devices attached
# 0R15205I23100583    device

# Get device info
adb -s <SERIAL> shell getprop ro.product.model
adb -s <SERIAL> shell getprop ro.build.version.release
adb -s <SERIAL> shell getprop ro.build.version.sdk
```

### Step 4: Verify Device in HydraLab
```bash
# The agent auto-detects Android devices via ADB's IDeviceChangeListener
# No restart is needed — just check the device list:
curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys, json
for agent in json.load(sys.stdin).get('content', []):
    for d in agent.get('devices', []):
        if d.get('type') == 'ANDROID' and d.get('status') == 'ONLINE':
            print(f\"serial={d['serialNum']}  model={d['model']}  os={d['osVersion']}  sdk={d.get('osSDKInt','')}\")"
```

### Step 5: Test Device Functionality
```bash
# Take a screenshot
adb -s <SERIAL> exec-out screencap -p > /tmp/test_screenshot.png

# Check storage space
adb -s <SERIAL> shell df /data

# Test app install/uninstall
adb -s <SERIAL> install -t /path/to/test.apk
adb -s <SERIAL> uninstall com.example.app
```

### Device Onboarding Checklist

| Step                    | Verification                         | Expected Result                          |
|-------------------------|--------------------------------------|------------------------------------------|
| USB Connection          | `adb devices`                        | Device serial listed as "device"         |
| Developer Options       | Device settings                      | Developer options visible                |
| USB Debugging           | Device settings                      | Enabled, authorization accepted          |
| HydraLab Detection      | `/api/device/list`                   | Device status: ONLINE, type: ANDROID     |
| Screenshot              | `adb exec-out screencap -p`          | Image file created                       |
| Storage                 | `adb shell df /data`                 | Sufficient free space (>2GB recommended) |

---

## Running Android Tests

HydraLab supports multiple Android test types:

| Test Type            | Runner Class       | `runningType`       | Description                                    |
|----------------------|--------------------|---------------------|------------------------------------------------|
| Espresso/JUnit       | `EspressoRunner`   | `INSTRUMENTATION`   | Standard Android instrumentation tests         |
| Monkey               | `AdbMonkeyRunner`  | `MONKEY`            | Random UI stress testing                       |
| Appium               | `AppiumRunner`     | `APPIUM`            | Cross-platform Appium test automation          |
| Maestro              | `MaestroRunner`    | `MAESTRO`           | Maestro UI flow tests                          |

### 1. Prepare Test Package

#### For Instrumentation Tests (Espresso/JUnit)
You need two APKs:
```
app.apk              # The application under test
app-androidTest.apk  # The instrumentation test APK
```

Build them with Gradle:
```bash
./gradlew assembleDebug assembleDebugAndroidTest
```

#### For Monkey Tests
Only the app APK is needed — no separate test package.

### 2. Upload and Run Tests

#### 2.1 Upload Package
```bash
# Upload both APKs (app + test)
FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@app.apk" \
  -F "testAppFile=@app-androidTest.apk" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))")

echo "File Set ID: $FILE_SET_ID"
```

#### 2.2 Find Available Device
```bash
DEVICE_SERIAL=$(curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys, json
for a in json.load(sys.stdin).get('content', []):
    for d in a.get('devices', []):
        if d.get('type') == 'ANDROID' and d.get('status') == 'ONLINE':
            print(d.get('serialNum')); exit(0)")

echo "Device: $DEVICE_SERIAL"
```

#### 2.3 Run Instrumentation Test
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

#### 2.4 Run Monkey Test
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

### Test Scope Options

| Scope        | Value        | Description                                    | Extra Fields Required          |
|--------------|--------------|------------------------------------------------|--------------------------------|
| Full app     | `TEST_APP`   | Runs all test cases                            | None                           |
| Package      | `PACKAGE`    | Runs tests in a specific Java package          | `testSuiteClass` (package)     |
| Class        | `CLASS`      | Runs a specific test class                     | `testSuiteClass` (FQCN)       |

Example — run a single class:
```json
{
  "testScope": "CLASS",
  "testSuiteClass": "com.example.app.test.MainActivityTest"
}
```

### 3. Monitor Test Execution
```bash
# Check task status
TASK_ID="<from run response>"
curl -s "http://localhost:9886/api/test/task/$TASK_ID" | python3 -m json.tool

# View live agent logs
tail -f storage/logs/agent.log
```

### 4. Retrieve Results
Test results are stored in:
```
storage/test/result/YYYY/MM/DD/<timestamp>/<serial-num>/
├── Documents/              ← pulled files (if pullFileFromDevice configured)
│   ├── tti_performance.json
│   └── ...
├── merged_test.mp4         ← video recording
├── logcat.log              ← logcat output
├── test_result.xml         ← JUnit-style XML report
└── ...
```

---

## Screen Recording

HydraLab supports two screen recording strategies for Android.

### Strategy 1: PhoneAppScreenRecorder (Default)
Uses a companion app (`com.microsoft.hydralab.android.client`) installed on the device that provides MediaProjection-based recording.

**How it works:**
1. HydraLab installs `record_release.apk` on the device
2. Grants necessary permissions (foreground service, display over apps, media projection)
3. Starts recording via `am startservice` → captures to `/sdcard/Movies/test_lab/`
4. After test, pulls the video file via `adb pull`

**Recorded file:** `merged_test.mp4` in the result directory

### Strategy 2: ADBScreenRecorder
Uses `adb shell screenrecord` (native Android screen recording). Selected when the test package name matches the recording app's package.

**How it works:**
1. Runs `adb shell screenrecord --bit-rate 3200000 --time-limit 180` in 3-minute segments
2. Pulls each segment to the agent
3. Merges segments using ffmpeg via `FFmpegConcatUtil`

**Note:** ADB screen recording has a 3-minute per-segment limit imposed by Android.

### Disable Recording
Add `"disableRecording": true` to the test task JSON to skip recording entirely (faster execution).

---

## Pull Files from Device

Pull files written by your app to the device filesystem into the test result folder.

### Usage
Add a `pullFileFromDevice` tearDown action to your test task:
```json
{
  "deviceActions": {
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

### How It Works
- Uses `adb pull <pathOnDevice> <resultFolder>/` under the hood
- `adb pull` preserves the directory name, so `/sdcard/Documents/` creates a `Documents/` subfolder in the result directory
- Includes retry logic with file size validation (retries up to `RETRY_TIME` if the pulled file size doesn't match the device file size)

### Common Paths to Pull

| Path                              | Description                              |
|-----------------------------------|------------------------------------------|
| `/sdcard/Documents/`              | App documents / test output              |
| `/sdcard/Download/`               | Downloads folder                         |
| `/data/local/tmp/`                | Temp files (requires root on some)       |
| `/sdcard/Android/data/<pkg>/`     | App-specific external storage            |

### Full Example
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

---

## Device Actions Reference

Device actions run before (setUp) or after (tearDown) test execution.

### Supported Methods

| Method                  | Args                             | Description                                     |
|-------------------------|----------------------------------|-------------------------------------------------|
| `setProperty`           | `[property, value]`              | Set a system property via `setprop`             |
| `setDefaultLauncher`    | `[packageName, activity]`        | Set default launcher app                        |
| `backToHome`            | `[]`                             | Press HOME key                                  |
| `changeGlobalSetting`   | `[setting, value]`               | Change global settings via `settings put global`|
| `changeSystemSetting`   | `[setting, value]`               | Change system settings via `settings put system`|
| `execCommandOnDevice`   | `[command]`                      | Run shell command on device via ADB             |
| `execCommandOnAgent`    | `[command]`                      | Run shell command on the agent host machine     |
| `pushFileToDevice`      | `[pathOnAgent, pathOnDevice]`    | Push file from agent to device                  |
| `pullFileFromDevice`    | `[pathOnDevice]`                 | Pull files from device to result folder         |
| `addToBatteryWhiteList` | `[packageName]`                  | Add app to battery optimization whitelist       |
| `grantPermission`       | `[packageName, permissionName]`  | Grant a runtime permission                      |
| `resetPackage`          | `[packageName]`                  | Clear app data (`pm clear`)                     |

### Example: setUp + tearDown Actions
```json
{
  "deviceActions": {
    "setUp": [
      {
        "deviceType": "Android",
        "method": "addToBatteryWhiteList",
        "args": ["com.example.app"]
      },
      {
        "deviceType": "Android",
        "method": "changeGlobalSetting",
        "args": ["always_finish_activities", "0"]
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

### Device Setup (Automatic)
Before each test, HydraLab automatically:
- Disables animations (`window_animation_scale`, `transition_animation_scale`, `animator_duration_scale` → 0)
- Sets screen timeout to 3 minutes
- Enables touch position display (`pointer_location`)

After the test, these settings are restored to defaults.

---

## Troubleshooting

### 1. Device Not Detected
**Symptoms:** Device doesn't appear in `/api/device/list`

**Check ADB:**
```bash
adb devices
# If "unauthorized" → unlock device and accept USB debugging prompt
# If "offline" → disconnect and reconnect USB
# If not listed → check USB cable and port
```

**Check ANDROID_HOME:**
```bash
echo $ANDROID_HOME
$ANDROID_HOME/platform-tools/adb version
# Must be set and pointing to a valid Android SDK
```

### 2. App Installation Fails
**Symptoms:** Test fails at installation step

**Solutions:**
```bash
# Check device storage
adb -s <SERIAL> shell df /data

# Try manual install with all flags
adb -s <SERIAL> install -r -t -d -g /path/to/app.apk

# If "INSTALL_FAILED_UPDATE_INCOMPATIBLE" → uninstall first
adb -s <SERIAL> uninstall com.example.app
```

**Install flags used by HydraLab:**
- `-d`: Allow version code downgrade
- `-r`: Reinstall, keeping data
- `-t`: Allow test APKs
- `-g`: Grant all manifest permissions

### 3. Test Timeout
**Symptoms:** Test runs until `testTimeOutSec` and gets cancelled

**Solutions:**
- Increase `testTimeOutSec` in the task JSON
- Check if the device is responsive: `adb -s <SERIAL> shell input keyevent 82`
- Check agent logs for ADB timeout errors:
  ```bash
  grep -i "TimeoutException\|ShellCommandUnresponsive" storage/logs/agent.log | tail -10
  ```

### 4. ADB Command Rejected
**Symptoms:** `AdbCommandRejectedException` in agent logs

**Solutions:**
```bash
# Restart ADB server
adb kill-server
adb start-server

# Restart the HydraLab agent after ADB restart
```

### 5. Screen Recording Issues
**Symptoms:** `merged_test.mp4` is 0 bytes or missing

**PhoneAppScreenRecorder issues:**
```bash
# Check if recording app is installed
adb -s <SERIAL> shell pm list packages | grep hydralab

# Check media projection permission
# The device must be unlocked and the "Start now" dialog must be accepted
```

**ADBScreenRecorder issues:**
```bash
# Check if screenrecord is available
adb -s <SERIAL> shell screenrecord --help

# Check if ffmpeg is available for merging
ffmpeg -version
```

### 6. Pull File Fails
**Symptoms:** Files not appearing in result folder

**Check file exists on device:**
```bash
adb -s <SERIAL> shell ls -la /sdcard/Documents/

# Check permissions
adb -s <SERIAL> shell ls -la /sdcard/
```

**Check agent logs:**
```bash
grep -i "Pull file\|pullFile" storage/logs/agent.log | tail -10
```

### 7. `deviceIdentifier` Not Found
**Symptoms:** Task returns error about device not found

**Important:** `deviceIdentifier` must be the device's `serialNum` (not `deviceId`). Check via:
```bash
curl -s "http://localhost:9886/api/device/list" | python3 -c "
import sys, json
for a in json.load(sys.stdin).get('content', []):
    for d in a.get('devices', []):
        if d.get('type') == 'ANDROID':
            print(f\"serialNum={d['serialNum']}  deviceId={d.get('deviceId','')}  status={d['status']}\")"
```

---

## Best Practices

### 1. Pre-Test Checklist
```bash
# Verify device is online
adb devices

# Check HydraLab device list
curl -s "http://localhost:9886/api/device/list" | python3 -m json.tool

# Ensure sufficient storage on device (>2GB)
adb -s <SERIAL> shell df /data
```

### 2. Test Package Guidelines
- Always use `assembleDebug` + `assembleDebugAndroidTest` (debug variants have debug signing)
- Use `testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"` in `build.gradle`
- Avoid hardcoding device-specific values in tests
- Use `testTimeOutSec` appropriate for your test suite (default: 2700s = 45 min)

### 3. Device Management
- Keep devices charged (>20% battery recommended)
- Ensure sufficient storage space (>2GB free)
- Use stable USB cables and direct connections (avoid hubs)
- Disable screen lock / set long screen timeout for test stability

### 4. Test Orchestrator
For flaky tests, consider enabling Android Test Orchestrator:
```json
{
  "isEnableTestOrchestrator": true
}
```
This runs each test in an isolated process, preventing shared state between tests.

### 5. Permission Handling
HydraLab auto-grants standard Android permissions from the APK manifest using `-g` flag. For custom permissions or runtime-only permissions, use setUp device actions:
```json
{
  "deviceActions": {
    "setUp": [
      {
        "deviceType": "Android",
        "method": "grantPermission",
        "args": ["com.example.app", "android.permission.ACCESS_FINE_LOCATION"]
      }
    ]
  }
}
```

### 6. Log Monitoring
```bash
# Monitor agent logs during test execution
tail -f storage/logs/agent.log

# Check for specific errors
grep -i "error\|exception" storage/logs/agent.log | tail -20

# View today's test results
ls -lrt storage/test/result/$(date +%Y/%m/%d)/
```

### 7. CI/CD Integration
```yaml
# Example: GitHub Actions
steps:
  - name: Upload Package
    run: |
      FILE_SET_ID=$(curl -s -X POST "http://$HYDRA_HOST:9886/api/package/add" \
        -F "appFile=@app.apk" \
        -F "testAppFile=@app-androidTest.apk" \
        -F "teamName=Default" \
        -F "buildType=release" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))")
      echo "FILE_SET_ID=$FILE_SET_ID" >> $GITHUB_ENV

  - name: Run Tests
    run: |
      TASK_ID=$(curl -s -X POST "http://$HYDRA_HOST:9886/api/test/task/run" \
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
          "needUninstall":true,
          "deviceActions":{
            "tearDown":[{"deviceType":"Android","method":"pullFileFromDevice","args":["/sdcard/Documents/"]}]
          }
        }' | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))")
      echo "TASK_ID=$TASK_ID" >> $GITHUB_ENV

  - name: Wait for Results
    run: |
      # Poll task status until completion
      while true; do
        STATUS=$(curl -s "http://$HYDRA_HOST:9886/api/test/task/$TASK_ID" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('status',''))")
        echo "Task status: $STATUS"
        if [ "$STATUS" = "FINISHED" ] || [ "$STATUS" = "FAILED" ]; then break; fi
        sleep 30
      done
```

---

## Known Issues & Solutions

### Issue 1: ADB Timeout Exceptions
**Status:** Known, Recoverable
**Impact:** Commands occasionally timeout on slow USB connections
**Solution:** HydraLab sets `adbTimeout` flag on the device. Reconnect the device or restart ADB:
```bash
adb kill-server && adb start-server
```

### Issue 2: Recording Permission Dialog
**Status:** Known, Handled
**Impact:** PhoneAppScreenRecorder needs MediaProjection permission
**Solution:** HydraLab auto-clicks "Start now" / "Allow" dialogs via UIAutomator dump + tap. Keep the device unlocked during test setup.

### Issue 3: Install Failures on Downgrade
**Status:** Known, Handled
**Impact:** `INSTALL_FAILED_VERSION_DOWNGRADE` when installing older APK
**Solution:** HydraLab uses `-d` flag (allow downgrade). If it still fails, set `"needUninstall": true` in the task JSON.

---

## File Locations

### Important Directories
```
HydraLab/
├── storage/
│   ├── logs/
│   │   └── agent.log                 # Main agent logs
│   ├── test/result/                  # Test results by date
│   │   └── YYYY/MM/DD/<ts>/<serial>/
│   │       ├── Documents/            # Pulled files
│   │       ├── merged_test.mp4       # Video recording
│   │       ├── logcat.log            # Logcat output
│   │       └── test_result.xml       # JUnit XML report
│   ├── errorOutput/                  # Error logs by date
│   └── packages/                     # Uploaded test packages
├── common/src/main/java/com/microsoft/hydralab/common/
│   ├── util/ADBOperateUtil.java      # ADB operations (install, pull, push, exec)
│   ├── management/device/impl/
│   │   └── AndroidDeviceDriver.java  # Android device driver
│   └── screen/
│       ├── PhoneAppScreenRecorder.java   # App-based recording
│       └── ADBScreenRecorder.java        # ADB-based recording
├── agent/src/main/java/com/microsoft/hydralab/agent/runner/
│   ├── espresso/EspressoRunner.java      # Instrumentation test runner
│   ├── monkey/AdbMonkeyRunner.java       # Monkey test runner
│   ├── appium/AppiumRunner.java          # Appium test runner
│   └── maestro/MaestroRunner.java        # Maestro test runner
└── docs/
    ├── Android-Testing-Guide.md      # This guide
    ├── iOS-Testing-Guide.md          # iOS testing guide
    └── API-Reference.md              # Full API reference
```

---

**Last Updated:** 2026-02-19
**Maintained By:** HydraLab Team
