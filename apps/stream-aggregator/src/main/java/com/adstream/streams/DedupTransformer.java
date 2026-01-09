package com.adstream.streams;

import com.adstream.metrics.MetricsRegistry;
import com.adstream.model.ClickEvent;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.kstream.Transformer;

import java.time.Duration;

public class DedupTransformer implements Transformer<String, ClickEvent, KeyValue<String, ClickEvent>> {
    public static final String STORE_NAME = "dedup-store";

    private final Duration ttl;
    private KeyValueStore<String, Long> store;
    private ProcessorContext context;

    public DedupTransformer(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.store = context.getStateStore(STORE_NAME);
        this.context.schedule(Duration.ofMinutes(1), PunctuationType.WALL_CLOCK_TIME, timestamp -> purgeOld(timestamp));
    }

    @Override
    public KeyValue<String, ClickEvent> transform(String key, ClickEvent value) {
        if (value == null) {
            return null;
        }
        Long existing = store.get(value.getEventId());
        if (existing != null) {
            MetricsRegistry.recordDedup();
            return null;
        }
        store.put(value.getEventId(), context.timestamp());
        return KeyValue.pair(key, value);
    }

    @Override
    public void close() {
    }

    private void purgeOld(long now) {
        try (KeyValueIterator<String, Long> iterator = store.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, Long> entry = iterator.next();
                if (now - entry.value > ttl.toMillis()) {
                    store.delete(entry.key);
                }
            }
        }
    }

    public static StoreBuilder<KeyValueStore<String, Long>> storeBuilder() {
        return Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(STORE_NAME),
            org.apache.kafka.common.serialization.Serdes.String(),
            org.apache.kafka.common.serialization.Serdes.Long()
        );
    }
}
