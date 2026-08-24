#!/usr/bin/env python3
"""Generates the legacy (pre-API 26) launcher PNGs from the same shapes as the adaptive icon.

The adaptive icon (res/mipmap-anydpi-v26) is XML and is what modern launchers use. Android 7.x
still needs raster mipmaps, so this script renders them with 4x supersampling and no third-party
dependencies. Re-run it only if the artwork changes:

    python3 tools/generate_launcher_icons.py
"""

import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "app", "src", "main", "res")

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BACKGROUND = (26, 44, 74)
SKY = (44, 95, 168)
SUN = (255, 201, 102)
RIDGE_FAR = (140, 178, 232)
RIDGE_NEAR = (233, 241, 255)

SS = 4  # supersampling factor


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def render(size, rounded):
    """Returns an RGBA pixel buffer of the icon at `size` px, box-filtered from SS x SS samples."""
    hi = size * SS
    acc = [[[0, 0, 0, 0] for _ in range(size)] for _ in range(size)]

    cx = cy = hi / 2.0
    radius = hi / 2.0 if rounded else None
    corner = hi * 0.22

    sun_x, sun_y, sun_r = hi * 0.68, hi * 0.32, hi * 0.11

    for y in range(hi):
        for x in range(hi):
            fx, fy = x + 0.5, y + 0.5

            # Icon silhouette: circle for the round variant, rounded square otherwise.
            if rounded:
                inside = (fx - cx) ** 2 + (fy - cy) ** 2 <= radius ** 2
            else:
                dx = max(corner - fx, fx - (hi - corner), 0.0)
                dy = max(corner - fy, fy - (hi - corner), 0.0)
                inside = dx * dx + dy * dy <= corner * corner
            if not inside:
                continue

            # Vertical sky gradient.
            colour = lerp(BACKGROUND, SKY, fy / hi)

            if (fx - sun_x) ** 2 + (fy - sun_y) ** 2 <= sun_r ** 2:
                colour = SUN

            # Two mountain ridges, back to front.
            far = hi * 0.66 + math.sin((fx / hi) * math.pi * 1.6 + 0.7) * hi * 0.09
            near = hi * 0.80 + math.sin((fx / hi) * math.pi * 2.4 + 2.2) * hi * 0.07
            if fy >= far:
                colour = RIDGE_FAR
            if fy >= near:
                colour = RIDGE_NEAR

            cell = acc[y // SS][x // SS]
            cell[0] += colour[0]
            cell[1] += colour[1]
            cell[2] += colour[2]
            cell[3] += 255

    samples = SS * SS
    rows = []
    for row in acc:
        out = bytearray()
        for r, g, b, a in row:
            if a == 0:
                out += bytes((0, 0, 0, 0))
            else:
                # Premultiplied average, then un-premultiply against coverage.
                out += bytes((
                    min(255, round(r / (a / 255.0))),
                    min(255, round(g / (a / 255.0))),
                    min(255, round(b / (a / 255.0))),
                    round(a / samples),
                ))
        rows.append(bytes(out))
    return rows


def write_png(path, rows, size):
    raw = b"".join(b"\x00" + row for row in rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)


def main():
    for density, size in DENSITIES.items():
        directory = os.path.join(RES, "mipmap-" + density)
        os.makedirs(directory, exist_ok=True)
        for name, rounded in (("ic_launcher", False), ("ic_launcher_round", True)):
            write_png(os.path.join(directory, name + ".png"), render(size, rounded), size)
            print("wrote", density, name)


if __name__ == "__main__":
    main()
