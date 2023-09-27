from dataclasses import dataclass, field
from typing import Optional


@dataclass
class FrameDataArguments:
    """
    argments for preprocess data from raw HTML/VH elements to tensors as model input
    """

    # General arguments.
    reprocess: Optional[bool] = field(default=False, metadata={"help": "force to reprocess data"})
    multiprocess: Optional[bool] = field(default=False, metadata={"help": "multiprocess to process or not"})
    SENTENCE_ENCODER_MODEL: Optional[str] = field(
        default="bert-base-nli-mean-tokens",
        metadata={"help": "The name to call corresponding model from sentence-bert"},
    )

    # Screen-related arguments.
    data_type: Optional[str] = field(default="vh", metadata={"help": "[dom, vh]"})
    split_type: Optional[str] = field(default="dev", metadata={"help": "[train, dev, test]"})
    norm_max_dom_width: Optional[int] = field(default=411,
                                              metadata={"help": "the number used to norm bbox coorddinate"})
    norm_max_dom_height: Optional[int] = field(default=823,
                                               metadata={"help": "the number used to norm bbox coorddinate"})
    norm_max_vh_width: Optional[int] = field(default=1440,
                                             metadata={"help": "the number used to norm bbox coorddinate"})
    norm_max_vh_height: Optional[int] = field(default=2560,
                                              metadata={"help": "the number used to norm bbox coorddinate"})

    # Transformer-related arguments.
    max_seq_len: Optional[int] = field(default=256,
                                       metadata={"help": "max element number in the input seq (padded len)"})
    max_cur_el_len: Optional[int] = field(default=250, metadata={"help": "the max element number from current frame"})
    max_normed_bbox_pos: Optional[int] = field(default=1024, metadata={"help": "TODO"})
    max_relative_bbox: Optional[int] = field(default=10, metadata={"help": "TODO"})
    relative_scale: Optional[int] = field(default=5, metadata={"help": "TODO"})
    el_img_size: Optional[int] = field(default=32, metadata={"help": "max size of element's cropped images"})

    # VH-specific arguments.
    task_type: Optional[str] = field(default="app_sim", metadata={"help": "[app_sim, ref_exp]. only for vh"})
