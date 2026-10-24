import json
import os
import sys
import time
import traceback
import ssl
from typing import Dict, List, Optional

from loguru import logger

from common.constants import platform
from decision_maker.decision_metric import DecisionMetricCounter
from decision_maker.depth_first_search import SingleDeviceDFSDecisionMaker
from device_manager.device_manager_android import DeviceManagerAndroid
from entities.device import Device
from entities.page_info import PageInfo
from entities.test_app import TestApplication
from factory.decision_maker_factory import DecisionMakerFactory
from factory.device_manager_factory import DeviceManagerFactory
from factory.page_encoder_factory import PageEncoderFactory
from llm.lang_chain_client import LLM
from screen_comprehension.classifier.classifier import Classifier
from screen_comprehension.screen_comprehension_screenbert import ScreenBertComprehension
from utils.exception import BrokenScreenshotException
from utils.helper import logger_init, load_strings, get_package_name

ssl._create_default_https_context = ssl._create_unverified_context


class SmartTest:
    def __init__(self, devices: List[Device], apps: Dict[Device, Optional[List[TestApplication]]], strategy,
                 page_encoder, screen_comprehension_kwargs, string_pool_dir, output_dir, llm_param, metric_counter):
        self.devices = devices
        self.apps = apps
        self.output_dir = output_dir

        strings = load_strings(string_pool_dir)

        self.device_manager_factory = DeviceManagerFactory()
        self.page_encoder_factory = PageEncoderFactory()
        self.decision_maker_factory = DecisionMakerFactory()

        for device in devices:
            if device.platform == platform.ANDROID:
                device_manager = DeviceManagerAndroid
            else:
                raise ValueError
            self.device_manager_factory.register_and_create_device_manager(device, apps[device], device_manager)

        if strategy == "DFS":
            self.decision_maker = self.decision_maker_factory.register_and_create_decision_maker(strategy,
                                                                                                 SingleDeviceDFSDecisionMaker,
                                                                                                 **{"strings": strings,
                                                                                                    "metric_counter": metric_counter})
        else:
            raise ValueError

        if page_encoder == "ScreenBert":
            self.page_encoder = self.page_encoder_factory.register_and_create_page_encoder(page_encoder,
                                                                                           ScreenBertComprehension,
                                                                                           **screen_comprehension_kwargs)
        else:
            raise ValueError

        self.llm = None
        if llm_param and llm_param.get('enable_llm'):
            self.llm = LLM(deployment_name=llm_param.get("deployment_name"),
                           openai_api_key=llm_param.get("openai_api_key"),
                           openai_api_base=llm_param.get("openai_api_base"),
                           openai_api_version=llm_param.get("openai_api_version"),
                           temperature=0.0)

    def extract_page_info(self):
        page_info_of_each_device = {}
        for device in self.devices:
            device_manager = self.device_manager_factory.get_device_manager(device)
            sub_page_info = device_manager.extract_page_info()
            page_info_of_each_device.update({device: sub_page_info})

        return page_info_of_each_device

    def encode_page(self, page_info_of_each_device: Dict[Device, PageInfo], method='separately'):
        frame_feature_of_each_device = {}
        if method == "separately":
            for device, page_info in page_info_of_each_device.items():
                frame_feature_of_each_device.update({device: self.page_encoder.encode(device, page_info)})
        else:
            raise ValueError
        return frame_feature_of_each_device

    def start(self, max_step=sys.maxsize):
        # for device in self.devices:
        #     self.device_manager_factory.get_device_manager(device).reset()
        device_manager = self.device_manager_factory.get_device_manager(self.devices[0])
        device_manager.reset()

        done = False
        curr_step = 0
        while (not done) and curr_step < max_step:
            page_info_of_each_device = self.extract_page_info()
            try:
                frame_feature_of_each_device = self.encode_page(page_info_of_each_device)
                actions, done, info = self.decision_maker.action_selector(self.output_dir, frame_feature_of_each_device,
                                                                          self.llm, curr_step)
            except BrokenScreenshotException as exception:
                logger.error(exception)
                actions, done, info = self.decision_maker.exceptional_restart(self.devices[0])

            logger.info(info)

            self.decision_maker.before_action()

            for action in actions:
                logger.info(action)
                device_manager.execute_action(action)

            self.decision_maker.after_action()
            curr_step = curr_step + 1
            logger.info(f"Current step num is {curr_step}")

    def save_test_result(self, output_dir):
        output_dir = os.path.abspath(output_dir)
        self.decision_maker.save(output_dir)
        target_file_name = "directed_acyclic_graph.gexf"

        wait_time = 5
        while wait_time > 0:
            file_list = [name for name in os.listdir(output_dir) if os.path.isfile(os.path.join(output_dir, name))]
            if target_file_name in file_list \
                    and len(file_list) == 5 \
                    and os.path.getsize(os.path.join(output_dir, target_file_name)) > 0:
                break
            time.sleep(8)
            wait_time -= 1

            if wait_time == 0:
                raise Exception(
                    "directed_acyclic_graph.gexf not generated, async save may exist during smart test procedure.")

    def load_test_result(self, load_dir):
        self.decision_maker.load(load_dir)

    def quit(self):
        for device in self.devices:
            self.device_manager_factory.get_device_manager(device).quit()


def devices_parser(string):
    return [Device(device_info["identifier"], device_info["platform"], device_info["platform_version"]) for device_info
            in json.loads(string)]


if __name__ == '__main__':
    logger_init()

    # parser = argparse.ArgumentParser()
    # parser.add_argument('--devices', '-s', type=devices_parser, required=True, help='')
    # parser.add_argument('--test_apps', '-a', type=json.loads, required=True, help='')
    # parser.add_argument('--screen_comprehension_kwargs', type=json.loads, required=False, help='')
    # parser.add_argument('--device_manager_kwargs', type=json.loads, required=False, help='')
    # parser.add_argument('--decision_maker_kwargs', type=json.loads, required=False, help='')
    # parser.add_argument('--string_pool_dir', type=str, required=True, help='')
    # parser.add_argument('--output_dir', type=str, required=True, help='')

    # args = parser.parse_args()

    # test_devices = args.devices
    # test_applications = {list(filter(lambda x: x.identifier == key, test_devices))[0]: [TestApplication(test_app_info["path"], test_app_info["package_name"]) for test_app_info in args.test_apps if test_app_info['device'] == key] for key in set(test_app['device'] for test_app in args.test_apps)}

    # smart_test = SmartTest(test_devices, test_applications, "DFS", "ScreenBert", args.screen_comprehension_kwargs, args.string_pool_dir)
    # smart_test.start()
    # smart_test.save_test_result(args.output_dir)
    # smart_test.quit()

    a = []
    for i in range(1, len(sys.argv)):
        a.append(sys.argv[i])

    del sys.argv[1:]

    app_path = a[0]
    device_info = a[1].replace("'", "\"")
    model_id = a[2].replace("'", "\"")
    max_step = int(a[3])
    string_pool_dir = a[4].replace("'", "\"")
    result_dir = a[5].replace("'", "\"")
    llm_param = a[6].replace("'", "\"")

    device_info_dict = json.loads(device_info)
    serial_number = device_info_dict['serialNum']
    os_version = device_info_dict['osVersion']

    model_id_dict = json.loads(model_id)
    path_to_screen_bert_model = model_id_dict['path_to_screen_bert_model']
    path_to_screen_topic_classifier_model = model_id_dict['path_to_screen_topic_classifier_model']

    device = Device(serial_number, "ANDROID", os_version)
    test_devices = [device]
    test_applications = {device: [TestApplication(app_path, get_package_name(app_path))]}
    screen_comprehension_kwargs = {"path_to_comprehension_model": path_to_screen_bert_model,
                                   "path_to_classifier_model": path_to_screen_topic_classifier_model}

    llm_param_dict = dict()
    if len(llm_param) > 0:
        llm_param_dict = json.loads(llm_param)

    metric_counter = DecisionMetricCounter(max_step)
    test_result = dict()
    smart_test = None

    try:
        smart_test = SmartTest(test_devices, test_applications, "DFS", "ScreenBert", screen_comprehension_kwargs,
                               string_pool_dir, result_dir, llm_param_dict, metric_counter)
        smart_test.start(max_step)
        smart_test.save_test_result(result_dir)
        smart_test.quit()
    except Exception as e:
        traceback.print_exc()
        test_result['success'] = False
        test_result['coverage'] = {}
        test_result['actionList'] = []
        test_result['appException'] = []
        test_result['metric'] = metric_counter.output()
        # The Hydra Lab agent will parse the test result from the stdout
        print("smartTestResult:" + json.dumps(test_result))
        if smart_test:
            smart_test.quit()
    else:
        test_result['success'] = True
        test_result['coverage'] = {}
        test_result['actionList'] = []
        test_result['appException'] = []
        test_result['metric'] = metric_counter.output()
        print("smartTestResult:" + json.dumps(test_result))
        logger.info('Test Finished')
