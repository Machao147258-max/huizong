package com.detector.roothook.util;

import android.content.Context;
import android.os.Build;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class EmulatorDetector {

    public static class DetectionResult {
        public String name;
        public boolean detected;
        public String detail;

        public DetectionResult(String name, boolean detected, String detail) {
            this.name = name;
            this.detected = detected;
            this.detail = detail;
        }
    }

    public static List<DetectionResult> detectAll(Context context) {
        List<DetectionResult> results = new ArrayList<>();
        results.add(checkEmulatorFiles());
        results.add(checkEmulatorProps());
        results.add(checkCpuAbi());
        results.add(checkBuildProps());
        results.add(checkSelinux());
        results.add(checkSensors(context));
        results.add(checkQemuDrivers());
        results.add(checkNetworkOperator(context));

        results.add(nativeDetectSelinux());
        results.add(nativeDetectProps());
        results.add(nativeDetectCpuinfo());
        results.add(nativeDetectTtyDrivers());
        return results;
    }

    /**
     * 模拟器专属文件 — 真机绝对不会有这些文件
     */
    private static DetectionResult checkEmulatorFiles() {
        String[] paths = {
            "/system/bin/nemuinit",
            "/data/data/com.mumu.store",
            "/data/data/com.mumu.acc",
            "/data/data/com.nemu.nlp",
            "/data/data/com.nemu.oaidmanager",
            "/system/priv-app/com.mumu.store",
            "/system/priv-app/com.mumu.acc",
            "/system/priv-app/com.nemu.nlp",
            "/dev/goldfish_pipe",
            "/dev/vboxguest",
            "/init.goldfish.rc",
            "/fstab.goldfish",
        };
        List<String> found = new ArrayList<>();
        for (String path : paths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        return new DetectionResult("模拟器专属文件", !found.isEmpty(),
                found.isEmpty() ? "未找到" : "找到: " + found.size() + "个文件");
    }

    /**
     * 系统属性 — 仅检测 QEMU/goldfish/vbox/ranchu 等虚拟机专属属性
     * 不检测 brand/manufacturer（真机可能是 Xiaomi/Samsung 等，模拟器伪装后也一样）
     */
    private static DetectionResult checkEmulatorProps() {
        List<String> suspicious = new ArrayList<>();
        // 只检测明确属于虚拟机的专属硬件属性
        String[][] emuProps = {
            {"ro.hardware", null},
            {"ro.boot.hardware", null},
        };
        for (String[] pair : emuProps) {
            String value = getSystemProperty(pair[0]);
            if (value == null || value.isEmpty()) continue;
            String low = value.toLowerCase();
            // 只匹配明确的虚拟机硬件标识
            if (low.equals("goldfish") || low.equals("ranchu") ||
                low.equals("vbox86") || low.contains("vbox") ||
                low.contains("qemu") || low.contains("mumu") ||
                low.contains("nemu")) {
                suspicious.add(pair[0] + "=" + value);
            }
        }
        return new DetectionResult("模拟器属性", !suspicious.isEmpty(),
                suspicious.isEmpty() ? "未发现虚拟机属性" : "发现: " + String.join(", ", suspicious));
    }

    /**
     * CPU 架构 — 仅展示信息，不单独作为告警依据
     * 有 x86 平板等真机存在
     */
    private static DetectionResult checkCpuAbi() {
        String[] abis = Build.SUPPORTED_ABIS;
        StringBuilder detail = new StringBuilder();
        for (String a : abis) detail.append(a).append(" ");
        boolean isX86 = abis.length > 0 && abis[0].contains("x86");
        return new DetectionResult("CPU 架构", false,
                (isX86 ? "x86架构 (模拟器常见)" : "ARM架构") + " | " + detail.toString().trim());
    }

    /**
     * Build 指纹 — 检测明显的 SDK/emulator 指纹
     * 不误报正常厂商指纹
     */
    private static DetectionResult checkBuildProps() {
        String fp = Build.FINGERPRINT;
        String model = Build.MODEL;
        boolean isEmu = false;
        String reason = "";

        if (fp != null) {
            String l = fp.toLowerCase();
            if (l.contains("generic") || l.contains("sdk_gphone") || l.contains("ranchu")) {
                isEmu = true;
                reason = "Fingerprint异常: " + fp;
            }
        }
        if (!isEmu && model != null) {
            String l = model.toLowerCase();
            if (l.contains("sdk") && l.contains("google")) {
                isEmu = true;
                reason = "Model异常: " + model;
            }
        }
        if (!isEmu) {
            reason = "Fingerprint=" + fp + " | Model=" + model;
        }
        return new DetectionResult("Build 指纹", isEmu, reason);
    }

    /**
     * SELinux — Permissive 不等于模拟器（开发机也会开）
     * 降低告警级别
     */
    private static DetectionResult checkSelinux() {
        try {
            Process p = Runtime.getRuntime().exec("getenforce");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String status = r.readLine();
            p.waitFor();
            boolean permissive = "Permissive".equalsIgnoreCase(status);
            return new DetectionResult("SELinux", permissive,
                    "SELinux = " + status + (permissive ? " (非强制模式)" : " (强制模式)"));
        } catch (Exception e) {
            return new DetectionResult("SELinux", false, "无法获取");
        }
    }

    /**
     * 传感器 — 真机通常 20+，模拟器 5-10
     * 但低端真机也可能少，阈值设保守
     */
    private static DetectionResult checkSensors(Context context) {
        android.hardware.SensorManager sm = (android.hardware.SensorManager)
                context.getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) return new DetectionResult("传感器", false, "无法获取");

        List<android.hardware.Sensor> list = sm.getSensorList(android.hardware.Sensor.TYPE_ALL);
        int count = (list != null) ? list.size() : 0;

        StringBuilder sb = new StringBuilder();
        if (list != null) {
            for (android.hardware.Sensor s : list) {
                sb.append(s.getName()).append(", ");
            }
        }
        boolean suspicious = count < 8 && count > 0;
        return new DetectionResult("传感器", suspicious,
                suspicious ? "仅" + count + "个传感器 (真机通常20+)" : "传感器数量正常 (" + count + "个)");
    }

    /**
     * 内核驱动 — 检查 /proc/tty/drivers 中的 goldfish
     */
    private static DetectionResult checkQemuDrivers() {
        List<String> found = new ArrayList<>();
        try {
            File f = new File("/proc/tty/drivers");
            if (f.exists()) {
                BufferedReader r = new BufferedReader(new InputStreamReader(
                        new java.io.FileInputStream(f)));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("goldfish")) found.add(line.trim());
                }
                r.close();
            }
        } catch (Exception ignored) {}
        try {
            Process p = Runtime.getRuntime().exec("ls /dev/");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                String low = line.toLowerCase();
                if (low.contains("qemu") || low.contains("goldfish") || low.contains("vbox"))
                    found.add("/dev/" + line);
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return new DetectionResult("虚拟化驱动", !found.isEmpty(),
                found.isEmpty() ? "未检测到虚拟化驱动" : "找到: " + found.size() + "个");
    }

    /**
     * 运营商 — 模拟器通常无 SIM 卡
     */
    private static DetectionResult checkNetworkOperator(Context context) {
        try {
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String op = tm.getNetworkOperatorName();
                if (op != null && !op.isEmpty()) {
                    return new DetectionResult("运营商", false, "运营商: " + op);
                }
            }
        } catch (Exception ignored) {}
        return new DetectionResult("运营商", true, "无法获取运营商 (无SIM卡/模拟器)");
    }

    private static String getSystemProperty(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            return (String) c.getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            return null;
        }
    }

    private static DetectionResult nativeDetectSelinux() {
        try {
            String status = NativeDetector.nativeDetectSelinux();
            boolean permissive = "Permissive".equalsIgnoreCase(status);
            return new DetectionResult("Native SELinux 检测", permissive,
                    "Native getenforce = " + status);
        } catch (Exception e) {
            return new DetectionResult("Native SELinux 检测", false, "失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeDetectProps() {
        try {
            String props = NativeDetector.nativeDetectProps();
            boolean detected = props != null && !props.isEmpty();
            return new DetectionResult("Native getprop 检测", detected,
                    detected ? "属性: " + props : "正常");
        } catch (Exception e) {
            return new DetectionResult("Native getprop 检测", false, "失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeDetectCpuinfo() {
        try {
            String found = NativeDetector.nativeDetectCpuinfo();
            boolean detected = found != null && !found.isEmpty();
            return new DetectionResult("Native /proc/cpuinfo", detected,
                    detected ? "发现模拟器特征: " + found : "未发现 goldfish/qemu");
        } catch (Exception e) {
            return new DetectionResult("Native /proc/cpuinfo", false, "失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeDetectTtyDrivers() {
        try {
            String found = NativeDetector.nativeDetectTtyDrivers();
            boolean detected = found != null && !found.isEmpty();
            return new DetectionResult("Native tty drivers", detected,
                    detected ? "发现 goldfish: " + found : "未发现");
        } catch (Exception e) {
            return new DetectionResult("Native tty drivers", false, "失败: " + e.getMessage());
        }
    }
}
