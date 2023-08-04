import os

import openai
from langchain.chat_models import ChatOpenAI
from langchain.prompts import ChatPromptTemplate

api_type    = "azure"
api_base    = "https://max-es-gpt.openai.azure.com"
api_engine  = "gpt-35-turbo"
api_version = "2023-03-15-preview"
api_key     = "xxxxxxxxxxxxxxxxxxxxxxxxxx"

openai.api_type = api_type
openai.api_base = api_base
openai.engine = api_engine
openai.api_version = api_version
openai.api_key = api_key

os.environ["OPENAI_API_TYPE"] = api_type
os.environ["OPENAI_API_BASE"] = api_base
os.environ["OPENAI_API_VERSION"] = api_version
os.environ["OPENAI_API_KEY"] = api_key

class client:
    def __init__(self):
        self.chat = ChatOpenAI(
            api_type=api_type,
            api_base=api_base,
            api_key=api_key,
            api_version=api_version,
            engine=api_engine,
            max_tokens=1024,
            temperature=0.01)

    def JsonSourceToFunctions(self, source):
        with open(f'{os.path.dirname(os.path.realpath(__file__))}/prompts/performance_analyze_3.txt', 'r') as f:
            template_string = f.read()
        prompt_template = ChatPromptTemplate.from_template(template_string)
        prompt_message = prompt_template.format_messages(source=js)
        response = chat(prompt_message)
        return response.content

if __name__ == '__main__':
    pass
