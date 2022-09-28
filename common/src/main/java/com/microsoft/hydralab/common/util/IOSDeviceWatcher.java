// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
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

    private final WeakReference<IOSDeviceManager> iosDeviceManagerRef;

    public IOSDeviceWatcher(InputStream inputStream, Logger logger, IOSDeviceManager iosDeviceManager) {
        this.inputStream = inputStream;
        this.logger = logger;
        this.iosDeviceManagerRef = new WeakReference<>(iosDeviceManager);
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, "GBK");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (iosDeviceManagerRef.get() == null) break;
                if (line.contains("MessageType")) {
                    Objects.requireNonNull(iosDeviceManagerRef.get()).updateAllDeviceInfo();
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
