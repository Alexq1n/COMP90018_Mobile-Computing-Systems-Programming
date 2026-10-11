package com.group5.roammate.sensor

import android.app.Application

class RoamMateApp : Application() {

    val sensorRepository: SensorRepository by lazy {
        SensorRepository(
            context = applicationContext,

            // [NEW] Temporary storage until database integration
            stepHistoryStorage = DatabaseStepHistoryStorage()
        )
    }
}