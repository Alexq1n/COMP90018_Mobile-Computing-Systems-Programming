package com.example.localdatebase.database;

import java.util.List;

/** Stable API shared by sensor, chart, analysis and upload feature modules. */
public interface SensorDataStore {
    long saveSensorReading(SensorReading reading);
    void saveSensorReadings(List<SensorReading> readings);
    List<SensorReading> getLatestSensorReadings(String ownerModule, String sensorType, int limit);
    List<SensorReading> getSensorReadingsBetween(String ownerModule, String sensorType,
                                                 long startInclusive, long endExclusive);
    int deleteSensorReadingsBefore(String ownerModule, long cutoffExclusive);
}
