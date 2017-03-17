# Explicitly list the source files for this subdirectory
#
# If you add any source files to this subdirectory
#    that should be included in the kenlm library,
#        (this excludes any unit test files)
#    you should add them to the following list:
set(CORPUS_SOURCE
        CorpusReader.cpp
        CorpusReader.h
        )


# Group these objects together for later use.
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#

add_library(corpus OBJECT ${CORPUS_SOURCE})
set(TARGET_OBJECTS_corpus $<TARGET_OBJECTS:corpus> PARENT_SCOPE)

