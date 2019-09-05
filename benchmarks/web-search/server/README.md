Before generating a server image, please make sure you have a folder with index file and dump file. If the dump folder's name is wiki_dump, it might contain following files

```
enwiki-latest-pages-articles-multistream.xml.bz2 
enwiki-latest-pages-articles-multistream-index.txt.bz2
```

Unzip the index file 
```
bzip2 -kd enwiki-latest-pages-articles-multistream-index.txt.bz2 
```
Run generate_index.sh to generate an index wiki_dump.xml inside the dump folder according to the specifed page number

