# AutoSkip

一款基于[Shizuku](https://github.com/RikkaApps/Shizuku)授权**自动跳过**工具

## 简介

本应用专注于帮助您跳过应用的启动广告/宣传页，相比于其他同类产品，本应用具有以下特点：

## 特点

- 不需要您手动开启辅助功能
- 支持开机自启
- 不需要在通知栏显示通知以保持后台运行
- 不需要刻意保活便可常驻后台
- 省电且占用系统资源较少
- 代码开源，安全可信

## 实现

利用Shizuku授予特权，使用安卓内置的 [UiAutomation](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/UiAutomation.java) 框架用于目标识别和模拟点击。详见 [AutomatorConnection.kt](https://github.com/xjunz/AutoSkip/blob/master/automator/src/main/java/top/xjunz/automator/AutomatorConnection.kt)。

## 构建

如果你想构建此项目，请将代码`git clone`到本地后，手动建立`top.xjunz.automator.Constants.kt`类，并在其中定义:

```kotlin
const val ALIPAY_DONATE_URL ="xxxx"
const val EMAIL_ADDRESS = "xxxx"
```

## 其他

> 本应用基于Apache License 2.0开源，请在开源协议约束范围内使用源代码 | *Copyright 2021 XJUNZ*
>

