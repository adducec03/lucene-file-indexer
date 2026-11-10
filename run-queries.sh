#!/usr/bin/env bash
# Runs every query of a file against the index and writes a Markdown report.
# Usage: ./run-queries.sh [queriesFile] [indexDir] [outputFile]
# Defaults: queries.txt, index, results/query-results.md
# The queries file has one query per line; a line starting with '#' is the
# description of the next query; blank lines are ignored.
# Note: paths containing spaces are not supported.
set -euo pipefail
cd "$(dirname "$0")"

QUERIES_FILE="${1:-queries.txt}"
INDEX_DIR="${2:-index}"
OUT_FILE="${3:-results/query-results.md}"

[ -f "$QUERIES_FILE" ] || { echo "Queries file not found: $QUERIES_FILE" >&2; exit 1; }
[ -d "$INDEX_DIR" ] || { echo "Index not found: $INDEX_DIR (build it with ./index.sh)" >&2; exit 1; }

./mvnw -q compile
mkdir -p "$(dirname "$OUT_FILE")"

DETAILS="$(mktemp)"
TABLE="$(mktemp)"
trap 'rm -f "$DETAILS" "$TABLE"' EXIT

# Removes the Java/Lucene startup warnings, the welcome banner and the prompts.
clean() {
    grep -v -E 'WARNING|AVVERT|VectorizationProvider' \
        | awk 'f{print} /^Shows the top/{f=1}' \
        | sed -e 's/^> //' -e '/^>*$/d'
}

n=0
desc=""
while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
        "# "*) desc="${line#\# }"; continue ;;
        "#"*|"") continue ;;
    esac

    n=$((n + 1))
    query="$line"
    echo "[$n] $query"
    output="$(printf '%s\nexit\n' "$query" | ./mvnw -q exec:java -Dexec.args="search $INDEX_DIR" 2>&1 | clean || true)"

    if echo "$output" | grep -q '^Error:'; then
        results="error"
    elif echo "$output" | grep -q 'no useful term'; then
        results="no useful terms"
    else
        results="$(echo "$output" | sed -n 's/^total number of results: //p' | head -1)"
        results="${results:-?}"
    fi
    top="$(echo "$output" | sed -n 's/^name: \([^;]*\);.*/\1/p' | head -1)"
    top="${top:--}"

    echo "| $n | \`$query\` | ${desc:--} | $results | $top |" >> "$TABLE"
    {
        echo "### $n. \`$query\`"
        echo
        [ -n "$desc" ] && { echo "*$desc*"; echo; }
        echo '```'
        echo "$output"
        echo '```'
        echo
    } >> "$DETAILS"
    desc=""
done < "$QUERIES_FILE"

{
    echo "# Query results"
    echo
    echo "- Generated: $(date '+%Y-%m-%d %H:%M')"
    echo "- Index: \`$INDEX_DIR\`"
    echo "- Queries file: \`$QUERIES_FILE\` ($n queries)"
    echo
    echo "## Summary"
    echo
    echo "| # | Query | Description | Results | Top result |"
    echo "|---|---|---|---|---|"
    cat "$TABLE"
    echo
    echo "## Details"
    echo
    cat "$DETAILS"
} > "$OUT_FILE"

echo "Saved $n queries to $OUT_FILE"
