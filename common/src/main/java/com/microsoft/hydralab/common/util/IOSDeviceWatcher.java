// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.management.device.impl.IOSDeviceDriver;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class IOSDeviceWatcher extends CommandOutputReceiver {

    private final WeakReference<IOSDeviceDriver> iosDeviceDriverRef;

    public IOSDeviceWatcher(InputStream inputStream, Logger logger, IOSDeviceDriver iosDeviceDriver) {
        super(inputStream, logger);
        this.iosDeviceDriverRef = new WeakReference<>(iosDeviceDriver);
    }

    @Override
    protected boolean handleEachLine(String line) {
        if (iosDeviceDriverRef.get() == null) {
            return true;
        }
        if (line.contains("MessageType")) {
            Objects.requireNonNull(iosDeviceDriverRef.get()).updateAllDeviceInfo();
        }
        return super.handleEachLine(line);
    }
}
