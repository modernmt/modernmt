cmake_minimum_required(VERSION 3.2)
project(mmtcore)

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
        vocabulary/Vocabulary.h
        vocabulary/IdGenerator.cpp vocabulary/IdGenerator.h
        vocabulary/InMemoryVocabulary.cpp vocabulary/InMemoryVocabulary.h
        vocabulary/PersistentVocabulary.cpp vocabulary/PersistentVocabulary.h

        jni/handle.h

        jheaders/eu_modernmt_vocabulary_Vocabulary.h jni/eu_modernmt_vocabulary_Vocabulary.cpp
        jheaders/eu_modernmt_vocabulary_VocabularyBuilder.h jni/eu_modernmt_vocabulary_VocabularyBuilder.cpp)

include_directories(${CMAKE_SOURCE_DIR})
include_directories(jheaders) # Automatically generated JNI headers

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