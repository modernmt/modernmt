cmake_minimum_required(VERSION 3.2)
project(jcld2)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-narrowing -fPIC")

set(SOURCE_FILES
        cld2/internal/cldutil.cc
        cld2/internal/cldutil_shared.cc
        cld2/internal/compact_lang_det.cc
        cld2/internal/compact_lang_det_hint_code.cc
        cld2/internal/compact_lang_det_impl.cc
        cld2/internal/debug.cc
        cld2/internal/fixunicodevalue.cc
        cld2/internal/generated_entities.cc
        cld2/internal/generated_language.cc
        cld2/internal/generated_ulscript.cc
        cld2/internal/getonescriptspan.cc
        cld2/internal/lang_script.cc
        cld2/internal/offsetmap.cc
        cld2/internal/scoreonescriptspan.cc
        cld2/internal/tote.cc
        cld2/internal/utf8statetable.cc
        cld2/internal/cld_generated_cjk_uni_prop_80.cc
        cld2/internal/cld2_generated_cjk_compatible.cc
        cld2/internal/cld_generated_cjk_delta_bi_32.cc
        cld2/internal/generated_distinct_bi_0.cc
        cld2/internal/cld2_generated_quad0122.cc
        cld2/internal/cld2_generated_deltaocta0122.cc
        cld2/internal/cld2_generated_distinctocta0122.cc
        cld2/internal/cld_generated_score_quad_octa_0122.cc

        java/jniutil.h
        java/eu_modernmt_cleaning_filters_lang_cld2_CLD2LanguageDetector.cpp
        javah/eu_modernmt_cleaning_filters_lang_cld2_CLD2LanguageDetector.h)

include_directories(${CMAKE_SOURCE_DIR})
include_directories(javah)

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

## JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

install(TARGETS ${PROJECT_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)