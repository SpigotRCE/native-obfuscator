/*
 * Target-specific JNI definitions for cross-compiling with Zig.
 *
 * The platform-independent jni.h is taken from the build host JDK. This file
 * replaces the host-specific <jdk>/include/<host-os>/jni_md.h so that the
 * generated library always uses the definitions of the *target* platform.
 *
 * Only 64-bit Windows targets are supported: modern jni.h hardcodes an empty
 * JNICALL, which would break the __stdcall convention of 32-bit JNI.
 */
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#ifndef JNIEXPORT
  #define JNIEXPORT __declspec(dllexport)
#endif
#ifndef JNIIMPORT
  #define JNIIMPORT __declspec(dllimport)
#endif

#ifndef JNICALL
  #define JNICALL
#endif

typedef int jint;
typedef long long jlong;
typedef signed char jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
