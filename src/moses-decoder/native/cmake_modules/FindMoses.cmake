# - Try to find Moses
# Once done this will define
#  Moses_FOUND
#  Moses_INCLUDE_DIRS
#  Moses_LIBRARIES

# MMT builds mosesdecoder in <MMT>/vendor/moses and install prefix is <MMT>/build
#
# can either use CMAKE_SOURCE_DIR which will be <MMT> if built on that level, and
#   <MMT>/test/Moses/native if only Decoder is built.
#
# or, we can use PROJECT_SOURCE_DIR which should always point to <MMT>/test/Moses/native
#   for the Decoder project.
#
find_path(Moses_INCLUDE_DIRS moses/FF/StatefulFeatureFunction.h
          HINTS "${PROJECT_SOURCE_DIR}/../../../vendor/moses/")

find_library(Moses_LIBRARIES NAMES moses libmoses
             HINTS "${PROJECT_SOURCE_DIR}/../../../build/lib/")

if (Moses_LIBRARIES STREQUAL "Moses_LIBRARIES-NOTFOUND")
    message(FATAL_ERROR "Could not locate Moses shared library.")
else()
    set(Moses_FOUND TRUE)
    set(Moses_DEFINITIONS -DMAX_NUM_FACTORS=4 -DWITH_THREADS=1)
    message(STATUS "Found Moses: ${Moses_LIBRARIES}")
endif()