from pyspark.sql import SparkSession
from pyspark.sql.functions import col, length, substring, count, sum as spark_sum

spark = SparkSession.builder.appName("check_news_parquet").getOrCreate()
spark.sparkContext.setLogLevel("ERROR")

print("\n" + "=" * 60)
print("=== [1차 정제] /processed/news/ Parquet ===")
print("=" * 60)

try:
    df_news = spark.read.parquet("hdfs://namenode:9000/processed/news/")
    total = df_news.count()
    print(f"총 {total}건\n")

    print("--- 스키마 ---")
    df_news.printSchema()

    print("--- title + published ---")
    df_news.select("title", "published").show(30, truncate=60)

    print("--- article 앞 100자 + 길이 ---")
    df_news.select(
        substring("title", 1, 40).alias("title"),
        substring("article", 1, 100).alias("article_preview"),
        length("article").alias("len"),
    ).show(30, truncate=False)

    print("--- 날짜별 기사 수 ---")
    from pyspark.sql.functions import to_date
    df_news.withColumn("date", to_date(col("published"))) \
        .groupBy("date").count() \
        .orderBy("date") \
        .show(30, truncate=False)

except Exception as e:
    print(f"[ERROR] /processed/news/ 읽기 실패: {e}")

print("\n" + "=" * 60)
print("=== [2차 정제] /processed/news_mentions/ Parquet ===")
print("=" * 60)

try:
    df_mentions = spark.read.parquet("hdfs://namenode:9000/processed/news_mentions/")
    total = df_mentions.count()
    print(f"총 {total}건\n")

    print("--- 스키마 ---")
    df_mentions.printSchema()

    print("--- 전체 데이터 (날짜 × 메뉴별 언급량) ---")
    df_mentions.orderBy("date", "menu_name").show(100, truncate=False)

    print("--- 날짜별 총 언급량 ---")
    df_mentions.groupBy("date") \
        .agg(spark_sum("mention_count").alias("total_mentions")) \
        .orderBy("date") \
        .show(30, truncate=False)

    print("--- 메뉴별 총 언급량 ---")
    df_mentions.groupBy("menu_name") \
        .agg(spark_sum("mention_count").alias("total_mentions")) \
        .orderBy(col("total_mentions").desc()) \
        .show(30, truncate=False)

    print("--- 날짜 수 / 메뉴 수 ---")
    distinct_dates = df_mentions.select("date").distinct().count()
    distinct_menus = df_mentions.select("menu_name").distinct().count()
    print(f"  날짜 수: {distinct_dates}")
    print(f"  메뉴 수: {distinct_menus}")

except Exception as e:
    print(f"[ERROR] /processed/news_mentions/ 읽기 실패: {e}")

print("\n" + "=" * 60)
print("검증 완료!")
print("=" * 60)

spark.stop()
