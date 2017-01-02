cmake_minimum_required(VERSION 3.2)
project(mmtc) # mmt-mmt

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
        mmt/sentence.h
        mmt/jniutil.h
        mmt/IncrementalModel.h

        mmt/aligner/Aligner.h
        mmt/aligner/AlignerModel.h

        mmt/vocabulary/Vocabulary.h

        mmt/logging/Logger.h
        mmt/logging/Logger.cpp
        javah/eu_modernmt_logging_NativeLogger.h java/eu_modernmt_logging_NativeLogger.cpp)

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

include_directories(include)
include_directories(${PROJECT_SOURCE_DIR})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# Install rules

install(FILES mmt/aligner/Aligner.h mmt/aligner/AlignerModel.h DESTINATION include/mmt/aligner)
install(FILES mmt/logging/Logger.h DESTINATION include/mmt/logging)
install(FILES mmt/vocabulary/Vocabulary.h DESTINATION include/mmt/vocabulary)
install(FILES mmt/IncrementalModel.h mmt/jniutil.h mmt/sentence.h DESTINATION include/mmt)

install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)