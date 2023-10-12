from abc import ABC, abstractmethod

from entities.device import Device
from entities.frame_feature import FrameFeature
from entities.page_info import PageInfo


class ScreenComprehension(ABC):
    def __init__(self, path_to_comprehension_model, path_to_classifier_model):
        self.device = self.get_device()
        self.screen_comprehension_model = self.init_screen_comprehension_model(path_to_comprehension_model)
        self.screen_topic_classifier_model = self.init_screen_topic_classifier_model(path_to_classifier_model)

        self.page_group = {}

    @abstractmethod
    def get_device(self):
        pass

    @abstractmethod
    def init_screen_comprehension_model(self, path_to_comprehension_model):
        pass

    @abstractmethod
    def init_screen_topic_classifier_model(self, path_to_classifier_model):
        pass

    @abstractmethod
    def encode(self, device: Device, page_info: PageInfo) -> FrameFeature:
        pass

