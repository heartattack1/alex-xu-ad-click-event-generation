import json
import os
import time
import uuid
from pathlib import Path

from confluent_kafka import Producer
from faker import Faker
from fastavro import parse_schema, schemaless_writer

faker = Faker()


def load_schema() -> dict:
    schema_path = Path(__file__).resolve().parents[2] / "contracts" / "click-event.avsc"
    return json.loads(schema_path.read_text())


def encode_event(schema, event: dict) -> bytes:
    buffer = bytearray()
    schemaless_writer(buffer, schema, event)
    return bytes(buffer)


def build_event() -> dict:
    return {
        "event_id": str(uuid.uuid4()),
        "ad_id": faker.random_element(elements=[f"ad-{i}" for i in range(1, 11)]),
        "click_ts": int(time.time() * 1000),
        "user_id": faker.user_name(),
        "ip": faker.ipv4_public(),
        "country": faker.country_code(),
    }


def main() -> None:
    bootstrap = os.getenv("KAFKA_BOOTSTRAP", "localhost:9092")
    topic = os.getenv("INPUT_TOPIC", "click-events")
    rate_per_sec = float(os.getenv("EVENTS_PER_SEC", "10"))

    schema = parse_schema(load_schema())
    producer = Producer({"bootstrap.servers": bootstrap})

    delay = 1.0 / rate_per_sec if rate_per_sec > 0 else 0
    while True:
        event = build_event()
        payload = encode_event(schema, event)
        producer.produce(topic, payload)
        producer.poll(0)
        if delay:
            time.sleep(delay)


if __name__ == "__main__":
    main()
