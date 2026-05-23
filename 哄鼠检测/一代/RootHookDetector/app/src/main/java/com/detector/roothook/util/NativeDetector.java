package com.detector.roothook.util;

public class NativeDetector {
    static {
        System.loadLibrary("native_detector");
    }

    public static native String nativeDetectRootFiles();

    public static native String nativeDetectByAccess();

    public static native String nativeDetectSelinux();

    public static native String nativeDetectProps();

    public static native String nativeDetectProcMaps();

    public static native int nativeDetectTracerPid();

    public static native String nativeDetectMounts();

    public static native String nativeDetectCpuinfo();

    public static native String nativeDetectTtyDrivers();

    public static native String nativeWhichSu();
}
