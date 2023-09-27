from typing import Dict, Type, List

from device_manager.device_manager import DeviceManager
from entities.device import Device
from entities.test_app import TestApplication


class DeviceManagerFactory:
    def __init__(self) -> None:
        self.device_managers: Dict[Device, DeviceManager] = {}

    def register_and_create_device_manager(self, device: Device, application_info: List[TestApplication], device_managers_class: Type[DeviceManager], **kwargs) -> None:
        self.device_managers[device] = device_managers_class(device, application_info, **kwargs)

    def get_device_manager(self, device: Device) -> DeviceManager:
        if device not in self.device_managers.keys():
            raise ValueError
        return self.device_managers[device]
