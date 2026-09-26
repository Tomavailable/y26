import zlib
import re

def extract_all_mdx_entries(mdx_path):
    print(f"Opening {mdx_path}...")
    with open(mdx_path, 'rb') as f:
        content = f.read()

    entries = {}
    # Scan all zlib blocks
    for i in range(len(content) - 2):
        if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
            try:
                decompressed = zlib.decompress(content[i:i+500000])
                if len(decompressed) > 1000:
                    text = decompressed.decode('utf-8', errors='ignore')
                    parts = text.split('\x00')
                    for j in range(0, len(parts) - 1, 2):
                        w = parts[j].strip().lower()
                        d = parts[j+1].strip() if j+1 < len(parts) else ""
                        if w and d:
                            # Clean HTML tags like <font color=orangered>40%</font>
                            # Convert to clean readable text, e.g. "蛋(40%)，产蛋(31%)"
                            clean_d = re.sub(r'<font[^>]*>', '', d)
                            clean_d = clean_d.replace('</font>', '')
                            clean_d = re.sub(r'<[^>]+>', '', clean_d)
                            clean_d = clean_d.strip()
                            if clean_d:
                                entries[w] = clean_d
            except Exception:
                pass
    print(f"Total extracted valid entries from MDX: {len(entries)}")
    return entries

if __name__ == '__main__':
    ents = extract_all_mdx_entries('/tmp/meaning_ratio.mdx')
    print("Sample 'abandon':", ents.get('abandon'))
    print("Sample 'apple':", ents.get('apple'))
