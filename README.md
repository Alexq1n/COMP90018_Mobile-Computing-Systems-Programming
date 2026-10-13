# 云端与本地数据库接口及使用方式

当前项目把数据库分成两部分：

- **本地 SQLite 数据库**：景点、Sensor 数据、模块分类记录，数据只保存在每台设备中。
- **Firebase 云端数据库**：用户资料、偏好和行程，不同设备登录同一账号后可以访问。

## 一、本地数据库接口

统一入口：

```text
core/database/src/main/java/com/example/localdatebase/database/LocalDatabaseProvider.java
```

### 1. 景点接口

推荐使用：

```text
feature/poi/src/main/java/com/example/localdatebase/poi/PoiRepository.java
```

```kotlin
private val poiRepository by lazy {
    PoiRepository(applicationContext)
}

if (poiRepository.count() == 0) {
    poiRepository.importBundledPois()
}

val allPois = poiRepository.getAllPois()
val poi = poiRepository.byId(poiId)

val results = poiRepository.find(
    "MUSEUM",     // 分类；不限制传 null
    "Melbourne",  // 城市；不限制传 null
    "museum",     // 匹配名称、地址和描述
    20
)
```

`PoiData` 包含：

```kotlin
poi.id
poi.name
poi.category
poi.environment
poi.latitude
poi.longitude
poi.address
poi.description
poi.openTime
poi.closeTime
poi.baseScore
```

底层接口：

```text
core/database/src/main/java/com/example/localdatebase/database/PoiDataStore.java
```

### 2. Planner 调用景点接口

```text
feature/planner/src/main/java/com/example/localdatebase/planner/LocalPoiRepository.kt
```

```kotlin
val repository = LocalPoiRepository(applicationContext)

val allMelbournePois = repository.getPOIsByCity("Melbourne")

val museums = repository.getPOIsByCategory(
    "Melbourne",
    POICategory.MUSEUM
)

val nearby = repository.getPOIsWithinRadius(
    latitude = -37.8136,
    longitude = 144.9631,
    radiusMeters = 3000.0,
    city = "Melbourne"
)

val poi = repository.getPOIById(poiId)
```

这个接口返回后端规划算法使用的 `POI` 类型，可以直接传给行程生成模块。

### 3. Sensor 数据接口

```text
core/database/src/main/java/com/example/localdatebase/database/SensorDataStore.java
```

```kotlin
val sensorStore = LocalDatabaseProvider.sensors(applicationContext)

val reading = SensorReading.create(
    "sensor-module",
    "accelerometer",
    9.81,
    "m/s²",
    System.currentTimeMillis(),
    "session-001",
    """{"axis":"z"}"""
)

val databaseId = sensorStore.saveSensorReading(reading)

val latest = sensorStore.getLatestSensorReadings(
    "sensor-module",
    "accelerometer",
    100
)

val readings = sensorStore.getSensorReadingsBetween(
    "sensor-module",
    "accelerometer",
    startTime,
    endTime
)

val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
sensorStore.deleteSensorReadingsBefore("sensor-module", cutoff)
```

### 4. 通用分类存储接口

```text
core/database/src/main/java/com/example/localdatebase/database/CategoryRecordStore.java
```

```kotlin
val records = LocalDatabaseProvider.records(applicationContext)
val categoryId = records.addCategory("weather")

val recordId = records.saveRecord(
    null,
    categoryId,
    "Melbourne weather",
    """{"temperature":18}"""
)

val result = records.getRecords(categoryId, "Melbourne")
records.deleteRecord(recordId)
```

数据库文件名是 `classified_data.db`。

建议在 `Dispatchers.IO` 中执行本地数据库操作：

```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    val results = poiRepository.getAllPois()

    withContext(Dispatchers.Main) {
        // 更新 UI
    }
}
```

## 二、Firebase 云端接口

### 1. 用户注册、登录和偏好

稳定接口：

```text
feature/cloud/src/main/java/com/example/localdatebase/cloud/CloudUserRepository.java
```

实际实现：

```text
feature/cloud/src/main/java/com/example/localdatebase/cloud/FirebaseCloudUserRepository.java
```

```kotlin
private val cloudUserRepository by lazy {
    FirebaseCloudUserRepository()
}
```

注册：

```kotlin
cloudUserRepository.register(
    "User Name",
    "user@example.com",
    "password123",
    object : CloudResultCallback<CloudUserSession> {
        override fun onSuccess(value: CloudUserSession) {
            val uid = value.uid
            val email = value.email
            val name = value.displayName
        }

        override fun onError(error: Exception) {
            Log.e("Firebase", "Registration failed", error)
        }
    }
)
```

登录：

```kotlin
cloudUserRepository.signIn(
    "user@example.com",
    "password123",
    object : CloudResultCallback<CloudUserSession> {
        override fun onSuccess(value: CloudUserSession) {
            // 登录成功
        }

        override fun onError(error: Exception) {
            // 显示错误
        }
    }
)
```

当前用户、退出登录和更新用户名：

```kotlin
val session = cloudUserRepository.currentUser()
val uid = session?.uid

cloudUserRepository.signOut()

cloudUserRepository.updateDisplayName(
    "New Name",
    object : CloudResultCallback<CloudUserSession> {
        override fun onSuccess(value: CloudUserSession) {
            // 更新成功
        }

        override fun onError(error: Exception) {
            // 更新失败
        }
    }
)
```

保存用户偏好：

```kotlin
val preferences = UserPreferences(
    "dark",
    "zh-CN",
    listOf("MUSEUM", "PARK", "FOOD"),
    true
)

cloudUserRepository.savePreferences(
    preferences,
    object : CloudResultCallback<Void> {
        override fun onSuccess(value: Void?) {
            // 保存成功
        }

        override fun onError(error: Exception) {
            // 保存失败
        }
    }
)
```

读取用户偏好：

```kotlin
cloudUserRepository.loadPreferences(
    object : CloudResultCallback<UserPreferences> {
        override fun onSuccess(value: UserPreferences) {
            val theme = value.theme
            val language = value.language
            val categories = value.preferredCategories
        }

        override fun onError(error: Exception) {
            // 读取失败
        }
    }
)
```

Firestore 存储位置：

```text
users/{uid}
users/{uid}/preferences/default
```

密码不会保存到 Firestore，而是交给 Firebase Authentication 管理。

### 2. 云端行程接口

```text
feature/planner/src/main/java/com/example/localdatebase/planner/FirebasePlannerRepository.kt
```

```kotlin
private val plannerRepository by lazy {
    FirebasePlannerRepository()
}
```

保存行程：

```kotlin
plannerRepository.saveTrip(
    itinerary,
    object : PlannerResultCallback<Unit> {
        override fun onSuccess(value: Unit) {
            // 保存成功
        }

        override fun onError(error: Exception) {
            // 保存失败
        }
    }
)
```

读取指定行程：

```kotlin
plannerRepository.loadTrip(
    tripId,
    object : PlannerResultCallback<Itinerary> {
        override fun onSuccess(value: Itinerary) {
            // 使用完整行程
        }

        override fun onError(error: Exception) {
            // 行程不存在或读取失败
        }
    }
)
```

读取当前用户的全部行程：

```kotlin
plannerRepository.listTrips(
    object : PlannerResultCallback<List<SavedTripSummary>> {
        override fun onSuccess(value: List<SavedTripSummary>) {
            // 更新 Saved Trips 页面
        }

        override fun onError(error: Exception) {
            // 读取失败
        }
    }
)
```

Firestore 存储位置：

```text
users/{uid}/plannerProfile/default
users/{uid}/trips/{tripId}
```

调用这些接口前，用户必须先登录。

## 三、其他成员如何接入

同一 GitHub 项目的成员拉取整合后的分支。模块配置如下：

```kotlin
// settings.gradle.kts
include(
    ":app",
    ":backend",
    ":core:database",
    ":feature:cloud",
    ":feature:planner",
    ":feature:poi"
)
```

功能模块的 `build.gradle.kts` 按需要加入：

```kotlin
dependencies {
    implementation(project(":core:database"))
    implementation(project(":feature:poi"))
    implementation(project(":feature:cloud"))
    implementation(project(":feature:planner"))
}
```

然后导入：

```kotlin
import com.example.localdatebase.database.LocalDatabaseProvider
import com.example.localdatebase.database.SensorReading
import com.example.localdatebase.poi.PoiRepository
import com.example.localdatebase.planner.LocalPoiRepository
import com.example.localdatebase.cloud.FirebaseCloudUserRepository
import com.example.localdatebase.planner.FirebasePlannerRepository
```

如果是完全独立的 Android 项目，需要复制：

```text
backend/
core/database/
feature/poi/
feature/cloud/
feature/planner/
```

并修改其 `settings.gradle.kts` 和模块依赖。

云端必要条件：

- 如果对方项目的 `applicationId` 也是 `com.group5.roammate`，可以使用同一个正式 `google-services.json`。
- 如果包名不同，必须在同一个 Firebase 项目中注册新的 Android 应用，并下载对应的 `google-services.json`。
- 目前项目中的 Firebase 配置仍是临时兼容配置；正式整合前应在 Firebase 注册 `com.group5.roammate`。
