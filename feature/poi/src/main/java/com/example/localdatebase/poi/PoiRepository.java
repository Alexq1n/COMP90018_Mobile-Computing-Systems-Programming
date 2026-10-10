package com.example.localdatebase.poi;

import android.content.Context;

import com.example.localdatebase.database.LocalDatabaseProvider;
import com.example.localdatebase.database.PoiData;
import com.example.localdatebase.database.PoiDataStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** POI 模块入口：导入随应用打包的 JSON，并提供查询接口。 */
public final class PoiRepository {
    private static final String ASSET_NAME = "pois.json";

    private final Context context;
    private final PoiDataStore store;

    public PoiRepository(Context context) {
        this(context, LocalDatabaseProvider.pois(context));
    }

    public PoiRepository(Context context, PoiDataStore store) {
        this.context = context.getApplicationContext();
        this.store = store;
    }

    /** Idempotent: records with the same JSON id and category are updated rather than duplicated. */
    public int importBundledPois() throws IOException {
        try (InputStream input = context.getAssets().open(ASSET_NAME)) {
            List<PoiData> pois = parse(input);
            store.upsertPois(pois);
            return pois.size();
        }
    }

    public PoiData byId(String id) {
        return store.getPoiById(id);
    }

    public List<PoiData> find(String category, String city, String keyword, int limit) {
        return store.getPois(category, city, keyword, limit);
    }

    public int count() {
        return store.countPois();
    }

    static List<PoiData> parse(InputStream input) throws IOException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) json.append(line).append('\n');
        }
        try {
            JSONArray array = new JSONArray(json.toString());
            List<PoiData> result = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                JSONObject coordinates = item.getJSONObject("coordinates");
                JSONObject hours = item.optJSONObject("operatingHours");
                result.add(new PoiData(
                        item.getString("id"),
                        item.getString("name"),
                        item.optDouble("baseScore", 0),
                        item.optString("category", ""),
                        item.optString("environment", ""),
                        coordinates.getDouble("latitude"),
                        coordinates.getDouble("longitude"),
                        item.optInt("recommendedVisitDuration", 0),
                        hours == null ? "" : hours.optString("openTime", ""),
                        hours == null ? "" : hours.optString("closeTime", ""),
                        item.optBoolean("isFiller", false),
                        item.optString("city", ""),
                        item.optString("address", ""),
                        item.isNull("description") ? "" : item.optString("description", "")
                ));
            }
            return result;
        } catch (JSONException error) {
            throw new IOException("pois.json 格式无效", error);
        }
    }
}
