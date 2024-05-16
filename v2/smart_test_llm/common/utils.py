import json, re, os
import xmltodict
import logging
from json import JSONEncoder
from PIL import Image, ImageDraw, ImageFont, ImageChops
import random
from typing import List
import uuid
import json
import base64
from PIL import Image
from io import BytesIO

LOG_LEVEL = logging.INFO
LOG_PATTERN = '%(levelname)s %(asctime)s (%(filename)s:%(lineno)d) - %(message)s'


class MyEncoder(JSONEncoder):
    def default(self, o):
        if not o:
            return None
        return o.__dict__


def build_file_logger(logger_name, need_console=True):
    logger = logging.getLogger(logger_name)
    logger.setLevel(LOG_LEVEL)
    formatter = logging.Formatter(LOG_PATTERN)
    if not os.path.exists("logs"):
        os.makedirs("logs")
    handler = logging.FileHandler(f"logs/{logger_name}.log", encoding='utf-8')
    handler.setLevel(LOG_LEVEL)
    handler.setFormatter(formatter)
    logger.addHandler(handler)
    if need_console:
        console_handler = logging.StreamHandler()
        console_handler.setLevel(LOG_LEVEL)
        console_handler.setFormatter(formatter)
        logger.addHandler(console_handler)
    return logger


def extract_json(message_contains_json: str):
    if "```json" in message_contains_json:
        message_contains_json = message_contains_json.split("```json")[1].strip()
        message_contains_json = message_contains_json.split("```")[0].strip()
        return json.loads(message_contains_json)
    elif message_contains_json.startswith("{") and message_contains_json.endswith("}"):
        return json.loads(message_contains_json)
    return None


def capture_json_object(text):
    text = text.replace("\n", "")
    try:
        # The regex pattern to match a JSON object
        pattern = r"(\{.*\}|\[.*\])"
        # Search for the JSON object in the text
        match = re.search(pattern, text)
        if match:
            # If a match is found, try to parse it as JSON
            print(match.group(0))
            json_object = json.loads(match.group(0))
            return json_object
        else:
            # If no match is found, return None
            return None
    except json.JSONDecodeError as e:
        # If the match is not a valid JSON object, return None
        return None


def get_xml_as_json(xml):
    return json.dumps(xmltodict.parse(xml), indent=2)


def is_bounds_in_center(bounds, display_width, display_height, center_x_offset_ratio, center_y_offset_ratio):
    """
    判断元素的边界是否在屏幕中心区域。

    参数:
    bounds (str): 元素的边界坐标，格式为"[left, top][right, bottom]"。
    display_width (int): 屏幕宽度。
    display_height (int): 屏幕高度。
    center_x_offset_ratio (float): 屏幕中心区域的宽度占比。
    center_y_offset_ratio (float): 屏幕中心区域的高度占比。

    返回:
    bool: 如果元素的边界在屏幕中心区域内，则返回True；否则返回False。
    """
    bounds = bounds.strip("[]").split("][")
    left, top = map(int, bounds[0].split(","))
    right, bottom = map(int, bounds[1].split(","))
    center_x_offset = display_width * center_x_offset_ratio
    center_y_offset = display_height * center_y_offset_ratio
    center_x = display_width / 2
    center_y = display_height / 2
    return left < center_x + center_x_offset and right > center_x - center_x_offset and top < center_y + center_y_offset and bottom > center_y - center_y_offset


def get_cord_from_bounds(bounds, scale=None):
    bounds = bounds.strip("[]").split("][")
    left, top = map(int, bounds[0].split(","))
    right, bottom = map(int, bounds[1].split(","))
    if scale:
        left *= scale
        top *= scale
        right *= scale
        bottom *= scale
    return left, top, right, bottom


def draw_rectangle_on_image(target_image: Image, coordinates, tag=None, z_index=0, coordinates_scale=None,
                            width=2):
    """
    在图像上根据指定的字符串坐标画一个方框。

    参数:
    image_path (str): 图像文件的路径。
    coordinates (str): 字符串形式的坐标，格式为"[left, top][right, bottom]"。
    color (str): 方框的颜色，默认为黄色。
    """
    # 加载图像
    draw = ImageDraw.Draw(target_image)
    left, top, right, bottom = get_cord_from_bounds(coordinates, coordinates_scale)
    color_intensity = max(255 - int((z_index / 16) * 255), 0)
    color = (color_intensity, color_intensity, 0)
    # 画方框
    draw.rectangle([left, top, right, bottom], outline=color, width=width)

    # 选择一个角来绘制tag
    if tag:
        corners = [(left + 5, top + 5), (right - 5, top + 5), (left + 5, bottom - 5), (right - 5, bottom - 5)]
        selected_corner = random.choice(corners)
        text_height = 16
        # 加载一个字体
        try:
            font = ImageFont.truetype("arial.ttf", text_height)  # 可能需要调整字体路径和大小
        except IOError:
            font = ImageFont.load_default()

        # 计算文本尺寸
        text_width = draw.textlength(tag, font=font)
        # 根据选定的角调整文本位置，避免绘制超出框内
        text_x, text_y = selected_corner
        if text_x > right / 2:
            text_x -= text_width
        if text_y > bottom / 2:
            text_y -= text_height

        # 绘制带背景的文本
        text_background_color = color
        draw.rectangle([text_x, text_y, text_x + text_width, text_y + text_height], fill=text_background_color)
        draw.text((text_x, text_y), tag, font=font, fill="white")
    return target_image


def draw_multiple_rectangles_on_image(target_image: Image, coordinates: List[str], coordinates_scale=None,
                                      color='green',
                                      width=2):
    """
    在图像上根据指定的字符串坐标画一个方框。

    参数:
    image_path (str): 图像文件的路径。
    coordinates (str): 字符串形式的坐标，格式为"[left, top][right, bottom]"。
    color (str): 方框的颜色，默认为黄色。
    """
    # 加载图像
    draw = ImageDraw.Draw(target_image)
    for coordinate in coordinates:
        left, top, right, bottom = get_cord_from_bounds(coordinate, coordinates_scale)
        # 画方框
        draw.rectangle([left, top, right, bottom], outline=color, width=width)
    return target_image


def get_center(bounds):
    bounds = bounds[1:-1].split("][")
    x1, y1 = map(int, bounds[0].split(","))
    x2, y2 = map(int, bounds[1].split(","))
    return (x1 + x2) // 2, (y1 + y2) // 2


def generate_short_id():
    # 生成一个UUID并转换为字符串
    uuid_str = str(uuid.uuid4()).replace('-', '')

    # 确保UUID字符串足够长，可以随机选择10个字符
    if len(uuid_str) < 10:
        return uuid_str

    # 随机选择10个字符作为ID
    return ''.join(random.sample(uuid_str, 12))


def rename_file(file_path, new_file_name):
    # 提取文件的目录路径
    directory = os.path.dirname(file_path)
    # 构造新的完整文件路径
    new_file_path = os.path.join(directory, new_file_name)
    # 重命名文件
    os.rename(file_path, new_file_path)
    return new_file_path


def get_image_difference_ratio(current_screenshot: Image, previous_screenshot: Image):
    diff = ImageChops.difference(previous_screenshot, current_screenshot)
    # 将差异图像转换为灰度并统计非零像素，即差异像素
    non_zero_pixels = sum(diff.convert("L").point(lambda x: 0 if x == 0 else 1).getdata())
    total_pixels = previous_screenshot.width * previous_screenshot.height
    # 计算差异比例
    diff_ratio = non_zero_pixels / total_pixels
    return diff_ratio


def resize_image_by_width(image: Image, width):
    """
    Resize an image by width while keeping the aspect ratio.

    Args:
    image (PIL Image): The image to be resized.
    width (int): The target width.

    Returns:
    PIL Image: The resized image.
    """
    height = int(width * image.height / image.width)
    return image.resize((width, height))


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
