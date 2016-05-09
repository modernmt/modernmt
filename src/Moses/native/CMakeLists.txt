cmake_minimum_required(VERSION 2.8.8)
project(jnimoses)

file(GLOB WRAPPER_SRC src/wrapper/*.h src/wrapper/*.cpp)
file(GLOB JNI_SRC src/jni/*.h src/jni/*.cpp)

# Set install prefix to use <MMT>/bin/ and <MMT>/lib/
# ---------------------------------------------------
#
if(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
  set(CMAKE_INSTALL_PREFIX "${CMAKE_SOURCE_DIR}" CACHE PATH "MMT install prefix" FORCE)
endif(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)

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
# for FindMoses.cmake
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${PROJECT_SOURCE_DIR}/cmake/")
find_package(Moses REQUIRED)

# TODO: these should move into FindMoses.cmake, with set(Moses_DEFINITIONS ...) coming from the values defined in the original moses CMake configuration.
# for a complicated version of generating the config, see https://cmake.org/cmake/help/git-master/manual/cmake-packages.7.html#creating-packages
# but it may be entirely feasible to just dump these values into a MosesConfig.cmake in that build.

add_definitions(-DMAX_NUM_FACTORS=4)
add_definitions(-DWITH_THREADS=1)
#add_definitions(-DHAVE_XMLRPC_C=0)  # note: even defining this to 0 triggers #ifdef ...
#add_definitions(-DTRACE_ENABLE)  # add this to enable verbose moses output in general (otherwise, not even --verbose level will help)
include_directories(${Moses_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Moses_LIBRARIES})

install(TARGETS ${PROJECT_NAME} DESTINATION lib)
#install(TARGETS jnitest DESTINATION bin)  # we do not install that. no need for it in production.

