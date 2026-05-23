package com.detector.roothook.util;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RootDetector {

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

        results.add(checkSuBinary());
        results.add(checkMagisk());
        results.add(checkMagiskInitFiles());
        results.add(checkSuperSU());
        results.add(checkTestKeys());
        results.add(checkRootApps(context));
        results.add(checkBuildTags());
        results.add(checkMagiskHide());
        results.add(checkSuPath());

        results.add(nativeDetectRootFiles());
        results.add(nativeDetectByAccess());
        results.add(nativeWhichSu());

        return results;
    }

    private static DetectionResult checkSuBinary() {
        String[] paths = {
                "/system/bin/su", "/system/xbin/su", "/sbin/su",
                "/su/bin/su", "/system/su", "/system/app/Superuser.apk",
                "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
                "/sbin/bin/su", "/system/bin/.ext/su", "/system/usr/we-need-root/su"
        };
        List<String> found = new ArrayList<>();
        for (String path : paths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        return new DetectionResult("su 二进制文件", !found.isEmpty(),
                found.isEmpty() ? "未找到 su 文件" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkMagisk() {
        String[] magiskPaths = {
                "/sbin/.magisk", "/data/adb/magisk", "/data/adb/magisk.db",
                "/data/adb/magisk.img", "/data/adb/magisk_simple",
                "/sbin/magisk", "/system/bin/magisk",
                "/system/etc/init/magisk", "/system/etc/init/magisk.rc",
                "/system/bin/su", "/system/xbin/su",
                "/cache/.disable_magisk", "/dev/.magisk"
        };
        List<String> found = new ArrayList<>();
        for (String path : magiskPaths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        return new DetectionResult("Magisk 检测", !found.isEmpty(),
                found.isEmpty() ? "未找到 Magisk 文件" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkMagiskInitFiles() {
        String[] paths = {
                "/system/etc/init/magisk", "/system/etc/init/magisk.rc",
                "/system/etc/init/magisk/magiskinit", "/system/etc/init/magisk/magiskpolicy",
                "/system/etc/init/magisk/magisk32", "/system/etc/init/magisk/magisk64",
        };
        List<String> found = new ArrayList<>();
        for (String path : paths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        if (!found.isEmpty()) return new DetectionResult("Magisk init 文件", true,
                "找到: " + String.join(", ", found));

        File initDir = new File("/system/etc/init/magisk");
        if (initDir.isDirectory()) {
            String[] children = initDir.list();
            if (children != null && children.length > 0) {
                return new DetectionResult("Magisk init 目录", true,
                        "目录存在，包含 " + children.length + " 个文件");
            }
        }
        return new DetectionResult("Magisk init 文件", false, "未找到 Magisk init 文件");
    }

    private static DetectionResult checkSuperSU() {
        String[] supersuPaths = {
                "/system/etc/init.d/99SuperSUDaemon",
                "/system/bin/.ext/.su",
                "/system/xbin/daemonsu",
                "/system/etc/.installed_su_daemon",
                "/system/etc/.has_su_daemon"
        };
        List<String> found = new ArrayList<>();
        for (String path : supersuPaths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        return new DetectionResult("SuperSU 检测", !found.isEmpty(),
                found.isEmpty() ? "未找到 SuperSU 文件" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkTestKeys() {
        String buildTags = android.os.Build.TAGS;
        boolean hasTestKeys = buildTags != null && buildTags.contains("test-keys");
        return new DetectionResult("test-keys 检测", hasTestKeys,
                hasTestKeys ? "Build.TAGS 包含 test-keys (自定义ROM特征)" : "Build.TAGS: " + buildTags);
    }

    private static DetectionResult checkRootApps(Context context) {
        String[] rootApps = {
                "com.noshufou.android.su",
                "com.thirdparty.superuser",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser",
                "com.topjohnwu.magisk",
                "io.github.huskydg.magisk",
                "com.magus.magisk"
        };
        PackageManager pm = context.getPackageManager();
        List<String> found = new ArrayList<>();
        for (String pkg : rootApps) {
            try {
                pm.getPackageInfo(pkg, 0);
                found.add(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                // not installed
            }
        }
        return new DetectionResult("Root 管理应用", !found.isEmpty(),
                found.isEmpty() ? "未安装 Root 管理应用" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkBuildTags() {
        String buildTags = android.os.Build.TAGS;
        boolean suspicious = buildTags != null &&
                (buildTags.contains("test-keys") || buildTags.contains("dev-keys"));
        return new DetectionResult("Build Tags 异常", suspicious,
                "Build.TAGS = " + (buildTags != null ? buildTags : "null"));
    }

    private static DetectionResult checkMagiskHide() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "magisk --hide"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();
            boolean detected = output.length() > 0 || process.exitValue() == 0;
            return new DetectionResult("Magisk Hide/zygisk", detected,
                    detected ? "Magisk 隐藏功能可访问" : "无法访问 Magisk 命令");
        } catch (Exception e) {
            return new DetectionResult("Magisk Hide/zygisk", false, "Magisk 命令不可用");
        }
    }

    private static DetectionResult checkSuPath() {
        try {
            Process process = Runtime.getRuntime().exec("which su");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            process.waitFor();
            boolean found = result != null && !result.isEmpty();
            return new DetectionResult("which su 检测", found,
                    found ? "su 在 PATH 中: " + result : "PATH 中未找到 su");
        } catch (Exception e) {
            return new DetectionResult("which su 检测", false, "执行 which su 失败");
        }
    }

    private static DetectionResult nativeDetectRootFiles() {
        try {
            String found = NativeDetector.nativeDetectRootFiles();
            boolean detected = found != null && !found.isEmpty();
            return new DetectionResult("Native fopen 文件检测", detected,
                    detected ? "Native层找到: " + found : "Native层未找到");
        } catch (Exception e) {
            return new DetectionResult("Native fopen 文件检测", false, "Native检测失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeDetectByAccess() {
        try {
            String found = NativeDetector.nativeDetectByAccess();
            boolean detected = found != null && !found.isEmpty();
            return new DetectionResult("Native access/stat 检测", detected,
                    detected ? "Native层探测到: " + found : "Native层未发现");
        } catch (Exception e) {
            return new DetectionResult("Native access/stat 检测", false, "Native检测失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeWhichSu() {
        try {
            String path = NativeDetector.nativeWhichSu();
            boolean found = path != null && !path.isEmpty();
            return new DetectionResult("Native which su 检测", found,
                    found ? "su路径: " + path : "未找到");
        } catch (Exception e) {
            return new DetectionResult("Native which su 检测", false, "检测失败: " + e.getMessage());
        }
    }
}
