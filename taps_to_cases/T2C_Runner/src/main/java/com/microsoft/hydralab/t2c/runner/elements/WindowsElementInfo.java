// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.elements;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class WindowsElementInfo extends BaseElementInfo {
    private String AcceleratorKey;
    private String AccessKey;
    private String AutomationId;
    private String ClassName;
    private String FrameworkId;
    private String HasKeyboardFocus;

    private String HelpText;
    private String IsContentElement;
    private String IsControlElement;
    private String IsEnabled;
    private String IsKeyboardFocusable;
    private String IsOffscreen;
    private String IsPassword;
    private String IsRequiredForForm;
    private String ItemStatus;
    private String ItemType;
    private String LocalizedControlType;
    private String Name;
    private String Orientation;
    private String ProcessId;
    private String RuntimeId;
    private String x;
    private String y;
    private String width;
    private String height;
    private String xpath;
    private Integer centerX;
    private Integer centerY;

    public WindowsElementInfo(String acceleratorKey, String accessKey, String automationId, String className,
                              String frameworkId, String hasKeyboardFocus, String helpText, String isContentElement,
                              String isControlElement, String isEnabled, String isKeyboardFocusable, String isOffscreen,
                              String isPassword, String isRequiredForForm, String itemStatus, String itemType,
                              String localizedControlType, String name, String orientation, String processId,
                              String runtimeId, String x, String y, String width, String height, String xpath){
        super(automationId,xpath,name);
        this.AcceleratorKey = acceleratorKey;
        this.AccessKey = accessKey;
        this.AutomationId = automationId;
        this.ClassName = className;
        this.FrameworkId = frameworkId;
        this.HasKeyboardFocus = hasKeyboardFocus;
        this.HelpText = helpText;
        this.IsContentElement = isContentElement;
        this.IsControlElement = isControlElement;
        this.IsEnabled = isEnabled;
        this.IsKeyboardFocusable = isKeyboardFocusable;
        this.IsOffscreen = isOffscreen;
        this.IsPassword = isPassword;
        this.IsRequiredForForm = isRequiredForForm;
        this.ItemStatus = itemStatus;
        this.ItemType = itemType;
        this.LocalizedControlType = localizedControlType;
        this.Name = name;
        this.Orientation = orientation;
        this.ProcessId = processId;
        this.RuntimeId = runtimeId;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.xpath = xpath;
        this.centerX = Integer.parseInt(x)+Integer.parseInt(width)/2;
        this.centerY = Integer.parseInt(y)+Integer.parseInt(height)/2;
    }

    public String getElementInfo(){
        return ToStringBuilder.reflectionToString(this);
    }
    public String getAcceleratorKey() {
        return AcceleratorKey;
    }

    public String getAccessKey() {
        return AccessKey;
    }

    public String getAutomationId() {
        return AutomationId;
    }

    public String getClassName() {
        return ClassName;
    }

    public String getIsEnabled() {
        return IsEnabled;
    }

    public String getIsContentElement() {
        return IsContentElement;
    }

    public String getHelpText() {
        return HelpText;
    }

    public String getItemType() {
        return ItemType;
    }

    public String getLocalizedControlType() {
        return LocalizedControlType;
    }

    public String getFrameworkId() {
        return FrameworkId;
    }

    public String getHasKeyboardFocus() {
        return HasKeyboardFocus;
    }

    public String getName() {
        return Name;
    }

    public String getIsOffscreen() {
        return IsOffscreen;
    }

    public Integer getCenterX() {
        return centerX;
    }

    public Integer getCenterY() {
        return centerY;
    }

    public String getXpath() {
        return xpath;
    }

    public String getHeight() {
        return height;
    }

    public String getWidth() {
        return width;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getIsControlElement() {
        return IsControlElement;
    }

    public String getIsKeyboardFocusable() {
        return IsKeyboardFocusable;
    }

    public String getIsPassword() {
        return IsPassword;
    }

    public String getItemStatus() {
        return ItemStatus;
    }

    public String getOrientation() {
        return Orientation;
    }

    public String getIsRequiredForForm() {
        return IsRequiredForForm;
    }

    public String getProcessId() {
        return ProcessId;
    }

    public String getRuntimeId() {
        return RuntimeId;
    }
}
