package com.example.localdatebase.database;

/** A module-owned, timestamped sensor sample. Metadata can contain module-specific JSON. */
public final class SensorReading {
    public final long id;
    public final String ownerModule;
    public final String sensorType;
    public final double value;
    public final String unit;
    public final long recordedAt;
    public final String sessionId;
    public final String metadata;

    public SensorReading(long id, String ownerModule, String sensorType, double value, String unit,
                         long recordedAt, String sessionId, String metadata) {
        this.id = id;
        this.ownerModule = ownerModule;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.sessionId = sessionId;
        this.metadata = metadata;
    }

    public static SensorReading create(String ownerModule, String sensorType, double value,
                                       String unit, long recordedAt, String sessionId, String metadata) {
        return new SensorReading(0, ownerModule, sensorType, value, unit, recordedAt,
                sessionId == null ? "" : sessionId, metadata == null ? "" : metadata);
    }
}
