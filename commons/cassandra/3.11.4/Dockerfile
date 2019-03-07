FROM zilutian/java-openjdk8

ENV CASSANDRA_VERSION 3.11.4

ENV CASSANDRA_CONFIG /etc/cassandra

RUN export DEBIAN_FRONTEND=noninteractive\
    && apt-get update \
    && apt-get -y install wget \
    &&  wget http://archive.apache.org/dist/cassandra/$CASSANDRA_VERSION/apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz \
    && tar -xvzf apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz \
    && rm -rf /var/lib/apt/lists/*

RUN echo "Add limits settings ...\n"\
    && echo "* soft nofile 1000000" >> /etc/security/limits.conf \
    && echo "* hard nofile 1000000" >> /etc/security/limits.conf
