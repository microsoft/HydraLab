from typing import List, Dict

from entities.element import Element
from entities.test_app import TestApplication


class PageInfo:
    def __init__(self, screenshot, page_source, element_list: List[Element], running_status: Dict[TestApplication, int],
                 current_activity, is_login):
        self.screenshot = screenshot
        self.page_source = page_source
        self.element_list = element_list
        self.running_status = running_status
        self.page_activity = current_activity
        self.is_login_page = is_login
