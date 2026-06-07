# MuMu 模拟器 Burp Suite 证书安装与代理配置

## 环境信息

| 项目 | 值 |
|------|-----|
| 模拟器 | MuMu Player 12 |
| 网络模式 | 桥接模式 |
| 模拟器 IP | 192.168.1.8 |
| 模拟器网关 | 192.168.1.1 |
| PC IP (WLAN) | 192.168.1.7 |
| Burp 代理端口 | 8080 |
| ADB 路径 | `D:\MuMuPlayer\nx_main\adb.exe` |

---

## 一、证书格式转换（DER → PEM）

```powershell
$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2
$cert.Import("C:\Users\20751\Desktop\cacert.der")

$pem = "-----BEGIN CERTIFICATE-----`n" +
       [Convert]::ToBase64String(
           $cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert),
           [Base64FormattingOptions]::InsertLineBreaks
       ) +
       "`n-----END CERTIFICATE-----`n"

Set-Content -LiteralPath "C:\Users\20751\Desktop\cacert.pem" -Value $pem -Encoding ASCII
```

---

## 二、计算 Android 系统证书哈希文件名

```powershell
$subjectRaw = $cert.SubjectName.RawData
$md5 = [System.Security.Cryptography.MD5]::Create()
$hash = $md5.ComputeHash($subjectRaw)

# 前4字节按小端序转uint32 → 8位十六进制
$hashInt = ([uint32]$hash[0]) -bor
           ([uint32]$hash[1] -shl 8) -bor
           ([uint32]$hash[2] -shl 16) -bor
           ([uint32]$hash[3] -shl 24)
$hashHex = $hashInt.ToString("x8")          # 例如: 9a5ba575
$certFileName = "$hashHex.0"                 # 例如: 9a5ba575.0
```

---

## 三、推送到模拟器并安装为系统证书

```powershell
# 推送 PEM 文件到 SD 卡
& "D:\MuMuPlayer\nx_main\adb.exe" push "C:\Users\20751\Desktop\cacert.pem" /sdcard/9a5ba575.0

# 重新挂载 /system 为可写
& "D:\MuMuPlayer\nx_main\adb.exe" shell "su -c 'mount -o rw,remount /'"

# 复制到系统证书目录并设置权限
& "D:\MuMuPlayer\nx_main\adb.exe" shell "su -c '
    cp /sdcard/9a5ba575.0 /system/etc/security/cacerts/9a5ba575.0
    chmod 644 /system/etc/security/cacerts/9a5ba575.0
    chown root:root /system/etc/security/cacerts/9a5ba575.0
'"
```

---

## 四、Postern 代理转发（替代系统全局代理）

> 不使用系统全局代理（`http_proxy`），避免 APP 通过 API 检测到代理。改用 Postern 的 VPN 模式转发流量。

### 4.1 安装 Postern

在模拟器中安装 Postern APK。

### 4.2 配置代理服务器

Postern → 配置代理 → 右上角 `+` 添加：

| 选项 | 值 |
|------|-----|
| 服务器名称 | Burp Suite |
| 服务器类型 | HTTP/HTTPS |
| 服务器地址 | 192.168.1.7 |
| 服务器端口 | 8080 |

保存。

### 4.3 配置规则

Postern → 配置规则 → 右上角 `+`：

| 选项 | 值 |
|------|-----|
| 规则名称 | 全部代理 |
| 匹配类型 | 匹配全部 |
| 动作 | 通过代理连接 |
| 代理/代理组 | Burp Suite |

> 如需只抓特定 APP，匹配类型选「应用」然后选择目标 APP。

保存。

### 4.4 启动

点击 Postern 主界面圆形按钮，同意 VPN 授权即可。

### 4.5 关闭 / 恢复

- 关闭: 再次点击按钮断开 VPN
- 确保系统代理为空: `adb shell settings put global http_proxy :0`

---

## 五、Burp Suite 监听器配置

在 Burp Suite 中：

1. 打开 `Proxy` → `Options`
2. 添加/编辑监听器：
   - **Bind to address**: `All interfaces`（不能是 127.0.0.1）
   - **Bind to port**: `8080`
   - **Request handling**: 勾选 `Support invisible proxying`
3. 确保勾选 `Running`

> 如果只绑定 `127.0.0.1`，模拟器无法连接到此代理。

---

## 六、Windows 防火墙放行端口

```powershell
# 添加入站规则，允许 Burp 端口通过防火墙
netsh advfirewall firewall add rule name="Burp Proxy" dir=in action=allow protocol=tcp localport=8080

# 查看已有规则
netsh advfirewall firewall show rule name="Burp Proxy"

# 如需删除规则:
# netsh advfirewall firewall delete rule name="Burp Proxy"
```

---

## 七、重启模拟器

证书安装后需要重启模拟器才能生效。在 MuMu 中手动重启即可。

---

## 八、验证步骤（按顺序）

1. **确认系统代理为空**: `adb shell settings get global http_proxy` → 输出 `:0`
2. **确认证书存在**: `adb shell ls -la /system/etc/security/cacerts/9a5ba575.0`
3. **打开 Postern 并启动 VPN**
4. **确认模拟器网络连通**: `adb shell ping 192.168.1.7`
5. **Burp 中检查**: 打开 `Proxy` → `HTTP history`，在模拟器中访问任意网页，观察是否出现流量记录

---

## 九、常见问题

| 问题 | 排查方向 |
|------|---------|
| Burp 无流量 | 检查监听是否为 All interfaces + Support invisible proxying；防火墙；Postern VPN 是否已连接 |
| HTTPS 请求报证书错误 | 证书未重启生效；证书哈希计算错误 |
| Postern 连接失败 | 确认 PC IP 和端口正确；防火墙已放行 |
| 部分 APP 仍无法抓包 | SSL Pinning 保护，需要 Frida 或 Xposed 模块绕过 |
| ADB 设备 offline | 模拟器网络桥接断开，重启模拟器或重新连接 |
