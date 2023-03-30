// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.img.ImgUtil;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestRunDeviceCombo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.impl.DeviceDriverManager;
import com.microsoft.hydralab.common.screen.FFmpegConcatUtil;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ImageUtil;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TestRunDeviceOrchestrator {
    @Resource
    DeviceDriverManager deviceDriverManager;
    @Resource
    ActionExecutor actionExecutor;

    public AppiumServerManager getAppiumServerManager() {
        return deviceDriverManager.getAppiumServerManager();
    }

    public File getScreenShot(@NotNull TestRunDevice testRunDevice, @NotNull File screenshotDir, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            List<File> screenShots = new ArrayList<>();
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> screenShots.add(this.getScreenShot(testRunDevice1, screenshotDir, logger)));
            return ImageUtil.joinImages(screenshotDir, "merged_pic.jpg", screenShots);
        } else {
            return deviceDriverManager.getScreenShot(testRunDevice.getDeviceInfo(), logger);
        }
    }

    public void wakeUpDevice(@NotNull TestRunDevice testRunDevice, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.wakeUpDevice(testRunDevice1, logger));
        } else {
            deviceDriverManager.wakeUpDevice(testRunDevice.getDeviceInfo(), logger);
        }
    }

    public boolean installApp(@NotNull TestRunDevice testRunDevice, @NotNull String packagePath, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            boolean isInstalled = true;
            for (TestRunDevice testRunDevice1 : ((TestRunDeviceCombo) testRunDevice).getDevices()) {
                isInstalled = isInstalled && this.installApp(testRunDevice1, packagePath, logger);
            }
            return isInstalled;
        } else {
            return deviceDriverManager.installApp(testRunDevice.getDeviceInfo(), packagePath, logger);
        }
    }

    public void uninstallApp(@NotNull TestRunDevice testRunDevice, @NotNull String packageName, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.uninstallApp(testRunDevice1, packageName, logger));
        } else {
            deviceDriverManager.uninstallApp(testRunDevice.getDeviceInfo(), packageName, logger);
        }
    }

    public void resetPackage(@NotNull TestRunDevice testRunDevice, @NotNull String packageName, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.resetPackage(testRunDevice1, packageName, logger));
        } else {
            deviceDriverManager.resetPackage(testRunDevice.getDeviceInfo(), packageName, logger);
        }
    }

    public void startScreenRecorder(@NotNull TestRunDevice testRunDevice, @NotNull File folder, int maxTimeInSecond, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> {
                File childFolder = new File(folder, testRunDevice1.getDeviceInfo().getSerialNum());
                this.startScreenRecorder(testRunDevice1, childFolder, maxTimeInSecond, logger);
            });
        } else {
            ScreenRecorder screenRecorder = deviceDriverManager.getScreenRecorder(testRunDevice.getDeviceInfo(), folder, logger);
            screenRecorder.setupDevice();
            screenRecorder.startRecord(maxTimeInSecond);
            testRunDevice.setScreenRecorder(screenRecorder);
        }
    }

    public String stopScreenRecorder(@NotNull TestRunDevice testRunDevice, @NotNull File folder, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            List<String> videoFilePaths = new ArrayList<>();
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> {
                String path = this.stopScreenRecorder(testRunDevice1, folder, logger);
                if (path != null && !path.isEmpty()) {
                    videoFilePaths.add(path);
                }
            });
            return FFmpegConcatUtil.mergeVideosSideBySide(videoFilePaths, folder, logger).getAbsolutePath();
        } else {
            return testRunDevice.getScreenRecorder().finishRecording();
        }
    }

    public void startLogCollector(@NotNull TestRunDevice testRunDevice, @NotNull String pkgName, @NotNull TestRun testRun, @NotNull Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> {
                //todo: need to generate a new log path for each device
                this.startLogCollector(testRunDevice1, pkgName, testRun, logger);
            });
        } else {
            LogCollector logCollector = deviceDriverManager.getLogCollector(testRunDevice.getDeviceInfo(), pkgName, testRun, logger);
            testRunDevice.setLogCollector(logCollector);
            testRunDevice.setLogPath(logCollector.start());
        }
    }

    public void stopLogCollector(@NotNull TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(this::stopLogCollector);
        } else {
            testRunDevice.getLogCollector().stopAndAnalyse();
        }
    }

    public void testDeviceSetup(@NotNull TestRunDevice testRunDevice, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> {
                this.testDeviceSetup(testRunDevice1, logger);
            });
        } else {
            deviceDriverManager.testDeviceSetup(testRunDevice.getDeviceInfo(), logger);
        }
    }

    public void testDeviceUnset(@NotNull TestRunDevice testRunDevice, Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.testDeviceUnset(testRunDevice1, logger));
        } else {
            deviceDriverManager.testDeviceUnset(testRunDevice.getDeviceInfo(), logger);
        }
    }

    public void setRunningTestName(TestRunDevice testRunDevice, String runningTestName) {
        testRunDevice.getDeviceInfo().setRunningTestName(runningTestName);
    }

    public String getSerialNum(@NotNull TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            String serialNum = testRunDevice.getDeviceInfo().getSerialNum();
            for (TestRunDevice temp : ((TestRunDeviceCombo) testRunDevice).getPairedDevices()) {
                serialNum += "," + temp.getDeviceInfo().getSerialNum();
            }
            return serialNum;
        } else {
            return testRunDevice.getDeviceInfo().getSerialNum();
        }
    }

    public String getName(@NotNull TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            StringBuilder name = new StringBuilder(testRunDevice.getDeviceInfo().getName());
            for (TestRunDevice temp : ((TestRunDeviceCombo) testRunDevice).getPairedDevices()) {
                name.append("-").append(temp.getDeviceInfo().getName());
            }
            return name.toString();
        } else {
            return testRunDevice.getDeviceInfo().getName();
        }
    }

    public void killAll(TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(this::killAll);
        } else {
            testRunDevice.getDeviceInfo().killAll();
        }
    }

    public void addCurrentTask(@NotNull TestRunDevice testRunDevice, TestTask testTask) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.addCurrentTask(testRunDevice1, testTask));
        } else {
            testRunDevice.getDeviceInfo().addCurrentTask(testTask);
        }
    }

    public void finishTask(@NotNull TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(this::finishTask);
        } else {
            testRunDevice.getDeviceInfo().finishTask();
        }
    }

    public void getAppiumDriver(@NotNull TestRunDevice testRunDevice, @NotNull Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.getAppiumDriver(testRunDevice1, logger));
        } else {
            testRunDevice.setWebDriver(deviceDriverManager.getAppiumDriver(testRunDevice.getDeviceInfo(), logger));
        }
    }

    public void quitAppiumDriver(@NotNull TestRunDevice testRunDevice, @NotNull Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.quitAppiumDriver(testRunDevice1, logger));
        } else {
            deviceDriverManager.quitAppiumDriver(testRunDevice.getDeviceInfo(), logger);
        }
    }

    public void addGifFrameAsyncDelay(@NotNull TestRunDevice testRunDevice, @NotNull File screenshotDir, int delaySeconds, @NotNull Logger logger) {
        ThreadPoolUtil.SCREENSHOT_EXECUTOR.execute(() -> {
            ThreadUtils.safeSleep(TimeUnit.SECONDS.toMillis(delaySeconds));
            File imageFile = getScreenShot(testRunDevice, screenshotDir, logger);
            if (imageFile == null || !testRunDevice.getGifEncoder().isStarted()) {
                return;
            }
            try {
                testRunDevice.getGifEncoder().addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imageFile), 0.3f)));
                testRunDevice.setGifFrameCount(testRunDevice.getGifFrameCount() + 1);
            } catch (IOException e) {
                logger.error("Failed to add frame to gif", e);
            }
        });
    }

    public void grantAllTaskNeededPermissions(@NotNull TestRunDevice testRunDevice, @NotNull TestTask testTask, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.grantAllTaskNeededPermissions(testRunDevice1, testTask, logger));
        } else {
            deviceDriverManager.grantAllTaskNeededPermissions(testRunDevice.getDeviceInfo(), testTask, logger);
        }
    }

    public void startGifEncoder(@NotNull TestRunDevice testRunDevice, @NotNull File resultFolder, @NotNull String fileName) {
        File gifFile = new File(resultFolder, fileName);
        testRunDevice.setGifFile(gifFile);
        testRunDevice.getGifEncoder().start(gifFile.getAbsolutePath());
        testRunDevice.getGifEncoder().setDelay(1000);
        testRunDevice.getGifEncoder().setRepeat(0);
    }

    public void stopGitEncoder(@NotNull TestRunDevice testRunDevice, @NotNull File screenshotDir, @NotNull Logger logger) {
        if (!testRunDevice.getGifEncoder().isStarted()) {
            return;
        }
        if (testRunDevice.getGifFrameCount() < 2) {
            addGifFrameAsyncDelay(testRunDevice, screenshotDir, 0, logger);
        }
        testRunDevice.getGifEncoder().finish();
    }

    public void backToHome(@NotNull TestRunDevice testRunDevice, @Nullable Logger logger) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            ((TestRunDeviceCombo) testRunDevice).getDevices().forEach(testRunDevice1 -> this.backToHome(testRunDevice1, logger));
        } else {
            deviceDriverManager.backToHome(testRunDevice.getDeviceInfo(), logger);
        }
    }

    public boolean isAlive(@NotNull TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            Boolean isAlive = true;
            for (TestRunDevice testRunDevice1 : ((TestRunDeviceCombo) testRunDevice).getDevices()) {
                isAlive = isAlive && this.isAlive(testRunDevice1);
            }
            return isAlive;
        } else {
            return testRunDevice.getDeviceInfo().isAlive();
        }
    }

    public boolean isTesting(@NotNull TestRunDevice testRunDevice) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            Boolean isTesting = false;
            for (TestRunDevice testRunDevice1 : ((TestRunDeviceCombo) testRunDevice).getDevices()) {
                isTesting = isTesting || this.isTesting(testRunDevice1);
            }
            return isTesting;
        } else {
            return testRunDevice.getDeviceInfo().isTesting();
        }
    }

    public Logger getDeviceLogger(TestRunDevice testRunDevice) {
        return deviceDriverManager.getDeviceLogger(testRunDevice.getDeviceInfo());
    }

    public List<Exception> doActions(TestRunDevice testRunDevice, Logger logger, Map<String, List<DeviceAction>> deviceActions, String tearDown) {
        if (testRunDevice instanceof TestRunDeviceCombo) {
            List<Exception> exceptions = new ArrayList<>();
            for (TestRunDevice testRunDevice1 : ((TestRunDeviceCombo) testRunDevice).getDevices()) {
                exceptions.addAll(actionExecutor.doActions(deviceDriverManager, testRunDevice, logger, deviceActions, tearDown));
            }
            return exceptions;
        } else {
            return actionExecutor.doActions(deviceDriverManager, testRunDevice, logger, deviceActions, tearDown);
        }

    }
}