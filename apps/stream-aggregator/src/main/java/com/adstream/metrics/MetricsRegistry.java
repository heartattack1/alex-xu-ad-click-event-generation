package com.adstream.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsRegistry {
    private static final AtomicLong processed = new AtomicLong();
    private static final AtomicLong dedupHits = new AtomicLong();
    private static final AtomicLong lateEvents = new AtomicLong();
    private static final AtomicLong lastLagMs = new AtomicLong();

    public static void recordProcessed(long clickTs) {
        processed.incrementAndGet();
        long lag = Instant.now().toEpochMilli() - clickTs;
        lastLagMs.set(lag);
    }

    public static void recordDedup() {
        dedupHits.incrementAndGet();
    }

    public static void recordLate() {
        lateEvents.incrementAndGet();
    }

    public static long processed() {
        return processed.get();
    }

    public static long dedupHits() {
        return dedupHits.get();
    }

    public static long lateEvents() {
        return lateEvents.get();
    }

    public static long lastLagMs() {
        return lastLagMs.get();
    }
}
