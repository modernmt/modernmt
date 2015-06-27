# MMT 0.9 - ALPHA RELEASE 

## About MMT
MMT is a context-aware, incremental and general purpose Machine Translation technology.

MMT goal is to make MT easy to adopt and scale.

With MMT you don't need anymore to train multiple custom engines, you can push all your data to a single engine that will automatically and in real-time adapt to the context you provide.
MMT aims to deliver the quality of a custom engine and the low sparsity of your all data combined.

You can find more information on: http://www.modermmt.eu

## About this Release
This application is the binary version of MMT (open source distribution expected by 2016). 

This MMT release will allow you to create an MT engine, available via a REST API, given your training data (folder with line aligned text files)
Ex. domain1.en domain1.it domain2.en domain2.it 
In general:
<domain-id>.<2 letters iso lang code|5 letters RFC3066>

Note: domain-id must be [a-zA-Z0-9] only without spaces.

## Current Limitations

- This alpha only support of 1 engine live at a time. You will need to move the old engine to create a new one.
- It only support latin languages.
- context and query needs to be tokenized before input.
- incremental training not implemented.

## Requirements

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

### Usage
 
#### Create and Start and engine
./create-engine -i ./example/train-data/ -s en -t it 

Use --debug for not deleting temp files in engines/temp/



#### Translate via API
http://localhost:8000/?text=party&context=President

#### Translate via command line
./translate "<text to translate>" "<context>"

#### To Stop an engine
./server stop

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