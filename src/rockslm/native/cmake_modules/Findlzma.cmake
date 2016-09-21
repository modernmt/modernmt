# - Find lzma
# Find the native lzma includes and library
#
#  lzma_INCLUDE_DIR - where to find lzma.h, etc.
#  lzma_LIBRARIES   - List of libraries when using lzma.
#  lzma_FOUND       - True if lzma found.

set(lzma_NAMES lzma)

find_library(lzma_LIBRARY NAMES ${lzma_NAMES} )

if (lzma_LIBRARY)
    set(lzma_FOUND TRUE)
    set( lzma_LIBRARIES ${lzma_LIBRARY})
else ()
    set(lzma_FOUND FALSE)
    set( lzma_LIBRARIES )
endif ()

if (lzma_FOUND)
    message(STATUS "Found lzma: ${lzma_LIBRARY}")
    message(STATUS "Found lzma: ${lzma_LIBRARIES}")
else ()
    message(STATUS "Not Found lzma")
    if (lzma_FIND_REQUIRED)
        message(STATUS "Looked for lzma libraries named ${lzma_NAMES}.")
        message(FATAL_ERROR "Could NOT find lzma library")
    endif ()
endif ()

mark_as_advanced(
        lzma_LIBRARIES
)
