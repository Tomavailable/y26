import csv
import re
import os

def clean_pos_from_tsv(file_path):
    temp_path = file_path + ".tmp"
    
    with open(file_path, 'r', encoding='utf-8') as f_in, \
         open(temp_path, 'w', encoding='utf-8', newline='') as f_out:
        
        reader = csv.reader(f_in, delimiter='\t')
        writer = csv.writer(f_out, delimiter='\t', quoting=csv.QUOTE_MINIMAL)
        
        header = next(reader)
        writer.writerow(header)
        
        try:
            trans_idx = header.index('translation')
        except ValueError:
            trans_idx = 4
            
        print(f"Cleaning column '{header[trans_idx]}' in {file_path}")
        
        count = 0
        for row in reader:
            if len(row) > trans_idx:
                original = row[trans_idx]
                cleaned = re.sub(r'\s*POS:.*$', '', original)
                if original != cleaned:
                    row[trans_idx] = cleaned
                    count += 1
            writer.writerow(row)
            
    os.replace(temp_path, file_path)
    print(f"Successfully cleaned {count} entries in {file_path}")

if __name__ == "__main__":
    # Look for the file in common locations
    paths = [
        'app/src/main/assets/ecdict_coca15000.tsv',
        '/app/src/main/assets/ecdict_coca15000.tsv',
        'src/main/assets/ecdict_coca15000.tsv'
    ]
    
    found = False
    for p in paths:
        if os.path.exists(p):
            clean_pos_from_tsv(p)
            found = True
            break
            
    if not found:
        print("Error: ecdict_coca15000.tsv not found in known paths.")
