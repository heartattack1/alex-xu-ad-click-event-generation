CREATE TABLE IF NOT EXISTS ad_click_aggregates (
    ad_id String,
    click_minute DateTime,
    count UInt64,
    updated_at DateTime
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (ad_id, click_minute);
