import os

import torch
from torch.utils.data import Dataset


class EnricoDataSet(Dataset):
    def __init__(self, dataset_dir):
        self.dataset_dir = dataset_dir
        self.filenames = list(filter(lambda x: x.endswith(".pt"), os.listdir(self.dataset_dir)))
        self.filenames.sort()

    def __len__(self):
        return len(self.filenames)

    def __getitem__(self, idx):
        path = os.path.join(self.dataset_dir, self.filenames[idx])
        data = torch.load(path)
        data = data['pooler_output']
        data = data.view(-1)
        # print(os.path.splitext(self.filenames[idx])[0].split('-'))
        _label = os.path.splitext(self.filenames[idx])[0].split('_')[2]
        _label = topics[_label]
        # label = torch.LongTensor(_label)
        return data, _label


topics = {
    'bare': 0,
    'calculator': 1,
    'camera': 2,
    'chat': 3,
    'editor': 4,
    'form': 5,
    'gallery': 6,
    'list': 7,
    'login': 8,
    'maps': 9,
    'mediaplayer': 10,
    'menu': 11,
    'modal': 12,
    'news': 13,
    'other': 14,
    'profile': 15,
    'search': 16,
    'settings': 17,
    'terms': 18,
    'tutorial': 19
}
