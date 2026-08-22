"""Frame raw device captures into Play Store marketing screenshots.

Reads captures/01..05.png (1080x2400), crops the status bar + bottom ad/nav,
places each on a branded gradient with a caption, and writes
play-assets/screenshot-01..05.png at 1242x2208 (9:16, within Play's 2:1 limit).
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
CAPS = os.path.join(HERE, "..", "captures")
BOLD = "C:/Windows/Fonts/arialbd.ttf"

W, H = 1242, 2208
C1, C2 = (73, 63, 214), (140, 92, 220)
# Uniform crop (status bar off the top; ad + nav off the bottom).
CROP = (0, 100, 1080, 1960)

CAPTIONS = {
    "01": "Every spot on the map",
    "02": "See where the crowd lands",
    "03": "Rank it S through F",
    "04": "4,700+ local restaurants",
    "05": "Your personal tier list",
}


def dgrad(w, h, c1, c2):
    img = Image.new("RGB", (w, h), c1)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        d.line([(0, y), (w, y)],
               fill=tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)))
    return img


def frame(tag):
    shot = Image.open(os.path.join(CAPS, f"{tag}.png")).convert("RGB").crop(CROP)
    bg = dgrad(W, H, C1, C2).convert("RGBA")
    d = ImageDraw.Draw(bg)

    # Caption
    f = ImageFont.truetype(BOLD, 66)
    text = CAPTIONS[tag]
    bbox = d.textbbox((0, 0), text, font=f)
    tw = bbox[2] - bbox[0]
    d.text(((W - tw) / 2 - bbox[0], 150), text, font=f, fill="white")

    # Scale the shot to a fixed height so every screenshot's phone is the same size.
    target_h = 1600
    scale = target_h / shot.height
    new_w, new_h = int(shot.width * scale), target_h
    shot = shot.resize((new_w, new_h), Image.LANCZOS)
    x, y = (W - new_w) // 2, H - new_h - 90

    # Rounded corners + drop shadow.
    radius = 40
    mask = Image.new("L", (new_w, new_h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, new_w, new_h], radius=radius, fill=255)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x - 6, y - 4, x + new_w + 6, y + new_h + 10], radius=radius + 8, fill=(0, 0, 0, 130))
    bg = Image.alpha_composite(bg, shadow.filter(ImageFilter.GaussianBlur(20)))
    bg.paste(shot, (x, y), mask)

    out = os.path.join(HERE, f"screenshot-{tag}.png")
    bg.convert("RGB").save(out)
    print("wrote", os.path.basename(out))


if __name__ == "__main__":
    for tag in CAPTIONS:
        frame(tag)
