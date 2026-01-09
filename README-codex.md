# README-codex — Ad Click Event Aggregation (Spring Boot, Java, Gradle)

## Цель
Сгенерировать мульти-модульный Gradle репозиторий на Java/Spring Boot для системы агрегации кликов:
- event-producer (dev-only)
- stream-aggregator (near real-time агрегатор)
- query-service (HTTP API для агрегатов и Top-N)
- contracts (общие DTO/Avro/Proto, OpenAPI)
- infra (docker-compose)

## Рекомендуемый стек
- Java 17+
- Spring Boot 3.x
- Gradle (Kotlin DSL предпочтительно)
- Kafka: Redpanda (docker-compose) или Kafka
- Stream processing:
  - Вариант А (проще): Spring for Apache Kafka + Kafka Streams
  - Вариант B (тяжелее): Flink (но для Spring Boot интервью обычно Kafka Streams)
- Aggregated DB:
  - ClickHouse (быстро, удобно для агрегатов) или Postgres (для простоты)
- Observability: Micrometer + Prometheus endpoint (actuator)

## Модульная структура
/settings.gradle.kts
/build.gradle.kts
/gradle/libs.versions.toml
/modules
  /contracts
  /event-producer
  /stream-aggregator
  /query-service
/infra
  docker-compose.yml
/docs
  c4-context.mmd
  c4-container.mmd
  c4-component-aggregator.mmd

## Контракты
### Kafka topic: `ad-click-events`
Сообщение ClickEvent:
- eventId: UUID (идемпотентность/дедуп)
- adId: String
- clickTs: Instant (event time)
- userId: String
- ip: String
- country: String
- optional: device, appVersion, etc.

### Aggregates storage
Таблица `ad_click_minute_agg`:
- ad_id (PK part)
- minute_bucket (PK part, truncated to minute)
- country (optional dimension)
- count (Long)
- updated_at

Таблица `topn_minute` (optional materialization):
- minute_bucket
- country (optional)
- topn_json (или rows: rank, ad_id, count)

## Сервисы

### stream-aggregator
Spring Boot app с Kafka Streams:
- consume `ad-click-events`
- dedup по eventId (state store TTL)
- windowing (tumbling 1m) по event time + allowed lateness
- aggregate counts per (adId, minute[, country])
- write upserts в Aggregated DB (idempotent)
- optional: compute Top-N per minute и писать отдельно

Настройки:
- processing guarantee: at-least-once как baseline; exactly-once-v2 если хотите показать зрелость
- partitioning: ключ Kafka = adId (или adId|country)

### query-service
Spring Boot REST:
- `GET /v1/ads/{adId}/count?windowMinutes=M&country=...`
- `GET /v1/ads/top?windowMinutes=M&topN=N&country=...`
Читает из Aggregated DB:
- для окна M: суммирует последние M минутных бакетов
- Top-N: либо читает materialized topn_minute, либо выполняет запрос с ORDER BY/LIMIT по последнему бакету и объединяет по окну

### event-producer (dev)
Spring Boot CommandLineRunner или отдельный профиль:
- генерирует клики в Kafka (rate configurable)
- поддержка hot keys (несколько adId с повышенной частотой)

## Infra (docker-compose)
- redpanda/kafka
- clickhouse (или postgres)
- prometheus (optional)
- grafana (optional)

## Порядок генерации (Codex checklist)
1) Создать Gradle multi-module + общие версии зависимостей (BOM Spring Boot).
2) contracts:
   - Java DTO + (опц.) Avro schema
   - unit tests на сериализацию/deserialization
3) infra: docker-compose + init scripts для БД (DDL).
4) stream-aggregator:
   - Kafka Streams topology
   - dedup store (RocksDB state store)
   - windowed aggregation
   - writer слой (JdbcTemplate/R2DBC) с idempotent upsert
   - integration test: EmbeddedKafka + Testcontainers DB
5) query-service:
   - OpenAPI (springdoc)
   - repository + service + controller
   - integration tests на SQL запросы (Testcontainers)
6) event-producer:
   - configurable generator
7) Observability:
   - actuator + micrometer metrics (processed/sec, late events, dedup hits, db write latency)
   - health checks

## Done criteria
- `docker compose up` поднимает infra
- producer шлёт события
- aggregator пишет агрегаты
- query-service отвечает корректно по count/top
- есть e2e test или manual сценарий с curl
