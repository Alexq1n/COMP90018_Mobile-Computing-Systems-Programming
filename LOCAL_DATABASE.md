# 多模块本地数据库

应用使用一个共享的 SQLite 文件 `classified_data.db`，文件位于应用私有的 `databases` 目录。数据不需要登录或网络；卸载应用或清除应用数据时会被删除。

## 模块结构

- `:core:database`：数据库表、升级迁移、数据模型和稳定接口。
- `:feature:sensor`：Sensor 模块的数据仓库示例，只调用 `SensorDataStore`，不直接写 SQL。
- `:feature:poi`：把 `assets/pois.json` 导入数据库，并提供地点查询接口。
- `:feature:cloud`：Firebase Authentication 账号与 Firestore 用户偏好接口。
- `:app`：现有分类记录页面，只调用 `CategoryRecordStore`。

本地业务模块共享同一个进程级数据库实例。功能模块不要关闭 `LocalDatabaseProvider` 返回的实例，也不要依赖 SQLite 表名。

云端模块不写入 SQLite。Firebase Authentication 管理邮箱和密码，Firestore 使用
`users/{uid}` 与 `users/{uid}/preferences/default` 保存用户资料和偏好。密码不会写入
Firestore、本地数据库或日志。

## Firebase 用户接口

```kotlin
val cloud = FirebaseCloudUserRepository()

cloud.register(email, password, callback)
cloud.signIn(email, password, callback)
cloud.savePreferences(preferences, callback)
cloud.loadPreferences(callback)
cloud.signOut()
```

当前 Android 应用包名为 `com.example.localdatebase`，Firebase 项目为
`android-database-4aa91`。安全规则源文件位于项目根目录 `firestore.rules`；发布后，
每个登录用户只能访问自己的 `users/{uid}` 文档及其偏好子集合。

## 给新模块预留的入口

模块先依赖数据库核心：

```groovy
dependencies {
    implementation project(':core:database')
}
```

分类记录模块使用：

```java
CategoryRecordStore records = LocalDatabaseProvider.records(context);
```

Sensor、图表、分析或上传模块使用：

```java
SensorDataStore sensors = LocalDatabaseProvider.sensors(context);
```

所有数据库调用都应在后台线程执行。若增加一种新的业务数据，可在 `:core:database` 中新增对应的数据模型和接口，在 `SQLiteLocalDataStore` 中实现，再由 `LocalDatabaseProvider` 暴露窄接口。

## Sensor 模块示例

Sensor 模块已经提供 `SensorRepository`，它固定使用模块标识 `feature.sensor`，防止不同模块的数据混在一起：

```java
SensorRepository repository = new SensorRepository(context);

repository.save(
        "temperature",
        24.6,
        "°C",
        System.currentTimeMillis(),
        "session-001",
        "{\"source\":\"ambient\"}"
);

List<SensorReading> latest = repository.latest("temperature", 100);
```

批量采样可调用 `saveBatch`，整个批次在一个事务中写入；任意一项不合法时不会只保存一部分。查询接口支持“最近 N 条”和半开时间区间 `[开始时间, 结束时间)`。

## 分类和保留时间

`sensor_readings` 使用这些字段分类数据：

- `owner_module`：数据所属模块，例如 `feature.sensor`。
- `sensor_type`：传感器类型，例如 `temperature`、`light`。
- `session_id`：一次采集会话或任务的标识。
- `recorded_at`：Unix 毫秒时间戳。
- `metadata`：模块自己的 JSON 扩展字段。

按保留天数主动清理：

```java
long cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
int deleted = repository.deleteOlderThan(cutoff);
```

可把这段清理逻辑放入 Android `WorkManager` 的每日任务中。SQLite 不会根据某一列自动过期，应用需要按计划调用删除接口。

## 数据升级

数据库版本先从 1 升到 2，并新增了 `sensor_readings` 表和索引；版本 3 新增了 `pois` 表，当前版本 4 允许同一个地点 ID 属于不同分类。逐级升级会保留原有 `categories`、`records`、传感器和 POI 数据。

## POI JSON 导入与调用

App 首次进入页面时会在后台调用：

```kotlin
val repository = PoiRepository(context)
repository.importBundledPois()
```

导入使用 JSON 的 `id + category` 作为联合主键。这样源文件里同一地点属于多个分类时不会丢失，同时重复导入同一个文件也不会增加重复记录。查询示例：

```kotlin
// 全部博物馆，按评分从高到低，最多 50 条
val museums = repository.find("MUSEUM", "Melbourne", "", 50)

// 名称、地址或描述中包含 tower 的地点
val matches = repository.find(null, "Melbourne", "tower", 50)

// 按 JSON ID 精确读取
val poi = repository.byId(poiId)

// 当前数据库的 POI 数量
val count = repository.count()
```

这些调用都应放在后台线程中。地图、路线规划和推荐模块只需依赖 `:feature:poi`，无需访问表名或编写 SQL。

## 构建和测试

在 Android Studio 同步 Gradle 后运行 `app`，或在项目终端执行：

```powershell
.\gradlew.bat :core:database:testDebugUnitTest :app:testDebugUnitTest :feature:sensor:assembleDebug :app:assembleDebug
```

数据库测试覆盖分类记录、模块隔离、批量事务、时间范围、过期清理、非法数据回滚，以及版本 1 到版本 2 的无损升级。

## RoamMate 行程算法整合

`:backend` 保留 `zewen/backend-logic` 分支的行程规划算法和数据模型。`:feature:planner` 负责连接算法、SQLite POI 数据与 Firebase，避免算法模块直接依赖数据库实现。

```kotlin
val poiRepository = LocalPoiRepository(context)
val engine = TripPlannerEngine(poiRepository)
```

`LocalPoiRepository` 实现后端定义的 `IPoiRepository`，把本地 `PoiData` 转换为算法使用的 `POI`。当前本地数据没有预算字段，因此转换时使用 `BudgetLevel.MEDIUM` 作为默认值。

登录后，规划资料和行程分别保存到：

```text
users/{uid}/plannerProfile/default
users/{uid}/trips/{tripId}
```

App 的“RoamMate 行程算法”卡片可生成一日墨尔本示例行程，并将规划资料和结果保存到 Firebase。发布项目根目录的 `firestore.rules` 后才能写入这两个新路径。

整合验证命令：

```powershell
.\gradlew.bat --no-daemon :backend:testDebugUnitTest :feature:planner:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```
