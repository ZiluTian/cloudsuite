#!/bin/bash

NO_PAGES=7000000
INDEX_FILE=$DUMP_FOLDER/enwiki-latest-pages-articles-multistream-index.txt
DUMP_FILE=$DUMP_FOLDER/enwiki-latest-pages-articles-multistream.xml.bz2

DUMPNAME=dump_$NO_PAGES

if [[ $NO_PAGES -ge `wc -l < $INDEX_FILE` ]]; then
        echo "wikipedia dump does not have $NO_PAGES pages"
        exit 1
fi

BYTE_OFFSET=`sed "${NO_PAGES}q;d" $INDEX_FILE | sed "s/:.*//"`


head -c $BYTE_OFFSET $DUMP_FILE > $DUMPNAME.xml.bz2
pbzip2 -dc $DUMPNAME.xml.bz2 > wiki_dump.xml
rm $DUMPNAME.xml.bz2
echo "</mediawiki>" >> wiki_dump.xml

