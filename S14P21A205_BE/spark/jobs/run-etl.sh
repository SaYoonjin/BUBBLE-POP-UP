#!/bin/bash
echo "Starting ETL jobs..."

if curl -sf "http://namenode:9870/webhdfs/v1/processed/population/_SUCCESS?op=GETFILESTATUS" > /dev/null 2>&1; then
  echo "Processed data already exists. Skipping ETL."
  exit 0
fi

/spark/bin/spark-submit --master spark://spark-master:7077 /opt/spark-jobs/etl_population.py && \
/spark/bin/spark-submit --master spark://spark-master:7077 /opt/spark-jobs/etl_news.py && \
/spark/bin/spark-submit --master spark://spark-master:7077 /opt/spark-jobs/etl_news_score.py && \
/spark/bin/spark-submit --master spark://spark-master:7077 /opt/spark-jobs/etl_traffic.py

echo "All ETL jobs complete!"
