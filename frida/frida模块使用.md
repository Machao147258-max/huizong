# Frida 反检测模块使用说明

## 1. 版本对比

| 版本 | 文件 | 进程名 | 端口 | 反检测 |
|:---|:---|:---|:---|:---|
| **v1 (原版)** | `frida_auto_start.zip` | `frida-server` | 27042 | 无 |
| **v2 (魔改)** | `frida_anti_detect_v2.zip` | `sys-helper` | 31337 | 全自动 |

## 2. v2 魔改内容

| 改动 | v1 原版 | v2 增强版 |
|:---|:---|:---|
| 二进制名 | `frida-server` | `sys-helper` |
| 安装路径 | `/data/local/tmp/frida-server` | `/data/local/tmp/.cache/sys-helper` |
| 监听端口 | 27042 (知名) | 31337 (自定义) |
| 监听地址 | `0.0.0.0` (全网) | `127.0.0.1` (仅本地) |
| 模块 ID | `frida_auto_start` | `frida_hidden` |
| 显示名 | "Frida Auto-Start" | "System Helper Service" |
| 作者 | `哄鼠` | `anonymous` |
| 反检测脚本 | 无 | **anti_detect.js 自动注入** |

## 3. anti_detect.js 防护清单

### Native 层 (libc Hook)
| 函数 | 拦截内容 |
|:---|:---|
| `fopen` | 31 条 root/magisk/frida/模拟器路径 → 返回 NULL |
| `open` | 同上 → 返回 -1 |
| `access` | 同上 → 返回 -1 |
| `stat` | 同上 → 返回 -1 |

### Java 层 (Xposed 等效)
| 目标 | 拦截内容 |
|:---|:---|
| `File.exists` | 31 条屏蔽路径 |
| `Runtime.exec` | `getenforce`→Enforcing, 过滤 frida 关键字 |
| `SystemProperties.get` | 伪造 `ro.build.tags`/`ro.debuggable`/`ro.kernel.qemu` 等 |
| `PackageManager.getPackageInfo` | 隐藏 root/magisk/xposed 包名 |
| `ClassLoader.loadClass` | 隐藏 Xposed/Magisk 类 |
| `Throwable.getStackTrace` | 清洗 Xposed 堆栈 |

## 4. 安装步骤

```bat
adb push frida_anti_detect_v2.zip /sdcard/
adb shell su -c "magisk --install-module /sdcard/frida_anti_detect_v2.zip"
adb reboot
```

## 5. 连接使用

```bash
# 端口转发（v2 用 31337 端口）
adb forward tcp:31337 tcp:31337

# Python
import frida
dev = frida.get_device_manager().add_remote_device("127.0.0.1:31337")
session = dev.attach("com.target.app")

# 注入反检测脚本
frida -H 127.0.0.1:31337 -n com.detector.roothook -l /data/local/tmp/.cache/anti_detect.js --no-pause
```

## 6. 验证

```bash
adb shell su -c "ps -A | grep sys-helper"       # 应看到 sys-helper
adb shell su -c "netstat -tlnp | grep 31337"    # 应看到 31337 端口
```
