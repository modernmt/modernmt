cmake_minimum_required(VERSION 3.2)
project(fastalign)

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3")

set(SOURCE_FILES
        fastalign/Model.h fastalign/Model.cpp
        fastalign/ModelBuilder.h fastalign/ModelBuilder.cpp
        fastalign/Corpus.h fastalign/Corpus.cpp
        fastalign/TTable.cpp fastalign/TTable.h
        fastalign/DiagonalAlignment.h

        jni/handle.h )

include_directories(${CMAKE_SOURCE_DIR})
include_directories(jheaders) # Automatically generated JNI headers

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

file(GLOB executables executables/*.cpp)
foreach(exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
endforeach()

# External Libraries

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## TcMalloc
find_package(Tcmalloc REQUIRED)
target_link_libraries(${PROJECT_NAME} ${Tcmalloc_LIBRARIES})

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS thread system)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})

## OpenMP
find_package(OpenMP REQUIRED)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")