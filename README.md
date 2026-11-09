# lucene-file-indexer

Homework for the Data Engineering course: index the `.txt` files of a directory
with Apache Lucene (fields: file name, file content) and query them from the console.

## Requirements
- Java 21
- Maven (via the included `./mvnw`)

## Usage
```bash
./mvnw compile
./mvnw exec:java -Dexec.args="index data/docs index"
./mvnw exec:java -Dexec.args="search index"
```

### Shortcut scripts (macOS/Linux)
```bash
index.sh [docsDir] [indexDir]   # defaults: data/docs, index
search.sh [indexDir]            # default: index
```
They wrap the `./mvnw` commands above (paths with spaces are not supported).

## Query syntax
```
nome <terms>            e.g.  nome ingegneria
contenuto <terms>       e.g.  contenuto data scientist
contenuto "<phrase>"    e.g.  contenuto "data scientist"
```

## Report (TODO)
- GitHub URL
- Analyzers and motivation
- Number of indexed files and indexing time
- Test queries
