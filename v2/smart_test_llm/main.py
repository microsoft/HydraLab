from smart_explore import SmartExplorer
from common.action_definitions import ExploreInstructions
from extractor import UIContextExtractor
from decoder import GPT4VUIDecoder
import common.test_definitions as test_definitions
from navigation import Navigator, GPTNavigator, GPT4VNavigator
from common.langchain_models import LangChainChatModels
from action_executor import ActionExecutor
from memory import ExploreMemory
import adbutils
import os
from dotenv import load_dotenv, find_dotenv

_ = load_dotenv(find_dotenv())

# get the AZURE_OPENAI_DEPLOYMENT environment variable
azure_openai_deployment = os.getenv("AZURE_OPENAI_DEPLOYMENT")

test_set = test_definitions.launcher_test_set


def main():
    adb = adbutils.AdbClient(host="127.0.0.1", port=5037)
    device_list = adb.device_list()
    chosen_device = device_list[0]
    # Initialize the SmartExplorer object

    langchain_models = LangChainChatModels()

    explorer = SmartExplorer(
        chosen_device.serial,
        test_set.app_info,
        explore_instructions=ExploreInstructions(test_set.test_instruction_login_and_change_setting,
                                                 test_set.test_identity, None),
        models=langchain_models,
        ui_extractor=UIContextExtractor("Android", "en"),
        page_decoder=GPT4VUIDecoder(langchain_models),
        navigator=GPTNavigator(langchain_models),
        executor=ActionExecutor(),
        memory=ExploreMemory("0")
    )

    # Start the exploration process
    explorer.explore()


if __name__ == '__main__':
    main()
