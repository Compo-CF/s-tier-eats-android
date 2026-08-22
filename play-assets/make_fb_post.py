"""Facebook tester-recruitment graphic for S-Tier Eats Android. 1200x1200."""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
BLACK = "C:/Windows/Fonts/ariblk.ttf"
BOLD = "C:/Windows/Fonts/arialbd.ttf"
REG = "C:/Windows/Fonts/arial.ttf"

W = H = 1200
C1, C2 = (73, 63, 214), (140, 92, 220)
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

    centered(d, "Now on Android", 150, ImageFont.truetype(BOLD, 56), (255, 255, 255, 230))
    centered(d, "S-Tier Eats", 235, ImageFont.truetype(BLACK, 130))

    tag = "Rank Houston-area restaurants on a clean S/A/B/C/F tier list."
    tf = ImageFont.truetype(REG, 42)
    y = 430
    for line in wrap(d, tag, tf, 980):
        centered(d, line, y, tf, (255, 255, 255, 235)); y += 58

    # Tier badges row
    bs, gap = 140, 22
    total = len(TIERS) * bs + (len(TIERS) - 1) * gap
    x = (W - total) // 2
    by = 640
    for letter, color in TIERS:
        img.alpha_composite(badge(letter, color, bs), (x, by)); x += bs + gap

    # Call-to-action pill
    pill_w, pill_h = 640, 96
    px, py = (W - pill_w) // 2, 850
    d.rounded_rectangle([px, py, px + pill_w, py + pill_h], radius=48, fill="white")
    cf = ImageFont.truetype(BLACK, 40)
    ctext = "BE A FOUNDING TESTER"
    b = d.textbbox((0, 0), ctext, font=cf)
    d.text(((W - (b[2] - b[0])) / 2 - b[0], py + (pill_h - (b[3] - b[1])) / 2 - b[1]),
           ctext, font=cf, fill=(73, 63, 214))

    centered(d, "Free  ·  Details in the post", 1000, ImageFont.truetype(REG, 34), (255, 255, 255, 210))

    img.convert("RGB").save(os.path.join(HERE, "fb-tester-post.png"))
    print("wrote fb-tester-post.png")


if __name__ == "__main__":
    main()
