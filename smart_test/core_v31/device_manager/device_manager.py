from abc import ABC, abstractmethod
from typing import List, Dict

from entities.action import SingleAction
from entities.device import Device
from entities.element import Element
from entities.page_info import PageInfo
from entities.test_app import TestApplication


class DeviceManager(ABC):
    def __init__(self, device_info: Device, application_info: List[TestApplication], **kwargs):
        self.vaild_actions = {
            "Relaunch": self.relaunch,
            "Interact_With_Element": self.interact_with_element,
            "Login": self.perform_login
        }

    def extract_page_info(self):
        return PageInfo(self.get_screenshot(), self.get_page_source(), self.get_elements_list(), self.get_running_state(), self.get_current_activity(), self.is_login_page())

    def register_or_update_custom_action(self, action_type, action_function):
        self.vaild_actions.update({action_type: action_function})

    def execute_action(self, action: SingleAction):
        if action.action_type not in self.vaild_actions.keys():
            raise ValueError
        if action.element:
            action.kwargs.update({"element": action.element})
        self.vaild_actions[action.action_type](**action.kwargs)

    @abstractmethod
    def quit(self, uninstall=True):
        pass

    @abstractmethod
    def relaunch(self, index=0):
        pass

    @abstractmethod
    def get_page_source(self):
        pass

    @abstractmethod
    def get_screenshot(self, format="ndarray"):
        pass

    @abstractmethod
    def get_elements_list(self) -> List[Element]:
        pass

    @abstractmethod
    def get_running_state(self) -> Dict[TestApplication, int]:
        pass

    @abstractmethod
    def get_current_activity(self) -> str:
        pass

    @abstractmethod
    def interact_with_element(self, element, strings):
        pass

    @abstractmethod
    def reset(self):
        pass

    @abstractmethod
    def is_login_page(self):
        pass

    @abstractmethod
    def get_login_client_instance(self):
        pass

    @abstractmethod
    def perform_login(self, strings):
        pass





