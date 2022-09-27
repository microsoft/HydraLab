cd /opt/grafana-9.0.1/conf/
sed -i -e "s|env:MAIL_ADDRESS|${MAIL_ADDRESS}|g" -e "s|env:MAIL_PASS|${MAIL_PASS}|g" ./custom.ini
sed -i -e "s|env:HYDRA_LAB_HOST|${HYDRA_LAB_HOST}|g" ./defaults.ini
cd /opt/grafana-9.0.1/bin/
nohup ./grafana-server &

nohup /opt/pushgateway-1.4.3.linux-amd64/pushgateway >> /opt/mount_data/logs/pushgateway/pushgateway_current.log &
nohup /opt/prometheus-2.36.2.linux-amd64/prometheus --config.file=/opt/prometheus-2.36.2.linux-amd64/prometheus.yml --storage.tsdb.path=/opt/mount_data/prometheus_data --storage.tsdb.retention.time=1y >> /opt/mount_data/logs/prometheus/prometheus_current.log &
/usr/sbin/nginx &
/etc/init.d/cron start &
cd /
java -jar /app.jar --spring.profiles.active=release