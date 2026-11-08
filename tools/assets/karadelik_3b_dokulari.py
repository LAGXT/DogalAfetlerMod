#!/usr/bin/env python3
"""Pulsar-2 tur 4: 3B kara delik dokuları (Gargantua referansı `karadelik.png`; `KaraDelik3B`).

Çalıştırma (worktree kökünden):
    KOK/.venv/bin/python tools/assets/karadelik_3b_dokulari.py

Çıktı: src/main/resources/assets/dogalafetler/textures/environment/gokyuzu/karadelik_3b_{disk,tup,halka,yildiz}.png
(+ .png.mcmeta). Tohum ayrı (20261109): eski dokuların baytları değişmez. RGB rengi, alfa yoğunluğu taşır; hepsi
toplamalı çizilir (parlama silüetinde disk dokusu koyu renkle normal karışımla da kullanılır).

- disk: u = azimut (dikişsiz, UV kaydırılır: akış), v = 0 iç kenar (beyaz-sıcak) → 1 dış kenar (koyu turuncu), sarmal
  iplikler. Merceklenmiş arka yarının yayları aynı dokuyu UV haritasıyla kullanır (ayrı yay dokusu yok).
- tup: spagetti tüpü; u boyuna (dikişsiz, kayar), v çevre (dikişsiz).
- halka: ince foton halkası ve Einstein halkası şeridi; v enine Gauss profili.
- yildiz: merceklenen yıldız sprite'ı (yumuşak nokta).
"""
from __future__ import annotations

import numpy as np

from gokyuzu_dokulari import HEDEF, izgara, kaydet, renk, sirtli, yumusak_basamak


def periyodik_gurultu(w, h, gx, gy, rng):
    """Her iki eksende dikişsiz değer gürültüsü, [0,1]."""
    g = rng.random((gy, gx), dtype=np.float32)
    x = np.arange(w, dtype=np.float32) * gx / w
    y = np.arange(h, dtype=np.float32) * gy / h
    xi = x.astype(np.int32)
    yi = y.astype(np.int32)
    xf = x - xi
    yf = y - yi
    xf = xf * xf * (3 - 2 * xf)
    yf = yf * yf * (3 - 2 * yf)
    x1 = (xi + 1) % gx
    y1 = (yi + 1) % gy
    ust = g[np.ix_(yi, xi)] + (g[np.ix_(yi, x1)] - g[np.ix_(yi, xi)]) * xf[None, :]
    alt = g[np.ix_(y1, xi)] + (g[np.ix_(y1, x1)] - g[np.ix_(y1, xi)]) * xf[None, :]
    return ust + (alt - ust) * yf[:, None]


def renk_rampasi(v, duraklar):
    """v ∈ [0,1] için (konum, renk) duraklarından doğrusal renk."""
    konum = np.array([d[0] for d in duraklar], np.float32)
    rgb = np.stack([renk(d[1]) for d in duraklar])
    return np.stack([np.interp(v, konum, rgb[:, k]) for k in range(3)], axis=-1).astype(np.float32)


def disk(rng, w=1024, h=128):
    """Akresyon diski akış dokusu: iç kenar beyaz-sıcak, dış kenar koyu turuncu; sarmal iplikler (satır başına sabit
    kayma u'da dikişsizliği bozmaz)."""
    x, y = izgara(w, h)
    v = (y + 1) * 0.5
    iplik = sirtli(w, h, 10, 40, 3, rng, periyodik_x=True)
    ince = sirtli(w, h, 24, 96, 2, rng, periyodik_x=True)
    kayma = (np.arange(h) * 0.35 * w / h).astype(np.int32)
    for j in range(h):
        iplik[j] = np.roll(iplik[j], kayma[j])
        ince[j] = np.roll(ince[j], 2 * kayma[j])
    rgb = renk_rampasi(v, [(0.0, "#FFFAF2"), (0.18, "#FFEAC4"), (0.42, "#FFC27A"), (0.7, "#F0802E"),
                           (1.0, "#8A2E0A")])
    profil = 0.3 + 0.7 * (1 - v) ** 1.3
    a = profil * (0.42 + 0.4 * yumusak_basamak(0.25, 0.85, iplik) + 0.18 * ince)
    a *= yumusak_basamak(0.0, 0.06, v) * yumusak_basamak(1.0, 0.8, v)
    kaydet("karadelik_3b_disk", rgb, np.clip(a, 0, 1), clamp=False)


def tup(rng, w=256, h=64):
    """Spagetti tüpü: boyuna uzanan plazma iplikleri; renk oyunda köşe rengiyle verilir."""
    iplik = periyodik_gurultu(w, h, 6, 16, rng)
    damar = 1.0 - np.abs(2.0 * periyodik_gurultu(w, h, 12, 32, rng) - 1.0)
    a = 0.35 + 0.4 * yumusak_basamak(0.3, 0.8, iplik) + 0.25 * damar ** 3
    rgb = np.ones((h, w, 3), np.float32) * renk("#FFF4E6")[None, None, :]
    kaydet("karadelik_3b_tup", rgb, np.clip(a, 0, 1), clamp=False)


def halka(w=16, h=64):
    """İnce halka şeridi: enine Gauss çekirdek + geniş soluk örtü, kenarda 0."""
    _, y = izgara(w, h)
    a = np.exp(-(y / 0.28) ** 2) + 0.25 * np.exp(-(y / 0.7) ** 2)
    a *= yumusak_basamak(1.0, 0.9, np.abs(y))
    kaydet("karadelik_3b_halka", np.ones((h, w, 3), np.float32), np.clip(a / 1.25, 0, 1))


def yildiz(boyut=32):
    """Merceklenen yıldız: keskin çekirdek + yumuşak ışıma (uzatılınca yay olur)."""
    x, y = izgara(boyut, boyut)
    r2 = x ** 2 + y ** 2
    a = np.exp(-r2 / 0.02) + 0.35 * np.exp(-r2 / 0.15)
    a *= yumusak_basamak(1.0, 0.85, np.sqrt(r2))
    kaydet("karadelik_3b_yildiz", np.ones((boyut, boyut, 3), np.float32), np.clip(a, 0, 1))


def main() -> None:
    print(f"Hedef: {HEDEF}")
    rng = np.random.default_rng(20261109)
    disk(rng)
    tup(rng)
    halka()
    yildiz()


if __name__ == "__main__":
    main()
