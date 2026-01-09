package com.adstream.model;

import java.time.Instant;
import java.util.List;

public class TopAds {
    private final Instant clickMinute;
    private final List<AggregatedCount> topAds;

    public TopAds(Instant clickMinute, List<AggregatedCount> topAds) {
        this.clickMinute = clickMinute;
        this.topAds = topAds;
    }

    public Instant getClickMinute() {
        return clickMinute;
    }

    public List<AggregatedCount> getTopAds() {
        return topAds;
    }
}
