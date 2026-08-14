"""Generate Play Store graphic assets for S-Tier Eats Android.

Outputs:
  play-assets/icon-512.png            (app icon, 512x512)
  play-assets/feature-graphic-1024x500.png
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.dirname(os.path.abspath(__file__))
BLACK = "C:/Windows/Fonts/ariblk.ttf"   # Arial Black
BOLD = "C:/Windows/Fonts/arialbd.ttf"
REG = "C:/Windows/Fonts/arial.ttf"

TIERS = [
    ("S", (226, 59, 59)),
    ("A", (238, 122, 46)),
    ("B", (224, 194, 58)),
    ("C", (79, 180, 119)),
    ("F", (140, 89, 199)),
]


def vgrad(w, h, top, bot):
    img = Image.new("RGB", (w, h), top)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def dgrad(w, h, c1, c2):
    """Diagonal gradient."""
    img = Image.new("RGB", (w, h), c1)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        c = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def rounded_badge(letter, color, size, radius=None, font_frac=0.6):
    radius = radius or int(size * 0.22)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=color)
    f = ImageFont.truetype(BLACK, int(size * font_frac))
    bbox = d.textbbox((0, 0), letter, font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text(((size - tw) / 2 - bbox[0], (size - th) / 2 - bbox[1]), letter,
           font=f, fill="white")
    return img


# ── App icon 512x512 — full-bleed S-tier red with a bold white S ──
def make_icon():
    S = 512
    icon = vgrad(S, S, (240, 74, 74), (196, 47, 46)).convert("RGBA")
    d = ImageDraw.Draw(icon)
    f = ImageFont.truetype(BLACK, 380)
    letter = "S"
    bbox = d.textbbox((0, 0), letter, font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (S - tw) / 2 - bbox[0]
    y = (S - th) / 2 - bbox[1] - 10
    # soft shadow
    d.text((x + 6, y + 8), letter, font=f, fill=(120, 20, 20, 120))
    d.text((x, y), letter, font=f, fill="white")
    icon.convert("RGB").save(os.path.join(OUT, "icon-512.png"))
    print("wrote icon-512.png")


# ── Feature graphic 1024x500 ──
def make_feature():
    W, H = 1024, 500
    bg = dgrad(W, H, (73, 63, 214), (140, 92, 220)).convert("RGBA")
    d = ImageDraw.Draw(bg)

    title_f = ImageFont.truetype(BLACK, 92)
    tag_f = ImageFont.truetype(BOLD, 34)
    sub_f = ImageFont.truetype(REG, 28)

    d.text((70, 120), "S-Tier Eats", font=title_f, fill="white")
    d.text((74, 232), "The Woodlands & Houston, ranked.", font=tag_f,
           fill=(255, 255, 255, 235))
    d.text((74, 286), "Rank restaurants on an S/A/B/C/F tier list —",
           font=sub_f, fill=(255, 255, 255, 200))
    d.text((74, 322), "and see where the crowd lands.",
           font=sub_f, fill=(255, 255, 255, 200))

    # Row of tier badges, lower area
    bsize = 74
    gap = 16
    total = len(TIERS) * bsize + (len(TIERS) - 1) * gap
    x0 = 74
    y0 = 390
    for i, (letter, color) in enumerate(TIERS):
        b = rounded_badge(letter, color, bsize)
        bg.alpha_composite(b, (x0 + i * (bsize + gap), y0))

    bg.convert("RGB").save(os.path.join(OUT, "feature-graphic-1024x500.png"))
    print("wrote feature-graphic-1024x500.png")


if __name__ == "__main__":
    make_icon()
    make_feature()
