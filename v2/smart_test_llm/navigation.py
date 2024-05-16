import os

from langchain_core.output_parsers import JsonOutputParser

from common.test_definitions import AppInfo
from common.action_definitions import PageContext
from typing import List
from langchain.prompts.chat import (
    ChatPromptTemplate,
    HumanMessagePromptTemplate,
    SystemMessagePromptTemplate,
)
from langchain.memory import VectorStoreRetrieverMemory, ConversationBufferMemory
from langchain_core.messages import SystemMessage
from langchain.chains import ConversationChain
from dotenv import load_dotenv, find_dotenv
from common.action_definitions import Action, ActionPlan, ExploreInstructions
from memory import ExploreMemory
from common.utils import build_file_logger, MyEncoder, extract_json
from common import ui_navigation_prompt
from common.langchain_models import LangChainChatModels
import common.constants as const
import json
import common.utils as utils

_ = load_dotenv(find_dotenv())


class Navigator:
    def __init__(self):
        self.logger = build_file_logger("navigator")

    def navigate(self, app_info: AppInfo, explore_instructions, page_context: PageContext,
                 memory: ExploreMemory, should_try_another_action) -> ActionPlan:
        memory.locate_page_by_descriptor(page_context)
        target_element = self.choose_elements(app_info, explore_instructions, page_context, memory)
        if not target_element:
            go_back = ActionPlan()
            go_back.actions.append(Action(None, 'back'))
            return go_back
        actions = self.decide_action(app_info, explore_instructions, target_element, page_context, memory)
        action_plan = ActionPlan()
        action_plan.instructions = explore_instructions
        action_plan.actions.extend(actions)
        return action_plan

    def decide_action(self, app_info: AppInfo, explore_instructions, element, page_context, memory) -> List[Action]:
        element_class = element["class"].strip()
        if element_class in const.android_widget.EDIT_TEXT:
            return [Action(element, 'input', {"input_text": "tflplxtest14@outlook.com"})]
        if element_class in const.android_widget.BUTTON:
            return [Action(element, 'click')]
        if element_class in const.android_widget.RECYCLER_VIEW:
            return [Action(element, 'swipe_up')]
        return [Action(element, 'click')]

    def choose_elements(self, app_info: AppInfo, explore_instructions, page_context: PageContext,
                        memory: ExploreMemory):
        elements = page_context.key_elements
        clickable_elements = [e for e in elements if not e["enabled"] == "false"]
        for e in clickable_elements:
            if is_empty_bounds(e["bounds"]):
                continue
            if e["package"] == "com.android.systemui":
                continue
            query_result = memory.query_element(json.dumps(e))
            if len(query_result['documents'][0]) == 1:
                distance = query_result['distances'][0][0]
                if distance < 0.05:
                    continue
            self.logger.debug(f"query result: {query_result}")
            return e
        return None


def is_empty_bounds(bounds):
    bounds = bounds[1:-1].split("][")
    x1, y1 = map(int, bounds[0].split(","))
    x2, y2 = map(int, bounds[1].split(","))
    return x1 == x2 and y1 == y2


class GPTNavigator(Navigator):
    def __init__(self, langchain_chat_models: LangChainChatModels):
        super().__init__()
        self.model = langchain_chat_models.azure_gpt4_text_model
        prompt = ChatPromptTemplate.from_messages(
            [
                SystemMessagePromptTemplate.from_template(ui_navigation_prompt.system_message_android_ui_navi_prompt),
                # SystemMessagePromptTemplate.from_template(ui_navigation_prompt.memory_prompt),
                HumanMessagePromptTemplate.from_template(ui_navigation_prompt.ui_navi_prompt),
            ]
        )
        self.chain = prompt | self.model | JsonOutputParser()

    def navigate(self, app_info: AppInfo, explore_instructions: ExploreInstructions, page_context: PageContext,
                 memory: ExploreMemory, should_try_another_action) -> ActionPlan:
        prompt = ChatPromptTemplate.from_messages(
            [
                SystemMessagePromptTemplate.from_template(ui_navigation_prompt.system_message_android_ui_navi_prompt),
                # SystemMessagePromptTemplate.from_template(ui_navigation_prompt.memory_prompt),
                HumanMessagePromptTemplate.from_template(ui_navigation_prompt.ui_navi_prompt),
            ]
        )
        # TODO 这里加上conversationBufferMemory?
        action_plan_dict = self.model.invoke(
            [prompt.format(
                explore_instruction=explore_instructions.instructions,
                current_step_index=explore_instructions.current_step,
                current_step=explore_instructions.steps[explore_instructions.current_step],
                app_description=app_info.description,
                page_scenario_category=page_context.scenario_category,
                key_elements=json.dumps(page_context.key_elements, cls=MyEncoder),
                focused_element_desc=f"The page has a focus on element {json.dumps(page_context.focused_element)}" if page_context.focused_element else "",
                top_activity=page_context.top_activity,
                current_app_package=page_context.current_app_package,
                test_identity=json.dumps(explore_instructions.identity, cls=MyEncoder),
                previous_action=page_context.previous_action_plan.actions[-1].name if page_context.previous_action_plan and len(page_context.previous_action_plan.actions) > 0 else "none",
                previous_scenario_category=page_context.previous_page_context.scenario_category if page_context.previous_page_context else "none",
                previous_step=page_context.previous_action_plan.task if page_context.previous_action_plan else "none",
                extra_spec="" if not should_try_another_action else ", but it didn't not work, please try another action.",
                supported_actions_desc=json.dumps(page_context.page_possible_actions),
                action_plan_format=ui_navigation_prompt.action_plan_format,
                supported_actions=json.dumps(Action.SUPPORTED_ACTIONS),
                action_plan_example_1=ui_navigation_prompt.action_plan_example_1,
                action_plan_example_2=ui_navigation_prompt.action_plan_example_2,
                action_error_example=ui_navigation_prompt.action_error_example)]
        ).content

        self.logger.info(f"Action plan from GPT: {action_plan_dict}\n")
        action_plan_dict = utils.extract_json(action_plan_dict)
        action_plan = ActionPlan(page_context, explore_instructions.steps[explore_instructions.current_step])
        action_plan.finish_task_with_actions = action_plan_dict["finish_task_with_actions"]
        action_plan.description = action_plan_dict["description"]
        for action in action_plan_dict["actions"]:
            action_plan.actions.append(
                Action(action["name"], action["thought"], action.get("spec",None), action.get("data", None),
                       action.get("target_element_index", None))
            )
        return action_plan


class GPT4VNavigator(Navigator):
    def __init__(self, langchain_chat_models: LangChainChatModels):
        super().__init__()
        self.model = langchain_chat_models.azure_gpt4_vision_model

    def navigate(self, app_info: AppInfo, explore_instructions: ExploreInstructions, page_context: PageContext,
                 memory: ExploreMemory, should_try_another_action) -> ActionPlan:
        # TODO

        action_plan = ActionPlan()
        # for action in action_plan_dict["actions"]:
        #     action_plan.actions.append(Action(action["target"], action["action"]))
        return action_plan
