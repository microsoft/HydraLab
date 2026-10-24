import os
import random
import time
from typing import List, Dict

import scrcpy
from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
from appium.webdriver.common.touch_action import TouchAction
from loguru import logger

from common.constants import *
from device_manager.device_manager import DeviceManager
from device_manager.elements_extractor import parse_page_source
from entities.device import Device
from entities.element import Element
from entities.test_app import TestApplication
from helper.login_client import MicrosoftLoginClient
from utils.AppiumLauncher import AppiumLauncher


class AndroidDeviceInfo:
    def __init__(self, serial_number, platform_version):
        self.serial_number = serial_number
        self.platform_version = platform_version


class AndroidApplicationInfo:
    def __init__(self, app_path, package_name):
        self.app_path = app_path
        self.package_name = package_name


class DeviceManagerAndroid(DeviceManager):
    def __init__(self, device_info: Device, application_info: List[TestApplication], **kwargs):
        super().__init__(device_info, application_info, **kwargs)
        self.device_info = device_info
        self.application_info = application_info

        self.driver = self.appium_driver_init()
        self.scrcpy_client = self.scrcpy_init()

        self.username_idx = -1

        self.register_or_update_custom_action('Back', self.back)

        self.appium = None

        self.login_config_reset()

    def reset(self):
        self.relaunch()

    def relaunch(self, index=0, reinstall=False, clear_log=False, wait_time=10):
        self.login_config_reset()
        if self.driver.is_app_installed(self.application_info[index].package_name) and reinstall:
            self.driver.remove_app(self.application_info[index].package_name)

        if not self.driver.is_app_installed(self.application_info[index].package_name) or reinstall:
            self.driver.install_app(self.application_info[index].path, replace=reinstall, grantPermissions=True)

        self.terminate_app()
        self.activate_app(index, wait_time)
        time.sleep(8)

        if clear_log:
            os.system(f'adb -s {self.device_info.identifier} {adb_command.CLEAR_LOG}')

    def get_page_source(self):
        return self.driver.page_source

    def get_screenshot(self, format="ndarray"):
        return self.scrcpy_client.last_frame

    def get_elements_list(self, page_source=None) -> List[Element]:
        retry_times = 5
        element_list = []
        while retry_times > 0:
            retry_times -= 1
            page_source = page_source if page_source else self.driver.page_source
            parsed_dict = parse_page_source(page_source)
            if parsed_dict:
                element_list = parsed_dict["vh_elements"]
                self.is_microsoft_login_page = parsed_dict["is_microsoft_login"]
                break

        return element_list

    def get_current_activity(self):
        return self.driver.current_activity

    def get_running_state(self) -> Dict[TestApplication, int]:
        running_state = {}
        for app in self.application_info:
            running_state.update({app: self.driver.query_app_state(app.package_name)})

        return running_state

    def interact_with_element(self, element, strings):
        action_info = {}

        element_type = element.element_type
        element_xpath = element.xpath
        element_identifier = "@".join(
            [element.element_class, element.content_desc[0], element.resource_id, element.text_hint])
        clickable = element.clickable
        long_clickable = element.long_clickable
        password = element.password
        scrollable = element.scrollable
        bbox = element.bounds

        action_info['element_info'] = {
            "identifier": element_identifier,
            "bbox": bbox,
            "platform": platform.ANDROID
        }
        if element_type == 'input':
            action_info['name'] = 'INPUT'
            if any(username_identifier in element_identifier.lower() for username_identifier in identifiers.USERNAME):
                self.username_idx = random.randrange(0, len(strings['username']))
                string = strings['username'][self.username_idx]
            elif password:
                string = strings['password'][
                    random.randrange(0, strings['password'])] if self.username_idx == -1 else \
                    strings['password'][self.username_idx]
            else:
                string = strings['generic'][random.randrange(0, len(strings['generic']))]
            action_info['extra_info'] = string
            self.input(element_xpath, string)
        elif element_type == 'clickable' and clickable:
            action_info['name'] = 'CLICK'
            self.click(element_xpath)
        elif element_type == 'clickable' and long_clickable:
            action_info['name'] = 'LONG-CLICK'
            self.long_click(element_xpath)
        elif element_type == 'clickable' and scrollable:
            action_info['name'] = 'SCROLL'
            action_info['extra_info'] = f'Start Position: ({bbox[2]}, {bbox[3]}), End Position: ({bbox[0]}, {bbox[1]})'
            self.swipe(bbox)

    def quit(self, uninstall=False):
        try:
            logger.info('<--- TRY TO QUIT ANDROID DEVICE MANAGER  --->')
            if uninstall and self.driver.is_app_installed(self.application_info[0].package_name):
                self.driver.remove_app(self.application_info[0].package_name)

            time.sleep(10)

            self.driver.quit()

            self.scrcpy_client.stop()

            logger.info('<--- ANDROID DEVICE MANAGER QUIT  --->')
        except Exception as e:
            logger.exception(e)

    def appium_driver_init(self, appium_port=10086):
        try:
            return self.try_init_driver(appium_port)
        except Exception as e:
            if 'target machine actively refused it' in str(e):
                # Failed to establish a new connection: [WinError 10061] No connection could be made
                # because the target machine actively refused it
                logger.info("Start Appium server...")
                self.appium = AppiumLauncher(appium_port)
                return self.try_init_driver(appium_port)
            else:
                raise e

    def try_init_driver(self, appium_port):
        desired_caps = {
            'platformName': 'android',
            'platformVersion': self.device_info.platform_version,
            'udid': self.device_info.identifier,
            'unicodeKeyboard': True,
            'resetKeyboard': True,
            'forceStop': True,
            'automationName': 'uiautomator2' if float(self.device_info.platform_version) >= 5.0 else 'uiautomator1',
            'adbExecTimeout': 600000,
            'newCommandTimeout': 600000
        }

        driver = webdriver.Remote(f'http://127.0.0.1:{appium_port}/wd/hub', desired_caps)
        driver.implicitly_wait(1)

        return driver

    def scrcpy_init(self):
        scrcpy_client = scrcpy.Client(device=self.device_info.identifier)
        scrcpy_client.start(threaded=True)

        return scrcpy_client

    def activate_app(self, index, wait_time=5):
        if self.driver.query_app_state(self.application_info[index].package_name) == app_state.RUNNING_IN_FOREGROUND:
            return True

        self.driver.activate_app(self.application_info[index].package_name)

        while not self.is_running_foreground() and wait_time > 0:
            logger.info('WAIT FOR APP TO ACTIVATE, CURRENT APP STATE: ' + str(
                self.driver.query_app_state(self.application_info[0].package_name)))
            wait_time -= 1
            time.sleep(1)

        if self.is_running_foreground():
            logger.info(f'{self.application_info[index].package_name} is ACTIVATED')
        else:
            logger.info(f'FAIL TO ACTIVATE {self.application_info[index].package_name}, TRY TO CLICK BACK.')
            self.driver.back()
            time.sleep(1)

        return self.is_running_foreground()

    def terminate_app(self, timeout=5000):
        try:
            self.driver.terminate_app(self.application_info[0].package_name, timeout=timeout)
            time.sleep(1)
        except Exception as exception:
            logger.info(exception)
            logger.info('TERMINATE APP FAILED, TRY INIT DRIVER WITH FORCE STOP.')
            self.driver = self.appium_driver_init()
            time.sleep(2)

    def is_running_foreground(self):
        return self.driver.query_app_state(self.application_info[0].package_name) == app_state.RUNNING_IN_FOREGROUND

    def input(self, xpath, string):
        try:
            element = self.driver.find_element(AppiumBy.XPATH, xpath)
            element.clear()
            element.click()
            element.send_keys(string)
            time.sleep(0.5)
            self.driver.press_keycode(66)
            time.sleep(0.5)
        except Exception as exception:
            logger.info(exception)

    def input_coordinate(self, coordinate, string):
        try:
            actions = TouchAction(self.driver)
            actions.tap(x=coordinate[0], y=coordinate[1]).perform()
            time.sleep(0.5)
            os.system(f'adb -s {self.device_info.identifier} shell input text "{string}"')
            time.sleep(0.5)
            self.driver.press_keycode(66)
            time.sleep(0.5)
        except Exception as exception:
            logger.info(exception)

    def click(self, xpath):
        try:
            element = self.driver.find_element(AppiumBy.XPATH, xpath)
            element.click()
        except Exception as exception:
            logger.info(exception)

    def long_click(self, xpath):
        try:
            element = self.driver.find_element(AppiumBy.XPATH, xpath)
            TouchAction(self.driver).long_press(element, duration=2000).release().perform()
        except Exception as exception:
            logger.info(exception)

    def swipe(self, bbox):
        try:
            self.driver.swipe(bbox[2], bbox[3], bbox[0], bbox[1], duration=200)
        except Exception as exception:
            logger.info(exception)

    def back(self):
        self.driver.press_keycode(4)
        time.sleep(5)

    def is_login_page(self):
        return self.is_microsoft_login_page

    def get_login_client_instance(self):
        if not self.is_microsoft_login_page:
            return None

        # create when initially perform login actions
        if self.login_client is None:
            self.login_client = MicrosoftLoginClient(self.driver)

        return self.login_client

    def perform_login(self, strings):
        login_client = self.get_login_client_instance()

        if self.is_microsoft_login_page:
            finished = login_client.perform_action(strings)
            if finished:
                self.login_config_reset()

    def login_config_reset(self):
        self.is_microsoft_login_page = False
        # destroy when finish login actions
        self.login_client = None
