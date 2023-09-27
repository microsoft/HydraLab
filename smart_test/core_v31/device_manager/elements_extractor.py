import copy
import json
import os
import time
import xml.etree.ElementTree as ET
from collections import defaultdict
from typing import List, Dict

from appium import webdriver
from loguru import logger

from common.constants import android_widget, platform
from entities.element import Element


def extractor(node, parent, xpath, element_dict, keep_content=False):
    i_xpath = xpath + "/*[@index='" + node.attrib.get("index", "0") + "']"
    children = list(node)
    class_name = node.attrib.get("class", "")
    interactable = node.attrib.get("clickable", "false").lower() == 'true' \
                   or node.attrib.get("long-clickable", "false").lower() == 'true' \
                   or node.attrib.get("scrollable", "false").lower() == 'true' \
                   or class_name in android_widget.INTERACTABLE
    extracted = 0

    # WebView: If text is not empty, ignore all child nodes.
    if class_name in android_widget.WEB_VIEW:
        text = node.attrib.get("text", "")
        if text != "":
            if text == "Sign in to your Microsoft account" or "Sign in":
                # mark this page as LOGIN start page
                element_dict["is_microsoft_login"] = True
                pass
            else:
                element = element_builder(node, 'clickable', i_xpath, parent, keep_content)
                element_dict["vh_elements"].append(element)
                return True, 1, 0, False
        elif not children:
            # resource_id = node.attrib.get("resource-id", "")
            # if resource_id == "com.microsoft.appmanager:id/oneauth_navigation_web_view":
            return False, 0, -1, True
    '''
    RecyclerView: xpath
      Follow-up operations:
      list: random click on two of them, priority click on the first one
      setting & menu: traverse
      others: priority try to scroll
    In other cases, if all child nodes of a clickable element are not clickable, ignore those child nodes
    '''
    children_interactable = False
    children_extracted = 0
    height = -1

    if len(children) > 0:
        parent.append(class_name)
        for child_node in children:
            child_interactable, child_extracted, child_height, need_retry = extractor(child_node, parent, i_xpath, element_dict)
            if need_retry:
                return False, 0, -1, True
            children_interactable = child_interactable or children_interactable
            children_extracted += child_extracted
            height = max(height, child_height)
        parent.pop()

        children_attrib = defaultdict(list)
        for child_element in element_dict["vh_elements"][-children_extracted:]:
            if child_element.text != '':
                children_attrib["text"].append(child_element.text)
            if child_element.content_desc != ['']:
                children_attrib["content-desc"] += child_element.content_desc
            children_attrib["class"].append(child_element.element_class)
            children_attrib["xpath"].append(child_element.xpath)

        if class_name in android_widget.RECYCLER_VIEW and children_extracted > 1:
            children_attrib["xpath"].insert(0, i_xpath)
            element = element_builder(node, 'clickable', ';'.join(children_attrib["xpath"]), parent, keep_content)
            element_dict["vh_elements"].insert(len(element_dict["vh_elements"]) - children_extracted, element)
            extracted += (children_extracted + 1)
            height += 1
        elif interactable and not children_interactable and height < 5:
            if children_extracted > 0:
                del element_dict["vh_elements"][-children_extracted:]
                if not node.attrib.get('text', ''):
                    node.set('text', ';'.join(children_attrib["text"]))
                if not node.attrib.get('content-desc', ''):
                    node.set('content-desc', ';'.join(children_attrib["content-desc"]))

            element = element_builder(node, 'clickable', i_xpath, parent, keep_content)
            element_dict["vh_elements"].append(element)
            extracted += 1
            height = 0
        else:
            extracted += children_extracted
            height += 1
    else:
        # Leaf nodes
        element = None

        if class_name in android_widget.EDIT_TEXT:
            element = element_builder(node, 'input', i_xpath, parent, keep_content)
        elif interactable or class_name in android_widget.BUTTON:
            element = element_builder(node, 'clickable', i_xpath, parent, keep_content)
        # Ignore non-interactive elements with no text and description or text longer than 100.
        elif class_name in android_widget.IMAGE_VIEW:
            element = element_builder(node, 'icon', i_xpath, parent, keep_content)
        elif (len(node.attrib.get("text", "")) <= 100 or keep_content) and (node.attrib.get("text", "") != "" or node.attrib.get("content-desc", "") != ""):
            element = element_builder(node, 'text', i_xpath, parent, keep_content)

        if element:
            element_dict["vh_elements"].append(element)
            extracted += 1
            height = 0
        else:
            extracted = 0
            height = -1

    return interactable or children_interactable, extracted, height, False


def element_builder(node, element_type, xpath, parent, keep_content):
    element = Element(
        element_type=element_type,
        element_class=node.attrib.get("class", ""),
        text=node.attrib.get("text", ""),
        text_hint=node.attrib.get("text-hint", ""),
        content_desc=[node.attrib.get("content-desc", "")],
        resource_id=node.attrib.get("resource-id", ""),
        checkable=(node.attrib.get("checkable", "false").lower() == 'true'),
        checked=(node.attrib.get("checked", "false").lower() == 'true'),
        clickable=(node.attrib.get("clickable", "false").lower() == 'true'),
        enabled=(node.attrib.get("enabled", "false").lower() == 'true'),
        focusable=(node.attrib.get("focusable", "false").lower() == 'true'),
        long_clickable=(node.attrib.get("long-clickable", "false").lower() == 'true'),
        password=(node.attrib.get("password", "false").lower() == 'true'),
        scrollable=(node.attrib.get("scrollable", "false").lower() == 'true'),
        selected=(node.attrib.get("selected", "false").lower() == 'true'),
        bounds=node.attrib.get("bounds", "[0,0][0,0]"),
        displayed=(node.attrib.get("displayed", "false").lower() == 'true'),
        xpath=xpath,
        ancestors=parent.copy(),
        image_override=(element_type == "icon" and not keep_content),
    )

    return element


def split_element_dict(element_dict):
    elements_list = element_dict["vh_elements"]
    new_elements_list = []
    for idx, element in enumerate(elements_list):
        clickable, long_clickable, scrollable = element["clickable"], element["long-clickable"], element["scrollable"]
        if element["element_type"] == 'clickable':
            if clickable:
                new_element = copy.deepcopy(element)
                new_element["long-clickable"], new_element["scrollable"] = False, False
                new_elements_list.append(new_element)
            if long_clickable:
                new_element = copy.deepcopy(element)
                new_element["clickable"], new_element["scrollable"] = False, False
                new_elements_list.append(new_element)
            if scrollable:
                new_element = copy.deepcopy(element)
                new_element["clickable"], new_element["long-clickable"] = False, False
                new_elements_list.append(new_element)
            if not clickable and not long_clickable and not scrollable:
                new_elements_list.append(copy.deepcopy(element))
        else:
            new_elements_list.append(copy.deepcopy(element))

    new_element_dict = {"vh_elements": new_elements_list}

    return new_element_dict


def parse_page_source(page_source, save_file_name=None) -> Dict[str, List]:
    page_source_tree = ET.XML(page_source)
    root_node = list(page_source_tree)[0]
    parent = []
    element_dict = {"vh_elements": [], "is_microsoft_login": False}
    _, _, _, need_retry = extractor(root_node, parent, "/*[@index='0']", element_dict)
    if need_retry:
        # add wait time for render of WebView in Microsoft LOGIN page specifically.
        time.sleep(3)
        logger.warning("Retry on parse page source as it's not rendered completely")
        return dict()

    if save_file_name:
        with open(f"{save_file_name}.json", "w", encoding="utf-8") as f:
            json.dump(element_dict, f, sort_keys=True)
    return element_dict


if __name__ == "__main__":
    desired_caps = {
        'platformName': 'Android',
        'udid': "RFCT40A8FYP",
    }

    driver = webdriver.Remote(f'http://127.0.0.1:4723/wd/hub', desired_caps)
    driver.implicitly_wait(1)

    parse_page_source(driver.page_source)

    dataset_dir = ''
    filenames = sorted(list(filter(lambda x: x.endswith(".xml"), os.listdir(dataset_dir))))
    for filename in filenames:
        with open(os.path.join(dataset_dir, filename)) as f:
            page_source = f.read()
        if len(page_source) > 0:
            print(filename[:-4])
            parse_page_source(page_source, os.path.join(dataset_dir, filename)[:-4])
