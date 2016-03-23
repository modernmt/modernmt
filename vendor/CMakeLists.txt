# Top-level build file for C++ parts of MMT
# =========================================
#
# This builds every C++ dependency of MMT.
#
# TODO: IRSTLM needs to be built separately first (because moses looks for it at config time).
# TODO: to support clean build from scratch, this needs to be written using ExternalProject_Add()
#
# The sources in vendor/ come from git submodules.
# See INSTALL.md for details.
#
# Author: David Madl <git@abanbytes.eu>

# old enough for AWS Ubuntu 14.04
cmake_minimum_required(VERSION 3.2)

# <MMT> refers to this directory (checkout root of https://github.com/ModernMT/MMT/)


# Set install prefix to use <MMT>/bin/ and <MMT>/lib/
# ---------------------------------------------------
#
if(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
  set(CMAKE_INSTALL_PREFIX
    "${CMAKE_SOURCE_DIR}" CACHE PATH "MMT install prefix" FORCE
    )

  # <hack>
  # pretend we have a prefix set by the user.
  # This works around irstlm/CMakeLists.txt checking IF(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
  # Maybe we should remove that block there instead.
  #
  set(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT OFF CACHE BOOL "override vs. IRSTLM" FORCE)
  # </hack>

endif(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)


# Configure IRSTLM for dynamic linking
# ------------------------------------
#
# original was: cmake .. -DCMAKE_INSTALL_PREFIX=$(pwd) -DCXX0:BOOL=OFF -DBUILD_SHARED_LIBS=ON -DBUILD_STATIC_LIBS=OFF
#
set(BUILD_STATIC_LIBS OFF CACHE BOOL "Build irstlm static library")
set(BUILD_SHARED_LIBS ON CACHE BOOL "Build irstlm shared library")


add_subdirectory(irstlm)
add_subdirectory(moses)

add_subdirectory(Decoder/native)
