import csv
import re
import os

input_file = 'app/src/main/assets/ecdict_coca15000.tsv'
output_file = 'app/src/main/assets/ecdict_coca15000_cleaned.tsv'

def clean_translation(text):
    if not text:
        return ""
    
    # 1. Remove POS information (everything after \nPOS:)
    # Handle both literal \n and escaped \\n depending on how it was saved
    main_part = re.split(r'\\nPOS:|\\npos:|\nPOS:|\npos:', text)[0]
    
    # 2. Remove percentage info like (58%)
    cleaned = re.sub(r'\(?\d+%\)?', '', main_part)
    
    # 3. Handle delimiters: replace Chinese and English commas/semicolons with Chinese semicolon
    # First, replace potential whitespace around delimiters
    cleaned = re.sub(r'\s*[，,；;]\s*', '；', cleaned)
    
    # 4. Clean up: remove multiple semicolons, leading/trailing semicolons
    cleaned = re.sub(r'；+', '；', cleaned)
    cleaned = cleaned.strip('；').strip()
    
    return cleaned

def process_file():
    if not os.path.exists(input_file):
        print(f"Error: {input_file} not found")
        return

    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f, delimiter='\t')
        rows = list(reader)

    updated_count = 0
    for row in rows:
        if len(row) > 4:
            original = row[4]
            cleaned = clean_translation(original)
            if original != cleaned:
                row[4] = cleaned
                updated_count += 1

    with open(output_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter='\t', quoting=csv.QUOTE_MINIMAL)
        writer.writerows(rows)
    
    # Move the cleaned file to replace the original
    os.rename(output_file, input_file)
    print(f"Successfully cleaned {updated_count} entries.")

if __name__ == "__main__":
    process_file()
