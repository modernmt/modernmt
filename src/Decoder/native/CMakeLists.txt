cmake_minimum_required(VERSION 3.2)
project(jnimoses)

file(GLOB WRAPPER_SRC src/wrapper/*.h src/wrapper/*.cpp)
file(GLOB JNI_SRC src/jni/*.h src/jni/*.cpp)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
add_library(${PROJECT_NAME} SHARED ${WRAPPER_SRC} ${JNI_SRC})

include_directories(src)
include_directories(libs/include)
include_directories(jheaders)

# External Libraries

## BZip2
find_package(BZip2)
if (BZIP2_FOUND)
    include_directories(${BZIP_INCLUDE_DIRS})
    target_link_libraries(${PROJECT_NAME} ${BZIP2_LIBRARIES})
else (BZIP2_FOUND)
    message(FATAL_ERROR "Could not locate BZip2.")
endif (BZIP2_FOUND)

## XMLRPC
find_package(XMLRPC REQUIRED c++2 abyss-server)
if (XMLRPC_FOUND)
    include_directories(${XMLRPC_INCLUDE_DIRS})
    target_link_libraries(${PROJECT_NAME} ${XMLRPC_LIBRARIES})
else (XMLRPC_FOUND)
    message(FATAL_ERROR "Could not locate xmlrpc-c.")
endif (XMLRPC_FOUND)

## Boost
set(Boost_USE_STATIC_LIBS ON)
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
find_library(MOSES moses ${PROJECT_SOURCE_DIR}/libs)
if (MOSES)
    add_definitions(-DMAX_NUM_FACTORS=4)
    add_definitions(-DWITH_THREADS=1)
    add_definitions(-DHAVE_XMLRPC_C=1)
    target_link_libraries(${PROJECT_NAME} ${MOSES})
else (MOSES)
    message(FATAL_ERROR "Could not locate Moses.")
endif (MOSES)

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