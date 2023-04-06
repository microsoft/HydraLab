// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.management.device.impl.IOSDeviceDriver;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class IOSDeviceWatcher extends Thread {
    private final InputStream inputStream;
    private final Logger logger;

    private final WeakReference<IOSDeviceDriver> iosDeviceDriverRef;

    public IOSDeviceWatcher(InputStream inputStream, Logger logger, IOSDeviceDriver iosDeviceDriver) {
        this.inputStream = inputStream;
        this.logger = logger;
        this.iosDeviceDriverRef = new WeakReference<>(iosDeviceDriver);
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, "GBK");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (iosDeviceDriverRef.get() == null) {
                    break;
                }
                if (line.contains("MessageType")) {
                    Objects.requireNonNull(iosDeviceDriverRef.get()).updateAllDeviceInfo();
                }
                logger.info(line);
            }
            isr.close();
            bufferedReader.close();
        } catch (IOException e) {
            logger.info("Exception:" + e);
            e.printStackTrace();
        } finally {
            synchronized (this) {
                notify();
            }
        }
    }
}
