#!/bin/bash
#
# Set up MMT within a conda environment.
#
# Create conda environment 'mmt' like this:
#
# $ conda create --name mmt -c https://conda.anaconda.org/cidermole cmake gcc boost zlib gperftools python=2.7 requests jdk8 apache-maven

# this script is in <MMT>/extras/
MMT_HOME=$(dirname $0)/..

if [ -z "$CONDA_ENV_PATH" ]; then
  echo "Please activate a conda environment with: source activate env-name"
  exit 1
fi

# due to the weird LD_LIBRARY_PATH setup in MMT, we need these libs to be available in <MMT>/lib/
for f in $CONDA_ENV_PATH/lib/libboost_* $CONDA_ENV_PATH/lib/{libstdc++.so.6,libgcc_s*,libz.so*,libtcmalloc_minimal.so*}; do
	ln -sf $f $MMT_HOME/lib/$(basename $f)
done

