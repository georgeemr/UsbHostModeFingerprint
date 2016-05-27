LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := Prebuild_usb1.0
LOCAL_SRC_FILES :=$(TARGET_ARCH_ABI)/libusb-1.0.so
include $(PREBUILT_SHARED_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_MODULE    := Prebuild_ftrScanAPI
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libftrScanAPI.so
include $(PREBUILT_SHARED_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_MODULE    := Prebuild_ftrAnsiSDK
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libftrAnsiSDK.so
include $(PREBUILT_SHARED_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_MODULE    := ftrAnsiSDKAndroidJni
LOCAL_CFLAGS := -D__ANDROID_API__
LOCAL_SRC_FILES := ftrAnsiSDKAndroidJni.cpp
LOCAL_SHARED_LIBRARIES := Prebuild_usb1.0 Prebuild_ftrScanAPI Prebuild_ftrAnsiSDK
include $(BUILD_SHARED_LIBRARY)
