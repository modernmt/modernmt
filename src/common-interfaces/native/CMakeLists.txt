cmake_minimum_required(VERSION 3.6)
project(mmt-common-interfaces)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES include/mmt/sentence.h include/mmt/Aligner.h include/mmt/jniutil.h)
add_library(${PROJECT_NAME} INTERFACE)

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})