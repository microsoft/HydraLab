// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.elements;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class AndroidElementInfo extends BaseElementInfo {
    private String index;
    @JSONField(name = "package")
    private String packageName;
    @JSONField(name = "class")
    private String className;
    private String text;
    @JSONField(name = "content_desc")
    private String contentDesc;
    private String checkable;
    private String checked;
    private String clickable;
    private String enabled;
    private String focusable;
    private String focused;
    @JSONField(name = "long-clickable")
    private String longClickable;
    private String password;
    private String scrollable;
    private String selected;
    private String bounds;
    private String displayed;
    private String xpath;

    @JSONField(serialize = false, deserialize = false)
    private int top;
    @JSONField(serialize = false, deserialize = false)
    private int left;
    @JSONField(serialize = false, deserialize = false)
    private int width;
    @JSONField(serialize = false, deserialize = false)
    private int height;
    @JSONField(serialize = false, deserialize = false)
    private int centerX;
    @JSONField(serialize = false, deserialize = false)
    private int centerY;
    @JSONField(name = "resource-id")
    private String resourceId;

    public AndroidElementInfo(String index, String packageName, String className, String text, String contentDesc, String checkable,
                              String checked, String clickable, String enabled, String focusable, String focused, String long_clickable,
                              String password, String scrollable, String selected, String bounds, String displayed, String xpath,
                              String resourceId) {
        super(xpath);
        this.index = index;
        this.packageName = packageName;
        this.className = className;
        this.text = text;
        this.contentDesc = contentDesc;
        this.checkable = checkable;
        this.checked = checked;
        this.clickable = clickable;
        this.enabled = enabled;
        this.focusable = focusable;
        this.focused = focused;
        this.longClickable = long_clickable;
        this.password = password;
        this.scrollable = scrollable;
        this.selected = selected;
        this.bounds = bounds;
        this.displayed = displayed;
        this.xpath = xpath;
        this.resourceId = resourceId;
        if(bounds != null){
            parseCoordinates(bounds);
        }
    }

    private void parseCoordinates(String bounds){
        String[] boundsArray = bounds.split("\\[|\\]|,");
        String[] validArr = Arrays.stream(boundsArray).filter(StringUtils::isNotEmpty).toArray(String[]::new);
        int x1 = Integer.parseInt(validArr[0]);
        int x2 = Integer.parseInt(validArr[2]);
        int y1 = Integer.parseInt(validArr[1]);
        int y2 = Integer.parseInt(validArr[3]);
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

    public String getContentDesc() {
        return contentDesc;
    }

    public String getText(){
        return text;
    }


    public String getXpath() {
        return xpath;
    }

    public int getTop() {
        return top;
    }

    public int getLeft() {
        return left;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getResourceId() {
        return resourceId;
    }


    public boolean isCheckable() {
        return Boolean.parseBoolean(checkable);
    }

    public boolean isChecked() {
        return Boolean.parseBoolean(checked);
    }

    public boolean isClickable() {
        return Boolean.parseBoolean(clickable);
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(enabled);
    }

    public boolean isFocusable() {
        return Boolean.parseBoolean(focusable);
    }

    public boolean isFocused() {
        return Boolean.parseBoolean(focused);
    }

    public boolean isLongClickable() {
        return Boolean.parseBoolean(longClickable);
    }

    public boolean isPassword() {
        return Boolean.parseBoolean(password);
    }

    public boolean isScrollable() {
        return Boolean.parseBoolean(scrollable);
    }

    public boolean isSelected() {
        return Boolean.parseBoolean(selected);
    }

    public String getBounds() {
        return bounds;
    }

    public boolean isDisplayed() {
        return Boolean.parseBoolean(displayed);
    }
}
