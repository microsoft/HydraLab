package com.microsoft.hydralab.t2c.runner.finder;

import com.google.common.base.Strings;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.elements.AndroidElementInfo;
import org.openqa.selenium.WebElement;

public class AndroidElementFinder implements ElementFinder<AndroidElementInfo> {

    private final BaseDriverController driverController;

    AndroidElementFinder(BaseDriverController driverController) {
        this.driverController = driverController;
    }

    @Override
    public WebElement findElement(AndroidElementInfo elementInfo) {
        WebElement elementFound;
        if (!Strings.isNullOrEmpty(elementInfo.getAccessibilityId())) {
            elementFound = driverController.findElementByAccessibilityId(elementInfo.getAccessibilityId());
            if (elementFound != null) {
                return elementFound;
            }
        }
        if (!Strings.isNullOrEmpty(elementInfo.getXpath())) {
            elementFound = driverController.findElementByXPath(elementInfo.getXpath());
            if (elementFound != null) {
                return elementFound;
            }
        }

        if (!Strings.isNullOrEmpty(elementInfo.getResourceId())) {
            elementFound = driverController.findElementById(elementInfo.getResourceId());
            if (elementFound != null) {
                return elementFound;
            }
        }

        if (!Strings.isNullOrEmpty(elementInfo.getText())) {
            elementFound = driverController.findElementByText(elementInfo.getText());
            if (elementFound != null) {
                return elementFound;
            }
        }
        return null;
    }
}
