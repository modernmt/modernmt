# - Find ROCKSDB (db.h, librocksdb.a, librocksdb.so)
# This module defines
#  ROCKSDB_INCLUDE_DIRS, directory containing headers
#  ROCKSDB_LIBRARIES, directory containing snappy libraries
#  ROCKSDB_FOUND, whether snappy has been found

set(ROCKSDB_SEARCH_HEADER_PATHS ${ROCKSDB_ROOT}/include $ENV{ROCKSDB_ROOT}/include )

set(ROCKSDB_SEARCH_LIB_PATHS ${ROCKSDB_ROOT}/lib $ENV{ROCKSDB_ROOT}/lib )

find_path(ROCKSDB_INCLUDE_DIRS rocksdb/db.h PATHS ${ROCKSDB_SEARCH_HEADER_PATHS} )

find_library(ROCKSDB_LIBRARIES NAMES rocksdb PATHS ${ROCKSDB_SEARCH_LIB_PATHS})

if (ROCKSDB_INCLUDE_DIRS AND ROCKSDB_LIBRARIES)
    set(ROCKSDB_FOUND TRUE)
else ()
    set(ROCKSDB_FOUND FALSE)
endif ()

if (ROCKSDB_FOUND)
    if (NOT ROCKSDB_FIND_QUIETLY)
        message(STATUS "Found ROCKSDB: ${ROCKSDB_LIBRARIES}")
    endif ()
else ()
    if (NOT ROCKSDB_FIND_QUIETLY)
        set(ROCKSDB_ERR_MSG "Could not find ROCKSDB. Set ROCKSDB_ROOT to the RocksDB root folder (current value is '${ROCKSDB_ROOT}')")
        if (ROCKSDB_FIND_REQUIRED)
            message(FATAL_ERROR "${ROCKSDB_ERR_MSG}")
        else (ROCKSDB_FIND_REQUIRED)
            message(STATUS "${ROCKSDB_ERR_MSG}")
        endif (ROCKSDB_FIND_REQUIRED)
    endif ()
endif ()

mark_as_advanced(
        ROCKSDB_INCLUDE_DIRS
        ROCKSDB_LIBRARIES
        ROCKSDB_FOUND
)
