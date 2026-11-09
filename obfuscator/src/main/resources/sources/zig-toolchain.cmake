# CMake toolchain file for cross-compiling the generated native library with Zig.
#
# Zig ships its own libc headers, compiler-rt and linkers for every supported
# target, so a single Zig installation can cross-compile the generated C++ code
# to Linux, Windows and macOS from any host.
#
# Usage (Ninja or Makefiles generator required):
#
#   cmake -S . -B build/zig/x64-linux -G Ninja \
#         -DCMAKE_TOOLCHAIN_FILE=zig-toolchain.cmake \
#         -DZIG_TARGET=x86_64-linux-gnu
#   cmake --build build/zig/x64-linux
#
# Supported ZIG_TARGET values:
#   x86_64-linux-gnu    aarch64-linux-gnu
#   x86_64-windows-gnu  aarch64-windows-gnu
#   x86_64-macos        aarch64-macos
#
# The helper CMakePresets.json in this directory contains ready to use presets
# for every target above.

if(NOT DEFINED ZIG_TARGET OR "${ZIG_TARGET}" STREQUAL "")
    message(FATAL_ERROR
        "ZIG_TARGET is not set. Pass one of the supported target triples, e.g. "
        "-DZIG_TARGET=x86_64-linux-gnu. See zig-toolchain.cmake for the full list.")
endif()

set(ZIG_TARGET "${ZIG_TARGET}" CACHE STRING "Zig target triple")

# try_compile() sub-projects re-run this toolchain file in a fresh project, so
# make sure our variables are forwarded to them.
list(APPEND CMAKE_TRY_COMPILE_PLATFORM_VARIABLES ZIG_TARGET ZIG_EXECUTABLE)

# Visual Studio and Xcode generators ignore CMAKE_<LANG>_COMPILER and drive
# MSVC/Xcode directly, which cannot be combined with zig cc/zig c++.
if(CMAKE_GENERATOR MATCHES "Visual Studio|Xcode")
    message(FATAL_ERROR
        "The '${CMAKE_GENERATOR}' generator cannot be used for Zig cross-compilation. "
        "Use the Ninja generator ('-G Ninja') or a Makefiles generator.")
endif()

# Split the triple into arch-os-rest. Zig accepts optional ABI/environment
# components after the OS, e.g. x86_64-linux-gnu or x86_64-windows-gnu.
string(REPLACE "-" ";" _zig_target_parts "${ZIG_TARGET}")
list(LENGTH _zig_target_parts _zig_target_length)
if(_zig_target_length LESS 2)
    message(FATAL_ERROR
        "Invalid ZIG_TARGET '${ZIG_TARGET}'. Expected a triple like "
        "x86_64-linux-gnu, aarch64-linux-gnu, x86_64-windows-gnu, "
        "aarch64-windows-gnu, x86_64-macos or aarch64-macos.")
endif()

list(GET _zig_target_parts 0 _zig_arch)
list(GET _zig_target_parts 1 _zig_os)

if(_zig_arch STREQUAL "x86_64" OR _zig_arch STREQUAL "amd64")
    set(CMAKE_SYSTEM_PROCESSOR "x86_64")
elseif(_zig_arch STREQUAL "aarch64" OR _zig_arch STREQUAL "arm64")
    set(CMAKE_SYSTEM_PROCESSOR "aarch64")
else()
    message(FATAL_ERROR
        "Unsupported Zig target architecture '${_zig_arch}'. Supported "
        "architectures are x86_64 and aarch64.")
endif()

if(_zig_os STREQUAL "linux")
    set(CMAKE_SYSTEM_NAME "Linux")
elseif(_zig_os STREQUAL "windows")
    set(CMAKE_SYSTEM_NAME "Windows")
elseif(_zig_os STREQUAL "macos" OR _zig_os STREQUAL "darwin")
    set(CMAKE_SYSTEM_NAME "Darwin")
else()
    message(FATAL_ERROR
        "Unsupported Zig target OS '${_zig_os}'. Supported operating systems "
        "are linux, windows and macos.")
endif()

# Locate the Zig executable. A user supplied -DZIG_EXECUTABLE=... wins.
if(NOT ZIG_EXECUTABLE)
    find_program(ZIG_EXECUTABLE NAMES zig zig.exe
        DOC "Path to the Zig executable used for cross-compilation")
endif()

if(NOT ZIG_EXECUTABLE)
    message(FATAL_ERROR
        "Zig was not found. Install Zig and add it to PATH, or pass "
        "-DZIG_EXECUTABLE=/path/to/zig.")
endif()

set(ZIG_EXECUTABLE "${ZIG_EXECUTABLE}" CACHE FILEPATH "Zig executable")

# Route CMake through `zig cc` / `zig c++`. CMAKE_<LANG>_COMPILER_TARGET makes
# CMake pass --target=<triple> to Zig, which selects the target libc, headers,
# compiler-rt and linker.
set(CMAKE_C_COMPILER "${ZIG_EXECUTABLE}")
set(CMAKE_C_COMPILER_ARG1 "cc")
set(CMAKE_C_COMPILER_TARGET "${ZIG_TARGET}")

set(CMAKE_CXX_COMPILER "${ZIG_EXECUTABLE}")
set(CMAKE_CXX_COMPILER_ARG1 "c++")
set(CMAKE_CXX_COMPILER_TARGET "${ZIG_TARGET}")

# Make sure CMake does not try to run target executables during configuration.
set(CMAKE_CROSSCOMPILING ON)

message(STATUS "Zig cross-compilation enabled")
message(STATUS "  Zig executable : ${ZIG_EXECUTABLE}")
message(STATUS "  Zig target     : ${ZIG_TARGET}")
message(STATUS "  Target system  : ${CMAKE_SYSTEM_NAME}")
message(STATUS "  Target processor: ${CMAKE_SYSTEM_PROCESSOR}")
