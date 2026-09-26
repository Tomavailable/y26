import sys
from readmdict import MDX, MDD

def parse_mdx(mdx_path):
    print(f"Parsing MDX file: {mdx_path}")
    try:
        mdx = MDX(mdx_path)
        items = list(mdx.items())
        print(f"Total entries in MDX: {len(items)}")
        # Print first 5 entries as text sample
        for i, (key, value) in enumerate(items[:5]):
            word = key.decode('utf-8', errors='ignore')
            definition = value.decode('utf-8', errors='ignore')
            print(f"[{i+1}] Word: {word}")
            print(f"Definition snippet: {definition[:200]}...")
            print("-" * 40)
    except Exception as e:
        print(f"Error parsing MDX: {e}")

if __name__ == '__main__':
    print("MDX parser demo using readmdict library.")
