import zlib

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 10000:
                # Print hex and ascii of first 200 bytes
                print("Bytes sample:", decompressed[:200])
                break
        except Exception:
            pass
