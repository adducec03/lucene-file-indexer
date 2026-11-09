#!/usr/bin/env bash
# Starts the interactive search on an existing index.
# Usage: scripts/search.sh [indexDir]
# Default: indexDir=index
# Note: paths containing spaces are not supported.
set -euo pipefail
cd "$(dirname "$0")/.."

INDEX_DIR="${1:-index}"

./mvnw -q exec:java -Dexec.args="search $INDEX_DIR"
