FROM zilutian/cassandra 

COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["cassandra", "-R", "-f"]

EXPOSE 7000 7001 7199 9042 9160

