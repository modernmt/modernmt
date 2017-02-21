cmake_minimum_required(VERSION 3.2)
project(ilm)

if (${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(MAC_OSX true)
    message (STATUS "Compiling on a Mac; special operations for OpenMP (MAC_OSX:${MAC_OSX})")
else ()
    set(MAC_OSX false)
    message (STATUS "Compiling on a Linux; no special operations for OpenMP")
endif ()

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

option(BUILD_STATIC_LIBS "Build the static library (default is ON) " OFF)
option(BUILD_SHARED_LIBS "Build the shared library (default is OFF)" ON)

set(CMAKE_MACOSX_RPATH 1)

if (BUILD_STATIC_LIBS AND BUILD_SHARED_LIBS)
    MESSAGE(FATAL_ERROR " It is not allowed compiling both static and shared libraries and executables;
 - to enable/disable static compilation, set -DBUILD_STATIC_LIBS=[ON|OFF] (default is ON)
 - to enable/disable shared compilation, set -DBUILD_SHARED_LIBS=[ON|OFF] (default is OFF)")
endif ()

if (NOT BUILD_STATIC_LIBS AND NOT BUILD_SHARED_LIBS)
    MESSAGE(FATAL_ERROR " It is mandatory enabling either static or shared compilation of libraries and executables;
 - to enable/disable static compilation, set -DBUILD_STATIC_LIBS=[ON|OFF] (default is ON)
 - to enable/disable shared compilation, set -DBUILD_SHARED_LIBS=[ON|OFF] (default is OFF)")
endif ()

# Set output directory
IF (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
    SET(CMAKE_INSTALL_PREFIX
            "${CMAKE_SOURCE_DIR}/inst" CACHE PATH "IRSTLM install prefix" FORCE
            )
    MESSAGE(STATUS "You have not set the install dir, default to ${CMAKE_INSTALL_PREFIX}, if
    you want to set it, use cmake -DCMAKE_INSTALL_PREFIX to do so")
ENDIF (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -fPIC")

include_directories(${CMAKE_SOURCE_DIR})
include_directories(${CMAKE_SOURCE_DIR}/kenlm)

#########
## Finding thrid-party packages
#########

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options)
include_directories(${Boost_INCLUDE_DIRS})

## RocksDB
find_package(Rocksdb REQUIRED)
include_directories(${Rocksdb_INCLUDE_DIR})

## BZip2
find_package(BZip2 QUIET)
if (BZIP2_FOUND)
    include_directories(${BZIP_INCLUDE_DIRS})
endif (BZIP2_FOUND)

## ZLIB
find_package(ZLIB QUIET)
if (ZLIB_FOUND)
    include_directories(${ZLIB_INCLUDE_DIRS})
endif (ZLIB_FOUND)

## lzma
find_package(lzma QUIET)
if (lzma_FOUND)
    include_directories(${lzma_INCLUDE_DIRS})
endif (lzma_FOUND)

## OpenMP
find_package(OpenMP QUIET)
if (OPENMP_FOUND)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}")
    include_directories(${OpenMP_INCLUDE_DIR})

    message(STATUS "Compiling with OpenMP")
endif (OPENMP_FOUND)

## ModernMT
find_package(ModernMT REQUIRED COMPONENTS common)
include_directories(${ModernMT_INCLUDE_DIRS})

#########
## Preparing target, recursively
#########
add_definitions(-DKENLM_MAX_ORDER=6)

add_subdirectory(util)
add_subdirectory(corpus)
add_subdirectory(lm)
add_subdirectory(db)
add_subdirectory(kenlm)

link_directories(build)

add_library(${PROJECT_NAME} ${TARGET_OBJECTS_util} ${TARGET_OBJECTS_kenlm} ${TARGET_OBJECTS_lm} ${TARGET_OBJECTS_db} ${TARGET_OBJECTS_corpus} ${SOURCE_FILES})

#########
## Third-party libraries linking
#########

target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})
target_link_libraries(${PROJECT_NAME} ${Rocksdb_LIBS})
if (BZIP2_FOUND)
    target_link_libraries(${PROJECT_NAME} ${BZIP2_LIBRARIES})
endif (BZIP2_FOUND)
if (ZLIB_FOUND)
    target_link_libraries(${PROJECT_NAME} ${ZLIB_LIBRARIES})
endif (ZLIB_FOUND)
if (lzma_FOUND)
    target_link_libraries(${PROJECT_NAME} ${lzma_LIBRARIES})
endif (lzma_FOUND)
if (OPENMP_FOUND)
    link_directories(${OpenMP_LIBRARY_DIR})
endif (OPENMP_FOUND)
target_link_libraries(${PROJECT_NAME} ${ModernMT_LIBS})

#########
## Install targets
#########

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

install(FILES lm/LM.h lm/Options.h lm/InterpolatedLM.h lm/CachedLM.h DESTINATION ${CMAKE_INSTALL_PREFIX}/include/ilm)

file(GLOB executables executables/*.cpp)
foreach (exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
endforeach ()

foreach (exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    install(TARGETS ${exe}
            RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
            LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
            ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)
endforeach ()

message(STATUS "Executables will be installed in ${CMAKE_INSTALL_PREFIX}/bin")
message(STATUS "Libraries will be installed in ${CMAKE_INSTALL_PREFIX}/lib")
message(STATUS "Include files will be installed in ${CMAKE_INSTALL_PREFIX}/include")

