import os
import sqlite3

def main():
    db_path = "/tmp/ecdict_sqlite_extracted/stardict.db"
    tsv_path = "app/src/main/assets/ecdict_coca15000.tsv"
    
    if not os.path.exists(db_path):
        print(f"Error: Database {db_path} does not exist.")
        return
    if not os.path.exists(tsv_path):
        print(f"Error: TSV file {tsv_path} does not exist.")
        return
        
    print("Connecting to SQLite database...")
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    print("Reading TSV rows...")
    with open(tsv_path, "r", encoding="utf-8") as f:
        header = f.readline()
        rows = []
        for line in f:
            parts = line.strip("\r\n").split("\t")
            if len(parts) >= 5:
                rows.append(parts)
            else:
                print(f"Skipping malformed row: {parts}")
        
    print(f"Loaded {len(rows)} rows from TSV.")
    
    restored_count = 0
    new_rows = [header]
    
    for row in rows:
        word = row[0]
        # Query translation from sqlite stardict where word matches
        cursor.execute("SELECT translation FROM stardict WHERE word = ? LIMIT 1", (word,))
        res = cursor.fetchone()
        if res and res[0]:
            orig_translation = res[0]
            # ECDICT stores multiple meanings with newline.
            # In TSV, we must escape newlines as \\n
            orig_translation = orig_translation.replace("\n", "\\n").replace("\r", "")
            row[4] = orig_translation
            restored_count += 1
        
        # Ensure the row has exactly 8 columns by padding with empty strings if needed
        while len(row) < 8:
            row.append("")
        new_rows.append("\t".join(row) + "\n")
        
    print(f"Restored {restored_count} / {len(rows)} word translations to their original ECDICT values.")
    
    print(f"Writing output to {tsv_path}...")
    with open(tsv_path, "w", encoding="utf-8") as f:
        f.writelines(new_rows)
        
    conn.close()
    print("Restore complete!")

if __name__ == "__main__":
    main()
