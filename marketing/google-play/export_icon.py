#!/usr/bin/env python3
"""Export the existing Android vector launcher layers as the 512 px Play icon."""
from html import escape
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET
import cairo
import gi
gi.require_version('Rsvg','2.0')
from gi.repository import Rsvg

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[1]
ANDROID='{http://schemas.android.com/apk/res/android}'
AAPT='{http://schemas.android.com/aapt}'


def color(value):
    value=value.removeprefix('#')
    return ('#'+value[2:],int(value[:2],16)/255) if len(value)==8 else ('#'+value,1)


def convert(node,defs):
    if node.tag=='group':
        x=node.get(ANDROID+'pivotX','0'); y=node.get(ANDROID+'pivotY','0')
        rotation=node.get(ANDROID+'rotation','0')
        return f'<g transform="rotate({rotation} {x} {y})">'+''.join(convert(child,defs) for child in node)+'</g>'
    if node.tag!='path':
        raise ValueError(f'Unsupported vector element: {node.tag}')
    attrs={'d':node.get(ANDROID+'pathData')}
    gradient=node.find(f'{AAPT}attr/gradient')
    if gradient is not None:
        gid=f'gradient{len(defs)}'
        stops=[]
        for item in gradient:
            fill,opacity=color(item.get(ANDROID+'color'))
            stops.append(f'<stop offset="{item.get(ANDROID+"offset")}" stop-color="{fill}" stop-opacity="{opacity}"/>')
        if gradient.get(ANDROID+'type')=='radial':
            opening=(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{gradient.get(ANDROID+"centerX")}" '
                     f'cy="{gradient.get(ANDROID+"centerY")}" r="{gradient.get(ANDROID+"gradientRadius")}">')
            closing='</radialGradient>'
        else:
            opening=(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{gradient.get(ANDROID+"startX")}" '
                     f'y1="{gradient.get(ANDROID+"startY")}" x2="{gradient.get(ANDROID+"endX")}" y2="{gradient.get(ANDROID+"endY")}">')
            closing='</linearGradient>'
        defs.append(opening+''.join(stops)+closing)
        attrs['fill']=f'url(#{gid})'
    else:
        attrs['fill'],attrs['fill-opacity']=color(node.get(ANDROID+'fillColor','#00000000'))
    if node.get(ANDROID+'strokeColor'):
        attrs['stroke'],attrs['stroke-opacity']=color(node.get(ANDROID+'strokeColor'))
        attrs['stroke-width']=node.get(ANDROID+'strokeWidth','0')
        attrs['stroke-linejoin']=node.get(ANDROID+'strokeLineJoin','miter')
    return '<path '+' '.join(f'{key}="{escape(str(value),quote=True)}"' for key,value in attrs.items())+'/>'


def main():
    defs=[]; layers=[]
    for name in ['ic_launcher_background.xml','ic_launcher_foreground.xml']:
        vector=ET.parse(REPO/'app/src/main/res/drawable'/name).getroot()
        assert vector.get(ANDROID+'viewportWidth')=='108'
        assert vector.get(ANDROID+'viewportHeight')=='108'
        layers.extend(convert(child,defs) for child in vector)
    svg='<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108"><defs>'+''.join(defs)+'</defs>'+''.join(layers)+'</svg>'
    (ROOT/'vectors').mkdir(exist_ok=True)
    (ROOT/'vectors/icon.svg').write_text(svg)
    surface=cairo.ImageSurface(cairo.FORMAT_RGB24,512,512)
    bounds=Rsvg.Rectangle(); bounds.width=bounds.height=512
    Rsvg.Handle.new_from_data(svg.encode()).render_document(cairo.Context(surface),bounds)
    destination=REPO/'fastlane/metadata/android/en-US/images/icon.png'
    destination.parent.mkdir(parents=True,exist_ok=True)
    surface.write_to_png(str(destination))
    shutil.copy2(destination,REPO/'app/src/main/ic_launcher-playstore.png')
    print('Exported current launcher vector layers as RGB 512 × 512 Play icon')


if __name__=='__main__':
    main()
