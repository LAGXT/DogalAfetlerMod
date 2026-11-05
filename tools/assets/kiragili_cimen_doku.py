#!/usr/bin/env python3
"""dogalafetler:kiragili_cimen üst dokusunu üretir (16x16 PNG, prosedürel; Mojang dokusu kopyalanmaz).

Zemin: serin yeşil çimen tonları (sabit tohumlu gürültü). Üstüne beyaz-mavi kırağı iğneleri: rastgele başlangıç noktalarından
1-3 piksellik çapraz çizgiler. Doku renklendirilmez (biyom rengi uygulanmaz), kırağı her biyomda aynı görünür.
Yanlar vanilla `grass_block_snow`, alt vanilla `dirt` dokusunu modelden kullanır.

Kullanım (KÖK'ten): KOK/.venv/bin/python tools/assets/kiragili_cimen_doku.py
Çıktı: src/main/resources/assets/dogalafetler/textures/block/kiragili_cimen_ust.png
"""
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/assets/dogalafetler/textures/block/kiragili_cimen_ust.png"

YESILLER = [(86, 122, 70), (98, 136, 78), (110, 148, 88), (122, 158, 100), (92, 128, 84)]
KIRAGI = [(232, 240, 248), (214, 228, 242), (246, 250, 255)]


def main():
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
    CIKTI.parent.mkdir(parents=True, exist_ok=True)
    img.save(CIKTI, optimize=True)
    print(CIKTI)


if __name__ == "__main__":
    main()
