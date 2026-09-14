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