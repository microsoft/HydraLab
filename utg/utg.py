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

user_config = {}
with open("config.json", "r") as f:
    user_config = json.load(f)

src_paths = user_config["src_paths"]
repo_paths = user_config["repo_paths"]
ut_build = user_config["ut_build"]
client = None

from util import *
from workflow.java.client import client as java_client


def entry(src_p, cmd):
    if not src_p.endswith(".java"):
        return
    ut_p = src_p.replace("main", "test").replace(".java", "Test.java")
    if os.path.exists(ut_p):
        return
    try:
        client.SourceToUt(src_p, ut_p)
        fixed = client.FixUt(src_p, ut_p, cmd)
        if fixed == False and os.path.exists(ut_p):
            os.remove(ut_p)
    except Exception as e:
        print(f"Error: {e}")
        if os.path.exists(ut_p):
            os.remove(ut_p)

if __name__ == "__main__":
    global client
    client = java_client(repo_paths)
    for src_path in src_paths:
        if os.path.exists(src_path):
            entry(src_path, ut_build_s)
        else:
            for root, dirs, files in os.walk(src_path):
                for f in files:
                    entry(os.path.join(root, f), ut_build)
