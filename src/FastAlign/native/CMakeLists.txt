cmake_minimum_required(VERSION 2.8.8)
project(jnifastalign)

file(GLOB SRC src/*.h src/*.cpp)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
add_library(${PROJECT_NAME} SHARED ${SRC} src/eu_modernmt_aligner_fastalign_FastAlign.cpp src/handle.h src/jniutil.h)

include_directories(src)
include_directories(jheaders)

# External Libraries

## JNI
find_package(JNI)
if (JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
else (JNI_FOUND)
    message(FATAL_ERROR "Could not locate JNI. Check if JAVA_HOME is defined in your system.")
endif (JNI_FOUND)

## FastAlign
# for FindFastAlign.cmake
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${PROJECT_SOURCE_DIR}/cmake/")
find_package(FastAlign REQUIRED)

include_directories(${FastAlign_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${FastAlign_LIBRARIES})