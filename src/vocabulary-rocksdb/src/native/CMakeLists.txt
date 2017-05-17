cmake_minimum_required(VERSION 3.2)
project(mmt_vocabulary)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -O3")

set(SOURCE_FILES
        Vocabulary.cpp Vocabulary.h

        javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary.h java/eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary.cpp
        javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder.h java/eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder.cpp)

include_directories(${CMAKE_SOURCE_DIR})

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})
install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)

# External Libraries

## ModernMT
find_package(ModernMT REQUIRED COMPONENTS commons)
include_directories(${ModernMT_INCLUDE_DIRS})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem system program_options thread)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})