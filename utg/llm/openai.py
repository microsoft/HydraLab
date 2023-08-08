import os

import openai
from langchain.chat_models import ChatOpenAI
from langchain.prompts import ChatPromptTemplate

api_type = "azure"
api_base = "https://max-es-gpt.openai.azure.com"
api_engine = "gpt-35-turbo"
api_version = "2023-03-15-preview"
api_key = "xxxxxxx"

openai.api_type = api_type
openai.api_base = api_base
openai.engine = api_engine
openai.api_version = api_version
openai.api_key = api_key

os.environ["OPENAI_API_TYPE"] = api_type
os.environ["OPENAI_API_BASE"] = api_base
os.environ["OPENAI_API_VERSION"] = api_version
os.environ["OPENAI_API_KEY"] = api_key

pmpt_1 = "prompts/java_source_to_ut_1/_1_source_to_functions.txt"
pmpt_2 = "prompts/java_source_to_ut_1/_2_source_function_to_ut.txt"


class client:
    def __init__(self):
        self.chat = ChatOpenAI(
            api_type=api_type,
            api_base=api_base,
            api_key=api_key,
            api_version=api_version,
            engine=api_engine,
            max_tokens=1024,
            temperature=0.01,
        )

    def SourceToFunctions(self, src):
        return self.__React(pmpt_1, source=src)

    def SourceFunctionToUt(self, src, func):
        return self.__React(pmpt_2, source=src, function=func)

    def __React(self, prompt, **pt_vars):
        with open(f"{os.path.dirname(os.path.realpath(__file__))}/{prompt}", "r") as f:
            template_string = f.read()
        prompt_template = ChatPromptTemplate.from_template(template_string)
        prompt_message = prompt_template.format_messages(**pt_vars)
        response = self.chat(prompt_message)
        return response.content


if __name__ == "__main__":
    pass
