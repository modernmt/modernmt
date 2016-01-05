## HW Requirements

Storage: 4 times the corpus size, min 10GB 

CPU: No minimum required. 
  More cores generally will give you a faster training and translation request throughput. 
  More clock speed will generally give you a faster translation for the single request.

Memory: >30GB needed to train 1B words

# Support

You can report issues on (GitHub)[https://github.com/ModernMT/MMT/issues] 

For customizations and enterprise support: davide.caroselli@translated.net

# Linux - Using MMT Binaries Release

Download from here: [https://github.com/ModernMT/MMT/releases]

** Only works on Ubuntu 14.04 **

This release was tested on a clean Ubuntu 14.04 server from Amazon AWS.
AMI: Ubuntu Server 14.04 LTS (HVM), SSD Volume Type -  ami-accff2b1

For training >100M words we suggest to use this instance: 
c3.4xlarge (30GB RAM, 16 core, circa $0.90/hour)

### Libraries that MMT requires:

```bash
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk
```

Done! go to README 

# Installing from Sources (for Private Open Source Contributors)

## Instructions to compile MMT:

Get sources from GitHub

```bash
git clone https://github.com/ModernMT/MMT.git
git checkout ?
```

Install Maven to build the packadge

```bash
sudo apt-get install maven 
```

```bash
sudo apt-get install maven 
```

Davide to continue here....


```bash
mvn 
```

### General-purpose requirements

```bash
sudo apt-get install build-essential
sudo apt-get install cmake
```

# Deprecated Stuff

## Instruction to compile IRSTLM and MOSES (Optional, binay for Ubuntu already provided)

### IRSTLM

```bash
sudo apt-get install autoconf
sudo apt-get install libtool
sudo apt-get install zlib1g-dev
```

### MOSES

```bash
sudo apt-get install libboost1.55-all-dev
sudo apt-get install libbz2-dev
```

and here the steps to "install" all software

```bash
tar xzf mmt-mvp-v0.2.1-makefiles.tgz (Ask Uli or Nicola for the .tgz)
pushd mmt-mvp-v0.2.1
make -f Makefile.install-moses >& Makefile.install-moses.log &
cd /mnt/mvp
create-mvp.sh 1.0
```

### Installing tools

```bash
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
```
