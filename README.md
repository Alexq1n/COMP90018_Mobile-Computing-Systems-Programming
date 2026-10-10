# LocalDatebase / RoamMate 数据层
读取景点使用 PoiRepository：
val repository = PoiRepository(context)

// 首次使用前，把 pois.json 导入本地 SQLite
repository.importBundledPois()

// 查询墨尔本全部景点，最多返回 1000 条
val pois = repository.find(
    "",           // 分类为空：不限分类
    "Melbourne",  // 城市
    "",           // 关键词为空：不限关键词
    1000          // 最大返回数量
)

// 读取结果
pois.forEach { poi ->
    println(poi.name)
    println(poi.address)
    println("${poi.latitude}, ${poi.longitude}")
}
按分类读取：
val museums = repository.find(
    "MUSEUM",
    "Melbourne",
    "",
    100
)
按关键词读取：
val matches = repository.find(
    "",
    "Melbourne",
    "tower",
    100
)
按景点 ID 读取一条：
val poi = repository.byId(poiId)

poi?.let {
    println(it.name)
    println(it.description)
}
这些操作要放在后台线程中，例如：
viewModelScope.launch(Dispatchers.IO) {
    val repository = PoiRepository(context)
    repository.importBundledPois()
    val pois = repository.find("", "Melbourne", "", 1000)

    withContext(Dispatchers.Main) {
        // 在这里更新界面
    }
}
接口文件在 [PoiRepository.java](C:/Users/ROG/AndroidStudioProjects/LocalDatebase/feature/poi/src/main/java/com/example/localdatebase/poi/PoiRepository.java)。
