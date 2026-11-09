/*
 * Target-specific JNI definitions for cross-compiling with Zig.
 *
 * The platform-independent jni.h is taken from the build host JDK. This file
 * replaces the host-specific <jdk>/include/<host-os>/jni_md.h so that the
 * generated library always uses the definitions of the *target* platform.
 */
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#if defined(__GNUC__) || defined(__clang__)
  #define JNIEXPORT __attribute__((visibility("default")))
  #define JNIIMPORT __attribute__((visibility("default")))
#else
  #define JNIEXPORT
  #define JNIIMPORT
#endif

#ifndef JNICALL
  #define JNICALL
#endif

typedef int jint;
#if defined(_LP64)
typedef long jlong;
#else
typedef long long jlong;
#endif
typedef signed char jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
