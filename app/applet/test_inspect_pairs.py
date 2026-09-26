import zlib
import re

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

count = 0
for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 1000:
                text = decompressed.decode('utf-8', errors='ignore')
                # Find patterns of word followed by definition
                # Usually word \x00 definition or word \t definition
                tokens = [t.strip() for t in text.split('\x00') if t.strip()]
                for t in tokens:
                    if len(t) < 50 and not '<' in t and ('(' in t or '%' in t or len(tokens) > 1):
                        pass
                # Let's print non-empty pairs
                for k in range(0, len(tokens)-1, 2):
                    w = tokens[k]
                    d = tokens[k+1] if k+1 < len(tokens) else ""
                    if len(w) < 30 and d:
                        print(f"[{w}] -> [{d[:60]}]")
                        count += 1
                        if count >= 15:
                            break
            if count >= 15:
                break
        except Exception:
            pass
