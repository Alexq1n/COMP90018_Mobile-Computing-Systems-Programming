# LocalDatebase / RoamMate 数据层

这是一个 Android 多模块示例项目，包含本地 SQLite 分类存储、Sensor 数据、墨尔本 POI、Firebase Authentication、Cloud Firestore 用户偏好，以及 RoamMate 行程规划算法。

当前代码位于 `database` 分支。Android 包名为 `com.example.localdatebase`，Firebase 项目为 `android-database-4aa91`。

## 1. 模块和数据流

| 模块 | 用途 | 主要入口 |
| --- | --- | --- |
| `:core:database` | SQLite 表、迁移、数据模型和接口 | `LocalDatabaseProvider` |
| `:feature:poi` | 导入与查询 `pois.json` | `PoiRepository` |
| `:feature:sensor` | Sensor 数据保存、查询和过期清理 | `SensorRepository` |
| `:feature:cloud` | Firebase 注册、登录和普通用户偏好 | `FirebaseCloudUserRepository` |
| `:backend` | 行程生成和动态调整算法 | `TripPlannerEngine` |
| `:feature:planner` | 连接本地 POI、算法和 Firebase | `LocalPoiRepository`、`FirebasePlannerRepository` |
| `:app` | Compose 演示界面和 ViewModel | `MainActivity` |

```text
pois.json ──> PoiRepository ──> SQLite pois
                                      │
                                      ▼
UI ──> PlannerViewModel ──> LocalPoiRepository ──> TripPlannerEngine
 │                                                       │
 └──────── Firebase Authentication / Firestore <─────────┘
```

本地数据保存在应用私有目录中的 `classified_data.db`。关闭应用或重启设备后数据仍保留；卸载应用或清除应用数据会删除本地数据库。


## 5. POI 景点接口

正式景点数据文件：

```text
feature/poi/src/main/assets/pois.json
```

### 5.1 导入 JSON

```java
PoiRepository repository = new PoiRepository(context);
int importedCount = repository.importBundledPois();
```

导入操作可以重复调用。相同景点会更新而不会无限增加重复数据。当前 JSON 大约包含 135 个墨尔本地点。

### 5.2 查询景点

| 方法 | 作用 |
| --- | --- |
| `byId(id)` | 按 JSON 景点 ID 查询一条记录 |
| `find(category, city, keyword, limit)` | 按分类、城市和关键词组合查询 |
| `count()` | 返回本地 POI 总数 |

```java
PoiRepository repository = new PoiRepository(context);

// category、city、keyword 使用空字符串或 null 表示不限制。
List<PoiData> museums = repository.find("MUSEUM", "Melbourne", "", 50);
List<PoiData> towerMatches = repository.find("", "Melbourne", "tower", 50);
PoiData onePoi = repository.byId("景点 ID");
int total = repository.count();
```

关键词会匹配景点名称、地址或描述。`limit` 控制最多返回多少条。

一次查询返回的是“所有符合条件的景点行”，每个返回的 `PoiData` 都包含该景点的完整本地字段：

- `id`、`name`、`baseScore`、`category`、`environment`
- `latitude`、`longitude`
- `recommendedVisitDuration`
- `openTime`、`closeTime`
- `filler`、`city`、`address`、`description`

完整字段不代表读取数据库中的全部景点；是否读取全部行取决于筛选条件和 `limit`。

### 5.3 给规划算法使用

`LocalPoiRepository` 实现 `IPoiRepository`，把 SQLite 的 `PoiData` 转换为算法模型 `POI`。

```kotlin
val plannerPois = LocalPoiRepository(context)

val all = plannerPois.getPOIsByCity("Melbourne")
val museums = plannerPois.getPOIsByCategory("Melbourne", POICategory.MUSEUM)
val nearby = plannerPois.getPOIsWithinRadius(
    latitude = -37.8136,
    longitude = 144.9631,
    radiusMeters = 2_000.0,
    city = "Melbourne"
)
val one = plannerPois.getPOIById("景点 ID")
```

规划模型没有 `address` 字段，因此地址仍保留在 `PoiData` 中，但不会传给 `TripPlannerEngine`。本地 JSON 没有预算字段，转换时默认使用 `BudgetLevel.MEDIUM`。类似 `26:00` 的跨午夜关门时间会在规划接口层转换为当日 `23:59`，避免时间解析失败。

## 6. Sensor 数据接口

推荐使用 `SensorRepository`。它固定使用模块标识 `feature.sensor`，避免不同功能模块的数据混在一起。

### 6.1 保存单条数据

```java
SensorRepository sensors = new SensorRepository(context);

long id = sensors.save(
        "temperature",
        24.6,
        "°C",
        System.currentTimeMillis(),
        "session-001",
        "{\"source\":\"ambient\"}"
);
```

### 6.2 批量保存

```java
List<SensorRepository.SensorSample> batch = Arrays.asList(
        new SensorRepository.SensorSample(
                "temperature", 24.6, "°C", System.currentTimeMillis(),
                "session-001", "{}"
        ),
        new SensorRepository.SensorSample(
                "temperature", 24.8, "°C", System.currentTimeMillis() + 1000,
                "session-001", "{}"
        )
);
sensors.saveBatch(batch);
```

批量写入使用事务；任意一条数据不合法时整批不会只保存一部分。

### 6.3 查询与过期清理

```java
List<SensorReading> latest = sensors.latest("temperature", 100);

long from = System.currentTimeMillis() - 60L * 60 * 1000;
long to = System.currentTimeMillis();
List<SensorReading> lastHour = sensors.between("temperature", from, to);

long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
int deleted = sensors.deleteOlderThan(sevenDaysAgo);
```

`between()` 使用半开区间 `[fromInclusive, toExclusive)`。SQLite 不会自行删除过期数据；可使用 WorkManager 每日调用 `deleteOlderThan()`。

如果新模块需要自己的隔离标识，可以直接使用 `SensorDataStore` 并在 `SensorReading.ownerModule` 中写入独立模块名。

## 7. Firebase 用户和普通偏好接口

入口：`CloudUserRepository cloud = new FirebaseCloudUserRepository()`。

| 方法 | 作用 |
| --- | --- |
| `currentUser()` | 返回当前登录用户；未登录返回 `null` |
| `register(email, password, callback)` | 创建 Firebase Authentication 用户 |
| `signIn(email, password, callback)` | 邮箱密码登录 |
| `signOut()` | 退出登录 |
| `savePreferences(preferences, callback)` | 保存当前用户偏好 |
| `loadPreferences(callback)` | 读取当前用户偏好 |

```java
CloudUserRepository cloud = new FirebaseCloudUserRepository();

cloud.signIn(email, password, new CloudResultCallback<CloudUserSession>() {
    @Override public void onSuccess(CloudUserSession user) {
        // user.uid 和 user.email
    }

    @Override public void onError(Exception error) {
        // 在 UI 上显示 error.getMessage()
    }
});
```

保存偏好：

```java
UserPreferences preferences = new UserPreferences(
        "dark",
        "zh-CN",
        Arrays.asList("MUSEUM", "PARK"),
        true
);

cloud.savePreferences(preferences, new CloudResultCallback<Void>() {
    @Override public void onSuccess(Void ignored) { }
    @Override public void onError(Exception error) { }
});
```

Firestore 路径：

```text
users/{uid}
users/{uid}/preferences/default
```

邮箱和密码交给 Firebase Authentication。原始密码不会写入 SQLite、Firestore 或日志。除 `currentUser()` 和 `signOut()` 外，Firebase 操作通过回调异步返回；未登录时调用用户数据接口会抛出“请先登录云端账号”。

## 8. 行程规划接口

### 8.1 生成行程

```kotlin
val poiImporter = PoiRepository(context)
poiImporter.importBundledPois()

val engine = TripPlannerEngine(LocalPoiRepository(context))
val profile = FirebasePlannerRepository.defaultProfile()

val itinerary = engine.generateInitialTrip(
    tripId = UUID.randomUUID().toString(),
    destination = "Melbourne",
    startDate = "2026-10-10",
    numberOfDays = 1,
    userProfile = profile,
    startingLocation = Coordinates(-37.8136, 144.9631),
    dailyStartTime = "09:00",
    dailyEndTime = "18:00",
    weatherStatus = WeatherStatus.SUNNY,
    includeFillers = true
)
```

生成操作是同步计算，建议在后台线程运行。结果 `Itinerary` 包含：

- `tripId`
- `days`
- `totalEstimatedCost`
- `totalExpectedValue`

每个 `DayItinerary` 包含日期和 `ItineraryPOI` 列表；每个地点包含 `startTime`、`endTime`、路程分钟数、预期价值和完整的算法 `POI`。

### 8.2 其他算法入口

```kotlin
val allPois = engine.getPOIsForDestination("Melbourne", includeFillers = false)
val score = engine.calculatePOIValue(poi, profile, WeatherStatus.SUNNY)

val options = engine.adjustTripState(
    currentItinerary = currentDay.pois,
    contextPayload = contextPayload,
    destination = "Melbourne",
    userProfile = profile,
    dailyEndTime = "18:00"
)
```

`adjustTripState()` 返回两个调整方案：体验优先和效率优先。

## 9. Firebase 规划偏好和行程接口

入口：`FirebasePlannerRepository`。这些接口要求 Firebase Authentication 已登录。

```kotlin
val cloudPlanner = FirebasePlannerRepository()

cloudPlanner.saveProfile(profile, callback)
cloudPlanner.loadProfile(callback)
cloudPlanner.saveTrip(itinerary, callback)
cloudPlanner.loadTrip(itinerary.tripId, callback)
```

回调形式：

```kotlin
val callback = object : PlannerResultCallback<Itinerary> {
    override fun onSuccess(value: Itinerary) {
        // 更新 Compose State 或 ViewModel
    }

    override fun onError(error: Exception) {
        // 显示错误
    }
}
```

Firestore 路径：

```text
users/{uid}/plannerProfile/default
users/{uid}/trips/{tripId}
```

行程文档同时保存汇总字段和完整 `payloadJson`。`tripId` 不能为空，也不能包含 `/`。

## 10. Firestore 安全规则

根目录的 `firestore.rules` 限制登录用户只能访问自己的 UID 路径。修改本地规则文件不会自动影响 Firebase；需要在 Firebase Console 的 Firestore Rules 页面发布，或使用 Firebase CLI 部署。

规则涵盖：

```text
users/{uid}
users/{uid}/preferences/{documentId}
users/{uid}/plannerProfile/{documentId}
users/{uid}/trips/{tripId}
```

不要把用户密码写入任何 Firestore 文档。其他团队成员运行此 App 时，会使用同一个 Firebase 项目，但每个登录用户只能读写自己的数据。

## 11. 新功能模块如何接入

1. 在新模块的 `build.gradle` 中依赖 `:core:database`，需要 POI 或云端能力时再依赖对应 feature 模块。
2. 通过 `LocalDatabaseProvider` 或 feature Repository 获取接口。
3. 在后台线程执行 SQLite 操作。
4. 通过 ViewModel 将结果暴露给 UI，避免在 `MainActivity` 中直接编写数据库逻辑。
5. 新增数据类型时，在 `:core:database` 新增模型和窄接口，在 `SQLiteLocalDataStore` 中实现，并通过 `LocalDatabaseProvider` 暴露。
6. 数据库结构变化时提高数据库版本并编写升级逻辑，避免删除用户已有数据。

更详细的本地数据库设计说明见 [`LOCAL_DATABASE.md`](LOCAL_DATABASE.md)。
