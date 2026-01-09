package com.adstream;

import com.adstream.metrics.MetricsRegistry;
import com.adstream.model.AggregatedCount;
import com.adstream.model.ClickEvent;
import com.adstream.model.TopAds;
import com.adstream.model.TopNAccumulator;
import com.adstream.serialization.ClickEventSerde;
import com.adstream.serialization.JsonSerde;
import com.adstream.storage.ClickHouseWriter;
import com.adstream.streams.DedupTransformer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

public class StreamAggregatorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamAggregatorApp.class);

    public static void main(String[] args) {
        String bootstrapServers = getenv("KAFKA_BOOTSTRAP", "localhost:9092");
        String inputTopic = getenv("INPUT_TOPIC", "click-events");
        String aggregatesTopic = getenv("AGGREGATES_TOPIC", "ad-aggregates");
        String topTopic = getenv("TOP_TOPIC", "ad-top-n");
        String clickhouseUrl = getenv("CLICKHOUSE_URL", "jdbc:ch://localhost:8123/default");
        Duration dedupTtl = Duration.ofMillis(Long.parseLong(getenv("DEDUP_TTL_MS", "600000")));
        int topLimit = Integer.parseInt(getenv("TOP_N", "10"));
        long lateEventMs = Long.parseLong(getenv("LATE_EVENT_MS", "60000"));

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ad-click-stream-aggregator");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();
        builder.addStateStore(DedupTransformer.storeBuilder());

        ClickEventSerde clickEventSerde = new ClickEventSerde();
        KStream<String, ClickEvent> events = builder.stream(
            inputTopic,
            Consumed.with(Serdes.String(), clickEventSerde).withTimestampExtractor(clickEventTimestamp())
        );

        KStream<String, ClickEvent> enriched = events.peek((key, value) -> {
            if (value != null) {
                MetricsRegistry.recordProcessed(value.getClickTs());
                if (System.currentTimeMillis() - value.getClickTs() > lateEventMs) {
                    MetricsRegistry.recordLate();
                }
            }
        });

        KStream<String, ClickEvent> deduped = enriched.transform(
            () -> new DedupTransformer(dedupTtl),
            DedupTransformer.STORE_NAME
        );

        KTable<Windowed<String>, Long> counts = deduped
            .groupBy((key, value) -> value.getAdId(), Grouped.with(Serdes.String(), clickEventSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .count(Materialized.as("ad-counts"));

        JsonSerde<AggregatedCount> aggregatedSerde = new JsonSerde<>(AggregatedCount.class);

        KStream<Windowed<String>, Long> countsStream = counts.toStream();

        countsStream
            .map((windowedKey, count) -> KeyValue.pair(windowedKey.key(), new AggregatedCount(
                windowedKey.key(),
                windowedKey.window().startTime(),
                count
            )))
            .to(aggregatesTopic, org.apache.kafka.streams.kstream.Produced.with(Serdes.String(), aggregatedSerde));

        countsStream
            .map((windowedKey, count) -> KeyValue.pair(
                windowedKey.window().startTime().toString(),
                new AggregatedCount(windowedKey.key(), windowedKey.window().startTime(), count)
            ))
            .groupByKey(Grouped.with(Serdes.String(), aggregatedSerde))
            .aggregate(
                TopNAccumulator::new,
                (key, value, aggregate) -> aggregate.update(value, topLimit),
                Materialized.with(Serdes.String(), new JsonSerde<>(TopNAccumulator.class))
            )
            .toStream()
            .map((minute, accumulator) -> KeyValue.pair(minute, new TopAds(Instant.parse(minute), accumulator.getTopAds())))
            .to(topTopic, org.apache.kafka.streams.kstream.Produced.with(Serdes.String(), new JsonSerde<>(TopAds.class)));

        ClickHouseWriter writer = new ClickHouseWriter(clickhouseUrl);
        writer.ensureTables();
        countsStream.foreach((windowedKey, count) -> writer.upsertAggregate(
            new AggregatedCount(windowedKey.key(), windowedKey.window().startTime(), count)
        ));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        startMetricsReporter();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }

    private static TimestampExtractor clickEventTimestamp() {
        return (record, previousTimestamp) -> {
            if (record.value() instanceof ClickEvent event) {
                return event.getClickTs();
            }
            return previousTimestamp != null ? previousTimestamp : System.currentTimeMillis();
        };
    }

    private static String getenv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static void startMetricsReporter() {
        Thread reporter = new Thread(() -> {
            long lastProcessed = 0;
            while (true) {
                long current = MetricsRegistry.processed();
                long processedPerSec = current - lastProcessed;
                lastProcessed = current;
                LOGGER.info(
                    \"metrics processed_total={} processed_per_sec={} dedup_hits={} late_events={} lag_ms={}\",
                    current,
                    processedPerSec,
                    MetricsRegistry.dedupHits(),
                    MetricsRegistry.lateEvents(),
                    MetricsRegistry.lastLagMs()
                );
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        reporter.setDaemon(true);
        reporter.start();
    }
}
