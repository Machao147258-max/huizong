cmake_minimum_required(VERSION 3.18.1)
project("nativelib")

add_library(native_detector SHARED
    native_detector.c
)

target_link_libraries(native_detector
    android
    log
)
