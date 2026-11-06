#!/usr/bin/env python3
"""Donma bloklarının dokularını üretir (16x16 PNG, prosedürel, sabit tohumlu; Mojang dokusu kopyalanmaz).

- kiragili_cimen_ust.png: serin yeşil çimen zemini üstüne beyaz-mavi kırağı iğneleri (1-3 piksellik çapraz çizgiler, tasarım
  notu §1: kristaller çimen uçlarında). Renklendirilmez: kırağı her biyomda aynı. Yanlar vanilla grass_block_snow, alt dirt.
- ince_buz.png: neredeyse kara, ~%70 saydam koyu buz; içinde ince beyaz çatlak ağı (rastgele yürüyüşlü dallanan çizgiler)
  ve seyrek hava kabarcıkları. Modeli translucent; altındaki su görünür.

Kullanım (worktree kökünden): KOK/.venv/bin/python tools/assets/donma_dokular.py
Çıktı: src/main/resources/assets/dogalafetler/textures/block/{kiragili_cimen_ust,ince_buz}.png
"""
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
KLASOR = KOK / "src/main/resources/assets/dogalafetler/textures/block"

YESILLER = [(86, 122, 70), (98, 136, 78), (110, 148, 88), (122, 158, 100), (92, 128, 84)]
KIRAGI = [(232, 240, 248), (214, 228, 242), (246, 250, 255)]


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


def main():
    KLASOR.mkdir(parents=True, exist_ok=True)
    for ad, img in (("kiragili_cimen_ust", kiragili_cimen()), ("ince_buz", ince_buz())):
        yol = KLASOR / f"{ad}.png"
        img.save(yol, optimize=True)
        print(yol)


if __name__ == "__main__":
    main()
