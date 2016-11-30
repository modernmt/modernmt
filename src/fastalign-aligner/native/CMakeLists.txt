cmake_minimum_required(VERSION 3.2)
project(fastalign)

if (${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(MAC_OSX true)
    message (STATUS "Compiling on a Mac; special operations for OpenMP (MAC_OSX:${MAC_OSX})")
else ()
    set(MAC_OSX false)
    message (STATUS "Compiling on a Linux; no special operations for OpenMP")
endif ()

# where to find cmake_modules modules
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake_modules")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -O3")

########
## find third-party packages
########

## MMT common-interfaces
find_package(MMTCommons REQUIRED)

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options)

## JNI
find_package(JNI REQUIRED)

## TcMalloc
find_package(Tcmalloc REQUIRED)


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

set(SOURCE_FILES
        fastalign/Model.h fastalign/Model.cpp
        fastalign/ModelBuilder.h fastalign/ModelBuilder.cpp
        fastalign/Corpus.h fastalign/Corpus.cpp
        fastalign/DiagonalAlignment.h
        fastalign/FastAligner.cpp fastalign/FastAligner.h

        symal/SymAlignment.cpp symal/SymAlignment.h

        util/chrono.h

        javah/eu_modernmt_aligner_fastalign_FastAlign.h java/eu_modernmt_aligner_fastalign_FastAlign.cpp)

########
## include and lib directories
########
include_directories(${CMAKE_SOURCE_DIR})

## MMT common-interfaces
include_directories(${MMTCommons_INCLUDE_DIR})

## Boost
include_directories(${Boost_INCLUDE_DIRS})

## JNI
include_directories(${JNI_INCLUDE_DIRS})

## TcMalloc
include_directories(${Tcmalloc_INCLUDE_DIR})

# MyOpenMP
if (MAC_OSX)
    if (MYOPENMP_FOUND)
        include_directories(${MyOpenMP_INCLUDE_DIR})
        link_directories(${MyOpenMP_LIBRARY_DIR})
        message(STATUS "MyOpenMP_INCLUDE_DIR:${MyOpenMP_INCLUDE_DIR}")
        message(STATUS "MyOpenMP_LIBRARY_DIR:${MyOpenMP_LIBRARY_DIR}")
    endif (MYOPENMP_FOUND)
endif (MAC_OSX)

########
## Create library
########
add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})


########
## External Libraries
########

## TcMalloc
target_link_libraries(${PROJECT_NAME} ${Tcmalloc_LIBRARIES})


## Boost
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})

#########
## Install targets
#########
install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

file(GLOB executables executables/*.cpp)
foreach(exefile ${executables})
    get_filename_component(exe ${exefile} NAME_WE)
    add_executable(${exe} ${exefile})
    target_link_libraries(${exe} ${PROJECT_NAME})
    install(TARGETS ${exe} RUNTIME DESTINATION bin)
endforeach()


install(FILES fastalign/FastAligner.h fastalign/Model.h DESTINATION ${CMAKE_INSTALL_PREFIX}/include/fastalign)
