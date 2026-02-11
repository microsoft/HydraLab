# iOS XCTest Execution via HydraLab CLI Guide

## Architecture Overview

**iOS 16.x Architecture (Direct USB)**
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Center ◄──WS──► Agent ◄───── iproxy (USB) ─────► iOS Device (16.x)  │
│  :9886           :10086 (Appium)                    WDA :8100         │
└─────────────────────────────────────────────────────────────────────────────┘
• No tunnel required — direct USB via usbmuxd/lockdown
• WDA launched with: --udid <DEVICE_UDID>
```

**iOS 17+ Architecture (Tunnel Required)**
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Center ◄──WS──► Agent ◄── pymobiledevice3 tunneld ──► iOS Device (17+) │
│  :9886           :10086 (Appium)   (sudo required)   WDA :8100        │
└─────────────────────────────────────────────────────────────────────────────┘
• Tunnel required — CoreDevice Framework replaces lockdown
• WDA launched with: --tunnel <DEVICE_UDID>
```

## iOS Version Comparison

| Aspect                | iOS 16.x                      | iOS 17+                                |
|-----------------------|-------------------------------|----------------------------------------|
| **Device Protocol**   | usbmuxd / lockdown            | CoreDevice Framework (DDI)             |
| **Tunnel Required**   | ❌ No                         | ✅ Yes (`tunneld` daemon)              |
| **Sudo Required**     | ❌ No                         | ✅ Yes (for tunneld)                   |
| **WDA Launch**        | `--udid <UDID>`               | `--tunnel <UDID>`                      |
| **Connection**        | Direct USB                    | Via tunnel                             |
| **pymobiledevice3**   | v4.x (optional)               | v4.x (required)                        |
| **Video Recording**   | ✅ Supported                  | ✅ Supported                           |

## Prerequisites

1. **Xcode 16+** with iOS 17+ SDK
2. **pymobiledevice3 v4.x**: `pip install pymobiledevice3==4.27.7`
3. **Appium 2.x**: `npm install -g appium` (required for video recording)
4. **XCUITest Driver**: `appium driver install xcuitest`
5. **WDA** installed with bundle ID: `com.microsoft.wdar.xctrunner`
6. **HydraLab** jars rebuilt with iOS 17+ support

## Quick Start

### For iOS 16.x (3 Steps)
```bash
# Step 1: Start HydraLab Center
nohup java -jar center/build/libs/center.jar \
  --spring.config.additional-location=center-application.yml > center.log 2>&1 &

# Step 2: Start HydraLab Agent (includes Appium on port 10086)
nohup java -jar agent/build/libs/agent.jar \
  --spring.config.additional-location=application.yml > agent.log 2>&1 &

# Step 3: Launch WDA on device (direct USB)
python3 -m pymobiledevice3 developer dvt launch com.microsoft.wdar.xctrunner --udid <DEVICE_UDID>
```

### For iOS 17+ (4 Steps)
```bash
# Step 1: Start tunnel daemon (requires sudo, keep running)
sudo python3 -m pymobiledevice3 remote tunneld -d

# Step 2: Start HydraLab Center
nohup java -jar center/build/libs/center.jar \
  --spring.config.additional-location=center-application.yml > center.log 2>&1 &

# Step 3: Start HydraLab Agent (includes Appium on port 10086)
nohup java -jar agent/build/libs/agent.jar \
  --spring.config.additional-location=application.yml > agent.log 2>&1 &

# Step 4: Launch WDA on device (via tunnel)
python3 -m pymobiledevice3 developer dvt launch --tunnel <DEVICE_UDID> com.microsoft.wdar.xctrunner
```

### Upload Package & Run Test
```bash
# Upload test package
FILE_SET_ID=$(curl -s -X POST "http://localhost:9886/api/package/add" \
  -F "appFile=@hydralab_test_package.zip" \
  -F "teamName=Default" -F "buildType=release" | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('content',{}).get('id',''))")

# Get device UDID
DEVICE_UDID=$(curl -s "http://localhost:9886/api/device/list" | \
  python3 -c "import sys,json; [print(d.get('deviceId')) or exit(0) for a in json.load(sys.stdin).get('content',[]) for d in a.get('devices',[]) if d.get('type')=='IOS' and d.get('status')=='ONLINE']")

# Run test with video recording
curl -s -X POST "http://localhost:9886/api/test/task/run" \
  -H "Content-Type: application/json" \
  -d '{"fileSetId":"'"$FILE_SET_ID"'","deviceIdentifier":"'"$DEVICE_UDID"'","runningType":"XCTEST","pkgName":"com.example.app","testScope":"TEST_APP","testTimeOutSec":1800,"frameworkType":"XCTest","disableRecording":false}'
```

## WDA Setup (One-time)

Build WDA with correct bundle ID:
```bash
cd ~/.appium/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent

xcodebuild -project WebDriverAgent.xcodeproj \
  -scheme WebDriverAgentRunner \
  -destination "id=$DEVICE_UDID" \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM=YOUR_TEAM_ID \
  CODE_SIGN_IDENTITY="Apple Development" \
  PRODUCT_BUNDLE_IDENTIFIER="com.microsoft.wdar" \
  build-for-testing
```

Install WDA:
```bash
cd /tmp && rm -rf Payload WDA.ipa && mkdir -p Payload
cp -r ~/Library/Developer/Xcode/DerivedData/WebDriverAgent-*/Build/Products/Debug-iphoneos/WebDriverAgentRunner-Runner.app Payload/
zip -rq WDA.ipa Payload
tidevice -u $DEVICE_UDID install /tmp/WDA.ipa
```

## API Reference

| Endpoint                      | Method | Description                    |
|-------------------------------|--------|--------------------------------|
| `/api/package/add`            | POST   | Upload test package (.zip)     |
| `/api/test/task/run`          | POST   | Start test execution           |
| `/api/device/list`            | GET    | List connected devices         |
| `/api/test/task/{id}/status`  | GET    | Check test status              |

## Troubleshooting

| Issue                              | Solution                                           |
|------------------------------------|----------------------------------------------------|
| Device not found                   | Ensure `tunneld` is running with `sudo`            |
| WDA bundle ID mismatch             | Rebuild WDA with `PRODUCT_BUNDLE_IDENTIFIER`       |
| pymobiledevice3 errors             | Downgrade to v4.x: `pip install pymobiledevice3==4.27.7` |
| Agent ID conflict                  | Kill duplicate agent processes                     |
| Device stuck in TESTING            | Restart agent or wait for timeout                  |

## Video Recording Requirements

| Component         | iOS 16.x        | iOS 17+                | Purpose                           |
|-------------------|-----------------|------------------------|-----------------------------------|
| tunneld           | ❌ Not needed   | ✅ Required (sudo)     | Device communication tunnel       |
| HydraLab Agent    | ✅ Running      | ✅ Running             | Includes Appium server (:10086)   |
| WDA               | ✅ Running      | ✅ Running             | Screen capture via MJPEG stream   |
| `disableRecording`| `false`         | `false`                | Enable in test request            |

**Startup Sequence:**
```
iOS 16.x: Center → Agent → WDA (--udid) → Test
iOS 17+:  tunneld (sudo) → Center → Agent → WDA (--tunnel) → Test
```

> ❌ **Common Error**: `Cannot start recording - IOSDriver not initialized`
> ✅ **Fix**: Ensure WDA is running on the device before starting tests.

> ❌ **Common Error**: `The screen capture process 'ffmpeg' died unexpectedly`
> ✅ **Fix**: Ensure ffmpeg is installed: `brew install ffmpeg`

## Test Results Location

```
storage/test/result/YYYY/MM/DD/HHMMSS/<device_udid>/
├── Xctest/                    # xctestrun files & test app
├── result.xcresult/           # Xcode test results
├── result.xcresult.zip        # Compressed test results
├── <pkgName>_MMDDHHMMSS.log   # Test execution logs
├── <pkgName>.gif              # Screenshot GIF (always generated)
├── iOSSysLog.log              # iOS system logs
├── LegacyCrash/               # Crash logs from device
└── merged_test.mp4            # Video recording (if WDA running)
```

---
**File**: `ios_test_via_cli_guide.md` | **Location**: `/Users/abhishek.bedi/peet/HydraLab/`
