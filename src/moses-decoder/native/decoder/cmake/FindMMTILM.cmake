# - Try to find MMTILM
# Once done this will define
#  MMTILM_FOUND
#  MMTILM_INCLUDE_DIRS
#  MMTILM_LIBRARIES

set(MMTILM_SEARCH_HEADER_PATHS ${MMTILM_ROOT}/include $ENV{MMTILM_ROOT}/include )

set(MMTILM_SEARCH_LIB_PATHS ${MMTILM_ROOT}/lib $ENV{MMTILM_ROOT}/lib )

find_path(MMTILM_INCLUDE_DIRS ilm/InterpolatedLM.h PATHS ${MMTILM_SEARCH_HEADER_PATHS} )

find_library(MMTILM_LIBRARIES NAMES ilm PATHS ${MMTILM_SEARCH_LIB_PATHS} )

if (MMTILM_INCLUDE_DIRS AND MMTILM_LIBRARIES)
    set(MMTILM_FOUND TRUE)
else ()
    set(MMTILM_FOUND FALSE)
endif ()

if (MMTILM_FOUND)
    set (MMTILM_INCLUDE_DIRS ${MMTILM_INCLUDE_DIRS} ${MMTILM_INCLUDE_DIRS})
    if (NOT MMTILM_FIND_QUIETLY)
        message(STATUS "Found MMT InterpolatedLM: ${MMTILM_LIBRARIES} ${MMTILM_INCLUDE_DIRS}")
    endif ()
else ()
    if (NOT MMTILM_FIND_QUIETLY)
        set(MMTILM_ERR_MSG "Could not find the MMT InterpolatedLM library. Set MMTILM_ROOT to the InterpolatedLM root folder (current value is '${MMTILM_ROOT}')")
        if (MMTILM_FIND_REQUIRED)
            message(FATAL_ERROR "${MMTILM_ERR_MSG}")
        else (MMTILM_FIND_REQUIRED)
            message(STATUS "${MMTILM_ERR_MSG}")
        endif (MMTILM_FIND_REQUIRED)
    endif ()
endif ()

mark_as_advanced(
        MMTILM_INCLUDE_DIRS
        MMTILM_LIBRARIES
        MMTILM_FOUND
)

