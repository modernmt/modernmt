#!/bin/bash

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

cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$MMT_HOME"
make -j$(nproc)
make install

popd &> /dev/null
