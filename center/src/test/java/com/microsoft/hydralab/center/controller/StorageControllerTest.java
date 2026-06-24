package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.controller.StorageController;
import com.microsoft.hydralab.center.service.StorageTokenManageService;
import com.microsoft.hydralab.center.util.LocalStorageIOUtil;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class StorageControllerTest {

    @Mock
    private StorageTokenManageService storageTokenManageService;
    @Mock
    private Logger logger;
    @InjectMocks
    private StorageController storageController;
    @Mock
    private MockHttpServletRequest request;
    @Mock
    private MockHttpServletResponse response;
    @Mock
    private MultipartFile uploadedFile;

    @Before
    public void setup() {
        storageController = new StorageController();
    }

    @Test
    public void testUploadFile() throws IOException {
        String fileUri = "testFile.txt";
        String storageToken = "Bearer token";
        InputStream inputStream = Mockito.mock(InputStream.class);
        Result expectedResult = Result.ok(fileUri);
        when(request.getHeader("Authorization")).thenReturn(storageToken);
        when(storageTokenManageService.validateAccessToken(storageToken)).thenReturn(true);
        when(uploadedFile.getInputStream()).thenReturn(inputStream);
        Result result = storageController.uploadFile(request, uploadedFile, fileUri);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testPostDownloadFile() throws IOException {
        String fileUri = "example/file.txt";
        File file = mock(File.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(storageController.storageTokenManageService.validateAccessToken("token")).thenReturn(true);
        when(LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)).thenReturn(true);
        when(new File(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT + fileUri)).thenReturn(file);
        when(file.exists()).thenReturn(true);
        when(file.getName()).thenReturn("file.txt");
        storageController.postDownloadFile(request, response, fileUri);
        verify(response).reset();
        verify(response).setContentType("application/octet-stream");
        verify(response).setCharacterEncoding("utf-8");
        verify(response).setContentLength((int) file.length());
        verify(response).setHeader("Content-Disposition", "attachment;filename=file.txt");
        verify(response.getOutputStream(), times(1)).write(any(byte[].class), anyInt(), anyInt());
        verify(storageController.logger).info(String.format("Output file: %s , size: %d!", fileUri, resLen));
    }
}