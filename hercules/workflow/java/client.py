import os
import subprocess
from datetime import datetime

from langchain.chains import LLMChain, SequentialChain
from langchain.chat_models import ChatOpenAI
from langchain.llms import AzureOpenAI, OpenAI
from langchain.prompts import PromptTemplate
from workflow.java.parser import *
from workflow.java.parser_system import *

langchain_verbose = False

user_config = {}
with open("../../config.json", "r") as f:
    user_config = json.load(f)

api_type = user_config["llm"]["api_type"]
api_base = user_config["llm"]["api_base"]
api_engine = user_config["llm"]["api_engine"]
api_version = user_config["llm"]["api_version"]
api_key = user_config["llm"]["api_key"]
max_tokens = user_config["llm"]["max_tokens"]


prompt_s2u = "prompts/source_to_ut_v1.txt"
prompt_simple_refactor = "prompts/ut_simple_refactor_v1.txt"
prompt_gradle_err = "prompts/gradle_err_v1.txt"
prompt_fix_build_err_by_source = "prompts/fix_build_err_by_source_v1.txt"
prompt_fix_build_err_by_alternative = "prompts/fix_build_err_by_alternative_v1.txt"
prompt_get_error_ctx_class = "prompts/get_error_ctx_class_v1.txt"
prompt_fix_build_err_by_ctx_class = "prompts/fix_build_err_by_ctx_class_v1.txt"
prompt_fix_build_err_by_delete = "prompts/fix_build_err_by_delete_v1.txt"
prompt_fix_build_err_by_delete_function = "prompts/fix_build_err_by_delete_function_v1.txt"


def entry_logs(logs):
    now = datetime.now()
    date_str = now.strftime("%Y-%m-%d  %H:%M:%S")
    max_len = len(date_str)
    [max_len := len(l) if len(l) > max_len else max_len for l in logs]
    print(f" {'_' * (max_len + 6)} ")
    print(f"|== {date_str} {'=' * (max_len - len(date_str) + 2)}|")
    for l in logs:
        print(f"|== {l} {'=' * (max_len - len(l) + 2)}|")
    print("|==" + ('=' * (max_len + 2)) + "==|")

class client:
    def __init__(self, repo_paths):
        self.llm = ChatOpenAI(
            api_type=api_type,
            api_base=api_base,
            api_key=api_key,
            api_version=api_version,
            engine=api_engine,
            openai_api_base=api_base,
            openai_api_key=api_key,
            max_tokens=max_tokens,
            temperature=0.01,
        )
        self.fs = java_system(repo_paths)
        self._SetupS2uChain()
        self._SetupFixUtBySourceChain()
        self._SetupGetUtErrorCtxClassChain()
        self._SetupFixUtByCtxClassChain()
        self._SetupFixUtByDeleteChain()
        self._SetupFixUtByDeleteFunctionChain()

    def SourceToUt(self, src_path, dst_path):
        entry_logs([
            "Source To Ut:",
            src_path,
            dst_path
        ])
        with open(src_path, "r") as f:
            src = f.read()
            jf = java_file(src)
        ut = self.s2u({ "source": src, "packages": "\n".join(jf.imports) })["ut_content"]
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        with open(dst_path, "w") as f:
            f.write(ut)

    def FixUt(self, src_path, ut_path, build_cmd):
        for i in range(4):
            for j in range(2):
                if self._FixUtBySource(src_path, ut_path, build_cmd):
                    return True
                if self._FixUtByCtxClass(src_path, ut_path, build_cmd):
                    return True
            for j in range(1 + i):
                if self._FixUtByDelete(src_path, ut_path, build_cmd):
                    return True
            if self._FixUtByDeleteFunction(src_path, ut_path, build_cmd):
                return True
        return False

    def _FixUtBySource(self, src_path, ut_path, build_cmd):
        entry_logs([
            "Fix UT by Source:",
            ut_path
        ])
        with open(src_path, "r") as f:
            src = f.read()
        with open(ut_path, "r") as f:
            ut = f.read()
        fix_times = 0
        while True:
            build_rst, build_output = self._Build(build_cmd)
            if build_rst:
                return True
            if fix_times > 2:
                return False
            chain_vars = self.fixUtBySource({
                "build_output": build_output,
                "source": src,
                "ut_original": ut
            })
            new_ut = chain_vars["ut_revised"]
            if new_ut == ut:
                return False
            ut = new_ut
            with open(ut_path, "w") as f:
                f.write(ut)
            fix_times += 1

    def _FixUtByCtxClass(self, src_path, ut_path, build_cmd):
        entry_logs([
            "Fix UT by Context Class:",
            ut_path
        ])
        with open(src_path, "r") as f:
            src = f.read()
        with open(ut_path, "r") as f:
            ut = f.read()
        fix_times = 0
        while True:
            build_rst, build_output = self._Build(build_cmd)
            if build_rst:
                return True,
            if fix_times > 0:
                return False
            chain_vars = self.getUtErrorCtxClass({
                "build_output": build_output,
                "source": src,
                "ut_original": ut
            })
            class_name_list = chain_vars["class_name"]
            class_name_list = class_name_list.split(',')
            ctx_code = ""
            for cn in class_name_list:
                cn = cn.split('.')[-1] if '.' in cn else cn
                cn_ctx = self.fs.get_code_by_class(cn)
                if cn_ctx is not None:
                    ctx_code += cn_ctx + "\n\n"
                    entry_logs([
                        "Fix UT by Context Class:",
                        ut_path,
                        f"Class Name: {cn} found ? {ctx_code is not None}"
                    ])

            if ctx_code == "":
                return False

            chain_vars = self.fixUtByCtxClass({
                "context": ctx_code,
                "ut_original": ut,
                "build_output": build_output
            })
            new_ut = chain_vars["ut_revised"]
            if new_ut == ut:
                return False
            ut = new_ut
            with open(ut_path, "w") as f:
                f.write(ut)
            fix_times += 1

    def _FixUtByDelete(self, src_path, ut_path, build_cmd):
        entry_logs([
            "Fix UT by Delete:",
            ut_path
        ])
        with open(src_path, "r") as f:
            src = f.read()
        with open(ut_path, "r") as f:
            ut = f.read()
        fix_times = 0
        while True:
            build_rst, build_output = self._Build(build_cmd)
            if build_rst:
                return True,
            if fix_times > 2:
                return False
            chain_vars = self.fixUtByDelete({
                "build_output": build_output,
                "ut_original": ut
            })
            new_ut = chain_vars["ut_revised"]
            if new_ut == ut:
                return False
            ut = new_ut
            with open(ut_path, "w") as f:
                f.write(ut)
            fix_times += 1

    def _FixUtByDeleteFunction(self, src_path, ut_path, build_cmd):
        entry_logs([
            "Fix UT by Delete Function:",
            ut_path
        ])
        with open(src_path, "r") as f:
            src = f.read()
        with open(ut_path, "r") as f:
            ut = f.read()
        fix_times = 0
        while True:
            build_rst, build_output = self._Build(build_cmd)
            if build_rst:
                return True,
            if fix_times > 0:
                return False
            chain_vars = self.fixUtByDeleteFunction({
                "build_output": build_output,
                "ut_original": ut
            })
            new_ut = chain_vars["ut_revised"]
            if new_ut == ut:
                return False
            ut = new_ut
            with open(ut_path, "w") as f:
                f.write(ut)
            fix_times += 1

    def _Build(self, cmd):
        p = subprocess.run(cmd["cmd"], cwd=cmd["cwd"], check=False, capture_output=True)
        build_output = p.stdout.decode('utf-8')
        build_error = p.stderr.decode('utf-8')
        if "BUILD SUCCESSFUL" in build_output:
            return True, build_output
        else:
            return False, build_error

    def _SetupS2uChain(self):
        chains = []
        chains.append(
            self._LoadChain(
                prompt_s2u,
                inputs=["source"],
                output="ut_original"))
        chains.append(
            self._LoadChain(
                prompt_simple_refactor,
                inputs=["ut_original", "packages"],
                output="ut_content"))
        self.s2u = SequentialChain(
            chains=chains,
            input_variables=["source", "packages"],
            output_variables=["ut_content"],
            verbose=langchain_verbose)

    def _SetupFixUtBySourceChain(self):
        chains = []
        chains.append(
            self._LoadChain(
                prompt_gradle_err,
                inputs=["build_output"],
                output="build_error"))
        chains.append(
            self._LoadChain(
                prompt_fix_build_err_by_source,
                inputs=["source", "ut_original", "build_error"],
                output="ut_try_fixed"))
        chains.append(
            self._LoadChain(
                prompt_fix_build_err_by_alternative,
                inputs=["source", "ut_try_fixed", "build_error"],
                output="ut_revised"))
        self.fixUtBySource = SequentialChain(
            chains=chains,
            input_variables=["build_output", "source", "ut_original"],
            output_variables=["ut_revised"],
            verbose=langchain_verbose)

    def _SetupGetUtErrorCtxClassChain(self):
        chains = []
        chains.append(
            self._LoadChain(
                prompt_gradle_err,
                inputs=["build_output"],
                output="build_error"))
        chains.append(
            self._LoadChain(
                prompt_get_error_ctx_class,
                inputs=["source", "ut_original", "build_error"],
                output="class_name"))
        self.getUtErrorCtxClass = SequentialChain(
            chains=chains,
            input_variables=["build_output", "source", "ut_original"],
            output_variables=["class_name", "build_error"],
            verbose=langchain_verbose)

    def _SetupFixUtByCtxClassChain(self):
        chains = []
        chains.append(
            self._LoadChain(
                prompt_gradle_err,
                inputs=["build_output"],
                output="build_error"))
        chains.append(
            self._LoadChain(
                prompt_get_error_ctx_class,
                inputs=["source", "ut_original", "build_error"],
                output="class_name"))
        self.getUtErrorCtxClass = SequentialChain(
            chains=chains,
            input_variables=["build_output", "source", "ut_original"],
            output_variables=["class_name"],
            verbose=langchain_verbose)

        chains = []
        chains.append(
            self._LoadChain(
                prompt_gradle_err,
                inputs=["build_output"],
                output="build_error"))
        chains.append(
            self._LoadChain(
                prompt_fix_build_err_by_ctx_class,
                inputs=["context", "ut_original", "build_error"],
                output="ut_revised"))
        self.fixUtByCtxClass = SequentialChain(
            chains=chains,
            input_variables=["context", "ut_original", "build_output"],
            output_variables=["ut_revised"],
            verbose=langchain_verbose)

    def _SetupFixUtByDeleteChain(self):
        chains = []
        chains.append(
            self._LoadChain(
                prompt_gradle_err,
                inputs=["build_output"],
                output="build_error"))
        chains.append(
            self._LoadChain(
                prompt_fix_build_err_by_delete,
                inputs=["ut_original", "build_error"],
                output="ut_revised"))
        self.fixUtByDelete = SequentialChain(
            chains=chains,
            input_variables=["build_output", "ut_original"],
            output_variables=["ut_revised"],
            verbose=langchain_verbose)

    def _SetupFixUtByDeleteFunctionChain(self):
        chains = []
        chains.append(
            self._LoadChain(
                prompt_gradle_err,
                inputs=["build_output"],
                output="build_error"))
        chains.append(
            self._LoadChain(
                prompt_fix_build_err_by_delete_function,
                inputs=["ut_original", "build_error"],
                output="ut_revised"))
        self.fixUtByDeleteFunction = SequentialChain(
            chains=chains,
            input_variables=["build_output", "ut_original"],
            output_variables=["ut_revised"],
            verbose=langchain_verbose)

    def _LoadChain(self, prompt_path, inputs, output):
        with open(f"{os.path.dirname(os.path.realpath(__file__))}/{prompt_path}", "r") as f:
            template_string = f.read()
        prompt_template = PromptTemplate(input_variables=inputs, template=template_string)
        return LLMChain(llm=self.llm, prompt=prompt_template, output_key=output)

if __name__ == "__main__":
    pass
