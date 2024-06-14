import os
from langchain_core.messages import HumanMessage
from langchain_openai import ChatOpenAI, AzureChatOpenAI
from dotenv import load_dotenv, find_dotenv
import base64
from PIL import Image
from io import BytesIO


def get_image_base64(image_path, image_resize_max_width):
    if image_resize_max_width:
        # Open an existing image
        original_image = Image.open(image_path)
        # Get the size of the original image
        original_width, original_height = original_image.size
        read_image = original_image
        if original_width > image_resize_max_width:
            # Set the new width
            new_width = image_resize_max_width
            # Calculate the new height to maintain the aspect ratio
            new_height = int((new_width * original_height) / original_width)
            # Resize the image
            read_image = original_image.resize((new_width, new_height))

        # Save the resized image to a buffer instead of a file
        buffered = BytesIO()
        read_image.save(buffered, format="JPEG")
        # Convert the image data to a base64 string
        base64_encoded_str = base64.b64encode(buffered.getvalue()).decode('ascii')
    else:
        # Open the image file in binary mode and convert it to a base64 string
        with open(image_path, 'rb') as image_file:
            # Read the image data
            image_data = image_file.read()
            # Encode the data as base64
            base64_encoded_str = base64.b64encode(image_data).decode('ascii')
    return base64_encoded_str


_ = load_dotenv(find_dotenv())
key = os.environ.get("OPENAI_API_KEY")
llm = ChatOpenAI(model="gpt-4o", api_key=os.environ.get("OPENAI_API_KEY"))

azure_gpt4_text_model_api_key = os.getenv("AZURE_OPENAI_API_KEY")
azure_gpt4_text_model_endpoint = os.getenv("AZURE_OPENAI_ENDPOINT")
azure_gpt4_text_model_deployment = os.getenv("AZURE_OPENAI_DEPLOYMENT")
azure_llm = AzureChatOpenAI(
    openai_api_version="2024-02-01",
    azure_deployment=azure_gpt4_text_model_deployment,
    azure_endpoint=azure_gpt4_text_model_endpoint,
    api_key=azure_gpt4_text_model_api_key
)

resp = azure_llm.invoke("上海是个什么样的城市")
print(resp)


def image_engage():
    base64_img_1 = get_image_base64("screenshots/screenshot_extracted_0.png", 480)
    base64_img_2 = get_image_base64("screenshots/screenshot_extracted_26.png", 480)
    base64_img_3 = get_image_base64("screenshots/screenshot_extracted_27.png", 480)

    messages = HumanMessage(
        content=[
            {'type': 'text',
             'text': 'Tell me the link between the 3 screenshots and the possible interactions happened between them.'},
            {'type': 'image_url', 'image_url':
                {'url': f"data:image/jpeg;base64,{base64_img_1}"}
             },
            {'type': 'image_url', 'image_url':
                {'url': f"data:image/jpeg;base64,{base64_img_2}"}
             },
            {'type': 'image_url', 'image_url':
                {'url': f"data:image/jpeg;base64,{base64_img_3}"}
             }
        ]
    )
    resp = azure_llm.invoke([messages])

    print(resp.content)


image_engage()
