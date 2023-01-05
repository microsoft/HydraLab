# run this after: ./gradlew.bat :bootJar
FROM mcr.microsoft.com/openjdk/jdk:11-ubuntu

# config files and sh script for running multiple services
ARG STARTUP_FOLDER_PATH=.

ENV TZ="Asia/Shanghai" \
    TIME_ZONE="Asia/Shanghai" \
    ANDROID_HOME=/opt/android-sdk-linux
ENV PATH $PATH:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

RUN \
    ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && \
    echo "{TZ}" > /etc/timezone && \
    apt-get update && \
    apt-get install -y wget && \
    apt-get install -y ffmpeg && \
    mkdir -m 0750 /.android && \
    mkdir -p /hydra/data && \
    wget -qO- "http://dl.google.com/android/android-sdk_r24.3.4-linux.tgz" | tar -zx -C /opt && \
    echo y | android update sdk --no-ui --all --filter platform-tools --force && \
    apt-get install -y curl

ARG CENTER_JAR_FILE=center.jar
ARG AGENT_JAR_FILE=agent.jar
ARG CENTER_INIT_SQLITE=hydra_lab_uber_center_db.sqlite
COPY ${CENTER_JAR_FILE} ${CENTER_JAR_FILE}
COPY ${AGENT_JAR_FILE} ${AGENT_JAR_FILE}
COPY ${STARTUP_FOLDER_PATH}/${CENTER_INIT_SQLITE} /hydra/data/${CENTER_INIT_SQLITE}
COPY ${STARTUP_FOLDER_PATH}/start.sh start.sh

# Connect package with repo HydraLab
LABEL org.opencontainers.image.source=https://github.com/microsoft/HydraLab

EXPOSE 9886
ENTRYPOINT ["sh", "start.sh"]