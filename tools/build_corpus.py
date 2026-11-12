#!/usr/bin/env python3
"""Builds the text corpus used to evaluate the Lucene indexer.

Source: Wikipedia (Italian and English) through the public MediaWiki API.
The corpus mixes two kinds of articles:
  * "topic" articles, taken from a few Wikipedia categories (one list per language);
    they give groups of documents about the same subject;
  * "random" articles, which act as background noise.
Every article is saved as <out>/<lang>_<Title>.txt (first line = title, then plain text)
and described in <manifest> (CSV: lang, title, file, source, chars, categories).
The manifest also keeps the Wikipedia categories of each article, so that a
topic-based ground truth can be built later without downloading anything again.

Only the Python standard library is needed. The script can be stopped and restarted:
files already downloaded are skipped.

Usage:
  python3 tools/build_corpus.py --out data/corpus --manifest data/corpus-manifest.csv
  python3 tools/build_corpus.py --list-only        # only checks the categories
Wikipedia text is released under CC BY-SA.
"""
import argparse
import csv
import json
import re
import threading
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

USER_AGENT = "lucene-homework/1.0 (student project; https://github.com/adducec03/lucene-file-indexer)"

# (topic, English category, Italian category)
TOPICS = [
    ("terremoti", "Earthquakes", "Terremoti"),
    ("battaglie_ww2", "Battles and operations of World War II", "Battaglie e operazioni della seconda guerra mondiale"),
    ("napoleone", "Napoleonic Wars", "Guerre napoleoniche"),
    ("calcio", "Football clubs in Italy", "Società calcistiche italiane"),
    ("teoremi", "Mathematical theorems", "Teoremi"),
    ("pittura", "Italian painters", "Pittori italiani"),
    ("chimica", "Chemical compounds", "Composti chimici"),
    ("vulcani", "Volcanoes", "Vulcani"),
    ("fiumi", "Rivers of Italy", "Fiumi d'Italia"),
    ("impero_romano", "Roman emperors", "Imperatori romani"),
    ("malattie", "Infectious diseases", "Malattie infettive"),
    ("opera", "Operas", "Opere liriche"),
    ("aerei", "Fighter aircraft", "Aerei militari della seconda guerra mondiale"),
    ("programmazione", "Programming languages", "Linguaggi di programmazione"),
]


class RateLimiter:
    """Keeps a minimum delay between requests (shared by all threads) and honours Retry-After."""

    def __init__(self, min_interval):
        self.min_interval = min_interval
        self.lock = threading.Lock()
        self.next_time = 0.0

    def wait(self):
        with self.lock:
            now = time.monotonic()
            delay = max(0.0, self.next_time - now)
            self.next_time = max(now, self.next_time) + self.min_interval
        if delay:
            time.sleep(delay)

    def penalty(self, seconds):
        with self.lock:
            self.next_time = max(self.next_time, time.monotonic() + seconds)


LIMITER = RateLimiter(0.25)


def api(lang, **params):
    params.update(action="query", format="json", maxlag=5)
    url = f"https://{lang}.wikipedia.org/w/api.php?" + urllib.parse.urlencode(params)
    for attempt in range(8):
        LIMITER.wait()
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=40) as resp:
                data = json.load(resp)
            if "error" in data and data["error"].get("code") == "maxlag":
                LIMITER.penalty(5)
                continue
            return data
        except urllib.error.HTTPError as e:
            wait = int(e.headers.get("Retry-After", "10")) + 1 if e.code in (429, 503) else 5
            LIMITER.penalty(wait)
        except Exception:
            LIMITER.penalty(3)
    return None


def slug(title):
    t = unicodedata.normalize("NFKD", title).encode("ascii", "ignore").decode()
    return re.sub(r"[^A-Za-z0-9]+", "_", t).strip("_")[:60] or "untitled"


def category_titles(lang, category, wanted, max_depth=2):
    """Collects article titles of a category, descending into sub-categories if needed."""
    found, seen_cats = [], set()
    queue = [(f"Category:{category}", 0)] if lang == "en" else [(f"Categoria:{category}", 0)]
    while queue and len(found) < wanted * 3:
        cat, depth = queue.pop(0)
        if cat in seen_cats:
            continue
        seen_cats.add(cat)
        cont = {}
        while True:
            d = api(lang, list="categorymembers", cmtitle=cat, cmlimit=500, cmtype="page|subcat", **cont)
            if not d:
                break
            for m in d["query"]["categorymembers"]:
                if m["ns"] == 0:
                    found.append(m["title"])
                elif m["ns"] == 14 and depth < max_depth:
                    queue.append((m["title"], depth + 1))
            if "continue" in d and len(found) < wanted * 3:
                cont = d["continue"]
            else:
                break
    return list(dict.fromkeys(found))


def random_titles(lang, wanted, min_length):
    found = []
    while len(found) < wanted * 2:
        d = api(lang, generator="random", grnnamespace=0, grnlimit=50, prop="info")
        if not d:
            continue
        for p in d["query"]["pages"].values():
            if p.get("length", 0) >= min_length:
                found.append(p["title"])
    return list(dict.fromkeys(found))


def fetch_article(lang, title):
    d = api(lang, titles=title, prop="extracts|categories", explaintext=1, cllimit="max", clshow="!hidden")
    if not d:
        return None
    page = next(iter(d["query"]["pages"].values()))
    text = page.get("extract", "")
    cats = [c["title"].split(":", 1)[1] for c in page.get("categories", [])]
    return page.get("title", title), text, cats


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", default="data/corpus")
    ap.add_argument("--manifest", default="data/corpus-manifest.csv")
    ap.add_argument("--per-topic", type=int, default=80, help="articles per topic and language")
    ap.add_argument("--random", type=int, default=400, help="random articles per language")
    ap.add_argument("--min-chars", type=int, default=3000, help="discard shorter articles")
    ap.add_argument("--workers", type=int, default=3)
    ap.add_argument("--list-only", action="store_true", help="only count candidate articles per topic")
    args = ap.parse_args()

    out = Path(args.out)
    manifest = Path(args.manifest)
    plan = []  # (lang, title, source)
    for lang in ("it", "en"):
        for topic, cat_en, cat_it in TOPICS:
            cat = cat_en if lang == "en" else cat_it
            titles = category_titles(lang, cat, args.per_topic)
            print(f"{lang} {topic:16s} {cat:45s} candidates: {len(titles)}", flush=True)
            plan += [(lang, t, topic) for t in titles]
    if args.list_only:
        return
    for lang in ("it", "en"):
        plan += [(lang, t, "random") for t in random_titles(lang, args.random, args.min_chars)]

    out.mkdir(parents=True, exist_ok=True)
    done = {}
    if manifest.exists():
        with manifest.open(encoding="utf-8", newline="") as f:
            for row in csv.DictReader(f):
                done[(row["lang"], row["title"])] = row
    quota = {}
    for row in done.values():
        quota[(row["lang"], row["source"])] = quota.get((row["lang"], row["source"]), 0) + 1

    new_manifest = not manifest.exists()
    mf = manifest.open("a", encoding="utf-8", newline="")
    writer = csv.writer(mf)
    if new_manifest:
        writer.writerow(["lang", "title", "file", "source", "chars", "categories"])
    used_files = {r["file"] for r in done.values()}
    lock = threading.Lock()
    saved = 0

    def work(item):
        lang, title, source = item
        with lock:
            target = args.random if source == "random" else args.per_topic
            if (lang, title) in done or quota.get((lang, source), 0) >= target:
                return None
        res = fetch_article(lang, title)
        if not res:
            return None
        real_title, text, cats = res
        if len(text) < args.min_chars:
            return None
        with lock:
            if (lang, real_title) in done or quota.get((lang, source), 0) >= target:
                return None
            name = f"{lang}_{slug(real_title)}.txt"
            n = 2
            while name in used_files:
                name = f"{lang}_{slug(real_title)}_{n}.txt"
                n += 1
            used_files.add(name)
            quota[(lang, source)] = quota.get((lang, source), 0) + 1
            done[(lang, real_title)] = {"lang": lang}
        (out / name).write_text(real_title + "\n\n" + text, encoding="utf-8")
        return [lang, real_title, name, source, len(text), ";".join(cats)]

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(work, it) for it in plan]
        for fut in as_completed(futures):
            row = fut.result()
            if row:
                with lock:
                    writer.writerow(row)
                    mf.flush()
                    saved += 1
                if saved % 100 == 0:
                    print(f"saved {saved} articles", flush=True)
    mf.close()
    print(f"done: {saved} new articles in {out}")


if __name__ == "__main__":
    main()
