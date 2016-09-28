cmake_minimum_required(VERSION 3.2)
project(sapt)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fPIC -O3")

set(SOURCE_FILES
        sapt/Options.h
        sapt/PhraseTable.cpp sapt/PhraseTable.h
        sapt/CorpusStorage.cpp sapt/CorpusStorage.h
        sapt/CorpusIndex.cpp sapt/CorpusIndex.h

        util/ioutils.h sapt/UpdateBatch.cpp sapt/UpdateBatch.h sapt/UpdateManager.cpp sapt/UpdateManager.h sapt/position.h)

include_directories(${CMAKE_SOURCE_DIR})

add_library(${PROJECT_NAME} ${SOURCE_FILES})

file(GLOB executables executables/*.cpp)
foreach (exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
endforeach ()

## MMT common-interfaces
find_package(MMTCommons REQUIRED)
include_directories(${MMTCommons_INCLUDE_DIR})

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})

## RocksDB
find_package(Rocksdb REQUIRED)
include_directories(${Rocksdb_INCLUDE_DIR})
target_link_libraries(${PROJECT_NAME} ${Rocksdb_LIBS})