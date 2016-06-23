# - Try to find Moses
# Once done this will define
#  Moses_FOUND
#  Moses_INCLUDE_DIRS
#  Moses_LIBRARIES

# MMT builds mosesdecoder in <MMT>/vendor/moses and install prefix is <MMT>/build
#
# can either use CMAKE_SOURCE_DIR which will be <MMT> if built on that level, and
#   <MMT>/src/Moses/native if only Decoder is built.
#
# or, we can use PROJECT_SOURCE_DIR which should always point to <MMT>/src/Moses/native
#   for the Decoder project.
#
find_path(Moses_INCLUDE_DIRS moses/FF/StatefulFeatureFunction.h
          HINTS "${PROJECT_SOURCE_DIR}/../../../vendor/moses/")

find_library(Moses_LIBRARIES NAMES moses libmoses
             HINTS "${PROJECT_SOURCE_DIR}/../../../build/lib/")

# find_path(Moses_LIBRARIES build/lib/${CMAKE_SHARED_LIBRARY_PREFIX}moses${CMAKE_SHARED_LIBRARY_SUFFIX} PATHS ENV MosesDECODER_HOME)
#mark_as_advanced(LIBXML2_INCLUDE_DIR LIBXML2_LIBRARY )

if (Moses_LIBRARIES STREQUAL "Moses_LIBRARIES-NOTFOUND")
  message(FATAL_ERROR "Could not locate Moses shared library.")
else()
  set(Moses_FOUND TRUE)
  message(STATUS "Found Moses: ${Moses_LIBRARIES}")
endif()