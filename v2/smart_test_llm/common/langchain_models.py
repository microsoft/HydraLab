import os
from common.azure_openai_http import AzureOpenAIHttpClient
from langchain_openai import AzureChatOpenAI
from dotenv import load_dotenv, find_dotenv

_ = load_dotenv(find_dotenv())

azure_gpt4_text_model_api_version = "2024-02-01"
azure_gpt4_text_model_api_key = os.getenv("AZURE_GPT4_TEXT_MODEL_API_KEY")
azure_gpt4_text_model_endpoint = os.getenv("AZURE_GPT4_TEXT_MODEL_ENDPOINT")
azure_gpt4_text_model_deployment = os.getenv("AZURE_GPT4_TEXT_MODEL_DEPLOYMENT")
azure_gpt4_text_model_temperature = 0

azure_gpt4_vision_model_api_version = "2024-02-15-preview"
azure_gpt4_vision_model_deployment = os.getenv("AZURE_GPT4_VISION_MODEL_DEPLOYMENT")
azure_gpt4_vision_model_api_key = os.getenv("AZURE_GPT4_VISION_MODEL_API_KEY")
azure_gpt4_vision_model_endpoint = os.getenv("AZURE_GPT4_VISION_MODEL_ENDPOINT")
azure_gpt4_vision_model_temperature = 0.2

azure_gpt35_text_model_api_version = "2024-02-01"
azure_gpt35_text_model_api_key = os.getenv("AZURE_GPT35_TEXT_MODEL_API_KEY")
azure_gpt35_text_model_endpoint = os.getenv("AZURE_GPT35_TEXT_MODEL_ENDPOINT")
azure_gpt35_text_model_deployment = os.getenv("AZURE_GPT35_TEXT_MODEL_DEPLOYMENT")
azure_gpt35_text_model_temperature = 0


class LangChainChatModels:
    def __init__(self):
        self.azure_gpt4_text_model = AzureChatOpenAI(
            openai_api_key=azure_gpt4_text_model_api_key,
            azure_endpoint=azure_gpt4_text_model_endpoint,
            openai_api_version=azure_gpt4_text_model_api_version,
            azure_deployment=azure_gpt4_text_model_deployment,
            temperature=azure_gpt4_text_model_temperature
        )
        self.azure_gpt4_vision_model = AzureOpenAIHttpClient(
            api_key=azure_gpt4_vision_model_api_key,
            endpoint=azure_gpt4_vision_model_endpoint,
            api_version=azure_gpt4_vision_model_api_version,
            deployment=azure_gpt4_vision_model_deployment,
            temperature=azure_gpt4_vision_model_temperature
        )
        self.azure_gpt35_text_model = AzureChatOpenAI(
            openai_api_key=azure_gpt35_text_model_api_key,
            azure_endpoint=azure_gpt35_text_model_endpoint,
            openai_api_version=azure_gpt35_text_model_api_version,
            azure_deployment=azure_gpt35_text_model_deployment,
            temperature=azure_gpt35_text_model_temperature
        )
