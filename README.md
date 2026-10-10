# 如何读取景点数据

使用 `PoiRepository` 读取景点。首次使用前，先把项目内的 `pois.json` 导入本地 SQLite 数据库。

```kotlin
val repository = PoiRepository(context)

// 首次使用前，把 pois.json 导入本地 SQLite
repository.importBundledPois()

// 获取数据库中的全部景点，不需要筛选条件或 limit
val pois = repository.allPois

// pois 的类型是 List<PoiData>；每个对象包含该景点的全部字段
pois.forEach { poi ->
    println(poi.id)
    println(poi.name)
    println(poi.baseScore)
    println(poi.category)
    println(poi.environment)
    println(poi.latitude)
    println(poi.longitude)
    println(poi.recommendedVisitDuration)
    println(poi.openTime)
    println(poi.closeTime)
    println(poi.filler)
    println(poi.city)
    println(poi.address)
    println(poi.description)
}
```

Java 中调用同一个接口：

```java
List<PoiData> pois = repository.getAllPois();
```

## 按分类读取

```kotlin
val museums = repository.find(
    "MUSEUM",
    "Melbourne",
    "",
    100
)
```

## 按关键词读取

关键词会匹配景点名称、地址或描述。

```kotlin
val matches = repository.find(
    "",
    "Melbourne",
    "tower",
    100
)
```

## 按景点 ID 读取一条

```kotlin
val poi = repository.byId(poiId)

poi?.let {
    println(it.name)
    println(it.description)
}
```

## 在后台线程读取

数据库操作需要放在后台线程中：

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val repository = PoiRepository(context)
    repository.importBundledPois()
    val pois = repository.allPois

    withContext(Dispatchers.Main) {
        // 在这里使用 pois 更新界面
    }
}
```

景点原始 JSON 文件：

```text
feature/poi/src/main/assets/pois.json
```

接口实现文件：

```text
feature/poi/src/main/java/com/example/localdatebase/poi/PoiRepository.java
```
