cmake_minimum_required(VERSION 2.8.8)
project(mosesdecoder)

# for FindIRSTLM.cmake
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${PROJECT_SOURCE_DIR}/cmake/")

set(CMAKE_MACOSX_RPATH 1)

if (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
    set(CMAKE_INSTALL_PREFIX "${CMAKE_SOURCE_DIR}" CACHE PATH "MMT install prefix" FORCE)
    MESSAGE(STATUS "Install prefix defaulted to ${CMAKE_INSTALL_PREFIX} -- for MMT, you should use cmake -DCMAKE_INSTALL_PREFIX=/home/ubuntu/MMT")
endif (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)


# Set output paths to <MMT>/bin/ and <MMT>/lib/
# ---------------------------------------------
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_INSTALL_PREFIX}/bin)
set(LIBRARY_OUTPUT_PATH ${CMAKE_INSTALL_PREFIX}/lib)


set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

# to build shared libmoses.so with KenLM inside (probably needed for KenLM only)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

# for KenLMFunctions.cmake
include(cmake/KenLMFunctions.cmake)


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

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

find_package(ZLIB REQUIRED)
find_package(GooglePerftools)  # for libtcmalloc_minimal

if (USE_IRSTLM)
    find_package(IRSTLM REQUIRED)
    add_definitions(-DLM_IRST=true)
endif (USE_IRSTLM)

if (USE_MMTILM)
    find_package(ROCKSDB REQUIRED)
    find_package(MMTCommons REQUIRED)
    find_package(MMTILM REQUIRED)
    add_definitions(-DLM_MMTILM=true)
endif (USE_MMTILM)

if (USE_SAPT)
    find_package(ROCKSDB REQUIRED)
    find_package(SAPT REQUIRED)
    add_definitions(-DSAPT=true)
endif (USE_SAPT)

add_definitions(-DHAVE_ZLIB=1)
add_definitions(-DMAX_NUM_FACTORS=4 -DKENLM_MAX_ORDER=6 -DWITH_THREADS -DBOOST_TEST_DYN_LINK)
# note: MOSES_VERSION_ID is defined in moses/CMakeLists.txt

# "use TRACE_ENABLE to turn off output of any debugging info" (moses/Util.h)
add_definitions(-DTRACE_ENABLE=1)

# . is for #include<moses/...> style includes
include_directories(${Boost_INCLUDE_DIRS} .)
include_directories(${MMT_INCLUDE_DIR})

if (USE_IRSTLM)
    include_directories(${IRSTLM_INCLUDE_DIRS})
endif (USE_IRSTLM)

## MMT commons
find_package(MMTCommons REQUIRED)
include_directories(${MMTCommons_INCLUDE_DIR})

# for the main interface to moses, include/decoder/MosesDecoder.h
include_directories(include)

if (USE_MMTILM)
    include_directories(${ROCKSDB_INCLUDE_DIRS})
    include_directories(${MMTILM_INCLUDE_DIRS})
endif (USE_MMTILM)

if (USE_SAPT)
    include_directories(${ROCKSDB_INCLUDE_DIRS})
    include_directories(${SAPT_INCLUDE_DIRS})
endif (USE_SAPT)

# KenLM
add_subdirectory(util)
add_subdirectory(lm)


set(MOSES_SRC
        phrase-extract/PhraseOrientation.cpp
        search/edge_generator.cc
        search/nbest.cc
        search/rule.cc
        search/vertex.cc

        # MMT specific: disable syntax decoding, unused phrase tables, unused features
        contrib/other-builds/mmt/FF/Factory.cpp
        contrib/other-builds/mmt/IOWrapper.cpp
        contrib/other-builds/mmt/TranslationTask.cpp
        )

# defines pseudo-libraries (each a group of source files):
# moses_moses
add_subdirectory(moses)


add_library(moses SHARED ${MOSES_SRC} $<TARGET_OBJECTS:moses_moses> $<TARGET_OBJECTS:kenlm> $<TARGET_OBJECTS:kenlm_util>)
target_link_libraries(moses ${Boost_LIBRARIES} ${ZLIB_LIBRARIES} pthread)
#target_link_libraries(moses ${MMTCommons_LIBRARIES}) # TODO david temp

add_library(mmt-decoder SHARED $<TARGET_OBJECTS:moses_decoder> $<TARGET_OBJECTS:moses_java>)
#install(TARGETS mmt-decoder LIBRARY DESTINATION lib) # installs to same location we built in -> fail?! -> 'INSTALL cannot find "/home/ubuntu/MMTy/build/lib/libmmt-decoder.so"'
target_link_libraries(mmt-decoder moses)

if (USE_IRSTLM)
    target_link_libraries(moses ${IRSTLM_LIBRARIES})
endif (USE_IRSTLM)

if (USE_MMTILM)
    target_link_libraries(moses ${ROCKSDB_LIBRARIES} )
    target_link_libraries(moses ${MMTILM_LIBRARIES} )
endif (USE_MMTILM)

if (USE_SAPT)
    target_link_libraries(moses ${SAPT_LIBRARIES} )
endif (USE_SAPT)

add_executable(moses-main moses-cmd/Main.cpp)
target_link_libraries(moses-main ${Boost_LIBRARIES} ${GooglePerftools_LIBRARIES} moses)
link_directories(${mosesdecoder_BINARY_DIR}/moses)  # link against libmoses.so

# Mert
add_subdirectory(mert)

link_directories(${mosesdecoder_BINARY_DIR}/moses)  # link against libmoses.so
