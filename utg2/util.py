import os
import re


def folder_dir(path):
    return os.path.dirname(path)


def get_all_java_files(path):
    ret = []
    for root, dirs, files in os.walk(path):
        for f in files:
            if f.endswith(".java"):
                ret.append(os.path.join(root, f))
    return ret


def create_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "x") as f:
        f.write(content)


def filter_functions(class_name, functions):
    ret = functions
    ret = [s for s in ret if re.match(r"public\s+\w+\s+\w+\((\w+,?\s*)*\)", s)]
    ret = [s for s in ret if (class_name + "(") not in s]
    return ret


def parse_java_path(java_path):
    if "/src/" not in java_path:
        return (None, None)
    if java_path.endswith(".java") == False:
        return (None, None)
    dst_path = java_path.replace("/main/", "/test/").replace(".java", "Test.java")
    utg_path = java_path.replace("/main/", "/utg/").replace(".java", "")
    return (dst_path, utg_path)
