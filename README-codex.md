# README-codex — Ad Click Event Aggregation

## Цель
Сгенерировать репозиторий с сервисами:
- ingestion (опционально mock producer)
- stream-aggregator
- query-service
- инфраструктура (docker-compose/k8s manifests) для локального прогона

## Минимальный tech stack (рекомендуемый)
- Kafka (Redpanda) локально
- Stream: Kafka Streams (Java) или Flink (Java)
- Aggregated DB: ClickHouse или Cassandra (локально проще ClickHouse)
- Raw: S3-minio (опционально) или просто файловый sink

## Репозиторная структура
/apps
  /query-service
  /stream-aggregator
  /event-producer (dev only)
/infra
  docker-compose.yml
  /k8s (опционально)
/docs
  README.md
  c4-context.mmd
  c4-container.mmd
  c4-component-aggregator.mmd
/contracts
  click-event.avsc (или protobuf)
  openapi.yaml

## Контракты
### ClickEvent
- event_id (uuid)
- ad_id (string)
- click_ts (epoch ms)
- user_id (string)
- ip (string)
- country (string)

### Aggregates schema
- ad_id, click_minute, count, filter_id (опционально)

## Tasks (порядок генерации)
1) Создать контракт события (Avro/Proto) + unit tests на сериализацию.
2) Поднять infra локально (Kafka + DB).
3) Реализовать stream-aggregator:
   - consume ClickEvent
   - dedup по event_id (state store TTL)
   - tumbling window 1m: count per (ad_id, minute[, filter])
   - materialize Top-N per minute (или отдельный топик)
   - idempotent write (ключ = ad_id+minute+filter)
4) Реализовать query-service:
   - endpoints aggregated_count, popular_ads
   - чтение из Aggregated DB
5) Нагрузочный dev-генератор событий.
6) Observability:
   - metrics: lag, processed/sec, late events, dedup hits
   - health endpoints

## Нефункциональные проверки
- Exactly-once в локальном режиме: включить processing.guarantee (если Kafka Streams) + транзакции.
- Fault injection: рестарт агрегатора; проверить отсутствие double-count (по возможности).
