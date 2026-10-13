package com.example.localdatebase.database;

public interface LocalDataStore extends CategoryRecordStore, SensorDataStore, PoiDataStore, AutoCloseable {
    @Override void close();
}
