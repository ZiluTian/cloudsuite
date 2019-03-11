FROM zilutian/cassandra 

RUN wget -q https://github.com/brianfrankcooper/YCSB/releases/download/0.13.0/ycsb-0.13.0.tar.gz -O /ycsb-0.13.0.tar.gz \
 && buildDeps="python" \ 
 && apt-get update \ 
 && apt-get install -y $buildDeps --no-install-recommends \
 && apt-get autoremove && apt-get clean \ 
 && tar -xzf /ycsb-0.13.0.tar.gz && rm /ycsb-0.13.0.tar.gz && mv /ycsb-0.13.0 /ycsb \
 && groupadd -r cassandra && useradd -r -g cassandra cassandra \ 
 && chown cassandra:cassandra -R /ycsb/workloads

COPY setup_tables.txt /setup_tables.txt 
COPY docker-entrypoint2.sh /docker-entrypoint2.sh
RUN chmod +x /docker-entrypoint2.sh
ENTRYPOINT ["/docker-entrypoint2.sh"]

USER cassandra
