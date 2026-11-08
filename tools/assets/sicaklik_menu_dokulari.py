"""Termometre menüsünün dokusunu üretir (sözleşme sicaklik-3 §A9, S3-08).

Çıktı (src/main/resources/assets/dogalafetler/textures/gui/sicaklik/termometre.png, 512x256):
  (0,0)     320x214 pano: vanilla GUI grisi, kabartma kenar, oyuncu kutusu (8,18 54x90), ayraç (y 112),
                    iki döküm sütunu (8,116 152x92; 164,116 148x92)
  (330,0)   12x80   cam tüp arka planı (koyu cam + çerçeve; iç 3..9 x 2..80 sıvıya açık)
  (344,0)   12x80   cam tüp önü (yalnız çerçeve ve parlama; iç saydam)
  (360,0)   16x16   hazne arka planı, (378,0) beyaz sıvı diski (renk kodda verilir), (396,0) hazne önü
  (420,0)   6x7     mekan işaretçisi (dolu, sola bakan), (428,0) hissedilen işaretçisi (içi boş)
  (440,0)   7x4     yukarı şevron (ısınma), (440,6) aşağı şevron (soğuma)
  (0,224)   16x16 x5 durum ikonları renkli: ıslak, üşüme, donma, bunalma, yanma; (0,240) aynıları gri

Koordinatlar TermometreEkrani sabitleriyle eşleşir. Çalıştırma: KÖK/.venv/bin/python tools/assets/sicaklik_menu_dokulari.py
(rastgelelik yok → aynı bayt).
"""
from pathlib import Path

from PIL import Image, ImageDraw

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/assets/dogalafetler/textures/gui/sicaklik/termometre.png"

PANO_G, PANO_Y = 320, 214
GRI = (198, 198, 198, 255)
BEYAZ = (255, 255, 255, 255)
GOLGE = (85, 85, 85, 255)
SIYAH = (0, 0, 0, 255)
YUVA = (139, 139, 139, 255)
YUVA_KOYU = (55, 55, 55, 255)


def kabartma(d, x0, y0, x1, y1, ic, ust, alt):
    """[x0,x1)x[y0,y1) dikdörtgen: sol-üst kenar `ust`, sağ-alt kenar `alt`, iç `ic`."""
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=ic)
    d.line([(x0, y0), (x1 - 2, y0)], fill=ust)
    d.line([(x0, y0), (x0, y1 - 2)], fill=ust)
    d.line([(x0 + 1, y1 - 1), (x1 - 1, y1 - 1)], fill=alt)
    d.line([(x1 - 1, y0 + 1), (x1 - 1, y1 - 1)], fill=alt)


def pano(img):
    d = ImageDraw.Draw(img)
    g, y = PANO_G, PANO_Y
    # Vanilla pencere: siyah dış hat (köşeler yuvarlak), beyaz üst-sol ve gölge alt-sağ kabartma.
    d.rectangle([1, 1, g - 2, y - 2], fill=GRI)
    d.line([(2, 0), (g - 4, 0)], fill=SIYAH)
    d.line([(0, 2), (0, y - 4)], fill=SIYAH)
    d.line([(3, y - 1), (g - 3, y - 1)], fill=SIYAH)
    d.line([(g - 1, 3), (g - 1, y - 3)], fill=SIYAH)
    for px, py in [(1, 1), (g - 3, 1), (g - 2, 2), (1, y - 3), (2, y - 2), (g - 2, y - 3), (g - 3, y - 2)]:
        d.point((px, py), fill=SIYAH)
    d.line([(2, 1), (g - 4, 1)], fill=BEYAZ)
    d.line([(1, 2), (1, y - 4)], fill=BEYAZ)
    d.line([(2, 2), (g - 5, 2)], fill=BEYAZ)
    d.line([(2, 2), (2, y - 5)], fill=BEYAZ)
    d.line([(3, y - 2), (g - 3, y - 2)], fill=GOLGE)
    d.line([(g - 2, 3), (g - 2, y - 3)], fill=GOLGE)
    d.line([(4, y - 3), (g - 3, y - 3)], fill=GOLGE)
    d.line([(g - 3, 4), (g - 3, y - 3)], fill=GOLGE)

    # Oyuncu kutusu: envanterdeki gibi çökük siyah çerçeve, içi alttan aydınlanan koyu gradyan.
    kabartma(d, 8, 18, 62, 108, YUVA, YUVA_KOYU, BEYAZ)
    px = img.load()
    for yy in range(19, 107):
        t = (yy - 19) / 87
        c = (round(28 + 30 * t), round(30 + 34 * t), round(38 + 36 * t), 255)
        for xx in range(9, 61):
            px[xx, yy] = c
    # Ayak zemini: kutunun altında hafif açık şerit.
    d.line([(10, 103), (59, 103)], fill=(96, 104, 116, 255))

    # Ayraç: oyulmuş çizgi.
    d.line([(6, 112), (g - 7, 112)], fill=YUVA)
    d.line([(6, 113), (g - 7, 113)], fill=BEYAZ)

    # Döküm sütunları: hafif çökük açık panolar.
    ic = (186, 186, 186, 255)
    kabartma(d, 8, 116, 160, 208, ic, YUVA, BEYAZ)
    kabartma(d, 164, 116, 312, 208, ic, YUVA, BEYAZ)
    # Başlık bantları.
    for x0, x1 in [(9, 159), (165, 311)]:
        d.rectangle([x0, 117, x1 - 1, 128], fill=(172, 172, 172, 255))
        d.line([(x0, 129), (x1 - 1, 129)], fill=YUVA)


def tup(img):
    d = ImageDraw.Draw(img)
    ox = 330
    cerceve = (40, 44, 52, 255)
    # Arka: yuvarlak tepe, koyu cam iç.
    d.rectangle([ox + 1, 1, ox + 10, 79], fill=cerceve)
    d.rectangle([ox + 2, 0, ox + 9, 79], fill=cerceve)
    d.rectangle([ox + 3, 2, ox + 8, 79], fill=(58, 66, 80, 255))
    for yy in range(4, 80, 4):
        d.point((ox + 8, yy), fill=(70, 80, 96, 255))
    # Ön: yalnız çerçeve ve sol parlama (iç saydam).
    # Alt kenar yok: sıvı sütunu hazneye kesintisiz iner.
    on = 344
    acik = (150, 160, 175, 200)
    d.line([(on + 2, 0), (on + 9, 0)], fill=cerceve)
    d.line([(on + 1, 1), (on + 1, 79)], fill=cerceve)
    d.line([(on + 10, 1), (on + 10, 79)], fill=cerceve)
    d.line([(on + 3, 1), (on + 8, 1)], fill=acik)
    d.line([(on + 2, 1), (on + 2, 79)], fill=acik)
    d.line([(on + 9, 1), (on + 9, 79)], fill=acik)
    d.line([(on + 3, 3), (on + 3, 78)], fill=(255, 255, 255, 120))
    d.line([(on + 4, 3), (on + 4, 40)], fill=(255, 255, 255, 60))


def hazne(img):
    d = ImageDraw.Draw(img)
    cerceve = (40, 44, 52, 255)
    d.ellipse([360, 0, 375, 15], fill=cerceve)
    d.ellipse([361, 1, 374, 14], fill=(150, 160, 175, 255))
    d.ellipse([362, 2, 373, 13], fill=(58, 66, 80, 255))
    d.ellipse([379, 1, 392, 14], fill=BEYAZ)
    on = 396
    d.ellipse([on, 0, on + 15, 15], outline=cerceve)
    d.ellipse([on + 1, 1, on + 14, 14], outline=(150, 160, 175, 220))
    d.line([(on + 4, 4), (on + 5, 3)], fill=(255, 255, 255, 200))
    d.point((on + 4, 5), fill=(255, 255, 255, 150))
    d.point((on + 4, 6), fill=(255, 255, 255, 90))


def isaretciler(img):
    d = ImageDraw.Draw(img)
    koyu = (24, 24, 28, 255)
    d.polygon([(420, 3), (425, 0), (425, 6)], fill=koyu)
    d.polygon([(422, 3), (424, 2), (424, 4)], fill=(255, 236, 160, 255))
    d.point((423, 3), fill=(255, 236, 160, 255))
    d.polygon([(428, 3), (433, 0), (433, 6)], fill=koyu)
    d.polygon([(430, 3), (432, 2), (432, 4)], fill=(120, 220, 255, 255))
    d.point((431, 3), fill=(30, 40, 60, 255))


def sevronlar(img):
    """Eğilim oku: tepe ortada; yukarı ısınma (turuncu), aşağı soğuma (mavi)."""
    px = img.load()
    yukari = ["...#...", "..###..", ".##.##.", "##...##"]
    asagi = list(reversed(yukari))
    for satirlar, y0, c in [(yukari, 0, (255, 140, 60, 255)), (asagi, 6, (90, 170, 255, 255))]:
        for yy, s in enumerate(satirlar):
            for xx, ch in enumerate(s):
                if ch == "#":
                    px[440 + xx, y0 + yy] = c


# 16x16 piksel çizimler: '.' saydam, harfler palet.
IKONLAR = {
    "islak": ([
        "................",
        ".......a........",
        ".......a........",
        "......aba.......",
        "......abb.......",
        ".....abbba......",
        ".....abbba......",
        "....abbbbba.....",
        "....acbbbba.....",
        "...acbbbbbba....",
        "...acbbbbbba....",
        "...accbbbbba....",
        "....acbbbba.....",
        ".....abbba......",
        "......aaa.......",
        "................",
    ], {"a": (24, 70, 160), "b": (60, 140, 235), "c": (190, 230, 255)}),
    "usume": ([
        "................",
        "...aaa..........",
        "..abbba.........",
        "..abcba..d......",
        "..abbba.ddd.....",
        "...aaa..d.d.....",
        "..aeeea..d......",
        ".aeeeeea...dd...",
        ".aeeeeea..dddd..",
        ".aeeeeea...dd...",
        ".aeeeeea........",
        "..aeeea.........",
        "..ae.ea.........",
        "..ae.ea.........",
        "..aa.aa.........",
        "................",
    ], {"a": (40, 60, 90), "b": (240, 210, 180), "c": (60, 60, 80), "d": (215, 240, 255),
        "e": (110, 170, 230)}),
    "donma": ([
        "................",
        ".......a........",
        "....a..a..a.....",
        ".....a.a.a......",
        "......bab.......",
        "..a...bab...a...",
        "...aaabcbaaa....",
        ".aaaabcccbaaaa..",
        "...aaabcbaaa....",
        "..a...bab...a...",
        "......bab.......",
        ".....a.a.a......",
        "....a..a..a.....",
        ".......a........",
        "................",
        "................",
    ], {"a": (70, 140, 230), "b": (160, 210, 255), "c": (240, 250, 255)}),
    "bunalma": ([
        "................",
        "....aaaaa.......",
        "...abbbbba..c...",
        "..abbbbbbba.c...",
        "..abdbbdbba.cc..",
        "..abbbbbbba.cc..",
        "..abbeeebba.....",
        "...abbbbba...c..",
        "....aaaaa....c..",
        "...afffffa..ccc.",
        "..afffffffa.ccc.",
        "..afffffffa.....",
        "..afffffffa.....",
        "..afffffffa.....",
        "..aaaaaaaaa.....",
        "................",
    ], {"a": (110, 50, 20), "b": (250, 175, 110), "c": (130, 210, 255), "d": (60, 30, 20),
        "e": (170, 70, 50), "f": (235, 110, 50)}),
    "yanma": ([
        "................",
        ".......a........",
        "......aa........",
        "......aba.......",
        ".....abba...a...",
        "..a..abbba..a...",
        "..aa.abbbba.aa..",
        "..aba.abcbbaba..",
        ".abbaabccbbbba..",
        ".abbbbbcccbbba..",
        ".abbbbccdccbba..",
        ".abbbccdddccba..",
        "..abbccdddcba...",
        "...abbcccbba....",
        "....aaaaaaa.....",
        "................",
    ], {"a": (140, 20, 10), "b": (230, 60, 20), "c": (255, 160, 40), "d": (255, 240, 150)}),
}
SIRA = ["islak", "usume", "donma", "bunalma", "yanma"]


def ikonlar(img):
    px = img.load()
    for i, ad in enumerate(SIRA):
        satirlar, palet = IKONLAR[ad]
        for yy, s in enumerate(satirlar):
            for xx, ch in enumerate(s):
                if ch == ".":
                    continue
                r, g, b = palet[ch]
                px[i * 16 + xx, 224 + yy] = (r, g, b, 255)
                gri = round(0.3 * r + 0.59 * g + 0.11 * b)
                gri = round(90 + (gri - 90) * 0.45)
                px[i * 16 + xx, 240 + yy] = (gri, gri, gri, 200)


def main():
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    pano(img)
    tup(img)
    hazne(img)
    isaretciler(img)
    sevronlar(img)
    ikonlar(img)
    CIKTI.parent.mkdir(parents=True, exist_ok=True)
    img.save(CIKTI, optimize=True)
    print(f"{CIKTI.relative_to(KOK)} {img.size[0]}x{img.size[1]}")


if __name__ == "__main__":
    main()
