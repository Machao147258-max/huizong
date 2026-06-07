# nosuke-server (魔改 Frida)

基于 Frida 17.11.0 源码的反检测版本，编译产物为 `nosuke-server`。

> **大文件下载**：`nosuke-server` & `nosuke-src.tar.gz` → **[frida-mogai](https://github.com/Machao147258-max/frida-mogai)**

## 成品

| 文件 | 说明 |
|---|---|
| `nosuke-server` | Android x86_64 编译好的反检测版 frida-server (106MB) |

## 反检测改动

| 检测点 | 原值 | 魔改后 |
|---|---|---|
| 默认端口 | `27042` | `31337` |
| 集群端口 | `27052` | `31347` |
| Inspector 端口 | `9229` | `19527` |
| 进程名 | `frida-server` | `nosuke-server` |
| Agent 库名 | `frida-agent-<arch>.so` | `nosuke-agent-<arch>.so` |
| 线程名 | `frida-server-main-loop` | `nosuke-server-main-loop` |
| 线程名 | `gum-modify-thread-worker` | `nosuke-modify-thread-worker` |
| Socket 路径 | `/frida-zymbiote-...` | `/nosuk-zymbiote-...` |
| HTTP User-Agent | `Frida/` | `Nosuke/` |
| HTTP Server 头 | `Frida/` | `Nosuke/` |
| RPC 消息标识 | `frida:rpc` | Base64 编码 |
| D-Bus 数据目录 | `re.frida.server` | UUID 随机 |
| FIFO 管道名 | `linjector-` | 指针散列 |
| Agent 入口点 | `frida_agent_main` | `nosuke_agent_main` |

**D-Bus 接口名保持原样 (`re.frida.*`)**，保证与标准 frida 客户端兼容。

## 依赖

- Android NDK r29
- Frida 预编译 Toolchain (valac + glib)
- Frida 预编译 SDK (android-x86_64 + linux-x86_64)

### 下载 SDK

```
# Toolchain (3MB)
https://build.frida.re/deps/20260531/toolchain-linux-x86_64.tar.xz

# SDK - 构建机 Linux x86_64 (38MB)
https://build.frida.re/deps/20260531/sdk-linux-x86_64.tar.xz

# SDK - 目标 Android x86_64 (37MB)
https://build.frida.re/deps/20260531/sdk-android-x86_64.tar.xz
```

## 编译

```bash
# 1. 准备环境
export ANDROID_NDK_ROOT=/path/to/android-ndk-r29

# 2. 解压 SDK 到 frida/frida/deps/
mkdir -p frida/deps/toolchain-linux-x86_64
mkdir -p frida/deps/sdk-linux-x86_64
mkdir -p frida/deps/sdk-android-x86_64
tar xf toolchain-linux-x86_64.tar.xz -C frida/deps/toolchain-linux-x86_64
tar xf sdk-linux-x86_64.tar.xz -C frida/deps/sdk-linux-x86_64
tar xf sdk-android-x86_64.tar.xz -C frida/deps/sdk-android-x86_64

# 3. 应用补丁
cd frida/subprojects/frida-core
patch -p1 < nosuke-full.patch

# 4. 应用 strongR 补丁
patch -p1 < strongr-patches/0001-*.patch
# ... 逐个应用 8 个 strongR 补丁

# 5. 修复并应用 strongR 冲突
# - server/server.vala: 手动修改 DEFAULT_DIRECTORY 为 static
# - agent-vala: 添加 [CCode (cname = "nosuke_agent_main")]

# 6. 编译
cd frida
sh configure --host=android-x86_64 --prefix=/tmp/frida-out
ninja -C build subprojects/frida-core/server/nosuke-server
```

## 部署

```bash
# 推送到 Android 设备
adb push nosuke-server /data/local/tmp/

# 以 root 运行
adb shell su -c '/data/local/tmp/nosuke-server -l 0.0.0.0:31337 &'

# 连接 (客户端版本需匹配 17.11.0)
frida -H 127.0.0.1:31337 -n 应用名
```

## 已知限制

- 快手/app 使用 `libaegon.so` + PLT 三件套防 ptrace，需要 gadget/zygisk 方式注入
- 编译仅验证了 `android-x86_64` 目标，arm/arm64 需额外测试

## 文件说明

```
魔改frida/
├── nosuke-server           # 编译好的二进制 (x86_64)
├── nosuke-src.tar.gz       # 魔改后的 frida-core 源码
├── nosuke-full.patch       # 完整 diff 补丁
├── strongr-patches/        # strongR 反检测原始补丁 (8个)
├── scripts/                # 构建辅助脚本
└── README.md               # 本文件
```

## Credits

- Frida: https://github.com/frida/frida
- strongR-frida: https://github.com/hzzheyang/strongR-frida-android
- Florida: https://github.com/Ylarod/Florida
