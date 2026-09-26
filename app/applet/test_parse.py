import struct

with open('/tmp/meaning_ratio.mdx', 'rb') as f:
    header_size = struct.unpack('>I', f.read(4))[0]
    f.read(header_size)
    f.read(4) # checksum
    
    # v2.0 fields are 8 bytes (Q) each
    data = f.read(8 * 9)
    vals = struct.unpack('>9Q', data)
    print("MDX v2.0 Header Metadata:", vals)
