FROM zilutian/java-openjdk8

ENV CASSANDRA_VERSION 3.11.4

RUN export DEBIAN_FRONTEND=noninteractive\
    && apt-get update \
    && apt-get -y install wget --no-install-recommends \
    &&  wget http://archive.apache.org/dist/cassandra/$CASSANDRA_VERSION/apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz \
    && tar -xvzf apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz \
    && rm apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz \ 
    && rm -rf /var/lib/apt/lists/*

RUN echo "Add limits settings ...\n"\
    && echo "* soft nofile 1000000" >> /etc/security/limits.conf \
    && echo "* hard nofile 1000000" >> /etc/security/limits.conf

ENV CASSANDRA_HOME apache-cassandra-$CASSANDRA_VERSION
ENV CASSANDRA_CONFIG $CASSANDRA_HOME/conf
ENV PATH $PATH:$CASSANDRA_HOME/bin

