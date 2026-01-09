package com.adstream.model;

import java.time.Instant;

public class AggregatedCount {
    private final String adId;
    private final Instant clickMinute;
    private final long count;

    public AggregatedCount(String adId, Instant clickMinute, long count) {
        this.adId = adId;
        this.clickMinute = clickMinute;
        this.count = count;
    }

    public String getAdId() {
        return adId;
    }

    public Instant getClickMinute() {
        return clickMinute;
    }

    public long getCount() {
        return count;
    }
}
