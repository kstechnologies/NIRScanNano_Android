#include <string.h>
#include <stdio.h>
#include <jni.h>
#include "dlpspec_scan.h"
#include "dlpspec_helper.h"

#include <android/log.h>

/**
 * @brief JNI Spectrum C library interface file.
 *
 * This file serves as a JNI wrapper for the spectrum C library. It takes JNI parameters, 
 * and returns jobjects, but makes C function calls internally. The data returned from the
 * C functions must be converted into JNI types.
 *
 * The following functions search for the classes used return the jobjects. The class path has to match
 * the actual java class location exactly or an error will occur.
 */

#define  LOG_TAG    "SCAN"

//Define macros for logging. messages will be available in the Android Monitor inside the IDE
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * @brief Function for interpreting scan data along with reference calibration data.
 * This function requires two Spectrum C Library calls  dlpspec_scan_interpret and dlpspec_scan_interpReference.
 *
 * WARNING: The prefix of this function must exactly it's project location to work properly
 *
 * @param[in] data      The serialzed scan data
 * @param[in] coeff     The serialized calibration coefficients
 * @param[in] matrix    The serialized calibration matrix
 *
 * @param[out] returnStruct a jobject that is initialized the the class specified by clazz.
 * The constructor for the class must be mapped to JNI parameters. It must be cast to the
 * appropriate data type inside the application
 */
JNIEXPORT jobject JNICALL Java_com_kstechnologies_nirscannanolibrary_KSTNanoSDK_dlpSpecScanInterpReference(JNIEnv* env, jobject thiz,jbyteArray data, jbyteArray coeff, jbyteArray matrix){
    
    
    if(env == NULL){
        LOGE("ENV is null");
    }
    
    jclass clazz = (*env)->FindClass(env, "com/kstechnologies/nirscannanolibrary/KSTNanoSDK$ScanResults");
    if(clazz == NULL){
        LOGE("results is null!");
    }
    jmethodID constructorz = (*env)->GetMethodID(env,clazz,"<init>","([D[I[II)V");
    if(constructorz == NULL){
        LOGE("contructorz is null!");
    }
    
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
    jsize lengthOfArray = (*env)->GetArrayLength(env, data);
    
    jbyte* coeffBuff = (*env)->GetByteArrayElements(env, coeff, NULL);
    jsize coeffBuffLen = (*env)->GetArrayLength(env, coeff);
    
    jbyte* matrixBuff = (*env)->GetByteArrayElements(env, matrix, NULL);
    jsize matrixBuffLen = (*env)->GetArrayLength(env, matrix);
    
    scanResults pResults;
    int specCRetVal;
    uScanData * dlpData;
    jint  result= dlpspec_scan_interpret(bufferPtr,lengthOfArray,&pResults);
    
    scanResults pRefResults;
    result= dlpspec_scan_interpReference(coeffBuff,coeffBuffLen,matrixBuff,matrixBuffLen,&pResults,&pRefResults);
    
    jint dataSize = pRefResults.length;
    jdoubleArray resultWavelength = (*env)->NewDoubleArray(env, dataSize);
    (*env)->SetDoubleArrayRegion(env, resultWavelength, 0, dataSize, pRefResults.wavelength);
    
    
    jintArray resultIntensity = (*env)->NewIntArray(env, dataSize);
    (*env)->SetIntArrayRegion(env, resultIntensity, 0, dataSize, pRefResults.intensity);
    
    jintArray uncalibIntensity = (*env)->NewIntArray(env, dataSize);
    (*env)->SetIntArrayRegion(env, uncalibIntensity, 0, dataSize, pResults.intensity);
    
    
    jobject returnStruct = (*env)->NewObject(env, clazz, constructorz, resultWavelength, resultIntensity,uncalibIntensity,pResults.length);
    
    
    if(returnStruct == NULL){
        return NULL;
    }
    return returnStruct;
}

/**
 * @brief Function for reading the scan configuration.
 *
 *  This function requires one Spectrum C Library call dlpspec_scan_read_configuration
 *
 * WARNING: The prefix of this function must exactly it's project location to work properly
 *
 * @param[in] data      The serialzed scan data
 *
 * @param[out] returnStruct a jobject that is initialized the the class specified by clazz. 
 * The constructor for the class must be mapped to JNI parameters. It must be cast to the
 * appropriate data type inside the application
 */
JNIEXPORT jobject JNICALL Java_com_kstechnologies_nirscannanolibrary_KSTNanoSDK_dlpSpecScanReadConfiguration(JNIEnv* env, jobject thiz,jbyteArray data){
    
    if(env == NULL){
        LOGE("ENV is null");
    }
    
    
    
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
    jsize lengthOfArray = (*env)->GetArrayLength(env, data);
    
    
    int specCRetVal;
    jobject returnStruct;
    
    jint serialNumberSize = 8;
    jint configNameSize = 40;
    
    if(dlpspec_is_slewcfgtype((void *)bufferPtr,lengthOfArray)){
        
        jclass clazz = (*env)->FindClass(env, "com/kstechnologies/nirscannanolibrary/KSTNanoSDK$ScanConfiguration");
        if(clazz == NULL){
            LOGE("scan configuration class is null!");
        }
        jmethodID constructorz = (*env)->GetMethodID(env,clazz,"<init>","(II[B[BB[B[B[I[I[I[I[I)V");
        if(constructorz == NULL){
            LOGE("contructorz is null!");
        }
        
        
        uScanConfig pConfig;
        
        jint  result= dlpspec_scan_read_configuration(bufferPtr,lengthOfArray);
        memset(&pConfig,0,sizeof(uScanConfig));
        memcpy(&pConfig,bufferPtr,sizeof(uScanConfig));
        
        jbyteArray configName = (*env)->NewByteArray(env, configNameSize);
        (*env)->SetByteArrayRegion(env, configName, 0, configNameSize, pConfig.scanCfg.config_name);
        
        jbyteArray serialNumber = (*env)->NewByteArray(env,serialNumberSize);
        (*env)->SetByteArrayRegion(env, serialNumber, 0, serialNumberSize, pConfig.scanCfg.ScanConfig_serial_number);

        jbyteArray sectionScanTypes = (*env)->NewByteArray(env, pConfig.slewScanCfg.head.num_sections);

        jbyteArray sectionWidths = (*env)->NewByteArray(env, pConfig.slewScanCfg.head.num_sections);

        jintArray sectionWavelengthStartNm = (*env)->NewIntArray(env, pConfig.slewScanCfg.head.num_sections);

        jintArray sectionWavelengthEndNm = (*env)->NewIntArray(env, pConfig.slewScanCfg.head.num_sections);

        jintArray sectionNumPatterns = (*env)->NewIntArray(env, pConfig.slewScanCfg.head.num_sections);

        jintArray sectionNumRepeats = (*env)->NewIntArray(env, pConfig.slewScanCfg.head.num_sections);

        jintArray sectionExposureTime = (*env)->NewIntArray(env, pConfig.slewScanCfg.head.num_sections);
        
        int i;
        for(i = 0; i < pConfig.slewScanCfg.head.num_sections; i++){
            
            (*env)->SetByteArrayRegion(env, sectionScanTypes, i, 1, (jbyte*)&pConfig.slewScanCfg.section[i].section_scan_type);
            (*env)->SetByteArrayRegion(env, sectionWidths, i, 1, (jbyte*)&pConfig.slewScanCfg.section[i].width_px);
            (*env)->SetIntArrayRegion(env, sectionWavelengthStartNm, i, 1, (jint*)&pConfig.slewScanCfg.section[i].wavelength_start_nm);
            (*env)->SetIntArrayRegion(env, sectionWavelengthEndNm, i, 1, (jint*)&pConfig.slewScanCfg.section[i].wavelength_end_nm);
            (*env)->SetIntArrayRegion(env, sectionNumPatterns, i, 1, (jint*)&pConfig.slewScanCfg.section[i].num_patterns);
            (*env)->SetIntArrayRegion(env, sectionNumRepeats, i, 1, (jint*)&pConfig.slewScanCfg.head.num_repeats);
            (*env)->SetIntArrayRegion(env, sectionExposureTime, i, 1, (jint*)&pConfig.slewScanCfg.section[i].exposure_time);
        }
        
        
        returnStruct = (*env)->NewObject(env, clazz, constructorz, pConfig.scanCfg.scan_type, pConfig.scanCfg.scanConfigIndex,serialNumber, configName,pConfig.slewScanCfg.head.num_sections, sectionScanTypes,sectionWidths,sectionWavelengthStartNm,sectionWavelengthEndNm,sectionNumPatterns,sectionNumRepeats,sectionExposureTime);
        
    }else{
        
        jclass clazz = (*env)->FindClass(env, "com/kstechnologies/nirscannanolibrary/KSTNanoSDK$ScanConfiguration");
        if(clazz == NULL){
            LOGE("scan configuration is null!");
        }
        jmethodID constructorz = (*env)->GetMethodID(env,clazz,"<init>","(II[B[BIIIII)V");
        if(constructorz == NULL){
            LOGE("contructorz is null!");
        }
        
        uScanConfig pConfig;
        
        jint  result= dlpspec_scan_read_configuration(bufferPtr,lengthOfArray);
        memset(&pConfig,0,sizeof(uScanConfig));
        memcpy(&pConfig,bufferPtr,sizeof(uScanConfig));
        
        jbyteArray configName = (*env)->NewByteArray(env, configNameSize);
        (*env)->SetByteArrayRegion(env, configName, 0, configNameSize, pConfig.scanCfg.config_name);
        
        jbyteArray serialNumber = (*env)->NewByteArray(env,serialNumberSize);
        (*env)->SetByteArrayRegion(env, serialNumber, 0, serialNumberSize, pConfig.scanCfg.ScanConfig_serial_number);
        
        
        returnStruct = (*env)->NewObject(env, clazz, constructorz,
                                         pConfig.scanCfg.scan_type,
                                         pConfig.scanCfg.scanConfigIndex,
                                         serialNumber,
                                         configName,
                                         pConfig.scanCfg.wavelength_start_nm,
                                         pConfig.scanCfg.wavelength_end_nm,
                                         pConfig.scanCfg.width_px,
                                         pConfig.scanCfg.num_patterns,
                                         pConfig.scanCfg.num_repeats);
    }
    
    if(returnStruct == NULL){
        return NULL;
    }
    return returnStruct;
}
