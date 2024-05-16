import time
import common.utils as utils
from common.action_definitions import ActionPlan, ExploreInstructions
from uiautomator2 import Device


class ActionExecutor:
    def __init__(self, device: Device = None):
        self.device = device
        self.logger = utils.build_file_logger("executor")

    def execute(self, action_plan: ActionPlan, explore_instructions: ExploreInstructions):
        for action in action_plan.actions:
            action.execute(self.device, action_plan.page_context, explore_instructions)
            self.logger.info(f"Executed action: {action.name}, thought: {action.thought} in plan: {action_plan.task}")
            time.sleep(1)
