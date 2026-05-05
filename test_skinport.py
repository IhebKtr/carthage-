import urllib.request
import json
import gzip

req = urllib.request.Request("https://api.skinport.com/v1/items", headers={"Accept-Encoding": "gzip, deflate"})
try:
    with urllib.request.urlopen(req) as response:
        if response.info().get('Content-Encoding') == 'gzip':
            data = gzip.decompress(response.read()).decode('utf-8')
            print("GZIP worked!")
        else:
            data = response.read().decode('utf-8')
        j = json.loads(data)
        print(len(j))
except Exception as e:
    print(e)
