# nosuke-server 模块安装说明

## 文件结构

```
frida_hidden_v3.zip
├── module.prop        ← Magisk 模块属性
├── post-fs-data.sh    ← 开机部署 + 首次启动
├── service.sh         ← 守护进程保活
├── anti_detect.js     ← 自动反检测脚本（注入时使用）
├── nosuke-server      ← 魔改版 binary (106MB)
└── README.md          ← 本说明
```

## vs v2 魔改对比

| 改动 | v1 原版 | v2 | v3 (本版) |
|---|---|---|---|
| 二进制 | `frida-server` | `frida-server` (已改名) | 源码级魔改 |
| 源码改动 | 无 | 无 | frida → nosuke (全链路) |
| 部署名 | `frida-server` | `sys-helper` | `sys-helper` |
| 端口 | 27042 | 31337 | 31337 |
| 进程名(ps) | `frida-server` | `sys-helper` | `sys-helper` |
| Agent 库 | `frida-agent.so` | `frida-agent.so` | `nosuke-agent.so` |
| 线程名 | frida-* | frida-* | nosuke-* |
| RPC 标识 | `frida:rpc` | `frida:rpc` | base64 编码 |
| D-Bus 数据目录 | `re.frida.server` | `re.frida.server` | UUID 随机 |
| FIFO 管道 | `linjector-` | `linjector-` | 指针散列 |
| strongR 补丁 | 无 | 无 | 8 个全打 |

## 安装

```bash
# 1. 推送模块
adb push frida_hidden_v3.zip /sdcard/

# 2. Magisk 刷入
adb shell su -c "magisk --install-module /sdcard/frida_hidden_v3.zip"

# 3. 重启
adb reboot

# 4. 验证
adb shell su -c "ps -A | grep sys-helper"
adb shell su -c "netstat -tlnp | grep 31337"
```

## 连接使用

```bash
# 端口转发
adb forward tcp:31337 tcp:31337

# CLI
frida -H 127.0.0.1:31337 -n 应用名

# 注入反检测脚本（遇到 root/模拟器检测时使用）
frida -H 127.0.0.1:31337 -n 应用名 -l /data/local/tmp/.cache/anti_detect.js
```

## 注意事项

1. 客户端版本需匹配 **17.11.0**
2. 快手等强防护 app 需要 gadget/zygisk 额外方案
3. 监听 `127.0.0.1`（仅本地），如需远程改为 `0.0.0.0`
