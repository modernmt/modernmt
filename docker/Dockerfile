FROM ubuntu:14.04
MAINTAINER Davide Caroselli <davide.caroselli@translated.net>

# Install dependencies
RUN apt-get -y update
RUN apt-get install -y software-properties-common

RUN add-apt-repository -y ppa:openjdk-r/ppa
RUN apt-get -y update

RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y python-requests python-psutil
RUN apt-get install -y wget

# Set ulimit
RUN echo "* soft nofile 1048576" > /etc/security/limits.conf
RUN echo "* hard nofile 1048576" >> /etc/security/limits.conf
RUN ulimit -n 1048576

# Set locales
RUN locale-gen "en_US.UTF-8"
RUN dpkg-reconfigure locales
RUN update-locale LANG=en_US.UTF-8 LANGUAGE=en_US.UTF-8 LC_ALL=en_US.UTF-8

# Download and install MMT
RUN wget -qO- https://github.com/ModernMT/MMT/releases/download/v0.14.1/mmt-0.14.1-ubuntu14_04.tar.gz | tar xvfz - 
WORKDIR /mmt/
RUN ./setup.py

# Make MMT example engine
RUN ./mmt create en it examples/data/train
