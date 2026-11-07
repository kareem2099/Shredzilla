#!/usr/bin/env python3
"""Create a local gallery of the final exported PNGs; never expose repository secrets."""
from html import escape
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
OUT = ROOT / 'preview'
ASSETS = OUT / 'assets'
ASSETS.mkdir(parents=True, exist_ok=True)
manifest = json.loads((ROOT / 'export-manifest.json').read_text())
sections = []
for lang in ['en-US']:
    cards = []
    for entry in manifest:
        path = Path(entry['file'])
        if path.parts[3] != lang:
            continue
        if path.parent.name == 'sevenInchScreenshots':
            continue
        name = lang + '-' + path.parent.name + '-' + path.name
        shutil.copy2(REPO / path, ASSETS / name)
        cards.append(f'<a href="assets/{escape(name)}"><img src="assets/{escape(name)}" alt="{escape(path.stem)}"><span>{escape(path.stem)}</span></a>')
    sections.append(f'<section><h2>{lang}</h2><div class="grid">{"".join(cards)}</div></section>')
(OUT / 'index.html').write_text('''<!doctype html><html><head><meta charset="utf-8"><title>DotShredzilla store artwork</title><style>
*{box-sizing:border-box}body{margin:0;padding:32px;background:#0a2016;color:#edf1e9;font:18px sans-serif}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:20px}section{margin:30px 0 50px}a{color:#d7f5e5;text-decoration:none}img{display:block;width:100%;max-height:430px;object-fit:contain;object-position:top;background:#173b3b;border-radius:8px}span{display:block;margin:10px 0}h1{font-size:28px}h2{font-size:20px}</style></head><body><h1>DotShredzilla · final store artwork</h1>'''+''.join(sections)+'</body></html>')
print('Gallery ready:', OUT)
