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
                # Try decoding as utf-8 or utf-16le
                for enc in ['utf-8', 'utf-16le']:
                    try:
                        text = decompressed.decode(enc, errors='ignore')
                        # Look for Chinese characters and percentages
                        if '%' in text or '％' in text or '(<font' in text:
                            # Split by null bytes or newlines
                            parts = text.split('\x00')
                            for p in parts:
                                if '(' in p and '%' in p and len(p) < 300:
                                    # Might be a definition. Let's see if we can find associated words.
                                    pass
                            # Also check regex for word and definition
                            # Records in MDX are typically word \x00 html_definition
                            # Let's find patterns
                            matches = re.findall(r'([a-zA-Z\-\s]{2,20})\x00([^\x00]+?(?:</font>|\)[^\x00]*))', text)
                            for w, d in matches:
                                cw = w.strip().lower()
                                cd = re.sub(r'<[^>]+>', '', d).strip()
                                if cw and cd and len(cw) < 20:
                                    ratio_dict[cw] = cd
                    except Exception:
                        pass
        except Exception:
            pass

print(f"Extracted {len(ratio_dict)} ratio meanings.")
print("Sample 'egg':", ratio_dict.get('egg'))
print("Sample 'water':", ratio_dict.get('water'))
print("Sample 'run':", ratio_dict.get('run'))
