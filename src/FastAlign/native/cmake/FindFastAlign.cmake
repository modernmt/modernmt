# - Try to find FastAlign
# Once done this will define
#  FastAlign_FOUND
#  FastAlign_INCLUDE_DIRS
#  FastAlign_LIBRARIES

# MMT builds fastalign in <MMT>/vendor/fastalign and install prefix is <MMT>/build
#
find_path(FastAlign_INCLUDE_DIRS src/fast_align.cc
        HINTS "${PROJECT_SOURCE_DIR}/../../../vendor/fastalign/")

find_library(FastAlign_LIBRARIES NAMES fastalign libfastalign
        HINTS "${PROJECT_SOURCE_DIR}/../../../build/lib/")

if (FastAlign_LIBRARIES STREQUAL "FastAlign_LIBRARIES-NOTFOUND")
    message(FATAL_ERROR "Could not locate FastAlign shared library.")
else()
    set(FastAlign_FOUND TRUE)
    message(STATUS "Found Moses: ${FastAlign_LIBRARIES}")
endif()