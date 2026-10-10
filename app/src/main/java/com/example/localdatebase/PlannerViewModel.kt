package com.example.localdatebase

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.localdatebase.planner.FirebasePlannerRepository
import com.example.localdatebase.planner.LocalPoiRepository
import com.example.localdatebase.planner.PlannerResultCallback
import com.example.localdatebase.poi.PoiRepository
import com.roammate.logic.TripPlannerEngine
import com.roammate.logic.models.Coordinates
import com.roammate.logic.models.Itinerary
import com.roammate.logic.models.UserProfile
import com.roammate.logic.models.WeatherStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val poiImporter = PoiRepository(application)
    private val engine = TripPlannerEngine(LocalPoiRepository(application))
    private val cloud = FirebasePlannerRepository()
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var profile: UserProfile = FirebasePlannerRepository.defaultProfile()

    var itinerary by mutableStateOf<Itinerary?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var feedback by mutableStateOf("后端算法已连接到本地 POI 数据库")
        private set

    fun generateMelbourneTrip() {
        if (busy) return
        busy = true
        feedback = "正在读取本地 POI 并生成行程…"
        executor.execute {
            try {
                poiImporter.importBundledPois()
                val generated = engine.generateInitialTrip(
                    tripId = UUID.randomUUID().toString(),
                    destination = "Melbourne",
                    startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    numberOfDays = 1,
                    userProfile = profile,
                    startingLocation = Coordinates(-37.8136, 144.9631),
                    dailyStartTime = "09:00",
                    dailyEndTime = "18:00",
                    weatherStatus = WeatherStatus.SUNNY,
                    includeFillers = true
                )
                main.post {
                    itinerary = generated
                    busy = false
                    val poiCount = generated.days.sumOf { it.pois.size }
                    feedback = "行程已生成：${generated.days.size} 天，$poiCount 个地点"
                }
            } catch (error: Exception) {
                finishError(error)
            }
        }
    }

    fun saveToCloud() {
        val current = itinerary ?: run {
            feedback = "请先生成行程"
            return
        }
        if (busy) return
        busy = true
        feedback = "正在保存规划偏好…"
        try {
            cloud.saveProfile(profile, callback {
                feedback = "正在保存行程…"
                cloud.saveTrip(current, callback {
                    feedback = "规划偏好和行程已保存到 Firebase"
                })
            })
        } catch (error: Exception) {
            finishError(error)
        }
    }

    fun loadCloudProfile() {
        if (busy) return
        busy = true
        feedback = "正在读取云端规划偏好…"
        try {
            cloud.loadProfile(callback { loaded ->
                profile = loaded
                feedback = "已读取规划偏好：${loaded.interests.joinToString { it.name }}"
            })
        } catch (error: Exception) {
            finishError(error)
        }
    }

    private fun <T> callback(onSuccess: (T) -> Unit) = object : PlannerResultCallback<T> {
        override fun onSuccess(value: T) {
            main.post {
                busy = false
                onSuccess(value)
            }
        }

        override fun onError(error: Exception) = finishError(error)
    }

    private fun finishError(error: Exception) {
        main.post {
            busy = false
            feedback = "规划操作失败：${error.localizedMessage ?: "请重试"}"
        }
    }

    override fun onCleared() {
        executor.shutdown()
    }
}
