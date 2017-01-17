cmake_minimum_required(VERSION 3.2)
project(mmt-decoder)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wno-dev -fPIC")

# moses defines
add_definitions(-DMAX_NUM_FACTORS=1 -DWITH_THREADS -DBOOST_TEST_DYN_LINK -DTRACE_ENABLE=1)
# note: MOSES_VERSION_ID is defined in moses/CMakeLists.txt
# "use TRACE_ENABLE to turn on output of any debugging info" (moses/Util.h)


# Includes

# for javah/ which is filled with auto-generated header eu_modernmt_decoder_moses_MosesDecoder.h
include_directories(.)
include_directories(decoder)



# Binaries

add_library(${PROJECT_NAME} SHARED $<TARGET_OBJECTS:moses_moses> $<TARGET_OBJECTS:kenlm_util> $<TARGET_OBJECTS:moses_decoder> $<TARGET_OBJECTS:moses_java>)
install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib)

add_executable(moses-main decoder/moses-cmd/Main.cpp)
target_link_libraries(moses-main ${Boost_LIBRARIES} ${PROJECT_NAME})


# External Libraries

## ModernMT
find_package(ModernMT REQUIRED COMPONENTS common ilm sapt)
message("ModernMT_INCLUDE_DIRS = ${ModernMT_INCLUDE_DIRS}")
include_directories(${ModernMT_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${ModernMT_LIBS})

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost REQUIRED COMPONENTS
        system
        thread
        filesystem
        unit_test_framework
        program_options
        iostreams
        )
include_directories(${Boost_INCLUDE_DIRS} .)
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES} pthread)

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## ZLIB
# only for util/read_compressed.cc
find_package(ZLIB REQUIRED)
add_definitions(-DHAVE_ZLIB=1)
target_link_libraries(${PROJECT_NAME} ${ZLIB_LIBRARIES})

## RocksDB
find_package(Rocksdb REQUIRED)
include_directories(${Rocksdb_INCLUDE_DIR})
target_link_libraries(${PROJECT_NAME} ${Rocksdb_LIBS})

## Google perftools: tcmalloc
find_package(GooglePerftools)  # for libtcmalloc_minimal
target_link_libraries(${PROJECT_NAME} ${GooglePerftools_LIBRARIES})


# Sources

add_subdirectory(decoder)  # note: include_directories() only affects later subdirectories.
