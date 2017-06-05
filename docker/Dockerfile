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

#create a new user mmtuser and login as mmtuser
#RUN adduser mmtuser --gecos "" 
RUN useradd -p $(echo "" | openssl passwd -1 -stdin) mmtuser
RUN usermod -aG sudo mmtuser
WORKDIR /home/mmtuser
RUN chown mmtuser:mmtuser /home/mmtuser
USER mmtuser

RUN wget -qO- https://github.com/ModernMT/MMT/releases/download/v1.0.1/mmt-1.0.1-ubuntu.tar.gz | tar xvfz -
WORKDIR /home/mmtuser/mmt

# Make MMT example engine
RUN ./mmt create en it examples/data/train
