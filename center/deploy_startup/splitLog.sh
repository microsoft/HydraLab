#!/bin/sh

date=`date -d "yesterday" +%Y_%m_%d`
cp /opt/mount_data/logs/prometheus/prometheus_current.log /opt/mount_data/logs/prometheus/$date.log
cat /dev/null > /opt/mount_data/logs/prometheus/prometheus_current.log

cp /opt/mount_data/logs/pushgateway/pushgateway_current.log /opt/mount_data/logs/pushgateway/$date.log
cat /dev/null > /opt/mount_data/logs/pushgateway/pushgateway_current.log

cp /opt/mount_data/logs/restart_service/restart_monitor_service.log /opt/mount_data/logs/restart_service/$date.log
cat /dev/null > /opt/mount_data/logs/restart_service/restart_monitor_service.log

cp /opt/mount_data/logs/pushgateway_scheduled_clean/pushgateway_scheduled_clean.log /opt/mount_data/logs/pushgateway_scheduled_clean/$date.log
cat /dev/null > /opt/mount_data/logs/pushgateway_scheduled_clean/pushgateway_scheduled_clean.log

find /opt/mount_data/logs -mtime +30 -name '*.log' -exec rm -rf {} \;