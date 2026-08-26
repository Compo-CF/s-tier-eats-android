"""Instagram Story for S-Tier Eats Android tester recruiting. 1080x1920 (9:16)."""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
BLACK = "C:/Windows/Fonts/ariblk.ttf"
BOLD = "C:/Windows/Fonts/arialbd.ttf"
REG = "C:/Windows/Fonts/arial.ttf"

W, H = 1080, 1920
C1, C2 = (63, 55, 190), (150, 92, 224)   # deep indigo -> violet
TIERS = [("S", (226, 59, 59)), ("A", (238, 122, 46)), ("B", (224, 194, 58)),
         ("C", (79, 180, 119)), ("F", (140, 89, 199))]


def dgrad(w, h, c1, c2):
    img = Image.new("RGB", (w, h), c1)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)))
    return img


def badge(letter, color, size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=color)
    f = ImageFont.truetype(BLACK, int(size * 0.55))
    b = d.textbbox((0, 0), letter, font=f)
    d.text(((size - (b[2] - b[0])) / 2 - b[0], (size - (b[3] - b[1])) / 2 - b[1]),
           letter, font=f, fill="white")
    return img


def centered(d, text, y, font, fill="white"):
    b = d.textbbox((0, 0), text, font=font)
    d.text(((W - (b[2] - b[0])) / 2 - b[0], y), text, font=font, fill=fill)


def wrap(d, text, font, maxw):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if d.textlength(t, font=font) <= maxw:
            cur = t
        else:
            lines.append(cur); cur = w
    if cur:
        lines.append(cur)
    return lines


def main():
    img = dgrad(W, H, C1, C2).convert("RGBA")
    d = ImageDraw.Draw(img)

    # Top eyebrow
    centered(d, "CALLING ALL FOODIES", 300, ImageFont.truetype(BOLD, 44), (255, 255, 255, 220))

    # Headline
    centered(d, "Now on", 400, ImageFont.truetype(BOLD, 70), (255, 255, 255, 235))
    centered(d, "ANDROID", 480, ImageFont.truetype(BLACK, 130))

    # App name
    centered(d, "S-Tier Eats", 660, ImageFont.truetype(BLACK, 96))

    # Tagline
    tag = "Rank Houston-area restaurants on a clean S / A / B / C / F tier list."
    tf = ImageFont.truetype(REG, 46)
    y = 800
    for line in wrap(d, tag, tf, 900):
        centered(d, line, y, tf, (255, 255, 255, 235)); y += 62

    # Tier badges row
    bs, gap = 150, 26
    total = len(TIERS) * bs + (len(TIERS) - 1) * gap
    x = (W - total) // 2
    by = 1010
    for letter, color in TIERS:
        img.alpha_composite(badge(letter, color, bs), (x, by)); x += bs + gap

    # "Be a founding tester" pill
    pill_w, pill_h = 720, 110
    px, py = (W - pill_w) // 2, 1260
    d.rounded_rectangle([px, py, px + pill_w, py + pill_h], radius=55, fill="white")
    cf = ImageFont.truetype(BLACK, 46)
    ctext = "BE A FOUNDING TESTER"
    b = d.textbbox((0, 0), ctext, font=cf)
    d.text(((W - (b[2] - b[0])) / 2 - b[0], py + (pill_h - (b[3] - b[1])) / 2 - b[1]),
           ctext, font=cf, fill=(73, 63, 214))

    # 3-step how-to (Story viewers can't tap through, so make it dead simple)
    steps = [
        "1.  Tap the link sticker above",
        "2.  Join the testers group",
        "3.  Download & start ranking",
    ]
    sf = ImageFont.truetype(BOLD, 44)
    sy = 1470
    for s in steps:
        centered(d, s, sy, sf, (255, 255, 255, 240)); sy += 74

    # Footer note
    centered(d, "Free  ·  Android only  ·  Founding testers get bragging rights",
             1720, ImageFont.truetype(REG, 34), (255, 255, 255, 205))

    img.convert("RGB").save(os.path.join(HERE, "ig-story-tester.png"))
    print("wrote ig-story-tester.png")


if __name__ == "__main__":
    main()
