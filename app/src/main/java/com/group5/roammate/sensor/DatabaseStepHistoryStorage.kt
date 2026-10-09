
package com.group5.roammate.sensor
import android.util.Log
/**
 * Temporary implementation.
 * Replace with the database implementation later.
 */
class DatabaseStepHistoryStorage : StepHistoryStorage {

    override suspend fun loadStepHistory(): List<StepData> {
        // load data from database
        // Return emptyList() if database is empty
        Log.d("RoamMateSensor", "loadStepHistory() CALLED")
        return emptyList()
    }

    override suspend fun saveStepHistory(record: StepData) {
        // save data to database
        Log.d(
            "RoamMateSensor",
            "saveStepHistory() CALLED | steps=${record.steps}, timestamp=${record.timestamp}"
        )
    }
}
