import time, json, os
from typing import List
from uiautomator2 import Device
import common.utils as utils
from PIL import Image
from uiautomator2.exceptions import UiObjectNotFoundError


class ActionErrorException(Exception):
    pass


def find_element_by_index(target_element_index, key_elements):
    try:
        return key_elements[int(target_element_index)]
    except Exception:
        return None


class Action:
    SUPPORTED_ACTIONS = ["swipe", "click", "input", "back", "long_click", "key_event"]

    # "error", "wait", "drag", "check"
    logger = utils.build_file_logger("action")

    def __init__(self, name, thought=None, action_type=None, data=None, target_element_index=None):
        self.name = name
        self.thought = thought
        self.action_type = action_type
        self.data = data
        self.target_element_index = target_element_index
        self.element_descriptor = None

    def __repr__(self):
        element_desc = f" on {self.element_descriptor}" if self.element_descriptor else f" on {self.target_element_index}" if self.target_element_index else ""
        return f"{self.name} with {self.action_type}{element_desc}"

    def __eq__(self, other):
        return self.name == other.name and self.action_type == other.action_type and self.target_element_index == other.target_element_index and self.data == other.data

    def execute(self, device: Device, page_context, explore_instructions):
        if self.name == "back":
            device.keyevent("KEYCODE_BACK")
        if self.name == "swipe":
            device.swipe_ext('left' if self.action_type == 'right' else 'right', scale=float(self.data))
        if self.name == "click":
            element = find_element_by_index(self.target_element_index, page_context.key_elements)
            if not element:
                Action.logger.warning(f"Element not found by index: {self.target_element_index}")
                return
            self.element_descriptor = f'{element["descriptor"]}' + (
                f'with text: {element["text"]}' if element["text"] else "")
            coordinates = utils.get_center(element["bounds"])
            device.click(coordinates[0], coordinates[1])
            Action.logger.info(f"Clicked on element: {element} with coordinates: {coordinates}")
        if self.name == "input":
            if self.target_element_index:
                element = find_element_by_index(self.target_element_index, page_context.key_elements)
                if not element:
                    Action.logger.warning(f"Element not found by index: {self.target_element_index}")
                    return
                coordinates = utils.get_center(element["bounds"])
                device.click(coordinates[0], coordinates[1])
            try:
                if "password" in self.data.lower():
                    self.data = explore_instructions.identity['password']
                device.send_keys(self.data, True)
            except UiObjectNotFoundError as e:
                Action.logger.warning(f"No element selected: {e}")
        if self.name == "drag":
            pass
        if self.name == "long_click":
            element = find_element_by_index(self.target_element_index, page_context.key_elements)
            if not element:
                Action.logger.warning(f"Element not found by index: {self.target_element_index}")
                return
            coordinates = utils.get_center(element["bounds"])
            device.long_click(coordinates[0], coordinates[1])
        if self.name == "key_event":
            device.keyevent(self.data)
        if self.name == "error":
            raise ActionErrorException(self.thought)
        if self.name == "wait":
            wait_time = int(self.data) if self.data else 0
            wait_time = wait_time / 1000 if wait_time > 1000 else wait_time
            time.sleep(wait_time)
        if self.name == "check":
            if self.action_type == "text":
                element = page_context.key_elements[self.target_element_index]
                assert element["text"] == self.data
            if self.action_type == "content-desc":
                element = page_context.key_elements[self.target_element_index]
                assert element["content-desc"] == self.data


class ExploreInstructions:
    def __init__(self, instructions, identity, test_data):
        self.instructions = instructions
        self.steps = []
        self.current_step = 0
        self.identity = identity
        self.test_data = test_data

    def get_current_step(self):
        return self.steps[self.current_step]


class ActionPlan:
    def __init__(self, page_context, task):
        self.page_context = page_context
        self.task: str = task
        self.description = ""
        self.actions: List[Action] = []
        self.finish_task_with_actions = False

    def __repr__(self):
        actions_descriptors = [action for action in self.actions]
        return f"Action plan: {actions_descriptors}"

    def __eq__(self, other):
        return self.actions == other.actions and self.page_context == other.page_context

    def compare_action_content(self, other):
        return self.actions == other.actions


class PageContext:
    def __init__(self, target_app_package, screenshot_file, top_activity, current_app_package):
        self.target_app_package = target_app_package
        self.screenshot_file = screenshot_file
        self.top_activity = top_activity
        self.current_app_package = current_app_package
        self.key_elements = []
        self.page_possible_actions = []
        self.display_texts = []
        self.element_content_descriptions = []
        self.previous_action_plan: ActionPlan = None
        self.previous_page_context: PageContext = None
        self.scenario_category = None
        self.focused_element = None
        self.node_id = None
        self.alike_page_context: PageContext = None

    def __hash__(self):
        return (hash(self.display_texts) + hash(self.top_activity)
                + hash(self.current_app_package)) + hash(self.scenario_category)

    def __eq__(self, other):
        if self.top_activity == other.top_activity and self.current_app_package == other.current_app_package and self.scenario_category == other.scenario_category:
            width = 256
            diff_ratio = utils.get_image_difference_ratio(
                utils.resize_image_by_width(self.load_screenshot_image(), width),
                utils.resize_image_by_width(other.load_screenshot_image(), width))
            return diff_ratio < 0.01
        return False

    def update_node_id(self, node_id: str):
        self.node_id = node_id
        self.rename_screenshot_file_name_by_id(node_id)

    def get_page_descriptor(self):
        content_description = f" and element content descriptions {json.dumps(self.element_content_descriptions)}" \
            if len(self.element_content_descriptions) > 0 else ""
        return f"{self.scenario_category} page of activity {self.top_activity} in app {self.current_app_package}" \
               f" with display texts: {json.dumps(self.display_texts)}{content_description} on it"

    def load_screenshot_image(self) -> Image:
        return Image.open(self.screenshot_file)

    def rename_screenshot_file_name_by_id(self, node_id):
        new_name = f"{node_id}_{os.path.basename(self.screenshot_file)}"
        self.screenshot_file = utils.rename_file(self.screenshot_file, new_name)
