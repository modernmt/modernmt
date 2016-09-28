# - Find Rocksdb (db.h, librocksdb.a, librocksdb.so)
# This module defines
#  Rocksdb_INCLUDE_DIR, directory containing headers
#  Rocksdb_LIBS, directory containing rocksdb libraries
#  Rocksdb_FOUND, whether rocksdb has been found

set(Rocksdb_SEARCH_HEADER_PATHS
        ${ROCKSDB_ROOT}/include
        )

set(Rocksdb_SEARCH_LIB_PATH
        ${ROCKSDB_ROOT}/lib
        )

find_path(Rocksdb_INCLUDE_DIR rocksdb/db.h PATHS
        ${Rocksdb_SEARCH_HEADER_PATHS}
        )

find_library(Rocksdb_LIB_PATH NAMES rocksdb PATHS ${Rocksdb_SEARCH_LIB_PATH})

if (Rocksdb_INCLUDE_DIR AND Rocksdb_LIB_PATH)
    set(Rocksdb_FOUND TRUE)
    set(Rocksdb_LIBS ${Rocksdb_LIB_PATH})
#    set(Rocksdb_INCLUDE_DIR ${Rocksdb_INCLUDE_DIR}/include)
else ()
    set(Rocksdb_FOUND FALSE)
endif ()

if (Rocksdb_FOUND)
    if (NOT Rocksdb_FIND_QUIETLY)
        message(STATUS "Found the Rocksdb library: ${Rocksdb_LIB_PATH} ${Rocksdb_INCLUDE_DIR}")
    endif ()
else ()
    if (NOT Rocksdb_FIND_QUIETLY)
        set(Rocksdb_ERR_MSG "Could not find the Rocksdb library. Set ROCKSDB_ROOT to the RocksDB root folder (current value is '${ROCKSDB_ROOT}')")
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
