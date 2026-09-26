import zlib
import re

def parse_mdx_entries():
    with open('/tmp/meaning_ratio.mdx', 'rb') as f:
        content = f.read()

    entries = {}
    for i in range(len(content) - 2):
        if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
            try:
                decompressed = zlib.decompress(content[i:i+500000])
                if len(decompressed) > 1000:
                    text = decompressed.decode('utf-8', errors='ignore')
                    # MDX record blocks typically contain null-separated or tab-separated entries
                    # Let's inspect parts
                    parts = text.split('\x00')
                    for j in range(0, len(parts) - 1, 2):
                        w = parts[j].strip()
                        d = parts[j+1].strip() if j+1 < len(parts) else ""
                        if w:
                            entries[w.lower()] = d
            except Exception:
                pass
    print(f"Extracted {len(entries)} entries from MDX.")
    # Print 5 sample entries
    samples = list(entries.items())[:5]
    for w, d in samples:
        print(f"Word: {w} -> Def: {d[:100]}")

parse_mdx_entries()
