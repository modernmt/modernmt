cmake_minimum_required(VERSION 3.2)
project(mmt_logging)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
        include/mmt/sentence.h
        include/mmt/jniutil.h

        include/mmt/vocabulary/Vocabulary.h

        include/mmt/logging/Logger.h logging/Logger.cpp
        javah/eu_modernmt_logging_NativeLogger.h java/eu_modernmt_logging_NativeLogger.cpp)

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

include_directories(include)
include_directories(${PROJECT_SOURCE_DIR})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)