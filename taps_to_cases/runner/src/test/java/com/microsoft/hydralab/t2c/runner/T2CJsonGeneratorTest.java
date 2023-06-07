// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author taoran
 * @date 5/10/2023
 */

public class T2CJsonGeneratorTest {
    @Test
    public void testGenerateT2CJsonFromGraphXml() throws Exception {
        FileInputStream in = new FileInputStream("src/test/resources/directed_acyclic_graph.gexf");
        String graphXml = IOUtils.toString(in, StandardCharsets.UTF_8);
        String path = "0,2,3";
        String packageName = "com.microsoft.appmanager";
        String deviceType = "android";
        String result = T2CJsonGenerator.generateT2CJsonFromGraphXml(graphXml, path, null, packageName, deviceType);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("android.widget.Button"));
    }
}
