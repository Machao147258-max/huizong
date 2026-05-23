package com.detector.roothook.util;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HookDetector {

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

        results.add(checkXposed(context));
        results.add(checkLSPosed(context));
        results.add(checkEdXposed(context));
        results.add(checkFrida());
        results.add(checkFridaServer());
        results.add(checkXposedInstaller(context));
        results.add(checkVirtualXposed(context));
        results.add(checkTaichi(context));
        results.add(checkStackDump());
        results.add(checkNativeLibraries());
        results.add(checkSystemProperties());
        results.add(checkClassLoader());

        results.add(nativeDetectProcMaps());
        results.add(nativeDetectTracerPid());
        results.add(nativeDetectMounts());

        return results;
    }

    private static DetectionResult checkXposed(Context context) {
        try {
            Class<?> xposedHelpers = Class.forName("de.robv.android.xposed.XposedHelpers");
            return new DetectionResult("Xposed 框架", true,
                    "检测到 XposedHelpers 类加载");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("de.robv.android.xposed.XC_MethodHook");
                return new DetectionResult("Xposed 框架", true,
                        "检测到 XC_MethodHook 类加载");
            } catch (ClassNotFoundException e2) {
                return new DetectionResult("Xposed 框架", false,
                        "未检测到 Xposed 类");
            }
        }
    }

    private static DetectionResult checkLSPosed(Context context) {
        String[] lsposedPaths = {
                "/data/adb/lspd", "/data/adb/modules/zygisk_lsposed",
                "/data/adb/modules/lsposed", "/data/adb/lsp"
        };
        List<String> found = new ArrayList<>();
        for (String path : lsposedPaths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        try {
            Class.forName("org.lsposed.lspd.nativebridge.NativeAPI");
            found.add("NativeAPI class");
        } catch (ClassNotFoundException e) {
        }
        return new DetectionResult("LSPosed 检测", !found.isEmpty(),
                found.isEmpty() ? "未检测到 LSPosed" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkEdXposed(Context context) {
        String[] edxposedPaths = {
                "/data/adb/edxp", "/data/adb/modules/edxp",
                "/data/adb/edxposed"
        };
        List<String> found = new ArrayList<>();
        for (String path : edxposedPaths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        return new DetectionResult("EdXposed 检测", !found.isEmpty(),
                found.isEmpty() ? "未检测到 EdXposed" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkFrida() {
        String[] fridaPaths = {
                "/data/local/tmp/frida-server",
                "/data/local/tmp/re.frida.server",
                "/data/local/tmp/fs",
                "/system/bin/frida-server",
                "/system/xbin/frida-server"
        };
        List<String> found = new ArrayList<>();
        for (String path : fridaPaths) {
            if (new File(path).exists()) {
                found.add(path);
            }
        }
        return new DetectionResult("Frida 检测", !found.isEmpty(),
                found.isEmpty() ? "未找到 Frida 文件" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkFridaServer() {
        try {
            Process process = Runtime.getRuntime().exec("ps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean found = false;
            List<String> matches = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("frida") || line.toLowerCase().contains("frida-server")) {
                    found = true;
                    matches.add(line.trim());
                }
            }
            process.waitFor();
            return new DetectionResult("Frida 进程", found,
                    found ? "发现 Frida 相关进程" : "未发现 Frida 进程");
        } catch (Exception e) {
            return new DetectionResult("Frida 进程", false, "检查进程失败");
        }
    }

    private static DetectionResult checkXposedInstaller(Context context) {
        String[] xposedPackages = {
                "de.robv.android.xposed.installer",
                "org.meowcat.edxposed.manager",
                "org.lsposed.manager",
                "io.github.lsposed.manager",
                "com.magus.moduleinstaller"
        };
        PackageManager pm = context.getPackageManager();
        List<String> found = new ArrayList<>();
        for (String pkg : xposedPackages) {
            try {
                pm.getPackageInfo(pkg, 0);
                found.add(pkg);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return new DetectionResult("Xposed 管理器", !found.isEmpty(),
                found.isEmpty() ? "未安装 Xposed 管理器" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkVirtualXposed(Context context) {
        String[] virtualXposedPkgs = {
                "io.va.exposed",
                "io.va.exposed.64",
                "io.virtualapp",
                "com.lbe.parallel",
                "com.excean.na"
        };
        PackageManager pm = context.getPackageManager();
        List<String> found = new ArrayList<>();
        for (String pkg : virtualXposedPkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                found.add(pkg);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return new DetectionResult("VirtualXposed/VA", !found.isEmpty(),
                found.isEmpty() ? "未检测到 VirtualXposed" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkTaichi(Context context) {
        String[] taichiPkgs = {
                "me.weishu.exp",
                "me.weishu.kyu"
        };
        PackageManager pm = context.getPackageManager();
        List<String> found = new ArrayList<>();
        for (String pkg : taichiPkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                found.add(pkg);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return new DetectionResult("太极(Taichi)", !found.isEmpty(),
                found.isEmpty() ? "未检测到太极" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkStackDump() {
        try {
            Throwable throwable = new Throwable();
            StackTraceElement[] elements = throwable.getStackTrace();
            boolean found = false;
            List<String> suspicious = new ArrayList<>();
            for (StackTraceElement element : elements) {
                String className = element.getClassName();
                if (className.contains("de.robv.android.xposed") ||
                        className.contains("org.lsposed") ||
                        className.contains("com.magus") ||
                        className.contains("frida")) {
                    found = true;
                    suspicious.add(className);
                }
            }
            return new DetectionResult("堆栈跟踪检测", found,
                    found ? "堆栈中发现 Hook 框架类: " + String.join(", ", suspicious)
                            : "堆栈中未发现 Hook 框架类");
        } catch (Exception e) {
            return new DetectionResult("堆栈跟踪检测", false, "检查堆栈失败");
        }
    }

    private static DetectionResult checkNativeLibraries() {
        String[] hookLibs = {
                "libriru", "liblspd", "libxposed", "libfrida",
                "libsubstrate", "libhook", "libcyto"
        };
        List<String> found = new ArrayList<>();
        try {
            String nativeDir = System.getProperty("java.library.path");
            if (nativeDir != null) {
                for (String lib : hookLibs) {
                    File f = new File(nativeDir, "lib" + lib + ".so");
                    if (f.exists()) {
                        found.add(lib);
                    }
                }
            }
            File systemLibDir = new File("/system/lib");
            if (systemLibDir.exists()) {
                for (String lib : hookLibs) {
                    for (File f : systemLibDir.listFiles()) {
                        if (f.getName().contains(lib)) {
                            found.add(lib + "(" + f.getName() + ")");
                        }
                    }
                }
            }
            File systemLib64Dir = new File("/system/lib64");
            if (systemLib64Dir.exists()) {
                for (String lib : hookLibs) {
                    for (File f : systemLib64Dir.listFiles()) {
                        if (f.getName().contains(lib)) {
                            found.add(lib + "(" + f.getName() + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return new DetectionResult("Native Hook 库", !found.isEmpty(),
                found.isEmpty() ? "未检测到 Native Hook 库" : "找到: " + String.join(", ", found));
    }

    private static DetectionResult checkSystemProperties() {
        List<String> suspicious = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String lower = line.toLowerCase();
                if (lower.contains("xposed") || lower.contains("lspd") ||
                        lower.contains("edxposed") || lower.contains("lsposed") ||
                        lower.contains("frida") || lower.contains("magisk")) {
                    suspicious.add(line.trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
        }
        return new DetectionResult("系统属性检测", !suspicious.isEmpty(),
                suspicious.isEmpty() ? "系统属性中无 Hook 框架痕迹"
                        : "发现 " + suspicious.size() + " 条可疑属性");
    }

    private static DetectionResult checkClassLoader() {
        try {
            ClassLoader cl = HookDetector.class.getClassLoader();
            String clName = cl != null ? cl.getClass().getName() : "null";
            boolean suspicious = clName.contains("xposed") || clName.contains("lspd") ||
                    clName.contains("magisk") || clName.contains("taichi");
            return new DetectionResult("ClassLoader 检测", suspicious,
                    "ClassLoader: " + clName);
        } catch (Exception e) {
            return new DetectionResult("ClassLoader 检测", false, "检查 ClassLoader 失败");
        }
    }

    private static DetectionResult nativeDetectProcMaps() {
        try {
            String found = NativeDetector.nativeDetectProcMaps();
            boolean detected = found != null && !found.isEmpty();
            return new DetectionResult("Native /proc/maps 扫描", detected,
                    detected ? "发现注入SO: " + found : "未发现");
        } catch (Exception e) {
            return new DetectionResult("Native /proc/maps 扫描", false, "失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeDetectTracerPid() {
        try {
            int tracerPid = NativeDetector.nativeDetectTracerPid();
            boolean detected = tracerPid != 0;
            return new DetectionResult("Native TracerPid 检测", detected,
                    detected ? "被调试! TracerPid=" + tracerPid : "TracerPid=0 (正常)");
        } catch (Exception e) {
            return new DetectionResult("Native TracerPid 检测", false, "失败: " + e.getMessage());
        }
    }

    private static DetectionResult nativeDetectMounts() {
        try {
            String found = NativeDetector.nativeDetectMounts();
            boolean detected = found != null && !found.isEmpty();
            return new DetectionResult("Native 挂载点扫描", detected,
                    detected ? "发现magisk挂载: " + found : "未发现");
        } catch (Exception e) {
            return new DetectionResult("Native 挂载点扫描", false, "失败: " + e.getMessage());
        }
    }
}
