// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.util;

import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * @author Li Shen
 * @date 6/10/2025
 */

public final class ClamAVScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClamAVScanner.class);
    private static volatile ClamAVScanner instance;
    private ClamavClient client;

    private ClamAVScanner() {}

    public static ClamAVScanner getInstance() {
        if (instance == null) {
            synchronized (ClamAVScanner.class) {
                if (instance == null) {
                    instance = new ClamAVScanner();
                    instance.client = new ClamavClient("localhost", 3310);
                }
            }
        }
        return instance;
    }

    public void scan(String fileName, InputStream is) throws HydraLabRuntimeException {
        int timeout = 3000; // milliseconds, default timeout for checking ClamAV connection

        if (this.client.isReachable(timeout)) {
            ScanResult res = client.scan(is);
            if (res instanceof ScanResult.OK) {
                LOGGER.info("File {} scanned successfully, no virus found.", fileName);
                return;
            } else if (res instanceof ScanResult.VirusFound) {
                Map<String, Collection<String>> viruses = ((ScanResult.VirusFound) res).getFoundViruses();
                LOGGER.warn("{} viruses found in file {}: {}", viruses.size(), fileName, res);
                StringBuilder sb = new StringBuilder();
                viruses.forEach((virusName, virusDetails) -> {
                    sb.append("Virus: ").append(virusName).append("\n");
                    if (virusDetails != null && !virusDetails.isEmpty()) {
                        sb.append("Details: ").append(String.join(", ", virusDetails)).append("\n");
                    }
                });
                throw new HydraLabRuntimeException(
                        HttpStatus.BAD_REQUEST.value(),
                        "Viruses found in file " + fileName + ": \n" + sb
                );
            }
        } else {
            // todo: restart or ignore?
            LOGGER.error("ClamAV is shutdown");
            throw new HydraLabRuntimeException("ClamAV is shutdown");
        }
    }
}
