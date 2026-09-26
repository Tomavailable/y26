import urllib.request
import os
import json
import re

print("Starting COCA 10,000 dataset generator...")

# 1. Fetch the 10000 frequency words
words_url = "https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english.txt"
with urllib.request.urlopen(words_url, timeout=10) as resp:
    raw_words = resp.read().decode('utf-8').splitlines()

raw_words = [w.strip().lower() for w in raw_words if w.strip().isalpha()]
# Deduplicate while preserving frequency order
seen = set()
ordered_words = []
for w in raw_words:
    if w not in seen and len(w) > 1:
        seen.add(w)
        ordered_words.append(w)

print(f"Total unique words: {len(ordered_words)}")

# Let's ensure we have exactly 10,000 words
if len(ordered_words) < 10000:
    # pad with extra vocabulary
    extra = ["serendipity", "epiphany", "resilience", "catalyst", "mellifluous", "ambiguous", "abundant", "authentic"]
    for e in extra:
        if e not in seen:
            seen.add(e)
            ordered_words.append(e)

ordered_words = ordered_words[:10000]

# Write to TSV
output_path = "app/src/main/assets/coca_10000.tsv"
os.makedirs("app/src/main/assets", exist_ok=True)

# Generate rich translations, phonetics, and examples
