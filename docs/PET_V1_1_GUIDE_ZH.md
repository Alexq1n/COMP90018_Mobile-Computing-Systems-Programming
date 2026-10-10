# RoamMate 宠物 v1.1.1：更新与交接指南

本版把 Yufei 的最新 UI、考拉的步行动画、统一天气穿搭和直接触摸互动放进同一个工程。
交付的是完整源代码与增量补丁；本环境没有完成 Android 编译、模拟器运行或真机验证。

v1.1.1 已完整移除 `Talk`、语音识别、TTS 朗读、文字对话和麦克风权限。
旧更新包与既有分支名中的 `Voice` 仅是历史名称，不代表当前应用仍包含语音功能。

## 1. 本次使用了哪一版代码

- UI：`yufei_ui` 的 [`9b9de53`](https://github.com/Alexq1n/COMP90018_Mobile-Computing-Systems-Programming/commit/9b9de53)，是本次读取到的分支版本。
- Sensor：检查了 `alexqin-sensor` 的 [`c9eb54f`](https://github.com/Alexq1n/COMP90018_Mobile-Computing-Systems-Programming/commit/c9eb54f)。
- 宠物：在单考拉、像素循环动画及上一版天气衣橱基础上继续修改，保留同一套 Home/Pet/Photo 状态。
- 没有直接用 Sensor 分支替换整个工程，也没有把它的 `com.example.sensors` 独立示例应用合并成第二个主界面。

已采用 Yufei 更新的界面和逻辑：

| 页面 | 合入内容 |
|---|---|
| Home、Trip、Profile 等 | 顶部间距和系统窗口边距调整 |
| Trip | 多日行程摘要卡片 |
| Edit itinerary | 长按拖动排序，把排序结果交给保存回调 |
| Add a stop | 确认搜索、加载状态、禁用重复搜索 |
| Plan my trip | 必去地点流程、生成中按钮状态 |
| Attraction detail | URL 景点照片及失败占位；修正照片遮住返回按钮 |
| Explore | 移除示意地图，使用筛选与结果列表 |

上游仍有示例数据：登录用户、景点搜索、生成行程、Trip 摘要等接口尚未成为真实后端流程。
其中加载状态的接口已就位，但上游搜索/生成回调仍以 Toast 演示，不能当作后端已接通。

## 2. 宠物页面现在怎么用

主页面继续保持单屏，不需要上下滑动；宠物只有考拉。
中间是天气场景和宠物，下方是衣橱选择，再下方是等宽的 `Treat`、`Photo`。
左右滑动或衣橱箭头切换保存的时装；点击、长按等互动直接发生在舞台。
页面不再展示 `tap`、`shake` 等操作教学文案。

- 走动：宠物播放 Walk 循环。
- 停下：宠物回到原地呼吸、眨眼循环，不自动频繁轮换睡觉、开心、伤心。
- 抚摸、喂食：临时反应结束后，回到当前的走路或原地循环。
- 摇晃手机：宠物给出天气关心提醒或下一站建议，与走路判定分开处理。
- 拍照：使用舞台实际穿着的衣服；并非使用一个独立的默认考拉资源。

点击右上方天气徽章可以查看天气来源、位置和移动检测信息。
需要更可靠的计步检测时，在该窗口点 `Enable step detection`，再授权 Android 的身体活动权限。
不会一打开宠物页就强制弹出活动权限请求；拒绝授权时仍可使用其他功能。

## 3. 移动检测为什么这样实现

Alex 的 `StepCounterSenser.kt` 使用 `TYPE_STEP_COUNTER`，每分钟保存一次累计值，再计算历史差值。
它适合进一步接入活动量统计，但一分钟采样结果无法及时驱动走路/停下动画。
本版独立读取前台传感器事件，不把“最近一小时步数”直接当作当前是否在走路。

优先顺序是 `TYPE_STEP_DETECTOR` → `TYPE_STEP_COUNTER` → 加速度计估计 → 无可用传感器。

- 计步检测器连续两步、间隔不超过 2.5 秒，开始走路循环。
- 累计计步器首次读数仅作基线；后续累计增加达到两步才启动，避免打开页面就误判。
- 最后一次有效移动后约 6.5 秒没有新移动，回到原地循环，避免每一步之间闪回 Idle。
- 加速度计需多个连续、近似步行节奏的波峰，并拒绝明显强烈摇晃；这是估计，仍可能误判。
- 切走 Pet 页面、应用暂停或进入后台时注销监听；没有常驻后台服务或健康数据采集。

这里的“移动”指随身持机步行的动画信号，不是 GPS 位移、开车速度、医学疲劳度或精准计步成绩。
模拟器可能没有计步传感器；最终请用你的真机检查，之前日志中的 type 18/19 可供优先使用。
Android 10 及以上的计步权限要求见[官方运动传感器说明](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion)。

## 4. 天气、服装、场景使用同一份状态

默认查询 Melbourne，并明确显示这是选择的城市，不冒充手机 GPS。
在天气窗口主动选择使用当前位置并授权后，才尝试平台定位；没有新鲜定位时仍显示所用城市和提示。
使用平台 LocationManager，不要求额外购买定位 API，也不要求 Google Play Services 定位 SDK。

天气来自 Open-Meteo 当前天气及当地日期的今日预报，包括当前温度、风速、天气代码、昼夜、今日高低温和降水概率。
前台正常约每 15 分钟刷新；失败后重试。超过 45 分钟的观测不再当作当前天气使用。
离线或天气不可用时显示不可用状态，不用预设 18°C 或模拟下雨冒充实时天气。
背景、穿搭和关心提示共同读取同一个 `PetWeatherSnapshot`。

服装采用“天气保护优先，个人时装保留”的规则：

| 当前条件 | 舞台实际衣服 |
|---|---|
| 雨、雷雨 | Raincoat 雨衣 |
| 下雪，或温度 ≤ 9°C | Winter 保暖装 |
| 风速 ≥ 25 km/h | Windbreaker 防风装 |
| 白天晴朗且温度 ≥ 25°C | Sunshine 遮阳装 |
| 其他温和天气 | 用户保存的时装；Weather 选项使用 Everyday |
| 天气无效或过期 | 用户保存的时装或 Everyday，不推断天气装备 |

规则按表内顺序处理，同时冷且下雨会先穿雨衣；这些阈值是产品表现规则。
例如下雨时选 Streetwear，缩略图仍展示保存的 Streetwear，主舞台继续穿雨衣，并说明时装已保存。
天气转温和后自动恢复 Streetwear；不会悄悄修改用户的衣橱选择。
本版复用已有贴身动画服装，采用完整造型切换，没有生成“所有时装 × 所有天气”的分层素材包。

Open-Meteo 的免费公开接口针对符合条件的非商业使用，有调用限制和署名要求；课程演示应保留来源说明。
商用或增大调用量前重新核对[服务条款](https://open-meteo.com/en/terms)与[API 文档](https://open-meteo.com/en/docs)，不要把“无 API Key”理解为无限配额。

## 5. Windows 更新步骤

本节适用于已经运行过旧更新脚本、当前位于 `jie-pet-ui-voice-...` 更新分支的工程。
不要再次运行 `RoamMate-Pet-UI-Voice-Update`；它是 v1.1.0 的历史基础包，本身仍包含已废弃的语音功能。
下载新的 `0001-Remove-pet-voice-and-dialogue-feature.patch` 到 Downloads。
以下每个代码块单独复制；不要复制 PowerShell 的 `PS C:\...>` 提示符。

```powershell
cd C:\Projects\COMP90018
git branch --show-current
git status
```

本补丁的前置版本包含中文字符串插值修复。如果 `git status` 只显示下面这个文件已修改，先把已经验证通过的修复提交：

```powershell
git add "app/src/main/java/com/group5/roammate/pet/PetDialogueEngine.kt"
git commit -m "Fix Chinese pet dialogue interpolation"
```

如果工作区本来就是干净的，不要重复提交。若还有其他文件被修改，先停止并确认来源，不要让本补丁覆盖组员工作。
选择 Downloads 中最新的无语音补丁并应用：

```powershell
$patch = Get-ChildItem "$HOME\Downloads\*Remove-pet-voice-and-dialogue-feature*.patch" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
$patch.FullName
git am $patch.FullName
```

成功后应看到 `Applying: Remove pet voice and dialogue feature`。检查结果并编译：

```powershell
git status
git --no-pager log --oneline -5
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

`git am` 出错时停止，不要重复应用；保留完整输出以便定位。
编译成功后安装到模拟器或真机，确认不再出现 `Talk` 或麦克风权限，且 `Treat`、`Photo`、触摸、换装、天气、移动动画与摇一摇建议仍工作。
确认功能后再决定推送当前分支；本补丁不会自动推送或合并其他组员分支。

## 6. 文件与组员接口

| 文件 | 负责内容 |
|---|---|
| `pet/PetModels.kt` | 天气、穿搭、行程最小数据模型 |
| `pet/PetStateEngine.kt` | 天气保护与时装选择的最终解析 |
| `pet/PetMotionEngine.kt`、`ui/pet/PetMotionEffect.kt` | 移动判定与 Android 权限/生命周期监听 |
| `ui/pet/PetEnvironment.kt`、`pet/OpenMeteoPetWeatherRepository.kt` | 城市/GPS、刷新、实时天气和今日预报 |
| `ui/pet/CompanionsScreen.kt`、`InteractivePetStage.kt` | 单屏舞台、衣橱、交互和走路/原地切换 |
| `ui/pet/PetAvatar.kt`、`WeatherBackdrop.kt`、`PetCameraScreen.kt` | 动画帧、天气场景、合影 |
| `MainActivity.kt` | 上游页面导航、共享宠物状态和行程上下文注入 |

以上路径均相对于 `app/src/main/java/com/group5/roammate/`。
Zewen/Yuxiang 接好真实下一站后，用真实数据替换示例，再显式设为非示例：

```kotlin
val petTripContext = PetTripContext(
    nextStopName = confirmedNextStop.name,
    nextStopTime = confirmedNextStop.displayTime,
    isDemo = false,
)
// 将 petTripContext 传给 CompanionsScreen 的 tripContext 参数。
```

天气组可以继续用本版仓库，或把自己的结果转换成 `PetWeatherSnapshot`，交给同一 `PetStateEngine`：

```kotlin
val liveWeather = PetWeatherSnapshot(
    condition = PetWeatherAdapter.fromWmoCode(actualWmoCode),
    label = actualConditionLabel,
    temperatureC = actualTemperatureC,
    windSpeedKmh = actualWindKmh,
    locationLabel = actualLocationLabel,
    source = actualProviderName,
    observedAtMillis = actualObservationEpochMillis,
)
val sharedPetState = PetStateEngine.buildUiState(petProfile, liveWeather)
```

上述 `actual...`、`confirmedNextStop` 是组员的数据变量占位，不要原样作为生产数据使用。
必须传真实观测时间；不要为使缓存“看起来新鲜”而把旧数据时间改成当前时间。
后台接口替换位置为 `PetEnvironment` 的天气获取入口；统一只维护一份天气，避免舞台与合影各查一套。
Alex 若提供事件流，可接入 `PetMotionEngine.onStepDetected()` / `onStepCounter()`；时间使用 elapsedRealtime 同一单调时钟。
不能把 `getStepsLastHour()` 返回的小时差值伪装成传感器累计步数。

## 7. 验收与测试状态

已完成静态资源检查：216 个考拉动画帧均为 128×128、含透明通道；资源 XML 解析检查通过。
项目包含 31 个领域层 `@Test` 和 9 个 Android 测试 `@Test`（包含原有示例测试）；这些是源码数量，不是运行通过数量。
本环境没有执行 Kotlin/Android 测试、没有生成经验证 APK，也没有在手机上实际拍照。

请在本机重点验收：真机授权后走两步/停止 6.5 秒；拒绝权限；摇晃不持续误判走路；后台返回。
天气要验收城市与 GPS 标签、断网/过期、雨衣优先与时装恢复、舞台/Home/合影实际服装一致。
最后在常用真机尺寸、系统字体放大及模拟器上检查舞台、`Treat` 与 `Photo` 均可见；宠物主页面不滚动。
