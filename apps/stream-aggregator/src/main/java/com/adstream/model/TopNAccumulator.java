package com.adstream.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TopNAccumulator {
    private List<AggregatedCount> topAds = new ArrayList<>();

    public TopNAccumulator() {
    }

    public TopNAccumulator(List<AggregatedCount> topAds) {
        this.topAds = topAds;
    }

    public List<AggregatedCount> getTopAds() {
        return topAds;
    }

    public TopNAccumulator update(AggregatedCount update, int limit) {
        topAds.removeIf(existing -> existing.getAdId().equals(update.getAdId()));
        topAds.add(update);
        topAds.sort(Comparator.comparingLong(AggregatedCount::getCount).reversed());
        if (topAds.size() > limit) {
            topAds = new ArrayList<>(topAds.subList(0, limit));
        }
        return this;
    }
}
