import csv
import re
import os

def clean_pos_from_tsv(file_path):
    temp_path = file_path + ".tmp"
    
    with open(file_path, 'r', encoding='utf-8') as f_in, \
         open(temp_path, 'w', encoding='utf-8', newline='') as f_out:
        
        # Read the file as TSV
        reader = csv.reader(f_in, delimiter='\t')
        writer = csv.writer(f_out, delimiter='\t', quoting=csv.QUOTE_MINIMAL)
        
        # Read header
        header = next(reader)
        writer.writerow(header)
        
        # Find the translation column index (usually the 5th column, index 4)
        # Based on previous output: word, phonetic_uk, phonetic_us, definition, translation...
        # Let's dynamically find it if possible, otherwise use index 4
        try:
            trans_idx = header.index('translation')
        except ValueError:
            trans_idx = 4 # Fallback
            
        print(f"Cleaning POS data from column '{header[trans_idx]}' (index {trans_idx})...")
        
        count = 0
        for row in reader:
            if len(row) > trans_idx:
                original = row[trans_idx]
                # Regex to remove "POS: ..." and any trailing space before it
                # Matches "POS:" until the end of the string
                cleaned = re.sub(r'\s*POS:.*$', '', original)
                if original != cleaned:
                    row[trans_idx] = cleaned
                    count += 1
            writer.writerow(row)
            
    # Replace the original file with the cleaned one
    os.replace(temp_path, file_path)
    print(f"Successfully cleaned {count} entries.")

if __name__ == "__main__":
    target_file = 'app/src/main/assets/ecdict_coca15000.tsv'
    if os.path.exists(target_file):
        clean_pos_from_tsv(target_file)
    else:
        print(f"Error: {target_file} not found.")
