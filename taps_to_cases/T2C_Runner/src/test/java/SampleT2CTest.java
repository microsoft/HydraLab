// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import com.microsoft.hydralab.t2c.runner.*;
import com.microsoft.hydralab.t2c.runner.controller.AndroidDriverController;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.WindowsDriverController;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.windows.WindowsDriver;
import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SampleT2CTest {

    public AndroidDriver driver;

    public T2CJsonParser t2CJsonParser;
    private TestInfo testInfo;
    private Logger logger;
    String filePath = "src/test/jsonFiles/testFile_android_dragAndDrop.json";
    AppiumDriverLocalService service;


    private final Map<String, BaseDriverController> driverControllerMap = new HashMap<>();

    @BeforeEach
    public void setUp() throws MalformedURLException {
        logger = LoggerFactory.getLogger(SampleT2CTest.class);
        t2CJsonParser = new T2CJsonParser(logger);
        testInfo = t2CJsonParser.parseJsonFile(filePath);

        service = AppiumDriverLocalService.buildService(new AppiumServiceBuilder()
                .usingPort(4723).withArgument(GeneralServerFlag.BASEPATH, "/wd/hub/")
                .withArgument(GeneralServerFlag.RELAXED_SECURITY)
                .withArgument(GeneralServerFlag.LOG_LEVEL, "error")
                .withArgument(GeneralServerFlag.ALLOW_INSECURE, "adb_shell"));
        service.start();


        getDriversMap(testInfo.getDrivers());

        //driver = new AndroidDriver<MobileElement>(new URL("http://127.0.0.1:4723/wd/hub"),caps);
    }

    public void getDriversMap(ArrayList<DriverInfo> drivers) {
        for (DriverInfo driverInfo : drivers) {
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("newCommandTimeout", 4000);
            caps.setCapability("clearDeviceLogonStart", true);
            caps.setCapability("noReset", true);
            if (driverInfo.getPlatform().equalsIgnoreCase("android")) {
                caps.setCapability("automationName", "uiautomator2");
                AndroidDriver androidDriver = new AndroidDriver(service.getUrl(), caps);
                AndroidDriverController androidDriverController = new AndroidDriverController(androidDriver, logger);
                driverControllerMap.put(driverInfo.getId(), androidDriverController);
                if (driverInfo.getLauncherApp() != null && driverInfo.getLauncherApp().length() > 0) {
                    androidDriverController.activateApp(driverInfo.getLauncherApp());
                }
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("windows")) {
                if (driverInfo.getLauncherApp() != null && driverInfo.getLauncherApp().length() > 0) {
                    caps.setCapability("app", driverInfo.getLauncherApp() + "!app");
                }
                WindowsDriver windowsDriver = new WindowsDriver(service.getUrl(), caps);
                driverControllerMap.put(driverInfo.getId(), new WindowsDriverController(windowsDriver, logger));
            }
        }
    }

    @Test
    public void jsonTest() {
        ArrayList<ActionInfo> caseList = testInfo.getCases();

        for (ActionInfo actionInfo : caseList) {
            BaseDriverController driverController = driverControllerMap.get(actionInfo.getDriverId());
            System.out.println(actionInfo.getDriverId());
            T2CAppiumUtils.doAction(driverController, actionInfo);
        }
    }

    @After
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
        if (service != null) {
            service.stop();
        }
    }
}
