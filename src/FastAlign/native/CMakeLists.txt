cmake_minimum_required(VERSION 3.2)
project(fastalign)

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -O3")

set(SOURCE_FILES
        fastalign/Model.h fastalign/Model.cpp
        fastalign/ModelBuilder.h fastalign/ModelBuilder.cpp
        fastalign/Corpus.h fastalign/Corpus.cpp
        fastalign/DiagonalAlignment.h

        jni/jniutils.h
        jheaders/eu_modernmt_aligner_fastalign_FastAlign.h jni/eu_modernmt_aligner_fastalign_FastAlign.cpp)

include_directories(${CMAKE_SOURCE_DIR})
include_directories(jheaders) # Automatically generated JNI headers

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})
install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)

file(GLOB executables executables/*.cpp)
foreach(exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
    install(TARGETS ${exe} RUNTIME DESTINATION bin/fastalign)
endforeach()

# External Libraries

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## TcMalloc
find_package(Tcmalloc REQUIRED)
target_link_libraries(${PROJECT_NAME} ${Tcmalloc_LIBRARIES})

## OpenMP
find_package(OpenMP REQUIRED)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")