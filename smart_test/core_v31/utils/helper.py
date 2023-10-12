import logging
import os
import shutil
import sys
import warnings

import cv2
from androguard.core.bytecodes import apk
from loguru import logger


def create_if_not_exists(path):
    os.makedirs(path, exist_ok=True)


def save_screenshot_and_page_source(output_dir, file_name, screenshot, page_source):
    create_if_not_exists(f'{output_dir}')
    cv2.imwrite(os.path.join(output_dir, file_name + '.jpg'), screenshot)
    with open(os.path.join(output_dir, file_name + '.xml'), 'w') as f:
        try:
            f.write(page_source)
        except:
            pass


def load_strings(strings_pool_dir):
    strings = {}

    with open(os.path.join(strings_pool_dir, 'strings.txt'), 'r+') as f:
        strings['generic'] = f.read().split('\n')
    with open(os.path.join(strings_pool_dir, 'username.txt'), 'r+') as f:
        strings['username'] = f.read().split('\n')
    with open(os.path.join(strings_pool_dir, 'password.txt'), 'r+') as f:
        strings['password'] = f.read().split('\n')

    assert len(strings['username']) == len(strings['password'])

    return strings


def get_package_name(app_path):
    return apk.APK(app_path).get_package()


def make_absolute_path(path: str) -> str:
    if not os.path.isabs(path):
        path = os.path.abspath(path)
    return path


def logger_init():
    sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')
    warnings.filterwarnings('ignore')
    os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
    logging.getLogger('tensorflow').setLevel(logging.FATAL)
    logging.root.setLevel(logging.CRITICAL)
    logger.remove(handler_id=None)
    logger_format = "<green>{time:MM-DD HH:mm:ss.SSS}</green> | <level>{level: <5}</level> | <level>{message}</level>"
    logger.add(sys.stdout, colorize=True, format=logger_format)
    # used for metric output
    # logger.add(sys.stdout, level="ERROR", colorize=True, format=logger_format)


def copy_dir(source_path, target_path):
    if not os.path.exists(target_path):
        os.makedirs(target_path)
    if os.path.exists(source_path):
        shutil.rmtree(target_path)
    shutil.copytree(source_path, target_path)
