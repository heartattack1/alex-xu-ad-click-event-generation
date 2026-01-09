C4Context
title Ad Click Event Aggregation - Context
Person(advertiser, "Advertiser/Analyst", "Запрашивает агрегаты и Top-N")
System(system, "Ad Click Aggregation System", "Агрегирует клики по рекламе near real-time")
System_Ext(adTracker, "Ad Tracker / Log Watcher", "Пишет клики")
System_Ext(mq, "Message Queue", "Kafka/Pulsar")
System_Ext(rawStore, "Raw Store", "Хранение сырых кликов")
System_Ext(aggStore, "Aggregated Store", "Минутные агрегаты/индексы")
System_Ext(dashboard, "Dashboard/BI", "Визуализация метрик")

Rel(adTracker, mq, "Публикует click events")
Rel(mq, system, "Стрим событий")
Rel(system, rawStore, "Пишет raw (опц.)")
Rel(system, aggStore, "Пишет агрегаты")
Rel(advertiser, system, "HTTP API: агрегаты/Top-N")
Rel(system, dashboard, "Экспорт/чтение агрегатов")
