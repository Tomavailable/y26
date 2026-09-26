from extract_ratio_dict import extract_all_mdx_entries

ents = extract_all_mdx_entries('/tmp/meaning_ratio.mdx')
print("Sample keys:", list(ents.keys())[:20])
for k, v in list(ents.items()):
    if 'abandon' in k or 'apple' in k:
        print(f"Found: {repr(k)} -> {repr(v)}")
