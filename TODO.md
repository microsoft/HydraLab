# TODO

## iOS Device Hot-Plug Detection Missing

**Priority:** Medium
**Context:** pymobiledevice3 migration removed the real-time `tidevice usbmux watch` based device watcher but did not add a replacement. Currently, the agent must be restarted to detect newly connected iOS devices.

**Problem:**
- `IOSUtils.startIOSDeviceWatcher()` only triggers a one-time `updateAllDeviceInfo()` at startup.
- `IOSDeviceWatcher.java` (the old tidevice-based USB event listener) is no longer used.
- `pymobiledevice3 usbmux` does not have a `watch`/`listen` subcommand.
- `ScheduledDeviceControlTasks` has no periodic task calling `updateAllDeviceInfo()` for iOS.

**Proposed Fix:**
Add a `@Scheduled` task in `ScheduledDeviceControlTasks.java` that periodically calls `deviceControlService.updateAllDeviceInfo()` (e.g., every 10–15 seconds) so newly connected iOS devices are automatically discovered without an agent restart.

**Files involved:**
- `common/src/main/java/com/microsoft/hydralab/common/util/IOSUtils.java` (line 84–90)
- `common/src/main/java/com/microsoft/hydralab/common/util/IOSDeviceWatcher.java` (unused, can be removed)
- `agent/src/main/java/com/microsoft/hydralab/agent/scheduled/ScheduledDeviceControlTasks.java`
