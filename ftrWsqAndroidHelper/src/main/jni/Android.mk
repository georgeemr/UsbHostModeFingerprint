LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := Prebuild_usb1.0
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libusb-1.0.so
include $(PREBUILT_SHARED_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_MODULE    := Prebuild_ftrScanAPI
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libftrScanAPI.so
include $(PREBUILT_SHARED_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_MODULE    := ftrWSQAndroid
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libftrWSQAndroid.so
include $(PREBUILT_SHARED_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_MODULE    := ftrWSQAndroidJni
LOCAL_SRC_FILES := ftrWSQAndroidJni.cpp
LOCAL_SHARED_LIBRARIES := prebuild_usb1.0 Prebuild_ftrScanAPI ftrWSQAndroid
include $(BUILD_SHARED_LIBRARY)
