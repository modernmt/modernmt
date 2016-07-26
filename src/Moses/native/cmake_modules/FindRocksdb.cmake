# - Find Rocksdb (db.h, librocksdb.a, librocksdb.so)
# This module defines
#  Rocksdb_INCLUDE_DIR, directory containing headers
#  Rocksdb_LIBS, directory containing snappy libraries
#  Rocksdb_FOUND, whether snappy has been found

set(Rocksdb_SEARCH_HEADER_PATHS
        ${PROJECT_SOURCE_DIR}/../../../vendor/rocksdb/include
        )

set(Rocksdb_SEARCH_LIB_PATH
        ${PROJECT_SOURCE_DIR}/../../../build/lib/
        )

find_path(Rocksdb_INCLUDE_DIR rocksdb/db.h PATHS
        ${Rocksdb_SEARCH_HEADER_PATHS}
        # make sure we don't accidentally pick up a different version
        NO_DEFAULT_PATH
        )

find_library(Rocksdb_LIB_PATH NAMES rocksdb PATHS ${Rocksdb_SEARCH_LIB_PATH} NO_DEFAULT_PATH)

if (Rocksdb_INCLUDE_DIR AND Rocksdb_LIB_PATH)
    set(Rocksdb_FOUND TRUE)
    set(Rocksdb_LIBS ${Rocksdb_LIB_PATH})
else ()
    set(Rocksdb_FOUND FALSE)
endif ()

if (Rocksdb_FOUND)
    if (NOT Rocksdb_FIND_QUIETLY)
        message(STATUS "Found the Rocksdb library: ${Rocksdb_LIB_PATH}")
    endif ()
else ()
    if (NOT Rocksdb_FIND_QUIETLY)
        set(Rocksdb_ERR_MSG "Could not find the Rocksdb library. Looked for headers")
        set(Rocksdb_ERR_MSG "${Rocksdb_ERR_MSG} in ${Rocksdb_SEARCH_HEADER_PATHS}, and for libs")
        set(Rocksdb_ERR_MSG "${Rocksdb_ERR_MSG} in ${Rocksdb_SEARCH_LIB_PATH}")
        if (Rocksdb_FIND_REQUIRED)
            message(FATAL_ERROR "${Rocksdb_ERR_MSG}")
        else (Rocksdb_FIND_REQUIRED)
            message(STATUS "${Rocksdb_ERR_MSG}")
        endif (Rocksdb_FIND_REQUIRED)
    endif ()
endif ()

mark_as_advanced(
        Rocksdb_INCLUDE_DIR
        Rocksdb_LIBS
)