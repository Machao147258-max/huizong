#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/stat.h>
#include <android/log.h>

#define TAG "NativeDetect"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define MAX_RESULT 8192

// ====== 工具函数 ======

static void append_found(char* result, const char* path) {
    if (strlen(result) + strlen(path) + 2 < MAX_RESULT) {
        strcat(result, path);
        strcat(result, "|");
    }
}

static int file_exists_via_fopen(const char* path) {
    FILE* f = fopen(path, "r");
    if (f) { fclose(f); return 1; }
    return 0;
}

static int file_exists_via_access(const char* path) {
    return access(path, F_OK) == 0 ? 1 : 0;
}

static int dir_exists_via_opendir(const char* path) {
    DIR* d = opendir(path);
    if (d) { closedir(d); return 1; }
    return 0;
}

// ====== 1. Native fopen 检测 Root/Magisk/su 文件 ======
// 直调 fopen，完全绕过 Java File.exists() Hook
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectRootFiles(JNIEnv* env, jclass clazz) {
    static const char* paths[] = {
        "/sbin/magisk", "/sbin/magisk32", "/sbin/magisk64",
        "/sbin/.magisk",
        "/system/bin/magisk", "/system/xbin/magisk",
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/data/adb/magisk", "/data/adb/magisk.db", "/data/adb/modules",
        "/system/etc/init/magisk", "/system/etc/init/magisk.rc",
        "/system/etc/init/magisk/magiskinit",
        "/system/etc/init/magisk/magiskpolicy",
        "/cache/.disable_magisk",
        NULL
    };
    char result[MAX_RESULT] = {0};
    for (int i = 0; paths[i]; i++) {
        if (file_exists_via_fopen(paths[i])) {
            append_found(result, paths[i]);
        }
    }
    return (*env)->NewStringUTF(env, result);
}

// ====== 2. Native access()/stat() 检测 (更底层) ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectByAccess(JNIEnv* env, jclass clazz) {
    static const char* paths[] = {
        "/system/bin/su", "/sbin/magisk",
        "/data/adb/magisk", "/system/etc/init/magisk",
        "/dev/goldfish_pipe", "/dev/vboxguest",
        NULL
    };
    char result[MAX_RESULT] = {0};
    for (int i = 0; paths[i]; i++) {
        if (file_exists_via_access(paths[i])) {
            append_found(result, paths[i]);
        }
        // stat() 二次确认
        struct stat st;
        if (stat(paths[i], &st) == 0) {
            char buf[64];
            snprintf(buf, sizeof(buf), "%s(stat_ok)", paths[i]);
            append_found(result, buf);
        }
    }
    return (*env)->NewStringUTF(env, result);
}

// ====== 3. Native popen("getenforce") 绕过 Runtime.exec Hook ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectSelinux(JNIEnv* env, jclass clazz) {
    FILE* p = popen("getenforce", "r");
    char status[64] = "unknown";
    if (p) {
        if (fgets(status, sizeof(status), p) == NULL) {
            strcpy(status, "read_error");
        }
        int len = strlen(status);
        if (len > 0 && (status[len-1] == '\n' || status[len-1] == '\r'))
            status[len-1] = '\0';
        if (len > 1 && (status[len-2] == '\r'))
            status[len-2] = '\0';
        pclose(p);
    }
    return (*env)->NewStringUTF(env, status);
}

// ====== 4. Native popen("getprop") 绕过 SystemProperties Hook ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectProps(JNIEnv* env, jclass clazz) {
    static const char* props[] = {
        "ro.build.tags", "ro.debuggable", "ro.secure",
        "ro.kernel.qemu", "ro.kernel.qemu.gles",
        "ro.hardware", "ro.product.cpu.abi",
        "ro.boot.hardware",
        NULL
    };
    char result[MAX_RESULT] = {0};
    for (int i = 0; props[i]; i++) {
        char cmd[256];
        snprintf(cmd, sizeof(cmd), "getprop %s", props[i]);
        FILE* p = popen(cmd, "r");
        if (p) {
            char val[256] = {0};
            if (fgets(val, sizeof(val), p)) {
                int len = strlen(val);
                if (len > 0 && val[len-1] == '\n') val[len-1] = '\0';
                if (len > 1 && val[len-2] == '\r') val[len-2] = '\0';
                char buf[512];
                snprintf(buf, sizeof(buf), "%s=%s", props[i], val);
                append_found(result, buf);
            }
            pclose(p);
        }
    }
    return (*env)->NewStringUTF(env, result);
}

// ====== 5. Native 读 /proc/self/maps 检测注入 SO ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectProcMaps(JNIEnv* env, jclass clazz) {
    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return (*env)->NewStringUTF(env, "");
    char line[512], result[MAX_RESULT] = {0};
    static const char* keywords[] = {
        "libriru", "liblspd", "frida", "libfrida",
        "lsposed", "libxposed", "libsubstrate",
        "libhook", "libinject", "gadget",
        "xposed", "edxp", NULL
    };
    while (fgets(line, sizeof(line), f)) {
        for (int i = 0; keywords[i]; i++) {
            if (strstr(line, keywords[i])) {
                char buf[512];
                int len = strlen(line);
                if (len > 0 && line[len-1] == '\n') line[len-1] = '\0';
                snprintf(buf, sizeof(buf), "%s -> %s", keywords[i], line);
                append_found(result, buf);
                break;
            }
        }
    }
    fclose(f);
    return (*env)->NewStringUTF(env, result);
}

// ====== 6. Native 读 /proc/self/status 检测 TracerPid ======
JNIEXPORT jint JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectTracerPid(JNIEnv* env, jclass clazz) {
    FILE* f = fopen("/proc/self/status", "r");
    if (!f) return -1;
    char line[128];
    int tracerPid = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            sscanf(line + 10, "%d", &tracerPid);
            break;
        }
    }
    fclose(f);
    return (jint)tracerPid;
}

// ====== 7. Native 读 /proc/mounts 检测 magisk 挂载 ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectMounts(JNIEnv* env, jclass clazz) {
    FILE* f = fopen("/proc/self/mounts", "r");
    if (!f) f = fopen("/proc/mounts", "r");
    if (!f) return (*env)->NewStringUTF(env, "");
    char line[512], result[MAX_RESULT] = {0};
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "magisk") || strstr(line, ".magisk")) {
            int len = strlen(line);
            if (len > 0 && line[len-1] == '\n') line[len-1] = '\0';
            append_found(result, line);
        }
    }
    fclose(f);
    return (*env)->NewStringUTF(env, result);
}

// ====== 8. Native 读 /proc/cpuinfo 检测模拟器特征 ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectCpuinfo(JNIEnv* env, jclass clazz) {
    FILE* f = fopen("/proc/cpuinfo", "r");
    if (!f) return (*env)->NewStringUTF(env, "");
    char line[256], result[MAX_RESULT] = {0};
    static const char* emu_keywords[] = {
        "goldfish", "qemu", "ranchu", NULL
    };
    while (fgets(line, sizeof(line), f)) {
        for (int i = 0; emu_keywords[i]; i++) {
            if (strstr(line, emu_keywords[i])) {
                int len = strlen(line);
                if (len > 0 && line[len-1] == '\n') line[len-1] = '\0';
                append_found(result, line);
                break;
            }
        }
    }
    fclose(f);
    return (*env)->NewStringUTF(env, result);
}

// ====== 9. Native 检测 /proc/tty/drivers 中的 goldfish ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeDetectTtyDrivers(JNIEnv* env, jclass clazz) {
    FILE* f = fopen("/proc/tty/drivers", "r");
    if (!f) return (*env)->NewStringUTF(env, "");
    char line[256], result[MAX_RESULT] = {0};
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "goldfish")) {
            int len = strlen(line);
            if (len > 0 && line[len-1] == '\n') line[len-1] = '\0';
            append_found(result, line);
        }
    }
    fclose(f);
    return (*env)->NewStringUTF(env, result);
}

// ====== 10. Native system("which su") 绕过 Runtime.exec ======
JNIEXPORT jstring JNICALL
Java_com_detector_roothook_util_NativeDetector_nativeWhichSu(JNIEnv* env, jclass clazz) {
    FILE* p = popen("which su", "r");
    char path[256] = {0};
    if (p) {
        if (fgets(path, sizeof(path), p)) {
            int len = strlen(path);
            if (len > 0 && path[len-1] == '\n') path[len-1] = '\0';
        }
        pclose(p);
    }
    return (*env)->NewStringUTF(env, path);
}
