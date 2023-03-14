// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.file.local;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.center.util.LocalStorageIOUtil;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Li Shen
 * @date 3/14/2023
 */

public class LocalStorageIOTest extends BaseTest {
    File sampleFile = new File("src/test/resources/uitestsample.ipa");

    @Test
    public void uploadStream() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(sampleFile);
        String fileUri = "test/unit/" + sampleFile.getName();
        LocalStorageIOUtil.copyUploadedStreamToFile(fileInputStream, fileUri);

        int inputLen = (int) sampleFile.length();
        int outputLen = (int) new File(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT + fileUri).length();
        Assertions.assertEquals(inputLen, outputLen, "Upload stream failed!");
    }

    @Test
    public void downloadStream() throws IOException {
        String fileUri = "test/unit/" + sampleFile.getName();
        File downloadedFile = new File(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT + fileUri);
        File outputFile = new File("src/test/resources/outputStream.ipa");
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

        int inputLen = (int) downloadedFile.length();
        int resLen = LocalStorageIOUtil.copyDownloadedStreamToResponse(downloadedFile, fileOutputStream);
        Assertions.assertEquals(inputLen, resLen, "Download stream failed!");

        fileOutputStream.close();
        outputFile.delete();
    }
}
