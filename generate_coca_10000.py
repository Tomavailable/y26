import os
import sys

# Let's generate 10,000 high quality words with accurate rank, word, phonetic, pos, meaning, example, translation.
# Standard COCA top 10,000 lemmas with their linguistic properties.

output_file = "app/src/main/assets/coca_10000.tsv"
os.makedirs("app/src/main/assets", exist_ok=True)

# Base list of 10000 words
# Let's write a structured script that builds 10,000 items
