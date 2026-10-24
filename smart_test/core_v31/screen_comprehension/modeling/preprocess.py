import functools
import json
import logging
import multiprocessing
import os

import torch
from sentence_transformers import SentenceTransformer
from tqdm import tqdm

from screen_comprehension.modeling.frame_data import FrameData
from screen_comprehension.modeling.utils import get_device

device = get_device()
CPU = torch.device("cpu")

logger = logging.getLogger(__name__)


def single_preprocess_dom(filename, data_root, rico_root, sentence_encoder, args):
    with open(os.path.join(data_root, filename), "r") as f:
        trace_json = json.load(f)
    for idx, frame in enumerate(trace_json["frames"]):
        if "elements" not in frame or "screenshot" not in frame:
            continue
        frame_data = FrameData(
            url=frame["url_to_go"], screenshot=frame["screenshot"][23:], args=args)
        frame_data.metadata["frame_id"] = idx
        # assign frame_data.metadata["filename"]
        frame_data.assign_file_name(filename)
        flag = frame_data.parse_elements(sentence_encoder, elements=frame["elements"])
        if not flag:
            logger.info(f"skip trace {filename}-{idx} for too less elements")
            break

        cache_dir = os.path.join(data_root, "cache")
        if not os.path.exists(cache_dir):
            os.makedirs(cache_dir)
        torch.save(frame_data, os.path.join(
            cache_dir, "cached_{}_{}.pt".format(filename[:-5], idx)))


def single_preprocess_vh(filename, data_root, rico_root, sentence_encoder, args):
    with open(os.path.join(data_root, filename), "r") as f:
        frame = json.load(f)
    if args.task_type == "app_sim":
        img_filename = os.path.join(rico_root, filename.replace(".json", ".jpg"))
    else:
        img_filename = os.path.join(rico_root, "{}.jpg".format(frame["image/id"]))
    frame_data = FrameData(
        img_filename=img_filename, args=args)
    if args.task_type == "app_sim":
        idx = int(filename.split(".")[0])
    else:
        idx = int(filename.split(".")[0].split("_")[1])
    frame_data.metadata["frame_id"] = idx
    # assign frame_data.metadata["filename"]
    frame_data.assign_file_name(filename)
    flag = frame_data.parse_elements(sentence_encoder, frame=frame)
    if not flag:
        logger.info(f"skip trace {filename}-{idx} for too less elements")

    cache_dir = data_root.replace("cache_json", "cache")
    os.makedirs(cache_dir, exist_ok=True)
    torch.save(frame_data, os.path.join(
        cache_dir, "cached_{}.pt".format(filename[:-5])))


def preprocess(data_root, rico_root, args):
    filenames = list(
        filter(lambda x: x.endswith(".json"), os.listdir(data_root)))
    sentence_encoder = SentenceTransformer(args.SENTENCE_ENCODER_MODEL).cuda()
    if args.multiprocess:
        with multiprocessing.Pool() as p:
            max_ = len(filenames)
            with tqdm(total=max_, desc="Preprocessing the raw data...") as pbar:
                for i, _ in enumerate(
                        p.imap_unordered(functools.partial(
                            single_preprocess_dom if args.data_type == "dom" else single_preprocess_vh,
                            data_root=data_root, rico_root=rico_root,
                            sentence_encoder=sentence_encoder, args=args), filenames)):
                    pbar.update()
    else:
        max_ = len(filenames)
        with tqdm(total=max_, desc="Preprocessing the raw data...") as pbar:
            for index, filename in enumerate(filenames):
                _ = (single_preprocess_dom if args.data_type == "dom" else single_preprocess_vh)(
                    filename, data_root=data_root, rico_root=rico_root, sentence_encoder=sentence_encoder, args=args)
                pbar.update()


if __name__ == "__main__":
    from frame_data_args import FrameDataArguments
    from transformers import HfArgumentParser

    parser = HfArgumentParser((FrameDataArguments))
    args = parser.parse_args_into_dataclasses()[0]
    # if args.data_type == "dom":  # DOM.
    #     data_root = "data/dummy/training_data"
    # elif args.task_type == "app_sim":  # VH. app_sim.
    #     data_root = "data/rico/combined"
    # else:  # VH. ref_exp.
    #     data_root = "data/ref_exp/{}_data/cache_json".format(args.split_type)
    rico_root = "../datasets/rico/raw"
    data_root = "../datasets/rico/vh"
    args.task_type = "app_sim"
    preprocess(data_root, rico_root, args)
