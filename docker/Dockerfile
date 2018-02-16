FROM nvidia/cuda:9.1-cudnn7-runtime-ubuntu16.04
MAINTAINER Davide Caroselli <davide@modernmt.eu>

# Install dependencies
RUN apt-get -y update

RUN apt-get install -y software-properties-common
RUN apt-get install -y wget
RUN apt-get -y update --fix-missing

RUN apt-get install -y libnuma-dev
RUN add-apt-repository -y ppa:openjdk-r/ppa
RUN apt-get -y update

RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y python-pip python-requests python-psutil

# Set ulimit
RUN echo "* soft nofile 1048576" > /etc/security/limits.conf
RUN echo "* hard nofile 1048576" >> /etc/security/limits.conf
RUN ulimit -n 1048576

# Set locales
RUN apt-get install -y locales
RUN locale-gen en_US.UTF-8
RUN dpkg-reconfigure locales
RUN localedef -i en_US -c -f UTF-8 en_US.UTF-8
RUN echo "LC_ALL=en_US.UTF-8" >> /etc/environment
RUN echo "LANG=en_US.UTF-8" >> /etc/environment

# Install PyTorch
RUN pip install http://download.pytorch.org/whl/cu91/torch-0.3.1-cp27-cp27mu-linux_x86_64.whl

# Create a new user "mmt" and login as "mmt"
RUN apt-get install -y sudo && rm -rf /var/lib/apt/lists/*
RUN useradd mmt
RUN passwd -d mmt
RUN usermod -aG sudo mmt
WORKDIR /home/mmt
RUN chown mmt:mmt /home/mmt
USER mmt

# Install MMT
RUN wget -qO- https://github.com/ModernMT/MMT/releases/download/v2.4/mmt-2.4-ubuntu.tar.gz | tar xvfz -
WORKDIR /home/mmt/mmt

# Make MMT example engine
RUN ./mmt create en it examples/data/train
