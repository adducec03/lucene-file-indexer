#!/usr/bin/env bash
# Builds the Lucene index.
# Usage: ./index.sh [docsDir] [indexDir]
# Defaults: docsDir=data/docs, indexDir=index
# Note: paths containing spaces are not supported.
set -euo pipefail
cd "$(dirname "$0")"

DOCS_DIR="${1:-data/docs}"
INDEX_DIR="${2:-index}"

./mvnw -q compile exec:java -Dexec.args="index $DOCS_DIR $INDEX_DIR"
