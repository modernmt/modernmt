# Explicitly list the source files for this subdirectory
#
# If you add any source files to this subdirectory
#    that should be included in the kenlm library,
#        (this excludes any unit test files)
#    you should add them to the following list:
#
# Because we do not set PARENT_SCOPE in the following definition,
#    CMake files in the parent directory won't be able to access this variable.
#
set(KENLM_UTIL_SOURCE
		bit_packing.cc
		ersatz_progress.cc
		exception.cc
		file.cc
		file_piece.cc
		float_to_string.cc
		integer_to_string.cc
		mmap.cc
		murmur_hash.cc
		parallel_read.cc
		pool.cc
		read_compressed.cc
		scoped.cc
        spaces.cc
		string_piece.cc
		usage.cc
	)

set(READ_COMPRESSED_FLAGS)
set(READ_COMPRESSED_LIBS)
find_package(ZLIB)
if (ZLIB_FOUND)
  set(READ_COMPRESSED_FLAGS "${READ_COMPRESSED_FLAGS} -DHAVE_ZLIB")
  set(READ_COMPRESSED_LIBS ${READ_COMPRESSED_LIBS} ${ZLIB_LIBRARIES})
  include_directories(${ZLIB_INCLUDE_DIR})
endif()

find_package(BZip2)
if (BZIP2_FOUND)
  set(READ_COMPRESSED_FLAGS "${READ_COMPRESSED_FLAGS} -DHAVE_BZLIB")
  set(READ_COMPRESSED_LIBS ${READ_COMPRESSED_LIBS} ${BZIP2_LIBRARIES})
  include_directories(${BZIP2_INCLUDE_DIR})
endif()

find_package(LibLZMA)
if (LIBLZMA_FOUND)
  set(READ_COMPRESSED_FLAGS "${READ_COMPRESSED_FLAGS} -DHAVE_XZLIB")
  set(READ_COMPRESSED_LIBS ${READ_COMPRESSED_LIBS} ${LIBLZMA_LIBRARIES})
  include_directories(${LIBLZMA_INCLUDE_DIRS})
endif()
set_source_files_properties(read_compressed.cc PROPERTIES COMPILE_FLAGS ${READ_COMPRESSED_FLAGS})
set_source_files_properties(read_compressed_test.cc PROPERTIES COMPILE_FLAGS ${READ_COMPRESSED_FLAGS})
set_source_files_properties(file_piece_test.cc PROPERTIES COMPILE_FLAGS ${READ_COMPRESSED_FLAGS})

# This directory has children that need to be processed
add_subdirectory(double-conversion)
add_subdirectory(stream)

if (UNIX AND NOT APPLE)
  set(TIMER_LINK rt)
else()
  set(TIMER_LINK)
endif()

if (NOT MSVC)
	set(THREADS pthread)
endif()

# Group these objects together for later use.
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#
add_library(kenlm_util OBJECT ${KENLM_UTIL_DOUBLECONVERSION_SOURCE} ${KENLM_UTIL_STREAM_SOURCE} ${KENLM_UTIL_SOURCE})
set (TARGET_OBJECTS_kenlm_util $<TARGET_OBJECTS:kenlm_util> PARENT_SCOPE)
