#!/bin/sh

echo [`date`]

processid=$(ps aux | grep 'master process /usr/sbin/nginx' | grep -v grep | awk '{ print $2}')
if [ -z $processid ]
then
  echo "nginx is OFFLINE, restarting..."
  /usr/sbin/nginx &
else
  echo "nginx is ONLINE, no need to restart"
fi

processid=$(ps aux | grep 'pushgateway-1.4.3.linux-amd64' | grep -v grep | awk '{ print $2}')
if [ -z $processid ]
then
  echo "Pushgateway is OFFLINE, restarting..."
  nohup /opt/pushgateway-1.4.3.linux-amd64/pushgateway --web.config.file=/opt/pushgateway-1.4.3.linux-amd64/pushgateway_auth.yml >> /opt/mount_data/logs/pushgateway/pushgateway_current.log &
else
  echo "Pushgateway is ONLINE, no need to restart"
fi

processid=$(ps aux | grep 'prometheus-2.36.2.linux-amd64' | grep -v grep | awk '{ print $2}')
if [ -z $processid ]
then
  echo "Prometheus is OFFLINE, restarting..."
  nohup /opt/prometheus-2.36.2.linux-amd64/prometheus --config.file=/opt/prometheus-2.36.2.linux-amd64/prometheus.yml --storage.tsdb.path=/opt/mount_data/prometheus_data_backup --storage.tsdb.retention.time=1y >> /opt/mount_data/logs/prometheus/prometheus_current.log &
else
  echo "Prometheus is ONLINE, no need to restart"
fi

processid=$(ps aux | grep 'grafana-server' | grep -v grep | awk '{ print $2}')
if [ -z $processid ]
then
  echo "Grafana is OFFLINE, restarting..."
  cd /opt/grafana-9.0.1/bin/
  nohup ./grafana-server > /dev/null 2>&1 &
else
  echo "Grafana is ONLINE, no need to restart"
fi