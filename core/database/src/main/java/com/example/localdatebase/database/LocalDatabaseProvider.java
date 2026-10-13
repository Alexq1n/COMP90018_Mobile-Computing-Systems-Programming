package com.example.localdatebase.database;

import android.content.Context;

/** Process-wide database entry point. Feature modules should keep and reuse this interface. */
public final class LocalDatabaseProvider {
    private static volatile LocalDataStore instance;

    private LocalDatabaseProvider() {}

    public static LocalDataStore get(Context context) {
        LocalDataStore current = instance;
        if (current == null) {
            synchronized (LocalDatabaseProvider.class) {
                current = instance;
                if (current == null) {
                    current = new SQLiteLocalDataStore(context, SQLiteLocalDataStore.DATABASE_NAME);
                    instance = current;
                }
            }
        }
        return current;
    }

    public static CategoryRecordStore records(Context context) {
        return get(context);
    }

    public static SensorDataStore sensors(Context context) {
        return get(context);
    }

    public static PoiDataStore pois(Context context) {
        return get(context);
    }

    /** Creates an isolated database, intended for tests and tools rather than feature modules. */
    public static LocalDataStore createIsolated(Context context, String databaseName) {
        return new SQLiteLocalDataStore(context, databaseName);
    }
}
