FROM ubuntu:14.04
MAINTAINER Ulrich Germann <ulrich.germann@gmail.com>

# let user specify target OS and MMT git hash at build time
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:openjdk-r/ppa
RUN add-apt-repository ppa:george-edison55/cmake-3.x
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk 
# RUN apt-get install -y build-essential cmake git maven libbz2-dev zlib1g-dev
# RUN apt-get install -y libboost1.55-all libgoogle-perftools
# RUN apt-get install -y libsparsehash-dev 
RUN apt-get install -y python-requests python-psutil 
RUN apt-get install -y wget
RUN apt-get install -y libjemalloc-dev

RUN wget -qO- https://github.com/ModernMT/MMT/releases/download/v0.14-alpha/mmt-0.14_alpha-ubuntu14_04.tar.gz | tar xvfz - 

WORKDIR /mmt/
RUN ./setup.py

ENV LD_LIBRARY_PATH=/mmt/build/lib
RUN ./mmt create en it examples/data/train -d --no-split

