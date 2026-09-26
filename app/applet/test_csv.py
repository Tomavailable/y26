import csv

def parse_line(line):
    res = []
    curr = []
    in_q = False
    i = 0
    while i < len(line):
        c = line[i]
        if c == '"' and in_q and i + 1 < len(line) and line[i+1] == '"':
            curr.append('"')
            i += 1
        elif c == '"':
            in_q = not in_q
        elif c == ',' and not in_q:
            res.append("".join(curr))
            curr = []
        else:
            curr.append(c)
        i += 1
    res.append("".join(curr))
    return res

with open("app/src/main/assets/ecdict_coca10000.csv", "r", encoding="utf-8") as f:
    lines = [l.rstrip("\r\n") for l in f.readlines()]

print("Header:", parse_line(lines[0]))
mismatch = 0
for idx, line in enumerate(lines[1:]):
    p = parse_line(line)
    if len(p) < 8:
        mismatch += 1

print("Total rows:", len(lines)-1, "Mismatches:", mismatch)
