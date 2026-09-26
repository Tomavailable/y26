import zlib

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

print(f"Total file size: {len(content)}")
# Search for zlib headers
for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 1000:
                print(f"Found zlib stream at offset {i}, decompressed size {len(decompressed)}")
                print("Sample text:", decompressed[:200])
        except Exception:
            pass
