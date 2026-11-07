#!/usr/bin/env python3
"""Render English Google Play artwork around unmodified Compose demo captures."""
import base64
from html import escape
import json
from pathlib import Path
import shutil
import struct

import cairo
import gi
gi.require_version('Rsvg', '2.0')
from gi.repository import Rsvg

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
IMAGES = REPO / 'fastlane/metadata/android/en-US/images'
CAPTIONS = json.loads((ROOT / 'captions.json').read_text())
PHONE = ['home', 'workout', 'today', 'history', 'records', 'body']
TABLET = ['home', 'workout', 'history', 'body']


def text(value, x, y, size, fill, *, bold=False, anchor='start'):
    return (f'<text x="{x}" y="{y}" font-family="Noto Sans" font-size="{size}" '
            f'font-weight="{700 if bold else 400}" fill="{fill}" text-anchor="{anchor}">'
            f'{escape(value)}</text>')


def embedded(path):
    return base64.b64encode(path.read_bytes()).decode()


def render(svg, width, height, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    surface = cairo.ImageSurface(cairo.FORMAT_RGB24, width, height)
    bounds = Rsvg.Rectangle()
    bounds.width, bounds.height = width, height
    Rsvg.Handle.new_from_data(svg.encode()).render_document(cairo.Context(surface), bounds)
    surface.write_to_png(str(destination))


def artwork(kind, page, number):
    feature = kind == 'feature'
    width, height = (1024, 500) if feature else (1080, 1920)
    dark = feature or page in ['home', 'workout', 'today', 'history']
    bg, ink, muted, glow = (('#0a2016', '#f2f8f3', '#bfd5ca', '#23583e') if dark else
                            ('#edf1e9', '#153e2b', '#526e63', '#c8ddd3'))
    title, subtitle = CAPTIONS[page]
    if feature:
        title, subtitle = 'Your training.\nClearly logged.', 'Workouts · Sessions · Personal records'
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             f'<defs><radialGradient id="glow"><stop stop-color="{glow}"/><stop offset="1" stop-color="{bg}"/></radialGradient></defs>',
             f'<rect width="{width}" height="{height}" fill="{bg}"/>',
             f'<ellipse cx="{width}" cy="{height*.68}" rx="{width*.8}" ry="{height*.65}" fill="url(#glow)"/>',
             f'<ellipse cx="{width+100}" cy="{height*.83}" rx="{width*.7}" ry="{height*.6}" fill="none" stroke="{muted}" stroke-opacity=".1"/>']
    margin = 52 if feature else 70
    parts += [f'<circle cx="{margin+6}" cy="61" r="6" fill="#6bd8a8"/>',
              text('DotShredzilla', margin+25, 70, 26 if feature else 27, ink, bold=True)]
    if not feature:
        parts.append(text('DOTSUITE', 1010, 68, 20, muted, anchor='end'))
    for line, value in enumerate(title.splitlines()):
        parts.append(text(value, margin, (185 if feature else 175)+(58 if feature else 88)*line,
                          49 if feature else 78, ink, bold=True))
    parts.append(text(subtitle, margin, 307 if feature else 325, 21 if feature else 29, muted))
    family = 'tablet' if kind == 'tablet' else 'phone'
    path = ROOT / 'screens' / f'store-{family}-{page}.png'
    pixel_width, pixel_height = struct.unpack('>II', path.read_bytes()[16:24])
    ratio = pixel_height / pixel_width
    if feature:
        iw, ih, ix, iy = 275, 275*ratio, 700, 58
    elif kind == 'tablet':
        iw, ih, ix, iy = 948, 948*ratio, 66, 473
    else:
        iw, ih, ix, iy = 1500/ratio, 1500, (1080-1500/ratio)/2, 360
    radius = 19 if feature else 28
    rotation = f'transform="rotate(-5 {ix+iw/2} {iy+ih/2})"' if feature else ''
    parts += [f'<g {rotation}>',
              f'<rect x="{ix-9}" y="{iy+20}" width="{iw+18}" height="{ih+8}" rx="{radius+9}" fill="#00190a" opacity=".11"/>',
              f'<defs><clipPath id="screen"><rect x="{ix}" y="{iy}" width="{iw}" height="{ih}" rx="{radius}"/></clipPath></defs>',
              f'<image x="{ix}" y="{iy}" width="{iw}" height="{ih}" href="data:image/png;base64,{embedded(path)}" clip-path="url(#screen)"/>',
              f'<rect x="{ix}" y="{iy}" width="{iw}" height="{ih}" rx="{radius}" fill="none" stroke="{muted}" stroke-opacity=".5" stroke-width="2"/>', '</g>']
    footer = '#a6c4b7' if dark else '#658579'
    parts.append(text('dotsuite.dev', margin, 450 if feature else 1890, 17 if feature else 19, footer))
    if not feature:
        parts.append(text(f'{number:02d} / {4 if kind == "tablet" else 6:02d}', 1010, 1890, 19, footer, anchor='end'))
    parts.append('</svg>')
    return ''.join(parts), width, height


def export(kind, page, number=1):
    svg, width, height = artwork(kind, page, number)
    vector = ROOT / 'vectors' / f'en-US-{kind}-{page}.svg'
    vector.parent.mkdir(exist_ok=True)
    vector.write_text(svg)
    group = 'phoneScreenshots' if kind == 'phone' else 'tenInchScreenshots' if kind == 'tablet' else ''
    destination = IMAGES / group / ('featureGraphic.png' if kind == 'feature' else f'{number:02d}-tablet-{page}.png' if kind == 'tablet' else f'{number:02d}-{page}.png')
    render(svg, width, height, destination)
    return {'file': str(destination.relative_to(REPO)), 'size': [width, height],
            'bytes': destination.stat().st_size, 'mode': 'RGB'}


def main():
    manifest = [export('phone', page, n) for n, page in enumerate(PHONE, 1)]
    manifest.append(export('feature', 'home'))
    for n, page in enumerate(TABLET, 1):
        record = export('tablet', page, n)
        manifest.append(record)
        source = REPO / record['file']
        seven = IMAGES / 'sevenInchScreenshots' / source.name
        seven.parent.mkdir(exist_ok=True)
        shutil.copy2(source, seven)
        manifest.append({**record, 'file': str(seven.relative_to(REPO))})
    icon = IMAGES / 'icon.png'
    manifest.append({'file': str(icon.relative_to(REPO)), 'size': [512, 512], 'bytes': icon.stat().st_size, 'mode': 'RGB'})
    (ROOT/'export-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    sheet = ['<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="1120">',
             '<rect width="1800" height="1120" fill="#0a2016"/>',
             text('DotShredzilla · English store artwork',24,40,25,'#edf1e9',bold=True)]
    for n,page in enumerate(PHONE,1):
        path=IMAGES/'phoneScreenshots'/f'{n:02d}-{page}.png'
        sheet.append(f'<image x="{24+(n-1)*296}" y="65" width="272" height="484" href="data:image/png;base64,{embedded(path)}"/>')
    sheet.append(text('Feature graphic',24,598,25,'#edf1e9',bold=True))
    sheet.append(f'<image x="24" y="625" width="922" height="450" href="data:image/png;base64,{embedded(IMAGES/"featureGraphic.png")}"/>')
    sheet.append(text('6 phone screenshots',1010,710,32,'#edf1e9',bold=True))
    sheet.append(text('4 responsive tablet screenshots',1010,775,32,'#edf1e9',bold=True))
    sheet.append(text('New launcher icon · 512 × 512',1010,840,30,'#bfd5ca'))
    sheet.append(text('Actual Compose screens · Demo data',1010,905,27,'#bfd5ca'))
    sheet.append('</svg>')
    render(''.join(sheet),1800,1120,ROOT/'contact-sheet.png')
    print(f'Exported {len(manifest)} English RGB files (12 distinct images, including icon).')


if __name__ == '__main__':
    main()
