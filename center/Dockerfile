# run this after: ./gradlew.bat :bootJar
FROM mcr.microsoft.com/openjdk/jdk:11-ubuntu

ARG JAR_FILE=center.jar
COPY ${JAR_FILE} app.jar

# config files and sh script for running multiple services
ARG STARTUP_FOLDER_PATH=./deploy_startup

ENV TZ="Asia/Shanghai" \
TIME_ZONE="Asia/Shanghai"

RUN \
    ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && \
    echo "{TZ}" > /etc/timezone && \
    apt-get update && \
    apt-get install -y nginx && \
    apt-get install -y curl && \
    apt-get install -y jq && \
    apt-get install -y cron && \
    apt-get install -y vim && \
    apt-get install -y sysstat

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

COPY ${STARTUP_FOLDER_PATH}/pushgateway_data_clean.sh /opt/
COPY ${STARTUP_FOLDER_PATH}/prometheus.yml /opt/prometheus-2.36.2.linux-amd64/
COPY ${STARTUP_FOLDER_PATH}/custom.ini /opt/grafana-9.0.1/conf/
COPY ${STARTUP_FOLDER_PATH}/defaults.ini /opt/grafana-9.0.1/conf/
COPY ${STARTUP_FOLDER_PATH}/alert_notification.html /opt/grafana-9.0.1/public/emails/ng_alert_notification.html
COPY ${STARTUP_FOLDER_PATH}/nginx.conf /etc/nginx/
COPY ${STARTUP_FOLDER_PATH}/restart_monitor_service.sh restart_monitor_service.sh
COPY ${STARTUP_FOLDER_PATH}/split_log.sh split_log.sh
COPY ${STARTUP_FOLDER_PATH}/start.sh start.sh

# Add config of scheduled cleaning pushgateway outdated data, run script every 1 minute
RUN echo '*/1 * * * *  ( sh /opt/pushgateway_data_clean.sh & ) ' >> /etc/crontab \
# Add restart logic by triggering shell script every 1 minute
    && echo '*/1 * * * *  ( sh /restart_monitor_service.sh >> /opt/mount_data/logs/restart_service/restart_monitor_service.log ) ' >> /etc/crontab \
    # Add log spliting logic by triggering shell script every day
    && echo '0 0 * * *  ( sh /split_log.sh ) ' >> /etc/crontab \
    && crontab -u root /etc/crontab

# Open port 2222 for SSH access, 80 for default app service
EXPOSE 80
ENTRYPOINT ["sh", "start.sh"]