import copy
import re
from typing import List

import cv2
import imageio
import numpy as np
import torch

from common.constants import *
from entities.element import Element
from screen_comprehension.modeling.utils import clamp, get_device, img_from_string
from utils.exception import BrokenScreenshotException

DOM_EL_TYPE_TO_IDX = {
    "icon": 1,
    "clickable": 2,
    "input": 3,
    "text": 4,
    "hist_icon": 5,
    "hist_clickable": 6,
    "hist_input": 7,
    "hist_text": 8,
}

VH_EL_IDX_TO_EL_TYPE = {
    1: "text",  # TEXT_VIEW
    2: "icon",  # IMAGE_VIEW
    3: "clickable",  # TEXT_BUTTON
    4: "clickable",  # IMAGE_BUTTON
    5: "input",  # TEXT_INPUT
    6: "icon",  # PROGRESS_BAR
    7: "clickable",  # CHECK_BOX
    8: "clickable",  # RADIO_BUTTON
    9: "icon",  # SPINNER
    10: "text",  # LABEL
    11: "clickable",  # SWITCH
    12: "text",  # LIST_VIEW
    13: "clickable",  # MENU_ITEM_VIEW
    14: "clickable",  # CARD_VIEW
    16: "clickable",  # TAB_VIEW
    17: "text",  # VIEW
    18: "text",  # LAYOUT
    19: "icon",  # DECOR_VIEW
    20: "text",  # WEB_VIEW
    21: "icon",  # TOOL_BAR
    22: "text",  # VIEW_GROUP
}

device = get_device()
CPU = torch.device("cpu")

# frame metadata store some basic information for a certain frame.
FRAME_METADATA_DICT_TMPL = {
    "filename": "",
    "xpath": [],  # DOM only
    "text": [],
    "bbox": [],
    "form_id": [],
    "element_type": [],
    "attention_mask": [],
    "label": [],
    "url_list": [],  # DOM only
    "img": [],
    "trace_id": "",
}


class FrameData:
    def __init__(self, url=None, screenshot=None, screenshot_as_ndarray=None, windows_screenshot=None, img_filename=None, args=None):
        # for DOM: (url, screenshot, args)
        # for VH: (img_filename, args)
        assert screenshot or img_filename or screenshot_as_ndarray is not None
        self.data_type = args.data_type
        self.task_type = args.task_type
        if self.data_type == "dom":  # DOM
            self.screenshot = img_from_string(screenshot)
        else:  # VH
            if img_filename:
                self.screenshot = imageio.imread(img_filename)  # Some VH are rotated!
            elif screenshot_as_ndarray is not None:
                self.screenshot = screenshot_as_ndarray[:, :, ::-1]
        if windows_screenshot is not None:
            self.windows_screenshot = img_from_string(windows_screenshot)
            self.windows_img_height, self.windows_img_width, _ = self.windows_screenshot.shape
        else:
            self.windows_screenshot = None

        self.img_height, self.img_width, _ = self.screenshot.shape
        ## TODO: check whether tfrecord handles screen rotation!
        ## TODO: camel case!
        self.rotated_screen = (self.img_height < self.img_width)

        # hyperparam
        self.norm_max_height = args.norm_max_dom_height if self.data_type == "dom" else args.norm_max_vh_height
        self.norm_max_width = args.norm_max_dom_width if self.data_type == "dom" else args.norm_max_vh_width
        self.max_cur_el_len = args.max_cur_el_len
        self.max_normed_bbox_pos = args.max_normed_bbox_pos
        self.max_relative_bbox = args.max_relative_bbox
        self.el_img_size = args.el_img_size
        self.max_seq_len = args.max_seq_len
        self.relative_scale = args.relative_scale

        # metadata is used to store the raw data before tensoring them
        self.metadata = copy.deepcopy(FRAME_METADATA_DICT_TMPL)
        self.metadata["filename"] = "unknown"
        # self.metadata["trace_id"] = -1
        self.metadata["frame_id"] = -1  # -1 for unknown
        self.metadata["ancestors"] = []
        self.metadata["class"] = []
        self.metadata["clickable"] = []
        self.metadata["long-clickable"] = []
        self.metadata["password"] = []
        self.metadata["scrollable"] = []
        self.metadata["resource-id"] = []
        self.metadata["identifier"] = []
        self.metadata["original_bbox"] = []
        self.metadata["platform"] = []

        # the tensored attributes used to training
        self.img = None
        self.txt_features = None
        self.img_features = None
        self.attention_mask = None
        self.el_type_ids = None
        self.bboxes = None
        self.url = url
        self.el_num = 0

    def assign_file_name(self, filename):
        self.metadata["filename"] = filename

    def crop_element_from_img(self, element):
        bbox_left = element["screen_bbox"]["left"]
        bbox_top = element["screen_bbox"]["top"]
        bbox_right = element["screen_bbox"]["right"]
        bbox_bottom = element["screen_bbox"]["bottom"]
        cropped_image = self.screenshot[bbox_top:bbox_bottom, bbox_left:bbox_right, :]
        return cropped_image

    def is_in_screen(self, el):
        """ This function is only for DOM. """
        el["screen_bbox"]["left"] = clamp(el["screen_bbox"]["left"], 0, self.screenshot.shape[1])
        el["screen_bbox"]["top"] = clamp(el["screen_bbox"]["top"], 0, self.screenshot.shape[0])
        el["screen_bbox"]["right"] = clamp(el["screen_bbox"]["right"], 0, self.screenshot.shape[1])
        el["screen_bbox"]["bottom"] = clamp(el["screen_bbox"]["bottom"], 0, self.screenshot.shape[0])
        if (el["screen_bbox"]["right"] - el["screen_bbox"]["left"] < 5 or el["screen_bbox"]["bottom"] - el["screen_bbox"]["top"] < 5):
            return False
        else:
            return True

    def get_dom_el_img(self, el):
        el_img = self.crop_element_from_img(el)
        el_img = cv2.resize(el_img, (self.el_img_size, self.el_img_size))
        return el_img

    def get_vh_el_img(self, bbox):
        el_img = self.screenshot[bbox[1]:bbox[3], bbox[0]:bbox[2], :]
        el_img = cv2.resize(el_img, (self.el_img_size, self.el_img_size))
        return el_img

    def get_windows_vh_el_img(self, bbox):
        el_img = self.windows_screenshot[bbox[1]:bbox[3], bbox[0]:bbox[2], :]
        el_img = cv2.resize(el_img, (self.el_img_size, self.el_img_size))
        return el_img

    def add_dom_element_data(self, el):
        el_text = ""
        reassign_el_type = False

        for key in [
            "value",
            "label-for",
            "nearby-text",
            "aria-labelledby",
            "aria-describedby",
            "aria-label",
            "placeholder",
            "name",
            "inner-text",
            "icon_label",
            "type",
        ]:
            val = el.get(key, "")
            el_text = el_text + " " + val if val else el_text
            if not el_text and key == "icon_label" and val:
                el["type"] = "icon"
                reassign_el_type = True
        if not el_text:
            el_text = el["tag-name"]

        el_text = el_text.replace("_", " ").replace("[", " ").replace("]", " ")

        # reassign element type TODO: if else
        if not reassign_el_type:
            for word in ["password", "email", "username", "text"]:
                if word in el.get("type", "").split(" ") and "input" in el.get("tag-name", "").split(" "):
                    el["type"] = "input"
                    reassign_el_type = True
                    break

        if not reassign_el_type:
            if (
                    "button" in el.get("type", "").split(" ")
                    or "button" in el.get("tag-name", "").split(" ")
                    or "a" in el.get("tag-name", "").split(" ")
                    or "checkbox" in el.get("type", "").split(" ")
            ):
                el["type"] = "clickable"
                reassign_el_type = True

        if not reassign_el_type:
            if "text" in el.get("type", "").split(" "):
                el["type"] = "text"
                reassign_el_type = True

        el_bbox = [
            int(el["page_bbox"]["left"]),
            int(el["page_bbox"]["top"]),
            int(el["page_bbox"]["right"]),
            int(el["page_bbox"]["bottom"]),
        ]

        if el_bbox[2] < 0 or el_bbox[3] < 0:
            return

        # Skip element if there is no text
        if not el_text:
            return

        # Can use element_type if more types are needed
        if el.get("type"):
            el_type = el["type"]
        else:
            el_type = "text"

        form_id = el["form_id"]

        el_img = self.get_dom_el_img(el) if self.is_in_screen(el) else np.zeros([self.el_img_size, self.el_img_size, 3])

        assert el_type in DOM_EL_TYPE_TO_IDX.keys(), f"unkown element type {el_type}"
        self.metadata["xpath"].append(el["xpath"])
        self.metadata["text"].append(el_text)
        self.metadata["bbox"].append(el_bbox)
        self.metadata["form_id"].append(form_id)
        self.metadata["element_type"].append(el_type)
        self.metadata["attention_mask"].append(1)
        self.metadata["label"].append(el["action"])
        self.metadata["url_list"].append(self.url)
        self.metadata["img"].append(el_img)

    def add_vh_element_data(self, element_list, idx):
        # get el_text, el_type, el_bbox, el_label.
        vh_element = element_list[idx]
        # get_el_text.
        content_desc = vh_element.content_desc[0]
        if not content_desc:
            content_desc = ""
        resource_id = vh_element.resource_id.split("/")[-1]
        if "_" in resource_id:  # blah_blah_blah
            resource_id = resource_id.replace("_", " ")
        else:  # blahBLAHBlah
            resource_id = FrameData.camelcase_split(resource_id)
        inner_text = vh_element.text
        text_hint = vh_element.text_hint
        el_text = " ".join([content_desc, resource_id, inner_text, text_hint])
        identifier = "@".join([content_desc, vh_element.resource_id, inner_text])
        # get el_type.
        el_type = vh_element.element_type
        el_bbox = vh_element.bounds
        el_label = 2
        original_bbox = el_bbox
        el_bbox = [
            clamp(el_bbox[0], 0, self.norm_max_width),
            clamp(el_bbox[1], 0, self.norm_max_height),
            clamp(el_bbox[2], 0, self.norm_max_width),
            clamp(el_bbox[3], 0, self.norm_max_height),
        ]
        el_bbox = [
            round(el_bbox[0] * self.img_width / self.norm_max_width),
            round(el_bbox[1] * self.img_height / self.norm_max_height),
            round(el_bbox[2] * self.img_width / self.norm_max_width),
            round(el_bbox[3] * self.img_height / self.norm_max_height),
        ]
        form_id = 0
        if el_bbox[2] - el_bbox[0] > 0 and el_bbox[3] - el_bbox[1] > 0:
            el_img = self.get_vh_el_img(el_bbox)
        else:
            el_img = np.zeros([self.el_img_size, self.el_img_size, 3])

        # assign metadata
        self.metadata["xpath"].append(vh_element.xpath)
        self.metadata["ancestors"].append(vh_element.ancestors)
        self.metadata["class"].append(vh_element.element_class)
        self.metadata["clickable"].append(vh_element.clickable)
        self.metadata["long-clickable"].append(vh_element.long_clickable)
        self.metadata["password"].append(vh_element.password)
        self.metadata["scrollable"].append(vh_element.scrollable)
        self.metadata["resource-id"].append(vh_element.resource_id)
        self.metadata["text"].append(el_text)
        self.metadata["bbox"].append(el_bbox)
        self.metadata["form_id"].append(form_id)
        self.metadata["element_type"].append(el_type)
        self.metadata["attention_mask"].append(1)
        self.metadata["label"].append(el_label)
        self.metadata["img"].append(el_img)
        self.metadata["identifier"].append(identifier)
        self.metadata["original_bbox"].append(original_bbox)

    @classmethod
    def camelcase_split(cls, string: str):
        result = re.sub("([A-Z][a-z])", r" \1", string)
        result = re.sub("([A-Z][A-Z]+)", r" \1", result)
        return result

    def add_ref_exp_token(self, el_text):
        self.metadata["text"].append(el_text)
        self.metadata["bbox"].append([0, 0, 0, 0])
        self.metadata["form_id"].append(0)
        self.metadata["element_type"].append("text")
        self.metadata["attention_mask"].append(1)
        self.metadata["label"].append(0)
        self.metadata["img"].append(np.zeros([self.el_img_size, self.el_img_size, 3]))

    def parse_elements(self, sentence_encoder, element_list: List[Element]):
        for idx in range(len(element_list)):
            self.add_vh_element_data(element_list, idx)
        # post-process for VH. simply trim. (because how can we know which VH elements are more important as context?)
        if len(self.metadata["text"]) > self.max_cur_el_len:
            for el_attr in self.metadata:
                if isinstance(self.metadata[el_attr], list) and len(self.metadata[el_attr]) > 0:
                    self.metadata[el_attr] = self.metadata[el_attr][:self.max_cur_el_len]

        # final step. tokenize text and pad tokens.
        self.tokenizing_embedding_and_padding(sentence_encoder)
        return True

    def tokenizing_embedding_and_padding(self, sentence_encoder):
        # tokenize element type
        el_type_ids = [DOM_EL_TYPE_TO_IDX[el_type] for el_type in self.metadata["element_type"]]

        # tokenize bbox
        bboxes = []
        for bbox in self.metadata["bbox"]:
            normed_bbox = self.bbox_norm(bbox)
            assert normed_bbox[2] < self.max_normed_bbox_pos
            assert normed_bbox[3] < self.max_normed_bbox_pos
            bboxes.append(normed_bbox)

        texts = self.metadata["text"]

        try:
            imgs = torch.Tensor(np.array(self.metadata["img"])).permute(0, 3, 1, 2)
        except RuntimeError as error:
            raise BrokenScreenshotException(str(error.args[0]))

        # get embedding for text
        with torch.no_grad():
            embeds = sentence_encoder.encode(sentences=texts, show_progress_bar=False, convert_to_tensor=True)
            embeds = embeds.to(CPU)

        sentence_embeds = embeds
        assert len(sentence_embeds) == len(imgs) == len(texts) == len(bboxes) == len(el_type_ids)

        pad_len = self.max_seq_len - len(sentence_embeds)

        form_id = copy.copy(self.metadata["form_id"])
        url_lists = copy.copy(self.metadata["url_list"])
        assert (
                len(sentence_embeds)
                == len(imgs)
                == len(texts)
                == len(bboxes)
                == len(el_type_ids)
                == len(form_id)
            # == len(url_lists)  # commented because it violates the setting of VH.
        )
        assert (
                len(texts) < self.max_seq_len
        ), f"""elements number {len(texts)} is larger than max seq len {self.max_seq_len} in {self.metadata["filename"]}"""

        relative_bboxes = self.relative_bbox(bboxes, url_lists)
        relative_form_ids = self.relative_form_id(form_id, url_lists)

        # get padded for text
        # padding = torch.zeros(pad_len, EMBED_SHAPE).to(CPU)
        # padded_embeds = torch.cat((embeds, padding), dim=0)
        # self.content_features = padded_embeds
        sentence_embeds_padding = torch.zeros(pad_len, sentence_embeds.size(-1))
        sentence_embeds_padded = torch.cat((sentence_embeds, sentence_embeds_padding), dim=0)
        imgs_padding = torch.zeros(pad_len, 3, self.el_img_size, self.el_img_size)
        imgs_padded = torch.cat((imgs, imgs_padding), dim=0)
        self.txt_features = sentence_embeds_padded
        self.imgs = imgs_padded
        self.el_type_ids = torch.LongTensor(el_type_ids + [0] * pad_len)
        self.bboxes = torch.LongTensor(bboxes + [[0, 0, 0, 0]] * pad_len)
        self.attention_mask = torch.LongTensor(self.metadata["attention_mask"] + [0] * pad_len)
        self.relative_form_ids = torch.LongTensor(relative_form_ids)
        self.relative_bboxes = torch.LongTensor(relative_bboxes)
        self.el_num = torch.LongTensor([len(self.metadata["text"])])
        assert self.el_num.shape[0] == 1

    def bbox_norm(self, bbox, max_w=50, max_h=100):
        scale_factor_w = max_w / self.norm_max_width
        scale_factor_h = max_h / self.norm_max_height
        normed_bbox = [
            round(bbox[0] * scale_factor_w),
            round(bbox[1] * scale_factor_h),
            round(bbox[2] * scale_factor_w),
            round(bbox[3] * scale_factor_h),
        ]
        for idx in range(4):
            normed_bbox[idx] = clamp(normed_bbox[idx], 0, self.max_normed_bbox_pos - 1)
        return normed_bbox

    def relative_bbox(self, bboxes, url_lists):
        xylist = []
        relative_bboxes = torch.ones((self.max_seq_len, self.max_seq_len, 2)) * (-self.max_relative_bbox - 1)
        for bbox in bboxes:
            xylist.append([round(0.5 * bbox[0] + 0.5 * bbox[2]), round(0.5 * bbox[1] + 0.5 * bbox[3])])
        for i, xyi in enumerate(xylist):
            for j, xyj in enumerate(xylist):
                if url_lists and url_lists[i] != url_lists[j]:
                    relative_bboxes[i][j][0] = self.max_relative_bbox + 1
                    relative_bboxes[i][j][1] = self.max_relative_bbox + 1
                else:
                    relative_bboxes[i][j][0] = clamp(
                        round((xyi[0] - xyj[0]) / self.relative_scale), -self.max_relative_bbox, self.max_relative_bbox
                    )
                    relative_bboxes[i][j][1] = clamp(
                        round((xyi[1] - xyj[1]) / self.relative_scale), -self.max_relative_bbox, self.max_relative_bbox
                    )
        relative_bboxes = (relative_bboxes + self.max_relative_bbox + 1).long()
        return relative_bboxes

    def relative_form_id(self, form_id, url_lists):
        relative_form_ids = torch.zeros((self.max_seq_len, self.max_seq_len))
        i = 0
        j = 0
        for form_idi in form_id:
            j = 0
            for form_idj in form_id:
                if url_lists and url_lists[i] != url_lists[j]:
                    relative_form_ids[i][j] = 5
                elif form_idi == 0 and form_idj == 0:
                    relative_form_ids[i][j] = 1
                elif form_idi == 0 or form_idj == 0:
                    relative_form_ids[i][j] = 2
                elif form_idi != 0 and form_idj != 0:
                    if form_idi == form_idj:
                        relative_form_ids[i][j] = 3
                    else:
                        relative_form_ids[i][j] = 4
                j = j + 1
            i = i + 1
        return relative_form_ids.long()
