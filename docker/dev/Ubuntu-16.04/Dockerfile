FROM ubuntu:16.04
MAINTAINER Ulrich Germann <ulrich.germann@gmail.com>

# let user specify MMT git hash at build time
ARG MMT_CHECKOUT=master
ENV MMT_CHECKOUT=$MMT_CHECKOUT

RUN apt-get update
RUN apt-get install -y software-properties-common
RUN apt-get install -y python-software-properties
RUN apt-get install -y build-essential cmake git maven libbz2-dev zlib1g-dev
RUN apt-get install -y libboost-all-dev
RUN apt-get install -y libgoogle-perftools-dev
RUN apt-get install -y libsparsehash-dev openjdk-8-jdk 
RUN apt-get install -y python-requests python-psutil 
RUN apt-get install -y libjemalloc-dev 

RUN git clone http://github.com/modernmt/MMT -b $MMT_CHECKOUT
WORKDIR /MMT/vendor
RUN git submodule init
RUN git submodule update
# RUN make clean
# RUN git submodule update
RUN make res 
RUN make -j
RUN make install
WORKDIR /MMT/src
RUN mvn clean install

WORKDIR /MMT/
RUN ./mmt create en it examples/data/train -d --no-split
