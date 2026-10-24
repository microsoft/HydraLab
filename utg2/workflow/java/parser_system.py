import os

import regex

from workflow.java.parser import *

_debug_breakpoint_class = [
    # "NotificationChannelManager"
]

class java_system:
    def __init__(self, repo_paths):
        file_paths = []
        java_files = []
        java_class_map = {}
        for p in repo_paths:
            for root, dirs, files in os.walk(p):
                file_paths.extend([os.path.join(root, f) for f in files])
        file_paths = [f for f in file_paths if f.endswith('.java')]
        for fp in file_paths:
            try:
                print(f"parsing file {fp}")
                for bp in _debug_breakpoint_class:
                    if bp in fp:
                        breakpoint()
                with open(fp, "r") as f:
                    file_content = f.read()
                jf = java_file(file_content)
                jc_a = jf.get_classes()
                for jc in jc_a:
                    for bp in _debug_breakpoint_class:
                        if bp in jc.class_name:
                            breakpoint()
                    java_class_map[jc.class_name] = jc
                java_files.append(jf)
            except Exception as e:
                print(f"!!! Error parsing {f}: {e}")
        self.java_files = java_files
        self.java_class_map = java_class_map

    def get_code_by_class(self, class_name):
        if self.java_class_map.get(class_name) is None:
            return None
        else:
            return self.java_class_map[class_name].java_file.get_codes_str()
