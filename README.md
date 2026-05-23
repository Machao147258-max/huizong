# 哄鼠工具集

> MuMu 模拟器环境伪装 + Root/Hook 检测与反检测工具集合

## 目录结构

```
汇总/
├── frida/                  # Frida 自动启动 Magisk 模块
├── hook(java层)/           # Xposed/LSPosed Java 层 Hook 框架（PassFramework）
├── mumu必刷/               # MuMu 模拟器 K40S 伪装 Magisk 模块合集
└── 哄鼠检测/               # Root/Hook/模拟器 检测工具（一代 & 二代）
```

---

## 1. frida/ — Frida 自动启动模块

Magisk 模块，系统启动时自动部署 `frida-server`（x86_64），含守护进程保活。

| 文件 | 说明 |
|:---|:---|
| `frida_auto_start.zip` | Magisk 安装包，含 `post-fs-data.sh` + `service.sh` 守护进程 |
| `frida模块使用.md` | 安装与使用说明 |

**安装**：Magisk Manager → 模块 → 从本地安装，选择 `frida_auto_start.zip`，重启即可。

---

## 2. hook(java层)/ — Java 层 Hook 框架

基于 Xposed/LSPosed 的 Java Hook 框架源码，用于绕过应用检测。

| 文件 | 说明 |
|:---|:---|
| `PassFramework源码/` | Xposed 模块源码（Gradle 项目） |
| `1.apk` / `2.apk` | 测试用 APK |

**核心文件**：
- `XposedEntry.java` — Hook 入口，拦截检测类
- `PassFramework` — 绕过框架核心逻辑

---

## 3. mumu必刷/ — MuMu 模拟器 K40S 伪装模块

将 MuMu Player 12 (Android 12) 伪装为 Redmi K40S (`munch`)，共 6 个 Magisk 模块：

| 模块 | 功能 |
|:---|:---|
| `fix_k40s_board_hardware.zip` | 修正主板平台 `kona`、硬件 `qcom`、设备代号 `munch` |
| `fix_k40s_fingerprint.zip` | 修正系统指纹、安全补丁日期 |
| `hide_mumu_files.zip` | 隐藏 MuMu 特征文件（nemuinit、模拟器数据目录） |
| `fix_k40s_serial_safe.zip` | 固化设备序列号 |
| `fix_k40s_telephony_v3.zip` | 网络环境伪装（守护进程对抗 rild 覆盖） |
| `remove_mumu_ads_v2.zip` | 去除 MuMu 广告与游戏中心 |

| 文档 | 说明 |
|:---|:---|
| `huizo.md` | 完整伪装指南（六阶段） |
| `刷入过程.md` | ADB 一键刷入脚本 |

**刷入方式**：参见 `刷入过程.md`，支持逐个刷入或批量 PowerShell 脚本。

---

## 4. 哄鼠检测/ — Root/Hook 检测工具

检测当前设备是否被 Root、是否存在 Hook 框架、是否运行在模拟器中。

### 一代（Java 层检测）

| 检测项 | 说明 |
|:---|:---|
| Root 检测 | su 文件、Magisk、SuperSU 等 |
| Hook 检测 | Xposed、LSPosed、Frida 等 |
| 模拟器检测 | 硬件特征、传感器、Build 属性 |

### 二代（Native 层检测）

在一代基础上增强：
- C/C++ Native 层直接检测 `/proc/self/maps`、`/proc/self/mem`
- 检测 Frida `frida-agent` 内存注入特征
- 检测 inline hook / PLT hook 篡改

| 文件 | 说明 |
|:---|:---|
| `一代/哄鼠检测.apk` | Java 层检测 APK |
| `一代/RootHookDetector/` | 一代源码（Gradle + NDK） |
| `二代/哄鼠检测_v2_Native.apk` | Native 增强版检测 APK |
| `二代/RootHookDetector/` | 二代源码（Gradle + CMake + NDK） |

**核心源码**：
- `RootDetector.java` — Root 检测逻辑
- `HookDetector.java` — Hook 框架检测
- `EmulatorDetector.java` — 模拟器环境检测
- `native_detector.c` — Native 层内存扫描 / Frida 特征检测（二代）

---

## 环境要求

- **模拟器**：MuMu Player 12 (Android 12, x86_64)
- **Magisk**：已安装并正常运作
- **Frida**：16.x+ 客户端（PC 端）
- **ADB**：MuMu 自带 `nx_main\adb.exe`

---

## 免责声明

本项目仅供安全研究与学习使用，请勿用于非法用途。
