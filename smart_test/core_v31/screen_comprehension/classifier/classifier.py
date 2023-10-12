import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from loguru import logger
from torch.utils.data import DataLoader

from screen_comprehension.classifier.data_set import EnricoDataSet


class Classifier(nn.Module):
    def __init__(self, device):
        super().__init__()
        self.device = device

        self.fc1 = nn.Linear(768, 120)
        self.fc2 = nn.Linear(120, 84)
        self.fc3 = nn.Linear(84, 20)

    def forward(self, x):
        x = F.relu(self.fc1(x))
        x = F.relu(self.fc2(x))
        x = self.fc3(x)
        output = F.log_softmax(x, dim=1)
        return output

    def load_from_pretrain(self, path_to_pretrained_model):
        warning_message = self.load_state_dict(torch.load(path_to_pretrained_model, map_location=self.device).state_dict())
        logger.info("Loading from pretrained Topic Classifier......")
        logger.info("Missing keys: " + str(warning_message[0]))
        logger.info("Unexpected keys: " + str(warning_message[1]))

    def classify(self, screen_feature):
        screen_feature.to(self.device)
        self.eval()
        _, predicted = torch.max(self.forward(screen_feature).data, 1)
        return predicted


def train(model, device, train_loader, epoch, log_interval):
    model.train()

    criterion = nn.CrossEntropyLoss()
    optimizer = optim.SGD(model.parameters(), lr=0.0001, momentum=0.9)

    for batch_idx, (data, target) in enumerate(train_loader):
        data, target = data.to(device), target.to(device)
        optimizer.zero_grad()
        output = model(data)
        loss = criterion(output, target)
        loss.backward()
        optimizer.step()
        if batch_idx % log_interval == (log_interval - 1):
            print('Train Epoch: {} [{}/{} ({:.0f}%)]\tLoss: {:.6f}'.format(
                epoch, batch_idx * len(data), len(train_loader.dataset),
                       100. * batch_idx / len(train_loader), loss.item()))

    print('Finished Training')
    torch.save(model, './model/{}.pt'.format(epoch))


def test(model, device, criterion, test_loader):
    model.eval()
    test_loss = 0
    correct = 0
    with torch.no_grad():
        for data, target in test_loader:
            data, target = data.to(device), target.to(device)
            output = model(data)
            test_loss += criterion(output, target).item()
            pred = output.argmax(dim=1, keepdim=True)
            correct += pred.eq(target.view_as(pred)).sum().item()

    test_loss /= len(test_loader.dataset)

    print('\nTest set: Average loss: {:.4f}, Accuracy: {}/{} ({:.0f}%)\n'.format(
        test_loss, correct, len(test_loader.dataset),
        100. * correct / len(test_loader.dataset)))


def classify(path_to_model, device, screen_bert):
    model = torch.load(path_to_model, map_location=device)
    model.eval()

    output = model(screen_bert)

    _, predicted = torch.max(output.data, 1)

    return predicted


if __name__ == '__main__':
    epochs = 0

    device = torch.device("cpu")

    training_data = EnricoDataSet("../datasets/rico/classifier")
    train_loader = DataLoader(training_data, batch_size=8, shuffle=True)

    # model = Classifier().to(device)

    model = torch.load('./model/0.pt', map_location=torch.device('cpu'))

    for epoch in range(0, epochs + 1):
        train(model, device, train_loader, epoch, 1)

# if __name__ == '__main__':
#     device = torch.device("cpu")
#
#     training_data = EnricoDataSet("../datasets/rico/classifier")
#     train_loader = DataLoader(training_data, batch_size=64, shuffle=True)
#
#     train_features, train_labels = next(iter(train_loader))
#
#     print(classify('./model/2001.pt', device, train_features))
#     print(train_labels)
