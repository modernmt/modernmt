cmake_minimum_required(VERSION 3.2)
project(mmt_pbdecoder)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wno-dev -O3 -fPIC")
set(CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH};${CMAKE_SOURCE_DIR}/cmake_modules")

include_directories(include)

add_subdirectory(interpolated-lm)
add_subdirectory(suffixarray-phrasetable)
add_subdirectory(decoder)