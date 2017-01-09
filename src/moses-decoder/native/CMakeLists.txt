cmake_minimum_required(VERSION 3.2)
project(mmt-decoder)

# for javah/ which is filled with auto-generated header eu_modernmt_decoder_moses_MosesDecoder.h
include_directories(.)

add_subdirectory(decoder)
