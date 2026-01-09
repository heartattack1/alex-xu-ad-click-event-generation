# Ad Click Event Aggregation

## Overview
This repository provides a local stack for ingesting click events, aggregating them in real time, and querying aggregates.

## Services
- **event-producer**: generates ClickEvent payloads and publishes to Kafka.
- **stream-aggregator**: Kafka Streams app that deduplicates events, aggregates counts per minute, and writes to ClickHouse.
- **query-service**: FastAPI service that queries aggregated counts.

## Local Run
```bash
docker compose -f infra/docker-compose.yml up --build
```

## Query API Examples
```bash
curl "http://localhost:8080/aggregated_count?ad_id=ad-1&start_minute=2024-01-01T00:00:00&end_minute=2024-01-01T00:10:00"
curl "http://localhost:8080/popular_ads?minute=2024-01-01T00:00:00&limit=5"
```

## Topics
- `click-events`: raw click events (Avro)
- `ad-aggregates`: aggregated counts per ad/minute
- `ad-top-n`: top ads per minute
