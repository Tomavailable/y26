import zlib

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    content = f.read()

for i in range(len(content) - 2):
    if content[i] == 0x78 and content[i+1] in (0x01, 0x9c, 0xda):
        try:
            decompressed = zlib.decompress(content[i:i+500000])
            if len(decompressed) > 50000:
                print(f"Sample block at {i}, len {len(decompressed)}")
                # Print first 500 bytes decoded
                try:
                    text = decompressed.decode('utf-8', errors='ignore')
                    print(text[:500])
                except Exception as e:
                    print("Decode error:", e)
                break
        except Exception:
            pass
