package com.example.localdatebase

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.localdatebase.cloud.CloudResultCallback
import com.example.localdatebase.cloud.CloudUserSession
import com.example.localdatebase.cloud.FirebaseCloudUserRepository
import com.example.localdatebase.cloud.UserPreferences

class CloudAccountViewModel : ViewModel() {
    private val repository = FirebaseCloudUserRepository()
    private val main = Handler(Looper.getMainLooper())

    var session by mutableStateOf<CloudUserSession?>(repository.currentUser())
        private set
    var email by mutableStateOf(session?.email ?: "")
    var password by mutableStateOf("")
    var theme by mutableStateOf("system")
    var language by mutableStateOf("zh-CN")
    var preferredCategories by mutableStateOf("MUSEUM, NATURE")
    var notificationsEnabled by mutableStateOf(true)
    var busy by mutableStateOf(false)
        private set
    var feedback by mutableStateOf(if (session == null) "尚未登录" else "已登录：${session?.email}")
        private set

    init {
        if (session != null) loadPreferences(false)
    }

    fun register() = authenticate(true)
    fun signIn() = authenticate(false)

    private fun authenticate(register: Boolean) {
        if (busy) return
        busy = true
        feedback = if (register) "正在注册…" else "正在登录…"
        val callback = callback<CloudUserSession> { user ->
            session = user
            email = user.email
            password = ""
            feedback = if (register) "注册成功，用户资料已创建" else "登录成功"
            loadPreferences(false)
        }
        try {
            if (register) repository.register(email, password, callback)
            else repository.signIn(email, password, callback)
        } catch (error: Exception) {
            finishError(error)
        }
    }

    fun signOut() {
        repository.signOut()
        session = null
        password = ""
        busy = false
        feedback = "已退出云端账号"
    }

    fun savePreferences() {
        if (busy) return
        busy = true
        feedback = "正在保存用户偏好…"
        val categories = preferredCategories.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        try {
            repository.savePreferences(
                UserPreferences(theme, language, categories, notificationsEnabled),
                callback { feedback = "用户偏好已保存到 Firestore" }
            )
        } catch (error: Exception) {
            finishError(error)
        }
    }

    fun loadPreferences(showMessage: Boolean = true) {
        if (busy && showMessage) return
        busy = true
        if (showMessage) feedback = "正在读取用户偏好…"
        try {
            repository.loadPreferences(callback { preferences ->
                theme = preferences.theme
                language = preferences.language
                preferredCategories = preferences.preferredCategories.joinToString(", ")
                notificationsEnabled = preferences.notificationsEnabled
                feedback = if (showMessage) "已从 Firestore 读取用户偏好" else "已登录：${session?.email}"
            })
        } catch (error: Exception) {
            finishError(error)
        }
    }

    private fun <T> callback(onSuccess: (T) -> Unit) = object : CloudResultCallback<T> {
        override fun onSuccess(value: T) = main.post {
            busy = false
            onSuccess(value)
        }.let { Unit }

        override fun onError(error: Exception) = main.post {
            finishError(error)
        }.let { Unit }
    }

    private fun finishError(error: Exception) {
        busy = false
        val raw = error.localizedMessage ?: "请检查 Firebase 配置和网络"
        feedback = when {
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Firestore 拒绝访问：请发布 users/{uid} 私有规则"
            raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "邮箱密码登录尚未在 Firebase Authentication 启用"
            else -> "云端操作失败：$raw"
        }
    }
}
