import sys
import struct
import zlib

def parse_mdx(filepath):
    print(f"Reading {filepath}...")
    with open(filepath, 'rb') as f:
        header_bytes = f.read(512)
        print("Header sample (first 100 bytes):", header_bytes[:100])

if __name__ == "__main__":
    parse_mdx("/tmp/meaning_ratio.mdx")
