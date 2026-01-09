package com.adstream.storage;

import com.adstream.model.AggregatedCount;
import com.clickhouse.jdbc.ClickHouseDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public class ClickHouseWriter {
    private final ClickHouseDataSource dataSource;

    public ClickHouseWriter(String jdbcUrl) {
        this.dataSource = new ClickHouseDataSource(jdbcUrl);
    }

    public void ensureTables() {
        String ddl = "CREATE TABLE IF NOT EXISTS ad_click_aggregates (" +
            "ad_id String, " +
            "click_minute DateTime, " +
            "count UInt64, " +
            "updated_at DateTime" +
            ") ENGINE = ReplacingMergeTree(updated_at) " +
            "ORDER BY (ad_id, click_minute)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(ddl)) {
            statement.execute();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to ensure ClickHouse schema", ex);
        }
    }

    public void upsertAggregate(AggregatedCount aggregate) {
        String sql = "INSERT INTO ad_click_aggregates (ad_id, click_minute, count, updated_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, aggregate.getAdId());
            statement.setObject(2, aggregate.getClickMinute());
            statement.setLong(3, aggregate.getCount());
            statement.setObject(4, Instant.now());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to upsert aggregate", ex);
        }
    }
}
