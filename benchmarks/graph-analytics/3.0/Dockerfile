FROM zilutian/spark

# Copy files
COPY benchmark /root/benchmark
COPY files /root/
     
WORKDIR /root

# Build the benchmark using sbt
RUN sed -i "s/EDGES_FILE/\/data\/edges\.csv/g" benchmark/src/main/scala/GraphAnalytics.scala \
    && (cd benchmark && sbt package) \
    && mkdir -p /benchmarks \
    && mv benchmark/target/scala-2.10/*.jar benchmark/run_benchmark.sh /benchmarks \
    && rm -r benchmark \
    && apt-get purge -y --auto-remove sbt \
    && rm -r .sbt .ivy2 \
    && chmod +x /root/entrypoint.sh


ENTRYPOINT ["/root/entrypoint.sh"]
