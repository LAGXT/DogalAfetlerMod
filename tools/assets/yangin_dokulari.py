#!/usr/bin/env python3
"""Yangın parçacık dokularını üretir (madde 6, tasarım notu §5): 4 kare alev (16x16), 4 kare duman (32x32; sütun ve pirokümülüs de kullanır) ve 2 kare kıvılcım (8x8).

Kullanım: python3 tools/assets/yangin_dokulari.py  (kökten). Aynı tohumla aynı PNG'leri yazar.
"""
import math
import random
from pathlib import Path

from PIL import Image

HEDEF = Path(__file__).resolve().parents[2] / "src/main/resources/assets/dogalafetler/textures/particle"


def gurultu(rng, boy, olcek):
    kaba = [[rng.random() for _ in range(olcek + 1)] for _ in range(olcek + 1)]

    def deger(x, y):
        gx, gy = x / boy * olcek, y / boy * olcek
        x0, y0 = int(gx), int(gy)
        tx, ty = gx - x0, gy - y0
        a = kaba[y0][x0] * (1 - tx) + kaba[y0][x0 + 1] * tx
        b = kaba[y0 + 1][x0] * (1 - tx) + kaba[y0 + 1][x0 + 1] * tx
        return a * (1 - ty) + b * ty

    return deger


def kenar_sonumu(d, ic=0.6):
    """Merkezden normalize uzaklık d için 1 (d <= ic) → 0 (d >= 1): büyük quad'ın kenarı görünmez."""
    t = min(1.0, max(0.0, (1.0 - d) / (1.0 - ic)))
    return t * t * (3 - 2 * t)


def alev(kare, rng):
    boy = 16
    img = Image.new("RGBA", (boy, boy))
    n = gurultu(rng, boy, 4)
    for y in range(boy):
        for x in range(boy):
            yukseklik = 1 - y / (boy - 1)  # 0 alt, 1 üst
            genislik = 0.42 * (1 - yukseklik ** 1.4) + 0.05
            dx = abs(x + 0.5 - boy / 2) / boy
            kenar = genislik - dx + (n(x, y) - 0.5) * 0.18 - kare * 0.01
            if kenar <= 0:
                continue
            sicak = min(1.0, kenar / genislik + (1 - yukseklik) * 0.3)
            r = 255
            g = int(90 + 165 * sicak ** 1.5)
            b = int(30 + 170 * max(0.0, sicak - 0.7) / 0.3) if sicak > 0.7 else 30
            # kenar satır ve sütunlarda alfa düşer: sprite kenarı sert kesilmesin
            alt = min(1.0, (boy - 1 - y + 0.5) / 2.5, (y + 0.5) / 2.5)
            yan = min(1.0, (min(x, boy - 1 - x) + 0.5) / 1.5)
            a = int(255 * min(1.0, kenar * 9) * alt * yan)
            img.putpixel((x, y), (r, min(255, g), min(255, b), a))
    return img


def duman(kare, rng):
    boy = 32
    img = Image.new("RGBA", (boy, boy))
    n = gurultu(rng, boy, 5)
    for y in range(boy):
        for x in range(boy):
            d = math.hypot(x + 0.5 - boy / 2, y + 0.5 - boy / 2) / (boy / 2)
            yogun = ((1 - d) * 1.3 + (n(x, y) - 0.5) * 0.9 - kare * 0.08) * kenar_sonumu(d)
            if yogun <= 0:
                continue
            ton = int(150 + 70 * n(y, x))
            img.putpixel((x, y), (ton, ton, ton, int(255 * min(1.0, yogun) ** 1.2)))
    return img


def kivilcim(kare):
    """8x8 kıvılcım: beyaz çekirdek, yumuşak kenar; renk parçacıkta verilir (sarıdan kızıla)."""
    boy = 8
    img = Image.new("RGBA", (boy, boy))
    yaricap = 3.2 - kare * 0.7
    for y in range(boy):
        for x in range(boy):
            d = math.hypot(x + 0.5 - boy / 2, y + 0.5 - boy / 2) / yaricap
            if d >= 1:
                continue
            a = (1 - d) ** 1.5
            img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    return img


def main():
    HEDEF.mkdir(parents=True, exist_ok=True)
    for kare in range(4):
        alev(kare, random.Random(100 + kare)).save(HEDEF / f"yangin_alevi_{kare}.png")
        duman(kare, random.Random(200 + kare)).save(HEDEF / f"yangin_dumani_{kare}.png")
    for kare in range(2):
        kivilcim(kare).save(HEDEF / f"yangin_kivilcimi_{kare}.png")
    print(f"10 doku yazıldı: {HEDEF}")


if __name__ == "__main__":
    main()
