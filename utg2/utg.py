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

repo_paths_s = [
    "C:/r/mmx/android/apps/YPC/app/src/main/java/com/microsoft/appmanager",
]
src_path = "123C:/r/mmx/android/apps/YPC/app/src/main/java/com/microsoft/appmanager/ViewHelper.java"
ut_build_s = {
    "cmd": [r"C:\r\mmx\android\apps\YPC\gradlew.bat", ":app:compileTeamDebugUnitTestSources"],
    "cwd": r"C:\r\mmx\android\apps\YPC",
}

repo_paths = [
    "C:/r/mmx/android/apps/YPC/app/src/main/java/com/microsoft/appmanager",
    "C:/r/mmx/android/apps/YPC/agents/src/main",
    "C:/r/mmx/android/apps/YPC/authbroker/src/main",
    "C:/r/mmx/android/apps/YPC/common/src/main",
    "C:/r/mmx/android/apps/YPC/core/src/main",
    "C:/r/mmx/android/apps/YPC/stub_core/src/main",
    "C:/r/mmx/android/apps/YPC/stub_breadth/src/main",
]
search_path = "C:/r/mmx/android/apps/YPC/app/src/main/java/com/microsoft/appmanager"
# search_path = "C:/r/mmx/android/apps/YPC/app/src/main/java/com/microsoft/appmanager/ViewHelper.java"

ut_build = {
    "cmd": [r"C:\r\mmx\android\apps\YPC\gradlew.bat", ":app:compileTeamDebugUnitTestSources"],
    "cwd": r"C:\r\mmx\android\apps\YPC",
}
# ut_build = {
#     "cmd": [r'C:\x\gradle-6.9.4-bin\gradle-6.9.4\bin\gradle.bat', 'build', '-p', r'C:\Gh\HydraLab2\common'],
#     "cwd": "",
# }

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
    if os.path.exists(src_path):
        client = java_client(repo_paths_s)
        entry(src_path, ut_build_s)
    else:
        client = java_client(repo_paths)
        for root, dirs, files in os.walk(search_path):
            for f in files:
                entry(os.path.join(root, f), ut_build)
