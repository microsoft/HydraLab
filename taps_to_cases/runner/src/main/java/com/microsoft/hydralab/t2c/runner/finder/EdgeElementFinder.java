package com.microsoft.hydralab.t2c.runner.finder;

import com.google.common.base.Strings;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.elements.EdgeElementInfo;
import org.openqa.selenium.WebElement;

public class EdgeElementFinder implements ElementFinder<EdgeElementInfo>  {

    private final BaseDriverController driverController;

    EdgeElementFinder(BaseDriverController driverController) {
        this.driverController = driverController;
    }

    @Override
    public WebElement findElement(EdgeElementInfo elementInfo) {
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

        if (!Strings.isNullOrEmpty(elementInfo.getText())) {
            elementFound = driverController.findElementByText(elementInfo.getText());
            if (elementFound != null) {
                return elementFound;
            }
        }
        return null;
    }
}
