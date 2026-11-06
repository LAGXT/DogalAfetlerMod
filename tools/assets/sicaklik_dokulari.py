"""Sıcaklık sisteminin dokularını üretir (madde 7c, 7d; docs/tasarim/sicaklik.md).

Çıktılar (src/main/resources/assets/dogalafetler/textures/):
  gui/sicaklik/bar.png     256x32  bar (0,0 normal; 0,8 parlak), vücut işaretçisi (184,0 beyaz; 192,0 buz; 200,0 kor),
                                   ortam oku (208,0; 5x3)
  misc/sicaklik_buz.png    256x256 buz kenarı (alfa): kenarlardan içeri dallanan buz kristalleri + kırağı
  misc/sicaklik_sicak.png  256x256 sıcak hava vinyeti; dikey yönde kesintisiz (HUD onu yukarı kaydırır)

Çalıştırma: KÖK/.venv/bin/python tools/assets/sicaklik_dokulari.py (aynı tohum → aynı bayt).
"""
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

KOK = Path(__file__).resolve().parents[2]
DOKU = KOK / "src/main/resources/assets/dogalafetler/textures"

# Bar renk geçişi: (konum 0-1, RGB). 0 = -100 (donma), 0,5 = nötr, 1 = +100 (yanma).
DURAKLAR = [
    (0.00, (27, 58, 140)),
    (0.20, (46, 123, 214)),
    (0.36, (143, 211, 244)),
    (0.50, (241, 238, 228)),
    (0.64, (247, 185, 85)),
    (0.80, (232, 97, 44)),
    (1.00, (142, 20, 20)),
]


def renk(f):
    for (a, ca), (b, cb) in zip(DURAKLAR, DURAKLAR[1:]):
        if f <= b:
            t = (f - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return DURAKLAR[-1][1]


def olcekle(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def karistir(c, d, t):
    return tuple(round(c[i] + (d[i] - c[i]) * t) for i in range(3))


def bar(img, y0, parlak):
    px = img.load()
    w = 182
    cerceve = (18, 18, 22, 235)
    for x in range(w):
        px[x, y0] = cerceve
        px[x, y0 + 4] = cerceve
    for y in range(y0, y0 + 5):
        px[0, y] = cerceve
        px[w - 1, y] = cerceve
    for x in range(1, w - 1):
        c = renk((x - 1) / (w - 3))
        if parlak:
            c = karistir(c, (255, 255, 255), 0.35)
        px[x, y0 + 1] = olcekle(c, 1.18) + (255,)
        px[x, y0 + 2] = c + (255,)
        px[x, y0 + 3] = olcekle(c, 0.82) + (255,)


def isaretci(img, x0, dolgu):
    d = ImageDraw.Draw(img)
    koyu = (16, 16, 16, 255)
    d.polygon([(x0, 0), (x0 + 6, 0), (x0 + 3, 3)], fill=koyu)
    d.polygon([(x0 + 1, 0), (x0 + 5, 0), (x0 + 3, 2)], fill=dolgu)
    px = img.load()
    for y in range(3, 9):
        px[x0 + 2, y] = koyu
        px[x0 + 3, y] = dolgu
        px[x0 + 4, y] = koyu
    px[x0 + 3, 8] = koyu


def ortam_oku(img, x0):
    px = img.load()
    acik = (235, 235, 235, 230)
    koyu = (20, 20, 20, 200)
    px[x0 + 2, 0] = acik
    for x in range(1, 4):
        px[x0 + x, 1] = acik
    for x in range(0, 5):
        px[x0 + x, 2] = koyu if x in (0, 4) else acik


def bar_dokusu():
    img = Image.new("RGBA", (256, 32), (0, 0, 0, 0))
    bar(img, 0, False)
    bar(img, 8, True)
    isaretci(img, 184, (255, 255, 255, 255))
    isaretci(img, 192, (150, 230, 255, 255))
    isaretci(img, 200, (255, 120, 40, 255))
    ortam_oku(img, 208)
    return img


def dal(d, rnd, x, y, aci, uzunluk, kalinlik, derinlik):
    if derinlik == 0 or uzunluk < 3:
        return
    x2 = x + math.cos(aci) * uzunluk
    y2 = y + math.sin(aci) * uzunluk
    alfa = int(120 + 90 * (derinlik / 5))
    d.line([(x, y), (x2, y2)], fill=(235, 250, 255, alfa), width=max(1, kalinlik))
    adim = rnd.randint(2, 4)
    for i in range(1, adim + 1):
        t = i / (adim + 1)
        bx = x + (x2 - x) * t
        by = y + (y2 - y) * t
        for isaret in (-1, 1):
            dal(d, rnd, bx, by, aci + isaret * math.radians(60), uzunluk * (0.45 - 0.05 * i) * (1 - t * 0.3),
                max(1, kalinlik - 1), derinlik - 1)
    dal(d, rnd, x2, y2, aci + rnd.uniform(-0.2, 0.2), uzunluk * 0.55, max(1, kalinlik - 1), derinlik - 1)


def buz_dokusu():
    n = 256
    rnd = random.Random(20261106)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    dx = (xx - (n - 1) / 2) / (n / 2)
    dy = (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx ** 2 * 0.9 + dy ** 2 * 1.1)
    kenar = np.clip((r - 0.62) / 0.5, 0, 1) ** 1.6
    gurultu = np.random.default_rng(7).random((n, n))
    kiragi = np.clip(kenar * (0.55 + 0.45 * gurultu), 0, 1)
    taban = np.zeros((n, n, 4), dtype=np.uint8)
    taban[..., 0] = 200
    taban[..., 1] = 235
    taban[..., 2] = 255
    taban[..., 3] = (kiragi * 200).astype(np.uint8)
    img = Image.fromarray(taban, "RGBA")
    kristal = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(kristal)
    for _ in range(26):
        kenar_no = rnd.randrange(4)
        p = rnd.uniform(0, n)
        x, y, aci = [(p, -2, math.pi / 2), (p, n + 1, -math.pi / 2), (-2, p, 0.0), (n + 1, p, math.pi)][kenar_no]
        aci += rnd.uniform(-0.5, 0.5)
        dal(d, rnd, x, y, aci, rnd.uniform(28, 62), 2, 5)
    kristal = kristal.filter(ImageFilter.GaussianBlur(0.4))
    img = Image.alpha_composite(img, kristal)
    a = np.array(img).astype(np.float64)
    # Ortaya doğru kristaller de sönsün: ekranın ortası açık kalır.
    a[..., 3] *= np.clip((r - 0.35) / 0.45, 0, 1)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def sicak_dokusu():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    dx = (xx - (n - 1) / 2) / (n / 2)
    dy = (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx ** 2 + dy ** 2 * 0.8)
    kenar = np.clip((r - 0.45) / 0.7, 0, 1) ** 1.3
    # Dikeyde 256 periyotlu dalgalar: doku kaydırılınca dikis görünmez.
    faz = 2 * math.pi * yy / n
    dalga = (0.5 + 0.5 * np.sin(6 * faz + 2.2 * np.sin(2 * math.pi * xx / n * 3 + 3 * faz))) ** 3
    alfa = np.clip(kenar * (0.55 + 0.45 * dalga) + 0.10 * dalga * (1 - kenar), 0, 1)
    sicaklik = np.clip(kenar * 1.2, 0, 1)
    img = np.zeros((n, n, 4), dtype=np.uint8)
    img[..., 0] = 255
    img[..., 1] = (170 - 120 * sicaklik).astype(np.uint8)
    img[..., 2] = (60 - 50 * sicaklik).astype(np.uint8)
    img[..., 3] = (alfa * 190).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def kaydet(img, yol):
    hedef = DOKU / yol
    hedef.parent.mkdir(parents=True, exist_ok=True)
    img.save(hedef, optimize=True)
    print(f"{hedef.relative_to(KOK)} {img.size[0]}x{img.size[1]}")


if __name__ == "__main__":
    kaydet(bar_dokusu(), "gui/sicaklik/bar.png")
    kaydet(buz_dokusu(), "misc/sicaklik_buz.png")
    kaydet(sicak_dokusu(), "misc/sicaklik_sicak.png")
