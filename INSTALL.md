## System Requirements

###Storage:

Make sure you have available space in:
- engines/ (at least 4 times the corpus size) 

### Linux Version

Only works on: 
- Ubuntu 14.04

On Amazon AWS:
AMI: thefactory-ubuntu-14.04-base-2014-09-02T00-42-39Z - ami-028c2b6a
Instance: c3.4xlarge (30GB RAM, 16 core, circa $0.90/hour)

### Libraries required to run MMT:
- sudo apt-get update
- sudo apt-get install openjdk-7-jdk
- sudo apt-get install jsvc
- sudo apt-get install make


## Current Limitations

- It only supports latin languages.
- context and query need to be tokenized before input.
- incremental training not implemented yet.


### Support
You can report issues to davide.caroselli@translated.net


# Optional Stuff (for Open Source Contributors)

## Instructions to compile MMT:

### General-purpose
sudo apt-get install build-essential
sudo apt-get install cmake

### IRSTLM
sudo apt-get install autoconf
sudo apt-get install libtool
sudo apt-get install zlib1g-dev

### MOSES
sudo apt-get install libboost1.55-all-dev
sudo apt-get install libbz2-dev

and here the steps to "install" all software

Note: Pay attention to the disk space:   /home is very small

tar xzf mmt-mvp-v0.2.1-makefiles.tgz (Ask Uli or Nicola for the .tgz)
pushd mmt-mvp-v0.2.1
make -f Makefile.install-moses >& Makefile.install-moses.log &

cd /mnt/mvp
create-mvp.sh 1.0

### Installing tools
pushd /mnt/mvp/res/software_code/GPERFTOOLS ; bash -x README >& README.log ; popd
pushd /mnt/mvp/res/software_code/IRSTLM ; bash -x README >& README.log ; popd

pushd /mnt/mvp/res/software_code/SPARSEHASH ; bash -x README >& README.log ; popd

pushd /mnt/mvp/res/software_code/FAST_ALIGN ; bash -x README >& README.log ; popd
pushd /mnt/mvp/res/software_code/FAST_ALIGN_enhanced ; bash -x README >& README.log ; popd
pushd /mnt/mvp/res/software_code/FAST_ALIGN_uli ; bash -x README >& README.log ; popd

pushd /mnt/mvp/res/software_code/SALM ; bash -x README >& README.log ; popd

pushd /mnt/mvp/res/software_code/MGIZA ; bash -x README >& README.log ; popd

pushd /mnt/mvp/res/software_code/MOSES ; bash -x README >& README.log ; popd
pushd /mnt/mvp/res/software_code/FILTER-PT ; bash -x README >& README.log ; popd