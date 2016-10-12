FROM ubuntu:12.04
MAINTAINER Ulrich Germann <ulrich.germann@gmail.com>

# The primary purpose of this Dockerfile is to test installation with
# conda on legacy systems that do not meet the prerequisites for MMT
# installation and where the user has no root access.

# use bash, not dash
RUN rm /bin/sh && ln -s /bin/bash /bin/sh

# Install packages necessary to install miniconda
RUN apt-get update
RUN apt-get install -y wget bzip2

# for some odd reason, another round of apt-get update seeems necessary
# before we can install build-essential
RUN apt-get update
RUN apt-get install -y build-essential 

# Download Miniconda installer and install miniconda
RUN wget --no-check-certificate -q \
    https://repo.continuum.io/miniconda/Miniconda2-latest-Linux-x86_64.sh 
RUN bash ./Miniconda2-latest-Linux-x86_64.sh -b
ENV PATH=/root/miniconda2/bin:${PATH}

# set up a conda environment for mmt compilation and installation
RUN conda create --name mmt \
    -c https://conda.anaconda.org/cidermole \
    cmake gperftools python=2.7 requests jdk8 \
    apache-maven patchelf cloog git zlib boost

# activate the enviroment, install necessary packages and clone
# the MMT code repository
RUN source activate mmt \
    && conda install -f gcc \
    && pip install psutil requests \
    && git clone http://github.com/ugermann/MMT \
       -b tutorial-at-mt-marathon-2016

# we won't be able to compile without adjusting LD_LIBRARY_PATH
ENV LD_LIBRARY_PATH=/root/miniconda2/lib:${LD_LIBRARY_PATH}

# compile MMT
RUN source activate mmt \
    && cd MMT && ./util/full-recompile.sh

