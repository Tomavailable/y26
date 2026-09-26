import zlib

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

count = 0
for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 1000:
                text = decompressed.decode('utf-8', errors='ignore')
                if '<font' in text or '％' in text or '%' in text:
                    print("Found rich definition block snippet:")
                    print(text[text.find('<'):text.find('<')+300])
                    count += 1
                    if count >= 3:
                        break
        except Exception:
            pass
