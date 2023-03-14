package com.microsoft.hydralab.t2c.runner.finder;

import com.google.common.base.Strings;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.elements.WindowsElementInfo;
import org.openqa.selenium.WebElement;

public class WindowsElementFinder implements ElementFinder<WindowsElementInfo> {

    private final BaseDriverController driverController;

    public WindowsElementFinder(BaseDriverController driverController) {
        this.driverController = driverController;
    }

    @Override
    public WebElement findElement(WindowsElementInfo elementInfo) {
        WebElement elementFound;
        if (!Strings.isNullOrEmpty(elementInfo.getXpath())) {
            elementFound = driverController.findElementByXPath(elementInfo.getXpath());
            if (elementFound != null) {
                return elementFound;
            }
        }

        if (!Strings.isNullOrEmpty(elementInfo.getName())) {
            elementFound = driverController.findElementById(elementInfo.getName());
            if (elementFound != null) {
                return elementFound;
            }
        }

        if (!Strings.isNullOrEmpty(elementInfo.getAutomationId())) {
            elementFound = driverController.findElementByAccessibilityId(elementInfo.getAutomationId());
            if (elementFound != null) {
                return elementFound;
            }
        }
        return null;
    }
}
