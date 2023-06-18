package com.microsoft.hydralab.network;

import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestRunDeviceCombo;
import com.microsoft.hydralab.common.management.device.impl.DeviceDriverManager;
import com.microsoft.hydralab.common.util.ShellUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class NetworkTestRecords {
    public Map<String, String[]> deviceToRecords = new HashMap<>();
}

@Service
public class NetworkTestManagementService {
    @Resource
    DeviceDriverManager deviceDriverManager;
    public boolean start(@NotNull TestRunDevice testRunDevice, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            for (TestRunDevice d : ((TestRunDeviceCombo) testRunDevice).getDevices()) {
                this.startForDevice(d, logger);
            }
        } else {
            this.startForDevice(testRunDevice, logger);
        }
        return true;
    }

    public NetworkTestRecords stop(
            @NotNull TestRunDevice testRunDevice, String resultFolder,  @Nullable Logger logger) {
        NetworkTestRecords records = new NetworkTestRecords();
        if (testRunDevice instanceof TestRunDeviceCombo) {
            for (TestRunDevice d : ((TestRunDeviceCombo) testRunDevice).getDevices()) {
                records.deviceToRecords.put(d.getDeviceInfo().getDeviceId(), this.stopForDevice(d, resultFolder, logger));
            }
        } else {
            records.deviceToRecords.put(testRunDevice.getDeviceInfo().getDeviceId(), this.stopForDevice(testRunDevice, resultFolder, logger));
        }
        return records;
    }

    private boolean startForDevice(@NotNull TestRunDevice device, @Nullable Logger logger) {
        String packagePath = "";

        // install vpn
        deviceDriverManager.installApp(device.getDeviceInfo(), packagePath, logger);

        // launch vpn
        String command_launch = "adb shell am start";
        command_launch += " -a studio.hydralab.vpnservice.START";
        command_launch += " -n studio.hydralab.vpnservice/.MainActivity";
        ShellUtils.execLocalCommandWithResult(command_launch, logger);

        // start vpn
        String command_start = "adb shell am start";
        command_start += " -a studio.hydralab.vpnservice.START";
        command_start += " -n studio.hydralab.vpnservice/.MainActivity";
        command_start += String.format(" --es \"apps\" \"%s\"", "com.microsoft.appmanager");
        command_start += " --es \"output\" \"/Documents/dump.log\"";
        ShellUtils.execLocalCommandWithResult(command_start, logger);

        return true;
    }

    private String[] stopForDevice(
            @NotNull TestRunDevice device, String resultFolder, @Nullable Logger logger) {
        // stop vpn
        String command_stop = "adb shell am start -a studio.hydralab.vpnservice.STOP -n studio.hydralab.vpnservice/.MainActivity";
        ShellUtils.execLocalCommandWithResult(command_stop, logger);

        // pull result
        String command_result = "adb pull /sdcard/Documents/dump.log " + resultFolder + "/dump.log";
        ShellUtils.execLocalCommandWithResult(command_result, logger);

        // read log file
        String[] lines;
        try {
            lines = Files.readAllLines(Paths.get("./dump.log")).toArray(new String[0]);
        } catch (IOException e) {
            lines = new String[0];
        }
        return lines;
    }
}
