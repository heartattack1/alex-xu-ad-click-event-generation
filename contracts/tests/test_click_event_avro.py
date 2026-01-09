import json
from pathlib import Path

from fastavro import parse_schema, schemaless_reader, schemaless_writer


def test_click_event_roundtrip():
    schema_path = Path(__file__).resolve().parents[1] / "click-event.avsc"
    schema = json.loads(schema_path.read_text())
    parsed = parse_schema(schema)

    payload = {
        "event_id": "e7c122cc-2a3e-4f0c-9e0f-10d5b6c4c2e8",
        "ad_id": "ad-123",
        "click_ts": 1700000000000,
        "user_id": "user-1",
        "ip": "192.168.0.1",
        "country": "US",
    }

    buffer = bytearray()
    schemaless_writer(buffer, parsed, payload)
    decoded = schemaless_reader(buffer, parsed)

    assert decoded == payload
