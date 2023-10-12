from abc import ABC

from langchain.chat_models import AzureChatOpenAI
from langchain.output_parsers import PydanticOutputParser
from langchain.prompts import ChatPromptTemplate
from loguru import logger

from common.constants import llm_prompt
from llm.output_schema import ElementListRes


class LLM(ABC):
    def __init__(self, deployment_name, openai_api_key, openai_api_base, openai_api_version, temperature=0.0):
        self.llm_model = AzureChatOpenAI(deployment_name=deployment_name,
                                         openai_api_key=openai_api_key,
                                         openai_api_base=openai_api_base,
                                         openai_api_version=openai_api_version,
                                         temperature=temperature)

    def rank_element(self, element_input, template=llm_prompt.ELEMENT_RANKING_TEMPLATE) -> ElementListRes:
        res = None

        prompt_template = ChatPromptTemplate.from_template(template=template)
        output_parser = PydanticOutputParser(pydantic_object=ElementListRes)
        messages = prompt_template.format_messages(element_input=element_input,
                                                   format_instructions=output_parser.get_format_instructions())

        try:
            response = self.llm_model(messages)
            res = output_parser.parse(response.content)
        except:
            logger.exception("Result is malformed, cannot be parsed as object using output_parser.")

        return res
