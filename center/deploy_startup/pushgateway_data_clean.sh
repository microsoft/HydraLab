#!/bin/sh

baseurl=localhost:9091
for uri in $(curl -sS $baseurl/api/v1/metrics | jq -r '
  .data[].push_time_seconds.metrics[0] |
  select((now - (.value | tonumber)) > 20) |
  (.labels as $labels | ["job"] | map(.+"/"+$labels[.]) | join("/"))
'); do
  curl -XDELETE $baseurl/metrics/$uri | exit
  echo "[`date`] Deleting job: ${baseurl}/metrics/${uri}" >> /opt/mount_data/logs/pushgateway_scheduled_clean/pushgateway_scheduled_clean.log
done