# Changelog — tidevice to pymobiledevice3 Migration

## Background

HydraLab's iOS device management originally relied on [tidevice](https://github.com/alibaba/taobao-iphone-device), an open-source tool by Alibaba for communicating with iOS devices over USB. Starting with **iOS 17**, Apple introduced significant changes to its developer tooling protocol (replacing the legacy `lockdownd`-based developer image mounting with a new CoreDevice/RemoteXPC framework). tidevice does not support these changes and has not been actively maintained to address them.

**[pymobiledevice3](https://github.com/doronz88/pymobiledevice3)** is an actively maintained, pure-Python implementation that supports iOS 17+ and the new Apple protocols. This migration replaces all tidevice CLI calls with their pymobiledevice3 equivalents across the HydraLab agent and common modules.

---

## Phase 1 — Core Migration (`9cf52178`)

**Branch:** `pymobiledevice3-ios17-support-001`

### What changed
- **`IOSUtils.java`**: Replaced all `tidevice` CLI invocations with `pymobiledevice3` equivalents:
  - `tidevice list --json` → `pymobiledevice3 usbmux list`
  - `tidevice -u <udid> info --json` → `pymobiledevice3 lockdown info --udid <udid>`
  - `tidevice -u <udid> applist` → `pymobiledevice3 apps list --udid <udid>`
  - `tidevice -u <udid> install <path>` → `pymobiledevice3 apps install --udid <udid> <path>`
  - `tidevice -u <udid> uninstall <pkg>` → `pymobiledevice3 apps uninstall --udid <udid> <pkg>`
  - `tidevice -u <udid> launch <pkg>` → `pymobiledevice3 developer dvt launch --udid <udid> <pkg>`
  - `tidevice -u <udid> relay <local> <remote>` → `pymobiledevice3 usbmux forward --serial <udid> <local> <remote>`
  - `tidevice -u <udid> screenshot <path>` → `pymobiledevice3 developer dvt screenshot --udid <udid> <path>`
  - `tidevice -u <udid> syslog` → `pymobiledevice3 syslog live --udid <udid>`
  - `tidevice -u <udid> crashreport <dir>` → `pymobiledevice3 crash pull --udid <udid> <dir>`
- **`IOSDeviceDriver.java`**: Updated `parseJsonToDevice()` to handle pymobiledevice3's JSON field names (`Identifier`, `DeviceName`, `ProductType`, `ProductVersion`) with fallback to tidevice's old field names (`udid`, `name`, `market_name`, `product_version`).
- **`EnvCapability.java`**: Added `pymobiledevice3` as a recognized environment capability keyword.
- **Device watcher**: `tidevice usbmux watch` (continuous USB event stream) was removed because pymobiledevice3 has no equivalent `watch` command. Replaced with a one-time `updateAllDeviceInfo()` call at startup.

### Why
tidevice cannot communicate with iOS 17+ devices. The lockdown protocol and developer image mounting mechanism changed fundamentally in iOS 17. pymobiledevice3 supports both the legacy protocol (iOS < 17) and the new CoreDevice/RemoteXPC protocol (iOS 17+).

---

## Phase 2 — Video Recording Fix (`0e54ad41`, `0be2f9ce`)

**Branch:** `pymobiledevice3-ios17-support-001`

### What changed
- **`IOSAppiumScreenRecorderForMac.java`**: Fixed video recording failure on macOS.

### Why
After the migration, the `pymobiledevice3 developer dvt screenshot` command has different output behavior than tidevice's screenshot command. The screen recorder on Mac needed adjustments to correctly capture frames and produce video output via ffmpeg.

---

## Phase 3 — Zip Bomb Protection (`b0656788`)

**Branch:** `pymobiledevice3-ios17-support-001`

### What changed
- **`ZipBombChecker.java`**: Fixed zip bomb detection logic.

### Why
Unrelated security hardening fix included in the branch — ensures uploaded test packages are validated against zip bomb attacks before extraction.

---

## Phase 4 — iOS 17 WDA Launch & QuickTime Video (`b0b40b1a`)

**Branch:** `pymobiledevice3-ios17-support-002`

### What changed
- **`IOSUtils.java` — `proxyWDA()`**: Added iOS version-branched WDA (WebDriverAgent) launch strategy:
  - **iOS < 17**: Uses `pymobiledevice3 developer dvt launch` to start WDA (same as before).
  - **iOS 17+**: Uses `xcodebuild test-without-building` to start WDA. On iOS 17+, `dvt launch` crashes WDA because it doesn't create a proper XCUITest session. `xcodebuild` properly bootstraps the XCTest framework and keeps WDA's HTTP server alive.
- **`IOSUtils.java` — `killProxyWDA()`**: Updated to kill both `xcodebuild`-based and `dvt launch`-based WDA processes, scoped by device UDID.
- **`IOSUtils.java`**: Added `isIOS17OrAbove()` helper and WDA project path discovery (`getWdaProjectPath()`).
- **`IOSAppiumScreenRecorderForMac.java`**: Replaced ffmpeg-based frame stitching with QuickTime-compatible screen recording using `screencapture -v` on macOS, which is more reliable for iOS 17+ devices.
- **`AppiumServerManager.java`**: Minor adjustments for iOS driver initialization.
- **Scripts added**:
  - `scripts/install_wda.sh` — Automates WDA installation for iOS 17+ devices via `xcodebuild`.
  - `scripts/install_wda_below_ios_17.sh` — WDA installation for iOS < 17 devices.
  - `scripts/cleanup_ios_ports.sh` — Cleans up stale port forwarding and WDA processes.
- **`docs/iOS-Testing-Guide.md`**: Comprehensive guide for setting up iOS testing with HydraLab.

### Why
iOS 17 fundamentally changed how developer tools interact with devices. The old approach of launching WDA via `dvt launch` no longer works because iOS 17's XCTest framework requires a proper test session context. `xcodebuild test-without-building` is Apple's supported mechanism for running XCUITest bundles and is the only reliable way to keep WDA alive on iOS 17+.

---

## Phase 5 — XCTest Runner Cleanup Fix (`564b06d7`)

**Branch:** `pymobiledevice3-ios17-support-003-app-cleanup-fix`

### What changed
- **`XCTestRunner.java`**:
  - Fixed test completion detection to handle Objective-C formatted output (e.g., `Executed X tests` pattern) from `xcodebuild test-without-building`.
  - Added early completion detection so the runner doesn't wait for the full timeout when tests have already finished.
  - Ensured `finishTest()` always runs via try/finally, preventing resource leaks when tests fail or the process exits unexpectedly.
- **`XCTestCommandReceiver.java`**: Improved command output parsing for the new xcodebuild-based test execution.

### Why
When running XCTest via `xcodebuild test-without-building`, the output format differs from the old Appium-based execution. The runner was not detecting test completion correctly, causing it to either hang until timeout or skip cleanup. The fix ensures reliable detection of test results and guaranteed cleanup of device resources.

---

## Phase 6 — Auto Device Detection (`123cda5e` + uncommitted)

**Branch:** `pymobiledevice3-ios17-support-004-auto-device-detection`

### What changed
- **`ScheduledDeviceControlTasks.java`**: Added a `@Scheduled` task that calls `deviceControlService.updateDeviceList()` every **60 seconds** (with a 30-second initial delay after agent startup).
- **`DeviceControlService.java`**: Added `updateDeviceList()` method that delegates to `deviceDriverManager.updateAllDeviceInfo()`.

### Why
The original tidevice integration used `tidevice usbmux watch`, which spawned a long-running process that streamed USB connect/disconnect events in real time. The `IOSDeviceWatcher` thread read this stream and called `updateAllDeviceInfo()` on every `MessageType` event, enabling instant hot-plug detection.

pymobiledevice3's `usbmux` subcommand only supports `list` and `forward` — there is no `watch` or `listen` equivalent. During the Phase 1 migration, the continuous watcher was replaced with a single `updateAllDeviceInfo()` call at startup, with a comment stating "Device monitoring is now handled through periodic polling" — but **no polling was actually implemented**.

This meant newly connected iOS devices were invisible to the agent until it was restarted. The fix adds a simple periodic poll (every 60 seconds) as a pragmatic replacement. While not as responsive as the old event-driven approach, it ensures devices are discovered automatically within a reasonable time window.

### Removed / Deprecated
- **`IOSDeviceWatcher.java`**: Still exists in the codebase but is no longer invoked. Can be removed in a future cleanup.

---

## Phase 7 — iOS `pullFileFromDevice` Implementation (`uncommitted`)

**Branch:** `pymobiledevice3-ios17-support-005-tti-threshold-fix`

### What changed
- **`IOSDeviceDriver.java` — `pullFileFromDevice()`**: Replaced the no-op stub (`"Nothing Implemented for iOS"`) with a full implementation that:
  - Parses the `pathOnDevice` argument in two formats:
    - **`bundleId:/path`** (recommended) — explicit bundle ID and container path, e.g. `com.6alabat.cuisineApp:/Documents/`
    - **`/path`** (fallback) — uses the running task's `pkgName` as the bundle ID
  - Retrieves the current `TestRun` from `TestRunThreadContext` to determine the result folder
  - **Creates a named subfolder** matching the last component of the remote path (e.g. `/Documents/` → `Documents/`, `/Library/Caches/` → `Caches/`) so pulled files are organized separately from logs, crash reports, and video recordings. Falls back to `pulled_files/` if the remote path is just `/`.
  - Delegates the actual file transfer to `IOSUtils.pullFileFromApp()`
- **`IOSUtils.java` — `pullFileFromApp()`**: New static helper method that:
  - Creates the local target directory if it doesn't exist (`mkdirs()`)
  - Executes `python3 -m pymobiledevice3 apps pull --udid <UDID> <bundleId> <remotePath> <localPath>`
  - This is the iOS equivalent of Android's `adb pull` — it uses the AFC (Apple File Conduit) protocol to access the app's sandboxed container

### Path Resolution Logic
```
Remote Path          → Subfolder Name   → Local Path
/Documents/          → Documents         → <resultFolder>/Documents/
/Library/Caches/     → Caches            → <resultFolder>/Caches/
/tmp/logs/           → logs              → <resultFolder>/logs/
/                    → pulled_files      → <resultFolder>/pulled_files/
```

### Why
HydraLab's Android implementation uses `adb pull <path> <resultFolder>`, where `adb pull` preserves the directory name automatically. The iOS equivalent (`pymobiledevice3 apps pull`) dumps files flat into the target directory. Without the subfolder logic, pulled files (e.g. `tti_performance.json`, `fwfv2_db_*`) would mix with test artifacts like crash reports (`Crash/`), videos (`merged_test.mp4`), and log files in the result folder root — making it difficult to distinguish test output from HydraLab-generated artifacts.

The subfolder enhancement ensures parity with Android's behavior: pulled files are cleanly separated in a named subdirectory.

### How to use
Add a `pullFileFromDevice` tearDown action to your test task:
```json
{
  "deviceActions": {
    "tearDown": [
      {
        "deviceType": "IOS",
        "method": "pullFileFromDevice",
        "args": ["com.6alabat.cuisineApp:/Documents/"]
      }
    ]
  }
}
```

Result folder structure after test execution:
```
storage/test/result/YYYY/MM/DD/<timestamp>/<udid>/
├── Documents/              ← pulled files land here
│   ├── tti_performance.json
│   ├── fwfv2_db_cache.json
│   └── ...
├── Crash/                  ← crash reports (separate)
├── LegacyCrash/            ← legacy crash reports (separate)
├── merged_test.mp4         ← video recording
├── xctest_output.log       ← test logs
└── ...
```

---

## Summary of All Files Modified

| File                                                                    | Phases  |
|-------------------------------------------------------------------------|---------|
| `common/.../entity/agent/EnvCapability.java`                            | 1       |
| `common/.../management/device/impl/IOSDeviceDriver.java`               | 1       |
| `common/.../util/IOSUtils.java`                                        | 1, 2, 4 |
| `common/.../util/ZipBombChecker.java`                                   | 3       |
| `common/.../screen/IOSAppiumScreenRecorderForMac.java`                  | 2, 4    |
| `common/.../management/AppiumServerManager.java`                        | 4       |
| `agent/.../runner/xctest/XCTestRunner.java`                             | 5       |
| `agent/.../runner/xctest/XCTestCommandReceiver.java`                    | 5       |
| `agent/.../scheduled/ScheduledDeviceControlTasks.java`                  | 6       |
| `agent/.../service/DeviceControlService.java`                           | 6       |
| `common/.../management/device/impl/IOSDeviceDriver.java`               | 1, 7    |
| `common/.../util/IOSUtils.java`                                        | 1, 2, 4, 7 |
| `scripts/install_wda.sh`                                                | 4       |
| `scripts/install_wda_below_ios_17.sh`                                   | 4       |
| `scripts/cleanup_ios_ports.sh`                                          | 4       |
| `docs/iOS-Testing-Guide.md`                                             | 4, 7    |
| `TIDEVICE_TO_PYMOBILEDEVICE3_MIGRATION.md`                             | 1       |
