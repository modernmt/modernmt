cmake_minimum_required(VERSION 3.2)
project(mmt_logging)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -fPIC")

set(LOGGING_SOURCE
        Logger.h Logger.cpp
        eu_modernmt_decoder_phrasebased_NativeLogger.cpp)

include_directories(.)

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

add_library(${PROJECT_NAME} OBJECT ${LOGGING_SOURCE})