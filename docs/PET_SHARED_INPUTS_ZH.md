# 宠物共享天气与传感器联调

本轮由 Jie 的宠物模块消费现有公共输入。天气、传感器、数据库、行程模块的实现均未修改。`MainActivity.kt` 仅增加两处宠物参数接线：初始化传入现有 `WeatherService` 和 `SensorRepository`；Pet 页面传入同一个 repository 和现有活动识别权限申请回调。

## 输入与个人贡献

| 公共输入 | 提供模块 | 宠物模块的工作 |
| --- | --- | --- |
| `weatherService.currentWeather` | Leyan 天气模块 | `PetSharedWeatherAdapter` 转换天气类型与温度，驱动衣服、背景、提示 |
| `sensorRepository.locationFlow` / `getCurrentLocation()` | Alex 定位模块 | 天气刷新使用共享 GPS，取消宠物自己的城市选择和 GPS 监听 |
| `sensorRepository.stepEvents` | Alex 步行模块 | `PetSharedMotionSession` 检查事件时间；`PetMotionEngine` 判断走路与停止 |
| `sensorRepository.stepCountFlow` | Alex 计步模块 | 没有 step detector 时使用累计步数；首次值只建立基线 |
| `sensorRepository.shakeEvents` | Alex 摇一摇模块 | 仅在 Pet 页面恢复到前台时触发天气与行程提醒 |

Trip 的 `currentWeatherSummaryState` 和宠物现在来自同一份 `WeatherService.currentWeather`。宠物读取结构化 `CurrentWeather`，避免反向解析温度字符串。没有额外天气定时请求；宠物的刷新按钮调用已有 `WeatherService.getCurrentWeather()`，返回值也会更新 Trip。旧 Open-Meteo 客户端仍保留在仓库，但当前应用接线不再调用它。

## 天气缺字段与缓存

现有 `CurrentWeather` 只有天气类型和温度。宠物不修改队友的模型，也不填造缺失数据：

- 风速使用未知值，防风衣判断暂不可用；未知风速不阻断雨衣、雪天或低温冬装。
- 昼夜标记未知，太阳帽自动判断暂不可用；晴天背景不绘制确定的太阳或月亮。
- 上游观测时间未知，`observedAtMillis` 保持 0；另用 `receivedAtMillis` 记录本地收到新响应的时刻。
- 45 分钟限制表示本地响应年龄，不代表已验证气象观测年龄。后续可由 Leyan 扩展观测时间、风速和昼夜字段。
- 同一个 LiveData 响应重放不会刷新缓存时间；内容相同的新请求响应可以更新接收时间。
- 未知天气、缺温度、无效温度或过期结果不会强制换装。手动刷新失败时显示已有缓存，缓存过期后回退至个人服装。

## 传感器与生命周期

宠物不再注册步行、加速度或 GPS 监听。实际硬件和后台计步服务仍由 `MainActivity`、`SensorService` 和 `SensorRepository` 管理。

Pet 页使用 `repeatOnLifecycle(RESUMED)` 收集步行与摇一摇事件；离开页面或进入后台时取消收集并清除走路状态。步行事件使用 `SensorEvent.timestamp / 1_000_000` 与 `elapsedRealtime()` 的单调时钟；上一轮页面留下的事件、未来时间事件均被拒绝。连续两步可开始走路，最后有效步行后约 6.5 秒恢复静止。计步 StateFlow 的首个累计值不会把打开页面误判为走路。

活动识别权限申请复用 MainActivity 的现有 launcher，让授权成功后的公共传感器启动流程保持一致。宠物每 500 毫秒检查权限变化；精确定位要求沿用公共 LocationSensor 的现有规则。

公共接口没有原始加速度流，因此无计步硬件或无权限时，宠物保持静止并显示移动不可用；不再独立估计步行。公共摇一摇仍可使用。宠物不会停止其他页面或后台服务使用的公共传感器。

## 验证与真机操作

Kotlin 2.2.10 编译的宠物领域层、共享天气/步行转换层和资源命名测试，JUnit 4.13.2 共 42 项通过。其中新增 12 项覆盖天气映射、缺字段、无效数据、缓存重放、时间过期、步数基线、旧事件及页面重新进入。

完整 Android 构建仍受仓库缺少 `gradle/wrapper/gradle-wrapper.jar` 阻断；以上结果不等同于 APK 构建或手机验收。该构建文件属于团队工程，本轮未修改。

真机验收顺序：

1. 授予精确定位，比较 Trip 与 Pet 的天气类型及四舍五入后的温度。
2. 在宠物天气弹窗刷新，确认两个页面使用同一份更新结果。
3. 授予活动识别权限，走两步，再停止约 6.5 秒，观察走路与静止切换。
4. 打开 Pet 页面时已有较大累计步数，确认不会立即走路；退出再进入也需要新步行证据。
5. Pet 页摇一摇应显示宠物提醒；Explore 页摇一摇仍使用原有队友逻辑。
6. 拒绝权限、切换后台或更换到无计步硬件设备，检查不可用提示和静止状态。
7. 使用缺风速的雨天数据，确认雨衣可用，界面不会显示伪造的 0 km/h 风速。

## Viva 可说明的调用链

公共天气/传感器模块负责获取输入，宠物转换层负责数据验证和生命周期内消费，行为引擎负责动作与服装优先级，Compose 负责呈现。合影与 Home 继续共享同一份宠物外观状态。本轮未修改注册、Trip 进度、云端存储或队友的情境算法。

本轮代码与说明使用 Codex 辅助实现；个人汇报应结合以上调用链、提交差异与真机结果说明自己的理解，并按课程要求披露 AI 使用。
