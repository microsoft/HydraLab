# Before you build this docker image,
# please make sure you run the command:
# "./gradlew :agent:bootJar"
# in project root DIR to generate the JAR_FILE (build/libs/agent.jar).
FROM mcr.microsoft.com/openjdk/jdk:11-ubuntu
ARG JAR_FILE=build/libs/agent.jar

ENV TZ="Asia/Shanghai" \
    TIME_ZONE="Asia/Shanghai" \
    ANDROID_HOME=/opt/android-sdk-linux
ENV PATH $PATH:${ANDROID_HOME}/tools:$ANDROID_HOME/platform-tools

RUN \
    ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && \
    echo "{TZ}" > /etc/timezone && \
    apt-get update && \
    apt-get install -y wget && \
    apt-get install -y ffmpeg && \
    apt-get install -y python3.8 && \
    apt-get install -y python3-pip && \
    mkdir -m 0750 /.android && \
    mkdir /hydra && \
    wget -qO- "http://dl.google.com/android/android-sdk_r24.3.4-linux.tgz" | tar -zx -C /opt && \
    echo y | android update sdk --no-ui --all --filter platform-tools --force

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