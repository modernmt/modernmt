# Explicitly list the source files for this subdirectory
#
# If you add any source files to this subdirectory
#    that should be included in the kenlm library,
#        (this excludes any unit test files)
#    you should add them to the following list:
set(UTIL_SOURCE
        chrono.h
        ioutils.h
        BackgroundPollingThread.cpp BackgroundPollingThread.h)

# Group these objects together for later use.
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#

add_library(util OBJECT ${UTIL_SOURCE})
set(TARGET_OBJECTS_util $<TARGET_OBJECTS:util> PARENT_SCOPE)

