import copy
from typing import Set, List

import torch
from sentence_transformers import SentenceTransformer
from transformers import HfArgumentParser

from common.constants import *
from entities.device import Device
from entities.element import Element
from entities.frame_feature import FrameFeature
from entities.page_info import PageInfo
from screen_comprehension.classifier.classifier import Classifier
from screen_comprehension.modeling.app_cls_model import AppClsScreenBERT
from screen_comprehension.modeling.frame_data import FrameData
from screen_comprehension.modeling.frame_data_args import FrameDataArguments
from screen_comprehension.modeling.utils import get_device
from screen_comprehension.screen_comprehension import ScreenComprehension


def screen_transform(device, frame, screenshot, windows_screenshot=None):
    parser = HfArgumentParser(FrameDataArguments)
    args = parser.parse_args_into_dataclasses()[0]
    args.task_type = "app_sim"
    sentence_encoder = SentenceTransformer(args.SENTENCE_ENCODER_MODEL).to(device)

    frame_data = FrameData(screenshot_as_ndarray=screenshot, windows_screenshot=windows_screenshot, args=args)
    # frame_data.assign_file_name(screenshot)
    flag = frame_data.parse_elements(sentence_encoder, element_list=frame)
    if not flag:
        # todo replace with logger
        print(f"skip trace {screenshot} for too less elements")

    return frame_data


class ScreenBertComprehension(ScreenComprehension):
    def get_device(self):
        return get_device()

    def init_screen_comprehension_model(self, path_to_comprehension_model):
        screen_bert_model = AppClsScreenBERT(self.device)
        screen_bert_model.load_from_pretrain(path_to_comprehension_model)
        screen_bert_model.eval()
        return screen_bert_model

    def init_screen_topic_classifier_model(self, path_to_classifier_model):
        classifier_model = Classifier(self.device)
        classifier_model.load_from_pretrain(path_to_classifier_model)
        return classifier_model

    def encode(self, device: Device, page_info: PageInfo) -> FrameFeature:
        screenshot, elements = page_info.screenshot, page_info.element_list

        frame_data = screen_transform(self.device, elements, screenshot)
        valid_el_num = int(frame_data.el_num)

        # Inputs
        el_type_ids = frame_data.el_type_ids[:frame_data.el_num].long()
        txt_features = frame_data.txt_features[:frame_data.el_num]
        bboxes = frame_data.bboxes[:frame_data.el_num].long()
        imgs = frame_data.imgs[:frame_data.el_num]
        seg_ids = torch.LongTensor([1] * valid_el_num)

        '''
        {'last_hidden_state':
        tensor([[[ 0.5931, -1.8085, -1.3201,  ..., -0.5161,  0.2435, -0.7787],
                ...,
                [ 0.5084, -1.6724, -0.0978,  ..., -0.1089,  0.4094, -0.0056]]],
                grad_fn=<SliceBackward>),
        'pooler_output':
            tensor([[-8.5739e-01, -4.4140e-03,  7.7261e-01, -8.1104e-01,  8.7364e-01,
                    8.9189e-01, ..., -4.6588e-01]], grad_fn=<TanhBackward>)}
        '''

        with torch.no_grad():
            output = self.screen_comprehension_model.ee_model(torch.unsqueeze(el_type_ids, 0).to(self.device),
                                                              torch.unsqueeze(txt_features, 0).to(self.device),
                                                              torch.unsqueeze(imgs, 0).to(self.device),
                                                              torch.unsqueeze(bboxes, 0).to(self.device),
                                                              torch.unsqueeze(seg_ids, 0).to(self.device),
                                                              None, None, None, None, None)

            cls_prob = self.screen_comprehension_model.forward_test(torch.unsqueeze(el_type_ids, 0).to(self.device),
                                                                    torch.unsqueeze(txt_features, 0).to(self.device),
                                                                    torch.unsqueeze(imgs, 0).to(self.device),
                                                                    torch.unsqueeze(bboxes, 0).to(self.device),
                                                                    None,
                                                                    torch.unsqueeze(seg_ids, 0).to(self.device)
                                                                    )

            cls_prob = torch.argmax(cls_prob, dim=-1)
            pred_cls = screen_bert.APP_CLASSES[torch.argmax(cls_prob)]

        return FrameFeature(page_info, output['pooler_output'], list(torch.unbind(torch.squeeze(output['last_hidden_state']))), self.classify(output['pooler_output']))

    def classify(self, screen_feature):
        return screen_bert.FRAME_TOPICS[self.screen_topic_classifier_model.classify(screen_feature)]

    # def get_elements_unvisited_on_current_page(self, elements: List[Element],
    #                                            elements_visited_on_current_page: Set[Element], mode=None):
    #     interactive_elements_on_current_page = filter(lambda e: e.interactive(), elements)
    #     return set(interactive_elements_on_current_page).difference(elements_visited_on_current_page)
