#!/usr/bin/env python3
"""Normalize a 4x6 pet sheet and export Android-ready 128px WebP frames."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


LOOPS = ("idle", "walk", "sleep", "happy", "sad", "petted")
SHEET_SIZE = (512, 768)
CELL_SIZE = 128


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sheet", type=Path)
    parser.add_argument("outfit")
    parser.add_argument("--sheet-output", type=Path, required=True)
    parser.add_argument("--frames-output", type=Path, required=True)
    args = parser.parse_args()

    image = Image.open(args.sheet).convert("RGBA")
    if image.size != SHEET_SIZE:
        image = image.resize(SHEET_SIZE, Image.Resampling.NEAREST)

    args.sheet_output.parent.mkdir(parents=True, exist_ok=True)
    image.save(args.sheet_output, "WEBP", lossless=True, method=6)

    args.frames_output.mkdir(parents=True, exist_ok=True)
    for row, loop in enumerate(LOOPS):
        for frame in range(4):
            left = frame * CELL_SIZE
            top = row * CELL_SIZE
            cell = image.crop((left, top, left + CELL_SIZE, top + CELL_SIZE))
            destination = args.frames_output / (
                f"pet_pixel_koala_{args.outfit}_{loop}_{frame}.webp"
            )
            cell.save(destination, "WEBP", lossless=True, method=6)


if __name__ == "__main__":
    main()
