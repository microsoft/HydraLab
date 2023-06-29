package com.microsoft.hydralab.common.network;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelReceiver;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.microsoft.hydralab.common.util.NetworkMonitorUtil.*;

public class AndroidNetworkMonitor implements NetworkMonitor {

    protected DeviceDriver deviceDriver;
    protected ADBOperateUtil adbOperateUtil;
    protected DeviceInfo deviceInfo;
    protected String rule;
    protected File resultFolder;
    protected Logger logger;

    public AndroidNetworkMonitor(DeviceDriver deviceDriver, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, String rule, File resultFolder, Logger logger) {
        this.deviceDriver = deviceDriver;
        this.adbOperateUtil = adbOperateUtil;
        this.deviceInfo = deviceInfo;
        this.rule = rule;
        this.resultFolder = resultFolder;
        this.logger = logger;
    }

    @Override
    public void start() {
        // launch vpn
        String command_launch = "adb shell am start";
        command_launch += " -a com.microsoft.hydralab.android.client.vpn.START";
        command_launch += " -n com.microsoft.hydralab.android.client/.MainActivity";
        ShellUtils.execLocalCommandWithResult(command_launch, logger);
        while (true) {
            ThreadUtils.safeSleep(1000);
            boolean clicked = grantPermissionClick();
            if (!clicked) {
                break;
            }
        }

        // start vpn
        String command_start = "adb shell am start";
        command_start += " -a com.microsoft.hydralab.android.client.vpn.START";
        command_start += " -n com.microsoft.hydralab.android.client/.MainActivity";
        command_start += String.format(" --es \"apps\" \"%s\"", rule);
        command_start += " --es \"output\" \"" + AndroidDumpPath + "\"";
        ShellUtils.execLocalCommandWithResult(command_start, logger);
        while (true) {
            ThreadUtils.safeSleep(1000);
            boolean clicked = grantPermissionClick();
            if (!clicked) {
                break;
            }
        }
    }

    @Override
    public void stop() {
        // stop vpn
        String command_stop = "adb shell am start";
        command_stop += " -a com.microsoft.hydralab.android.client.vpn.STOP";
        command_stop += " -n com.microsoft.hydralab.android.client/.MainActivity";
        ShellUtils.execLocalCommandWithResult(command_stop, logger);
        ThreadUtils.safeSleep(2000);

        // pull result
        String local_dump_path = resultFolder.getAbsolutePath() + DumpPath;
        String command_result = "adb pull /sdcard/Documents/dump.log " + local_dump_path;
        ShellUtils.execLocalCommandWithResult(command_result, logger);
        ThreadUtils.safeSleep(2000);

        // parse
        String local_result_path = resultFolder.getAbsolutePath() + ResultPath;
        String[] lines;
        try {
            int count = 0;
            lines = Files.readAllLines(Paths.get(local_dump_path)).toArray(new String[0]);
            for (String line : lines) {
                if (!line.isEmpty()) {
                    ++count;
                }
            }
            File file = new File(local_result_path);
            FileWriter writer = new FileWriter(file);
            writer.write(count > 0 ? "Fail" : "Success");
            writer.close();
        } catch (IOException e) {
            // todo
        }
    }

    private boolean grantPermissionClick() {
        String[] possibleTexts = { "Start now", "Allow", "允许" };
        String dump = dumpView(deviceInfo, logger);
        // classLogger.info("Dump on {}: {}", adbDeviceInfo.getSerialNum(), dump);
        if (StringUtils.isBlank(dump)) {
            logger.error("did not find element with text {} on {}", Arrays.asList(possibleTexts).toString(),
                    deviceInfo.getSerialNum());
            return false;
        }
        Document viewTree = Jsoup.parse(dump, "", Parser.xmlParser());
        for (String possibleText : possibleTexts) {
            Elements startNowNode = viewTree.select(String.format("node[text=\"%s\"]", possibleText));
            if (!startNowNode.isEmpty()) {
                Element element = startNowNode.get(0);
                String bounds = element.attr("bounds");
                String[] boundsVal = bounds.split("[\\[\\],]+");
                int xStart = Integer.parseInt(boundsVal[1]);
                int yStart = Integer.parseInt(boundsVal[2]);
                int xEnd = Integer.parseInt(boundsVal[3]);
                int yEnd = Integer.parseInt(boundsVal[4]);
                int clickX = (xStart + xEnd) / 2;
                int clickY = (yStart + yEnd) / 2;
                adbOperateUtil.clickOnDeviceAbsoluteXY(deviceInfo, clickX, clickY, logger);
                return true;
            }
        }
        return false;
    }

    private String dumpView(DeviceInfo deviceInfo, Logger logger) {
        StringBuilder sb = new StringBuilder();
        adbOperateUtil.execOnDevice(deviceInfo, "uiautomator dump", new MultiLineNoCancelLoggingReceiver(logger),
                logger);
        adbOperateUtil.execOnDevice(deviceInfo, "cat /sdcard/window_dump.xml", new MultiLineNoCancelReceiver() {
            @Override
            public void processNewLines(@NotNull String[] lines) {
                for (String line : lines) {
                    sb.append(line);
                }
            }
        }, logger);
        return sb.toString();
    }
}
