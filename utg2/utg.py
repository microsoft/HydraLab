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

from util import *
from workflow.java.client import client as java_client

proj_path = "C:/Gh/HydraLab2"
src_path = f"{proj_path}/common/src/main/java/com/microsoft/hydralab/common/util/LogUtils.java"
ut_path = f"{proj_path}/common/src/test/java/com/microsoft/hydralab/common/util/LogUtilsTest.java"

build_cmd = [r'C:\x\gradle-6.9.4-bin\gradle-6.9.4\bin\gradle.bat', 'build', '-p', r'C:\Gh\HydraLab2\common']

if __name__ == "__main__":
    client = java_client()
    client.SourceToUt(src_path, ut_path)
    fixed = client.FixUt(src_path, ut_path, build_cmd)
