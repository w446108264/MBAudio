#ifndef ANDROIDMP3RECORDER_LAMEUTIL_H
#define ANDROIDMP3RECORDER_LAMEUTIL_H

#include <stdio.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_mb_lame_util_LameUtil_init(JNIEnv *, jclass, jint, jint, jint, jint, jint);

JNIEXPORT jint JNICALL Java_mb_lame_util_LameUtil_encode(JNIEnv *, jclass, jshortArray, jshortArray, jint, jbyteArray);

JNIEXPORT jint JNICALL Java_mb_lame_util_LameUtil_flush(JNIEnv *, jclass, jbyteArray);

JNIEXPORT void JNICALL Java_mb_lame_util_LameUtil_close(JNIEnv *, jclass);

JNIEXPORT jstring JNICALL Java_mb_lame_util_LameUtil_getLameVersion
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif

#endif