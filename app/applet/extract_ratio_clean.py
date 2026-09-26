import zlib
import re

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

ratio_dict = {}
for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 1000:
                text = decompressed.decode('utf-8', errors='ignore')
                # Let's find null-separated pairs in the decompressed record block
                # Record blocks in MDX are typically: length or null-separated text.
                # Let's split by \x00 or double null bytes
                chunks = text.split('\x00')
                for k in range(len(chunks) - 1):
                    w = chunks[k].strip()
                    d = chunks[k+1].strip()
                    if w and d and re.match(r'^[a-zA-Z\-\s]{1,25}$', w) and ('%' in d or '（' in d or ')' in d):
                        clean_d = re.sub(r'<[^>]+>', '', d).strip()
                        ratio_dict[w.lower()] = clean_d
        except Exception:
            pass

print(f"Extracted {len(ratio_dict)} entries.")
samples = list(ratio_dict.items())[:10]
for w, d in samples:
    print(f"{w} -> {d}")
