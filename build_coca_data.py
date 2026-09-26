import os
import json
import gzip

# Let's verify writing TSV format
output_path = "app/src/main/assets/coca_10000.tsv"

# Format: rank \t word \t phonetic \t pos \t meaning \t exampleSentence \t exampleTranslation
print("Generating full COCA 10,000 dataset...")

# We will construct 10,000 real English words partitioned cleanly
