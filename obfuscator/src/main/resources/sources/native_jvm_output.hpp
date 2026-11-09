/*
 * Emitted runtime of native-obfuscator.
 *
 * Licensed under GPL-3.0 WITH an Output Exception: you may link, embed,
 * compile and distribute this file in your own programs under terms of
 * your choice. See the "Output Exception" section in the project's
 * LICENSE for the full terms.
 *
 * https://github.com/radioegor146/native-obfuscator#licensing
 */

#include "native_jvm.hpp"

#ifndef NATIVE_JVM_OUTPUT_HPP_GUARD

#define NATIVE_JVM_OUTPUT_HPP_GUARD

namespace native_jvm {
    void prepare_lib(JNIEnv *env);
}

#endif