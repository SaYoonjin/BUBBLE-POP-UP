from pyspark.sql import SparkSession
from pyspark.sql.functions import col, to_timestamp, trim

spark = SparkSession.builder \
    .appName("ETL_News") \
    .getOrCreate()

df = spark.read.csv(
    "hdfs://namenode:9000/data/news/",
    header=True,
    sep="|",
    multiLine=True,
    quote='"',
    escape='"',
    encoding="utf-8",
)

df_clean = df.select(
    trim(col("title")).alias("title"),
    to_timestamp(trim(col("published")), "yyyy-MM-dd HH:mm:ss").alias("published"),
    trim(col("article")).alias("article"),
)

df_clean = df_clean.filter(col("title").isNotNull() & col("article").isNotNull())

df_clean.write.mode("overwrite").parquet(
    "hdfs://namenode:9000/processed/news/"
)

print(f"News ETL complete. Rows: {df_clean.count()}")
spark.stop()
