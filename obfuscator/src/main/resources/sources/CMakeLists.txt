cmake_minimum_required(VERSION 3.8)
project($projectname)
set(CMAKE_CXX_STANDARD 17)

# Honor output directories provided on the command line (used by the Zig
# presets below to collect every cross-compiled library in one place).
if(NOT DEFINED CMAKE_ARCHIVE_OUTPUT_DIRECTORY)
    set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY build/lib)
endif()
if(NOT DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY build/lib)
endif()
if(NOT DEFINED CMAKE_RUNTIME_OUTPUT_DIRECTORY)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY build/lib)
endif()

if("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS 6.0)
        message(FATAL_ERROR "GCC version must be at least 6.0!")
    endif()
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -s -DNDEBUG")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "MSVC")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY build/lib)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_DEBUG build/lib)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE build/lib)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY build/lib)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_DEBUG build/lib)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE build/lib)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /MP8 /wd4102 /wd4101 /EHsc")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "AppleClang")
else()
    message(AUTHOR_WARNING "You are using an unsupported compiler! Compilation has only been tested with MSVC, GNU, Clang and AppleClang.")
endif()

if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    set(NATIVE_OS "linux")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    set(NATIVE_OS "windows")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    set(NATIVE_OS "macos")
else()
    set(NATIVE_OS "raw${CMAKE_SYSTEM_NAME}")
endif()

string(TOLOWER "${CMAKE_SYSTEM_PROCESSOR}" _arch_lower)
if(_arch_lower MATCHES "^(x86_64|amd64)")
    set(NATIVE_ARCH "x64")
elseif(_arch_lower MATCHES "^(aarch64|arm64)")
    set(NATIVE_ARCH "arm64")
elseif(_arch_lower MATCHES "^arm")
    set(NATIVE_ARCH "arm32")
elseif(_arch_lower MATCHES "^(i[3-6]86|x86)$")
    set(NATIVE_ARCH "x86")
else()
    set(NATIVE_ARCH "raw${CMAKE_SYSTEM_PROCESSOR}")
endif()

if(DEFINED ZIG_TARGET AND NOT "${ZIG_TARGET}" STREQUAL "")
    # Cross-compiling with Zig. The host JDK provides the platform-independent
    # jni.h, while zig-jni/<OS> provides the target-specific jni_md.h.
    if(NOT NATIVE_OBFUSCATOR_JNI_INCLUDE_DIR)
        if(NOT JAVA_HOME AND DEFINED ENV{JAVA_HOME})
            set(JAVA_HOME "$ENV{JAVA_HOME}")
        endif()
        find_path(NATIVE_OBFUSCATOR_JNI_INCLUDE_DIR
            NAMES jni.h
            HINTS
                "${JAVA_HOME}/include"
                "$ENV{JAVA_HOME}/include"
            PATHS
                /usr/lib/jvm/default-java/include
                /usr/lib/jvm/java-8-openjdk-amd64/include
                /usr/local/opt/openjdk/include
                /opt/homebrew/opt/openjdk/include
            NO_CMAKE_FIND_ROOT_PATH)
    endif()

    if(NOT NATIVE_OBFUSCATOR_JNI_INCLUDE_DIR)
        message(FATAL_ERROR
            "Zig cross-compilation requires jni.h. Set JAVA_HOME or pass "
            "-DNATIVE_OBFUSCATOR_JNI_INCLUDE_DIR=<jdk>/include.")
    endif()

    set(_native_zig_jni_dir "${CMAKE_CURRENT_SOURCE_DIR}/zig-jni/${NATIVE_OS}")
    if(NOT EXISTS "${_native_zig_jni_dir}/jni_md.h")
        message(FATAL_ERROR
            "Unsupported operating system for Zig cross-compilation: ${NATIVE_OS}. "
            "Supported targets are Linux, Windows and macOS.")
    endif()

    include_directories("${_native_zig_jni_dir}" "${NATIVE_OBFUSCATOR_JNI_INCLUDE_DIR}")

    # Zig reports itself as Clang, so the compiler-specific flags above do not
    # apply. Match the optimization and stripping of the regular GNU build.
    # Newer Zig versions (e.g. 0.16) enable nullability warnings for their
    # bundled libc++ headers, which floods the build log with harmless noise.
    if(NOT CMAKE_BUILD_TYPE OR CMAKE_BUILD_TYPE STREQUAL "Release")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -DNDEBUG -Wno-nullability-completeness")
        set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -s")
    endif()
else()
    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})
endif()

set(CLASS_FILES $classfiles)
set(MAIN_FILES $mainfiles)
add_definitions($definitions)

add_library($projectname SHARED ${CLASS_FILES} ${MAIN_FILES})

set_target_properties($projectname PROPERTIES
    PREFIX ""
    OUTPUT_NAME "${NATIVE_ARCH}-${NATIVE_OS}"
)

message(STATUS "Target platform : ${NATIVE_OS}")
message(STATUS "Target arch     : ${NATIVE_ARCH}")
if(DEFINED ZIG_TARGET AND NOT "${ZIG_TARGET}" STREQUAL "")
    message(STATUS "Zig target      : ${ZIG_TARGET}")
endif()
message(STATUS "Output filename : ${NATIVE_ARCH}-${NATIVE_OS}${CMAKE_SHARED_LIBRARY_SUFFIX}")
