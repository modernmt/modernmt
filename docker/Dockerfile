# ===================================================================================================
# STAGE: Base stage with CUDA/cuDNN libraries, Java and python
# ===================================================================================================
FROM nvidia/cuda:10.1-cudnn7-runtime-ubuntu18.04 AS modernmt-base

ARG MMT_VERSION
RUN : "${MMT_VERSION:?Build argument needs to be set and non-empty.}"

RUN apt-get update && apt-get install -y --no-install-recommends locales gcc g++ python3 python3-dev python3-setuptools python3-wheel python3-pip openjdk-8-jdk curl && \
    curl https://raw.githubusercontent.com/modernmt/modernmt/$MMT_VERSION/requirements.txt > /tmp/requirements.txt && pip3 install -r /tmp/requirements.txt && \
    locale-gen --purge en_US.UTF-8 && update-locale LANG=en_US.UTF-8 && echo "LANG=en_US.UTF-8" >> /etc/environment && \
    apt-get clean && rm -rf /root/.cache/pip/ /tmp/* /var/lib/apt/lists/* /var/tmp/*

ENV LD_LIBRARY_PATH /usr/local/cuda/lib64
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8
ENV LC_ALL en_US.UTF-8

# ===================================================================================================
# STAGE: Release builder
# ===================================================================================================
FROM modernmt-base AS release-builder

ARG MMT_VERSION

# Git clone repository
RUN apt-get update && apt-get install -y --no-install-recommends git libboost-all-dev cmake maven && \
    git clone https://github.com/modernmt/modernmt.git /tmp/mmt && cd /tmp/mmt && git checkout $MMT_VERSION

WORKDIR /tmp/mmt

# Compile ModernMT
RUN python3 setup.py --skip-pip && cd /tmp/mmt/src && mvn clean install && cd /tmp/mmt && \
    git log -1 --pretty=oneline > build/modernmt-commit.log

# Cleanup and pack
RUN find . -name ".?*" | xargs rm -rf &> /dev/null && rm -rf src test build/include && \
    mkdir vendor2 && mv vendor/kafka-* vendor/cassandra-* vendor2/ && rm -rf vendor && mv vendor2 vendor && \
    ldd build/bin/* build/lib/* | grep libboost_ | awk '{print $3}' | sort -u | xargs cp -t build/lib/

# ===================================================================================================
# Main image
# ===================================================================================================
FROM modernmt-base
MAINTAINER Davide Caroselli <davide@modernmt.eu>

COPY --from=release-builder /tmp/mmt /opt/mmt

ENV LD_LIBRARY_PATH ${LD_LIBRARY_PATH}:/opt/mmt/build/lib
ENV MMT_HOME /opt/mmt

# Running configuration
WORKDIR /opt/mmt
EXPOSE 8045
