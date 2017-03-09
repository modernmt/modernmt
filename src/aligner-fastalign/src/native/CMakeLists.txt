cmake_minimum_required(VERSION 3.2)
project(mmt_fastalign)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -O3")

set(SOURCE_FILES
        fastalign/Model.h fastalign/Model.cpp
        fastalign/ModelBuilder.h fastalign/ModelBuilder.cpp
        fastalign/Corpus.h fastalign/Corpus.cpp
        fastalign/DiagonalAlignment.h
        fastalign/FastAligner.cpp fastalign/FastAligner.h

        symal/SymAlignment.cpp symal/SymAlignment.h

        javah/eu_modernmt_aligner_fastalign_FastAlign.h java/eu_modernmt_aligner_fastalign_FastAlign.cpp)

include_directories(${CMAKE_SOURCE_DIR})

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

# External libraries

## ModernMT
find_package(ModernMT REQUIRED COMPONENTS commons)
include_directories(${ModernMT_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${ModernMT_LIBS})

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

## OpenMP
find_package(OpenMP QUIET)
if (OPENMP_FOUND)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}")
    include_directories(${OpenMP_INCLUDE_DIR})
    link_directories(${OpenMP_LIBRARY_DIR})

    message(STATUS "Compiling with OpenMP")
endif (OPENMP_FOUND)

# Install rules

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

file(GLOB executables executables/*.cpp)
foreach (exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
    install(TARGETS ${exe} RUNTIME DESTINATION bin)
endforeach ()

install(FILES fastalign/FastAligner.h fastalign/Model.h DESTINATION ${CMAKE_INSTALL_PREFIX}/include/fastalign)
