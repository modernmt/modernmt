cmake_minimum_required(VERSION 3.2)
project(jnimoses)

file(GLOB WRAPPER_SRC src/wrapper/*.h src/wrapper/*.cpp)
file(GLOB JNI_SRC src/jni/*.h src/jni/*.cpp)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
add_library(${PROJECT_NAME} SHARED ${WRAPPER_SRC} ${JNI_SRC})

add_executable(jnitest src/main.cpp)
target_link_libraries(jnitest ${PROJECT_NAME})

include_directories(src)
include_directories(jheaders)

# External Libraries

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 COMPONENTS filesystem thread system program_options iostreams)

if (Boost_FOUND)
    include_directories(${Boost_INCLUDE_DIRS})
    target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})
else (Boost_FOUND)
    message(FATAL_ERROR "Could not locate Boost. Check if BOOST_ROOT is defined in your system.")
endif (Boost_FOUND)

## JNI
find_package(JNI)
if (JNI_FOUND)
    include_directories(${JNI_INCLUDE_DIRS})
else (JNI_FOUND)
    message(FATAL_ERROR "Could not locate JNI. Check if JAVA_HOME is defined in your system.")
endif (JNI_FOUND)

## Moses
find_path(Moses_INCLUDE_DIRS moses/FF/StatefulFeatureFunction.h PATHS ENV MOSESDECODER_HOME)
if (Moses_INCLUDE_DIRS STREQUAL "Moses_INCLUDE_DIRS-NOTFOUND")
    message(FATAL_ERROR "Could not locate Moses home path. Try setting environment variable MOSESDECODER_HOME to the root path of mosesdecoder distribution.")
endif()

find_path(Moses_LIBRARIES build/lib/${CMAKE_SHARED_LIBRARY_PREFIX}moses${CMAKE_SHARED_LIBRARY_SUFFIX} PATHS ENV MOSESDECODER_HOME)
if (Moses_LIBRARIES STREQUAL "Moses_LIBRARIES-NOTFOUND")
    message(FATAL_ERROR "Could not locate Moses shared library. Try setting environment variable MOSESDECODER_HOME to the root path of mosesdecoder distribution or recompiling Moses.")
else()
    set(Moses_LIBRARIES ${Moses_LIBRARIES}/build/lib)
endif()

find_library(MOSES moses ${Moses_LIBRARIES})
add_definitions(-DMAX_NUM_FACTORS=4)
add_definitions(-DWITH_THREADS=1)
add_definitions(-DHAVE_XMLRPC_C=0)
#add_definitions(-DTRACE_ENABLE)  # add this to enable verbose moses output in general (otherwise, not even --verbose level will help)
include_directories(${Moses_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${MOSES})

## IRSTLM
find_path(IRSTLM_LIBRARIES build/lib/${CMAKE_SHARED_LIBRARY_PREFIX}irstlm${CMAKE_SHARED_LIBRARY_SUFFIX} PATHS ENV IRSTLM_HOME)
if (IRSTLM_LIBRARIES STREQUAL "IRSTLM_LIBRARIES-NOTFOUND")
    message(FATAL_ERROR "Could not locate IRSTLM shared library. Try setting environment variable IRSTLM_HOME to the root path of IRSTLM distribution or recompiling IRSTLM.")
else()
    set(IRSTLM_LIBRARIES ${IRSTLM_LIBRARIES}/build/lib)
endif()

find_library(IRSTLM irstlm ${IRSTLM_LIBRARIES})
target_link_libraries(${PROJECT_NAME} ${IRSTLM})

## CMPH
find_library(CMPH cmph)
if (CMPH)
    target_link_libraries(${PROJECT_NAME} ${CMPH})
else (CMPH)
    message(FATAL_ERROR "Could not locate cmlph.")
endif (CMPH)

## ZLib
find_package(ZLIB)
if (ZLIB_FOUND)
    target_link_libraries(${PROJECT_NAME} ${ZLIB_LIBRARIES})
else (ZLIB_FOUND)
    message(FATAL_ERROR "Could not locate ZLIB.")
endif (ZLIB_FOUND)