# Smart Test


## Preparation

### Prepare environment
- Install miniconda: https://docs.conda.io/en/latest/miniconda.html#windows-installers
- Create a conda env: `conda create --name smart_test python==3.9`
- (Optional) Init Powershell: `conda init powershell`
- Activate the environment: `conda activate smart_test` or [Configure a conda virtual environment in Pycharm](https://www.jetbrains.com/help/pycharm/conda-support-creating-conda-virtual-environment.html#580c7f1)

Or if you are using the python3 as exec under Windows OS:
```powershell
# check your python version, and you can download python 3.9 in the following page:
# https://www.python.org/downloads/release/python-390/
# It may be python3 or python, depending on your ENV configuration.
python3 --version
python3 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r .\requirements.txt
# going forward you can use python now under this env path
python --version
```

- Install requirements: `pip install -r requirements.txt`
- Install Appium Server: https://github.com/appium/appium-desktop/releases/tag/v1.22.3-4
- Add existing conda environment in Pycharm https://www.jetbrains.com/help/pycharm/conda-support-creating-conda-virtual-environment.html#580c7f1

### Download attachments
- Download attachments from [Attachments.zip](https://microsoft.sharepoint.com/:u:/t/stca/MobileX/EYrH0Rs8rVFKnH62qAD0tFMBt1Ep2Sn0nsRqpwYvY2qVkw?e=WHtcmr).

## Usage

### Args
```
app_path: String
device_info: Json String
    serialNum
    osVersion
test_info: Json String
    path_to_screen_bert_model
    path_to_screen_topic_classifier_model
max_step: Int
string_pool_dir: String (Reserve for login rules to store the username and password)
result_dir: String
```
#### Example

```
args = [
    "./apps/LTW.apk",
    "{'serialNum':'9ASACS000006UP','osVersion':'11'}",
    "{'path_to_screen_bert_model':'./screenBertv0.1_app_cls_220328.pt','path_to_screen_topic_classifier_model':'./Attachment/Topic/com.microsoft.appmanager.pt'}",
    "50",
    "./string_pool",
    "./result"
      ]
```
### Return:
```
String: "smartTestResult: result"
result: Json String
result = {
    "success": True or False,
    "coverage": {},
    "actionList": [
        {
            'action_number': action,
            'step': step,
            'time_stamp': DateTime,
            "action_name": CLICK, INPUT, RULE... 
            'element_info': {
                "identifier": element_identifier,
                "bbox": element_bbox,
                "platform": ANDROID or WINDOWS
            }
        }
        ...
    ]
    "appException": [
        "crashStack_1",
        "crashStack_2"
    ]
}
```

### Output files

1. Screenshots and xml layout files
   - 0-0.png
   - 0-0.jpg
   -...
2. Diagrams
   - `directed_acyclic_graph.gexf`: Route map of the app with page grouped
   - `raw_directed_acyclic_graph.gexf`: Route map of the app without page grouped
   - `tree_diagram.gexf`: Tree map of the app
3. `page_group.pt`: Page group weights
   
     

### Run

1. [Optional] Launch Appium with command: `appium -p 10086`
2. Run Smart Test
    - Replace serialNum and Paths.
    - Powershell: run `python main.py "appPath" "{'serialNum':'serialNum','osVersion':'11'}" "{'path_to_screen_bert_model':'./screenBertv0.1_app_cls_220328.pt','path_to_screen_topic_classifier_model':'./Attachment/Topic/com.microsoft.appmanager.pt'}" "50" "./string_pool" "./result"`
    - Pycharm: [Modify run configuration pycharm](https://www.jetbrains.com/help/pycharm/run-debug-configuration.html) and paste parameters `"appPath" "{'serialNum':'serialNum','osVersion':'11'}" "{'path_to_screen_bert_model':'./screenBertv0.1_app_cls_220328.pt','path_to_screen_topic_classifier_model':'./Attachment/Topic/com.microsoft.appmanager.pt'}" "50" "./string_pool" "./result"`
