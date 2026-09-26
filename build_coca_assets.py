import os
import re

# Comprehensive COCA base frequency word list generator
# Top frequent English lemmas from Corpus of Contemporary American English

def generate_coca_dataset():
    # Load or generate word lists with meanings
    # Core high frequency roots and words
    coca_words = []
    
    # We will construct a curated list of top 10000 words with phonetics, pos, meanings
    # Let's read existing local dictionary entries to seed exact details
    local_dict_words = {}
    if os.path.exists("app/src/main/java/com/example/data/dict/LocalDictionary.kt"):
        with open("app/src/main/java/com/example/data/dict/LocalDictionary.kt", "r", encoding="utf-8") as f:
            content = f.read()
            # Match DictEntry("word", "phonetic", "pos", "meaning", "ex", "trans", "notes")
            pattern = re.compile(r'DictEntry\(\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', re.DOTALL)
            for m in pattern.finditer(content):
                w, ph, pos, mng, ex, tr = m.groups()
                local_dict_words[w.lower()] = {
                    "phonetic": ph,
                    "pos": pos,
                    "meaning": mng,
                    "example": ex,
                    "trans": tr
                }

    print(f"Loaded {len(local_dict_words)} entries from LocalDictionary")
    return local_dict_words

generate_coca_dataset()
