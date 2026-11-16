"""Generate Android launcher icons from icon.jpg.

For modern adaptive launcher icons we need TWO layers at 108dp size class:
  ic_launcher_background.png  full-bleed icon (fills 108dp canvas)
  ic_launcher_foreground.png  same icon scaled inside the safe zone

For legacy bitmap launchers we ship square + circular icons at the regular
launcher sizes (mdpi 48 -> xxxhdpi 192).
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "icon.jpg")
RES = os.path.join(ROOT, "app", "src", "main", "res")

LEGACY = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive layers must be 108dp; native pixel sizes:
#   mdpi 108, hdpi 162, xhdpi 216, xxhdpi 324, xxxhdpi 432
ADAPTIVE = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

# Foreground occupies this fraction of the 108dp canvas.
# Safe zone is ~66/108 = 0.611 but we err slightly larger so the icon
# isn't tiny on most launchers (Pixel uses ~80% mask).
FG_FRACTION = 0.86

def square(src: Image.Image, size: int) -> Image.Image:
    return src.resize((size, size), Image.LANCZOS).convert("RGBA")

def circle(src: Image.Image, size: int) -> Image.Image:
    sq = square(src, size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(sq, (0, 0), mask)
    return out

def adaptive_foreground(src: Image.Image, canvas: int) -> Image.Image:
    inner = int(canvas * FG_FRACTION)
    icon = src.resize((inner, inner), Image.LANCZOS).convert("RGBA")
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    off = (canvas - inner) // 2
    out.paste(icon, (off, off), icon)
    return out

def main():
    if not os.path.isfile(SRC):
        raise SystemExit(f"icon.jpg not found at {SRC}")
    src = Image.open(SRC).convert("RGBA")
    print(f"Loaded {SRC} ({src.size[0]}x{src.size[1]})")

    for folder, size in LEGACY.items():
        d = os.path.join(RES, folder)
        os.makedirs(d, exist_ok=True)
        for stale in ("ic_launcher.webp", "ic_launcher_round.webp"):
            p = os.path.join(d, stale)
            if os.path.isfile(p):
                os.remove(p)
                print(f"  rm {os.path.relpath(p, ROOT)}")
        square(src, size).save(os.path.join(d, "ic_launcher.png"), "PNG", optimize=True)
        circle(src, size).save(os.path.join(d, "ic_launcher_round.png"), "PNG", optimize=True)
        print(f"  wrote {folder}/ic_launcher.png + ic_launcher_round.png ({size}px)")

    for folder, size in ADAPTIVE.items():
        d = os.path.join(RES, folder)
        # background: full bleed (no transparent border, no white edge)
        square(src, size).save(os.path.join(d, "ic_launcher_background.png"), "PNG", optimize=True)
        # foreground: scaled inside 86% of canvas, transparent outside
        adaptive_foreground(src, size).save(os.path.join(d, "ic_launcher_foreground.png"), "PNG", optimize=True)
        print(f"  wrote {folder}/ic_launcher_background.png + ic_launcher_foreground.png ({size}px)")

    print("done")

if __name__ == "__main__":
    main()
