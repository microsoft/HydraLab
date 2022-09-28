# run this after running command "./gradlew :agent:bootJar" in project root DIR.
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

# TODO: after we enabled the smart test in Github, we will renable the following layers.
# Install python/pip and smart test dependencies
# This file should be in position if we have run the bootJar task with Gradle.
# ARG PYTHON_REQUIREMENTS=build/requirements.txt
# COPY ${PYTHON_REQUIREMENTS} /requirements.txt
# RUN pip3 install -r /requirements.txt

COPY ${JAR_FILE} /app.jar

EXPOSE 8886
ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=docker"]