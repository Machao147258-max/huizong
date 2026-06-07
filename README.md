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

Magisk 模块，系统启动时自动部署 frida-server（x86_64），含守护进程保活。

### 版本对比

| 版本 | 模块文件 | 进程名 | 端口 | 二进制 | 反馈测 |
|:---|:---|:---|:---|:---|:---|
| **v1** | `frida_auto_start.zip` | `frida-server` | 27042 | 原版 17.9.11 | 弱 |
| **v2** | `frida_anti_detect_v2.zip` | `sys-helper` | 31337 | 原版改名 | 中 |
| **v3** | `frida_hidden_v3.zip` | `sys-helper` | 31337 | **源码级魔改 17.11.0** | 强 |

### v3 魔改详情

v3 基于 Frida 17.11.0 源码编译，对所有可检测特征进行深度修改：

| 检测点 | 原值 | 魔改后 |
|:---|:---|:---|
| 二进制名 | `frida-server` | `nosuke-server` |
| Agent 库名 | `frida-agent.so` | `nosuke-agent.so` |
| 入口点 | `frida_agent_main` | `nosuke_agent_main` |
| 线程名 | `frida-*` / `gum-*` | `nosuke-*` |
| Socket 路径 | `/frida-zymbiote-` | `/nosuk-zymbiote-` |
| RPC 消息 | `frida:rpc` | Base64 编码 |
| FIFO 管道 | `linjector-` | 指针散列 |
| D-Bus 数据目录 | `re.frida.server` | UUID 随机 |
| 集群端口 | 27052 | 31347 |

### 文件说明

```
frida/
├── frida_auto_start.zip       # v1 模块
├── frida_anti_detect_v2.zip   # v2 模块（原版改名 + anti_detect.js）
├── frida模块使用.md           # v1+v2 说明文档
├── frida模���/                 # v3 Magisk 模
│   ├── frida_hidden_v3.zip    # v3 模块包（可直刷）
│   ├── anti_detect.js         # 防检测脚本
│   ├── module.prop            # 模块属性
│   ├── post-fs-data.sh        # 开机部署脚本
│   ├── service.sh             # 守护保活脚本
│   └── README.md              # v3 说明
└── 魔改frida/                 # v3 源码 & 编译产物
    ├── nosuke-server           # 编译好的二进制
    ├── nosuke-full.patch       # 完整 diff 补丁
    ├── nosuke-src.tar.gz       # 魔改源码
    ├── strongr-patches/        # strongR 8个补丁
    ├── scripts/                # 编译辅助脚本
    └── README.md               # 编译说明
```

**安装**：Magisk Manager → 模块 → 从本地安装，选择对应 zip，重启即可。

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
- **Frida**：17.11.0 客户端（PC 端，v3 专用）/ 16.x+（v1/v2）
- **ADB**：MuMu 自带 `nx_main\adb.exe`

---

## 免责声明

本项目仅供安全研究与学习使用，请勿用于非法用途。
