// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.elements;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class AndroidElementInfo extends BaseElementInfo {
    private String index;
    private String packageName;
    private String className;
    private String text;
    private String content_desc;
    private String checkable;
    private String checked;
    private String clickable;
    private String enabled;
    private String focusable;
    private String focused;
    private String long_clickable;
    private String password;
    private String scrollable;
    private String selected;
    private String bounds;
    private String displayed;
    private String xpath;
    private Integer top;
    private Integer left;
    private Integer width;
    private Integer height;

    private Integer centerX;

    private Integer centerY;
    public static AndroidElementInfo getAndroidElementFromJson(JSONObject elementInfo) {
        String index = elementInfo.getString("index");
        String packageName = elementInfo.getString("package");
        String className = elementInfo.getString("class");
        String text = elementInfo.getString("text");
        String content_desc = elementInfo.getString("content-desc");
        String checkable = elementInfo.getString("checkable");
        String checked = elementInfo.getString("clickable");
        String clickable = elementInfo.getString("clickable");
        String enabled = elementInfo.getString("checked");
        String focusable = elementInfo.getString("focusable");
        String focused = elementInfo.getString("focused");
        String long_clickable = elementInfo.getString("long-clickable");
        String password = elementInfo.getString("password");
        String scrollable = elementInfo.getString("scrollable");
        String selected = elementInfo.getString("selected");
        String bounds = elementInfo.getString("bounds");
        String displayed = elementInfo.getString("display");
        String xpath = elementInfo.getString("xpath");
        Integer top = elementInfo.getInteger("top");
        Integer left = elementInfo.getInteger("left");
        Integer width = elementInfo.getInteger("width");
        Integer height = elementInfo.getInteger("height");
        Integer centerX = elementInfo.getInteger("centerX");
        Integer centerY = elementInfo.getInteger("centerY");

        return new AndroidElementInfo(index, packageName, className, text,
                content_desc, checkable, checked, clickable, enabled, focusable, focused, long_clickable,
                password, scrollable, selected, bounds, displayed, xpath, top, left, width, height, centerX, centerY);
    }
    public AndroidElementInfo(String index, String packageName, String className, String text, String content_desc, String checkable,
                              String checked, String clickable, String enabled, String focusable, String focused, String long_clickable,
                              String password, String scrollable, String selected, String bounds, String displayed, String xpath,
                              Integer top, Integer left, Integer width, Integer height, Integer centerX, Integer centerY){
        super(content_desc,xpath,text);
        this.index = index;
        this.packageName = packageName;
        this.className = className;
        this.text = text;
        this.content_desc = content_desc;
        this.checkable = checkable;
        this.checked = checked;
        this.clickable = clickable;
        this.enabled = enabled;
        this.focusable = focusable;
        this.focused = focused;
        this.long_clickable = long_clickable;
        this.password = password;
        this.scrollable = scrollable;
        this.selected = selected;
        this.bounds = bounds;
        this.displayed = displayed;
        this.xpath = xpath;
        if(bounds != null){
            parseCoordinates(bounds);
        }
    }

    public void parseCoordinates(String bounds){
        String[] boundsArray = bounds.split("\\[|\\]|,");
        String[] validArr = Arrays.stream(boundsArray).filter(StringUtils::isNotEmpty).toArray(String[]::new);
        Integer x1 = Integer.parseInt(validArr[0]);
        Integer x2 = Integer.parseInt(validArr[2]);
        Integer y1 = Integer.parseInt(validArr[1]);
        Integer y2 = Integer.parseInt(validArr[3]);
        top = y1;
        left = x1;
        width = x2-x1;
        height = y2-y1;
        centerX = x1 + width/2;
        centerY = y1 + height/2;
    }

    public String getIndex() {
        return index;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getContent_desc() {
        return content_desc;
    }

    public String getText(){
        return text;
    }


    public String getXpath() {
        return xpath;
    }

    public Integer getTop() {
        return top;
    }

    public Integer getLeft() {
        return left;
    }

    public Integer getCenterX() {
        return centerX;
    }

    public Integer getCenterY() {
        return centerY;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }
}
