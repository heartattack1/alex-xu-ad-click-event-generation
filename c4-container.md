C4Container
title Ad Click Event Aggregation - Containers
Person(user, "Advertiser/Analyst", "Запросы агрегатов")
System_Boundary(s1,"Ad Click Aggregation") {
  Container(api, "Query Service", "HTTP", "aggregated_count, popular_ads")
  Container(agg, "Stream Aggregator", "Flink/Spark/KStreams", "Окна, watermark, dedup")
  Container(dbAgg, "Aggregated DB", "Cassandra/ClickHouse/Druid", "Минутные бакеты + индексы")
  Container(dbRaw, "Raw DB", "S3/HDFS/Cassandra", "Сырые клики для перерасчёта")
  Container(mq, "Message Queue", "Kafka/Pulsar", "Партиции по ad_id")
}
Rel(user, api, "HTTPS")
Rel(api, dbAgg, "Read")
Rel(mq, agg, "Consume")
Rel(agg, dbAgg, "Write aggregates")
Rel(agg, dbRaw, "Write raw/backfill")
