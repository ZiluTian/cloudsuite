FROM zilutian/spark

# Benchmark files
COPY movielens-als /root/movielens-als

WORKDIR /root

# Build the benchmark using sbt
RUN set -x \
	&& apt-get update -y && apt-get install -y \ 
    && (cd movielens-als && sbt package) \
    && mkdir -p /benchmarks/movielens-als \
    && mv movielens-als/target/scala-2.10/*.jar movielens-als/run_benchmark.sh /benchmarks/movielens-als \
    && rm -r movielens-als \
    && apt-get purge -y --auto-remove sbt \
    && rm -r .sbt .ivy2

COPY files /root/

ENTRYPOINT ["/root/entrypoint.sh"]

