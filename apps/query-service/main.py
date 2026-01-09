from datetime import datetime
from typing import List
import os

from clickhouse_driver import Client
from fastapi import FastAPI, Query
from pydantic import BaseModel

app = FastAPI(title="Ad Click Query Service")


def get_client() -> Client:
    host = os.getenv("CLICKHOUSE_HOST", "localhost")
    port = int(os.getenv("CLICKHOUSE_PORT", "9000"))
    database = os.getenv("CLICKHOUSE_DB", "default")
    user = os.getenv("CLICKHOUSE_USER", "default")
    password = os.getenv("CLICKHOUSE_PASSWORD", "")

    return Client(host=host, port=port, database=database, user=user, password=password)


class AggregatedCountResponse(BaseModel):
    ad_id: str
    click_minute: datetime
    count: int


class PopularAdResponse(BaseModel):
    ad_id: str
    count: int


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/aggregated_count", response_model=List[AggregatedCountResponse])
def aggregated_count(
    ad_id: str,
    start_minute: datetime = Query(..., description="ISO-8601 minute timestamp"),
    end_minute: datetime = Query(..., description="ISO-8601 minute timestamp"),
) -> List[AggregatedCountResponse]:
    client = get_client()
    rows = client.execute(
        "SELECT ad_id, click_minute, count FROM ad_click_aggregates FINAL "
        "WHERE ad_id = %(ad_id)s AND click_minute BETWEEN %(start)s AND %(end)s "
        "ORDER BY click_minute",
        {"ad_id": ad_id, "start": start_minute, "end": end_minute},
    )
    return [AggregatedCountResponse(ad_id=row[0], click_minute=row[1], count=row[2]) for row in rows]


@app.get("/popular_ads", response_model=List[PopularAdResponse])
def popular_ads(
    minute: datetime = Query(..., description="ISO-8601 minute timestamp"),
    limit: int = Query(10, ge=1, le=100),
) -> List[PopularAdResponse]:
    client = get_client()
    rows = client.execute(
        "SELECT ad_id, count FROM ad_click_aggregates FINAL "
        "WHERE click_minute = %(minute)s "
        "ORDER BY count DESC LIMIT %(limit)s",
        {"minute": minute, "limit": limit},
    )
    return [PopularAdResponse(ad_id=row[0], count=row[1]) for row in rows]
