import re

print("Reading /tmp/mdx_dump.txt...")
with open('/tmp/mdx_dump.txt', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Let's see patterns in mdx_dump.txt
# For example, words followed by definitions with percentages
matches = re.findall(r'([a-zA-Z\-\s]{1,25})\x00([^\x00]{2,300})', text)
print(f"Found {len(matches)} pairs via null byte in dump.")
for w, d in matches[:10]:
    print(repr(w), "->", repr(d[:50]))
