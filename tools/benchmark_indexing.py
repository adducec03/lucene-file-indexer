#!/usr/bin/env python3
"""Measures how the indexing time grows with the number of files.

For every size N the script builds a subset of N files (symbolic links, chosen with a
fixed random seed so the subset is always the same and mixes languages and topics),
runs the real indexer (`./mvnw exec:java ... index`) a few times and records the time
printed by the program ("Indexed X files (Y skipped) in Z ms"), the size of the text
and the size of the index on disk.
Each run starts a new JVM, so the times include no warm-up (as in the report).

Output: a CSV file and a Markdown table with minimum, median and mean time.

Usage:
  python3 tools/benchmark_indexing.py --corpus data/corpus --sizes 200 500 1000 2000 all
Run it when the machine is idle: other programs change the measured times.
"""
import argparse
import csv
import random
import re
import shutil
import statistics
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LINE = re.compile(r"Indexed (\d+) files \((\d+) skipped\) in (\d+) ms")


def dir_size(path):
    return sum(f.stat().st_size for f in Path(path).rglob("*") if f.is_file())


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--corpus", default="data/corpus")
    ap.add_argument("--sizes", nargs="+", default=["200", "500", "1000", "2000", "all"])
    ap.add_argument("--repeats", type=int, default=5)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--csv", default="results/indexing-benchmark.csv")
    ap.add_argument("--md", default="results/indexing-benchmark.md")
    args = ap.parse_args()

    files = sorted(Path(args.corpus).resolve().glob("*.txt"))
    if not files:
        raise SystemExit(f"No .txt files in {args.corpus}")
    random.Random(args.seed).shuffle(files)

    print("compiling...", flush=True)
    subprocess.run([str(ROOT / "mvnw"), "-q", "compile"], cwd=ROOT, check=True)

    rows = []
    for size in args.sizes:
        n = len(files) if size == "all" else min(int(size), len(files))
        subset = files[:n]
        text_bytes = sum(f.stat().st_size for f in subset)
        times = []
        index_bytes = 0
        tmp = Path(tempfile.mkdtemp(prefix="bench_"))
        try:
            docs = tmp / "docs"
            docs.mkdir()
            for f in subset:
                (docs / f.name).symlink_to(f)
            for r in range(args.repeats):
                idx = tmp / f"index{r}"
                proc = subprocess.run(
                    [str(ROOT / "mvnw"), "-q", "exec:java", f"-Dexec.args=index {docs} {idx}"],
                    cwd=ROOT, capture_output=True, text=True)
                m = LINE.search(proc.stdout + proc.stderr)
                if not m:
                    raise SystemExit("unexpected output:\n" + proc.stdout + proc.stderr)
                indexed, skipped, ms = map(int, m.groups())
                times.append(ms)
                index_bytes = dir_size(idx)
                shutil.rmtree(idx)
            print(f"{n:6d} files  {text_bytes/1e6:7.1f} MB  times(ms) {times}", flush=True)
            rows.append({
                "files": indexed, "skipped": skipped, "text_mb": round(text_bytes / 1e6, 2),
                "index_mb": round(index_bytes / 1e6, 2), "min_ms": min(times),
                "median_ms": int(statistics.median(times)), "mean_ms": int(statistics.mean(times)),
                "ms_per_file": round(statistics.median(times) / indexed, 2),
                "runs": " ".join(map(str, times)),
            })
        finally:
            shutil.rmtree(tmp, ignore_errors=True)

    for p in (args.csv, args.md):
        (ROOT / p).parent.mkdir(parents=True, exist_ok=True)
    with (ROOT / args.csv).open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        w.writerows(rows)
    with (ROOT / args.md).open("w", encoding="utf-8") as f:
        f.write("| Files | Text (MB) | Index (MB) | Min (ms) | Median (ms) | Mean (ms) | ms/file |\n")
        f.write("|---:|---:|---:|---:|---:|---:|---:|\n")
        for r in rows:
            f.write(f"| {r['files']} | {r['text_mb']} | {r['index_mb']} | {r['min_ms']} | "
                    f"{r['median_ms']} | {r['mean_ms']} | {r['ms_per_file']} |\n")
    print(f"saved {args.csv} and {args.md}")


if __name__ == "__main__":
    main()
