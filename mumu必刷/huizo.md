# MuMu 模拟器伪装 Redmi K40S 完整指南

## 环境信息
- **模拟器版本**: MuMu Player 12 (Android 12)
- **伪装目标**: Redmi K40S (开发代号 `munch`，骁龙 870 / `kona` 平台)
- **核心目的**: 通过修改系统属性和配置文件，使模拟器在基础检测中呈现为真实的 Redmi K40S 设备

---

## 第一阶段：机型伪装（硬件属性）

### 关键参数

| 参数 | Key | 目标值 |
|:---|:---|:---|
| 手机型号 | `ro.product.model` | `22021211RC` |
| 手机品牌 | `ro.product.brand` | `Xiaomi` |
| 设备代号 | `ro.product.device` | `munch` |
| 制造商 | `ro.product.manufacturer` | `Xiaomi` |
| 主板平台 | `ro.board.platform` | `kona` |
| 硬件标识 | `ro.hardware` | `qcom` |
| 屏幕密度 | `ro.sf.lcd_density` | `440` |

### 实施步骤

**1. 外部配置修改（持久化）**

文件路径：`D:\MuMuPlayer\vms\MuMuPlayer-12.0-0\configs\customer_config.json`

```json
"phone": {
  "brand": "Xiaomi",
  "code": "munch",
  "imei": "865146042407069",
  "manufacturer": "Xiaomi",
  "miit": "22021211RC",
  "mode": {
    "choose": "phone.mode.custom"
  },
  "model": "22021211RC",
  "number": "",
  "vdid": "72216d2d-2a42-422a-8aa7-e0f398be9459"
}
```

> `phone.mode` 必须设为 `custom`，否则模拟器会使用预设模板覆盖自定义设置。

**2. 内部属性修正**

```bash
adb.exe -s 127.0.0.1:7555 shell
su
echo ro.product.device=munch > /data/local.prop
echo ro.product.manufacturer=Xiaomi >> /data/local.prop
echo ro.board.platform=kona >> /data/local.prop
echo ro.hardware=qcom >> /data/local.prop
chmod 644 /data/local.prop
```

**3. 底层服务覆盖（Magisk 模块）**

MuMu 内置 `nemuinit` 服务会在启动时强制重置 `ro.board.platform` 和 `ro.hardware`。使用 Magisk 模块 `fix_k40s_board_hardware` 的 `post-fs-data.sh` 对其覆盖：

```bash
#!/system/bin/sh
resetprop ro.board.platform kona
resetprop ro.hardware qcom
resetprop ro.product.device munch
```

### 验证命令

```bash
adb.exe -s 127.0.0.1:7555 shell "getprop ro.product.model && getprop ro.product.brand && getprop ro.product.device && getprop ro.product.manufacturer && getprop ro.board.platform && getprop ro.hardware"
```

预期输出：`22021211RC` / `Xiaomi` / `munch` / `Xiaomi` / `kona` / `qcom`

### 注意事项
- 品牌必须选择 **`Xiaomi`**（而非 `Redmi`），否则底层硬件映射会错误
- 修改完成后必须完全退出模拟器（右键托盘图标 -> 退出），再重新打开
- `/system/etc/rand_dev_prop/` 下存在随机机型文件，建议统一替换

---

## 第二阶段：系统指纹伪装

### 关键参数

| 参数 | Key | 目标值 |
|:---|:---|:---|
| 系统指纹 | `ro.build.fingerprint` | `Redmi/munch/munch:12/SKQ1.211006.001/V13.0.5.0.SLMCNXM:user/release-keys` |
| 系统描述 | `ro.build.description` | `munch-user 12 SKQ1.211006.001 V13.0.5.0.SLMCNXM release-keys` |
| 安全补丁 | `ro.build.version.security_patch` | `2022-05-01` |

### Magisk 模块 `fix_k40s_fingerprint`

```bash
#!/system/bin/sh
resetprop ro.build.fingerprint Redmi/munch/munch:12/SKQ1.211006.001/V13.0.5.0.SLMCNXM:user/release-keys
resetprop ro.system.build.fingerprint Redmi/munch/munch:12/SKQ1.211006.001/V13.0.5.0.SLMCNXM:user/release-keys
resetprop ro.vendor.build.fingerprint Redmi/munch/munch:12/SKQ1.211006.001/V13.0.5.0.SLMCNXM:user/release-keys
resetprop ro.build.description munch-user 12 SKQ1.211006.001 V13.0.5.0.SLMCNXM release-keys
resetprop ro.build.version.security_patch 2022-05-01
```

### 注意事项
- 部分深度检测 App 会分别读取 system 和 vendor 的指纹，两者不一致会被判定异常
- `ro.build.description` 有时无法完全覆盖，大多数 App 主要校验 `ro.build.fingerprint`

---

## 第三阶段：文件系统绕过

### 敏感文件/目录

| 路径 | 说明 | 危害 |
|:---|:---|:---|
| `/system/bin/nemuinit` | MuMu 核心初始化服务 | 强制重置硬件属性 |
| `/data/data/com.mumu.store` | MuMu 游戏中心数据 | 暴露模拟器身份 |
| `/data/data/com.mumu.acc` | MuMu 账号服务数据 | 暴露模拟器身份 |
| `/data/data/com.nemu.nlp` | 模拟器 NLP 服务 | 暴露模拟器身份 |

### Magisk 模块 `hide_mumu_files`

```bash
#!/system/bin/sh
# 隐藏 nemuinit（字符设备指向 /dev/null）
if [ -f /system/bin/nemuinit ]; then
    mount -o bind /dev/null /system/bin/nemuinit
fi

# 隐藏模拟器应用数据目录
EMPTY_DIR=/data/adb/modules/hide_mumu_files/empty_dir
mkdir -p $EMPTY_DIR

for dir in com.mumu.store com.mumu.acc com.mumu.shared.sdk com.nemu.nlp com.nemu.oaidmanager; do
    TARGET="/data/data/$dir"
    if [ -d "$TARGET" ]; then
        mount -o bind $EMPTY_DIR "$TARGET"
    fi
done
```

### 注意事项
- System 分区只读，无法直接删除文件；模拟器数据目录有自愈机制
- 隐藏 nemuinit 可能导致 MuMu 部分高级功能失效，但能彻底杜绝属性篡改

---

## 第四阶段：序列号固化

### 关键参数

| 参数 | Key | 目标值 |
|:---|:---|:---|
| 设备序列号 | `ro.serialno` | `865146042407069` |

### Magisk 模块 `fix_k40s_serial_safe`（安全模式）

```bash
#!/system/bin/sh
resetprop ro.serialno 865146042407069
```

### 注意事项
- **严禁**修改 `ril.serialnumber` 等基带相关属性，会导致模拟器断网/卡死
- 普通 App 在 Android 10+ 已无法读取 IMEI，只要 `ro.serialno` 合法即可
- 不要修改 MAC 地址，会导致网络桥接断开

---

## 第五阶段：网络环境伪装

### 关键参数

| 参数 | Key | 目标值 |
|:---|:---|:---|
| 网络类型 | `gsm.network.type` | `LTE` |
| 基带版本 | `gsm.version.baseband` | `MPSS.HE.2.0.c2-00036-SC8250_PACK-1.240520.1` |
| 基带标识 | `ro.baseband` | `msm` |

### Magisk 模块 `fix_k40s_telephony_v3`（守护进程方案）

模拟器的 `rild` 服务会持续覆盖网络属性，需要后台守护进程持续对抗：

```bash
#!/system/bin/sh
force_props() {
    while true; do
        val=$(getprop gsm.network.type)
        if [ "$val" != "LTE" ]; then
            resetprop gsm.network.type LTE
        fi

        val=$(getprop gsm.version.baseband)
        if [ "$val" != "MPSS.HE.2.0.c2-00036-SC8250_PACK-1.240520.1" ]; then
            resetprop gsm.version.baseband MPSS.HE.2.0.c2-00036-SC8250_PACK-1.240520.1
            resetprop ro.baseband msm
            resetprop ro.boot.baseband msm
        fi

        sleep 2
    done
}

force_props &
```

### 注意事项
- V1/V2 方案（`post-fs-data.sh` 单次覆盖 / sleep 延迟覆盖）均失败，必须使用守护进程
- 即使重启或模拟器尝试覆盖，守护进程会在 2 秒内将其改回

---

## 第六阶段：去除广告与游戏中心

### 敏感应用

| 包名 | 说明 | 处理方式 |
|:---|:---|:---|
| `com.mumu.store` | MuMu 游戏中心 | 挂载空文件替换 APK |
| `com.mumu.acc` | MuMu 账号服务 | 挂载空文件替换 APK |
| `com.nemu.nlp` | 模拟器 NLP 服务 | 挂载空文件替换 APK |

### Magisk 模块 `remove_mumu_ads_v2`

```bash
#!/system/bin/sh
EMPTY=/data/adb/modules/remove_mumu_ads_v2/empty.apk
touch $EMPTY

mount -o bind $EMPTY /system/priv-app/com.mumu.store/com.mumu.store.apk
mount -o bind $EMPTY /system/priv-app/com.mumu.acc/com.mumu.acc.apk
mount -o bind $EMPTY /system/priv-app/com.nemu.nlp/com.nemu.nlp.apk
```

### 注意事项
- V1 方案（`pm disable-user`）无效，Launcher 会忽略禁用状态且后台进程可能复活
- V2 方案利用 bind mount 空文件覆盖 APK，系统认为 APK 已损坏，强制隐藏图标

---

## 验证命令汇总

一次性验证所有伪装项：

```bash
adb.exe -s 127.0.0.1:7555 shell "
echo '=== Model ===' && getprop ro.product.model
echo '=== Brand ===' && getprop ro.product.brand
echo '=== Device ===' && getprop ro.product.device
echo '=== Board ===' && getprop ro.board.platform
echo '=== Hardware ===' && getprop ro.hardware
echo '=== Fingerprint ===' && getprop ro.build.fingerprint
echo '=== Security Patch ===' && getprop ro.build.version.security_patch
echo '=== Serial ===' && getprop ro.serialno
echo '=== Network Type ===' && getprop gsm.network.type
echo '=== Baseband ===' && getprop gsm.version.baseband
"
```

---

## 模块清单

| 模块 | 功能 | 文件 |
|:---|:---|:---|
| fix_k40s_board_hardware | 修正主板平台和硬件标识 | `fix_k40s_board_hardware.zip` |
| fix_k40s_fingerprint | 修正系统指纹和安全补丁 | `fix_k40s_fingerprint.zip` |
| hide_mumu_files | 隐藏 MuMu 特征文件和目录 | `hide_mumu_files.zip` |
| fix_k40s_serial_safe | 固化设备序列号 | `fix_k40s_serial_safe.zip` |
| fix_k40s_telephony_v3 | 网络环境伪装（守护进程） | `fix_k40s_telephony_v3.zip` |
| remove_mumu_ads_v2 | 去除广告与游戏中心 | `remove_mumu_ads_v2.zip` |
