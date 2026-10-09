
package com.group5.roammate.sensor

/**
 * Temporary implementation.
 * Replace with the database implementation later.
 */
class DatabaseStepHistoryStorage : StepHistoryStorage {

    override suspend fun loadStepHistory(): List<StepData> {
        // load data from database
        return emptyList()
    }

    override suspend fun saveStepHistory(record: StepData) {
        // save data to database
    }
}
