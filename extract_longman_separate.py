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

def get_english_only(node):
    if isinstance(node, str):
        return node
    elif isinstance(node, list):
        return "".join(get_english_only(x) for x in node)
    elif isinstance(node, dict):
        data_attr = node.get("data", {})
        cls = data_attr.get("class", "") if isinstance(data_attr, dict) else ""
        if cls == "ld-excn":
            return ""
        return get_english_only(node.get("content", ""))
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

def extract_pos_definitions_and_examples(node, current_pos, defs, examples):
    if isinstance(node, str):
        pass
    elif isinstance(node, list):
        for child in node:
            extract_pos_definitions_and_examples(child, current_pos, defs, examples)
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
                defs.append((current_pos[0], extracted_text))
        elif node_class == "ld-ex":
            en = get_english_only(node).strip()
            # Clean leading dashes, bullet points, and whitespace
            en = en.lstrip("–•\t ").strip()
            
            # Find nested ld-excn
            zhs = []
            def get_nested_zh(n):
                if isinstance(n, list):
                    for x in n: get_nested_zh(x)
                elif isinstance(n, dict):
                    d = n.get("data", {})
                    if isinstance(d, dict) and d.get("class") == "ld-excn":
                        zhs.append(get_text_clean(n))
                    get_nested_zh(n.get("content", []))
            get_nested_zh(node)
            zh = zhs[0].strip() if zhs else ""
            
            if en:
                examples.append((current_pos[0], en, zh))
        else:
            content = node.get("content", [])
            extract_pos_definitions_and_examples(content, current_pos, defs, examples)

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
                    words_in_order.append(w)
                    word_set.add(w.lower())
                    
    print(f"Total words to look up: {len(words_in_order)}")
    
    print("Step 2: Processing 25 Yomitan term bank files...")
    extracted_defs = {}  # word -> list of (pos, def)
    extracted_examples = {}  # word -> list of (pos, en, zh)
    
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
                    exs = []
                    extract_pos_definitions_and_examples(entry[5], current_pos, defs, exs)
                    if defs:
                        if word_lower not in extracted_defs:
                            extracted_defs[word_lower] = []
                        extracted_defs[word_lower].extend(defs)
                    if exs:
                        if word_lower not in extracted_examples:
                            extracted_examples[word_lower] = []
                        extracted_examples[word_lower].extend(exs)
        # Force garbage collection
        del data
        
    print("Step 3: Formatting and writing Chinese definitions file...")
    meanings_rows = ["word\tlongman_translation\n"]
    examples_rows = ["word\tlongman_examples\n"]
    
    meaning_matched = 0
    examples_matched = 0
    
    for word in words_in_order:
        word_lower = word.lower()
        
        # 1. Format Chinese definitions
        longman_defs = extracted_defs.get(word_lower)
        formatted_translation = ""
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
            
            pos_strings = []
            for pos_key in sorted(pos_groups.keys(), key=lambda x: x if x else "zzz"):
                meanings_str = "；".join(pos_groups[pos_key])
                if pos_key:
                    pos_strings.append(f"{pos_key}{meanings_str}")
                else:
                    pos_strings.append(meanings_str)
                    
            formatted_translation = "\\n".join(pos_strings)
            meaning_matched += 1
            
        meanings_rows.append(f"{word}\t{formatted_translation}\n")
        
        # 2. Format Example Sentences
        longman_exs = extracted_examples.get(word_lower)
        formatted_examples = ""
        if longman_exs:
            # Group examples by POS
            pos_exs_groups = {}
            for pos, en, zh in longman_exs:
                pos_key = pos if pos else ""
                if pos_key not in pos_exs_groups:
                    pos_exs_groups[pos_key] = []
                # Avoid exact duplicate examples
                if (en, zh) not in pos_exs_groups[pos_key]:
                    pos_exs_groups[pos_key].append((en, zh))
            
            # Select examples: one per POS
            selected_examples = []  # list of (pos, en, zh)
            used_exs_set = set()
            
            all_pos = sorted(pos_exs_groups.keys(), key=lambda x: x if x else "zzz")
            first_pos = all_pos[0] if all_pos else ""
            
            # First pass: try to get exactly one example for each POS
            for pos_key in all_pos:
                group_list = pos_exs_groups[pos_key]
                if group_list:
                    en, zh = group_list[0]
                    selected_examples.append((pos_key, en, zh))
                    used_exs_set.add((en, zh))
            
            # Second pass: if total examples < 3 and the first POS has more examples, fill up to 3 examples
            if len(selected_examples) < 3 and first_pos and pos_exs_groups.get(first_pos):
                for en, zh in pos_exs_groups[first_pos]:
                    if (en, zh) not in used_exs_set:
                        selected_examples.append((first_pos, en, zh))
                        used_exs_set.add((en, zh))
                        if len(selected_examples) >= 3:
                            break
                            
            # Limit to at most 3 examples in total
            selected_examples = selected_examples[:3]
            
            # Format according to:
            # pos
            # English example
            # Chinese translation
            ex_strings = []
            for pos_key, en, zh in selected_examples:
                pos_label = pos_key if pos_key else ""
                ex_strings.append(f"{pos_label}\\n{en}\\n{zh}")
                
            formatted_examples = "\\n".join(ex_strings)
            examples_matched += 1
            
        examples_rows.append(f"{word}\t{formatted_examples}\n")
        
    print(f"Matched definitions: {meaning_matched} / {len(words_in_order)}")
    print(f"Matched examples: {examples_matched} / {len(words_in_order)}")
    
    out_meanings_path = "app/src/main/assets/longman_meanings_15000.tsv"
    out_examples_path = "app/src/main/assets/longman_examples_15000.tsv"
    
    print(f"Writing meanings to {out_meanings_path}...")
    with open(out_meanings_path, "w", encoding="utf-8") as f:
        f.writelines(meanings_rows)
        
    print(f"Writing examples to {out_examples_path}...")
    with open(out_examples_path, "w", encoding="utf-8") as f:
        f.writelines(examples_rows)
        
    print("Done! Here are some sample entries from meanings:")
    print("--------------------------------------------------")
    for r in meanings_rows[1:11]:
        print(r.strip())
        
    print("\nHere are some sample entries from examples:")
    print("--------------------------------------------------")
    for r in examples_rows[1:11]:
        print(r.strip())

if __name__ == "__main__":
    main()
