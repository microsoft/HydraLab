import uiautomator2
from uiautomator2 import Device
from extractor import UIContextExtractor, UIPage
from navigation import Navigator
from common.action_definitions import ActionPlan, PageContext
from common.test_definitions import AppInfo
from decoder import UIDecoder
from action_executor import ActionExecutor
from langchain.prompts.prompt import PromptTemplate
from langchain_core.messages import SystemMessage
import traceback
from common import ui_navigation_prompt
from memory import ExploreMemory
from common.langchain_models import LangChainChatModels
from common.utils import build_file_logger, MyEncoder
from common.action_definitions import ExploreInstructions
from PIL import Image, ImageChops
import common.utils as utils
from langchain_core.pydantic_v1 import BaseModel, Field
from typing import List
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser
# from sentence_transformers import SentenceTransformer
# from sklearn.metrics.pairwise import cosine_similarity
import time
import json
from common.azure_openai_http import AzureAIRequestException


def should_resume(e, current_explore_stage, current_explore_stage1, previous_page_context, action_plan):
    if e and isinstance(e, AzureAIRequestException):
        return True
    if e and isinstance(e, json.decoder.JSONDecodeError):
        return True


class Steps(BaseModel):
    steps: List[str] = Field(description="List of steps to accomplish the instruction")


class SmartExplorer:

    def __init__(self, device_serial, app_info: AppInfo, explore_instructions: ExploreInstructions,
                 models: LangChainChatModels,
                 ui_extractor: UIContextExtractor,
                 page_decoder: UIDecoder, navigator: Navigator,
                 executor: ActionExecutor, memory: ExploreMemory):
        self.device = uiautomator2.connect(device_serial)
        self.app_info = app_info
        self.explore_instructions = explore_instructions
        self.models = models
        self.ui_extractor = ui_extractor
        self.page_decoder = page_decoder
        self.navigator = navigator
        self.executor = executor
        executor.device = self.device
        self.logger = build_file_logger(f"smart_explorer_{device_serial}_{self.app_info.package}")
        self.memory = memory
        self.step_counter = 0
        self.retry_counter = 0
        self.embedding_model = None
        self.parser_chain = None
        self.evaluate_diff_threshold = 0.03
        self.evaluate_image_width = 256

    def initialize_device(self):
        self.device.keyevent("KEYCODE_HOME")
        self.device.app_start(self.app_info.package, None, True, True, True)
        time.sleep(1)

    def explore(self):
        self.logger.info(f"------------------- Exploring {self.app_info.package} -------------------")
        self.initialize_device()
        self.parse_instructions()
        self.step_counter = 0

        previous_page_context = None
        action_plan = None
        current_explore_stage = ""
        should_try_another_action = False
        while True:
            self.logger.info(f"------------------- STEP {self.step_counter}-------------------")
            try:
                current_explore_stage = "Extract"
                page = self.ui_extractor.extract(self.device)

                current_explore_stage = "Decode"
                self.logger.info(f">> Page extraction is done. Start decoding...")
                current_page_context = self.page_decoder.decode(page, self.app_info, self.memory)

                current_explore_task = self.explore_instructions.steps[self.explore_instructions.current_step]
                if previous_page_context and action_plan:
                    current_explore_stage = "Evaluate"
                    should_try_another_action = False
                    current_page_context.previous_page_context = previous_page_context
                    current_page_context.previous_action_plan = action_plan
                    if self.evaluate_previous_action_plan(previous_page_context, action_plan, current_page_context,
                                                          self.explore_instructions):
                        update_status = self.memory.add(previous_page_context, action_plan, current_page_context)
                        if action_plan.finish_task_with_actions and (update_status.new_link_added or update_status.new_node_added):
                            self.logger.info(f"Take one step forward! Mark '{self.explore_instructions.get_current_step()}' as completed")
                            self.move_step_forward()
                    else:
                        should_try_another_action = True

                if self.explore_instructions.current_step >= len(self.explore_instructions.steps):
                    break

                current_explore_stage = "Navigate"
                self.logger.info(f">> Start navigating to complete task '{self.explore_instructions.get_current_step()}'")
                action_plan = self.navigator.navigate(self.app_info, self.explore_instructions, current_page_context,
                                                      self.memory, should_try_another_action)

                self.logger.debug(f"current_page_context elements: {json.dumps(current_page_context.key_elements, cls=MyEncoder)}")
                self.logger.debug(f"\taction: {json.dumps(action_plan.actions, cls=MyEncoder, indent=2)}")

                current_explore_stage = "Execute"
                self.logger.info(f">> Start executing...")
                self.executor.execute(action_plan, self.explore_instructions)

                current_explore_stage = "Wait"
                self.logger.info(f">> Wait_until_ui_stable...")
                self.wait_until_ui_stable(get_screenshot, page, threshold=0.01, timeout=30, check_interval=1)

                self.step_counter += 1
                previous_page_context = current_page_context
            except Exception as e:
                self.logger.exception(f"Error occurred: {e}\n\tDuring stage: {current_explore_stage}")
                if should_resume(e, current_explore_stage, current_explore_stage, previous_page_context, action_plan):
                    self.logger.warn(f"Resumed, retry the stage")
                    continue
                else:
                    self.logger.error(f"Cannot resume, stopping the exploration...")
                    break

    def should_stop(self, action_plan):
        if not action_plan:
            self.retry_counter += 1
            if self.retry_counter > 3:
                return True
            return False
        self.retry_counter = 0
        return False

    def parse_instructions(self):
        parser = JsonOutputParser(pydantic_object=Steps)
        prompt = ChatPromptTemplate.from_messages(
            [
                ("user", ui_navigation_prompt.instruction_breakdown_parser)
            ]
        )
        self.parser_chain = prompt | self.models.azure_gpt35_text_model | parser
        return_json = self.parser_chain.invoke({"instruction": self.explore_instructions.instructions,
                                                "app_context_description": self.app_info.description,
                                                "format_instructions": parser.get_format_instructions()})
        self.logger.info(f"Instruction breakdown: {return_json}")
        self.explore_instructions.steps = return_json['steps']

    def wait_until_ui_stable(self, get_screenshot_callback, page: UIPage, threshold=0.02, timeout=30, check_interval=1,
                             resize_width=320):
        """
        等待直到Android UI停止刷新并且画面保持静止。

        参数:
        get_screenshot (function): 一个函数，用于获取当前屏幕的截图，返回PIL Image格式的图像数据。
        threshold (float): 判断画面是否静止的阈值，表示允许的最大像素差异比例。
        timeout (int): 最大等待时间（秒）。
        check_interval (int): 检查间隔（秒），即每隔一定时间检查一次画面是否静止。

        返回:
        bool: 如果在超时时间内画面变为静止，则返回True；如果超时时间内画面仍未静止，返回False。
        stuck_loading_count: 页面卡在loading的次数
        """
        start_time = time.time()

        previous_screenshot = utils.resize_image_by_width(get_screenshot_callback(self.device), resize_width)
        stuck_loading_count = 0

        while (time.time() - start_time) < timeout:
            time.sleep(check_interval)
            window_width, window_height = self.device.window_size()
            if is_loading_page(page, window_width, window_height):
                stuck_loading_count += 1
                continue
            stuck_loading_count = 0
            current_screenshot = utils.resize_image_by_width(get_screenshot_callback(self.device), resize_width)
            current_screenshot = current_screenshot.resize(
                (resize_width, int(resize_width * previous_screenshot.height / previous_screenshot.width)))
            # resize the previous_screenshot to width 500
            if is_ui_stable(previous_screenshot, current_screenshot, threshold):
                return True, stuck_loading_count
            previous_screenshot = current_screenshot

        return False, stuck_loading_count

    def evaluate_previous_action_plan(self, previous_page_context: PageContext, action_plan: ActionPlan,
                                      current_page_context: PageContext, explore_instructions: ExploreInstructions):
        with current_page_context.load_screenshot_image() as current_image, previous_page_context.load_screenshot_image() as previous_image:
            diff_ratio = utils.get_image_difference_ratio(
                utils.resize_image_by_width(current_image, self.evaluate_image_width),
                utils.resize_image_by_width(previous_image, self.evaluate_image_width))
        self.logger.info(f"Evaluating: Image diff ratio: {diff_ratio}")
        if diff_ratio < self.evaluate_diff_threshold:
            # 如果有页面焦点发生变化，也算有效操作
            if current_page_context.focused_element and not previous_page_context.focused_element:
                return True
            # means the action didn't work out as expected
            self.logger.warn(
                f"ActionPlan ({action_plan}) didn't work out as expected, should retry another... image diff ratio: {diff_ratio}")
            return False
        return True

    def move_step_forward(self):
        self.explore_instructions.current_step += 1


def get_screenshot(device: Device):
    return device.screenshot()


def is_loading_page(page: UIPage, window_width, window_height):
    elements = page.root.find_all('node', recursive=True)
    for element in elements:
        if element.has_attr("visible-to-user") and element["visible-to-user"].strip().lower() == "false":
            continue
        # if the element is not in the center section of the screen, skip it
        bounds = element["bounds"] if element.has_attr("bounds") else None
        if bounds:
            if not utils.is_bounds_in_center(bounds, window_width, window_height, 0.4, 0.4):
                continue
        if element.has_attr("text") and "loading" in element["text"].strip().lower():
            return True
        if element.has_attr("content-desc") and "loading" in element["content-desc"].strip().lower():
            return True
    return False


def has_pop_up(page: UIPage):
    elements = page.root.find_all(recursive=True)
    for element in elements:
        if element.has_attr("visible-to-user") and element["visible-to-user"].strip().lower() == "false":
            continue
        if element.has_attr("text") and "loading" in element["text"].strip().lower():
            return True
        if element.has_attr("content-desc") and "loading" in element["content-desc"].strip().lower():
            return True
    return False


def is_ui_stable(previous_screenshot: Image, current_screenshot: Image, threshold):
    """
    使用Pillow库比较两个截图的差异，判断UI是否稳定。

    参数:
    previous_screenshot (PIL Image): 前一个屏幕截图。
    current_screenshot (PIL Image): 当前屏幕截图。
    threshold (float): 差异阈值。

    返回:
    bool: 如果差异小于阈值，则认为UI静止。
    """
    # 计算两个截图的差异
    diff_ratio = utils.get_image_difference_ratio(current_screenshot, previous_screenshot)
    # 根据阈值判断UI是否静止
    return diff_ratio < threshold
