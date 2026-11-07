#!/usr/bin/env python3
"""Ay çarpmasının gök dokularını üretir (bölüm H, docs/tasarim/ay_carpmasi.md §2, N19).

Çalıştırma (worktree kökünden):
    ../../.venv/bin/python tools/assets/ay_carpmasi_dokular.py

Çıktı: src/main/resources/assets/dogalafetler/textures/environment/ay_carpmasi/*.png (+ .png.mcmeta, doğrusal süzme).
Aynı tohumla her çalıştırmada aynı baytlar üretilir. RGB rengi, alfa yoğunluğu taşır; renk oyunda köşe rengiyle çarpılır.

| Dosya | Boyut | Karışım | İçerik |
|---|---|---|---|
| ay_yuzey.png | 256x256 | normal | denizli (maria), kraterli gri ay diski, kenar kararması; disk karenin 0,92'si |
| ay_catlak_1..3.png | 256x256 | toplamalı | parlayan turuncu-beyaz kırık ağı, üç aşama (seyrek → yoğun + parlama) |
| ay_kizil.png | 256x256 | toplamalı | atmosfere giren alt kenarın kızarması (alttan yukarı sönen disk) |
| ay_parca.png | 64x64 | normal | düzensiz kenarlı, soldan-üstten aydınlanan kaya parçası |
| ay_hare.png | 128x128 | toplamalı | yumuşak ışıma (toz haresi, ufuk parlaması, kızgın çekirdek, halka parıltısı) |
| enkaz_halkasi.png | 512x32 | toplamalı | yatay tekrarlanan ince enkaz şeridi (u yönünde periyodik) |
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

KOK = Path(__file__).resolve().parents[2]
HEDEF = KOK / "src/main/resources/assets/dogalafetler/textures/environment/ay_carpmasi"
DISK = 0.46


def gurultu(w: int, h: int, gx: int, gy: int, rng: np.random.Generator, periyodik_x: bool = False) -> np.ndarray:
    """Yumuşatılmış değer gürültüsü, [0,1]."""
    g = rng.random((gy + 2, gx + 2), dtype=np.float32)
    if periyodik_x:
        g[:, gx] = g[:, 0]
        g[:, gx + 1] = g[:, 1]
    x = np.arange(w, dtype=np.float32) * gx / w
    y = np.arange(h, dtype=np.float32) * gy / h
    xi, yi = x.astype(np.int32), y.astype(np.int32)
    xf, yf = x - xi, y - yi
    xf = xf * xf * (3 - 2 * xf)
    yf = yf * yf * (3 - 2 * yf)
    g00, g10 = g[np.ix_(yi, xi)], g[np.ix_(yi, xi + 1)]
    g01, g11 = g[np.ix_(yi + 1, xi)], g[np.ix_(yi + 1, xi + 1)]
    ust = g00 + (g10 - g00) * xf[None, :]
    alt = g01 + (g11 - g01) * xf[None, :]
    return ust + (alt - ust) * yf[:, None]


def fbm(w, h, gx, gy, oktav, rng, periyodik_x=False, kazanc=0.5):
    toplam = np.zeros((h, w), np.float32)
    genlik, norm = 1.0, 0.0
    for o in range(oktav):
        toplam += genlik * gurultu(w, h, gx << o, gy << o, rng, periyodik_x)
        norm += genlik
        genlik *= kazanc
    return toplam / norm


def koordinat(n: int):
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    x = (x + 0.5) / n - 0.5
    y = (y + 0.5) / n - 0.5
    return x, y, np.sqrt(x * x + y * y)


def disk_maskesi(n: int) -> np.ndarray:
    _, _, r = koordinat(n)
    return np.clip((DISK - r) * n * 0.8, 0, 1)


def bulanik(dizi: np.ndarray, yaricap: float) -> np.ndarray:
    img = Image.fromarray((np.clip(dizi, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(yaricap))
    return np.asarray(img).astype(np.float32) / 255


def kaydet(ad: str, rgb: np.ndarray, alfa: np.ndarray, clamp: bool = True) -> None:
    dizi = np.dstack([np.clip(rgb, 0, 1), np.clip(alfa, 0, 1)[..., None]])
    veri = (dizi * 255 + 0.5).astype(np.uint8)
    HEDEF.mkdir(parents=True, exist_ok=True)
    Image.fromarray(veri, "RGBA").save(HEDEF / f"{ad}.png", optimize=True)
    (HEDEF / f"{ad}.png.mcmeta").write_text(
        json.dumps({"texture": {"blur": True, "clamp": clamp}}, indent=2) + "\n", encoding="utf-8")
    print(f"  {ad}.png  {veri.shape[1]}x{veri.shape[0]}")


def ay_yuzey() -> None:
    n = 256
    rng = np.random.default_rng(20261106)
    x, y, r = koordinat(n)
    maske = disk_maskesi(n)
    taban = 0.70 + 0.16 * (fbm(n, n, 6, 6, 5, rng) - 0.5)
    # Denizler (maria): düşük frekanslı, belirgin koyu lekeler.
    deniz = np.clip((fbm(n, n, 3, 3, 3, rng) - 0.52) * 5, 0, 1)
    parlaklik = taban - 0.24 * deniz
    # Kraterler: koyu taban, parlak kenar, küçüklerden çok.
    for _ in range(70):
        cx, cy = rng.uniform(-0.42, 0.42, 2)
        cr = 0.006 + 0.05 * rng.random() ** 3
        d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2) / cr
        parlaklik -= 0.10 * np.clip(1 - d, 0, 1) ** 0.6
        parlaklik += 0.12 * np.exp(-((d - 1.05) / 0.18) ** 2) * (0.5 + 0.5 * np.sign(-(x - cx) - (y - cy)))
    # Tycho benzeri ışınlı krater.
    aci = np.arctan2(y + 0.2, x - 0.08)
    isinlar = np.clip(np.sin(aci * 13) ** 8, 0, 1) * np.exp(-np.sqrt((x - 0.08) ** 2 + (y + 0.2) ** 2) * 9)
    parlaklik += 0.10 * isinlar
    kenar = np.clip(r / DISK, 0, 1)
    parlaklik *= 1 - 0.28 * kenar ** 3
    parlaklik = np.clip(parlaklik, 0, 1)
    rgb = np.dstack([parlaklik * 0.98, parlaklik * 0.98, parlaklik * 1.02])
    kaydet("ay_yuzey", rgb, maske)


def catlaklar() -> None:
    """Kesişen kırık ağı: sırtlı gürültünün ince çizgileri; her aşama bir öncekini içerir, sonuncusu parlar."""
    n = 256
    rng = np.random.default_rng(20261107)
    maske = disk_maskesi(n)
    x, y, r = koordinat(n)
    katmanlar = []
    for oktav, g, esik in ((2, 3, 0.035), (3, 5, 0.03), (4, 8, 0.028)):
        alan = fbm(n, n, g, g, oktav, rng)
        cizgi = np.clip(1 - np.abs(alan - 0.5) / esik, 0, 1) ** 1.5
        katmanlar.append(cizgi)
    birikim = np.zeros((n, n), np.float32)
    for i, cizgi in enumerate(katmanlar):
        birikim = np.maximum(birikim, cizgi)
        parilti = bulanik(birikim, 2.5 + 1.5 * i)
        alfa = np.clip(birikim + 0.8 * parilti, 0, 1) * maske * (1 - 0.3 * (r / DISK) ** 4)
        sicak = np.clip(birikim * 1.2, 0, 1)
        rgb = np.dstack([np.ones_like(sicak), 0.72 + 0.28 * sicak, 0.45 + 0.5 * sicak])
        kaydet(f"ay_catlak_{i + 1}", rgb, alfa)


def kizil() -> None:
    n = 256
    _, y, r = koordinat(n)
    maske = disk_maskesi(n)
    alttan = np.clip((y + 0.05) / 0.5, 0, 1) ** 1.4
    kenar = np.clip(r / DISK, 0, 1) ** 3
    alfa = maske * np.clip(alttan * (0.65 + 0.6 * kenar), 0, 1)
    kaydet("ay_kizil", np.ones((n, n, 3), np.float32), alfa)


def parca() -> None:
    n = 64
    rng = np.random.default_rng(20261108)
    x, y, r = koordinat(n)
    aci = np.arctan2(y, x)
    sinir = 0.30 + 0.07 * np.sin(aci * 3 + 0.7) + 0.05 * np.sin(aci * 5 + 2.1) + 0.04 * (gurultu(n, n, 5, 5, rng) - 0.5)
    alfa = np.clip((sinir - r) * n * 0.6, 0, 1)
    isik = np.clip(0.62 - 0.9 * (x + y) * 0.5 + 0.18 * (fbm(n, n, 4, 4, 3, rng) - 0.5), 0.15, 1)
    rgb = np.dstack([isik * 0.92, isik * 0.92, isik * 0.96])
    kaydet("ay_parca", rgb, alfa)


def hare() -> None:
    n = 128
    _, _, r = koordinat(n)
    alfa = np.exp(-(r / 0.16) ** 2) * 0.85 + np.exp(-(r / 0.32) ** 2) * 0.35
    alfa *= np.clip((0.5 - r) / 0.08, 0, 1)
    kaydet("ay_hare", np.ones((n, n, 3), np.float32), alfa)


def enkaz_halkasi() -> None:
    w, h = 512, 32
    rng = np.random.default_rng(20261110)
    v = (np.arange(h, dtype=np.float32) + 0.5) / h - 0.5
    profil = np.exp(-(v / 0.22) ** 2)[:, None]
    serit = 0.6 + 0.4 * gurultu(w, h, 64, 2, rng, periyodik_x=True)
    tane = gurultu(w, h, 256, 16, rng, periyodik_x=True)
    taneler = np.clip((tane - 0.82) * 6, 0, 1)
    alfa = np.clip(profil * (0.55 * serit + 0.6 * taneler), 0, 1)
    kaydet("enkaz_halkasi", np.ones((h, w, 3), np.float32), alfa, clamp=False)


def main() -> None:
    print(f"çıktı: {HEDEF.relative_to(KOK)}")
    ay_yuzey()
    catlaklar()
    kizil()
    parca()
    hare()
    enkaz_halkasi()


if __name__ == "__main__":
    main()
