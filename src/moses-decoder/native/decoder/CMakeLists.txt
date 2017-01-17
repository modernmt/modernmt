# Includes

# for #include<moses/...> style includes
include_directories(.)
#include_directories(${CMAKE_SOURCE_DIR}/decoder)

# for the main interface to moses, include/decoder/MosesDecoder.h
include_directories(include)


# Sources

add_subdirectory(moses)

