FROM ubuntu:14.04

# Use 14.04 for gcc4.8, recent version of gcc causes makefile compilation failure 

#Original maintainer: MAINTAINER Tapti Palit <tpalit@cs.stonybrook.edu>
MAINTAINER Alexandros Daglis <alexandros.daglis@epfl.ch>

RUN apt-get update -y && apt-get install -y --no-install-recommends \
      bc \
      build-essential \
      libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY files /root/
RUN set -x \
    && chmod +x /root/docker-entrypoint.sh \
    && mkdir -p /root/build \
    && cd /root/build \
    && /root/videoperf/configure \
    && make \
    && make install

RUN mkdir /output
VOLUME ["/output"]

ENTRYPOINT ["/root/docker-entrypoint.sh"]
