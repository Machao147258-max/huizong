# Frida 自动启动模块使用说明

## 1. 模块概述
本模块 **frida_auto_start** 会在系统启动时自动将 `frida-server` 推送到 `/data/local/tmp/`，授予可执行权限并以 `-l 0.0.0.0` 方式后台运行。模块自带一个守护进程 (`service.sh`) 会每 30 秒检查 `frida-server` 是否仍在运行，若异常退出则重新启动，保证 Frida 随系统保持可用。

## 2. 文件结构
```text
frida_auto_start/
│   module.prop            # Magisk 模块元信息 (author=哄鼠)
│   post-fs-data.sh       # 启动时复制并运行 frida-server
│   service.sh            # 守护进程，确保 server 持续运行
│   README.txt            # 本说明的备份
│   frida-server          # 已经放入的 frida‑server‑17.9.10‑android‑x86_64 二进制
```

## 3. 安装步骤
1. **拷贝 ZIP**：本目录下的 `frida_auto_start.zip` 即为完整的 Magisk 安装包。
2. **使用 Magisk Manager 安装**：打开 MuMu 模拟器内的 Magisk Manager，点击 **Install → Install from storage**，选择 `frida_auto_start.zip` 完成安装。
   - 若没有 UI，可直接通过 ADB 安装：
     ```bat
     D:\MuMuPlayer\nx_main\adb.exe push "C:\Users\20751\Desktop\frida模块\frida_auto_start.zip" /data/local/tmp/
     D:\MuMuPlayer\nx_main\adb.exe shell su -c "magisk --install-module /data/local/tmp/frida_auto_start.zip"
     ```
3. **重启模拟器**：确保 Magisk 加载新模块。`adb reboot` 后等待系统启动完成。

## 4. 验证安装
- 进入 shell 并检查进程：
  ```bat
  D:\MuMuPlayer\nx_main\adb.exe shell su -c "pidof frida-server"
  ```
  若返回非空 PID，说明 Frida 已在后台运行。
- 查看二进制是否已放置：
  ```bat
  D:\MuMuPlayer\nx_main\adb.exe shell su -c "ls -l /data/local/tmp/frida-server"
  ```

## 5. 使用 Frida 客户端
- **本地 USB 连接**：
  ```bash
  frida -U -p <PID> -l your_script.js
  ```
- **端口转发（如需远程）**：
  ```bat
  D:\MuMuPlayer\nx_main\adb.exe forward tcp:27042 tcp:27042
  frida -H 127.0.0.1:27042 -p <PID> -l your_script.js
  ```

## 6. 常见问题
- **Frida 无法启动**：确认 `frida-server` 已具备执行权限（`chmod 755 /data/local/tmp/frida-server`），并在 `post-fs-data.sh` 中路径正确。
- **模块未加载**：检查 Magisk 状态，确保 `frida_auto_start` 出现在已安装模块列表中 (`magisk --list`).
- **需要更换平台**：直接替换 `frida-auto_start` 目录下的 `frida-server` 为对应平台（arm/arm64），重新压缩并重新安装即可。

---
**作者**：哄鼠
**版本**：1.0
**发布日期**：2026-05-22
