cmake_minimum_required(VERSION 3.3)
project(MMTDecoderJNI)

include_directories(
        /Library/Java/JavaVirtualMachines/jdk1.8.0_40.jdk/Contents/Home/include
        /Library/Java/JavaVirtualMachines/jdk1.8.0_40.jdk/Contents/Home/include/darwin
)

include_directories(
        jheaders
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES src/main.cpp src/jni/handle.h src/JNIMosesDecoderInterface.cpp src/JNIMosesDecoder.cpp src/JNIMosesDecoder.h src/jni/tconv.h)
add_library(jnimoses SHARED ${SOURCE_FILES})
add_executable(MMTDecoderJNI ${SOURCE_FILES})