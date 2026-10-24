import time

from selenium.webdriver.common.by import By
from loguru import logger

from common.constants import android_widget


class MicrosoftLoginClient:
    def __init__(self, driver):
        self.driver = driver
        self.current_step = 0
        # self.strings = strings

    def perform_action(self, strings):
        finished = False
        if self.current_step == 0:
            self.enter_username(strings)
        elif self.current_step == 1:
            self.enter_password(strings)
            finished = True

        self.current_step += 1
        return finished

    def enter_username(self, strings):
        element = self.get_element()
        if element:
            self.input(element, strings["username"][0])

    def enter_password(self, strings):
        element = self.get_element()
        if element:
            self.input(element, strings["password"][0])

    def get_element(self):
        # by element type: EditText
        element = None
        try:
            for widget in android_widget.EDIT_TEXT:
                element = self.driver.find_element(By.CLASS_NAME, widget)
                break
        except Exception as e:
            logger.error(e)

        return element

    def input(self, element, string):
        try:
            element.clear()
            element.click()
            element.send_keys(string)
            time.sleep(0.5)
            self.driver.press_keycode(66)
            time.sleep(0.5)
        except Exception as e:
            logger.error(e)
