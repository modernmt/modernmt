FROM nvidia/cuda:9.0-cudnn7-runtime-ubuntu16.04
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
RUN apt-get install -y python-dev python-pip python-requests python-psutil

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

# Install Tensorflow
RUN pip install numpy==1.14.5
RUN pip install tensorflow-gpu==1.8.0
RUN pip install tensor2tensor==1.6.3
RUN pip install --upgrade oauth2client

# Create a new user "ubuntu" and login as "ubuntu"
RUN apt-get install -y sudo && rm -rf /var/lib/apt/lists/*
RUN useradd ubuntu
RUN passwd -d ubuntu
RUN usermod -aG sudo ubuntu
WORKDIR /home/ubuntu
RUN chown ubuntu:ubuntu /home/ubuntu
USER ubuntu

# Download MMT
RUN wget -qO- https://github.com/ModernMT/MMT/releases/download/v3.0/mmt-3.0-ubuntu.tar.gz | tar xvfz -
WORKDIR /home/ubuntu/mmt