// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author taoran
 * @date 5/9/2023
 */

public final class T2CJsonGenerator {
    // Utility classes should not have a public or default constructor.
    private T2CJsonGenerator() {
    }

    public static String generateT2CJsonFromGraphXml(String graphXml, String path, Logger logger, String packageName, String deviceType) throws Exception {
        if (graphXml == null || graphXml.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }

        String[] nodes = path.split(",");
        if (nodes.length < 2) {
            logger.error("Graph path [" + path + "] is not valid");
            return null;
        }

        List<String> elementStrList = new ArrayList<>();
        for (int i = 0; i < nodes.length - 1; i++) {
            String source = nodes[i];
            String target = nodes[i + 1];
            elementStrList.add(extract(graphXml, source, target));
        }

        //TODO for other device type
        return exportT2CJsonForAndroid(elementStrList, packageName);
    }

    public static String extract(String s, String source, String target) {
        String startEdge = "<edge source=\"" + source + "\" target=\"" + target + "\"";
        String startAttr = "value=\"&quot;";
        String edge = s.substring(s.indexOf(startEdge) + startEdge.length());
        String attr = edge.substring(edge.indexOf(startAttr) + startAttr.length());
        return attr.substring(0, attr.indexOf("&quot;"));
    }

    /*
     * Export action from element for Android
     * Example:
     *
      {
        "drivers": [
          {
            "id": "android",
            "platform": "android",
            "init": {
              "launcherApp": "com.example.android.apis",
              "enableMemoryTest": false,
              "enableBatteryTest": false
            }
          }
        ],
        "cases": [
            {
            "index": 8,
            "elementInfo": {
              "index": "0",
              "package": "com.android.settings",
              "class": "android.widget.ImageView",
              "text": "",
              "resource-id": "android:id/icon",
              "clickable": "false",
              "enabled": "true",
              "long-clickable": "false",
              "password": "false",
              "scrollable": "false",
            },
            "driverId": "android",
            "action": {
              "actionType": "click",
              "arguments": {}
            },
            "isOption": true
          }
        ]
       }
     *
     */
    public static String exportT2CJsonForAndroid(List<String> elementStrList, String packageName) {
        if (elementStrList == null || elementStrList.isEmpty()) {
            return "";
        }

        JSONObject t2cJsonObject = new JSONObject();

        //Driver list
        JSONArray drivers = new JSONArray();
        JSONObject driver = new JSONObject();
        driver.put("id", "android");
        driver.put("platform", "android");
        JSONObject init = new JSONObject();
        init.put("launcherApp", packageName);
        init.put("enableMemoryTest", false);
        init.put("enableBatteryTest", false);
        driver.put("init", init);
        drivers.add(driver);

        //Action list
        JSONArray cases = new JSONArray();
        for (int i = 0; i < elementStrList.size(); i++) {
            String element = elementStrList.get(i);
            String className = extractElementAttr("Class", element);
            String text = extractElementAttr("Text", element);
            String resourceId = extractElementAttr("ResourceID", element);
            String clickable = extractElementAttr("Clickable", element);

            //Creating case objects
            JSONObject case1 = new JSONObject();
            case1.put("index", i);
            case1.put("driverId", "android");
            case1.put("isOption", false);

            //Creating elementInfo object for case1
            JSONObject elementInfo = new JSONObject();
            elementInfo.put("index", "0");
            elementInfo.put("package", packageName);
            elementInfo.put("class", className);
            elementInfo.put("text", text);
            elementInfo.put("clickable", clickable.toLowerCase(Locale.ROOT));
            elementInfo.put("resource-id", resourceId);
            case1.put("elementInfo", elementInfo);

            JSONObject action = new JSONObject();
            //TODO: support multiple action types
            action.put("actionType", "click");

            JSONObject arguments = new JSONObject();
            action.put("arguments", arguments);

            case1.put("action", action);
            cases.add(case1);
        }

        t2cJsonObject.put("drivers", drivers);
        t2cJsonObject.put("cases", cases);
        return JSONObject.toJSONString(t2cJsonObject, SerializerFeature.PrettyFormat);
    }

    private static String extractElementAttr(String attrName, String elementStr) {
        String[] attrs = elementStr.split(attrName + ": ");
        if (attrs.length > 1 && !attrs[1].startsWith(",")) {
            return attrs[1].split(",")[0];
        }
        return "";
    }
}
