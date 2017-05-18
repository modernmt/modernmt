cmake_minimum_required(VERSION 3.2)
project(mmt_ilm)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3 -fPIC")

include_directories(${CMAKE_SOURCE_DIR}/interpolated-lm)
include_directories(${CMAKE_SOURCE_DIR}/interpolated-lm/kenlm)

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

#########
## Preparing target, recursively
#########
add_definitions(-DKENLM_MAX_ORDER=6)

add_subdirectory(util)
add_subdirectory(corpus)
add_subdirectory(lm)
add_subdirectory(db)
add_subdirectory(kenlm)

add_library(${PROJECT_NAME} SHARED
        ${TARGET_OBJECTS_util} ${TARGET_OBJECTS_kenlm}
        ${TARGET_OBJECTS_lm} ${TARGET_OBJECTS_db} ${TARGET_OBJECTS_corpus}
        $<TARGET_OBJECTS:mmt_logging> $<TARGET_OBJECTS:mmt_vocabulary>
        ${SOURCE_FILES})

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

#########
## Install targets
#########

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

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

