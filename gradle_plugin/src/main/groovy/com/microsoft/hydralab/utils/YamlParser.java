// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.hydralab.config.DeviceConfig;
import com.microsoft.hydralab.config.HydraLabAPIConfig;
import com.microsoft.hydralab.config.TestConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * @author Li Shen
 * @date 2/9/2023
 */

public class YamlParser {
    private final LinkedHashMap fileRootMap;
    private final ObjectMapper objectMapper;

    public YamlParser(String configFile) throws IOException {
        File ymlFile = new File(configFile);
        InputStream inputStream = Files.newInputStream(ymlFile.toPath());
        Yaml yaml = new Yaml();
        this.fileRootMap = yaml.load(inputStream);

        this.objectMapper = new ObjectMapper();
        // ignore unknown fields in config yml
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public HydraLabAPIConfig parseAPIConfig() {
        Object target = fileRootMap.get("hydraLabAPIServer");
        return objectMapper.convertValue(target, HydraLabAPIConfig.class);
    }

    public TestConfig parseTestConfig() {
        Object target = fileRootMap.get("testSpec");
        TestConfig testConfig = objectMapper.convertValue(target, TestConfig.class);
        testConfig.constructField((HashMap<String, Object>) target);
        if (testConfig.deviceConfig != null) {
            testConfig.deviceConfig.extractFromExistingField();
        }
        return testConfig;
    }
}
