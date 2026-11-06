from pathlib import Path
import hashlib, io, json, urllib.request, zipfile
root = Path(__file__).resolve().parents[1]
for item in json.loads((root / 'dependencies.json').read_text(encoding='utf-8')):
    target = root / item['path']
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == item['sha256']:
        print('OK', item['path']); continue
    if 'url' in item:
        request = urllib.request.Request(item['url'], headers={'User-Agent': 'MinecraftModsOrganizer/1.0'})
        with urllib.request.urlopen(request, timeout=60) as response: data = response.read()
    else:
        with zipfile.ZipFile(root / item['from']) as archive: data = archive.read(item['member'])
    if hashlib.sha256(data).hexdigest() != item['sha256']: raise RuntimeError('SHA-256 mismatch: ' + item['path'])
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    print('Fetched', item['path'])
