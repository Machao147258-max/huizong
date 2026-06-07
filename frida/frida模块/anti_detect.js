/**
 * Frida Anti-Detect Shield v2.0
 * Auto-protects target app from root/magisk/frida detection.
 */

const BLOCKED = [
    "/sbin/magisk", "/sbin/.magisk",
    "/system/bin/su", "/system/xbin/su", "/sbin/su",
    "/system/bin/magisk", "/system/xbin/magisk",
    "/data/adb/magisk", "/data/adb/modules", "/data/adb/magisk.db",
    "/system/etc/init/magisk",
    "/data/local/tmp/frida-server", "/data/local/tmp/frida",
    "/data/local/tmp/nosuke-server", "/data/local/tmp/.cache/sys-helper",
    "/system/bin/frida-server", "/data/local/tmp/re.frida.server",
    "/data/adb/lspd", "/data/adb/modules/zygisk_lsposed",
    "/dev/goldfish_pipe", "/dev/vboxguest", "/dev/vboxuser",
    "/dev/socket/qemud", "/dev/qemu_pipe",
    "/data/data/com.mumu.store", "/data/data/com.mumu.acc",
    "/data/data/com.nemu.nlp",
];

function blocked(path) {
    if (!path) return false;
    for (let b of BLOCKED) if (path.startsWith(b)) return true;
    return false;
}

// ====== Native libc hooks ======
const libc = Process.getModuleByName("libc.so");
["fopen", "open", "access", "stat"].forEach(name => {
    const addr = libc.getExportByName(name);
    if (!addr) return;
    Interceptor.attach(addr, {
        onEnter(args) { this.hit = blocked(args[0].readCString()); },
        onLeave(r) { if (this.hit) r.replace(name === "fopen" ? ptr(0) : ptr(-1)); }
    });
});
console.log("[Shield] libc: fopen/open/access/stat");

Java.perform(function() {
    // File.*
    const File = Java.use("java.io.File");
    const origExists = File.exists;
    File.exists.implementation = function() {
        if (blocked(this.getAbsolutePath())) return false;
        return origExists.call(this);
    };

    // Runtime.exec
    const Runtime = Java.use("java.lang.Runtime");
    Runtime.exec.overload('java.lang.String').implementation = function(cmd) {
        if (cmd && cmd.indexOf("getenforce") >= 0)
            return Java.use("java.lang.ProcessBuilder").$new(
                Java.array("java.lang.String", ["sh","-c","echo Enforcing"])).start();
        if (cmd && (cmd.indexOf("frida") >= 0 || cmd.indexOf("sys-helper") >= 0 || cmd.indexOf("nosuke") >= 0))
            throw Java.use("java.lang.SecurityException").$new("Permission denied");
        return this.exec(cmd);
    };

    // SystemProperties
    try {
        const SP = Java.use("android.os.SystemProperties");
        SP.get.overload('java.lang.String').implementation = function(key) {
            if (key === "ro.build.tags") return "release-keys";
            if (key === "ro.debuggable") return "0";
            if (key === "ro.secure") return "1";
            if (key === "ro.kernel.qemu") return "0";
            if (key === "ro.hardware") return "qcom";
            if (key === "ro.product.cpu.abi") return "arm64-v8a";
            return this.get(key);
        };
        SP.getInt.overload('java.lang.String','int').implementation = function(key, def) {
            if (key === "ro.debuggable") return 0;
            if (key === "ro.kernel.qemu") return 0;
            return this.getInt(key, def);
        };
    } catch(e) {}

    // PackageManager
    try {
        const PM = Java.use("android.app.ApplicationPackageManager");
        const HIDDEN = ["de.robv.android.xposed.installer","org.lsposed.manager",
            "com.topjohnwu.magisk","io.github.huskydg.magisk","com.pass.framework"];
        PM.getPackageInfo.overload('java.lang.String','int').implementation = function(pkg, flags) {
            if (HIDDEN.indexOf(pkg) >= 0)
                throw Java.use("android.content.pm.PackageManager$NameNotFoundException").$new();
            return this.getPackageInfo(pkg, flags);
        };
    } catch(e) {}

    // ClassLoader
    try {
        const CL = Java.use("java.lang.ClassLoader");
        CL.loadClass.overload('java.lang.String','boolean').implementation = function(name, resolve) {
            if (name && (name.startsWith("de.robv.android.xposed") ||
                name.startsWith("org.lsposed") || name.startsWith("com.topjohnwu.magisk")))
                throw Java.use("java.lang.ClassNotFoundException").$new(name);
            return this.loadClass(name, resolve);
        };
    } catch(e) {}

    // StackTrace
    try {
        const Throwable = Java.use("java.lang.Throwable");
        Throwable.getStackTrace.implementation = function() {
            var trace = this.getStackTrace();
            var filtered = [];
            for (var i = 0; i < trace.length; i++) {
                var cn = trace[i].getClassName();
                if (cn.indexOf("de.robv.android.xposed") < 0 &&
                    cn.indexOf("org.lsposed") < 0 && cn.indexOf("com.pass.") < 0)
                    filtered.push(trace[i]);
            }
            return Java.array("java.lang.StackTraceElement", filtered);
        };
    } catch(e) {}

    console.log("[Shield] Java: File/Runtime/SP/PM/CL/StackTrace");
});
console.log("[Shield] All defenses active");
