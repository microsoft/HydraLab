from bs4 import BeautifulSoup
from uiautomator2 import Device
import re
import os


class UIPage:
    def __init__(self, xml, contexts, screenshot_file=None):
        self.xml = xml
        self.root = BeautifulSoup(xml, features="xml")
        self.contexts = contexts
        self.screenshot_file = screenshot_file


class UIContextExtractor:
    def __init__(self, os, lang):
        self.os = os
        self.lang = lang
        self.counter = 0

    def extract(self, device: Device) -> UIPage:
        ui_xml = device.dump_hierarchy()
        activity_dump = device.shell("dumpsys activity activities").output
        s_file = f"screenshots/screenshot_extracted_{self.counter}.png"
        s_file = os.path.abspath(s_file)
        # create the folder of s_file if not exists
        if not os.path.exists(os.path.dirname(s_file)):
            os.makedirs(os.path.dirname(s_file))

        device.screenshot(s_file)
        contexts = parse_activities_data(activity_dump)
        self.counter += 1
        return UIPage(ui_xml, contexts, s_file)


def parse_activities_data(data: str):
    task_pattern = re.compile(r"\* Task\{(\S+) #(\d+) type=(\S+)")
    package_pattern = re.compile(r"[A-Za-z.0-9]+")
    activity_pattern = re.compile(r"[A-Za-z0-9/.]+")
    locale_pattern = re.compile(r"[A-Za-z]{2}_[A-Za-z]{2}")
    dir_pattern = re.compile(r"(baseDir|dataDir)=(\S+)")

    tasks = {}
    current_task = None
    top_activity = None
    activity_record_point_to_top_resumed = False
    top_package = None

    for line in data.split("\n"):
        if 'ResumedActivity:' in line or 'Resumed activities' in line:
            activity_record_point_to_top_resumed = True
            continue
        if 'ActivityTaskSupervisor' in line:
            break
        if '* Task' in line:
            task_pattern_match = task_pattern.search(line)
            if not task_pattern_match:
                continue
            task_id = task_pattern_match.groups()[1]
            if task_id in tasks:
                current_task = tasks[task_id]
            else:
                package_name = package_pattern.findall(line)
                find_package = False
                for pn in package_name:
                    if '.' in pn:
                        package_name = pn
                        find_package = True
                        break
                if not find_package:
                    continue
                current_task = {
                    "task_id": task_id,
                    "type": task_pattern_match.groups()[2],
                    "package": package_name,
                }
                if not top_package:
                    top_package = package_name
                tasks[task_id] = current_task
        if 'ActivityRecord' in line:
            activity_pattern_match = activity_pattern.findall(line)
            for act in activity_pattern_match:
                if '.' in act and '/' in act:
                    activity_pattern_match = act
                    break
            if not top_activity and activity_record_point_to_top_resumed:
                top_activity = activity_pattern_match
                continue
            if current_task:
                if 'activities' not in current_task:
                    current_task['activities'] = []
                if activity_pattern_match not in current_task['activities']:
                    current_task['activities'].append(activity_pattern_match)
        if 'baseDir' in line or 'dataDir' in line:
            dir_pattern_match = dir_pattern.search(line)
            if dir_pattern_match:
                dir_info = dir_pattern_match.groups()
                if current_task:
                    current_task[dir_info[0]] = dir_info[1]
        if 'windows' in line:
            activity_pattern_match = activity_pattern.findall(line)
            for act in activity_pattern_match:
                if '.' in act and '/' in act:
                    activity_pattern_match = act
                    break
            if current_task and not current_task.get('windows'):
                current_task['windows'] = activity_pattern_match
        if 'GlobalConfig' in line:
            locale_pattern_match = locale_pattern.findall(line)
            if locale_pattern_match and current_task:
                current_task['locale'] = locale_pattern_match[0]
    return {"top_activity": top_activity, "top_package": top_package, "task": tasks}
