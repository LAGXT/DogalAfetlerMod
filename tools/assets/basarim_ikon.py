#!/usr/bin/env python3
""""Doğal Afetler" başarım sekmesinin arka planı (madde 12): 16×16, kenarları birbirine uyan (döşenebilir) doku.

Koyu bazalt/kül zemin, döşemenin kenarlarından taşan fay çatlakları ve çatlak içinde kor parıltısı: deprem yarığı ile
meteor/yangın korunun ortak dili. Advancement ekranı dokuyu 16 GUI pikselde bir döşer; ikonlar okunaklı kalsın diye
kontrast düşüktür. Çıktı: src/main/resources/assets/dogalafetler/textures/gui/advancements/backgrounds/felaket.png

Kullanım (worktree kökünden): KÖK/.venv/bin/python tools/assets/basarim_ikon.py
Sabit tohum: her koşu aynı baytları üretir.
"""
import random
from pathlib import Path

from PIL import Image

BOYUT = 16
TOHUM = 20261106
W = Path(__file__).resolve().parents[2]
CIKTI = W / "src/main/resources/assets/dogalafetler/textures/gui/advancements/backgrounds/felaket.png"

ZEMIN = [(38, 33, 31), (44, 38, 35), (50, 43, 39), (41, 36, 34), (56, 47, 42)]
CATLAK = (18, 13, 12)
KOR = [(150, 58, 24), (196, 84, 30), (232, 134, 42)]

# Döşenebilir çatlak yolları (x, y) — kenardan çıkan uç karşı kenardan girer.
YOLLAR = [
    [(0, 5), (1, 5), (2, 6), (3, 6), (4, 7), (5, 7), (6, 8), (7, 8), (8, 8), (9, 9), (10, 9), (11, 10), (12, 10),
     (13, 10), (14, 11), (15, 11)],
    [(0, 11), (15, 11)],
    [(7, 8), (7, 9), (6, 10), (6, 11), (5, 12), (5, 13), (4, 14), (4, 15)],
    [(4, 0), (5, 1), (5, 2), (6, 3)],
    [(11, 10), (12, 9), (12, 8), (13, 7), (14, 6), (14, 5), (15, 4)],
    [(0, 4), (0, 5)],
]
KOR_NOKTALARI = {(3, 6): 1, (8, 8): 2, (9, 9): 1, (13, 10): 1, (6, 10): 0, (12, 9): 0, (5, 7): 0}


def uret():
    rastgele = random.Random(TOHUM)
    resim = Image.new("RGBA", (BOYUT, BOYUT))
    piksel = resim.load()
    for y in range(BOYUT):
        for x in range(BOYUT):
            r, g, b = ZEMIN[rastgele.randrange(len(ZEMIN))]
            oynama = rastgele.randint(-3, 3)
            piksel[x, y] = (r + oynama, g + oynama, b + oynama, 255)
    for yol in YOLLAR:
        for x, y in yol:
            piksel[x % BOYUT, y % BOYUT] = (*CATLAK, 255)
    for (x, y), kademe in KOR_NOKTALARI.items():
        piksel[x, y] = (*KOR[kademe], 255)
    # Kor ışığı: korun komşu zemin piksellerini hafifçe ısıt (döşeme sınırında sarılarak).
    sicak = [(x, y) for (x, y) in KOR_NOKTALARI]
    for x, y in sicak:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            kx, ky = (x + dx) % BOYUT, (y + dy) % BOYUT
            r, g, b, a = piksel[kx, ky]
            if (r, g, b) != CATLAK and (kx, ky) not in KOR_NOKTALARI:
                piksel[kx, ky] = (min(255, r + 18), min(255, g + 6), b, a)
    return resim


def main():
    CIKTI.parent.mkdir(parents=True, exist_ok=True)
    uret().save(CIKTI, optimize=True)
    print(f"yazıldı: {CIKTI.relative_to(W)} ({BOYUT}x{BOYUT})")


if __name__ == "__main__":
    main()
