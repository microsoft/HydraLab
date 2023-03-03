package com.microsoft.hydralab.t2c.runner.finder;

import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import org.openqa.selenium.WebElement;

public interface ElementFinder<T extends BaseElementInfo> {

    WebElement findElement(T elementInfo);

}
