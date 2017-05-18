cmake_minimum_required(VERSION 3.2)
project(mmt_sapt)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fPIC -O3 -Wall")

set(SOURCE_FILES
        suffixarray/dbkv.h
        suffixarray/sample.h
        suffixarray/index_exception.h
        suffixarray/UpdateBatch.cpp suffixarray/UpdateBatch.h
        suffixarray/PostingList.cpp suffixarray/PostingList.h
        suffixarray/PrefixCursor.cpp suffixarray/PrefixCursor.h
        suffixarray/SuffixArray.cpp suffixarray/SuffixArray.h
        suffixarray/Collector.cpp suffixarray/Collector.h
        suffixarray/GarbageCollector.cpp suffixarray/GarbageCollector.h

        suffixarray/storage/storage_exception.h
        suffixarray/storage/CorporaStorage.cpp suffixarray/storage/CorporaStorage.h
        suffixarray/storage/StorageManifest.cpp suffixarray/storage/StorageManifest.h
        suffixarray/storage/StorageIterator.cpp suffixarray/storage/StorageIterator.h
        suffixarray/storage/StorageBucket.cpp suffixarray/storage/StorageBucket.h

        sapt/Options.h
        sapt/TranslationOption.h
        sapt/PhraseTable.cpp sapt/PhraseTable.h
        sapt/LexicalModel.cpp sapt/LexicalModel.h
        sapt/UpdateManager.cpp sapt/UpdateManager.h
        sapt/TranslationOptionBuilder.cpp sapt/TranslationOptionBuilder.h

        util/hashutils.h
        util/ioutils.h
        util/chrono.h
        util/randutils.h util/randutils.cpp
        util/BilingualCorpus.cpp util/BilingualCorpus.h
        util/BackgroundPollingThread.cpp util/BackgroundPollingThread.h)

include_directories(${CMAKE_SOURCE_DIR}/suffixarray-phrasetable)

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES} $<TARGET_OBJECTS:mmt_logging> $<TARGET_OBJECTS:mmt_vocabulary>)

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

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

message(STATUS "Executables will be installed in ${CMAKE_INSTALL_PREFIX}/bin")
message(STATUS "Libraries will be installed in ${CMAKE_INSTALL_PREFIX}/lib")

## Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.55.0 REQUIRED COMPONENTS filesystem thread system program_options)
include_directories(${Boost_INCLUDE_DIRS})
target_link_libraries(${PROJECT_NAME} ${Boost_LIBRARIES})

## RocksDB
find_package(Rocksdb REQUIRED)
include_directories(${Rocksdb_INCLUDE_DIR})
target_link_libraries(${PROJECT_NAME} ${Rocksdb_LIBS})

## OpenMP
find_package(OpenMP QUIET)
if (OPENMP_FOUND)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}")
    include_directories(${OpenMP_INCLUDE_DIR})
    link_directories(${OpenMP_LIBRARY_DIR})

    message(STATUS "Compiling with OpenMP")
endif (OPENMP_FOUND)

# Test cases
add_subdirectory(test)