package com.example.localdatebase.database;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class LocalDataStoreTest {
    private Context context;
    private String name;
    private LocalDataStore db;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        name = "test-" + UUID.randomUUID() + ".db";
        assertTrue(context.getDatabasePath(name).getParentFile().mkdirs()
                || context.getDatabasePath(name).getParentFile().isDirectory());
        db = LocalDatabaseProvider.createIsolated(context, name);
    }

    @After public void tearDown() {
        db.close();
        context.deleteDatabase(name);
    }

    @Test public void recordDataPersistsAndFiltersByCategory() {
        long work = db.getCategories().get(0).id;
        long study = db.addCategory("数据库练习");
        db.saveRecord(null, work, "项目会议", "讨论进度");
        db.saveRecord(null, study, "SQLite", "学习事务");
        db.close();
        db = LocalDatabaseProvider.createIsolated(context, name);
        assertEquals(2, db.getRecords(null, "").size());
        assertEquals("SQLite", db.getRecords(study, "").get(0).title);
        assertEquals(0, db.getRecords(work, "SQLite").size());
    }

    @Test public void recordEditDeleteAndConstraintsWork() {
        long first = db.getCategories().get(0).id;
        long second = db.getCategories().get(1).id;
        long id = db.saveRecord(null, first, "100%_done", "O'Reilly");
        long created = db.getRecords(first, "%_").get(0).createdAt;
        db.saveRecord(id, second, "已修改", "新内容");
        assertEquals(created, db.getRecords(second, "").get(0).createdAt);
        assertEquals(0, db.getRecords(null, "' OR 1=1 --").size());
        db.deleteRecord(id);
        assertEquals(0, db.getRecords(null, "").size());
        assertThrows(IllegalArgumentException.class, () -> db.saveRecord(null, first, "  ", ""));
        assertThrows(SQLiteConstraintException.class, () -> db.addCategory("工作"));
    }

    @Test public void sensorModulesBatchQueryAndRetentionAreIsolated() {
        List<SensorReading> samples = Arrays.asList(
                SensorReading.create("sensor", "temperature", 25.2, "°C", 1000, "s1", "{}"),
                SensorReading.create("sensor", "temperature", 25.6, "°C", 2000, "s1", "{}"),
                SensorReading.create("sensor", "light", 420, "lux", 1500, "s1", ""),
                SensorReading.create("other", "temperature", 99, "°C", 1800, "x", "")
        );
        db.saveSensorReadings(samples);
        assertEquals(2, db.getLatestSensorReadings("sensor", "temperature", 10).size());
        assertEquals(25.6, db.getLatestSensorReadings("sensor", "temperature", 1).get(0).value, 0.001);
        assertEquals(1, db.getSensorReadingsBetween("sensor", "temperature", 1000, 2000).size());
        assertEquals(2, db.deleteSensorReadingsBefore("sensor", 1800));
        assertEquals(1, db.getLatestSensorReadings("sensor", "temperature", 10).size());
        assertEquals(1, db.getLatestSensorReadings("other", "temperature", 10).size());
    }

    @Test public void invalidSensorBatchRollsBackCompletely() {
        List<SensorReading> batch = Arrays.asList(
                SensorReading.create("sensor", "pressure", 1000, "hPa", 1, "", ""),
                SensorReading.create("", "pressure", 1001, "hPa", 2, "", "")
        );
        assertThrows(IllegalArgumentException.class, () -> db.saveSensorReadings(batch));
        assertEquals(0, db.getLatestSensorReadings("sensor", "pressure", 10).size());
        assertThrows(IllegalArgumentException.class,
                () -> db.saveSensorReading(SensorReading.create("sensor", "x", Double.NaN, "", 1, "", "")));
    }

    @Test public void poiBatchUpsertsAndQueriesByModuleFilters() {
        PoiData museum = poi("museum-1", "Shot Tower Museum", "MUSEUM", 7.0, -37.81, 144.96);
        PoiData beach = poi("beach-1", "Brighton Beach", "BEACH", 8.2, -37.91, 144.98);
        db.upsertPois(Arrays.asList(museum, beach));
        assertEquals(2, db.countPois());
        assertEquals(2, db.getAllPois().size());
        assertEquals("Shot Tower Museum", db.getPoiById("museum-1").name);
        assertEquals(1, db.getPois("MUSEUM", "Melbourne", "tower", 20).size());
        assertEquals(0, db.getPois("BEACH", "Melbourne", "tower", 20).size());

        PoiData updated = poi("museum-1", "Updated Museum", "MUSEUM", 9.0, -37.81, 144.96);
        db.upsertPois(Arrays.asList(updated));
        assertEquals(2, db.countPois());
        assertEquals(9.0, db.getPoiById("museum-1").baseScore, 0.001);

        PoiData landmark = poi("museum-1", "Same place, second category", "LANDMARK", 6.0, -37.81, 144.96);
        db.upsertPois(Arrays.asList(landmark));
        assertEquals(3, db.countPois());
        assertEquals(1, db.getPois("MUSEUM", "Melbourne", "", 20).size());
        assertEquals(1, db.getPois("LANDMARK", "Melbourne", "", 20).size());
        assertThrows(IllegalArgumentException.class,
                () -> db.getPois(null, null, null, 0));
    }

    @Test public void versionOneDatabaseMigratesWithoutLosingRecords() {
        db.close();
        context.deleteDatabase(name);
        SQLiteDatabase versionOne = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null);
        versionOne.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(trim(name)) BETWEEN 1 AND 30))");
        versionOne.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT, "
                + "title TEXT NOT NULL CHECK(length(trim(title)) BETWEEN 1 AND 100), "
                + "content TEXT NOT NULL CHECK(length(content) <= 10000), "
                + "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        versionOne.execSQL("INSERT INTO categories(id, name) VALUES(1, '旧分类')");
        versionOne.execSQL("INSERT INTO records(category_id, title, content, created_at, updated_at) "
                + "VALUES(1, '旧记录', '迁移时必须保留', 10, 20)");
        versionOne.setVersion(1);
        versionOne.close();

        db = LocalDatabaseProvider.createIsolated(context, name);
        assertEquals("旧记录", db.getRecords(null, "").get(0).title);
        db.saveSensorReading(SensorReading.create("sensor", "temperature", 24.5, "°C", 30, "", ""));
        assertEquals(1, db.getLatestSensorReadings("sensor", "temperature", 10).size());
        db.upsertPois(Arrays.asList(poi("after-migration", "迁移后的地点", "LANDMARK", 8.0, -37.8, 144.9)));
        assertEquals(1, db.countPois());
    }

    private static PoiData poi(String id, String name, String category, double score,
                               double latitude, double longitude) {
        return new PoiData(id, name, score, category, "MIXED", latitude, longitude,
                60, "09:00", "17:00", false, "Melbourne", "Test address", "");
    }
}
