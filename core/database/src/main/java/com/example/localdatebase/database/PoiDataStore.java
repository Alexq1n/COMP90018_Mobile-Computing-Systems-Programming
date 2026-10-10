package com.example.localdatebase.database;

import java.util.List;

/** Stable local POI API used by map, route, recommendation and search modules. */
public interface PoiDataStore {
    void upsertPois(List<PoiData> pois);
    PoiData getPoiById(String id);
    List<PoiData> getPois(String category, String city, String search, int limit);
    int countPois();
}
