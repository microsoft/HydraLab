import json
import requests
import base64
from tenacity import retry, stop_after_attempt, wait_fixed
import common.utils as utils
from PIL import Image
from io import BytesIO


class OpenAIResponseMessage:
    def __init__(self, response_dict):
        """
        Create a GPT response from a dictionary, 一般结构如下:
        id: str
        object: str
        created: int
        model: str
        prompt_filter_results: List[Dict]
        choices: List[Dict]
        usage: Dict
        :param response_dict:
        """
        self.id = response_dict.get("id", None)
        self.object = response_dict.get("object", None)
        self.created = response_dict.get("created", None)
        self.model = response_dict.get("model", None)
        self.prompt_filter_results = response_dict.get("prompt_filter_results", None)
        self.choices = response_dict.get("choices", None)
        self.usage = response_dict.get("usage", None)

    def get_default_choice_content(self):
        return self.choices[0]['message']['content'] if self.choices and self.choices[0] else None

    def get_default_choice_role(self):
        return self.choices[0]['message']['role'] if self.choices and self.choices[0] else None

    def get_total_tokens(self):
        return self.usage.get("total_tokens", None) if self.usage else None

    def extract_default_choice_json_content(self):
        return utils.extract_json(self.get_default_choice_content())


class AzureAIRequestException(Exception):
    pass


class AzureOpenAIHttpClient:
    def __init__(self, api_key, endpoint, api_version, deployment, temperature=0.7, top_p=0.95, max_tokens=4096):
        self.api_key = api_key
        self.endpoint = endpoint
        self.api_version = api_version
        self.deployment = deployment
        self.temperature = temperature
        self.top_p = top_p
        self.max_tokens = max_tokens
        self.completion_base_url = f"{self.endpoint}openai/deployments/{self.deployment}/chat/completions?api-version={self.api_version}"

    @retry(stop=stop_after_attempt(3), wait=wait_fixed(3))
    def send_request(self, payload) -> OpenAIResponseMessage:
        headers = {
            "Content-Type": "application/json",
            "api-key": self.api_key,
        }
        response = None
        try:
            # print(f"Sending request to Azure {self.completion_base_url}")
            response = requests.post(self.completion_base_url, headers=headers, json=payload, timeout=120)
            # Will raise an HTTPError if the HTTP request returned an unsuccessful status code
            response.raise_for_status()
        except requests.RequestException as e:
            raise AzureAIRequestException(
                f"Failed to make the request. Error: {e}\nresponse error content: {response.content if response else ''}")
        return OpenAIResponseMessage(response.json())

    def chat_completion(self, messages) -> OpenAIResponseMessage:
        payload = {
            "messages": messages,
            "temperature": self.temperature,
            "top_p": self.top_p,
            "max_tokens": self.max_tokens
        }
        return self.send_request(payload)

    def chat_completion_with_image(self, system_message, user_message, image_path,
                                   image_resize_max_width=None) -> OpenAIResponseMessage:
        base64_encoded_str = utils.get_image_base64(image_path, image_resize_max_width)

        messages = [
            {
                "role": "system",
                "content": [
                    {
                        "type": "text",
                        "text": system_message
                    }
                ]
            },
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": user_message
                    },
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{base64_encoded_str}"
                        }
                    }
                ]
            }
        ]
        return self.chat_completion(messages)



