class Const(object):
    class ConstError(TypeError):
        pass

    class ConstCaseError(ConstError):
        pass

    def __setattr__(self, name, value):
        if name in self.__dict__:
            raise self.ConstError("Can't change const.%s" % name)
        if not name.isupper() and name != 'iOS':
            raise self.ConstCaseError('const name "%s" is not all supercase' % name)

        self.__dict__[name] = value


action = Const()
platform = Const()
# environment = Const()
adb_command = Const()
# patten = Const()
app_state = Const()
screen_bert = Const()
android_widget = Const()
# windows_widget = Const()
identifiers = Const()
llm_prompt = Const()
#
platform.ANDROID = 'ANDROID'
platform.iOS = 'iOS'
platform.WINDOWS = 'WINDOWS'
platform.CROSS_DEVICES = 'CROSS_DEVICES'
#
# environment.OBSERVATION_SPACE = 768
# environment.ELEMENT_SPACE = 768
# environment.EXTRA_ACTION_SPACE = 1
# environment.ACTION_SPACE = environment.EXTRA_ACTION_SPACE + environment.ELEMENT_SPACE

adb_command.COLLECT_LOG = 'logcat -d Finsky:S MirrorLink:S UiTest:D *:W'
adb_command.CLEAR_LOG = 'logcat -c'

# patten.CRASH_BEGINNINGS = ["beginning of crash", "AndroidRuntime: FATAL EXCEPTION"]
# patten.CRASH_TAGS = ["E", "F"]
# patten.LAUNCHER = ".launcher."

app_state.NOT_INSTALL = 0
app_state.NOT_RUNNING = 1
app_state.RUNNING_IN_BACKGROUND_SUSPENDED = 2
app_state.RUNNING_IN_BACKGROUND = 3
app_state.RUNNING_IN_FOREGROUND = 4
app_state.RUNNING = [2, 3, 4]

system_packages = [
    "com.android.systemui",
    "com.android.settings",
    "com.android.packageinstaller",
    "com.android.providers.settings",
    "com.android.providers.telephony",
    "com.android.providers.contacts",
    "com.android.providers.media",
    "com.android.providers.downloads",
    "com.android.phone",
    "com.android.mms"
]

keyboard_packages = [
    "com.google.android.inputmethod",
    "com.samsung.android.honeyboard",
    "com.sec.android.inputmethod",
    "com.htc.sense.ime",
    "com.touchtype.swiftkey"
]

screen_bert.APP_CLASSES = [
    "Art & Design",
    "Auto & Vehicles",
    "Beauty",
    "Books & Reference",
    "Business",
    "Comics",
    "Communication",
    "Dating",
    "Education",
    "Entertainment",
    "Events",
    "Finance",
    "Food & Drink",
    "Health & Fitness",
    "House & Home",
    "Lifestyle",
    "Maps & Navigation",
    "Medical",
    "Music & Audio",
    "News & Magazines",
    "Parenting",
    "Shopping",
    "Social",
    "Sports",
    "Travel & Local",
    "Video Players & Editors",
    "Weather"
]
screen_bert.FRAME_TOPICS = [
    'Bare',
    'calculator',
    'Camera',
    'Chat',
    'Editor',
    'Form',
    'Gallery',
    'List',
    'Login',
    'Maps',
    'Media Player',
    'Menu',
    'Modal',
    'News',
    'Other',
    'Profile',
    'Search',
    'Settings',
    'Terms',
    'Tutorial'
]
#
android_widget.BUTTON = [
    'android.widget.Button',
    'android.widget.CheckBox',
    'android.widget.CompoundButton',
    'android.widget.ImageButton',
    'android.widget.RadioButton',
    'android.widget.Switch',
    'android.widget.ToggleButton'
]
android_widget.EDIT_TEXT = [
    'android.widget.EditText',
    'android.widget.AutoCompleteTextView'
]
android_widget.RECYCLER_VIEW = [
    'android.support.v7.widget.RecyclerView',
    'androidx.recyclerview.widget.RecyclerView'
]
android_widget.TEXT_VIEW = [
    'android.widget.TextView',
    'android.widget.ImageView'
]
android_widget.IMAGE_VIEW = ['android.widget.ImageView']
android_widget.WEB_VIEW = ["android.webkit.WebView"]
android_widget.CONTAINER_CLASS = [
    "android.widget.FrameLayout",
    "android.widget.LinearLayout",
    "android.widget.RelativeLayout",
    "android.view.ViewGroup"
]
android_widget.INTERACTABLE = android_widget.EDIT_TEXT + android_widget.BUTTON + android_widget.WEB_VIEW + android_widget.RECYCLER_VIEW
#
identifiers.USERNAME = ['user', 'username', 'email', 'e-mail', 'phone']
#
# windows_widget.BUTTON = ['Button']
# windows_widget.EDIT_TEXT = ['Edit']
# windows_widget.TEXT = ['Text']
# windows_widget.IMAGE = ['Image']
# windows_widget.LIST = ["List"]
# windows_widget.LIST_ITEM = ["ListItem"]
#
# windows_widget.INTERACTABLE = windows_widget.BUTTON + windows_widget.EDIT_TEXT + windows_widget.LIST_ITEM

# LLM PROMPT for ranking elements during action selection
# 1. Select single element for action performing
llm_prompt.ELEMENT_RANKING_TEMPLATE_0_1 = """
    The information in the triple quotes are a list of Android elements of applications, in the format of "<{element_type_name} {element_attribute_1}={value_1} {element_attribute_2}={value_2} {element_attribute_3}={value_3} ... {element_attribute_N}={value_N} />" with a series of attribute-value pairs annotating it's unique.
    When it comes to a UI test for a application with the following elements in a same page, and the test has limited action steps, it needs to perform actions based on the priority of the importance of related elements. Which element would you choose to perform action on, and what's the reason?
    Provide the answer including only the element info and the related reason.

    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 2. Rank the given elements with the element type and resource-id
llm_prompt.ELEMENT_RANKING_TEMPLATE_0_2 = """
    The information in the triple quotes are a list of Android elements of applications, in the format of "<{element_type_name} {element_attribute_1}={value_1} {element_attribute_2}={value_2} ... {element_attribute_N}={value_N} />" with a series of attribute-value pairs annotating it's unique.
    When it comes to a UI test for a application with the following elements in a same page, and the test has limited action steps, it needs to perform actions based on the priority of the importance of related elements. Rank the elements based on priority that you would perform action on?
    Answer should only include a JSON array, including the rank of given elements. For each element in JSON array, output only the element info (element type, resource-id, text, xpath) and the ranking reason. For element info, if any attribute within it has no or empty value, hide the attribute from output. Don't output any text other than the JSON.
    
    Element information:
    
    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 3. Rank the given elements with the element type and resource-id
llm_prompt.ELEMENT_RANKING_TEMPLATE_0_3 = """
    The information in the triple quotes are a list of Android elements of applications, in the format of "<attribute_1: value_1, attribute_2: value_2, ... attribute_N: value_N />" with a series of attribute-value pairs annotating it's unique.
    When it comes to a UI test for a application with the following elements in a same page, and the test has limited action steps, it needs to perform actions based on the importance priority of the elements in normal use. Rank the elements based on priority that you would perform action on.
    
    Answer should only include a JSON array, including the rank of given elements. For each element in JSON array, output only the element info (xpath, element type, resource-id, text) and the ranking reason. For element info, if any attribute within it has no or empty value, hide the attribute from output. Don't output any text other than the JSON.
    
    Element information:
    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 4. Version 2.0 prompt with ranking list
# Change: Add "Ranking rule" explicitly.
# Result analysis: Result is rather ridiculous, as the main change from last version is ranking rule explicitly definition, this could be the major effect on the result.
llm_prompt.ELEMENT_RANKING_TEMPLATE_0_4 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique.
    Please rank the elements based on the given information and ranking rule, and output the reasons why the elements are ranked in this order.
    
    Answer should only include a JSON array, including the ranked elements. For each element in the array, output only the element info as (xpath, element_type, element_class) and the ranking reason as (ranking_reason). Don't output any text other than the JSON.
    
    <<<Ranking rule>>>
    When it comes to "UI test" for an application with the following elements in a same page, the tests always have limited action steps. Hence actions must be performed based on the importance priority of the elements the same as the normal use.
    
    <<<Element information>>>
    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 5. not explicitly target at the ranking rule
# Change: Remove "Ranking rule" section from PROMPT.

llm_prompt.ELEMENT_RANKING_TEMPLATE_1_1 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique.
    When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans.
    The following element information is in the same Android application page, and is provided for UI test. Please rank the elements based on the given information, and output why the elements are in this order.
    
    Answer should only include a JSON array, including the ranked elements. For each element in the array, output only the element xpath as (xpath) and the ranking reason as (reason). Don't output any text other than the JSON.
    
    <<<Element information>>>
    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 6. format output more feature of elements for LLM, provide more featurable data for the decision maker.
# Change: Add several extra attributes to the element attribute list, to help enrich the ranking evidence.

llm_prompt.ELEMENT_RANKING_TEMPLATE_1_2 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attributes with their values, here's the introduction of some attributes:
    - xpath: the xpath value that is used to locate the element within the page.
    - element_type: type of element annotating which interactive behaviour is the most suitable for it.
    - element_class: name of the Android element class.
    - element_size: the size of element within the phone screen.
    - text: text value that displays on the page.
    
    When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans. 
    The following element information is in the same Android application page, and is provided for UI test.
    Please rank the elements based on as many as the given attributes of them, and output the reason that the elements are in this order containing all considered attributes.
    
    Answer should only include a JSON array, including the ranked elements. For each element in the array, output only the element xpath as (xpath) and the ranking reason as (reason). Ranking reason should illustrate what attributes are referred to when decision is made. Don't output any text other than the JSON.
    
    <<<Element information>>>
    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 7. Change: specify rules and attribute explanations.
# Result: output is limited in the scope of attributes and rules, reason contains no info other than the attributes/rules. Result worse than 6.

llm_prompt.ELEMENT_RANKING_TEMPLATE_1_3 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attributes with their values, here's the introduction of some attributes:
    - xpath: the xpath value that is used to locate the element within the page.
    - element_size: the size of element within the phone screen.
    - text: text value that displays on the page.    
    - element_class: name of the Android element class.
    - element_type: type of element annotating which interactive behaviour is the most suitable for it.
        - Value "input" means this element is able to receive INPUT.
        - Value "clickable" means clicking on this element can trigger a page switching.
    - resource_id: the unique ID which is used for identification for the element.
    
    When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans. 
    The following element information is in the same Android application page, and is provided for UI test. Please rank the elements to determine the order to be interacted wtih based on the given information, and output why in this order.
    When you determine the order of a list of elements to interact, please consider the following UX design principles:
    - The element is interactive explicitly.
    - From the element attributes, it can be confirmed that the element is of high priority to interact.
    - Bigger element have a larger chance to lead to a new extensive UI path.
    - Any other experience from interactions between human-being and applications.
    
    Answer should only include a JSON array, including the ranked elements. For each element in the array, output only the element xpath as (xpath) and the ranking reason as (reason). Ranking reason should illustrate what attributes are referred to when decision is made. Don't output any text other than the JSON.
    
    <<<Element information>>>
    \"\"\"{element_input}\"\"\"
    
    {format_instructions}
"""

# 8. Based on 5.
# Change: Remove xpath from element list as LangChain OUTPUT PARSER will always return the original sequence.

llm_prompt.ELEMENT_RANKING_TEMPLATE_1_4 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique.      
    When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans.      
    The following element information is in the same Android application page, and is provided for UI test. Please rank the elements based on the given attributes, and output why the elements are in this order.
    
    Answer should only include an array of ranked elements. For each element in the array, output only the element id as (id) and the ranking reason as (reason). Don't output any other text.      

    <<<Element information>>>
    \"\"\"{element_input}\"\"\"

    {format_instructions}
"""

# 9. Change: add page bounds and all element bounds.
# Result: not working as expected. Reasons include the element's position cannot be recognized correctly.

llm_prompt.ELEMENT_RANKING_TEMPLATE_1_5 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique. 
    When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans.      
    The following elements are provided for UI test, and are in the same Android application page. The page size is "{page_size}". Please rank the elements based on the given attributes, and output why the elements are in this order.

    Answer should only include an array of ranked elements. For each element in the array, output only the element id as (id) and the ranking reason as (reason). Don't output any other text.      

    <<<Element information>>>
    \"\"\"    
    {element_input}
    \"\"\"

    {format_instructions}
"""

# 10. Change: add PROMPT for ranking BACK elements at the bottom of the page.\
llm_prompt.ELEMENT_RANKING_TEMPLATE_1_6 = """
    The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique.      
    When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans.      
    The following element information is in the same Android application page, and is provided for UI test. Please rank the elements based on the given attributes, and output why the elements are in this order.
    
    Below are specific rules for ranking (priority 1. > 2.):
    1. Any element with evidence proving it is used to <sign out an account> should always be ranked at the bottom of list.
    2. Any element with evidence proving it is used to <get BACK to previous page> should always be ranked at the bottom of list.

    Answer should only include an array of ranked elements. For each element in the array, output only the element id as (id) and the ranking reason as (reason). Don't output any other text.      

    <<<Element information>>>
    \"\"\"{element_input}\"\"\"

    {format_instructions}
"""

# 11. Change: paraphrase hints and rules to make responses stable and less change.
llm_prompt.ELEMENT_RANKING_TEMPLATE = """
The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique.      
When it comes to UI tests, the tests always have limited times to perform actions on an Android application due to time cost. Hence actions must be performed based on the importance priority of the target elements the same as normal use of humans.      
The following element information is in the same Android application page, and is provided for UI test.

Task: rank the given elements and give the reason why this order.

Below are specific rules for ranking (meet the rules base on the sequence):
- If an element has any attribute proving it is used to <sign in an account>, rank it at the top of list.
- Rank the rest elements based on priority if a human would interact with it according to its attributes.
- If an element has any attribute proving it is used to <sign out an account>, rank it at the bottom of list.
- If an element has any attribute proving it is used to <BACK to previous page>, rank it at the bottom of list.

Answer should only include an array of ranked elements. For each element in the array, output only the element id as (id) and the ranking reason as (reason). Don't output any other text.      

<<<Element information>>>
\"\"\"{element_input}\"\"\"

{format_instructions}
"""

# TODO: LOGIN PAGE RECOGNITION
llm_prompt.RECOGNIZE_LOGIN_TEMPLATE = """
The information in the triple quotes is a list of Android elements of applications, in the format of `<attribute_1="value_1" attribute_2="value_2" ... attribute_N="value_N" />` with a series of attribute-value pairs annotating it's unique.       
Please classify the page containing these elements, and reply with whether this page is a LOGIN page, and the reason. If not, reply with the page type and related reason.

<<<Element information>>>
\"\"\"
{element_input}
\"\"\"
"""
