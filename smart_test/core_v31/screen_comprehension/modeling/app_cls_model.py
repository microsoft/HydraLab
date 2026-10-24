from asyncio.log import logger

import torch
import torch.nn.functional as F
from torch import nn

from screen_comprehension.modeling.element_encoder_base import ElementsEncoderConfig, ElementsEncoderModel


class AppClsScreenBERT(nn.Module):
    def __init__(self, device, num_class=27):
        super(AppClsScreenBERT, self).__init__()
        self.device = device
        self.config = ElementsEncoderConfig()
        self.ee_model = ElementsEncoderModel(self.config)
        self.ee_model.to(self.device)

        # link component prediction head
        self.cls_head = nn.Linear(self.config.hidden_size, num_class).to(self.device)
        self.cls_loss = nn.CrossEntropyLoss().to(self.device)

    def forward_test(
            self,
            el_type_ids,
            txt_features,
            imgs,
            bboxes,
            attention_mask,
            seg_ids,
    ):
        output = self.ee_model(
            el_type_ids=el_type_ids,
            txt_features=txt_features,
            imgs=imgs,
            bboxes=bboxes,
            attention_mask=attention_mask,
            seg_ids=seg_ids,
        )

        pooler_output = output["pooler_output"]

        cls_logits = self.cls_head(pooler_output)
        return nn.functional.softmax(cls_logits, dim=-1)

    def forward(
            self,
            el_type_ids,
            txt_features,
            imgs,
            bboxes,
            attention_mask,
            seg_ids,
            app_cls_label,
            with_metric=False,
    ):
        # el_type_ids = el_type_ids.long()
        # attention_mask = attention_mask.long()
        # txt_mask = txt_mask.long()
        # link_label_mask = link_label_mask.long()
        # link_label = link_label.long()
        # triplet_label = triplet_label.long()

        output = self.ee_model(
            el_type_ids=el_type_ids,
            txt_features=txt_features,
            imgs=imgs,
            bboxes=bboxes,
            attention_mask=attention_mask,
            seg_ids=seg_ids,
        )

        pooler_output = output["pooler_output"]

        cls_logits = self.cls_head(pooler_output)
        cls_loss = self.cls_loss(cls_logits, app_cls_label.view(-1))

        loss_dict = {"loss": cls_loss}

        if with_metric:
            metric_dict = self.accuracy(cls_logits, app_cls_label)
            return metric_dict, loss_dict
        else:
            return {}, loss_dict

    def accuracy(self, cls_logits, app_cls_label):
        metric_dict = {}
        cls_pred = torch.argmax(cls_logits, -1)
        cls_pred = cls_pred.view(-1)
        app_cls_label = app_cls_label.view(-1)
        metric_dict["cls_acc"] = sum(cls_pred == app_cls_label) / (app_cls_label.shape[0] + 1e-5)

        return metric_dict

    def load_from_pretrain(self, path_to_pretrained_model):
        warning_message = self.load_state_dict(torch.load(path_to_pretrained_model, map_location=self.device), strict=False)
        logger.info("Loading from pretrained ScreenBert......")
        logger.info("Missing keys: " + str(warning_message[0]))
        logger.info("Unexpected keys: " + str(warning_message[1]))