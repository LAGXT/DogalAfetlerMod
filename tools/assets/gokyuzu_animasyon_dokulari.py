#!/usr/bin/env python3
"""Gökyüzü animasyon dokularını üretir: kayan yıldız, galaksi kol/halka katmanı, kara delik diski ve mercek halkası,
gezegenler (küre dokuları, halka, uydu).

Çalıştırma (KÖK'ten):
    KÖK/.venv/bin/python tools/assets/gokyuzu_animasyon_dokulari.py

Çıktı: src/main/resources/assets/dogalafetler/textures/environment/gokyuzu/*.png (+ .png.mcmeta). Önceki dokuları
(gokyuzu_dokulari.py) değiştirmez; kendi tohumuyla aynı baytları üretir. RGB rengi, alfa yoğunluğu taşır.
Kol/halka/disk/kayan yıldız toplamalı, gezegen ve uydu normal karışımla çizilir; dönüş oyunda uv/geometri
dönüşüyle yapılır (doku sabit).
"""
from __future__ import annotations

import numpy as np

from gokyuzu_dokulari import (fbm, gurultu, izgara, karistir, kaydet, kutup, renk, sirtli, yumusak_basamak)

GEZ_W, GEZ_H = 256, 128


def uc_renk(t, duraklar):
    """t (dizi, [0,1]) için (konum, renk) duraklarından doğrusal renk geçişi."""
    t = np.clip(t, 0, 1)
    sonuc = np.zeros(t.shape + (3,), np.float32)
    for (t0, c0), (t1, c1) in zip(duraklar[:-1], duraklar[1:]):
        m = (t >= t0) & (t <= t1)
        f = ((t - t0) / max(t1 - t0, 1e-6))[..., None]
        sonuc = np.where(m[..., None], c0[None, None, :] * (1 - f) + c1[None, None, :] * f, sonuc)
    return sonuc


# ---------------------------------------------------------------- kayan yıldız

def kayan_yildiz(w=256, h=32):
    """u: kuyruk (0) → baş (1); v: yanal. İnce çizgi + başta yumuşak parıltı. Toplamalı."""
    u = (np.arange(w, dtype=np.float32) + 0.5) / w
    v = (np.arange(h, dtype=np.float32) + 0.5) / h * 2 - 1
    U, V = np.meshgrid(u, v)
    cizgi = np.exp(-(V / 0.11) ** 2) * (U ** 2.4)
    bas = np.exp(-(((U - 0.965) / 0.036) ** 2 + (V / 0.38) ** 2))
    a = (cizgi + 1.3 * bas) * yumusak_basamak(0.0, 0.12, U) * yumusak_basamak(1.0, 0.85, np.abs(V))
    t = np.clip(U[..., None], 0, 1)
    rgb = renk("#7FA8FF")[None, None, :] * (1 - t) + renk("#FFFFFF")[None, None, :] * t
    kaydet("kayan_yildiz", rgb, np.clip(a, 0, 1))


# ---------------------------------------------------------------- galaksi

def galaksi_kol(rng, ad, kol, adim, boyut=256):
    """Yüzü dönük (egimsiz) logaritmik sarmal kollar; oyunda eğimle ezilir ve düzlem içinde döner. Toplamalı."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2) + 1e-4
    phi = np.arctan2(y, x)
    kollar = ((1 + np.cos(kol * (phi - np.log(r) / adim))) / 2) ** 4
    gur = fbm(boyut, boyut, 7, 7, 5, rng)
    govde = np.exp(-r / 0.38) * yumusak_basamak(0.05, 0.13, r)
    kol_y = kollar * govde * (0.45 + 1.0 * gur)
    dugum = kol_y * yumusak_basamak(0.55, 0.8, fbm(boyut, boyut, 26, 26, 3, rng)) * 2.0
    kenar = yumusak_basamak(1.0, 0.78, r)
    rgb, alfa = karistir(
        (kol_y * kenar * 1.1, renk("#BFD2FF")),
        (dugum * kenar, renk("#FF9CC4")),
        (np.exp(-(r / 0.09) ** 2) * 0.5, renk("#FFE2A8")),
    )
    kaydet(ad, rgb, np.clip(alfa, 0, 1))


def galaksi_halka(rng, boyut=256):
    """Dış halka (parlak düğümlü) ve zayıf iç halka; kollardan farklı hızla döner. Toplamalı."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    gur = fbm(boyut, boyut, 9, 9, 4, rng)
    dugum = yumusak_basamak(0.45, 0.8, fbm(boyut, boyut, 30, 30, 3, rng))
    dis = np.exp(-((r - 0.80) / 0.04) ** 2) * (0.5 + 1.5 * yumusak_basamak(0.35, 0.75, gur))
    ic = np.exp(-((r - 0.32) / 0.03) ** 2) * (0.25 + 0.9 * gur)
    kenar = yumusak_basamak(1.0, 0.9, r)
    rgb, alfa = karistir(
        (dis * kenar * 1.9, renk("#FFF0C4")),
        (dis * dugum * kenar * 2.2, renk("#FFC463")),
        (ic * 1.4, renk("#FFE3A0")),
    )
    kaydet("galaksi_halka", rgb, np.clip(alfa, 0, 1))


# ---------------------------------------------------------------- kara delik

def kara_delik_disk(rng, boyut=256):
    """Yüzü dönük akresyon diski: açısal iplikçikler, içe doğru sarmal kayma, içte beyaz-sarı dışta turuncu-kırmızı.
    Oyunda eğimle ezilip (elips) dönmeyle çizilir; Doppler parlaklığı köşe renginden gelir. Toplamalı."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2) + 1e-4
    phi = np.arctan2(y, x)
    an = 1024
    gur = np.zeros((boyut, an), np.float32)
    toplam = 0.0
    for o, kazanc in enumerate((1.0, 0.6, 0.35)):
        gur += kazanc * gurultu(an, boyut, 36 << o, 5 << o, rng, True)
        toplam += kazanc
    gur /= toplam
    ri = np.clip(((r - 0.2) / 0.8 * (boyut - 1)).astype(np.int32), 0, boyut - 1)
    ai = (((phi / (2 * np.pi) + 0.55 * np.log(r)) % 1.0) * an).astype(np.int32) % an
    iplik = yumusak_basamak(0.32, 0.78, gur[ri, ai])
    profil = (0.30 / r) ** 1.5 * yumusak_basamak(0.25, 0.36, r) * yumusak_basamak(1.0, 0.72, r)
    t = yumusak_basamak(0.3, 0.95, r)
    rgb = uc_renk(t, [(0.0, renk("#FFF6DC")), (0.35, renk("#FFC470")), (0.7, renk("#FF8228")), (1.0, renk("#B8300F"))])
    alfa = np.clip(profil * (0.3 + 1.4 * iplik) * 1.7, 0, 1)
    kaydet("kara_delik_disk", rgb, alfa)


def kara_delik_mercek(boyut=256):
    """Işık bükülmesi halkası: ince foton halkası + soluk hale; alt ve üstte kalınlaşan yaylar (arka diskin bükülmüş
    görüntüsü). Dönmez, yalnız yavaşça nefes alır. Toplamalı."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    sy = np.abs(y / np.maximum(r, 1e-4))
    halka = np.exp(-((r - 0.5) / 0.013) ** 2) * 1.6
    hale = np.exp(-((r - 0.5) / 0.09) ** 2) * 0.42
    yay = np.exp(-((r - 0.5 - 0.035 * sy) / (0.02 + 0.05 * sy)) ** 2) * sy ** 3 * 1.4
    kenar = yumusak_basamak(1.0, 0.8, r)
    rgb, alfa = karistir(
        (halka * kenar, renk("#FFF8E0")),
        (hale * kenar, renk("#FFC24D")),
        (yay * kenar, renk("#FFB347")),
    )
    kaydet("kara_delik_mercek", rgb, np.clip(alfa, 0, 1))


def kara_delik_gama(w=64, h=256):
    """Kara delik kutuplarından çıkan gama ışını huzmesi: ince, parlak açık sarı çekirdek, uca doğru genişleyen altın sarısı koni ve iki
    şok düğümü (K-E84: eski beyaz-mavi/mor renk sarıya çevrildi). Kuad merkezde iki kutba doğru uzanır (|y| = 0 delik merkezi,
    ortada boşluk). Toplamalı."""
    x, y = izgara(w, h)
    ay = np.abs(y)
    acilma = 0.07 + 0.30 * ay
    cekirdek = np.exp(-(x / acilma) ** 2) * yumusak_basamak(0.04, 0.14, ay) * np.exp(-ay / 0.7)
    koni = 0.34 * np.exp(-(x / (0.2 + 0.5 * ay)) ** 2) * yumusak_basamak(0.08, 0.3, ay) * np.exp(-ay / 0.9)
    dugum = sum(0.5 * np.exp(-((ay - k) / 0.05) ** 2) * np.exp(-(x / 0.18) ** 2) for k in (0.3, 0.6))
    a = (1.3 * cekirdek + koni + dugum) * yumusak_basamak(1.0, 0.82, ay) * yumusak_basamak(1.0, 0.8, np.abs(x))
    t = np.clip(np.abs(x) / 0.5, 0, 1)[..., None]
    rgb = renk("#FFF6B0")[None, None, :] * (1 - t) + renk("#FFC21F")[None, None, :] * t
    kaydet("kara_delik_gama", rgb, np.clip(a, 0, 1))


# ---------------------------------------------------------------- gezegenler (eşdikdörtgen, soldan sağa dikişsiz)

def enlem_izgara():
    """enlem φ ∈ [π/2 (üst), -π/2], boylam λ ∈ [0, 2π)."""
    v = (np.arange(GEZ_H, dtype=np.float32) + 0.5) / GEZ_H
    u = (np.arange(GEZ_W, dtype=np.float32) + 0.5) / GEZ_W
    lam, phi = np.meshgrid(u * 2 * np.pi, (0.5 - v) * np.pi)
    return lam, phi


def gezegen_kaydet(ad, rgb):
    kaydet(ad, rgb, np.ones((GEZ_H, GEZ_W), np.float32))


def bantli_gaz(rng, ad, duraklar, bant, bozulma, siddet, leke=None):
    lam, phi = enlem_izgara()
    bozuk = fbm(GEZ_W, GEZ_H, 8, 3, 4, rng, True)
    ince = fbm(GEZ_W, GEZ_H, 28, 6, 4, rng, True)
    t = 0.5 + 0.5 * np.sin(phi * bant + bozulma * (bozuk - 0.5) * 6.0)
    t = np.clip(t * 0.8 + 0.35 * (ince - 0.5) * siddet + 0.1, 0, 1)
    rgb = uc_renk(t, duraklar)
    if leke is not None:
        lx, lp, rx, ry, c = leke
        dl = np.angle(np.exp(1j * (lam - lx))) * np.cos(lp)
        d = np.sqrt((dl / rx) ** 2 + ((phi - lp) / ry) ** 2)
        w = (np.exp(-d ** 2 * 1.6) * (0.75 + 0.25 * ince))[..., None]
        halka = np.exp(-((d - 1.35) / 0.25) ** 2)[..., None] * 0.25
        rgb = rgb * (1 - np.clip(w + halka, 0, 1)) + c[None, None, :] * np.clip(w + halka, 0, 1)
    kutuplar = (1 - 0.35 * yumusak_basamak(0.9, 1.5, np.abs(phi)))[..., None]
    gezegen_kaydet(ad, rgb * kutuplar)


def gezegen_gaz(rng):
    bantli_gaz(rng, "gezegen_gaz",
               [(0.0, renk("#7A3E22")), (0.3, renk("#B5693B")), (0.55, renk("#E3C09A")), (0.8, renk("#F3E6D2")),
                (1.0, renk("#C98A57"))], bant=22, bozulma=1.4, siddet=1.0,
               leke=(1.2, -0.38, 0.5, 0.17, renk("#C4482B")))


def gezegen_halkali(rng):
    bantli_gaz(rng, "gezegen_halkali",
               [(0.0, renk("#A98F5E")), (0.4, renk("#D1B882")), (0.75, renk("#EAD9A8")), (1.0, renk("#F4E8C2"))],
               bant=15, bozulma=0.8, siddet=0.5)


def gezegen_buz(rng):
    lam, phi = enlem_izgara()
    bozuk = fbm(GEZ_W, GEZ_H, 6, 3, 4, rng, True)
    t = 0.5 + 0.5 * np.sin(phi * 9 + 3.0 * (bozuk - 0.5))
    rgb = uc_renk(t * 0.7 + 0.15, [(0.0, renk("#2B58A6")), (0.5, renk("#4B93CF")), (1.0, renk("#8FD0E8"))])
    bulut = sirtli(GEZ_W, GEZ_H, 10, 3, 4, rng, True)
    bulut = yumusak_basamak(0.55, 0.95, bulut) * (0.4 + 0.6 * np.cos(phi * 2)) * 0.8
    rgb = rgb * (1 - bulut[..., None]) + renk("#F2FBFF")[None, None, :] * bulut[..., None]
    gezegen_kaydet("gezegen_buz", rgb)


def gezegen_col(rng):
    lam, phi = enlem_izgara()
    n1 = fbm(GEZ_W, GEZ_H, 6, 3, 6, rng, True)
    n2 = fbm(GEZ_W, GEZ_H, 14, 6, 4, rng, True)
    rgb = uc_renk(n1, [(0.0, renk("#6E2A14")), (0.45, renk("#B5552B")), (0.8, renk("#D9935E")), (1.0, renk("#EBC092"))])
    koyu = yumusak_basamak(0.55, 0.3, n2)[..., None] * 0.45
    rgb = rgb * (1 - koyu)
    kap = yumusak_basamak(1.18, 1.38, np.abs(phi) + 0.12 * (n2 - 0.5))[..., None]
    rgb = rgb * (1 - kap) + renk("#F4F1EC")[None, None, :] * kap
    gezegen_kaydet("gezegen_col", rgb)


def gezegen_merkur(rng):
    """Merkür: koyu gri-kahve, yoğun kraterli, denizsiz ve soluk ışınlı kraterler."""
    lam, phi = enlem_izgara()
    taban = 0.46 + 0.14 * (fbm(GEZ_W, GEZ_H, 8, 4, 6, rng, True) - 0.5) * 2
    ton = taban * (1 - 0.2 * yumusak_basamak(0.52, 0.4, fbm(GEZ_W, GEZ_H, 3, 2, 3, rng, True)))
    for _ in range(150):
        cl = rng.uniform(0, 2 * np.pi)
        cp = np.arcsin(rng.uniform(-0.95, 0.95))
        rho = float(rng.choice([0.03, 0.04, 0.06, 0.09, 0.14, 0.2], p=[0.28, 0.25, 0.2, 0.15, 0.08, 0.04]))
        dl = np.angle(np.exp(1j * (lam - cl))) * np.cos(phi)
        d = np.sqrt(dl ** 2 + (phi - cp) ** 2)
        ton = ton - 0.17 * yumusak_basamak(rho, rho * 0.55, d) + 0.14 * np.exp(-((d - rho) / (0.2 * rho)) ** 2)
    ton = np.clip(ton, 0.05, 1.0)
    rgb = ton[..., None] * np.array([0.78, 0.73, 0.68], np.float32)[None, None, :]
    gezegen_kaydet("gezegen_merkur", rgb)


def gezegen_venus(rng):
    """Venüs: kalın sarı-krem bulut örtüsü, yumuşak girdaplı bantlar (yüzey görünmez)."""
    bantli_gaz(rng, "gezegen_venus",
               [(0.0, renk("#B98F55")), (0.35, renk("#D9B878")), (0.7, renk("#EBD6A4")), (1.0, renk("#F6E9C4"))],
               bant=5, bozulma=2.4, siddet=0.6)


def gezegen_uranus(rng):
    """Uranüs: soluk camgöbeği-turkuaz, neredeyse özelliksiz, çok hafif bantlı buz devi."""
    bantli_gaz(rng, "gezegen_uranus",
               [(0.0, renk("#7FC9CF")), (0.5, renk("#A4E0E3")), (1.0, renk("#D2F3F1"))],
               bant=7, bozulma=0.4, siddet=0.2)


def gezegen_pluton(rng):
    """Plüton: bej-kahverengi buzlu yüzey, koyu ekvator kuşağı ve kalp şekilli parlak ova (Tombaugh Regio)."""
    lam, phi = enlem_izgara()
    n1 = fbm(GEZ_W, GEZ_H, 6, 3, 6, rng, True)
    n2 = fbm(GEZ_W, GEZ_H, 16, 7, 4, rng, True)
    rgb = uc_renk(n1 * 0.8 + 0.1, [(0.0, renk("#5B3F2E")), (0.5, renk("#A58266")), (1.0, renk("#D8BFA0"))])
    kusak = (np.exp(-((phi + 0.05) / 0.28) ** 2) * yumusak_basamak(0.35, 0.65, n2))[..., None]
    rgb = rgb * (1 - 0.45 * kusak) + renk("#3A2418")[None, None, :] * 0.45 * kusak
    # Kalp (y aşağı sivri): boylam π çevresinde, dikisten uzak; içi açık krem.
    dl = np.angle(np.exp(1j * (lam - np.pi))) * np.cos(phi)
    x = dl / 0.42
    y = (phi - 0.35) / 0.42 + 0.15
    f = (x ** 2 + y ** 2 - 1) ** 3 - x ** 2 * y ** 3
    kalp = (yumusak_basamak(0.06, -0.06, f) * (0.88 + 0.12 * n2))[..., None]
    rgb = rgb * (1 - kalp) + renk("#F1E4D0")[None, None, :] * kalp
    gezegen_kaydet("gezegen_pluton", rgb)


# ---------------------------------------------------------------- halka ve uydu

def gezegen_halka(rng, boyut=256):
    """Yüzü dönük halka, iç yarıçap 0,50, dış 1,0; Cassini boşluğu ve ince radyal dalgalanma. Normal karışım."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    ornek = np.interp(r, np.linspace(0, 1, 140), rng.random(140).astype(np.float32))
    ince = np.interp(r, np.linspace(0, 1, 40), rng.random(40).astype(np.float32))
    b = yumusak_basamak(0.68, 0.7, r) * yumusak_basamak(0.9, 0.87, r)
    c = yumusak_basamak(0.5, 0.53, r) * yumusak_basamak(0.665, 0.64, r) * 0.55
    bosluk = 1 - 0.9 * np.exp(-((r - 0.675) / 0.012) ** 2)
    yog = (b * 0.95 + c) * bosluk * (0.7 + 0.3 * ornek) * (0.85 + 0.15 * ince)
    rgb = uc_renk(ince * 0.6 + 0.2, [(0.0, renk("#B89E78")), (1.0, renk("#EBDDBC"))])
    kaydet("gezegen_halka", rgb, np.clip(yog, 0, 1) * yumusak_basamak(1.0, 0.97, r))


def gezegen_uydu(boyut=32):
    x, y = kutup(boyut)
    r2 = x ** 2 + y ** 2
    z = np.sqrt(np.clip(1 - r2, 0, 1))
    isik = np.clip(-0.5 * x - 0.5 * y + 0.7 * z, 0, 1)
    ton = 0.12 + 0.88 * isik
    rgb = ton[..., None] * renk("#E4E0D8")[None, None, :]
    kaydet("gezegen_uydu", rgb, yumusak_basamak(1.0, 0.82, np.sqrt(r2)))


def main() -> None:
    rng = np.random.default_rng(20261109)
    kayan_yildiz()
    galaksi_kol(rng, "galaksi_kol_2", 2, 0.30)
    galaksi_kol(rng, "galaksi_kol_3", 3, 0.42)
    galaksi_halka(rng)
    kara_delik_disk(rng)
    kara_delik_mercek()
    kara_delik_gama()
    gezegen_gaz(rng)
    gezegen_halkali(rng)
    gezegen_buz(rng)
    gezegen_col(rng)
    gezegen_halka(rng)
    gezegen_uydu()
    # Gerçek güneş sistemi gezegenleri (Ay yok): kendi tohumları var. gezegen_ay kaldırıldığı için rng akışından sonra gelen
    # gezegen_halka bir kez farklı üretilir (önceki dokuların baytları aynı).
    rng2 = np.random.default_rng(20261110)
    gezegen_merkur(rng2)
    gezegen_venus(rng2)
    gezegen_uranus(rng2)
    gezegen_pluton(rng2)


if __name__ == "__main__":
    main()
