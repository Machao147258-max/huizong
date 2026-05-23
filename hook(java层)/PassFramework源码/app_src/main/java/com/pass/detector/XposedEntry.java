package com.pass.detector;

import android.os.Process;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TARGET_PKG = "com.detector.roothook";

    private static final String[] BLOCKED_PATHS = {
        "/system/bin/nemuinit",
        "/system/bin/magisk",
        "/system/bin/su",
        "/sbin/magisk",
        "/sbin/magisk32",
        "/sbin/magisk64",
        "/sbin/.magisk",
        "/system/xbin/su",
        "/system/xbin/magisk",
        "/data/adb/magisk",
        "/data/adb/modules",
        "/data/adb/magisk.db",
        "/system/etc/init/magisk",
        "/system/etc/init/magisk.rc",
        "/data/local/tmp/frida-server",
        "/data/local/tmp/re.frida.server",
        "/data/local/tmp/fs",
        "/system/bin/frida-server",
        "/data/adb/lspd",
        "/data/adb/modules/zygisk_lsposed",
        "/data/data/com.mumu.store",
        "/data/data/com.mumu.acc",
        "/data/data/com.nemu.nlp",
        "/data/data/com.nemu.oaidmanager",
        "/system/priv-app/com.mumu.store",
        "/system/priv-app/com.mumu.acc",
        "/system/priv-app/com.nemu.nlp",
        "/dev/goldfish_pipe",
        "/dev/vboxguest",
    };

    private static boolean isBlocked(String path) {
        if (path == null) return false;
        for (String b : BLOCKED_PATHS) {
            if (path.startsWith(b)) return true;
        }
        return false;
    }

    @Override
    public void initZygote(StartupParam param) {
        // nothing needed at zygote level
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName)) return;

        XposedBridge.log("[PassDetector] Hooking into " + TARGET_PKG);

        // --- Hook File.exists() ---
        XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File f = (File) param.thisObject;
                if (isBlocked(f.getAbsolutePath())) {
                    param.setResult(false);
                }
            }
        });

        // --- Hook File.isDirectory() ---
        XposedHelpers.findAndHookMethod(File.class, "isDirectory", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File f = (File) param.thisObject;
                if (isBlocked(f.getAbsolutePath())) {
                    param.setResult(false);
                }
            }
        });

        // --- Hook File.isFile() ---
        XposedHelpers.findAndHookMethod(File.class, "isFile", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File f = (File) param.thisObject;
                if (isBlocked(f.getAbsolutePath())) {
                    param.setResult(false);
                }
            }
        });

        // --- Hook File.canRead() ---
        XposedHelpers.findAndHookMethod(File.class, "canRead", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File f = (File) param.thisObject;
                if (isBlocked(f.getAbsolutePath())) {
                    param.setResult(false);
                }
            }
        });

        // --- Hook File.list() ---
        XposedHelpers.findAndHookMethod(File.class, "list", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File f = (File) param.thisObject;
                if (isBlocked(f.getAbsolutePath())) {
                    param.setResult(null);
                }
            }
        });

        // --- Hook File.listFiles() ---
        XposedHelpers.findAndHookMethod(File.class, "listFiles", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File f = (File) param.thisObject;
                if (isBlocked(f.getAbsolutePath())) {
                    param.setResult(null);
                }
            }
        });

        // --- Hook Runtime.exec(String) to intercept getenforce ---
        XposedHelpers.findAndHookMethod(java.lang.Runtime.class, "exec",
            String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String cmd = (String) param.args[0];
                if (cmd != null && cmd.trim().equals("getenforce")) {
                    XposedBridge.log("[PassDetector] Intercepted getenforce");
                    // Return a fake process that outputs "Enforcing"
                    try {
                        java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(
                            "sh", "-c", "echo Enforcing; exit 0");
                        param.setResult(pb.start());
                    } catch (Exception ignored) {}
                }
            }
        });

        // --- Hook Runtime.exec(String[]) ---
        XposedHelpers.findAndHookMethod(java.lang.Runtime.class, "exec",
            String[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String[] cmds = (String[]) param.args[0];
                if (cmds != null && cmds.length > 0 && "getenforce".equals(cmds[0])) {
                    try {
                        java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(
                            "sh", "-c", "echo Enforcing; exit 0");
                        param.setResult(pb.start());
                    } catch (Exception ignored) {}
                }
            }
        });

        // --- Hook Runtime.exec(String, String[]) ---
        XposedHelpers.findAndHookMethod(java.lang.Runtime.class, "exec",
            String.class, String[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String cmd = (String) param.args[0];
                if (cmd != null && cmd.trim().equals("getenforce")) {
                    try {
                        java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(
                            "sh", "-c", "echo Enforcing; exit 0");
                        param.setResult(pb.start());
                    } catch (Exception ignored) {}
                }
            }
        });

        // --- Hook ProcessBuilder.start() to intercept any remaining commands ---
        XposedHelpers.findAndHookMethod(java.lang.ProcessBuilder.class, "start", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                java.lang.ProcessBuilder pb = (java.lang.ProcessBuilder) param.thisObject;
                java.util.List<String> cmd = pb.command();
                if (cmd != null && cmd.size() > 0) {
                    String first = cmd.get(0);
                    if ("getenforce".equals(first) ||
                        (first != null && first.endsWith("/getenforce")) ||
                        (cmd.size() >= 3 && "su".equals(first) && "-c".equals(cmd.get(1)))) {
                        // Block su commands
                        XposedBridge.log("[PassDetector] Blocked command: " + cmd);
                        throw new SecurityException("Blocked");
                    }
                }
            }
        });

        XposedBridge.log("[PassDetector] All hooks installed for " + TARGET_PKG);
    }
}
