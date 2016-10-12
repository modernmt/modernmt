FROM ubuntu:14.04
MAINTAINER Ulrich Germann <ulrich.germann@gmail.com>

# let user specify target OS and MMT git hash at build time
ARG MMT_CHECKOUT=master
ENV MMT_CHECKOUT=$MMT_CHECKOUT

RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:openjdk-r/ppa
RUN add-apt-repository ppa:george-edison55/cmake-3.x
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk 
RUN apt-get install -y build-essential cmake git maven libbz2-dev zlib1g-dev
RUN apt-get install -y libboost1.55-all-dev libgoogle-perftools-dev
RUN apt-get install -y libsparsehash-dev 
RUN apt-get install -y python-requests python-psutil 

RUN git clone http://github.com/modernmt/MMT -b $MMT_CHECKOUT

WORKDIR /MMT/vendor
RUN git submodule init
RUN git submodule update
RUN make clean
RUN make res 
RUN make -j
RUN make install

WORKDIR /MMT/src
RUN mvn clean install

RUN java -version
# RUN echo 2 |  update-alternatives --config java

WORKDIR /MMT/
RUN ./mmt create en it examples/data/train -d --no-split

