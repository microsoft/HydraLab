import base64
import logging
import os
import shutil

import cv2
import numpy as np
import torch
from sklearn import metrics
from torch.utils.tensorboard import SummaryWriter
from tqdm import tqdm


class AverageMeter(object):
    """Computes and stores the average and current value"""

    def __init__(self, name=""):
        self.name = name
        self.reset()

    def reset(self):
        self.val = 0
        self.avg = 0
        self.sum = 0
        self.count = 0

    def update(self, val, n=1):
        self.val = val
        self.sum += val * n
        self.count += n
        self.avg = self.sum / self.count

    def average(self):
        return self.avg

    def __str__(self):
        return "{}: {:.8f}".format(self.name, self.avg)


# Move optinmizer to given device
# This is required since we save the optimizer to a checkpoint and
# restore the optimizer from the saved checkpoint
def optimizer_to(optim, device):
    for param in optim.state.values():
        # Not sure there are any global tensors in the state dict
        if isinstance(param, torch.Tensor):
            param.data = param.data.to(device)
            if param._grad is not None:
                param._grad.data = param._grad.data.to(device)
        elif isinstance(param, dict):
            for subparam in param.values():
                if isinstance(subparam, torch.Tensor):
                    subparam.data = subparam.data.to(device)
                    if subparam._grad is not None:
                        subparam._grad.data = subparam._grad.data.to(device)


def checkpoint_model(checkpoint_dir, model, optimizer, epoch, loss):
    if not os.path.exists(checkpoint_dir):
        os.makedirs(checkpoint_dir)
    checkpoint_path = os.path.join(checkpoint_dir, "model_{}.pt".format(epoch))
    torch.save(
        {
            "epoch": epoch,
            "model_state_dict": model.state_dict(),
            "optimizer_state_dict": optimizer.state_dict(),
            "loss": loss,
        },
        checkpoint_path,
    )


def restore_checkpoint(restore_checkpoint_path, model, optimizer, device):
    checkpoint = torch.load(restore_checkpoint_path, map_location="cpu")
    model.load_state_dict(checkpoint["model_state_dict"])
    optimizer.load_state_dict(checkpoint["optimizer_state_dict"])
    epoch = checkpoint["epoch"]
    loss = checkpoint["loss"]
    print("Restoring model from a checkpoint at epoch {} with loss {}".format(epoch, loss))
    # Move model and optimzier to appropriate device
    model.to(device)
    torch.cuda.empty_cache()
    optimizer_to(optimizer, device)


def get_summary_writer(path, comment=None):
    return SummaryWriter(path, comment=comment)


def get_device():
    device = "cuda" if torch.cuda.is_available() else "cpu"
    if device[0] == "cuda":
        available_gpus = [torch.cuda.device(i) for i in range(torch.cuda.device_count())]
        return available_gpus[0]
    return device


def saved_trained_model(model, global_step, epoch, logger, output_dir):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    model_to_save = model.module if hasattr(model, "module") else model  # Only save the model it-self

    torch.save(model_to_save.state_dict(), os.path.join(output_dir, f"apn_model{epoch}.bin"))
    logger.info("model saved at iteration %d, epoch %d ", global_step, epoch)
    output_config_file = os.path.join(output_dir, "config.json")
    with open(output_config_file, "w") as f:
        f.write(model_to_save.config.to_json_string())


def get_samples_per_class(dataloader):
    device = "cpu"
    batch_trange = tqdm(dataloader)
    labels_list = []
    for batch_idx, batch_sample in enumerate(batch_trange):
        labels = batch_sample["labels"].to(device).to(torch.long)
        attention_mask = batch_sample["attention_mask"].to(device).to(torch.long)
        mask = attention_mask.view(-1) == 1
        true_labels = labels.view(-1)[mask]
        labels_list.append(torch.unsqueeze(true_labels, 0))

    total_labels = torch.cat(labels_list, axis=1)
    labels_values, labels_count = torch.unique(total_labels, sorted=True, return_counts=True)
    # print(labels_values)
    # print(labels_count)
    return labels_count.detach().cpu().numpy()


def compute_f1_metrics(idx_to_labels_map, y_labels, y_pred):
    labels_names = list(map(lambda x: idx_to_labels_map[x], y_labels))
    predicted_names = list(map(lambda x: idx_to_labels_map[x], y_pred))

    f1_metrics_dict = metrics.classification_report(labels_names, predicted_names, digits=3, output_dict=True)
    f1_metrics_str = metrics.classification_report(labels_names, predicted_names, digits=3)
    confusion_matrix = metrics.confusion_matrix(labels_names, predicted_names)
    return {
        "f1_metrics_dict": f1_metrics_dict,
        "f1_metrics_str": f1_metrics_str,
        "confusion_matrix": confusion_matrix,
    }


def mkdir(dir):
    if os.path.exists(dir):
        shutil.rmtree(dir)
    os.makedirs(dir)


def img_from_string(img_string):
    nparr = np.frombuffer(base64.b64decode(img_string), np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_ANYCOLOR)
    return img


def clamp(number, minimum, maximum):
    if number < minimum:
        number = minimum
    elif number > maximum:
        number = maximum
    return number


def init_logger(logger, log_out_path):
    if logger.handlers:
        return
    logging.basicConfig(level=logging.INFO)
    formatter = logging.Formatter(
        "%(asctime)s - %(message)s",
        datefmt="%m/%d/%Y %H:%M:%S",
    )
    fh = logging.FileHandler(os.path.join(log_out_path, "log.txt"))
    fh.setFormatter(formatter)
    ch = logging.StreamHandler()
    ch.setFormatter(formatter)
    logger.addHandler(ch)
    logger.addHandler(fh)


def accelerator_clip_grad_norm(accelerator, parameters, max_norm, norm_type=2):
    accelerator.unscale_gradients()
    total_norm = torch.nn.utils.clip_grad_norm_(parameters, max_norm, norm_type=norm_type)
    return total_norm
