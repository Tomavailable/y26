import zlib
import re

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

entries = {}
for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 1000:
                text = decompressed.decode('utf-8', errors='ignore')
                # Find all word \x00 definition patterns or similar
                # Let's search for patterns where English words are followed by Chinese definitions
                matches = re.findall(r'([a-zA-Z\-\s]{2,25})\x00([^\x00]{2,200})', text)
                for w, d in matches:
                    clean_w = w.strip().lower()
                    clean_d = re.sub(r'<[^>]+>', '', d).strip()
                    if clean_w and clean_d and not clean_w.startswith('http'):
                        entries[clean_w] = clean_d
        except Exception:
            pass

print(f"Extracted {len(entries)} entries via regex.")
print("Sample 'abandon':", entries.get('abandon'))
print("Sample 'apple':", entries.get('apple'))
print("Sample 'table':", entries.get('table'))
