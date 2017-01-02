cmake_minimum_required(VERSION 3.2)
project(jnimoses)

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
        decoder/JNITranslationRequest.h decoder/JNITranslationRequest.cpp
        decoder/JNITranslator.h decoder/JNITranslator.cpp
        decoder/MosesDecoder.h decoder/MosesDecoder.cpp

        javah/eu_modernmt_decoder_moses_MosesDecoder.h java/eu_modernmt_decoder_moses_MosesDecoder.cpp
        java/JMosesFeature.h java/JMosesFeature.cpp
        java/JTranslation.h java/JTranslation.cpp)

include_directories(${CMAKE_SOURCE_DIR})

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})
install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)

file(GLOB executables executables/*.cpp)
foreach(exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
endforeach()

# External Libraries

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options iostreams)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})

## ModernMT
find_package(ModernMT REQUIRED COMPONENTS common)
include_directories(${ModernMT_INCLUDE_DIRS})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## Moses
find_package(Moses REQUIRED)
add_definitions(${Moses_DEFINITIONS})
include_directories(${Moses_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Moses_LIBRARIES})