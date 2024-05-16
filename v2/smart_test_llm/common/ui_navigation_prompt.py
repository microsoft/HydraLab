import json

system_message_android_ui_navi_prompt = """You are a helpful Android test automation assistant. 
Given a task instruction, you should give a list of actions in JSON format to finish the following task:
General instruction: {explore_instruction}
Now we are conducting the No.{current_step_index} subtask step: {current_step}
in the target app with the following description:
{app_description}
"""
ui_navi_prompt = '''
Below content delimited by triple backticks is a phone {page_scenario_category} UI element list:
```
{key_elements}
```
{focused_element_desc}
The elements are located in a page with top activity "{top_activity}" in the resumed app "{current_app_package}".
In above each element, the following attributes are provided:
- index: the index of the element in the list, you may use it to refer to the element.
- descriptor: the descriptor of the element, including the type of the view, the resource id name of the view, the description of the view if any, the depth of the element after the '-'.
- bounds: the out bounds of the element in a [left, top], [right, bottom] format, indicating diagonals' coordinates.
- text: the text showed on the element

And to complete the task, you have the identity information to leverage as auth input as follows:
{test_identity}
and below are some notes:
- You just did the action {previous_action} on previous {previous_scenario_category} page to complete task "{previous_step}"{extra_spec}.
- On this page, there are some general supported action descriptions: {supported_actions_desc}, you may pick one from them as exploration if you  don't see a clear path to finish the task.

Return the action in the following json format and **DO NOT** include actions that are targeted on elements which is not on current page (target_element_index is the index of the element in the above element list):
{action_plan_format}
The supported action names are: {supported_actions}
Below are just 2 examples showing the schemas, don't limit yourself to it (you actions should be based on the current page and above context):
You can return the following action plan to complete the task of "Find the login entries":
{action_plan_example_1}
Or you can return the following action plan to complete the task of "Input account name and submit":
{action_plan_example_2}
'''
action_plan_format = json.dumps({
    "finish_task_with_actions": "$true_or_false_to_indicate_whether_the_actions_can_finish_task_in_one_shot",
    "description": "$description_of_the_action_plan",
    "actions": [
        {
            "name": "$action_name",
            "thought": "$thought_for_doing_this_action",
            "spec": "$action_spec",
            "data": "$action_specific_data",
            "target_element_index": 3
        }
    ]
})

action_plan_example_1 = json.dumps({
    "finish_task_in_one_step": False,
    "description": "Swipe right to find more UI entries",
    "actions": [
        {
            "name": "swipe",
            "spec": "right",
            "data": "0.75",
            "thought": "Swipe right as there is limited UI entries shown on the screen",
            "target_element_index": "null"
        }
    ]
})
action_plan_example_2 = json.dumps({
    "finish_task_in_one_step": True,
    "description": "Input account name and submit",
    "actions": [
        {
            "name": "click",
            "spec": "element",
            "thought": "click the input field of the login page to activate the keyboard",
            "target_element_index": 4
        },
        {
            "name": "input",
            "data": "tflplxtest14@outlook.com",
            "thought": "type the account name in the input field",
            "target_element_index": 4
        },
        {
            "name": "key_event",
            "data": "KEYCODE_ENTER",
            "thought": "type enter to submit the account name",
            "target_element_index": 4
        }
    ]
})
action_error_example = json.dumps({
    "finish_task_with_actions": False,
    "actions": [
        {
            "name": "error",
            "thought": "${reason why of no actions can help or suggestion of what to do next}",
        }
    ]
})
instruction_breakdown = '''
You are an agent trained to perform basic tasks on a smartphone. Given a task instruction, you should break it down into 3-5 smaller steps to accomplish the instruction.
Below content delimited by triple backticks is a smartphone UI exploration instruction.
```
{instruction}
```
Please break down the instruction into smaller steps and list them in the following JSON format:
{returned_breakdown_step_format}
'''
instruction_breakdown_parser = '''
You are an agent trained to perform basic tasks on a smartphone. Given a task instruction, you should break it down into 3-5 smaller steps to accomplish the instruction.
Below content delimited by triple backticks is a smartphone UI exploration instruction.
```
{instruction}
```
Please break down the instruction into smaller steps and list them based on the following app context description:
{app_context_description}

{format_instructions}
'''

returned_breakdown_step_format = json.dumps([
    "Step 1 description", "Step 2 description", "Step 3 description"
])

system_message_android_json_reply = "You are a helpful Android automation assistant designed to only output JSON."

system_message_screenshot_vision = '''You are a helpful Android UI comprehension assistant designed to only output JSON.
You are `gpt-4-vision-preview`, the latest OpenAI model that can describe images provided by the user in extreme detail and will never reply that you cannot see the image.
The image doesn't contain any sensitive information or personal data, so please try all your best to help with the request.
'''

memory_prompt = """Relevant pieces of information:
{history}
(You do not need to use these pieces of information if not relevant)
"""

screenshot_vision_prompt = '''The image below is a screenshot of an Android phone.
Please provide the usage category of the page, and provide a list of actions that you think are necessary to explore it.
You may follow the below format, but don't limit yourself to it:
{page_decode_json_example}
The scenario_category could be one of below:
{page_categories}
'''
page_categories_str = """
LoginScreen, AIChatScreen, RegistrationScreen, HomeScreen, SettingsScreen, ProfileScreen, SearchScreen, ProductListScreen, 
ProductDetailsScreen, CartScreen, CheckoutScreen, OrderHistoryScreen, ShoppingHomeScreen, VideoPlaybackScreen, 
MusicPlaybackScreen, NewsFeedScreen, SocialMediaHomeScreen, MessageListScreen, ChatScreen, MapNavigationScreen, 
CalendarScreen, TaskManagerScreen, HealthTrackingScreen, PhotoGalleryScreen, SetupWizardScreen, FeedbackSupportScreen, 
DocumentEditorScreen, ImageEditorScreen, VideoEditorScreen, AudioEditorScreen, PresentationEditorScreen, 
SpreadsheetEditorScreen, PDFReaderScreen, CodeEditorScreen, ProjectManagementScreen, DataVisualizationScreen, 
FileManagerScreen, ContactsScreen, MeetingSchedulerScreen, EmailClientScreen, FlowchartEditorScreen, 
CollaborativeWhiteboardScreen, SomethingWrongScreen, TranslationToolScreen, ShortVideoPlaybackScreen, VideoDiscoveryScreen, 
LiveStreamingScreen, ErrorScreen, FunctionCardsScreen, VideoCreationScreen, FeedScreen, SocialFeedScreen, 
RecommendationScreen, LoadingScreen, CommentScreen, NotificationScreen, AppNotRespondingScreen, FansManagementScreen"""
page_categories = json.dumps([category.strip() for category in page_categories_str.split(",")])
vision_prompt_return_example = json.dumps({
    "scenario_category": "HomeScreen",
    "actions": [
        {
            "action": "click",
            "element_text": "${Text displayed on the target element}",
            "description": "Open a specific app by clicking on its icon (e.g., 'Gallery', 'Themes')."
        },
        {
            "action": "swipe",
            "description": "Change screens or view additional apps/widgets by swiping left or right."
        },
        {
            "action": "long_click",
            "element_text": "${Text displayed on the target element}",
            "description": "Access additional options or move app icons by long clicking on an app."
        },
        {
            "action": "input",
            "data": "${Suggestions for input text}",
            "element_text": "${Text displayed on the target element}",
            "description": "Input text to the search bar to search a city's weather or find apps."
        }
    ]
})
