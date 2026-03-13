#!/bin/bash
echo "Waiting for HDFS..."
sleep 20

if hdfs dfs -test -d /data/news; then
  echo "Data already exists. Skipping upload."
else
  hdfs dfs -mkdir -p /data
  hdfs dfs -put /local-data/news /data/
  hdfs dfs -put /local-data/population /data/
  hdfs dfs -put /local-data/traffic /data/

  echo "HDFS data upload complete!"
fi

hdfs dfs -ls -R /data/
