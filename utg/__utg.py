import importlib
import json
import os

import pip

required_packages = ["langchain", "regex", "openai", "chromadb", "tiktoken"]

for p in required_packages:
    try:
        importlib.import_module(p)
    except ImportError:
        pip.main(["install", p])


from code_parser import java
from llm import openai
from util import *

base_path = "C:/Gh/HydraLab2/center"

search_path = f"{base_path}/src/main/java/com/microsoft/hydralab/center/service"
search_path = f"{base_path}/src/main/java/com/microsoft/hydralab/center/service/SysPermissionService.java"


class utg:
    def __init__(self):
        self.client = openai.client()

    def print_title(self, title):
        src_len = len(src_path)
        side = f"|--{'-' * src_len}--|"
        title = f"|- {src_path} -|"
        print(side)
        print(title)
        print(side)
        print(side)

    def generate_java_ut(self, src_path, ut_path, output_folder=None):
        self.print_title(src_path)
        if os.path.isfile(src_path) == False:
            print(f"!!!!!!!!! File not found: {src_path}")
            return
        if os.path.exists(ut_path):
            print(f"!!!!!!!!! Test File already existed: {ut_path}")
            return

        p = java.parser(src_path)

        # get all functions and imports
        print(">>>>>>>>> Parsing functions in Java file")
        origin_imports = java.java_file(p.get_codes_str()).imports
        ans = self.client.SourceToFunctions(p.get_codes_str())
        ans = json.loads(ans)
        package = ans["Package"]
        class_name = ans["Class"]
        functions = ans["Functions"]
        functions = filter_functions(class_name, functions)

        # program ut for one function
        print(">>>>>>>>> Programming UT for functions")
        files = []
        for i, f in enumerate(functions):
            print(f"... Generating UT for function: {f}")
            file_content = self.client.SourceFunctionToUt(p.get_codes_str(), f)
            jf = java.java_file(file_content)
            files.append(jf)
            if output_folder != None:
                create_file(f"{output_folder}/{i}.java", jf.get_codes_str())
                create_file(f"{output_folder}/{i}_origin.java", file_content)
        if len(files) == 0:
            print("!!!!!!!!! No function found in Java file")
            return

        # merge ut file
        print(">>>>>>>>> Merging function-UTs into UT file")
        f_merged = None
        for i, f in enumerate(files):
            if i == 0:
                f_merged = f
            else:
                f_merged.bind(f)

        if f_merged == None:
            print("!!!!!!!!! Merge file is None")
            return

        f_merged.package = package
        [f_merged.add_import(i) for i in origin_imports]

        # prettify ut file
        print(">>>>>>>>> Revising UT file")
        src = f_merged.get_codes_str()
        ans = self.client.JavaCodeRevise(src)
        ans = json.loads(ans)
        print(">>>>>>>>> Revised UT file")
        create_file(ut_path, ans["Revised-File"])
        print(f"*** UT file: {ut_path}")


if __name__ == "__main__":
    utg = utg()
    if os.path.isfile(search_path):
        files = [search_path]
    elif os.path.isdir(search_path):
        files = get_all_java_files(search_path)
    else:
        files = []

    for src_path in files:
        (dst_path, utg_path) = parse_java_path(src_path)
        if dst_path == None or utg_path == None:
            continue
        if os.path.exists(dst_path):
            continue
        try:
            utg.generate_java_ut(src_path, dst_path, output_folder=utg_path)
        except Exception as ex:
            print(f"!!!!!!!!! Excpetion: {ex}")
