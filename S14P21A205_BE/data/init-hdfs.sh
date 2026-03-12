#!/bin/bash
hdfs dfs -mkdir -p /data
hdfs dfs -put /local-data/news /data/
hdfs dfs -put /local-data/population /data/
hdfs dfs -put /local-data/traffic /data/

echo "HDFS data upload complete!"
hdfs dfs -ls -R /data/
