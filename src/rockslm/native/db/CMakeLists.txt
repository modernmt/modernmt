# Explicitly list the source files for this subdirectory
#
# If you add any source files to this subdirectory
#    that should be included in the kenlm library,
#        (this excludes any unit test files)
#    you should add them to the following list:
set(DB_SOURCE
        dbkey.h
        counts.h
        NGramStorage.cpp NGramStorage.h
        NGramBatch.cpp NGramBatch.h)

# Group these objects together for later use.
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#

add_library(db OBJECT ${DB_SOURCE})
set (TARGET_OBJECTS_db $<TARGET_OBJECTS:db> PARENT_SCOPE)

