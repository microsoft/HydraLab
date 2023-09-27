import os
import platform
import subprocess
import time


class AppiumLauncher:

    def __init__(self, port):
        self.port = port
        self.system = platform.system()
        self.start_appium()

    def terminate(self):
        self.process.terminate()
        time.sleep(4.0)

    def start_appium(self):
        if self.system == 'Windows':
            self.process = subprocess.Popen(['node.exe', os.getenv('APPIUM_BINARY_PATH'), '-p', f'{self.port}', '--log-level', 'error:error'],
                                            creationflags=0x00000008, shell=True)
        else:
            self.process = subprocess.Popen(['node', os.getenv('APPIUM_BINARY_PATH'), '-p', f'{self.port}', '--log-level', 'error:error'])
        os.system('adb start-server')
        time.sleep(20.0)

    def restart_appium(self):
        self.terminate()
        self.start_appium()
