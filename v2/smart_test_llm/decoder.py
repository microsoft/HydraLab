from bs4 import BeautifulSoup
import common.ui_navigation_prompt as ui_navigation_prompt
from common.test_definitions import AppInfo
from extractor import UIPage
import common.constants as const
from common.langchain_models import LangChainChatModels
from common.action_definitions import PageContext
import common.utils as utils
from memory import ExploreMemory


def is_element_attribute_empty(element, attribute_name):
    # if the attribute is not present, return True
    if not element.has_attr(attribute_name):
        return True
    # if the attribute is present but empty, return True
    if not element[attribute_name]:
        return True
    if not element[attribute_name].strip():
        return True
    return False


class UIDecoder:
    def decode(self, page: UIPage, explored_app_info: AppInfo, memory: ExploreMemory) -> PageContext:
        pass


class GPT4VUIDecoder(UIDecoder):
    def __init__(self, models: LangChainChatModels):
        self.model = models.azure_gpt4_vision_model
        self.logger = utils.build_file_logger("page_decoder")

    def decode(self, page: UIPage, explored_app_info: AppInfo, memory: ExploreMemory) -> PageContext:
        """
        The key information that defines the context of the page:
        - The interactable elements on the page
        - The type of the page
        - The top activity of the page
        - The app package
        - The text on the page
        - The depth of the page
        - The screenshot summary of the page
        :param page:
        :param explored_app_info:
        :param memory:
        :return:
        """
        page_context = PageContext(explored_app_info.package, page.screenshot_file, page.contexts["top_activity"],
                                   page.contexts["top_package"])
        # iterate through the page and extract useful information
        add_elements(page.root, page_context)
        searched_page_context = memory.locate_page_by_screenshot(page.screenshot_file)
        if searched_page_context and searched_page_context.top_activity == page_context.top_activity:
            page_context.scenario_category = searched_page_context.scenario_category
            page_context.page_possible_actions = searched_page_context.page_possible_actions
            page_context.alike_page_context = searched_page_context
            self.logger.info(f"Reuse the context: {searched_page_context.scenario_category}/{searched_page_context.node_id}")
            return page_context

        self.logger.info("start vision completion")
        resp = self.model.chat_completion_with_image(
            ui_navigation_prompt.system_message_screenshot_vision,
            ui_navigation_prompt.screenshot_vision_prompt.format(
                page_decode_json_example=ui_navigation_prompt.vision_prompt_return_example,
                page_categories=ui_navigation_prompt.page_categories
            ), page.screenshot_file, 480)
        self.logger.debug(f"Vision response {resp.get_total_tokens()}: {resp.get_default_choice_content()}")
        page_usage_json = resp.extract_default_choice_json_content()
        page_context.scenario_category = page_usage_json["scenario_category"]
        self.logger.info(f"Page Category: {page_context.scenario_category}")
        page_context.page_possible_actions = page_usage_json["actions"]
        return page_context


def add_elements(root: BeautifulSoup, page_context: PageContext):
    elements = root.find_all('node', recursive=True)
    display_texts = []
    for element in elements:
        take_the_element = True
        element_class_strip = element["class"].replace("android.widget.", "") if element.has_attr("class") else ""
        element_resource_id = f'|{element["resource-id"]}' if element.has_attr("resource-id") and element["resource-id"] else ""
        element_content_desc = f'|{element["content-desc"]}' if element.has_attr("content-desc") and element["content-desc"] else ""
        element_index = f'|{element["index"]}' if element.has_attr("index") and element["index"] else ""
        descriptor = f"{element_class_strip}{element_resource_id}{element_content_desc}-{element_index}"
        adding_element = {
            'text': element["text"] if element.has_attr("text") else None,
            'descriptor': descriptor,
            'bounds': element["bounds"] if element.has_attr("bounds") else None,
            'scrollable': element["scrollable"] if element.has_attr("scrollable") else None,
        }
        if is_element_attribute_empty(element, "text") and is_element_attribute_empty(element, "content-desc"):
            take_the_element = False
        if not is_element_attribute_empty(element, "text"):
            display_texts.append({'t': element["text"], 'b': element["bounds"]})
        if not is_element_attribute_empty(element, "content_desc"):
            display_texts.append({'c': element["content_desc"], 'b': element["bounds"]})
        if element.has_attr("class") and element["class"].strip() in const.android_widget.CONTAINER_CLASS:
            take_the_element = False
        if element.has_attr("class") and element["class"].strip() in const.android_widget.INTERACTABLE:
            take_the_element = True
        if element.has_attr("visible-to-user") and element["visible-to-user"].strip().lower() == "false":
            take_the_element = False
        if element.has_attr("clickable") and element["clickable"].strip().lower() == "true":
            take_the_element = True
        # if element.has_attr("enabled") and element["enabled"].strip().lower() == "false":
        #     take_the_element = False
        if element.has_attr("focused") and element["focused"].strip().lower() == "true":
            take_the_element = True
            page_context.focused_element = adding_element
        if element.has_attr("selected") and element["selected"].strip().lower() == "true":
            take_the_element = True
        if element.has_attr("scrollable") and element["scrollable"].strip().lower() == "true":
            take_the_element = True

        if take_the_element:
            adding_element["index"] = len(page_context.key_elements)
            page_context.key_elements.append(adding_element)
    page_context.display_texts = display_texts
