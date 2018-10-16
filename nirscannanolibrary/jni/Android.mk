LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := dlpspectrum
LOCAL_SRC_FILES := interface.c dlpspec.c dlpspec_scan.c dlpspec_calib.c dlpspec_util.c tpl.c dlpspec_scan_col.c dlpspec_scan_had.c dlpspec_helper.c
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)
