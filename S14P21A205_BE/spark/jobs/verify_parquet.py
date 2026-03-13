from pyspark.sql import SparkSession
from pyspark.sql.functions import col, countDistinct, count, sort_array, collect_set

spark = SparkSession.builder.appName("VerifyParquet").getOrCreate()
spark.sparkContext.setLogLevel("ERROR")

errors = []

# 1. POPULATION: 격자별 날짜 × 시간대 완전성 확인
print("\n" + "=" * 60)
print("=== POPULATION 검증 ===")
print("=" * 60)

pop = spark.read.parquet("hdfs://namenode:9000/processed/population/")
pop_total = pop.count()
pop_null = pop.filter(col("SPOP").isNull()).count()

pop_summary = pop.groupBy("CELL_ID").agg(
    countDistinct("YMD").alias("days"),
    countDistinct("TT").alias("time_slots"),
    count("*").alias("rows")
).orderBy("CELL_ID")

print(f"총 행 수: {pop_total}")
print(f"SPOP null: {pop_null}")
print(f"{'CELL_ID':<16} {'일수':>4} {'시간대':>6} {'행수':>6} {'상태':>6}")
print("-" * 46)

for row in pop_summary.collect():
    expected = row["days"] * 24
    status = "✅" if row["time_slots"] == 24 and row["rows"] == expected else "❌"
    if status == "❌":
        errors.append(f"POPULATION {row['CELL_ID']}: {row['rows']}행 (기대 {expected})")
    print(f"{row['CELL_ID']:<16} {row['days']:>4} {row['time_slots']:>6} {row['rows']:>6} {status:>6}")

pop_missing = pop.groupBy("CELL_ID", "YMD").agg(
    countDistinct("TT").alias("hour_count")
).filter(col("hour_count") < 24)

pop_missing_count = pop_missing.count()
if pop_missing_count > 0:
    print(f"\n⚠️  시간대 누락 {pop_missing_count}건:")
    pop_missing.orderBy("CELL_ID", "YMD").show(100, truncate=False)
    errors.append(f"POPULATION: 시간대 누락 {pop_missing_count}건")
else:
    print("\n모든 격자의 모든 날짜에 0~23시 완전 ✅")

if pop_null > 0:
    pop_null_detail = pop.filter(col("SPOP").isNull()) \
        .groupBy("CELL_ID", "YMD").agg(
            count("*").alias("null_count"),
            sort_array(collect_set("TT")).alias("null_hours")
        ).orderBy("CELL_ID", "YMD")
    print(f"\n⚠️  SPOP null 상세:")
    pop_null_detail.show(100, truncate=False)

# 2. NEWS: 날짜별 기사 수 확인
print("\n" + "=" * 60)
print("=== NEWS 검증 ===")
print("=" * 60)

news = spark.read.parquet("hdfs://namenode:9000/processed/news/")
news_total = news.count()
news_null_title = news.filter(col("title").isNull()).count()
news_null_article = news.filter(col("article").isNull()).count()

print(f"총 기사 수: {news_total}")
print(f"title null: {news_null_title}, article null: {news_null_article}")

news_by_date = news.withColumn(
    "date", col("published").cast("date")
).groupBy("date").agg(
    count("*").alias("count")
).orderBy("date")

print(f"{'날짜':<12} {'기사 수':>6}")
print("-" * 20)
for row in news_by_date.collect():
    print(f"{str(row['date']):<12} {row['count']:>6}")

if news_null_title > 0:
    errors.append(f"NEWS: title null {news_null_title}건")
if news_null_article > 0:
    errors.append(f"NEWS: article null {news_null_article}건")

# 3. TRAFFIC: 지점별 날짜 × 시간대 완전성 확인
print("\n" + "=" * 60)
print("=== TRAFFIC 검증 ===")
print("=" * 60)

traffic = spark.read.parquet("hdfs://namenode:9000/processed/traffic/")
traffic_total = traffic.count()
traffic_null = traffic.filter(col("VOL").isNull()).count()

traffic_summary = traffic.groupBy("SPOT_NUM").agg(
    countDistinct("YMD").alias("days"),
    countDistinct("HH").alias("hours"),
    count("*").alias("rows")
).orderBy("SPOT_NUM")

print(f"총 행 수: {traffic_total}")
print(f"VOL null: {traffic_null}")
print(f"{'SPOT_NUM':<10} {'일수':>4} {'시간대':>6} {'행수':>6} {'상태':>6}")
print("-" * 40)

for row in traffic_summary.collect():
    expected = row["days"] * 24
    status = "✅" if row["hours"] == 24 and row["rows"] == expected else "❌"
    if status == "❌":
        errors.append(f"TRAFFIC {row['SPOT_NUM']}: {row['rows']}행 (기대 {expected})")
    print(f"{row['SPOT_NUM']:<10} {row['days']:>4} {row['hours']:>6} {row['rows']:>6} {status:>6}")

traffic_missing = traffic.groupBy("SPOT_NUM", "YMD").agg(
    countDistinct("HH").alias("hour_count")
).filter(col("hour_count") < 24)

traffic_missing_count = traffic_missing.count()
if traffic_missing_count > 0:
    print(f"\n⚠️  시간대 누락 {traffic_missing_count}건:")
    traffic_missing.orderBy("SPOT_NUM", "YMD").show(100, truncate=False)
    errors.append(f"TRAFFIC: 시간대 누락 {traffic_missing_count}건")
else:
    print("\n모든 지점의 모든 날짜에 0~23시 완전 ✅")

if traffic_null > 0:
    traffic_null_detail = traffic.filter(col("VOL").isNull()) \
        .groupBy("SPOT_NUM", "YMD").agg(
            count("*").alias("null_count"),
            sort_array(collect_set("HH")).alias("null_hours")
        ).orderBy("SPOT_NUM", "YMD")
    print(f"\n⚠️  VOL null 상세:")
    traffic_null_detail.show(100, truncate=False)

# 종합 결과
print("\n" + "=" * 60)
print("=== 종합 결과 ===")
print("=" * 60)

print(f"\n{'데이터셋':<12} {'총 행수':>8} {'null':>6}")
print("-" * 30)
print(f"{'POPULATION':<12} {pop_total:>8} {pop_null:>6}")
print(f"{'NEWS':<12} {news_total:>8} {news_null_title:>6}")
print(f"{'TRAFFIC':<12} {traffic_total:>8} {traffic_null:>6}")

if errors:
    print(f"\n❌ 구조 오류: {len(errors)}건")
    for e in errors:
        print(f"  - {e}")
else:
    print(f"\n✅ 전체 구조 검증 통과")

print("=" * 60)

spark.stop()
