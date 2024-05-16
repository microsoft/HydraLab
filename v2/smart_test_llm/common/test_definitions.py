import os
from dotenv import load_dotenv, find_dotenv

_ = load_dotenv(find_dotenv())


class AppInfo:

    def __init__(self, package, name, description):
        self.package = package
        self.name = name
        self.declared_activities = []
        self.description = description


class TestSet(object):
    def __init__(self, app_info: AppInfo, test_identity: dict):
        self.app_info = app_info
        self.test_identity = test_identity

    class TestSetNameError(TypeError):
        pass

    def __setattr__(self, name, value):
        if name in self.__dict__:
            raise self.TestSetNameError("Can't change TestSet.%s" % name)
        self.__dict__[name] = value


launcher_description = '''
Microsoft Launcher provides a new home screen experience that empowers you to be more productive on your Android device. 
Microsoft Launcher is highly customizable, allowing you to organize everything on your phone. 
Your personalized feed makes it easy to view your calendar, to do lists, and more. Sticky Notes on the go. 
The feed page is on the -1 screen on the glance tab page, and you can click the profile button to get to the login page.
The copilot feature is on the -1 screen on the copilot tab page, and you can chat with the copilot to get answers for common senses.
'''
launcher_test_set = TestSet(AppInfo("com.microsoft.launcher", "Microsoft Launcher", launcher_description),
                            test_identity={"account": "tflplxtest14@outlook.com",
                                           "password": os.getenv("MSA_ACCOUNT_PASS")})

launcher_test_set.test_instruction_login_and_use_weather_sticky_note = """
Login the app with the provided Microsoft account: tflplxtest14@outlook.com, 
swipe across the app to explore features, and then open the settings page to change the theme to dark mode,
then open the search bar and search for 'weather', click on the first search result,
then open sticky notes and create a new note with the title 'Test Note' and content 'This is a test note',
and finally, go to home screen and then logout from the app."""

launcher_test_set.test_instruction_login_and_try_copilot = """
Login the app with the provided Microsoft account: tflplxtest14@outlook.com, 
swipe to the -1 feedback and activate the copilot page by signing in with the account if not signed in yet,
then chat with the copilot and ask for the weather, and let the copilot to tell you a joke,
and then generate more questions as subtasks to test the copilot's capability.
There are some phone skills that the copilot can do, please try it out."""

launcher_test_set.test_instruction_login_and_change_setting = """
Login the app with the provided Microsoft account: tflplxtest14@outlook.com on -1 page glance tab,
and go back to home screen and swipe up to test the app drawer,
find the launcher setting page in app drawer and open launcher settings,
change the theme to dark mode, and then go back to home screen.
"""

microsoft_start_description = '''
Microsoft Start app is a mobile news app that helps you stay organized and productive with all your daily activities such as News, Messaging and more. 
Personalize your news feed and stay connected with the latest entertainment, sports, tech and science, lifestyles—even health and fitness trends.
'''
