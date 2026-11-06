#!/usr/bin/env python3
"""Asit yağmuru ve orman yangını afetlerinin dokularını üretir (tasarım notları asit_yagmuru.md §2, orman_yangini.md §2).

- textures/environment/asit_yagmuru.png (64x256): yağış sütunu dokusu; vanilla rain.png düzeni (u bir sütun, v 4 blokta
  tekrar), dikeyde kesintisiz döşenir. Damlalar sarımsı yeşil #C8E04A, parlak çekirdekli; vanilla'dan ~%15 sık.
- textures/particle/asit_sicramasi_0..3 (8x8): sıçrayan damla; renk parçacıkta verilir (beyaz taban).
- textures/particle/asit_dumani_0..3 (16x16): cızlama dumancığı ve yüzey buharı (beyaz, yumuşak kenar).
- textures/particle/asit_bulutu_0..3 (64x64): bulut tavanı (beyaz, renk parçacıkta; alfa geniş halkada sıfıra iner).
- textures/misc/isi_dalgasi.png (64x64): orman yangını ısı dalgalanması bandı, her iki yönde kesintisiz döşenir.

Tohumlu ve deterministiktir: iki üretimde dosyalar bayt bayt aynıdır. Çalıştırma: `KOK/.venv/bin/python tools/assets/asit_yagmuru_dokulari.py`.
"""
import math
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2] / "src/main/resources/assets/dogalafetler/textures"


def yumusak(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def gurultu(rng, boy, olcek):
    """Döşenebilir değer gürültüsü 0..1 (kaba ızgara + iki doğrusal ara değerleme)."""
    kaba = [[rng.random() for _ in range(olcek)] for _ in range(olcek)]
    deger = [[0.0] * boy for _ in range(boy)]
    for y in range(boy):
        for x in range(boy):
            fx = x / boy * olcek
            fy = y / boy * olcek
            x0, y0 = int(fx), int(fy)
            tx, ty = yumusak(fx - x0), yumusak(fy - y0)
            a = kaba[y0 % olcek][x0 % olcek]
            b = kaba[y0 % olcek][(x0 + 1) % olcek]
            c = kaba[(y0 + 1) % olcek][x0 % olcek]
            d = kaba[(y0 + 1) % olcek][(x0 + 1) % olcek]
            deger[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return deger


def yagmur(rng):
    gen, boy = 64, 256
    img = Image.new("RGBA", (gen, boy), (0, 0, 0, 0))
    px = img.load()
    for _ in range(74):
        x = rng.randrange(1, gen - 1)
        y0 = rng.randrange(boy)
        uzunluk = rng.randrange(7, 16)
        guc = 0.55 + rng.random() * 0.4
        for i in range(uzunluk):
            y = (y0 + i) % boy
            t = i / (uzunluk - 1)
            alfa = guc * math.sin(math.pi * min(1.0, t * 1.15)) ** 0.6
            cekirdek = 0.35 * (1 - abs(t - 0.6) * 2)
            r = int(min(255, 200 + 55 * cekirdek))
            g = int(min(255, 224 + 31 * cekirdek))
            b = int(min(255, 74 + 120 * cekirdek))
            px[x, y] = (r, g, b, int(255 * alfa))
            yan = int(255 * alfa * 0.25)
            for dx in (-1, 1):
                eski = px[x + dx, y]
                if eski[3] < yan:
                    px[x + dx, y] = (200, 224, 74, yan)
    return img


def sicrama(kare, rng):
    boy = 8
    img = Image.new("RGBA", (boy, boy), (0, 0, 0, 0))
    px = img.load()
    noktalar = [(3.5, 3.5, 1.6 - kare * 0.25)]
    for _ in range(kare + 1):
        aci = rng.random() * math.tau
        u = 1.4 + kare * 0.6
        noktalar.append((3.5 + math.cos(aci) * u, 3.5 + math.sin(aci) * u * 0.6, 0.8))
    for y in range(boy):
        for x in range(boy):
            a = 0.0
            for cx, cy, r in noktalar:
                d = math.hypot(x - cx, y - cy)
                a = max(a, yumusak((r - d) / 0.9 + 0.5))
            if x in (0, boy - 1) or y in (0, boy - 1):
                a = min(a, 0.2)
            px[x, y] = (255, 255, 255, int(255 * a))
    return img


def puf(kare, rng, boy, ic):
    g = gurultu(rng, boy, 4)
    img = Image.new("RGBA", (boy, boy), (0, 0, 0, 0))
    px = img.load()
    m = (boy - 1) / 2
    for y in range(boy):
        for x in range(boy):
            d = math.hypot(x - m, y - m) / (m + 0.5)
            kenar = yumusak((1.0 - d) / (1.0 - ic))
            a = kenar * (0.55 + 0.45 * g[y][x]) * (1.0 - 0.08 * kare)
            if x < 2 or y < 2 or x >= boy - 2 or y >= boy - 2:
                a = 0.0
            p = int(235 + 20 * g[y][x])
            px[x, y] = (p, p, p, int(255 * max(0.0, min(1.0, a))))
    return img


def isi_dalgasi(rng):
    boy = 64
    g = gurultu(rng, boy, 8)
    img = Image.new("RGBA", (boy, boy), (0, 0, 0, 0))
    px = img.load()
    for y in range(boy):
        for x in range(boy):
            dalga = 0.5 + 0.5 * math.sin((y / boy) * math.tau * 4 + math.sin(x / boy * math.tau * 2) * 1.6 + g[y][x] * 2.5)
            a = 0.25 + 0.75 * dalga ** 3
            px[x, y] = (255, int(214 + 30 * dalga), int(170 + 50 * dalga), int(255 * a))
    return img


def main():
    rng = random.Random(0xA517)
    cevre = KOK / "environment"
    parcacik = KOK / "particle"
    misc = KOK / "misc"
    for d in (cevre, parcacik, misc):
        d.mkdir(parents=True, exist_ok=True)
    yagmur(rng).save(cevre / "asit_yagmuru.png")
    for k in range(4):
        sicrama(k, rng).save(parcacik / f"asit_sicramasi_{k}.png")
        puf(k, rng, 16, 0.35).save(parcacik / f"asit_dumani_{k}.png")
        puf(k, rng, 64, 0.45).save(parcacik / f"asit_bulutu_{k}.png")
    isi_dalgasi(rng).save(misc / "isi_dalgasi.png")
    print(f"14 doku yazıldı: {KOK}")


if __name__ == "__main__":
    main()
