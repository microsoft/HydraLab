import os
from shutil import copyfile

import cv2
import pandas as pd
import torch

from environment.screen_bert_model import ScreenBertModel
from environment.device_manager.elements_extractor import *
from screenbert.classifier.classifier import TopicClassify, Classifier


design_topics = pd.read_csv("./design_topics.csv")

screen_ids = design_topics['screen_id'].values.tolist()

topics = set(design_topics['topic'].values.tolist())

screen_ids_dict = design_topics.set_index('screen_id').T.to_dict('list')


def label(src, dst):
    filenames = list(filter(lambda x: x.endswith(".pt"), os.listdir(src)))
    for index, filename in enumerate(filenames):
        screen_id = filename.split('.')[0].split('_')[1]
        _label = screen_ids_dict[int(screen_id)][0]
        copyfile(os.path.join(src, "{}".format(filename)), os.path.join(dst, "{}-{}.pt".format(screen_id, _label)))
        print(screen_id, label)


def copy_rico(screen_ids, src, dst):
    no_such_file = 0
    for screen_id in screen_ids:
        source_file_jpg = os.path.join(src, '{}.jpg'.format(screen_id))
        source_file_json = os.path.join(src, '{}.json'.format(screen_id))
        destination_file_jpg = os.path.join(dst, '{}.jpg'.format(screen_id))
        destination_file_json = os.path.join(src, '{}.json'.format(screen_id))

        try:
            copyfile(source_file_jpg, destination_file_jpg)
            copyfile(source_file_json, destination_file_json)
        except:
            no_such_file += 1

    return no_such_file


def run_screen_bert(src, path_to_screen_bert_model, path_to_screen_topic_classifier_model):
    screen_bert_model = ScreenBertModel(path_to_screen_bert_model, path_to_screen_topic_classifier_model)

    filenames = list(filter(lambda x: x.endswith(".xml"), os.listdir(src)))
    for index, filename in enumerate(filenames):
        print(filename)
        screenshot_filename = os.path.splitext(filename)[0] + '.jpg'
        screenshot = cv2.imread(os.path.join(src, screenshot_filename))
        with open(os.path.join(src, filename), 'r') as f:
            page_source = f.read()

        frame_data, output = screen_bert_model.encode_screen(screenshot, parse_page_source(page_source))
        topic = screen_bert_model.classify(output)

        torch.save(output, os.path.join(src, os.path.splitext(filename)[0] + '.pt'))
