# Agent Project

The agent project, powered by SQLite as storage, is used to manage devices and run test task on the hosted machine.

Before you run the agent jar, you need to provide the necessary configuration in application.yml or application.properties file.

## Run Agent in Docker

You need to install Docker or other compatible container before executing the below operations.

### Pull Docker Image from GitHub package and run

Pull the docker image:
```bash
docker pull ghcr.io/microsoft/hydra-lab-agent:0.0.2
```

Then you can download the YAML config from the center service Web portal Authentication -> AGENTS. And You can run the docker. If you are using bash terminal:
```bash
# Replace {REPLACE_WITH_YAML_CONFIG_FILE_PATH} with the application.yml file path, for example: /User/xxx/HydraLab/application.yml
# Replace {REPLACE_WITH_HYDRA_DATA_FOLDER_PATH} with the data folder path, for example: /User/xxx/HydraLab/data
docker run --mount type=bind,source={REPLACE_WITH_YAML_CONFIG_FILE_PATH},target=/application.yml \
           -v {REPLACE_WITH_HYDRA_DATA_FOLDER_PATH}:/hydra/data ghcr.io/microsoft/hydra-lab-agent:latest
```

### Build the Docker image locally and run it

Firstly, you need to build the agent.jar by running the below in root dir:

```bash
./gradlew :agent:bootJar
```

Then change working dir to the path of the agent Dockerfile and build the docker image:, and execute the following command using bash:

```bash
docker build -t hydra-lab-agent:local .
```

Then run the docker in container, and if you are using bash on Windows:

```bash
docker run --mount type=bind,source={REPLACE_WITH_YAML_CONFIG_FILE_PATH},target=/application.yml \
           -v {REPLACE_WITH_HYDRA_DATA_FOLDER_PATH}:/hydra/data hydra-lab-agent:local
```

Or using powershell:

```powershell
docker run --mount type=bind,source={REPLACE_WITH_YAML_CONFIG_FILE_PATH},target=/application.yml `
           -v {REPLACE_WITH_HYDRA_DATA_FOLDER_PATH}:/hydra/data hydra-lab-agent:local
```

If you are using Linux, you can map the USB devices leveraging the volume param, so that we could have better support for device connection:

```bash
docker run -v /dev/bus/usb:/dev/bus/usb --mount type=bind,source={REPLACE_WITH_YAML_CONFIG_FILE_PATH},target=/application.yml \
           -v {REPLACE_WITH_HYDRA_DATA_FOLDER_PATH}:/hydra/data hydra-lab-agent:local
```
In this mode, we don't need the port forwarding anymore, and you need to change the active profile to default in [Dockerfile](Dockerfile) to build the image.  The final solution also depends on where the tested app is running.

And if you are using MacOS with an Apple silicon chip, please refer to [this page](https://docs.docker.com/desktop/mac/apple-silicon/), and you may need to append the --platform linux/amd64 to build or run the image.

Please refer to the [application-sample.yml](application-sample.yml) file to understand the standard configuration of the docker agent. You may replace the placeholders listed in the file and rename it for application.

### Docker solution restrictions and dependencies

Currently, the docker solution only support running the test cases on Android connected devices and cases powered by Appium.

Dependencies:
- If you are running the test on Android Devices, please start the ADB server on the docker hosted machine with ```adb start-server```.
- If you want to run the test with Appium framework, please start the Appium server first. If you want to run it on Android you also need to run the command ```adb start-server```.
- Install WinAppDriver before you run Windows app test cases on Appium.

### How to start docker as a service on Windows/MacOS without user login

TBD
