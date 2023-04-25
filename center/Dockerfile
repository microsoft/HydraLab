# run this after: ./gradlew.bat :bootJar
FROM mcr.microsoft.com/openjdk/jdk:11-ubuntu

ARG JAR_FILE=center.jar
COPY ${JAR_FILE} app.jar

# config files and sh script for running multiple services
ARG STARTUP_FOLDER_PATH=./deploy_startup
# config env, can be override by --build-arg through "docker run" command.
ARG ARG_CENTER_PROFILE=release
ARG ARG_GRAFANA_ENABLED=true
ARG ARG_PUSHGATEWAY_ENABLED=true
# Sqlite file under the same directory, storing pre-defined data for center-image
ARG CENTER_INIT_SQLITE=hydra_lab_center_docker_db.sqlite

ENV TZ="Asia/Shanghai" \
    TIME_ZONE="Asia/Shanghai" \
    CENTER_APP_PROFILE=${ARG_CENTER_PROFILE} \
    GRAFANA_ENABLED=${ARG_GRAFANA_ENABLED} \
    PUSHGATEWAY_ENABLED=${ARG_PUSHGATEWAY_ENABLED}

RUN \
    ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && \
    echo "{TZ}" > /etc/timezone && \
    apt-get update && \
    apt-get install -y nginx && \
    apt-get install -y curl && \
    apt-get install -y jq && \
    apt-get install -y cron && \
    apt-get install -y vim && \
    apt-get install -y sysstat && \
    mkdir -p /hydra/data

COPY ${STARTUP_FOLDER_PATH}/prometheus-2.36.2.linux-amd64.tar.gz /opt/
COPY ${STARTUP_FOLDER_PATH}/pushgateway-1.4.3.linux-amd64.tar.gz /opt/
COPY ${STARTUP_FOLDER_PATH}/grafana-enterprise-9.0.1.linux-amd64.tar.gz /opt/

RUN cd /opt/ && \
    tar -zxvf prometheus-2.36.2.linux-amd64.tar.gz && \
    tar -zxvf pushgateway-1.4.3.linux-amd64.tar.gz && \
    tar -zxvf grafana-enterprise-9.0.1.linux-amd64.tar.gz && \
    rm prometheus-2.36.2.linux-amd64.tar.gz && \
    rm pushgateway-1.4.3.linux-amd64.tar.gz && \
    rm grafana-enterprise-9.0.1.linux-amd64.tar.gz

COPY ${STARTUP_FOLDER_PATH}/pushgatewayDataClean.sh /opt/
COPY ${STARTUP_FOLDER_PATH}/prometheus.yml /opt/prometheus-2.36.2.linux-amd64/
COPY ${STARTUP_FOLDER_PATH}/pushgateway_auth.yml /opt/pushgateway-1.4.3.linux-amd64/
COPY ${STARTUP_FOLDER_PATH}/custom.ini /opt/grafana-9.0.1/conf/
COPY ${STARTUP_FOLDER_PATH}/defaults.ini /opt/grafana-9.0.1/conf/
COPY ${STARTUP_FOLDER_PATH}/alert_notification.html /opt/grafana-9.0.1/public/emails/ng_alert_notification.html
COPY ${STARTUP_FOLDER_PATH}/nginx.conf /etc/nginx/
COPY ${STARTUP_FOLDER_PATH}/restartMonitorService.sh restartMonitorService.sh
COPY ${STARTUP_FOLDER_PATH}/splitLog.sh splitLog.sh
COPY ${STARTUP_FOLDER_PATH}/start.sh start.sh
#COPY uber_image/${CENTER_INIT_SQLITE} /hydra/data/${CENTER_INIT_SQLITE}
#COPY uber_image/record_release.apk /storage/local/pkgstore/2023/04/23/hydra_lab_android_client_release.apk
#COPY uber_image/record_androidTest.apk /storage/local/pkgstore/2023/04/23/hydra_lab_android_client_androidTest.apk

# Add config of scheduled cleaning pushgateway outdated data, run script every 1 minute
RUN echo '*/1 * * * *  ( sh /opt/pushgatewayDataClean.sh & ) ' >> /etc/crontab \
# Add restart logic by triggering shell script every 1 minute
    && echo '*/1 * * * *  ( sh /restartMonitorService.sh >> /opt/mount_data/logs/restart_service/restart_monitor_service.log ) ' >> /etc/crontab \
    # Add log spliting logic by triggering shell script every day
    && echo '0 0 * * *  ( sh /splitLog.sh ) ' >> /etc/crontab \
    && crontab -u root /etc/crontab

# Official doc about connecting package with repo: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#labelling-container-images
LABEL org.opencontainers.image.source=https://github.com/microsoft/HydraLab

# Open port 2222 for SSH access, 80 for default app service
EXPOSE 80
ENTRYPOINT ["sh", "start.sh"]