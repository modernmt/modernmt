cmake_minimum_required(VERSION 2.8.8)
project(MosesDecoder)

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
        decoder/JNITranslationRequest.h decoder/JNITranslationRequest.cpp
        decoder/JNITranslator.h decoder/JNITranslator.cpp
        decoder/MosesDecoder.h decoder/MosesDecoder.cpp

        jheaders/eu_modernmt_decoder_moses_MosesDecoder.h

        jni/handle.h
        jni/jniutil.h
        jni/eu_modernmt_decoder_moses_MosesDecoder.cpp
        jni/JMosesFeature.h jni/JMosesFeature.cpp
        jni/JTranslation.h jni/JTranslation.cpp

        vocabulary/Vocabulary.cpp vocabulary/Vocabulary.h
        vocabulary/IdGenerator.cpp vocabulary/IdGenerator.h
        vocabulary/NewWordOperator.cpp vocabulary/NewWordOperator.h vocabulary/VocabularyBuilder.cpp vocabulary/VocabularyBuilder.h util/TokenSource.cpp util/TokenSource.h)

include_directories(${CMAKE_SOURCE_DIR})

add_library(jnimoses SHARED ${SOURCE_FILES})

add_executable(decoder_test test/decoder_test.cpp)
target_link_libraries(decoder_test jnimoses)
add_executable(buildvb test/buildvb.cpp)
target_link_libraries(buildvb jnimoses)

# External Libraries

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options iostreams)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(jnimoses ${Boost_LIBRARIES})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## Moses
find_package(Moses REQUIRED)
add_definitions(${Moses_DEFINITIONS})
include_directories(${Moses_INCLUDE_DIRS})
target_link_libraries(jnimoses ${Moses_LIBRARIES})

## RocksDB
find_package(Rocksdb REQUIRED)
include_directories(${Rocksdb_INCLUDE_DIR})
target_link_libraries(jnimoses ${Rocksdb_LIBS})

## OpenMP
find_package(OpenMP)
if (OPENMP_FOUND)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    include_directories(${OPENMP_INCLUDE_DIRS})
    target_link_libraries(jnimoses ${OPENMP_LIBRARIES})
endif (OPENMP_FOUND)