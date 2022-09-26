# Installation
### 1. Download via pipeline
Go to the [pipeline](https://dlwteam.visualstudio.com/Next/_build?definitionId=739&_a=summary) link to download T2C release. We recommend choosing the latest build with success.

![image.png](https://s2.loli.net/2022/09/16/OfqbkxB2ManUS8g.png)

Click <u>artifact</u> to download Install package.

![image.png](https://s2.loli.net/2022/09/16/xujDLIZG2fJ7QqM.png)

### 2. Environment
before install Taps_to_Cases.exe, please enable your powershell script execution permission:

1. Launch "Powershell" app in your windows.

2. Input "Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy unrestricted" in the command line to unrestrict the execution policy.

Click *Next*. After loading, T2C installation will automatically run an environment configuration script.

![image.png](https://s2.loli.net/2022/09/16/zGJoa1jdeE6XUrt.png)

- choose the folder to download installation packages


 ![image.png](https://s2.loli.net/2022/09/16/ehIox3bJdSBkgPm.png)
- This script will automatically download the dependencies required by T2C, and you just need to select the installed version and press enter.

![image.png](https://s2.loli.net/2022/09/16/HaJ7B1642gw3pWI.png)
- If you have downloaded and installed the dependency, just enter `y` to skip

![image.png](https://s2.loli.net/2022/09/16/O9crvbLNUEFXugz.png)

### 3. Finish
You are now finish the installation.

![image.png](https://s2.loli.net/2022/09/16/NxQ9eiDg4w3FhTX.png)

# Getting Start
### 1. Connect to server
Click the *startSever* button in T2C Recorder, it will pop up a sever alert.

![image.png](https://s2.loli.net/2022/09/16/zGFKra28b3lyjxN.png)

Click *startSever* button in sever alert. If the alert status becomes the following, the server connection succeeded.
Minimize this window.

![image.png](https://s2.loli.net/2022/09/16/DA6PoRg9pFT5tad.png)

### 2. Start session
Select a connected device and an initialized app if needed. Then click the *Start Session* button in the lower right corner.

![image.png](https://s2.loli.net/2022/09/16/pUyoNQaRYiAmu8e.png)

# Record test case
### 1. Start recording
Click *Start Recording* button, T2C will start recording your next actions.

![image.png](https://s2.loli.net/2022/09/16/anwyC1Q9UpZKWLx.png)
### 2. Actions
- Select an element on page you want to operate on
- select action in action panel


![image.png](https://s2.loli.net/2022/09/16/3uymvPnUZfLTKz2.png)

### 3. Tap Detaction for android 
You are able to operate the touch actions on the phone to generate the test case directly.
- The current connected device is an Android device
- Click *Tap to Case* Button
- Supported touch actions
![image.png](https://s2.loli.net/2022/09/16/fzy3p8ek9Qd15mP.png)

### 3. Test case
The recorded test cases will be displayed in the Recorder panel.
You can perform the following operations on the test cases.

- reorder
- delete
- clear all
- action type and attributes editing
- choose case is optional

![image.png](https://s2.loli.net/2022/09/16/vbfwHQGFhCp2Nt1.png)

### 4. Export as Json File
After the test case is recorded, click save case and select the save path and filename.


![image.png](https://s2.loli.net/2022/09/16/PS57ptfMz6QCoVa.png)