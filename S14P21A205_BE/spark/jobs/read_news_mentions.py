"""
HDFS의 news_mentions parquet을 읽어서 JSON으로 stdout에 출력한다.
인자: total_days (int) — 랜덤으로 뽑을 날짜 수 (기본 7)

출력 형식 (JSON):
{
  "1": [{"menuName": "떡볶이", "mentionCount": 152}, ...],
  "2": [{"menuName": "햄버거", "mentionCount": 98}, ...],
  ...
}
"""
import json
import sys
import random

from pyspark.sql import SparkSession
from pyspark.sql.functions import col

spark = SparkSession.builder \
    .appName("Read_News_Mentions") \
    .config("spark.sql.session.timeZone", "Asia/Seoul") \
    .getOrCreate()

total_days = int(sys.argv[1]) if len(sys.argv) > 1 else 7

try:
    df = spark.read.parquet("hdfs://namenode:9000/processed/news_mentions/")
except Exception:
    print(json.dumps({}))
    spark.stop()
    sys.exit(0)

distinct_dates = [row["date"] for row in df.select("date").distinct().collect()]

if not distinct_dates:
    print(json.dumps({}))
    spark.stop()
    sys.exit(0)

selected_dates = random.sample(distinct_dates, min(total_days, len(distinct_dates)))
selected_dates.sort()

result = {}
for day_idx, date_val in enumerate(selected_dates, start=1):
    rows = df.filter(col("date") == date_val) \
        .orderBy(col("mention_count").desc()) \
        .collect()
    mentions = [
        {"menuName": row["menu_name"], "mentionCount": int(row["mention_count"])}
        for row in rows
    ]
    result[str(day_idx)] = mentions

print(json.dumps(result, ensure_ascii=False))

spark.stop()
