#!/usr/bin/env python3
"""3B pulsarın dokularını üretir (pulsar-2 tur 4, PL-28; Java: client/gokyuzu/Pulsar3B).

Çalıştırma (worktree kökünden):
    KOK/.venv/bin/python tools/assets/pulsar_3b_dokulari.py

Çıktı: src/main/resources/assets/dogalafetler/textures/environment/gokyuzu/pulsar3b_*.png (+ .png.mcmeta). Yardımcılar
gokyuzu_dokulari.py'den gelir; tohum ayrı (20261110): eski dokuların baytları değişmez, yalnız yeni dosyalar yazılır.
Renk referans videodan: parlak piksellerin ortalaması buz mavisi-beyaz (219, 239, 247); ısınma (mor-beyaz) Java'da köşe
rengiyle verilir. RGB rengi, alfa yoğunluğu taşır; yüzey dokusu dışındakiler toplamalı çizilir. Yüzey dokusu ayrı
tohumla (20261111) üretilir: sonradan eklendi, öteki dokuların baytları değişmez.
"""
from __future__ import annotations

import numpy as np

from gokyuzu_dokulari import HEDEF, fbm, gurultu, izgara, kaydet, kutup, renk, yumusak_basamak

BUZ = renk("#DBEFF7")
BEYAZ = renk("#FFFFFF")


def gecis(c0, c1, t):
    """t (h, w) boyunca c0 → c1 renk geçişi."""
    t = np.clip(t, 0, 1)[..., None]
    return c0[None, None, :] * (1 - t) + c1[None, None, :] * t


def pulsar3b_koni(rng, w=512, h=64):
    """Işın konisi: u = 0 kutup → 1 uç (koni ekseni boyunca), v = koninin çevresi (dikşsiz, ince plazma lifleri).
    Eksen boyunca iç (kutba yakın) parlak, dışa doğru saydamlaşır, uçta yumuşak söner. Enine profil (ortası parlak,
    kenarı saydam) Java'da yüzey normali ile bakış açısından köşe alfasıyla verilir."""
    x, _ = izgara(w, h)
    u = (x + 1) / 2
    boy = (0.22 + 0.78 * np.exp(-u / 0.3)) * yumusak_basamak(0.0, 0.025, u) * yumusak_basamak(1.0, 0.68, u)
    lif = gurultu(h, w, 8, 5, rng, periyodik_x=True).T * 0.6 + gurultu(h, w, 19, 11, rng, periyodik_x=True).T * 0.4
    lif = 0.62 + 0.38 * yumusak_basamak(0.25, 0.85, lif)
    rgb = gecis(BEYAZ, BUZ, yumusak_basamak(0.0, 0.35, u))
    rgb = rgb * (1 - 0.25 * yumusak_basamak(0.4, 1.0, u)[..., None]) + renk("#BCD2FF")[None, None, :] * (
        0.25 * yumusak_basamak(0.4, 1.0, u)[..., None])
    kaydet("pulsar3b_koni", rgb, np.clip(boy * lif, 0, 1))


def pulsar3b_parcacik(boyut=64):
    """Plazma parçacığı: sıcak beyaz çekirdek, buz mavisi yumuşak kenar."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    a = (np.exp(-(r / 0.3) ** 2) + 0.35 * np.exp(-r / 0.2)) * yumusak_basamak(1.0, 0.78, r)
    kaydet("pulsar3b_parcacik", gecis(BEYAZ, BUZ, r / 0.6), np.clip(a, 0, 1))


def pulsar3b_hale_katman(rng, boyut=256):
    """Hacimsel hale katmanı: kesişen bilboardlar ve kameraya dönük derinlik katmanları üst üste toplanınca hacim
    oluşturur. Görünür alfa yarıçapın ~0,85'ine kadar; ince bulutımsı lifler katmanlar arasında farklı düşsün diye
    açıya ve yarıçapa bağlı."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    bulut = 0.72 + 0.28 * fbm(boyut, boyut, 10, 10, 4, rng)
    a = (np.exp(-(r / 0.5) ** 2) * 0.75 + np.exp(-r / 0.28) * 0.32) * bulut * yumusak_basamak(1.0, 0.84, r)
    kaydet("pulsar3b_hale_katman", gecis(BEYAZ, BUZ, r / 0.7), np.clip(a, 0, 1))


def pulsar3b_alan(rng, w=256, h=32):
    """Manyetik alan çizgisi şerit dokusu: u çizgi boyunca (dikşsiz; Java akan plazma için kaydırır), v enine (ortası
    parlak ince çizgi, kenarlar yumuşak). Kenar alfası 0: kaydırmada sarılma dikş göstermez."""
    x, y = izgara(w, h)
    dugum = gurultu(w, 1, 14, 1, rng, periyodik_x=True)[0] * 0.6 + gurultu(w, 1, 37, 1, rng, periyodik_x=True)[0] * 0.4
    dugum = 0.5 + 0.5 * yumusak_basamak(0.3, 0.8, dugum)[None, :]
    en = np.exp(-(y / 0.28) ** 2) + 0.22 * np.exp(-(y / 0.65) ** 2)
    a = en * dugum * yumusak_basamak(1.0, 0.82, np.abs(y))
    kaydet("pulsar3b_alan", gecis(BEYAZ, renk("#C4D4FF"), np.abs(y) / 0.6), np.clip(a, 0, 1), clamp=False)


def periyodik_bant(boyut, merkez, genislik, rng):
    """İki eksende dikişsiz bant geçiren gürültü (beyaz gürültünün FFT süzgeci), standart sapması 1."""
    f = np.fft.fft2(rng.standard_normal((boyut, boyut)))
    fy = np.fft.fftfreq(boyut)[:, None] * boyut
    fx = np.fft.fftfreq(boyut)[None, :] * boyut
    n = np.real(np.fft.ifft2(f * np.exp(-((np.hypot(fx, fy) - merkez) / genislik) ** 2)))
    return n / n.std()


def pulsar3b_yuzey(rng, boyut=256):
    """Çekirdek yüzeyi (normal karışım, opak): iki eksende döşenebilir granülasyon/plazma kaynaması, gri. Java bunu
    köşe rengiyle çarpar; parlaklık 0,6–1 (ortalama ~0,8): ortalamaya göre ±%25. Koyu granül arası damarlar."""
    granul = periyodik_bant(boyut, 14, 4.5, rng)
    buyuk = periyodik_bant(boyut, 4, 1.5, rng)
    ince = periyodik_bant(boyut, 38, 10, rng)
    damar = np.exp(-(granul / 0.35) ** 2) * yumusak_basamak(0.4, -0.6, buyuk) * 0.5
    n = 0.6 * np.tanh(0.8 * granul) + 0.25 * np.tanh(0.7 * buyuk) + 0.15 * np.tanh(ince) - damar
    alt, ust = np.percentile(n, [1, 99])
    n = np.clip((n - alt) / (ust - alt), 0, 1)
    g = 0.6 + 0.4 * n
    kaydet("pulsar3b_yuzey", np.repeat(g[..., None], 3, axis=2), np.ones_like(g), clamp=False)


def main() -> None:
    print(f"Hedef: {HEDEF}")
    rng = np.random.default_rng(20261110)
    pulsar3b_koni(rng)
    pulsar3b_parcacik()
    pulsar3b_hale_katman(rng)
    pulsar3b_alan(rng)
    pulsar3b_yuzey(np.random.default_rng(20261111))


if __name__ == "__main__":
    main()
