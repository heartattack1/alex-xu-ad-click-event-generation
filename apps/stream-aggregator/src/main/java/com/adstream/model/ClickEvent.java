package com.adstream.model;

public class ClickEvent {
    private final String eventId;
    private final String adId;
    private final long clickTs;
    private final String userId;
    private final String ip;
    private final String country;

    public ClickEvent(String eventId, String adId, long clickTs, String userId, String ip, String country) {
        this.eventId = eventId;
        this.adId = adId;
        this.clickTs = clickTs;
        this.userId = userId;
        this.ip = ip;
        this.country = country;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAdId() {
        return adId;
    }

    public long getClickTs() {
        return clickTs;
    }

    public String getUserId() {
        return userId;
    }

    public String getIp() {
        return ip;
    }

    public String getCountry() {
        return country;
    }
}
