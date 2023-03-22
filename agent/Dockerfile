# Before you build this docker image,
# please make sure you run the command:
# "./gradlew :agent:bootJar"
# in project root DIR to generate the JAR_FILE (agent/build/libs/agent.jar).
FROM mcr.microsoft.com/openjdk/jdk:11-ubuntu
ARG JAR_FILE=build/libs/agent.jar

ENV TZ="Asia/Shanghai" \
    TIME_ZONE="Asia/Shanghai" \
    ANDROID_HOME=/opt/android-sdk-linux \
    ANDROID_SDK_ROOT=/opt/android-sdk-linux
ENV PATH $PATH:${ANDROID_HOME}/tools:${ANDROID_HOME}/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools/bin

RUN \
    ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && \
    echo "{TZ}" > /etc/timezone && \
    apt-get update && \
    apt-get install -y wget && \
    apt-get install -y ffmpeg && \
    apt-get install -y python3.8 && \
    apt-get install -y python3-pip && \
    mkdir -m 0750 /.android && \
    mkdir /hydra

# set up environment for emulator, a example ref: https://andresand.medium.com/android-emulator-on-docker-container-f20c49b129ef
# We attempt to start an Android emulator in the docker container so that when the agent is started, we will have a initial Android emulator phone available.
RUN \
    apt-get install -y unzip libnss3-dev libxcomposite-dev libxdamage1 && \
    # Accepted values are: [add-on, doc, extra, platform, platform-tool, sample, source, system-image, tool]
    mkdir -p /opt/android-sdk-linux/cmdline-tools && \
    # https://developer.android.com/studio#command-tools
    # https://developer.android.google.cn/studio/command-line#tools-sdk \
    wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip commandlinetools-linux-9477386_latest.zip && \
    # Either specify it explicitly with --sdk_root= or move this package into its expected location: <sdk>/cmdline-tools/latest/
    mv cmdline-tools /opt/android-sdk-linux/cmdline-tools/latest

RUN \
    ls -al /opt/android-sdk-linux/cmdline-tools/latest && \
    echo "y" | sdkmanager --install "platform-tools" "platforms;android-24" "emulator" "platforms;android-33" "platforms;android-30" && \
    echo "y" | sdkmanager --install "system-images;android-30;default;armeabi-v7a" && \
    # echo "y" | sdkmanager --licenses && \
    # https://developer.android.com/studio/command-line/avdmanager
    echo "no" | avdmanager create avd -n emuTestNexus -k "system-images;android-30;default;armeabi-v7a" -d "Pixel 5" -f && \
    # echo y | sdkmanager "system-images;android-24;default;armeabi-v7a"  --sdk_root=/opt/android-sdk-linux  && \
    # The binary emulator-headless is now retired. Headless builds of the engine are now launched via emulator -no-window, thus unifying the previously separate (but similar) paths.
    emulator -avd emuTestNexus -noaudio -no-boot-anim -gpu off -no-window

# TODO: after we enable the smart test in Github, we will reactivate the following layers.
# Install python/pip and smart test dependencies
# This file should be in position if we have run the bootJar task with Gradle.
# ARG PYTHON_REQUIREMENTS=build/requirements.txt
# COPY ${PYTHON_REQUIREMENTS} /requirements.txt
# RUN pip3 install -r /requirements.txt

COPY ${JAR_FILE} /app.jar

# Official doc about connecting package with repo: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#labelling-container-images
LABEL org.opencontainers.image.source=https://github.com/microsoft/HydraLab

ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=docker"]