with open('/tmp/mdx_dump.txt', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

count = 0
for line in lines:
    if '<font' in line or '%' in line:
        print(line.strip()[:150])
        count += 1
        if count >= 20:
            break
