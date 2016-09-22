# Explicitly list the source files for this subdirectory
#
# If you add any source files to this subdirectory
#    that should be included in the kenlm library,
#        (this excludes any unit test files)
#    you should add them to the following list:
set(KENLM_LM_SOURCE
	bhiksha.cc
	binary_format.cc
	config.cc
	lm_exception.cc
	model.cc
	quantize.cc
	read_arpa.cc
	search_hashed.cc
	search_trie.cc
	sizes.cc
	trie.cc
	trie_sort.cc
	value_build.cc
	virtual_interface.cc
	vocab.cc
)


# Group these objects together for later use.
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#
add_subdirectory(common)

set(THREADS pthread)

# This directory has children that need to be processed
add_subdirectory(builder)

add_library(kenlm OBJECT ${KENLM_LM_SOURCE} ${KENLM_LM_COMMON_SOURCE})
set (TARGET_OBJECTS_kenlm $<TARGET_OBJECTS:kenlm> ${TARGET_OBJECTS_kenlm_builder} PARENT_SCOPE)

set(KENLM_MAX_ORDER 6 CACHE STRING "Maximum supported ngram order")
target_compile_definitions(kenlm PUBLIC -DKENLM_MAX_ORDER=${KENLM_MAX_ORDER})
