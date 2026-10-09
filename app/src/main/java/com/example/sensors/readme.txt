Sensor 框架：
加速度计 Accelerometer → TYPE_ACCELEROMETER → X/Y/Z → m/s²
光线传感器 Light → TYPE_LIGHT → 光照 → lux
陀螺仪 Gyroscope → TYPE_GYROSCOPE → X/Y/Z → rad/s
磁力计 Magnetometer → TYPE_MAGNETIC_FIELD → X/Y/Z → μT
气压计 Pressure / Barometer → TYPE_PRESSURE → 气压 → hPa
距离传感器 Proximity → TYPE_PROXIMITY → 距离 → cm
环境温度传感器 Ambient Temperature → TYPE_AMBIENT_TEMPERATURE → 温度 → °C
相对湿度传感器 Relative Humidity → TYPE_RELATIVE_HUMIDITY → 湿度 → %
旋转向量 Rotation Vector → TYPE_ROTATION_VECTOR → 旋转方向
重力传感器 Gravity → TYPE_GRAVITY → X/Y/Z → m/s²
线性加速度 Linear Acceleration → TYPE_LINEAR_ACCELERATION → X/Y/Z → m/s²
步数计数器 Step Counter → TYPE_STEP_COUNTER → 步数
步数检测器 Step Detector → TYPE_STEP_DETECTOR → 每一步一个 event


GPS框架
GPS / 位置 → FusedLocationProviderClient
电池 → BatteryManager
蜂窝网络 → TelephonyManager


Others：
麦克风 → AudioRecord
指纹识别 → BiometricPrompt+


App 状态	Step Counter	GPS	Shake
App 刚启动	注册 Listener；如果已有持续追踪服务，连接现有状态	初始化 Location Client，不启动持续定位	注册 Listener
App 在前台	Listener 持续接收步数	按需请求位置	Listener 持续检测摇一摇
用户开启持续步数追踪	启动 Step Foreground Service，由 Service 管理 Listener	不变	不变
App 进入后台	有追踪服务则继续监听；否则停止	停止普通持续定位请求	取消 Listener
App 回到前台	没有运行中的追踪服务则重新监听	按需重新获取位置	重新注册 Listener
用户结束持续追踪	停止 Step Service；若 App 仍在前台，可恢复前台 Listener	不变	不变
App 进程被终止	Listener 停止，Room 已写入的数据保留；Service 也可能被终止	停止	停止
App 重新打开	读取 Room，恢复前台监听；检查是否需要恢复追踪	重新初始化	重新注册 Listener
手机重启	之前的 Listener/Service 不再运行；需要重新建立追踪，并处理累计步数重置	不运行	不运行






StateFlow(只保留一个最新值) / SharedFlow (EventBus)/ Function Call




onCreate()：初始化 UI、注册 Flow 订阅。
onStart()：只自动绑定 Service，不自动启动所有 Sensor。
onResume()：不自动启动 Shake，仍由测试按钮控制。
onStop()：停止本 Activity 启动的 GPS、Step Detector、Shake；解绑 Service。
onDestroy()：作为最终清理，不作为正常页面退出时唯一的停止机制。
