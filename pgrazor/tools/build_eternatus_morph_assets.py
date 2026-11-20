#!/usr/bin/env python3
"""Build transparent joystick layers + launcher frames from four user-owned Eternatus images.
This script is used only for asset imports; runtime rendering is in Pad.java.
Inputs: environment variables PGBR_JOY_CLEAN, PGBR_JOY_STORM,
        PGBR_BALL_CLEAN, PGBR_BALL_STORM (HTTPS source images).
"""
import io
import math
import os
from pathlib import Path
from urllib.request import Request, urlopen

from PIL import Image


OUT = Path(__file__).resolve().parents[1] / "assets" / "map-icons"
OUT.mkdir(parents=True, exist_ok=True)


def source(name):
    url = os.environ[name]
    req = Request(url, headers={"User-Agent": "PGBR-asset-build/1.0"})
    with urlopen(req, timeout=40) as stream:
        data = stream.read()
    result = Image.open(io.BytesIO(data)).convert("RGBA")
    if min(result.size) < 200:
        raise ValueError(f"{name}: minimum 200x200 input is required")
    if result.getpixel((0, 0))[3] != 0:
        raise ValueError(f"{name}: image corner is not transparent")
    print("source", name, result.size, len(data))
    return result.resize((384, 384), Image.Resampling.LANCZOS)


def base(original, storm):
    result = original.copy()
    px = result.load()
    n = result.width
    radius = n * .5
    for y in range(n):
        for x in range(n):
            r = math.hypot(x-radius+.5, y-radius+.5)/radius
            if r <= .37:
                alpha = 0.
            elif r < .43:
                alpha = ((r-.37)/.06)**2 * (.55 if storm else .40)
            elif r < .75:
                alpha = .62 if storm else .40
            elif r < .91:
                alpha = (.62 + .38*(r-.75)/.16) if storm else (.40 + .60*(r-.75)/.16)
            else:
                alpha = 1.
            red, green, blue, a = px[x, y]
            px[x, y] = red, green, blue, round(a*alpha)
    return result


def thumb(original):
    n=original.width
    center=n//2
    half=int(center*.41)
    result=original.crop((center-half,center-half,center+half,center+half))
    px=result.load()
    for y in range(2*half):
        for x in range(2*half):
            r=math.hypot(x-half+.5,y-half+.5)
            alpha=max(0.,min(1.,(half-.5-r)/3.))
            red,green,blue,a=px[x,y]
            px[x,y]=red,green,blue,round(a*alpha)
    return result


def save(image, name):
    target=OUT/name
    image.save(target, "WEBP", quality=90, method=4)
    if target.stat().st_size > 300_000:
        raise RuntimeError(f"Unexpected asset size: {target}")
    print("asset", name, target.stat().st_size)


def main():
    clean=source("PGBR_JOY_CLEAN")
    storm=source("PGBR_JOY_STORM")
    ball_clean=source("PGBR_BALL_CLEAN")
    ball_storm=source("PGBR_BALL_STORM")
    save(base(clean,False),"joystick-eternatus-morph-clean.webp")
    save(base(storm,True),"joystick-eternatus-morph-storm.webp")
    save(thumb(clean),"joystick-eternatus-morph-thumb.webp")
    save(ball_clean,"eternatus-ball-morph-clean.webp")
    save(ball_storm,"eternatus-ball-morph-storm.webp")


if __name__=="__main__":
    main()
