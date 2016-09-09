cmake_minimum_required(VERSION 3.6)
project(mmt-common-interfaces)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
        include/mmt/sentence.h
        include/mmt/jniutil.h
        include/mmt/IncrementalModel.h

        include/mmt/aligner/Aligner.h
        include/mmt/aligner/AlignerModel.h
        include/mmt/vocabulary/Vocabulary.h)

add_library(${PROJECT_NAME} INTERFACE)

include_directories(include)

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})