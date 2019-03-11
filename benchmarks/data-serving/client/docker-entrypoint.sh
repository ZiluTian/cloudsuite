#!/bin/bash

echo server\'s IP is $1
echo ==================

if [ ! -z "$RECORDCOUNT" ]; then
    RECORDCOUNT="-p recordcount=$RECORDCOUNT"
fi

if [ ! -z "$OPERATIONCOUNT" ]; then
    OPERATIONCOUNT="-p operationcount=$OPERATIONCOUNT"
fi

echo server\'s IP is $1
echo ==================

first_server=$(cut -d',' -f1 <<< "$1")

exit=0
while [ $exit -eq 0 ]; do
    set +e
    cqlsh -f /setup_tables.txt $first_server
    if [[ "$?" -eq 0 ]]; then
        exit=1
    else
        echo 'Cannot connect to the seed server. Trying again...'
    fi
    set -e
    sleep 5
done


/ycsb/bin/ycsb load cassandra-cql -p hosts=$1 -P /ycsb/workloads/workloada $RECORDCOUNT > /dev/null

/ycsb/bin/ycsb run cassandra-cql -p hosts=$1 -P /ycsb/workloads/workloada $OPERATIONCOUNT
