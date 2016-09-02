cmake_minimum_required(VERSION 3.2)
project(rocksvb)

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall")

set(SOURCE_FILES
        vocabulary/IdGenerator.cpp vocabulary/IdGenerator.h
        vocabulary/PersistentVocabulary.cpp vocabulary/PersistentVocabulary.h

        javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary.h java/eu_modernmt_vocabulary_rocksdb_RocksDBVocabulary.cpp
        javah/eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder.h java/eu_modernmt_vocabulary_rocksdb_RocksDBVocabularyBuilder.cpp)

include_directories(${CMAKE_SOURCE_DIR})
include_directories(${MMT_INCLUDE_DIR})

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})
install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)

file(GLOB executables executables/*.cpp)
foreach (exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
endforeach ()

# External Libraries

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## TcMalloc
find_package(Tcmalloc REQUIRED)
target_link_libraries(${PROJECT_NAME} ${Tcmalloc_LIBRARIES})

## RocksDB
find_package(Rocksdb REQUIRED)
include_directories(${Rocksdb_INCLUDE_DIR})
target_link_libraries(${PROJECT_NAME} ${Rocksdb_LIBS})