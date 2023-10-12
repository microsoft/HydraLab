import argparse
import copy
import pickle
import time
from collections import defaultdict, OrderedDict
from typing import Dict, List

import torch
import torch.nn.functional as F

from common.constants import app_state, llm_prompt, android_widget

from entities.action import SingleAction
from entities.device import Device
from entities.element import Element
from entities.frame_feature import FrameFeature
from route_map_visualization.route_map_visualization import RouteMapVisualization

from decision_maker.decision_maker import DecisionMaker
from utils.helper import *


class SingleDeviceDFSDecisionMaker(DecisionMaker):
    def __init__(self, strings, metric_counter):
        self.strings = strings

        self.element_visited = defaultdict(set)

        self.ranked_xpath_unvisited = defaultdict(list)

        self.page_visited = set()

        self.path = OrderedDict()

        self.page_group = {}

        self.replay = OrderedDict()

        self.route_map_visualization = RouteMapVisualization()

        self.route_map_visualization.add_node('-1', '-1', 'START')

        self.metric_counter = metric_counter

    def action_selector(self, output_dir, frame_feature_of_each_device: Dict[Device, FrameFeature], llm, curr_step) -> (
            List[SingleAction], bool, str):
        assert len(frame_feature_of_each_device) == 1
        assert len((list(frame_feature_of_each_device.items())[0][1]).page_info.running_status) == 1

        device, frame_feature = list(frame_feature_of_each_device.items())[0]
        device_page_info = frame_feature.page_info
        is_login_page = device_page_info.is_login_page
        current_topic = frame_feature.frame_topic
        # if current_topic == 'Login':
        #     logger.info(f'Current page topic: [{current_topic}]')
        #     # todo: general login logic

        running_foreground = list(device_page_info.running_status.values())[0] == app_state.RUNNING_IN_FOREGROUND
        page_group_id, page_id = self.page_identify(frame_feature, similarity_threshold=0.6)
        unvisited_elements = self.get_unvisited_elements(device_page_info.element_list, page_group_id)

        if running_foreground:
            save_screenshot_and_page_source(os.path.join(output_dir, str(page_group_id)), page_id,
                                            device_page_info.screenshot, device_page_info.page_source)

        if self.path:
            logger.info(
                " --> ".join([f"{group_id}" for group_id, (_, _, _) in self.path.items()]) + f' --> {page_group_id}')

        # Relaunch application at beginning
        if not self.path:
            if not running_foreground:
                self.path = OrderedDict()
                return [SingleAction(device, 'Relaunch', None)], False, "Relaunch application at beginning"
            elif len(unvisited_elements) <= 0:
                return [], True, "Start Page Has No Unvisited Element"
            else:
                self.route_map_visualization.add_node_and_edge('-1', '-1', str(page_group_id), str(page_id),
                                                               frame_feature.frame_topic, 'Launch')

        # Out of test application
        # Visited page
        # Leaf page
        if not running_foreground or page_group_id in self.page_visited or len(unvisited_elements) <= 0:
            if running_foreground:
                # TODO: record
                pass
            if len(unvisited_elements) <= 0:
                logger.info(f'[{page_group_id}] Visited')
                self.page_visited.add(page_group_id)
                if self.path:
                    parent_group_id, (parent_id, element, _) = list(self.path.items())[-1]
                    self.route_map_visualization.add_node_and_edge(str(parent_group_id), str(parent_id),
                                                                   str(page_group_id), str(page_id),
                                                                   frame_feature.frame_topic, str(element))
            self.update_visited()
            self.path = OrderedDict()
            return [SingleAction(device, 'Relaunch', None)], False, "Outside, Visited Page or Leaf Page"

        # 返回路径中的页面 -- 自身 / 祖先
        if page_group_id in self.path.keys():
            self.update_visited()
            self.path = OrderedDict()
            return [SingleAction(device, 'Relaunch', None)], False, "Loop"

        # 正常选择元素继续搜索
        if self.path:
            parent_group_id, (parent_id, element, _) = list(self.path.items())[-1]
            self.route_map_visualization.add_node_and_edge(str(parent_group_id), str(parent_id), str(page_group_id),
                                                           str(page_id), frame_feature.frame_topic, str(element))

        self.metric_counter.update_max_depth(page_group_id, curr_step)
        # Handle recognized login pages
        if is_login_page:
            element = None
            for edit_class in android_widget.EDIT_TEXT:
                element_list = list(filter(lambda x: x.element_class == edit_class, unvisited_elements))
                if element_list:
                    element = element_list[0]
                    break
            login_action = SingleAction(device, 'Login', None, **{"strings": self.strings})
            self.path.update({page_group_id: (page_id, element, True)})
            return [login_action], False, "Microsoft Login Logic"

        selected_element = None
        unvisited_element_sum = None
        # fallback to getting value from unvisited element list
        fallback = True
        if llm:
            fallback = False
            #  1. check if the current page is actually having the same element list or not. If not (rendering incompleteness or actually not the same page), BACKUP plan for it.
            #  2. store the ranked_element result for this page_group_id
            #  3. every time reaching the page_group_id, reuse the ranked_element result
            #  4. filter ranked_element list with unvisited_elements, to make sure the target ELEMENT object is in current page_source
            #  5. find the ELEMENT object with xpath value
            #  6. pop out from both the ranked_element and unvisited_element lists
            if self.ranked_xpath_unvisited[page_group_id]:
                # Filter out an element from ranked result ONLY when it is not in the unvisited_elements list.
                self.ranked_xpath_unvisited[page_group_id] = list(
                    filter(lambda xpath: xpath in [unvisited.xpath for unvisited in unvisited_elements],
                           self.ranked_xpath_unvisited[page_group_id]))
            else:
                unvisited_ele_list = list(unvisited_elements)
                element_prompt_input = "\n".join(
                    [element.prompt_format(idx) for idx, element in enumerate(unvisited_ele_list)])
                ranked_elements = llm.rank_element(element_input=element_prompt_input,
                                                   template=llm_prompt.ELEMENT_RANKING_TEMPLATE)
                if ranked_elements:
                    self.ranked_xpath_unvisited[page_group_id] = [unvisited_ele_list[int(item.element.id)].xpath for
                                                                  item in ranked_elements.element_list]

            # Check again for list length after filtering
            if not self.ranked_xpath_unvisited[page_group_id]:
                logger.error("Target element not exists. Possible reason: "
                             "1. token limit, LLM PROMPT is too long. "
                             "2. wrong classification results in different element list.")
                fallback = True
            else:
                chosen_element_xpath = self.ranked_xpath_unvisited[page_group_id][0]
                optional_element_list = list(
                    filter(lambda item: item.xpath == chosen_element_xpath, unvisited_elements))
                if optional_element_list:
                    selected_element = optional_element_list[0]
                    unvisited_element_sum = len(unvisited_elements) - 1
                else:
                    # 1. LLM returns different value from the INPUT, e.g. wrong XPATH that doesn't exist.
                    logger.error(
                        f"LLM returned XPATH is inconsistent with INPUT. Clean cached list and re-enable LLM for {page_group_id}.")
                    self.ranked_xpath_unvisited[page_group_id] = []
                    fallback = True
        if fallback:
            selected_element = unvisited_elements.pop()
            unvisited_element_sum = len(unvisited_elements)

        # todo: 需要处理当前EditText中已经填写了string中对应值的情况
        single_action = SingleAction(device, 'Interact_With_Element', selected_element, **{"strings": self.strings})
        self.path.update({page_group_id: (page_id, single_action.element, unvisited_element_sum == 0)})
        return [single_action], False, ""

    def before_action(self):
        pass

    def after_action(self):
        time.sleep(5)

    def save(self, output_dir):
        torch.save(self.page_group, os.path.join(output_dir, 'page_group.pt'))
        self.route_map_visualization.save_route_map(output_dir)
        with open(os.path.join(output_dir, 'element_visited.pickle'), "wb") as f:
            pickle.dump(self.element_visited, f, protocol=pickle.HIGHEST_PROTOCOL)

    def load(self, load_dir):
        self.page_group = torch.load(os.path.join(load_dir, 'page_group.pt'))
        self.route_map_visualization.load_route_map(load_dir)
        with open(os.path.join(load_dir, 'element_visited.pickle'), "rb") as f:
            self.element_visited = pickle.load(f)

    def page_identify(self, frame_feature: FrameFeature, similarity_threshold=0.6):
        if list(frame_feature.page_info.running_status.values())[0] != app_state.RUNNING_IN_FOREGROUND:
            return None, None

        #  check activity - only if activity exists in the map already, similarity on feature would be calculated.
        current_activity = frame_feature.page_info.page_activity

        page_group_to_compare = dict()
        for key, (previous_activity, _) in self.page_group.items():
            if current_activity == previous_activity:
                page_group_to_compare.update({key: self.page_group.get(key)})

        page_feature = frame_feature.page_feature
        page_group_id = -1
        max_similarity = -1
        for key, (_, previous_page_feature) in page_group_to_compare.items():
            if previous_page_feature.size(dim=0) >= 20:
                cosine_similarity = F.cosine_similarity(page_feature, previous_page_feature.mean(dim=0, keepdim=True),
                                                        dim=1).cpu().numpy()
            else:
                cosine_similarity = F.cosine_similarity(page_feature, previous_page_feature, dim=1).cpu().numpy()
            # logger.info(f'[{key}] MAX: {cosine_similarity.max()}, MIN: {cosine_similarity.min()}')
            if cosine_similarity.max() > max(similarity_threshold, max_similarity) and cosine_similarity.min() > 0.4:
                max_similarity = cosine_similarity.max()
                page_group_id = int(key)

        if page_group_id == -1:
            page_group_id = int(max(self.page_group, key=int)) + 1 if self.page_group else 0
            page_idx = 0
            self.page_group[str(page_group_id)] = (current_activity, copy.deepcopy(page_feature))
        else:
            page_idx = self.page_group[str(page_group_id)][1].size(dim=0)
            self.page_group[str(page_group_id)] = (current_activity, torch.cat((self.page_group[str(page_group_id)][1], page_feature), dim=0))

        return page_group_id, f'{page_group_id}-{page_idx}'

    def get_unvisited_elements(self, elements: List[Element], page_group_id):
        interactive_elements = filter(lambda e: e.interactive(), elements)
        return set(interactive_elements).difference(self.element_visited[page_group_id])

    def update_visited(self):
        while self.path:
            group_id, (_, element, last_element) = self.path.popitem()
            self.element_visited[group_id].add(element)
            if last_element:
                logger.info(f'[{group_id}] Visited')
                self.page_visited.add(group_id)
            else:
                break

    def exceptional_restart(self, device):
        logger.warning("Exception occurred during action selection, restart app with cross-action flags reset.")
        self.update_visited()
        self.path = OrderedDict()
        return [SingleAction(device, 'Relaunch', None)], False, "Loop"