package com.example.localdatebase.database;

/** One point of interest stored locally. */
public final class PoiData {
    public final String id;
    public final String name;
    public final double baseScore;
    public final String category;
    public final String environment;
    public final double latitude;
    public final double longitude;
    public final int recommendedVisitDuration;
    public final String openTime;
    public final String closeTime;
    public final boolean filler;
    public final String city;
    public final String address;
    public final String description;

    public PoiData(String id, String name, double baseScore, String category, String environment,
                   double latitude, double longitude, int recommendedVisitDuration,
                   String openTime, String closeTime, boolean filler, String city,
                   String address, String description) {
        this.id = id;
        this.name = name;
        this.baseScore = baseScore;
        this.category = category;
        this.environment = environment;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recommendedVisitDuration = recommendedVisitDuration;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.filler = filler;
        this.city = city;
        this.address = address;
        this.description = description;
    }
}
