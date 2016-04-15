#!/bin/bash

# exit if one of the commands fails
set -e

###

# this script is in <MMT>/vendor/
MMT_HOME=$(readlink -f $(dirname $0)/..)

# in a conda environment, we need RPATH of binaries to point to the locally installed lib/
if [ ! -z "$CONDA_ENV_PATH" ]; then
    ADDITIONAL_CMAKE_OPTIONS="-DCMAKE_BUILD_WITH_INSTALL_RPATH=TRUE -DCMAKE_INSTALL_RPATH=$MMT_HOME/lib"
fi

###


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MMT_HOME="$DIR/../"

# Build IRSTLM
mkdir -p "$DIR/irstlm/build"
pushd "$DIR/irstlm/build" &> /dev/null

cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$MMT_HOME" -DCXX0:BOOL=OFF -DBUILD_SHARED_LIBS=ON -DBUILD_STATIC_LIBS=OFF
make -j$(nproc)
make install

popd &> /dev/null

# Build Moses
mkdir -p "$DIR/moses/build"
pushd "$DIR/moses/build" &> /dev/null

cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$MMT_HOME" $ADDITIONAL_CMAKE_OPTIONS
make -j$(nproc)
make install

popd &> /dev/null
