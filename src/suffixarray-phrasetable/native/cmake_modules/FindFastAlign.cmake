# - Find FastAlign (db.h, libfastalign.a, libfastalign.so)
# This module defines
#  FastAlign_INCLUDE_DIR, directory containing headers
#  FastAlign_LIBS, directory containing FastAlign libraries
#  FastAlign_FOUND, whether FastAlign has been found

set(FastAlign_SEARCH_HEADER_PATHS
        ${FASTALIGN_ROOT}/include
        )

set(FastAlign_SEARCH_LIB_PATH
        ${FASTALIGN_ROOT}/lib
        )

find_path(FastAlign_INCLUDE_DIR fastalign/FastAligner.h PATHS
        ${FastAlign_SEARCH_HEADER_PATHS}
        )

find_library(FastAlign_LIB_PATH NAMES fastalign PATHS ${FastAlign_SEARCH_LIB_PATH})

if (FastAlign_INCLUDE_DIR AND FastAlign_LIB_PATH)
    set(FastAlign_FOUND TRUE)
    set(FastAlign_LIBS ${FastAlign_LIB_PATH})
else ()
    set(FastAlign_FOUND FALSE)
endif ()

if (FastAlign_FOUND)
    if (NOT FastAlign_FIND_QUIETLY)
        message(STATUS "Found the FastAlign library: ${FastAlign_LIB_PATH} ${FastAlign_INCLUDE_DIR}")
    endif ()
else ()
    if (NOT FastAlign_FIND_QUIETLY)
        set(FastAlign_ERR_MSG "Could not find the FastAlign library.")
        set(FastAlign_ERR_MSG "Could not find the Rocksdb library. Set FASTALIGN_ROOT to the FastAlign root folder (current value is '${FASTALIGN_ROOT}')")
        if (FastAlign_FIND_REQUIRED)
            message(FATAL_ERROR "${FastAlign_ERR_MSG}")
        else (FastAlign_FIND_REQUIRED)
            message(STATUS "${FastAlign_ERR_MSG}")
        endif (FastAlign_FIND_REQUIRED)
    endif ()
endif ()

mark_as_advanced(
        FastAlign_INCLUDE_DIR
        FastAlign_LIBS
)
