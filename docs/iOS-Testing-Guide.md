# iOS Testing Guide for HydraLab

## Table of Contents
- [Prerequisites](#prerequisites)
- [Onboarding a New iOS Device](#onboarding-a-new-ios-device)
- [Running iOS Tests](#running-ios-tests)
- [Screen Recording](#screen-recording)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)
- [Known Issues & Solutions](#known-issues--solutions)

---

## Prerequisites

### Required Tools
- **Xcode**: Latest stable version
- **pymobiledevice3**: iOS device management tool
- **Python 3**: Version 3.8 or higher
- **Appium**: iOS automation framework
- **HydraLab**: Agent running on macOS

### Device Requirements
- iOS device connected via USB
- Device must be in Developer Mode
- Device must be unlocked during test execution
- Trust relationship established between device and Mac

### Verify Setup
```bash
# Check pymobiledevice3 installation
python3 -m pymobiledevice3 usbmux list

# Check connected iOS devices
python3 -m pymobiledevice3 lockdown info --udid <device-udid>

# Verify HydraLab agent is running
curl -s http://localhost:9886/api/device/list
```

---

## Onboarding a New iOS Device

Follow these steps to add a new iOS device to HydraLab for testing.

### Step 1: Physical Setup
1. **Connect the device** via USB to the Mac running HydraLab agent
2. **Unlock the device** and keep it unlocked during setup
3. **Trust the computer** when prompted on the device

### Step 2: Enable Developer Mode (iOS 16+)
```bash
# For iOS 16 and later, Developer Mode must be enabled:
# On device: Settings → Privacy & Security → Developer Mode → Enable → Restart device
```

### Step 3: Verify Device Connection
```bash
# Check if pymobiledevice3 can see the device
python3 -m pymobiledevice3 usbmux list

# Expected output (JSON array with device info):
# [{"DeviceName": "iPhone", "Identifier": "c7ad90190806...", ...}]

# Get detailed device info
python3 -m pymobiledevice3 lockdown info --udid <device-udid>
```

### Step 4: Install WebDriverAgent (WDA)
WDA is required for device automation. It must be signed with a valid Apple Developer certificate.

```bash
# Option 1: Build and install WDA via Xcode
# 1. Open WebDriverAgent.xcodeproj in Xcode
# 2. Select your device as target
# 3. Configure signing (Team, Bundle ID)
# 4. Build and run WebDriverAgentRunner scheme

# Option 2: Use pre-built WDA (if available)
python3 -m pymobiledevice3 apps install --udid <device-udid> /path/to/WDA.ipa
```

### Step 5: Verify Device in HydraLab
```bash
# Restart the HydraLab agent to pick up the new device
# Then verify the device appears:
curl -s "http://localhost:9886/api/device/list" | python3 -m json.tool

# Expected: Device should appear with status "ONLINE" and type "IOS"
```

### Step 6: Test Device Functionality
```bash
# Test screenshot capability
python3 -m pymobiledevice3 developer dvt screenshot --udid <device-udid> /tmp/test.png

# Test app listing
python3 -m pymobiledevice3 apps list --udid <device-udid>

# Test port forwarding (for WDA)
python3 -m pymobiledevice3 usbmux forward --serial <device-udid> 8100 8100 &
curl http://127.0.0.1:8100/status
# Expected: {"value":{"ready":true,...}}
```

### Device Onboarding Checklist

| Step | Verification | Expected Result |
|------|--------------|------------------|
| USB Connection | `pymobiledevice3 usbmux list` | Device appears in list |
| Trust Established | `pymobiledevice3 lockdown info` | Returns device info (not error) |
| Developer Mode | Device settings | Enabled (iOS 16+) |
| WDA Installed | `pymobiledevice3 apps list \| grep wda` | WDA bundle ID appears |
| HydraLab Detection | `/api/device/list` | Device status: ONLINE |
| Screenshot | `pymobiledevice3 developer dvt screenshot` | Image file created |

### Troubleshooting Device Onboarding

**Device not appearing in `usbmux list`:**
- Check USB cable and port
- Try different USB port (preferably USB-A or direct connection)
- Restart the device
- On device: Settings → General → Reset → Reset Location & Privacy

**"Pairing not established" error:**
- Unlock device and tap "Trust" when prompted
- If no prompt, reset trust: Settings → General → Transfer or Reset → Reset Location & Privacy

**Developer Mode not available:**
- Connect device to Xcode first (triggers Developer Mode option)
- Xcode → Window → Devices and Simulators → Select device

**WDA fails to start:**
- Check provisioning profile is valid
- Verify signing identity matches device
- Try rebuilding WDA from source in Xcode

---

## Running iOS Tests

### 1. Prepare Test Package
Your test package should be a ZIP file containing:
```
hydralab_test_package.zip
├── Runner.app/          # Main app bundle
├── App.framework/       # App framework
└── RunnerUITests.xctest/ # UI test bundle
```

### 2. Upload and Run Tests

#### Using API (Recommended)
```bash
# Step 1: Upload test package
FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@/path/to/hydralab_test_package.zip" \
  -F "teamName=Default" \
  -F "buildType=release" | python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))")

echo "File Set ID: $FILE_SET_ID"

# Step 2: Get available iOS device
DEVICE_UDID=$(curl -s "http://localhost:9886/api/device/list" | python3 -c "import sys,json; [print(d.get('deviceId')) or exit(0) for a in json.load(sys.stdin).get('content',[]) for d in a.get('devices',[]) if d.get('type')=='IOS' and d.get('status')=='ONLINE']")

echo "Device UDID: $DEVICE_UDID"

# Step 3: Run tests
TASK_RESPONSE=$(curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSetId":"'"$FILE_SET_ID"'",
    "deviceIdentifier":"'"$DEVICE_UDID"'",
    "runningType":"XCTEST",
    "pkgName":"com.your.app",
    "testScope":"TEST_APP",
    "testTimeOutSec":1800,
    "frameworkType":"XCTest",
    "disableRecording":false
  }')

echo "Response: $TASK_RESPONSE"
```

#### Using Workflow (Warp Drive)
Save the "Build & Run iOS" workflow for quick execution:
```bash
# Run from Warp
⌘K → Select "Build & Run iOS" workflow
```

### 3. Monitor Test Execution
```bash
# Check task status
curl -s "http://localhost:9886/api/test/task/status?taskId=<task-id>"

# View logs
tail -f storage/logs/agent.log
```

### 4. Retrieve Results
Test results are stored in:
```
storage/test/result/YYYY/MM/DD/<timestamp>/<device-udid>/
├── Xctest/
│   ├── test_result.xml
│   ├── screenshots/
│   └── logs/
└── attachments/
```

---

## Screen Recording

HydraLab supports automatic screen recording during iOS test execution. The recording uses ffmpeg to capture MJPEG stream from the device.

### How It Works
1. **Port Forwarding**: pymobiledevice3 forwards device MJPEG port (9100) to a local port
2. **Stream Capture**: ffmpeg connects to the forwarded port and records H.264 video
3. **Output**: Video saved as `merged_test.mp4` in test results directory

### Recording Output Location
```
storage/test/result/YYYY/MM/DD/<timestamp>/<device-udid>/merged_test.mp4
```

### Verify Recording is Working
During test execution, check agent logs for:
```bash
# Successful recording shows:
# "Waiting for MJPEG port XXXX to become active..."
# "MJPEG port XXXX is now active"
# "Starting ffmpeg recording from MJPEG port XXXX to .../merged_test.mp4"
# "Input #0, mjpeg, from 'http://127.0.0.1:XXXX'"

tail -f agent/agent.log | grep -i "mjpeg\|ffmpeg\|recording"
```

### Disable Recording
To run tests without recording (faster execution):
```bash
curl -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{
    ...
    "disableRecording": true
  }'
```

### Recording Troubleshooting

**Video file is 0 bytes:**
- Check if MJPEG port forwarding started successfully
- Verify ffmpeg is installed: `ffmpeg -version`
- Check agent logs for "Connection refused" errors

**"Connection refused" on MJPEG port:**
- This was fixed by adding port readiness wait (see Known Issues)
- If still occurring, run cleanup script and restart agent

**Low quality or choppy video:**
- Default settings: 720x360, H.264 codec
- Recording quality depends on device-to-Mac USB bandwidth
- Use USB 3.0 ports for better quality

---

## Troubleshooting

### Critical Issue: Port Conflicts (SOLVED ✅)

#### Problem
```
Error: The port #7408 is occupied by an other process.
Cannot ensure MJPEG broadcast functionality...
```

#### Root Cause
- Stale `pymobiledevice3` port forwarding processes from interrupted tests
- Unreliable port occupation detection
- Race conditions during concurrent test execution

#### Solution 1: Run Cleanup Script (Recommended)
```bash
# Before running tests
./scripts/cleanup_ios_ports.sh

# Verify cleanup
./scripts/cleanup_ios_ports.sh
# Output: ✅ No stale port forwarding processes found
```

#### Solution 2: Manual Cleanup
```bash
# Kill all pymobiledevice3 port forwarding processes
ps aux | grep "pymobiledevice3 usbmux forward" | grep -v grep | awk '{print $2}' | xargs kill -9

# Verify ports are freed
lsof -i :7408
# Should return empty
```

#### Solution 3: Restart HydraLab Agent
```bash
# Stop agent
./stop_agent.sh  # or kill the Java process

# Run cleanup
./scripts/cleanup_ios_ports.sh

# Start agent
./start_agent.sh
```

#### Prevention
The following code improvements have been implemented to prevent this issue:

**✅ Enhanced Port Detection** (`IOSUtils.java`)
- Uses `lsof` instead of `netstat` for accurate port checking
- Detects both network listeners and forwarding processes
- Double-validation to prevent false negatives

**✅ Smart Port Reuse** (`IOSUtils.java`)
- Validates cached ports before reuse
- Automatically cleans up stale port mappings
- Prevents allocation of already-occupied ports

**✅ Automated Cleanup Script** (`scripts/cleanup_ios_ports.sh`)
- Run before test execution
- Safe - only targets pymobiledevice3 processes
- Can be integrated into CI/CD pipelines

---

### Common Issues

#### 1. Device Not Detected
**Symptoms:**
```bash
curl http://localhost:9886/api/device/list
# Returns empty devices array
```

**Solution:**
```bash
# Check USB connection
python3 -m pymobiledevice3 usbmux list

# Restart iOS device watcher
# Kill pymobiledevice3 processes and let HydraLab restart them
./scripts/cleanup_ios_ports.sh

# Check agent logs
tail -f storage/logs/agent.log | grep "iOS"
```

#### 2. App Installation Fails
**Symptoms:**
- Test fails at installation step
- Error: "Unable to install app"

**Solution:**
```bash
# Check device storage
python3 -m pymobiledevice3 lockdown info --udid <udid> | grep "TotalDiskCapacity"

# Manually test installation
python3 -m pymobiledevice3 apps install --udid <udid> path/to/app.ipa

# Check provisioning profile
codesign -d --entitlements - path/to/Runner.app
```

#### 3. WebDriverAgent (WDA) Fails to Start
**Symptoms:**
- "No WDA proxy is running on port XXXX"
- Tests timeout during setup

**Solution:**
```bash
# Check if WDA bundle is installed
python3 -m pymobiledevice3 apps list --udid <udid> | grep wda

# Check port forwarding
lsof -i :<wda-port>

# Manual WDA proxy test
python3 -m pymobiledevice3 usbmux forward --serial <udid> 8100 8100
curl http://127.0.0.1:8100/status
```

#### 4. Tests Pass But Task ID Shows "N/A"
**Symptoms:**
```
Task ID: N/A
Status: 200
```

**Solution:**
The task was submitted successfully but the response parsing failed. Check the actual response:
```bash
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{ ... }' | python3 -m json.tool
```

Look for the task ID in the response and use it to check status.

#### 5. Device Unlock Fails
**Symptoms:**
- "Failed to unlock device via Appium"
- This is logged as a warning but is **non-fatal for XCTest**

**Solution:**
- Keep device unlocked manually during test execution
- This warning can be safely ignored for XCTest runs
- For Appium tests, ensure device passcode is disabled

---

## Best Practices

### 1. Pre-Test Checklist
```bash
# Always run before starting tests
./scripts/cleanup_ios_ports.sh

# Verify device is online
curl -s http://localhost:9886/api/device/list | python3 -m json.tool

# Check HydraLab agent status
curl -s http://localhost:9886/api/agent/status
```

### 2. Test Package Structure
```bash
# Verify package structure before upload
unzip -l hydralab_test_package.zip

# Should contain:
# - Runner.app/
# - App.framework/
# - RunnerUITests.xctest/ (or similar test bundle)
```

### 3. Device Management
- Keep devices unlocked during test execution
- Ensure sufficient storage space (>5GB recommended)
- Use stable USB-C/Lightning cables
- Avoid device disconnection during tests

### 4. Port Management
- Always run cleanup script before batch tests
- Monitor port usage in logs
- If tests fail, run cleanup before retry

### 5. Log Monitoring
```bash
# Monitor agent logs during test execution
tail -f storage/logs/agent.log

# Check for specific errors
grep -i "error\|exception" storage/logs/agent.log | tail -20

# View test-specific logs
ls -lrt storage/test/result/$(date +%Y/%m/%d)/
```

### 6. CI/CD Integration
```yaml
# Example: GitHub Actions
steps:
  - name: Cleanup iOS Ports
    run: ./scripts/cleanup_ios_ports.sh
    
  - name: Run iOS Tests
    run: |
      FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" ...)
      # ... rest of test execution
    
  - name: Cleanup After Tests
    if: always()
    run: ./scripts/cleanup_ios_ports.sh
```

---

## Known Issues & Solutions

### Issue 1: Port 7408 Conflict ✅ RESOLVED
**Status:** Fixed in current version  
**Impact:** Test execution fails with port occupation error  
**Solution:** Code improvements + cleanup script (see Troubleshooting section)

### Issue 2: Device Stability Monitoring Warnings
**Status:** Known, Non-Critical  
**Impact:** Logs show stability warnings, but don't affect tests  
**Example:**
```
Window time length: 5 minutes, threshold of change number: 12.
Device contains 1 changes.
```
**Solution:** These are informational only, safe to ignore

### Issue 3: WebSocket Connection Drops
**Status:** Known, Auto-Recovery  
**Impact:** Agent reconnects automatically  
**Example:**
```
onClose 1006, null, false
```
**Solution:** Agent will reconnect. Check network stability if frequent.

---

## API Reference

### Key Endpoints

#### Upload Test Package
```bash
POST http://localhost:9886/api/package/add
Content-Type: multipart/form-data

Form Data:
- appFile: <zip-file>
- teamName: Default
- buildType: release

Response:
{
  "code": 200,
  "content": {
    "id": "<file-set-id>"
  }
}
```

#### List Devices
```bash
GET http://localhost:9886/api/device/list

Response:
{
  "code": 200,
  "content": [
    {
      "devices": [
        {
          "deviceId": "<udid>",
          "type": "IOS",
          "status": "ONLINE",
          "model": "iPhone 14 Pro"
        }
      ]
    }
  ]
}
```

#### Run Test Task
```bash
POST http://localhost:9886/api/test/task/run
Content-Type: application/json

Body:
{
  "fileSetId": "<file-set-id>",
  "deviceIdentifier": "<device-udid>",
  "runningType": "XCTEST",
  "pkgName": "com.your.app",
  "testScope": "TEST_APP",
  "testTimeOutSec": 1800,
  "frameworkType": "XCTest",
  "disableRecording": false
}

Response:
{
  "code": 200,
  "content": {
    "id": "<task-id>",
    "status": "RUNNING"
  }
}
```

#### Check Task Status
```bash
GET http://localhost:9886/api/test/task/status?taskId=<task-id>

Response:
{
  "code": 200,
  "content": {
    "status": "COMPLETED",
    "result": "PASSED",
    "totalTests": 50,
    "passedTests": 48,
    "failedTests": 2
  }
}
```

---

## File Locations

### Important Directories
```
HydraLab/
├── scripts/
│   └── cleanup_ios_ports.sh          # Port cleanup utility
├── storage/
│   ├── logs/
│   │   └── agent.log                 # Main agent logs
│   ├── test/result/                  # Test results by date
│   ├── errorOutput/                  # Error logs by date
│   └── packages/                     # Uploaded test packages
├── common/src/main/java/com/microsoft/hydralab/common/
│   ├── util/IOSUtils.java            # iOS utilities (port fixes)
│   └── management/
│       └── device/impl/IOSDeviceDriver.java
└── docs/
    └── iOS-Testing-Guide.md          # This guide
```

### Log Files
- Agent logs: `storage/logs/agent.log`
- Error logs: `storage/errorOutput/YYYY/MM/DD/`
- Test results: `storage/test/result/YYYY/MM/DD/<timestamp>/<udid>/`

---

## Version History

### v1.1.0 (2026-02-16)
- ✅ **Fixed video recording for Mac** - Switched to ffmpeg-based recording with pymobiledevice3 port forwarding
- ✅ **Fixed MJPEG port timing issue** - Added `waitForPortToBeListening()` to ensure port is ready before ffmpeg connects
- ✅ **Removed Appium mjpegServerPort capability conflict** - Screen recorder now handles its own port forwarding
- ✅ **Added device onboarding guide** - Step-by-step instructions for adding new iOS devices
- ✅ **Added screen recording documentation** - How recording works and troubleshooting

### v1.0.0 (2026-02-13)
- ✅ Fixed port conflict issue (7408 and other ports)
- ✅ Added enhanced port detection using `lsof`
- ✅ Implemented smart port reuse logic
- ✅ Created automated cleanup script
- ✅ Added comprehensive troubleshooting guide

---

## Support & Contact

### Getting Help
1. Check this guide's Troubleshooting section
2. Review agent logs: `tail -f storage/logs/agent.log`
3. Run diagnostics: `./scripts/cleanup_ios_ports.sh`
4. Check error outputs: `ls -lrt storage/errorOutput/$(date +%Y/%m/%d)/`

### Useful Commands
```bash
# Quick diagnostics
./scripts/cleanup_ios_ports.sh                    # Cleanup stale processes
python3 -m pymobiledevice3 usbmux list            # List devices
curl http://localhost:9886/api/device/list        # Check HydraLab devices
lsof -i :9886                                      # Check HydraLab port
ps aux | grep java | grep hydralab                # Check agent process

# Logs
tail -f storage/logs/agent.log                    # Monitor agent
grep -i "error" storage/logs/agent.log | tail -20 # Recent errors
ls -lrt storage/test/result/$(date +%Y/%m/%d)/   # Today's results
```

---

**Last Updated:** 2026-02-16  
**Maintained By:** HydraLab Team
