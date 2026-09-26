import os
import json
import re

def get_text_clean(node):
    if isinstance(node, str):
        return node
    elif isinstance(node, list):
        return "".join(get_text_clean(x) for x in node)
    elif isinstance(node, dict):
        return get_text_clean(node.get("content", ""))
    return ""

def clean_pos(pos_text):
    pos = pos_text.strip().lower()
    if "noun" in pos: return "n."
    if "verb" in pos: return "v."
    if "adjective" in pos or "adj" in pos: return "adj."
    if "adverb" in pos or "adv" in pos: return "adv."
    if "preposition" in pos or "prep" in pos: return "prep."
    if "conjunction" in pos or "conj" in pos: return "conj."
    if "pronoun" in pos or "pron" in pos: return "pron."
    if "determiner" in pos: return "det."
    if "number" in pos: return "num."
    return pos

def clean_chinese_def(text):
    text = re.sub(r'\[.*?\]', '', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def extract_pos_definitions(node, current_pos, results):
    if isinstance(node, str):
        pass
    elif isinstance(node, list):
        for child in node:
            extract_pos_definitions(child, current_pos, results)
    elif isinstance(node, dict):
        data = node.get("data", {})
        node_class = data.get("class", "") if isinstance(data, dict) else ""
        lang = node.get("lang", "")
        
        if node_class == "ld-pos":
            pos_text = get_text_clean(node.get("content", ""))
            current_pos[0] = clean_pos(pos_text)
        elif node_class == "ld-defcn" or lang == "zh":
            content = node.get("content", [])
            extracted_text = get_text_clean(content).strip()
            extracted_text = clean_chinese_def(extracted_text)
            if extracted_text:
                results.append((current_pos[0], extracted_text))
        else:
            content = node.get("content", [])
            extract_pos_definitions(content, current_pos, results)

def main():
    print("Step 1: Reading original 15000 wordlist...")
    orig_tsv_path = "app/src/main/assets/ecdict_coca15000.tsv"
    words_in_order = []
    word_set = set()
    
    with open(orig_tsv_path, "r", encoding="utf-8") as f:
        header = f.readline()
        for line in f:
            parts = line.split("\t")
            if parts:
                w = parts[0].strip()
                if w:
                    words_in_order.append(parts)
                    word_set.add(w.lower())
                    
    print(f"Total words to look up: {len(words_in_order)}")
    
    print("Step 2: Processing 25 Yomitan term bank files...")
    extracted_dict = {} # word -> list of (pos, def)
    
    for i in range(1, 26):
        path = f"/tmp/ldoce5_extracted/term_bank_{i}.json"
        if not os.path.exists(path):
            continue
        print(f"  Parsing {path}...")
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
            for entry in data:
                word = entry[0]
                if not word:
                    continue
                word_lower = word.lower()
                if word_lower in word_set:
                    current_pos = [None]
                    defs = []
                    extract_pos_definitions(entry[5], current_pos, defs)
                    if defs:
                        if word_lower not in extracted_dict:
                            extracted_dict[word_lower] = []
                        extracted_dict[word_lower].extend(defs)
        # Force garbage collection
        del data
        
    print(f"Step 3: Formatting and replacing definitions...")
    replaced_count = 0
    
    new_rows = [header]
    
    for row in words_in_order:
        word = row[0]
        word_lower = word.lower()
        
        longman_defs = extracted_dict.get(word_lower)
        if longman_defs:
            # Group by POS
            pos_groups = {}
            for pos, d in longman_defs:
                pos_key = pos if pos else ""
                if pos_key not in pos_groups:
                    pos_groups[pos_key] = []
                # Avoid exact duplicate meanings within the same POS
                if d not in pos_groups[pos_key]:
                    pos_groups[pos_key].append(d)
            
            # Format as POS. Meaning1；Meaning2
            pos_strings = []
            # Sort POS keys to keep consistent order if possible
            for pos_key in sorted(pos_groups.keys(), key=lambda x: x if x else "zzz"):
                meanings_str = "；".join(pos_groups[pos_key])
                if pos_key:
                    pos_strings.append(f"{pos_key} {meanings_str}")
                else:
                    pos_strings.append(meanings_str)
                    
            # Join groups with literal '\n'
            formatted_translation = "\\n".join(pos_strings)
            row[4] = formatted_translation
            replaced_count += 1
            
        new_rows.append("\t".join(row))
        
    print(f"Successfully matched and replaced: {replaced_count} / {len(words_in_order)}")
    
    out_path = "app/src/main/assets/ecdict_coca15000.tsv"
    print(f"Step 4: Writing output to {out_path}...")
    with open(out_path, "w", encoding="utf-8") as f:
        f.writelines(new_rows)
        
    print("Done! Here are some sample entries:")
    print("--------------------------------------------------")
    samples = 0
    for row in words_in_order:
        word = row[0]
        word_lower = word.lower()
        if word_lower in extracted_dict:
            print(f"Word: {word}")
            print(f"Translation: {row[4]}")
            print()
            samples += 1
            if samples >= 10:
                break

if __name__ == "__main__":
    main()
