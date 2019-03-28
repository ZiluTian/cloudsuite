FROM memcached:debug

ENV memcache_mem_limit 65535
ENV memcache_num_threads 4

RUN memcached -u root -m $memcache_mem_limit -t $memcache_num_threads
