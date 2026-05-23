LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := native_detector
LOCAL_SRC_FILES := native_detector.c
LOCAL_LDLIBS    := -llog
include $(BUILD_SHARED_LIBRARY)
