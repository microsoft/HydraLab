from torch import Tensor
from typing import List

from entities.page_info import PageInfo


class FrameFeature:
    def __init__(self, page_info: PageInfo, page_feature: Tensor, element_features: List[Tensor], frame_topic):
        self.page_info = page_info
        self.page_feature = page_feature
        self.element_features = element_features
        self.frame_topic = frame_topic
