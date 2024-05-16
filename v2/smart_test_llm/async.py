import threading
from smart_explore import SmartExplorer, ExploreInstructions
from extractor import UIContextExtractor
from decoder import GPT4VUIDecoder
from navigation import Navigator, GPTNavigator
from action_executor import ActionExecutor
from memory import ExploreMemory
import adbutils


def run(device_serial, index, app_package="com.microsoft.amp.apps.bingnews"):
    explorer = SmartExplorer(
        device_serial,
        app_package,
        explore_instructions=ExploreInstructions(None, None, None),
        ui_extractor=UIContextExtractor("Android", "en"),
        page_decoder=GPT4VUIDecoder(),
        navigator=Navigator(),
        executor=ActionExecutor(),
        memory=ExploreMemory(f"{index}")
    )
    print(f"Explorer {index} start to run on {device_serial}")
    # Start the exploration process
    explorer.explore()


def main():
    adb = adbutils.AdbClient(host="127.0.0.1", port=5037)
    device_list = adb.device_list()
    threads = []
    for i, device in enumerate(device_list):
        thread = threading.Thread(target=run, args=(device.serial, i))
        threads.append(thread)
        thread.start()
    for thread in threads:
        thread.join()


if __name__ == '__main__':
    main()
