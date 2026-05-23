package com.pass.framework;

import java.io.File;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {

    private static final String[] BLOCKED = {
        "/system/bin/nemuinit",  "/system/bin/magisk",  "/system/bin/su",
        "/system/xbin/su",       "/system/xbin/magisk",  "/sbin/magisk",
        "/sbin/magisk32",        "/sbin/magisk64",        "/sbin/.magisk",
        "/data/adb/magisk",      "/data/adb/modules",     "/data/adb/magisk.db",
        "/system/etc/init/magisk", "/system/etc/init/magisk.rc",
        "/data/local/tmp/frida-server", "/system/bin/frida-server",
        "/data/adb/lspd",
        "/data/data/com.mumu.store", "/data/data/com.mumu.acc",
        "/data/data/com.nemu.nlp", "/data/data/com.nemu.oaidmanager",
        "/system/priv-app/com.mumu.store", "/system/priv-app/com.mumu.acc",
        "/system/priv-app/com.nemu.nlp",
    };

    private static boolean blocked(String p) {
        if (p == null) return false;
        for (String b : BLOCKED) if (p.startsWith(b)) return true;
        return false;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {
        XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if (blocked(((File)p.thisObject).getAbsolutePath())) p.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod(File.class, "isFile", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if (blocked(((File)p.thisObject).getAbsolutePath())) p.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod(File.class, "isDirectory", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if (blocked(((File)p.thisObject).getAbsolutePath())) p.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod(File.class, "list", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if (blocked(((File)p.thisObject).getAbsolutePath())) p.setResult(null);
            }
        });
        XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if ("getenforce".equals(((String)p.args[0]).trim())) {
                    try { p.setResult(new ProcessBuilder("sh","-c","echo Enforcing").start()); }
                    catch (Exception e) {}
                }
            }
        });
    }
}
