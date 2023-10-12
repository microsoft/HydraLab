from typing import Optional

from entities.device import Device
from entities.element import Element


class SingleAction:
    def __init__(self, device: Device, action_type: str, element: Optional[Element], **kwargs):
        self.device = device
        self.action_type = action_type
        self.element = element
        self.kwargs = kwargs

    def __str__(self):
        if self.element:
            return f'[{self.device.identifier}]{self.element}'
        else:
            return f'[{self.device.identifier}]{self.action_type}'
