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

# where to find cmake modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

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

## RocksDB
find_package(Rocksdb REQUIRED)

## BZip2, ZLIB and lzma
find_package(BZip2)

## BZip2, ZLIB and lzma
find_package(BZip2)
find_package(ZLIB)
find_package(lzma)

# OpenMP
find_package(OpenMP QUIET)
if (OPENMP_FOUND)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    message(STATUS "OpenMP found")
endif (OPENMP_FOUND)

# MyOpenMP
if (MAC_OSX)
    if (NOT OPENMP_FOUND)
        message(STATUS "OpenMP not found; looking for the user-defined package MyOpenMP")
        find_package(MyOpenMP)
        if (MYOPENMP_FOUND)
            message(STATUS "CMAKE_EXE_LINKER_FLAGS:${CMAKE_EXE_LINKER_FLAGS}")
            set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${MyOpenMP_CXX_FLAGS}")
            set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${MyOpenMP_EXE_LINKER_FLAGS}")
            message(STATUS "MyOpenMP found")
            message(STATUS "CMAKE_EXE_LINKER_FLAGS:${CMAKE_EXE_LINKER_FLAGS}")
        else (MYOPENMP_FOUND)
            message(FATAL "MyOpenMP not found")
        endif (MYOPENMP_FOUND)
    endif (NOT OPENMP_FOUND)
endif (MAC_OSX)

## MMT common-interfaces
find_package(MMTCommons REQUIRED)

########
## include and lib directories
########

include_directories(${Boost_INCLUDE_DIRS})

## RocksDB
include_directories(${Rocksdb_INCLUDE_DIR})

## BZip2
if (BZIP2_FOUND)
    include_directories(${BZIP_INCLUDE_DIRS})
endif (BZIP2_FOUND)

## ZLIB
if (ZLIB_FOUND)
    include_directories(${ZLIB_INCLUDE_DIRS})
endif (ZLIB_FOUND)

##lzma
if (lzma_FOUND)
    include_directories(${lzma_INCLUDE_DIRS})
endif (lzma_FOUND)

## MMT common-interfaces
include_directories(${MMTCommons_INCLUDE_DIR})

# MyOpenMP
if (MAC_OSX)
    if (MYOPENMP_FOUND)
        include_directories(${MyOpenMP_INCLUDE_DIR})
        link_directories(${MyOpenMP_LIBRARY_DIR})
        message(STATUS "MyOpenMP_INCLUDE_DIR:${MyOpenMP_INCLUDE_DIR}")
        message(STATUS "MyOpenMP_LIBRARY_DIR:${MyOpenMP_LIBRARY_DIR}")
    endif (MYOPENMP_FOUND)
endif (MAC_OSX)

# Preparing target, recursively
add_definitions(-DKENLM_MAX_ORDER=6)

add_subdirectory(corpus)
add_subdirectory(lm)
add_subdirectory(db)
add_subdirectory(kenlm)

add_library(${PROJECT_NAME} ${TARGET_OBJECTS_kenlm} ${TARGET_OBJECTS_lm} ${TARGET_OBJECTS_db} ${TARGET_OBJECTS_corpus} ${SOURCE_FILES})
link_directories(build)

# Set third-party libraries
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
#[[
if (MYOPENMP_FOUND)
    link_directories(${MyOpenMP_LIBRARY_DIR})
endif (MYOPENMP_FOUND)
]]


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

