// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipBombChecker {

    private static final int THRESHOLD_ENTRIES = 10000; // Limit the number of entries
    private static final long THRESHOLD_SIZE = 1_000_000_000L; // Limit the uncompressed size (1 GB)
    private static final double THRESHOLD_RATIO = 10; // Limit the compression ratio
    private static final int BUFFER_SIZE = 2048; // Buffer size for reading data

    private static void extractZip(String zipFilePath, String destDirectory) throws IOException {
        int totalEntryArchive = 0;
        long totalSizeArchive = 0;

        try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                totalEntryArchive++;

                // Check for entry count limit
                if (totalEntryArchive > THRESHOLD_ENTRIES) {
                    throw new IOException("Too many entries in the archive. Possible Zip Bomb attack.");
                }

                // Get the uncompressed size (if available)
                long uncompressedSize = entry.getSize();

                // Check for compression ratio limit (if compressed size is available)
                if (entry.getCompressedSize() != -1 && uncompressedSize != -1) {
                    double compressionRatio = (double) uncompressedSize / entry.getCompressedSize();
                    if (compressionRatio > THRESHOLD_RATIO) {
                        throw new IOException("Suspicious compression ratio. Possible Zip Bomb attack.");
                    }
                }

                // Check for total uncompressed size limit
                totalSizeArchive += uncompressedSize;
                if (totalSizeArchive > THRESHOLD_SIZE) {
                    throw new IOException("Total uncompressed size exceeds the limit. Possible Zip Bomb attack.");
                }

                // Extract the entry (with size checks)
                if (!entry.isDirectory()) {
                    extractFile(zipIn, destDirectory + File.separator + entry.getName(), uncompressedSize);
                } else {
                    File dir = new File(destDirectory + File.separator + entry.getName());
                    dir.mkdir();
                }

                zipIn.closeEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath, long uncompressedSize) throws IOException {
        long totalSizeEntry = 0;
        try (OutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
                totalSizeEntry += read;

                // Check for individual entry size limit (if uncompressed size is known)
                if (uncompressedSize != -1 && totalSizeEntry > uncompressedSize) {
                    throw new IOException("Uncompressed size exceeds the reported size. Possible Zip Bomb attack.");
                }
            }
        }
    }

    public static boolean checkZipBomb(File file) {
        // Check if the file is a valid ZIP file
        if (!file.exists() || !file.isFile() || !file.getName().endsWith(".zip")) {
            return false; // Not a valid ZIP file
        }
        try {
            // Create a temporary directory for extraction
            File tempDir = new File(file.getParent(), UUID.randomUUID().toString().replace("-", ""));
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                throw new IOException("Failed to create temporary directory for extraction.");
            }

            // Extract the ZIP file and check for Zip Bomb conditions
            extractZip(file.getAbsolutePath(), tempDir.getAbsolutePath());

            // Clean up the temporary directory
            FileUtil.deleteFile(tempDir);
            return true; // No Zip Bomb detected
        } catch (IOException e) {
            // Log the exception or handle it as needed
            return false; // Zip Bomb detected or error occurred
        }
    }
}
