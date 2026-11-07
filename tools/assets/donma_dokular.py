#!/usr/bin/env python3
"""Donma bloklarının ve kar tanesi parçacığının dokularını üretir (16x16 PNG, prosedürel, sabit tohumlu; Mojang dokusu
kopyalanmaz).

- kiragili_cimen_ust.png: serin yeşil çimen zemini üstüne beyaz-mavi kırağı iğneleri (1-3 piksellik çapraz çizgiler, tasarım
  notu §1: kristaller çimen uçlarında). Renklendirilmez: kırağı her biyomda aynı. Yanlar vanilla grass_block_snow, alt dirt.
- ince_buz.png: neredeyse kara, ~%70 saydam koyu buz; içinde ince beyaz çatlak ağı (rastgele yürüyüşlü dallanan çizgiler)
  ve seyrek hava kabarcıkları. Modeli translucent; altındaki su görünür.
- kar_tanesi_0..3.png (parçacık): 0 minik nokta kümesi, 1 küçük altı kollu yıldız, 2 dallı orta yıldız, 3 yumuşak yuvarlak
  tane. Kenarlar alfa ile yumuşatılır (piksel başına 4x4 alt örnek); renk merkezde beyaz, uçlara doğru açık buz mavisi.
  Saydam piksellerin rengi de beyaz-mavi kalır (süzmede koyu hale olmasın). Kare parçacıkta rastgele seçilir.

Kullanım (worktree kökünden): KOK/.venv/bin/python tools/assets/donma_dokular.py
Çıktı: src/main/resources/assets/dogalafetler/textures/block/{kiragili_cimen_ust,ince_buz}.png,
src/main/resources/assets/dogalafetler/textures/particle/kar_tanesi_{0..3}.png
"""
import math
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
KLASOR = KOK / "src/main/resources/assets/dogalafetler/textures/block"
PARCACIK_KLASORU = KOK / "src/main/resources/assets/dogalafetler/textures/particle"

YESILLER = [(86, 122, 70), (98, 136, 78), (110, 148, 88), (122, 158, 100), (92, 128, 84)]
KIRAGI = [(232, 240, 248), (214, 228, 242), (246, 250, 255)]
KAR_BOY = 16
ALT_ORNEK = 4


def kiragili_cimen():
    r = random.Random(0x4B49)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (*r.choice(YESILLER), 255)
    for _ in range(22):
        x, y = r.randrange(16), r.randrange(16)
        dx, dy = r.choice([(1, 1), (1, -1), (-1, 1), (1, 0), (0, 1)])
        for k in range(r.randint(1, 3)):
            px[(x + dx * k) % 16, (y + dy * k) % 16] = (*r.choice(KIRAGI), 255)
    return img


def ince_buz():
    r = random.Random(0x1CE)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = r.randint(-6, 6)
            px[x, y] = (18 + v, 30 + v, 46 + v, 96 + r.randint(-8, 8))
    for _ in range(4):
        x, y = r.randrange(16), r.randrange(16)
        dx, dy = r.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        for _ in range(r.randint(5, 11)):
            px[x % 16, y % 16] = (210 + r.randint(0, 30), 226 + r.randint(0, 24), 245, 190 + r.randint(0, 50))
            if r.random() < 0.3:
                dx, dy = r.choice([(1, 0), (0, 1), (1, 1), (1, -1), (-1, 1)])
            x, y = x + dx, y + dy
    for _ in range(5):
        x, y = r.randrange(16), r.randrange(16)
        px[x, y] = (150, 175, 200, 150)
    return img


def _parca_uzakligi(x, y, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - ax - dx * t, y - ay - dy * t)


def _yumusak(uzaklik, yaricap, kenar=0.7):
    return max(0.0, min(1.0, (yaricap - uzaklik) / kenar + 0.5))


def _yildiz(kol, yari_kalinlik, merkez, dal_orani=0.0, dal_boyu=0.0):
    """Altı kollu yıldızın örtü işlevi; kollar dikey eksenden başlar, isteğe bağlı 60 derecelik yan dallar."""
    parcalar = []
    for k in range(6):
        a = math.pi / 2 + k * math.pi / 3
        ux, uy = math.cos(a), math.sin(a)
        parcalar.append((0.0, 0.0, ux * kol, uy * kol, yari_kalinlik))
        if dal_orani:
            bx, by = ux * kol * dal_orani, uy * kol * dal_orani
            for s in (-1, 1):
                b = a + s * math.pi / 3
                parcalar.append((bx, by, bx + math.cos(b) * dal_boyu, by + math.sin(b) * dal_boyu, yari_kalinlik * 0.85))

    def ortu(x, y):
        a = _yumusak(math.hypot(x, y), merkez)
        for ax, ay, bx, by, w in parcalar:
            a = max(a, _yumusak(_parca_uzakligi(x, y, ax, ay, bx, by), w))
        return a

    return ortu


def _kar_karesi(ortu, renk_yaricapi, uc_sonumu):
    img = Image.new("RGBA", (KAR_BOY, KAR_BOY))
    px = img.load()
    m = KAR_BOY / 2
    for y in range(KAR_BOY):
        for x in range(KAR_BOY):
            toplam = 0.0
            for sy in range(ALT_ORNEK):
                for sx in range(ALT_ORNEK):
                    toplam += ortu(x + (sx + 0.5) / ALT_ORNEK - m, y + (sy + 0.5) / ALT_ORNEK - m)
            t = min(1.0, math.hypot(x + 0.5 - m, y + 0.5 - m) / renk_yaricapi)
            a = toplam / (ALT_ORNEK * ALT_ORNEK) * (1.0 - uc_sonumu * t)
            px[x, y] = (round(252 - 46 * t), round(254 - 26 * t), 255, round(255 * a))
    return img


def kar_taneleri():
    r = random.Random(0x4B41)
    noktalar = [(0.0, 0.0, 1.3)]
    for _ in range(4):
        aci, uzak = r.uniform(0, 2 * math.pi), r.uniform(1.8, 3.4)
        noktalar.append((math.cos(aci) * uzak, math.sin(aci) * uzak, r.uniform(0.6, 1.0)))

    def kume(x, y):
        return max(_yumusak(math.hypot(x - nx, y - ny), nr, 0.9) for nx, ny, nr in noktalar)

    faz = r.uniform(0, 2 * math.pi)
    kaburga = _yildiz(4.6, 0.45, 0.0)

    def yuvarlak(x, y):
        d = math.hypot(x, y)
        sinir = 4.6 + 0.45 * math.sin(6 * math.atan2(y, x) + faz)
        govde = max(0.0, 1.0 - (d / sinir) ** 2) ** 1.4 * 0.9
        return max(govde, 0.55 * kaburga(x, y))

    return [
        _kar_karesi(kume, 4.5, 0.15),
        _kar_karesi(_yildiz(3.8, 0.55, 1.2), 4.0, 0.2),
        _kar_karesi(_yildiz(6.6, 0.6, 1.6, 0.55, 2.4), 7.0, 0.25),
        _kar_karesi(yuvarlak, 5.5, 0.1),
    ]


def main():
    KLASOR.mkdir(parents=True, exist_ok=True)
    for ad, img in (("kiragili_cimen_ust", kiragili_cimen()), ("ince_buz", ince_buz())):
        yol = KLASOR / f"{ad}.png"
        img.save(yol, optimize=True)
        print(yol)
    PARCACIK_KLASORU.mkdir(parents=True, exist_ok=True)
    for i, img in enumerate(kar_taneleri()):
        yol = PARCACIK_KLASORU / f"kar_tanesi_{i}.png"
        img.save(yol, optimize=True)
        print(yol)


if __name__ == "__main__":
    main()
