if ${GRAFANA_ENABLED}; then
  cd /opt/grafana-9.0.1/bin/
  nohup ./grafana-server &
fi

if ${PUSHGATEWAY_ENABLED}; then
  nohup /opt/pushgateway-1.4.3.linux-amd64/pushgateway --web.config.file=/opt/pushgateway-1.4.3.linux-amd64/pushgateway_auth.yml >> /opt/mount_data/logs/pushgateway/pushgateway_current.log &
  nohup /opt/prometheus-2.36.2.linux-amd64/prometheus --config.file=/opt/prometheus-2.36.2.linux-amd64/prometheus.yml --storage.tsdb.path=/opt/mount_data/prometheus_data --storage.tsdb.retention.time=1y >> /opt/mount_data/logs/prometheus/prometheus_current.log &
fi

/usr/sbin/nginx &
/usr/sbin/sshd &
/etc/init.d/cron start &

cd /
java -jar /app.jar --spring.profiles.active=${CENTER_APP_PROFILE}