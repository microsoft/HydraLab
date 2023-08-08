import os
import re

def folder_dir(path):
    return os.path.dirname(path)

def filter_functions(class_name, functions):
    ret = functions
    ret = [s for s in ret if re.match(r'public\s+\w+\s+\w+\((\w+,?\s*)*\)', s)]
    ret = [s for s in ret if (class_name + '(') not in s]
    return ret
