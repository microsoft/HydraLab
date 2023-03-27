package com.microsoft.hydralab.t2c.runner.finder;

import com.google.common.base.Strings;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.elements.IOSElementInfo;
import org.openqa.selenium.WebElement;

public class IOSElementFinder implements ElementFinder<IOSElementInfo> {

    private final BaseDriverController driverController;

    public IOSElementFinder(BaseDriverController driverController) {
        this.driverController = driverController;
    }

    @Override
    public WebElement findElement(IOSElementInfo elementInfo) {
        WebElement elementFound;

        if (!Strings.isNullOrEmpty(elementInfo.getLabel())) {
            elementFound = driverController.findElementByAccessibilityId(elementInfo.getLabel());
            if (elementFound != null) {
                return elementFound;
            }
        }

        if (!Strings.isNullOrEmpty(elementInfo.getName())) {
            elementFound = driverController.findElementByName(elementInfo.getName());
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
        return null;
    }
}
