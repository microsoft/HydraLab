import os
import subprocess

from langchain.chains import LLMChain, SequentialChain
from langchain.chat_models import ChatOpenAI
from langchain.llms import AzureOpenAI, OpenAI
from langchain.prompts import PromptTemplate

api_type = "azure"
api_base = "https://max-es-gpt.openai.azure.com"
api_engine = "gpt35turbo16k"
api_version = "2023-03-15-preview"
api_key = "xxxxxxxxxxxxxxxxxxxxx"

# pmpt_1 = "prompts/java_source_to_ut_1/_1_source_to_functions.txt"
# pmpt_2 = "prompts/java_source_to_ut_1/_2_source_function_to_ut.txt"
# pmpt_3 = "prompts/java_source_to_ut_1/_3_java_code_prettify.txt"
# class client:
#     def __init__(self):
#         self.chat = ChatOpenAI(
#             api_type=api_type,
#             api_base=api_base,
#             api_key=api_key,
#             api_version=api_version,
#             engine=api_engine,
#             openai_api_base=api_base,
#             openai_api_key=api_key,
#             max_tokens=2048,
#             temperature=0.01,
#         )

#     def SourceToFunctions(self, src):
#         return self.__React(pmpt_1, source=src)

#     def SourceFunctionToUt(self, src, func):
#         return self.__React(pmpt_2, source=src, function=func)

#     def JavaCodeRevise(self, src):
#         return self.__React(pmpt_3, source=src)

#     def __React(self, prompt, **pt_vars):
#         with open(f"{os.path.dirname(os.path.realpath(__file__))}/{prompt}", "r") as f:
#             template_string = f.read()
#         prompt_template = ChatPromptTemplate.from_template(template_string)
#         prompt_message = prompt_template.format_messages(**pt_vars)
#         response = self.chat(prompt_message)
#         return response.content

prompt_s2u = "prompts/source_to_ut_v1.txt"
prompt_gradle_err = "prompts/gradle_err_v1.txt"
prompt_fix_build_err_by_source = "prompts/fix_build_err_by_source_v1.txt"
prompt_get_error_ctx_class = "prompts/get_error_ctx_class_v1.txt"
prompt_fix_build_err_by_delete = "prompts/fix_build_err_by_delete_v1.txt"

class client:
    def __init__(self):
        self.llm = ChatOpenAI(
            api_type=api_type,
            api_base=api_base,
            api_key=api_key,
            api_version=api_version,
            engine=api_engine,
            openai_api_base=api_base,
            openai_api_key=api_key,
            max_tokens=1024*4,
            temperature=0.01,
        )
        self._SetupS2uChain()
        self._SetupFixUtBySourceChain()
        self._SetupGetUtErrorCtxClassChain()
        self._SetupFixUtByCtxClassChain()
        self._SetupFixUtByDeleteChain()

    def SourceToUt(self, src_path, dst_path):
        with open(src_path, "r") as f:
            src = f.read()
        ut = self.s2u({ "source": src })["ut_original"]
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        with open(dst_path, "w") as f:
            f.write(ut)

    def FixUt(self, src_path, ut_path, build_cmd):
        for i in range(3):
            for j in range(2):
                if self._FixUtBySource(src_path, ut_path, build_cmd):
                    return True
                # if self._FixUtByCtxClass(src_path, ut_path, build_cmd):
                    # return True
            if self._FixUtByDeleteFunction(src_path, ut_path, build_cmd):
                return True
        return False

    def _FixUtBySource(self, src_path, ut_path, build_cmd):
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
            chain_vars = self.fixUtBySource({
                "build_output": build_output,
                "source": src,
                "ut_original": ut
            })
            ut = chain_vars["ut_revised"]
            print(ut)
            with open(ut_path, "w") as f:
                f.write(ut)
            fix_times += 1

    def _FixUtByCtxClass(self, src_path, ut_path, build_cmd):
        with open(src_path, "r") as f:
            src = f.read()
        with open(ut_path, "r") as f:
            ut = f.read()
        fix_times = 0
        while True:
            build_rst, build_output = self._Build(build_cmd)
            if build_rst:
                return True,
            if fix_times > 3:
                return False
            chain_vars = self.getUtErrorCtxClass({
                "build_output": build_output,
                "source": src,
                "ut_original": ut
            })
            ctx_class = chain_vars["error_ctx_class"]
            print(ctx_class)
            # with open(ut_path, "w") as f:
            #     f.write(ut)
            fix_times += 1

    def _FixUtByDeleteFunction(self, src_path, ut_path, build_cmd):
        with open(src_path, "r") as f:
            src = f.read()
        with open(ut_path, "r") as f:
            ut = f.read()
        fix_times = 0
        while True:
            build_rst, build_output = self._Build(build_cmd)
            if build_rst:
                return True,
            if fix_times > 3:
                return False
            chain_vars = self.fixUtByDelete({
                "build_output": build_output,
                "ut_original": ut
            })
            ut = chain_vars["ut_revised"]
            print(ut)
            with open(ut_path, "w") as f:
                f.write(ut)
            fix_times += 1

    def _Build(self, build_cmd):
        p = subprocess.run(build_cmd, check=False, capture_output=True)
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
        self.s2u = SequentialChain(
            chains=chains,
            input_variables=["source"],
            output_variables=["ut_original"],
            verbose=True)

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
                output="ut_revised"))
        self.fixUtBySource = SequentialChain(
            chains=chains,
            input_variables=["build_output", "source", "ut_original"],
            output_variables=["ut_revised"],
            verbose=True)

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
                output="error_ctx_class"))
        self.getUtErrorCtxClass = SequentialChain(
            chains=chains,
            input_variables=["build_output", "source", "ut_original"],
            output_variables=["error_ctx_class"],
            verbose=True)

    def _SetupFixUtByCtxClassChain(self):
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
        self.fixUtByCtxClass = SequentialChain(
            chains=chains,
            input_variables=["build_output", "ut_original"],
            output_variables=["ut_revised"],
            verbose=True)

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
            verbose=True)

    def _LoadChain(self, prompt_path, inputs, output):
        with open(f"{os.path.dirname(os.path.realpath(__file__))}/{prompt_path}", "r") as f:
            template_string = f.read()
        prompt_template = PromptTemplate(input_variables=inputs, template=template_string)
        return LLMChain(llm=self.llm, prompt=prompt_template, output_key=output)

if __name__ == "__main__":
    pass
