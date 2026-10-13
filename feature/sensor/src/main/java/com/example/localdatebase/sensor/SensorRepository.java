package com.example.localdatebase.sensor;

import android.content.Context;

import com.example.localdatebase.database.LocalDatabaseProvider;
import com.example.localdatebase.database.SensorDataStore;
import com.example.localdatebase.database.SensorReading;

import java.util.ArrayList;
import java.util.List;

/** Sensor 模块唯一的数据访问入口。业务代码无需知道 SQLite 表名和 SQL。 */
public final class SensorRepository {
    public static final String MODULE_ID = "feature.sensor";

    private final SensorDataStore store;

    public SensorRepository(Context context) {
        this(LocalDatabaseProvider.sensors(context));
    }

    public SensorRepository(SensorDataStore store) {
        this.store = store;
    }

    public long save(
            String sensorType,
            double value,
            String unit,
            long recordedAt,
            String sessionId,
            String metadataJson
    ) {
        return store.saveSensorReading(SensorReading.create(
                MODULE_ID,
                sensorType,
                value,
                unit,
                recordedAt,
                sessionId,
                metadataJson
        ));
    }

    public void saveBatch(List<SensorSample> samples) {
        List<SensorReading> readings = new ArrayList<>(samples.size());
        for (SensorSample sample : samples) {
            readings.add(SensorReading.create(
                    MODULE_ID,
                    sample.sensorType,
                    sample.value,
                    sample.unit,
                    sample.recordedAt,
                    sample.sessionId,
                    sample.metadataJson
            ));
        }
        store.saveSensorReadings(readings);
    }

    public List<SensorReading> latest(String sensorType, int limit) {
        return store.getLatestSensorReadings(MODULE_ID, sensorType, limit);
    }

    public List<SensorReading> between(String sensorType, long fromInclusive, long toExclusive) {
        return store.getSensorReadingsBetween(MODULE_ID, sensorType, fromInclusive, toExclusive);
    }

    public int deleteOlderThan(long cutoffExclusive) {
        return store.deleteSensorReadingsBefore(MODULE_ID, cutoffExclusive);
    }

    public static final class SensorSample {
        public final String sensorType;
        public final double value;
        public final String unit;
        public final long recordedAt;
        public final String sessionId;
        public final String metadataJson;

        public SensorSample(
                String sensorType,
                double value,
                String unit,
                long recordedAt,
                String sessionId,
                String metadataJson
        ) {
            this.sensorType = sensorType;
            this.value = value;
            this.unit = unit;
            this.recordedAt = recordedAt;
            this.sessionId = sessionId;
            this.metadataJson = metadataJson;
        }
    }
}
