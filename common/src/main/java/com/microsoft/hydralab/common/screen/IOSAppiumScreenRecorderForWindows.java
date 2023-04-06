// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.microsoft.hydralab.common.util.ShellUtils.POWER_SHELL_PATH;

public class IOSAppiumScreenRecorderForWindows extends IOSAppiumScreenRecorder {
    private static File scriptFile;
    private final Timer timer = new Timer();
    private Process recordProcess;
    private String destPath;

    public IOSAppiumScreenRecorderForWindows(DeviceDriver deviceDriver, DeviceInfo info, String recordDir) {
        super(deviceDriver, info, recordDir);
    }

    public static void copyScript(File testBaseDir) {
        // copy script
        String name = INTERRUPT_SCRIPT_PATH;
        scriptFile = new File(testBaseDir, name);
        if (scriptFile.exists()) {
            scriptFile.delete();
        }
        try (InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(name); OutputStream out = new FileOutputStream(scriptFile)) {
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startRecord(int maxTimeInSecond) {
        int timeout = maxTimeInSecond > 0 ? maxTimeInSecond : DEFAULT_TIMEOUT_IN_SECOND;
        destPath = new File(recordDir, Const.ScreenRecoderConfig.DEFAULT_FILE_NAME).getAbsolutePath();
        try {
            iosDriver.startRecordingScreen();
            recordProcess = ShellUtils.execLocalCommand("ffmpeg -f mjpeg -i http://127.0.0.1:" + IOSUtils.getMjpegServerPortByUdid(deviceInfo.getSerialNum(), CLASS_LOGGER, deviceInfo) + " -vf scale=720:360 -vcodec h264 -y " + destPath, false, CLASS_LOGGER);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stopRecord();
                }
            }, timeout * 1000L);
            deviceInfo.addCurrentProcess(recordProcess);
            isStarted = true;
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Start recording, Ignore it to unblocking the following tests----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
        }
    }

    @Override
    public String finishRecording() {
        timer.cancel();
        return stopRecord();
    }

    private String stopRecord() {
        if (!isStarted) {
            return null;
        }
        try {
            // wait 5s to record more info after testing
            ThreadUtils.safeSleep(5000);
            CLASS_LOGGER.info("Stopping recording");
            synchronized (this) {
                iosDriver.stopRecordingScreen();
                if (recordProcess != null) {
                    long pid = recordProcess.pid();
                    ShellUtils.execLocalCommand(POWER_SHELL_PATH + " -Command " + scriptFile.getPath() + " " + pid, CLASS_LOGGER);
                    recordProcess = null;
                }
                isStarted = false;
            }
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Stop recording, Ignore it to unblocking the following tests-----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
            return null;
        }
        return destPath;
    }

}
