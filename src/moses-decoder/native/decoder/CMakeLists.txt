# Includes

# for #include<moses/...> style includes
include_directories(.)
#include_directories(${CMAKE_SOURCE_DIR}/decoder)

# for the main interface to moses, include/decoder/MosesDecoder.h
include_directories(include)


# Sources

set(MOSES_SRC
        # MMT specific: disable syntax decoding, unused phrase tables, unused features
        contrib/other-builds/mmt/FF/Factory.cpp
        contrib/other-builds/mmt/IOWrapper.cpp
        contrib/other-builds/mmt/TranslationTask.cpp
        )
add_library(moses_other_mmt OBJECT ${MOSES_SRC})

add_subdirectory(util)  # KenLM utils, used mostly by MERT

add_subdirectory(moses)
add_subdirectory(mert)

