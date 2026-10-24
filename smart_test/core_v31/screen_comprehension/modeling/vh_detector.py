''' Extract leaf VH elements for every frame. It takes ~2min to run the program on my PC.
'''
import functools
import json
import multiprocessing
import os
from typing import Dict

from tqdm import tqdm


class VhDetector:
    def __init__(self, filename, input_folder, output_folder):
        self.filename = filename
        self.input_folder = input_folder
        self.output_folder = output_folder
        with open(os.path.join(input_folder, filename), encoding="utf-8") as f:
            self.vh = json.load(f)
        self.elem_dict = {"vh_elements": list()}
        self.traverse(self.vh["activity"]["root"])  # update self.elem_dict
        with open(os.path.join(output_folder, filename), "w", encoding="utf-8") as f:
            json.dump(self.elem_dict, f, sort_keys=True)

    def traverse(self, node: Dict, threshold=5):
        if "children" in node:
            for child in node["children"]:
                if child is not None:  # There are children that are None.
                    self.traverse(child)
        else:  # Leaf nodes.
            if node["visible-to-user"] and node["bounds"][2] - node["bounds"][0] >= threshold \
                    and node["bounds"][3] - node["bounds"][1] >= threshold:
                self.elem_dict["vh_elements"].append(node)

    @classmethod
    def is_nonempty_element(cls, node: Dict):
        text = node.get("text")
        if text is None:
            text = ""
        text_hint = node.get("text-hint")
        if text_hint is None:
            text_hint = ""
        content_desc = node.get("content-desc")
        if content_desc is None:
            content_desc = ""
        else:
            content_desc = content_desc[0]
            if content_desc is None:
                content_desc = ""
        return text.strip() or text_hint.strip() or content_desc.strip()


def single_task(filename, input_folder, output_folder):
    vd = VhDetector(filename, input_folder, output_folder)


def main():
    input_folder = "../datasets/rico/raw"
    output_folder = "../datasets/rico/vh"
    filenames = sorted(
        list(filter(lambda x: x.endswith(".json"), os.listdir(input_folder))))

    with multiprocessing.Pool() as p:
        max_ = len(filenames)
        with tqdm(total=max_, desc="Extracting VH") as pbar:
            for i, _ in enumerate(p.imap_unordered(functools.partial(single_task,
                                                                     input_folder=input_folder,
                                                                     output_folder=output_folder), filenames)):
                pbar.update()

    # for filename in filenames:
    #     single_task(filename, input_folder, output_folder)


if __name__ == "__main__":
    main()
