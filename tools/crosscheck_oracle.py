#!/usr/bin/env python3
"""Cross-checks the lexical oracle of the Evaluator with grep.

The Evaluator (Java) writes, for every query, the files its oracle considers relevant
(results/oracle-sets.tsv). Here the same sets are recomputed with two other tools,
perl (lowercase + accent removal) and grep (search), and the two answers are compared
file by file. If the oracle and grep agree, the oracle is not just a copy of our own logic
applied by our own code. Both tools use the same definition of "word" (letters and digits,
everything else separates words), so this checks the implementation, not the definition.

Usage:
  python3 tools/crosscheck_oracle.py [--corpus data/corpus] [--sets results/oracle-sets.tsv]
                                      [--out results/oracle-crosscheck.md]
"""
import argparse
import re
import subprocess
import tempfile
from pathlib import Path

CLAUSE = re.compile(r'"([^"]+)"|(\S+)')
NORMALIZE = r"""
use Unicode::Normalize; use File::Basename;
my $out = shift @ARGV;
binmode(STDOUT, ':utf8');
foreach my $f (@ARGV) {
  open(my $in, '<:encoding(UTF-8)', $f) or die "$f: $!";
  local $/; my $t = <$in>; close $in;
  $t = NFD($t); $t =~ s/\p{M}//g; $t = lc($t); $t =~ s/\s+/ /g;
  open(my $o, '>:encoding(UTF-8)', "$out/" . basename($f)) or die; print $o $t; close $o;
}
"""


def words(text):
    """Lowercase words of a query part, accents removed (the corpus is normalised with perl)."""
    import unicodedata
    n = "".join(c for c in unicodedata.normalize("NFD", text) if not unicodedata.combining(c)).lower()
    return [w for w in re.split(r"[^0-9a-z]+", n) if w]


def pattern(text):
    """One grep -E pattern: a word, or several words separated by non-alphanumerics (phrase)."""
    alternatives = []
    for m in CLAUSE.finditer(text):
        ws = words(m.group(1) if m.group(1) else m.group(2))
        if ws:
            alternatives.append("[^[:alnum:]]+".join(ws))
    return "(^|[^[:alnum:]])(" + "|".join(alternatives) + ")([^[:alnum:]]|$)" if alternatives else None


def grep_files(pat, directory):
    r = subprocess.run(["grep", "-rlE", "-e", pat, str(directory)], capture_output=True, text=True,
                       env={"LC_ALL": "en_US.UTF-8", "PATH": "/usr/bin:/bin:/opt/homebrew/bin:/usr/local/bin"})
    return {Path(line).stem for line in r.stdout.splitlines() if line}


def grep_names(pat, names):
    lines = [re.sub(r"[^0-9a-z]+", " ", n.lower()) for n in names]
    r = subprocess.run(["grep", "-nE", "-e", pat], input="\n".join(lines) + "\n", capture_output=True, text=True,
                       env={"LC_ALL": "en_US.UTF-8", "PATH": "/usr/bin:/bin:/opt/homebrew/bin:/usr/local/bin"})
    return {names[int(l.split(":", 1)[0]) - 1] for l in r.stdout.splitlines() if l}


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--corpus", default="data/corpus")
    ap.add_argument("--sets", default="results/oracle-sets.tsv")
    ap.add_argument("--out", default="results/oracle-crosscheck.md")
    args = ap.parse_args()

    corpus = sorted(Path(args.corpus).glob("*.txt"))
    names = [f.stem for f in corpus]
    with tempfile.TemporaryDirectory(prefix="norm_") as tmp:
        subprocess.run(["perl", "-e", NORMALIZE, tmp] + [str(f) for f in corpus], check=True)
        rows = []
        for line in Path(args.sets).read_text(encoding="utf-8").splitlines():
            query, count, joined = (line.split("\t") + ["", ""])[:3]
            oracle = set(joined.split(";")) if joined else set()
            field, text = query.split(" ", 1)
            pat = pattern(text)
            found = set() if pat is None else (grep_names(pat, names) if field == "nome" else grep_files(pat, tmp))
            rows.append((query, len(oracle), len(found), sorted(oracle - found), sorted(found - oracle)))

    out = ["| Query | Oracle (Java) | grep | Only oracle | Only grep |", "|---|---:|---:|---:|---:|"]
    for q, o, g, only_o, only_g in rows:
        out.append(f"| `{q}` | {o} | {g} | {len(only_o)} | {len(only_g)} |")
    agree = sum(1 for r in rows if not r[3] and not r[4])
    out.append(f"\nIdentical sets: {agree} of {len(rows)} queries.")
    for q, o, g, only_o, only_g in rows:
        if only_o or only_g:
            out.append(f"\n- `{q}`: only oracle {only_o[:3]}; only grep {only_g[:3]}")
    Path(args.out).write_text("\n".join(out) + "\n", encoding="utf-8")
    print("\n".join(out))


if __name__ == "__main__":
    main()
