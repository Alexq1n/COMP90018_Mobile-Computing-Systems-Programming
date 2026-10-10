package com.example.localdatebase.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class SQLiteLocalDataStore extends SQLiteOpenHelper implements LocalDataStore {
    static final String DATABASE_NAME = "classified_data.db";
    private static final int DATABASE_VERSION = 4;

    SQLiteLocalDataStore(Context context, String name) {
        super(context.getApplicationContext(), name, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        createRecordTables(db);
        createSensorTable(db);
        createPoiTable(db);
        insertDefaultCategories(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createSensorTable(db);
        if (oldVersion < 3) {
            createPoiTable(db);
        } else if (oldVersion < 4) {
            migrateVersionThreePois(db);
        }
    }

    private static void createRecordTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(name)) BETWEEN 1 AND 30))");
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT, "
                + "title TEXT NOT NULL CHECK(length(trim(title)) BETWEEN 1 AND 100), "
                + "content TEXT NOT NULL CHECK(length(content) <= 10000), "
                + "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX records_category_updated ON records(category_id, updated_at DESC)");
    }

    private static void createSensorTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sensor_readings ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, owner_module TEXT NOT NULL, "
                + "sensor_type TEXT NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL, "
                + "recorded_at INTEGER NOT NULL, session_id TEXT NOT NULL DEFAULT '', "
                + "metadata TEXT NOT NULL DEFAULT '', "
                + "CHECK(length(trim(owner_module)) BETWEEN 1 AND 80), "
                + "CHECK(length(trim(sensor_type)) BETWEEN 1 AND 80))");
        db.execSQL("CREATE INDEX IF NOT EXISTS sensor_module_type_time "
                + "ON sensor_readings(owner_module, sensor_type, recorded_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS sensor_session_time "
                + "ON sensor_readings(session_id, recorded_at ASC)");
    }

    private static void createPoiTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS pois ("
                + "id TEXT NOT NULL, name TEXT NOT NULL, base_score REAL NOT NULL, "
                + "category TEXT NOT NULL, environment TEXT NOT NULL, latitude REAL NOT NULL, "
                + "longitude REAL NOT NULL, recommended_visit_duration INTEGER NOT NULL, "
                + "open_time TEXT NOT NULL, close_time TEXT NOT NULL, is_filler INTEGER NOT NULL, "
                + "city TEXT NOT NULL, address TEXT NOT NULL, description TEXT NOT NULL, "
                + "PRIMARY KEY(id, category))");
        db.execSQL("CREATE INDEX IF NOT EXISTS pois_category_city ON pois(category, city)");
        db.execSQL("CREATE INDEX IF NOT EXISTS pois_location ON pois(latitude, longitude)");
    }

    private static void migrateVersionThreePois(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("ALTER TABLE pois RENAME TO pois_v3");
            db.execSQL("DROP INDEX IF EXISTS pois_category_city");
            db.execSQL("DROP INDEX IF EXISTS pois_location");
            createPoiTable(db);
            db.execSQL("INSERT INTO pois(id, name, base_score, category, environment, latitude, "
                    + "longitude, recommended_visit_duration, open_time, close_time, is_filler, "
                    + "city, address, description) SELECT id, name, base_score, category, environment, "
                    + "latitude, longitude, recommended_visit_duration, open_time, close_time, "
                    + "is_filler, city, address, description FROM pois_v3");
            db.execSQL("DROP TABLE pois_v3");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void insertDefaultCategories(SQLiteDatabase db) {
        for (String name : new String[]{"工作", "学习", "生活", "其他"}) {
            ContentValues values = new ContentValues();
            values.put("name", name);
            db.insertOrThrow("categories", null, values);
        }
    }

    @Override public long addCategory(String name) {
        String cleaned = requireText(name, 30, "分类名称");
        ContentValues values = new ContentValues();
        values.put("name", cleaned);
        return getWritableDatabase().insertOrThrow("categories", null, values);
    }

    @Override public long saveRecord(Long id, long categoryId, String title, String content) {
        String cleaned = requireText(title, 100, "标题");
        if (content == null || content.length() > 10000)
            throw new IllegalArgumentException("内容不能超过 10000 个字符");
        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("title", cleaned);
        values.put("content", content);
        values.put("updated_at", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        if (id == null) {
            values.put("created_at", System.currentTimeMillis());
            return db.insertOrThrow("records", null, values);
        }
        if (db.update("records", values, "id = ?", new String[]{id.toString()}) != 1)
            throw new IllegalArgumentException("记录已不存在，请刷新后重试");
        return id;
    }

    @Override public void deleteRecord(long id) {
        getWritableDatabase().delete("records", "id = ?", new String[]{Long.toString(id)});
    }

    @Override public List<CategoryData> getCategories() {
        List<CategoryData> results = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT c.id, c.name, COUNT(r.id) FROM categories c LEFT JOIN records r "
                        + "ON r.category_id = c.id GROUP BY c.id, c.name ORDER BY c.id", null)) {
            while (c.moveToNext()) results.add(new CategoryData(c.getLong(0), c.getString(1), c.getInt(2)));
        }
        return results;
    }

    @Override public List<RecordData> getRecords(Long categoryId, String search) {
        List<String> args = new ArrayList<>();
        String where = "";
        if (categoryId != null) {
            where = " WHERE r.category_id = ?";
            args.add(categoryId.toString());
        }
        String text = search == null ? "" : search.trim();
        if (!text.isEmpty()) {
            where += where.isEmpty() ? " WHERE " : " AND ";
            where += "(instr(lower(r.title), lower(?)) > 0 OR instr(lower(r.content), lower(?)) > 0)";
            args.add(text);
            args.add(text);
        }
        List<RecordData> results = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT r.id, r.category_id, c.name, r.title, r.content, r.created_at, r.updated_at "
                        + "FROM records r JOIN categories c ON c.id = r.category_id" + where
                        + " ORDER BY r.updated_at DESC, r.id DESC", args.toArray(new String[0]))) {
            while (c.moveToNext()) results.add(new RecordData(c.getLong(0), c.getLong(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getLong(5), c.getLong(6)));
        }
        return results;
    }

    @Override public long saveSensorReading(SensorReading reading) {
        validateSensorReading(reading);
        return getWritableDatabase().insertOrThrow("sensor_readings", null, sensorValues(reading));
    }

    @Override public void saveSensorReadings(List<SensorReading> readings) {
        if (readings == null || readings.isEmpty()) return;
        for (SensorReading reading : readings) validateSensorReading(reading);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (SensorReading reading : readings)
                db.insertOrThrow("sensor_readings", null, sensorValues(reading));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public List<SensorReading> getLatestSensorReadings(
            String ownerModule, String sensorType, int limit) {
        String module = requireText(ownerModule, 80, "模块 ID");
        String type = requireText(sensorType, 80, "传感器类型");
        if (limit < 1 || limit > 10000) throw new IllegalArgumentException("limit 需要在 1–10000 之间");
        return querySensors("owner_module = ? AND sensor_type = ?",
                new String[]{module, type}, "recorded_at DESC, id DESC", Integer.toString(limit));
    }

    @Override public List<SensorReading> getSensorReadingsBetween(
            String ownerModule, String sensorType, long startInclusive, long endExclusive) {
        String module = requireText(ownerModule, 80, "模块 ID");
        String type = requireText(sensorType, 80, "传感器类型");
        if (endExclusive <= startInclusive) throw new IllegalArgumentException("结束时间必须晚于开始时间");
        return querySensors("owner_module = ? AND sensor_type = ? AND recorded_at >= ? AND recorded_at < ?",
                new String[]{module, type, Long.toString(startInclusive), Long.toString(endExclusive)},
                "recorded_at ASC, id ASC", null);
    }

    @Override public int deleteSensorReadingsBefore(String ownerModule, long cutoffExclusive) {
        String module = requireText(ownerModule, 80, "模块 ID");
        return getWritableDatabase().delete("sensor_readings", "owner_module = ? AND recorded_at < ?",
                new String[]{module, Long.toString(cutoffExclusive)});
    }

    private List<SensorReading> querySensors(String selection, String[] args, String order, String limit) {
        List<SensorReading> results = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("sensor_readings",
                new String[]{"id", "owner_module", "sensor_type", "value", "unit", "recorded_at", "session_id", "metadata"},
                selection, args, null, null, order, limit)) {
            while (c.moveToNext()) results.add(new SensorReading(c.getLong(0), c.getString(1), c.getString(2),
                    c.getDouble(3), c.getString(4), c.getLong(5), c.getString(6), c.getString(7)));
        }
        return results;
    }

    private static ContentValues sensorValues(SensorReading reading) {
        ContentValues values = new ContentValues();
        values.put("owner_module", reading.ownerModule.trim());
        values.put("sensor_type", reading.sensorType.trim());
        values.put("value", reading.value);
        values.put("unit", reading.unit == null ? "" : reading.unit);
        values.put("recorded_at", reading.recordedAt);
        values.put("session_id", reading.sessionId == null ? "" : reading.sessionId);
        values.put("metadata", reading.metadata == null ? "" : reading.metadata);
        return values;
    }

    @Override public void upsertPois(List<PoiData> pois) {
        if (pois == null || pois.isEmpty()) return;
        for (PoiData poi : pois) validatePoi(poi);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (PoiData poi : pois) {
                db.insertWithOnConflict("pois", null, poiValues(poi), SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public PoiData getPoiById(String id) {
        String cleaned = requireText(id, 512, "POI ID");
        try (Cursor cursor = getReadableDatabase().query("pois", poiColumns(), "id = ?",
                new String[]{cleaned}, null, null, "base_score DESC, category ASC", "1")) {
            return cursor.moveToFirst() ? readPoi(cursor) : null;
        }
    }

    @Override public List<PoiData> getPois(String category, String city, String search, int limit) {
        if (limit < 1 || limit > 10000) throw new IllegalArgumentException("limit 需要在 1–10000 之间");
        List<String> clauses = new ArrayList<>();
        List<String> args = new ArrayList<>();
        addEqualsFilter(clauses, args, "category", category);
        addEqualsFilter(clauses, args, "city", city);
        String keyword = search == null ? "" : search.trim();
        if (!keyword.isEmpty()) {
            clauses.add("(instr(lower(name), lower(?)) > 0 OR instr(lower(address), lower(?)) > 0 "
                    + "OR instr(lower(description), lower(?)) > 0)");
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        String selection = clauses.isEmpty() ? null : String.join(" AND ", clauses);
        List<PoiData> results = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("pois", poiColumns(), selection,
                args.toArray(new String[0]), null, null, "base_score DESC, name ASC",
                Integer.toString(limit))) {
            while (cursor.moveToNext()) results.add(readPoi(cursor));
        }
        return results;
    }

    @Override public int countPois() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM pois", null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    private static void addEqualsFilter(
            List<String> clauses, List<String> args, String column, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (!cleaned.isEmpty()) {
            clauses.add(column + " = ? COLLATE NOCASE");
            args.add(cleaned);
        }
    }

    private static String[] poiColumns() {
        return new String[]{"id", "name", "base_score", "category", "environment", "latitude",
                "longitude", "recommended_visit_duration", "open_time", "close_time", "is_filler",
                "city", "address", "description"};
    }

    private static PoiData readPoi(Cursor cursor) {
        return new PoiData(cursor.getString(0), cursor.getString(1), cursor.getDouble(2),
                cursor.getString(3), cursor.getString(4), cursor.getDouble(5), cursor.getDouble(6),
                cursor.getInt(7), cursor.getString(8), cursor.getString(9), cursor.getInt(10) != 0,
                cursor.getString(11), cursor.getString(12), cursor.getString(13));
    }

    private static ContentValues poiValues(PoiData poi) {
        ContentValues values = new ContentValues();
        values.put("id", poi.id.trim());
        values.put("name", poi.name.trim());
        values.put("base_score", poi.baseScore);
        values.put("category", safe(poi.category));
        values.put("environment", safe(poi.environment));
        values.put("latitude", poi.latitude);
        values.put("longitude", poi.longitude);
        values.put("recommended_visit_duration", poi.recommendedVisitDuration);
        values.put("open_time", safe(poi.openTime));
        values.put("close_time", safe(poi.closeTime));
        values.put("is_filler", poi.filler ? 1 : 0);
        values.put("city", safe(poi.city));
        values.put("address", safe(poi.address));
        values.put("description", safe(poi.description));
        return values;
    }

    private static void validatePoi(PoiData poi) {
        if (poi == null) throw new IllegalArgumentException("POI 数据不能为空");
        requireText(poi.id, 512, "POI ID");
        requireText(poi.name, 300, "POI 名称");
        if (!Double.isFinite(poi.baseScore)) throw new IllegalArgumentException("POI 评分必须是有限数字");
        if (!Double.isFinite(poi.latitude) || poi.latitude < -90 || poi.latitude > 90)
            throw new IllegalArgumentException("纬度无效");
        if (!Double.isFinite(poi.longitude) || poi.longitude < -180 || poi.longitude > 180)
            throw new IllegalArgumentException("经度无效");
        if (poi.recommendedVisitDuration < 0) throw new IllegalArgumentException("建议游览时长不能为负数");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void validateSensorReading(SensorReading reading) {
        if (reading == null) throw new IllegalArgumentException("传感器数据不能为空");
        requireText(reading.ownerModule, 80, "模块 ID");
        requireText(reading.sensorType, 80, "传感器类型");
        if (!Double.isFinite(reading.value)) throw new IllegalArgumentException("传感器数值必须是有限数字");
        if (reading.recordedAt < 0) throw new IllegalArgumentException("采集时间不能为负数");
        if (reading.unit != null && reading.unit.length() > 40) throw new IllegalArgumentException("单位过长");
        if (reading.sessionId != null && reading.sessionId.length() > 120) throw new IllegalArgumentException("会话 ID 过长");
        if (reading.metadata != null && reading.metadata.length() > 10000) throw new IllegalArgumentException("元数据过长");
    }

    private static String requireText(String value, int max, String label) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty() || cleaned.length() > max)
            throw new IllegalArgumentException(label + "需要 1–" + max + " 个字符");
        return cleaned;
    }
}
