# - Try to find GooglePerftools headers and libraries
#
# Usage of this module as follows:
#
#     find_package(GooglePerftools)
#
# Variables used by this module, they can change the default behaviour and need
# to be set before calling find_package:
#
#  GooglePerftools_ROOT_DIR  Set this variable to the root installation of
#                            GooglePerftools if the module has problems finding 
#                            the proper installation path.
#
# Variables defined by this module:
#
#  GOOGLEPERFTOOLS_FOUND              System has GooglePerftools libs/headers
#  TCMALLOC_FOUND                     System has GooglePerftools tcmalloc library
#  GooglePerftools_LIBRARIES          The GooglePerftools libraries
#  GooglePerftools_LIBRARIES_DEBUG    The GooglePerftools libraries for heap checking.
#  GooglePerftools_INCLUDE_DIR        The location of GooglePerftools headers
#
# Kudos to https://github.com/bro/cmake

find_path(GooglePerftools_ROOT_DIR
    NAMES include/google/heap-profiler.h
    PATHS ENV GooglePerftools_ROOT_DIR
)

find_library(GooglePerftools_LIBRARIES_DEBUG
    NAMES tcmalloc_and_profiler
    HINTS ${GooglePerftools_ROOT_DIR}/lib
)

find_library(GooglePerftools_LIBRARIES
#    NAMES tcmalloc tcmalloc_minimal
    NAMES tcmalloc_minimal
    HINTS ${GooglePerftools_ROOT_DIR}/lib
)

find_path(GooglePerftools_INCLUDE_DIR
    NAMES google/heap-profiler.h
    HINTS ${GooglePerftools_ROOT_DIR}/include
)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(GooglePerftools DEFAULT_MSG
    GooglePerftools_LIBRARIES
    GooglePerftools_LIBRARIES_DEBUG
    GooglePerftools_INCLUDE_DIR
)
find_package_handle_standard_args(tcmalloc DEFAULT_MSG
    GooglePerftools_LIBRARIES
)

mark_as_advanced(
    GooglePerftools_ROOT_DIR
    GooglePerftools_LIBRARIES
    GooglePerftools_LIBRARIES_DEBUG
    GooglePerftools_INCLUDE_DIR
)
