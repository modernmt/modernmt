# - Find MMT common-interface include dir
# This module defines
#  MMTCommons_INCLUDE_DIR, directory containing headers
#  MMTCommons_FOUND, whether MMT common-interface has been found

set(MMTCommons_SEARCH_HEADER_PATHS ${CMAKE_SOURCE_DIR}/../../common-interfaces/native/)

find_path(MMTCommons_INCLUDE_DIR include/mmt/sentence.h PATHS ${MMTCommons_SEARCH_HEADER_PATHS})

if (MMTCommons_INCLUDE_DIR)
    set(MMTCommons_FOUND TRUE)
    set(MMTCommons_INCLUDE_DIR ${MMTCommons_INCLUDE_DIR}/include)
else ()
    set(MMTCommons_FOUND FALSE)
endif ()

if (MMTCommons_FOUND)
    if (NOT MMTCommons_FIND_QUIETLY)
        message(STATUS "Found MMT common-interfaces module: ${MMTCommons_INCLUDE_DIR}")
    endif ()
else ()
    if (NOT MMTCommons_FIND_QUIETLY)
        set(MMTCommons_ERR_MSG "Could not find the MMT common-interfaces module. Searched in '${MMTCommons_SEARCH_HEADER_PATHS}'")
        if (MMTCommons_FIND_REQUIRED)
            message(FATAL_ERROR "${MMTCommons_ERR_MSG}")
        else (MMTCommons_FIND_REQUIRED)
            message(STATUS "${MMTCommons_ERR_MSG}")
        endif (MMTCommons_FIND_REQUIRED)
    endif ()
endif ()

mark_as_advanced(
        MMTCommons_INCLUDE_DIR
)