import sys
import struct
import zlib
import re

def extract_mdx(mdx_path, out_tsv_path):
    print(f"Parsing MDX: {mdx_path}")
    with open(mdx_path, 'rb') as f:
        header_size_bytes = f.read(4)
        if len(header_size_bytes) < 4:
            print("Invalid MDX file")
            return
        header_size = struct.unpack('>I', header_size_bytes)[0]
        header_bytes = f.read(header_size)
        
        # Try to parse header encoding
        header_text = header_bytes.decode('utf-16le', errors='ignore')
        print("Header text snippet:", header_text[:200])
        
        # Search for encoding in header
        encoding = 'utf-16le'
        if 'encoding="utf-8"' in header_text.lower():
            encoding = 'utf-8'
        elif 'encoding="gbk"' in header_text.lower() or 'encoding="gb2312"' in header_text.lower():
            encoding = 'gbk'
        print(f"Detected encoding: {encoding}")

        # For a robust extraction in Python without heavy external deps,
        # we can scan for compressed zlib blocks or text strings in the mdx file if it's not encrypted,
        # or use standard mdx parsing logic.
        
        f.seek(0)
        content = f.read()
        
    print(f"File size: {len(content)} bytes")

if __name__ == '__main__':
    extract_mdx("/tmp/meaning_ratio.mdx", "/tmp/extracted_ratio.tsv")
