# Explicitly list the source files for this subdirectory
#
# If you add any source files to this subdirectory
#    that should be included in the kenlm library,
#        (this excludes any unit test files)
#    you should add them to the following list:
set(LM_SOURCE
        Options.h
        LM.h
        InterpolatedLM.cpp InterpolatedLM.h
        AdaptiveLM.cpp AdaptiveLM.h
        AdaptiveLMCache.h
        BufferedUpdateManager.cpp BufferedUpdateManager.h
        StaticLM.cpp StaticLM.h
        CachedLM.cpp CachedLM.h
)


# Group these objects together for later use.
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#

add_library(lm OBJECT ${LM_SOURCE})
set (TARGET_OBJECTS_lm $<TARGET_OBJECTS:lm> PARENT_SCOPE)

