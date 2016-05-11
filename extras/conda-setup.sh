#!/bin/bash
#
# Set up MMT within a conda environment.

FAST_ALIGN=opt/bin/fastalign-maurobuild/fast_align

# this script is in <MMT>/extras/
MMT_HOME=$(dirname $0)/..

if [ -z "$CONDA_ENV_PATH" ]; then
  echo 'Please activate a conda environment with: $ source activate <your-env-name>'
  echo ""
  echo "If you do not have one, create one with:"
  echo '$ conda create --name mmt -c https://conda.anaconda.org/cidermole cmake gcc boost zlib gperftools python=2.7 requests jdk8 apache-maven patchelf'
  echo ""
  echo '$ source activate mmt'
  exit 1
fi

root_dir=$(echo "$CONDA_ENV_PATH" | awk -F "/" '{print $2}')
if [ "$root_dir" == "fs" ]; then
  echo '(Edinburgh note: conda itself should be pointed to via /mnt instead of /fs so it sets up RPATHs correctly for cluster-wide use).' >&2
fi

if [ ! -e "$MMT_HOME/$FAST_ALIGN" ]; then
  echo "Could not find fast_align binary for RPATH patching - did you forget to download 'opt' resources?"
  echo "see https://github.com/ModernMT/MMT/blob/master/INSTALL.md#install-mmt"
  exit 1
fi

# due to the weird LD_LIBRARY_PATH setup in MMT, we need these libs to be available in <MMT>/lib/
for f in $CONDA_ENV_PATH/lib/libboost_* $CONDA_ENV_PATH/lib/{libstdc++.so.6,libgcc_s*,libz.so*,libtcmalloc_minimal.so*}; do
	ln -sf $f $MMT_HOME/lib/$(basename $f)
done

# fast_align is built with C++11 and its binary is shipped in opt/ - we have to patch it to find recent libstdc++.so.6
patchelf --set-rpath $MMT_HOME/lib $MMT_HOME/$FAST_ALIGN
