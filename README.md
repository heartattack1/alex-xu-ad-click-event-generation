# Ad Click Event Aggregation

Сервис потоковой агрегации кликов по рекламе: 
1) вернуть агрегированное число кликов по `ad_id` за последние `M` минут, 
2) вернуть Top-N наиболее кликаемых `ad_id` за последние `M` минут,
3) поддержать фильтрацию по атрибутам (например, country/user/ip).

## Цели и нецели
### Goals
- Near real-time агрегация кликов с минутной гранулярностью.
- Поддержка запросов:
  - aggregated_count(ad_id, window=M, filters)
  - popular_ads(topN, window=M, filters)
- Корректность и устойчивость (дубликаты, late events, частичные отказы).

### Non-goals (упрощения для интервью)
- Антифрод, атрибуция конверсий, сложные модели биллинга RTB.
- Полная аналитика ad-tech (это отдельный контур).

## Требования
### Functional
- Агрегировать клики `ad_id` за последние `M` минут.
- Top-N `ad_id` за последние `M` минут.
- Фильтрация по измерениям (country, user_id, ip и т.д.).
- Обработка late events и дубликатов.

### Non-functional
- Высокая точность (данные могут влиять на RTB/биллинг).
- Низкая задержка ответа (минуты допустимы; это не user-facing миллисекунды).
- Масштабирование по QPS и объёму событий.
- Надёжность при сбоях отдельных компонент.

## API (пример)
- `GET /v1/ads/{ad_id}/aggregated_count?from=...&to=...&filter=...`
- `GET /v1/ads/popular_ads?topN=...&window=...&filter=...`

## Модель данных
### Raw click event
- `ad_id`, `click_timestamp`, `user_id`, `ip`, `country`, ...

### Aggregated (minute bucket)
- `ad_id`, `click_minute`, `count`
- Для фильтрации: star-schema подход (доп. измерения/ключи фильтра).

## Архитектура (high level)
- Ingestion (лог-ватчер / трекер событий) -> Message Queue
- Stream aggregation service (Map/Aggregate/Reduce DAG)
- Две записи:
  - raw store (для reprocessing / backfill)
  - aggregated store (для быстрых запросов)
- Query service для API.

## Exactly-once / delivery semantics
- На практике часто достаточно at-least-once + дедупликация.
- Для высокой точности: транзакционная запись/offset-commit, идемпотентные апдейты, либо exactly-once возможности стриминга.

## Late events / Watermark
- Event time vs processing time: выберите компромисс.
- Watermark/allowed lateness (например, +15s/+1m) повышает корректность за цену latency.

## Масштабирование
- Партиционирование по `ad_id` (или composite key) в MQ.
- Горизонтальное масштабирование агрегаторов.
- Кэш топ-N и hot keys.
- Pre-aggregation по минутам + rolling window для запросов.

## Надёжность и деградации
- Очередь как буфер при недоступности хранилища.
- Репликация стораджа.
- Backfill/recalculation pipeline из raw store.

## Observability
- Lag по очереди, watermark delay, rate дубликатов, точность агрегаций.
- SLO: freshness агрегатов, error budget, drop/late rate.
