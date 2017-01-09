# - Try to find SAPT
# Once done this will define
#  SAPT_FOUND
#  SAPT_INCLUDE_DIRS
#  SAPT_LIBRARIES

set(SAPT_SEARCH_HEADER_PATHS ${SAPT_ROOT}/include $ENV{SAPT_ROOT}/include )

set(SAPT_SEARCH_LIB_PATHS ${SAPT_ROOT}/lib $ENV{SAPT_ROOT}/lib )

find_path(SAPT_INCLUDE_DIRS sapt/PhraseTable.h PATHS ${SAPT_SEARCH_HEADER_PATHS} )

find_library(SAPT_LIBRARIES NAMES sapt PATHS ${SAPT_SEARCH_LIB_PATHS} )

if (SAPT_INCLUDE_DIRS AND SAPT_LIBRARIES)
    set(SAPT_FOUND TRUE)
else ()
    set(SAPT_FOUND FALSE)
endif ()

if (SAPT_FOUND)
    set (SAPT_INCLUDE_DIRS ${SAPT_INCLUDE_DIRS} ${SAPT_INCLUDE_DIRS})
    if (NOT SAPT_FIND_QUIETLY)
        message(STATUS "Found SAPT: ${SAPT_LIBRARIES} ${SAPT_INCLUDE_DIRS}")
    endif ()
else ()
    if (NOT SAPT_FIND_QUIETLY)
        set(SAPT_ERR_MSG "Could not find the SAPT library. Set SAPT_ROOT to the SAPT root folder (current value is '${SAPT_ROOT}')")
        if (SAPT_FIND_REQUIRED)
            message(FATAL_ERROR "${SAPT_ERR_MSG}")
        else (SAPT_FIND_REQUIRED)
            message(STATUS "${SAPT_ERR_MSG}")
        endif (SAPT_FIND_REQUIRED)
    endif ()
endif ()

mark_as_advanced(
        SAPT_INCLUDE_DIRS
        SAPT_LIBRARIES
        SAPT_FOUND
)

