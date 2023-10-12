from abc import ABC, abstractmethod
from typing import List, Dict

from entities.action import SingleAction
from entities.device import Device
from entities.frame_feature import FrameFeature


class DecisionMaker(ABC):
    @abstractmethod
    def __init__(self, **kwargs):
        pass

    @abstractmethod
    def action_selector(self, output_dir, frame_feature_of_each_device: Dict[Device, FrameFeature], llm, curr_step) -> (List[SingleAction], bool, str):
        pass

    @abstractmethod
    def before_action(self):
        pass

    @abstractmethod
    def after_action(self):
        pass

    @abstractmethod
    def save(self, output_dir):
        pass

    @abstractmethod
    def load(self, load_dir):
        pass

    @abstractmethod
    def exceptional_restart(self, device):
        pass
