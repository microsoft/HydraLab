// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local.client;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageToken;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.RestTemplateConfig;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Li Shen
 * @date 3/6/2023
 */

@Data
public class LocalStorageClient {
    RestTemplate restTemplate;
    private String endpoint;
    private String token;

    public LocalStorageClient() {
    }

    public LocalStorageClient(LocalStorageProperty localStorageProperty) {
        this.endpoint = localStorageProperty.getEndpoint();
        if (this.endpoint.endsWith("/")) {
            this.endpoint = this.endpoint.substring(0, this.endpoint.length() - 1);
        }
        this.token = localStorageProperty.getToken();
    }

    public void init(LocalStorageToken localStorageToken) {
        this.endpoint = localStorageToken.getEndpoint();
        this.token = localStorageToken.getToken();
    }

    /**
     * Upload a file to the local storage. If the file already exists, overwrite it.
     * <p>
     * //     * @param file
     * //     * @param storageFileInfo
     *
     * @return file download (rel?) path
     */
    public String upload(File file, StorageFileInfo storageFileInfo) {
        RestTemplate restTemplateHttps = RestTemplateConfig.getRestTemplateInstance();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", MediaType.MULTIPART_FORM_DATA.toString());

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("container", storageFileInfo.getBlobContainer());
        body.add("fileRelPath", storageFileInfo.getBlobPath());
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseAsStr = restTemplateHttps.exchange(this.getUploadUrl(), HttpMethod.POST, entity, String.class);
        JSONObject responseObject = JSONObject.parseObject(responseAsStr.getBody());
        return this.endpoint + Const.LocalStorageURL.CENTER_LOCAL_STORAGE_DOWNLOAD + "/" + responseObject.getString("content");
    }

    /**
     * Download a file from the local storage. If the file already exists, overwrite it.
     * <p>
     * //     * @param downloadToFile
     * //     * @param containerName
     * //     * @param blobFilePath
     *
     * @return
     */
    public void download(File file, StorageFileInfo storageFileInfo) {
        RestTemplate restTemplateHttps = RestTemplateConfig.getRestTemplateInstance();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", MediaType.MULTIPART_FORM_DATA.toString());

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("container", storageFileInfo.getBlobContainer());
        body.add("fileRelPath", storageFileInfo.getBlobPath());
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Resource> response = restTemplateHttps.exchange(this.getDownloadUrl(), HttpMethod.POST, entity, Resource.class);
            IOUtils.copy(response.getBody().getInputStream(), new FileOutputStream(file));
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "File stream downloaded, but saved to local failed.", e);
        }
    }

    public String getUploadUrl() {
        return this.endpoint + Const.LocalStorageURL.CENTER_LOCAL_STORAGE_UPLOAD;
    }

    public String getDownloadUrl() {
        return this.endpoint + Const.LocalStorageURL.CENTER_LOCAL_STORAGE_DOWNLOAD;
    }

}
