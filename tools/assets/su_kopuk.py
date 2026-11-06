#!/usr/bin/env python3
"""Sanal su köpük dokusu: assets/dogalafetler/textures/block/su_kopuk.png (16x256, 16 kare) + .mcmeta.

Kıyı şeridi, dalga tepesi ve kıvrılan dudak üstüne bindirilen beyaz köpük (SU-28). Döşenebilir: yalnız tam sayı frekanslı
sinüs toplamı, kareler arasında faz kayar (köpük kabarcıkları yavaşça akar, son kare ilkine bağlanır). Alfa kanalı köpük
yoğunluğudur; renk beyazdan çok açık mavi-griye. Blok atlası `block/` klasörünü bütün ad alanlarından toplar, model gerekmez.

Kullanım (worktree kökünden): KÖK/.venv/bin/python tools/assets/su_kopuk.py
Değiştirmek: KARE, frekans listesi ve eşik aşağıda; aynı adla 16 piksel genişliğinde dikey şerit yazılmalı.
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

KOK = Path(__file__).resolve().parents[2]
HEDEF = KOK / "src/main/resources/assets/dogalafetler/textures/block/su_kopuk.png"
KARE = 16
BOY = 16
# (fx, fy, genlik, faz hızı): tam sayı frekanslar döşenebilirlik içindir.
BILESENLER = [(1, 2, 1.0, 1), (3, 1, 0.7, -1), (2, 3, 0.55, 2), (5, 4, 0.35, -2), (4, 7, 0.25, 3), (7, 5, 0.18, -3)]


def kare(k):
    y, x = np.mgrid[0:BOY, 0:BOY] / BOY * 2 * math.pi
    faz = k / KARE * 2 * math.pi
    alan = np.zeros((BOY, BOY))
    for fx, fy, g, h in BILESENLER:
        alan += g * np.sin(fx * x + fy * y + h * faz)
        alan += 0.6 * g * np.cos(fy * x - fx * y - h * faz + 1.7)
    alan = (alan - alan.min()) / (alan.max() - alan.min())
    yogun = np.clip((alan - 0.38) / 0.4, 0, 1) ** 1.4
    rgba = np.zeros((BOY, BOY, 4), dtype=np.uint8)
    renk = 235 + 20 * yogun
    rgba[..., 0] = np.clip(renk - 8 * (1 - yogun), 0, 255)
    rgba[..., 1] = np.clip(renk - 3 * (1 - yogun), 0, 255)
    rgba[..., 2] = 255
    rgba[..., 3] = np.clip(40 + 215 * yogun, 0, 255)
    return rgba


def main():
    HEDEF.parent.mkdir(parents=True, exist_ok=True)
    serit = np.concatenate([kare(k) for k in range(KARE)], axis=0)
    Image.fromarray(serit, "RGBA").save(HEDEF)
    meta = {"animation": {"frametime": 3, "interpolate": True}}
    HEDEF.with_suffix(".png.mcmeta").write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    print(HEDEF, serit.shape)


if __name__ == "__main__":
    main()
