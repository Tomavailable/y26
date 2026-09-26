import urllib.request
import urllib.parse

url = "https://mdx.mdict.org/%E6%8C%89%E8%AF%8D%E5%85%B8%E8%AF%AD%E7%A7%8D%E6%9D%A5%E5%88%86%E7%B1%BB/%E8%AF%8D%E9%87%8A/%E5%8D%95%E8%AF%8D%E9%87%8A%E4%B9%89%E6%AF%94%E4%BE%8B%E8%AF%8D%E5%85%B8/%E5%8D%95%E8%AF%8D%E9%87%8A%E4%B9%89%E6%AF%94%E4%BE%8B%E8%AF%8D%E5%85%B8.mdx"
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}

req = urllib.request.Request(url, headers=headers)
print("Downloading mdx dictionary...")
try:
    with urllib.request.urlopen(req) as response, open('dict.mdx', 'wb') as out_file:
        data = response.read()
        out_file.write(data)
    print(f"Downloaded successfully, size: {len(data)} bytes")
except Exception as e:
    print(f"Error downloading: {e}")
