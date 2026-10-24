import importlib
import json
import os

import pip

required_packages = ["regex"]

for p in required_packages:
    try:
        importlib.import_module(p)
    except ImportError:
        pip.main(["install", p])


from code_parser import java
from llm import openai
from util import *


class utg:
    def __init__(self):
        self.client = openai.client()

    def generate_java_ut(self, src_path, ut_path, output_folder=None):
        if os.path.isfile(src_path) == False:
            print(f"!!!!!!!!! File not found: {src_path}")
            return
        if os.path.exists(ut_path):
            print(f"!!!!!!!!! Test File already existed: {ut_path}")
            return

        p = java.parser(src_path)

        # get all functions
        print(">>>>>>>>> Parsing functions in Java file")
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

        f_merged = None
        for i, f in enumerate(files):
            if i == 0:
                f_merged = f
            else:
                f_merged.bind(f)

        # prettify ut file
        print(">>>>>>>>> Revising UT file")
        src = f.get_codes_str()
        ans = self.client.JavaCodeRevise(src)
        ans = json.loads(ans)
        print(">>>>>>>>> Revised UT file")
        create_file(ut_path, ans["Revised-File"])
        print(f"*** UT file: {ut_path}")


if __name__ == "__main__":
    prj_path = "C:/Gh/HydraLab"
    sub_path = "/java/com/microsoft/hydralab/center/util"
    src_path = f"{prj_path}/center/src/main{sub_path}/AuthUtil.java"
    dst_path = f"{prj_path}/center/src/test{sub_path}/AuthUtilTest.java"
    rst_folder = f"{prj_path}/center/src/utg{sub_path}/AuthUtil"
    utg = utg()
    utg.generate_java_ut(src_path, dst_path, output_folder=rst_folder)
