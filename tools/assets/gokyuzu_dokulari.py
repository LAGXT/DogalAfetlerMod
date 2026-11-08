#!/usr/bin/env python3
"""Ortak gökyüzü dokularını üretir (madde 16, docs/tasarim/gokyuzu.md).

Çalıştırma (KÖK'ten ya da worktree kökünden):
    KÖK/.venv/bin/python tools/assets/gokyuzu_dokulari.py

Çıktı: src/main/resources/assets/dogalafetler/textures/environment/gokyuzu/*.png (+ .png.mcmeta, doğrusal süzme).
Aynı tohumla her çalıştırmada aynı baytlar üretilir. RGB rengi, alfa yoğunluğu taşır; oyunda
SRC_ALPHA, ONE (toplamalı) karışımla çizilir.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image

KOK = Path(__file__).resolve().parents[2]
HEDEF = KOK / "src/main/resources/assets/dogalafetler/textures/environment/gokyuzu"


# ---------------------------------------------------------------- gürültü yardımcıları

def gurultu(w: int, h: int, gx: int, gy: int, rng: np.random.Generator, periyodik_x: bool = False) -> np.ndarray:
    """Yumuşatılmış değer gürültüsü, [0,1]. periyodik_x: sağ ve sol kenar dikişsiz birleşir."""
    g = rng.random((gy + 2, gx + 2), dtype=np.float32)
    if periyodik_x:
        g[:, gx] = g[:, 0]
        g[:, gx + 1] = g[:, 1]
    x = np.arange(w, dtype=np.float32) * gx / w
    y = np.arange(h, dtype=np.float32) * gy / h
    xi = x.astype(np.int32)
    yi = y.astype(np.int32)
    xf = x - xi
    yf = y - yi
    xf = xf * xf * (3 - 2 * xf)
    yf = yf * yf * (3 - 2 * yf)
    g00 = g[np.ix_(yi, xi)]
    g10 = g[np.ix_(yi, xi + 1)]
    g01 = g[np.ix_(yi + 1, xi)]
    g11 = g[np.ix_(yi + 1, xi + 1)]
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


def sirtli(w, h, gx, gy, oktav, rng, periyodik_x=False):
    """Sırtlı (ridged) gürültü: ince iplik/damar yapıları için."""
    toplam = np.zeros((h, w), np.float32)
    genlik, norm = 1.0, 0.0
    for o in range(oktav):
        n = gurultu(w, h, gx << o, gy << o, rng, periyodik_x)
        toplam += genlik * (1.0 - np.abs(2.0 * n - 1.0)) ** 2
        norm += genlik
        genlik *= 0.55
    return toplam / norm


def yumusak_basamak(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def kutup(boyut: int):
    """Merkezli koordinatlar: x, y ∈ [-1, 1]."""
    k = (np.arange(boyut, dtype=np.float32) + 0.5) / boyut * 2 - 1
    y, x = np.meshgrid(k, k, indexing="ij")
    return x, y


def nokta_yildizlar(boyut, adet, rng, renk=(1.0, 1.0, 1.0), sigma=0.004):
    """Dokuya gömülü küçük yıldızlar (Gauss noktaları)."""
    x, y = kutup(boyut)
    a = np.zeros((boyut, boyut), np.float32)
    for _ in range(adet):
        cx, cy = rng.uniform(-0.8, 0.8, 2)
        p = rng.uniform(0.3, 1.0)
        a += p * np.exp(-((x - cx) ** 2 + (y - cy) ** 2) / (2 * sigma ** 2))
    return a


def kaydet(ad: str, rgb: np.ndarray, alfa: np.ndarray, clamp: bool = True) -> None:
    rgb = np.clip(rgb, 0, 1)
    alfa = np.clip(alfa, 0, 1)
    dizi = np.dstack([rgb, alfa[..., None]]) if rgb.ndim == 3 else None
    veri = (dizi * 255 + 0.5).astype(np.uint8)
    HEDEF.mkdir(parents=True, exist_ok=True)
    Image.fromarray(veri, "RGBA").save(HEDEF / f"{ad}.png", optimize=True)
    (HEDEF / f"{ad}.png.mcmeta").write_text(
        json.dumps({"texture": {"blur": True, "clamp": clamp}}, indent=2) + "\n", encoding="utf-8")
    print(f"  {ad}.png  {veri.shape[1]}x{veri.shape[0]}")


def kaydet_olcekli(ad: str, rgb: np.ndarray, alfa: np.ndarray, boyutlar: list[int]) -> None:
    """En büyük boyutta üretilen dokuyu kalite seviyeleri için küçültüp ayrı dosyalar yazar."""
    tam = Image.fromarray((np.dstack([np.clip(rgb, 0, 1), np.clip(alfa, 0, 1)[..., None]]) * 255 + 0.5)
                          .astype(np.uint8), "RGBA")
    for b in boyutlar:
        img = tam if b == tam.width else tam.resize((b, b), Image.LANCZOS)
        dizi = np.asarray(img).astype(np.float32) / 255
        kaydet(f"{ad}_{b}", dizi[..., :3], dizi[..., 3])


def renk(h: str):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)], np.float32)


def karistir(*katmanlar):
    """(yoğunluk, renk) çiftlerinden toplamalı RGB + alfa üretir."""
    toplam = sum(a for a, _ in katmanlar)
    rgb = sum(a[..., None] * c[None, None, :] for a, c in katmanlar)
    guvenli = np.maximum(toplam, 1e-6)[..., None]
    return rgb / guvenli, toplam


# ---------------------------------------------------------------- dokular

def samanyolu(rng):
    """2048x256 şerit: u boyunca gökyüzü çemberi (dikişsiz), v boyunca şerit genişliği."""
    w, h = 2048, 256
    u = (np.arange(w, dtype=np.float32) + 0.5) / w
    v = (np.arange(h, dtype=np.float32) + 0.5) / h
    U, V = np.meshgrid(u, v)
    # Galaksi merkezi u=0.5'te: şişkinlik hem kalınlaştırır hem parlatır.
    du = np.minimum(np.abs(U - 0.5), 1 - np.abs(U - 0.5))
    siskinlik = np.exp(-(du / 0.07) ** 2)
    genislik = 0.13 + 0.09 * siskinlik
    kayma = 0.04 * (fbm(w, 1, 6, 1, 3, rng, True)[0][None, :] - 0.5)
    profil = np.exp(-((V - 0.5 - kayma) ** 2) / (2 * genislik ** 2))
    bulut = fbm(w, h, 32, 4, 6, rng, True)
    bulut2 = fbm(w, h, 64, 8, 5, rng, True)
    yogunluk = profil * (0.35 + 1.1 * yumusak_basamak(0.3, 0.8, bulut)) * (0.7 + 0.6 * bulut2)
    yogunluk *= 0.55 + 0.9 * siskinlik
    # Koyu toz damarları (Great Rift): merkez hattı boyunca sırtlı gürültü ile oyulur.
    damar = sirtli(w, h, 24, 6, 5, rng, True)
    toz_hatti = np.exp(-((V - 0.5 - kayma - 0.015) ** 2) / (2 * 0.045 ** 2))
    toz = toz_hatti * yumusak_basamak(0.35, 0.75, damar)
    yogunluk *= 1 - 0.85 * toz
    # Yıldız tozu: tek piksellik sönük noktalar şeridi tanecikli gösterir.
    benek = (rng.random((h, w)) > 0.985).astype(np.float32) * rng.random((h, w)).astype(np.float32)
    yogunluk += benek * profil * 0.6
    # Kenarlar: şerit yarısının dışa doğru %70'inde yumuşak düşüş (sert şerit görünmesin) ve 1'de kırpmak yerine
    # yumuşak doyma (1 - e^-x): parlak merkez bile kenara doğru tek düze bir "bant" duvarı oluşturmaz.
    kenar = (yumusak_basamak(0.0, 0.35, V) * yumusak_basamak(0.0, 0.35, 1 - V)) ** 1.5
    alfa = np.clip(1 - np.exp(-1.6 * yogunluk * kenar), 0, 1)
    rgb_merkez, rgb_uc = renk("#FFE9C4"), renk("#C9D6FF")
    t = siskinlik[..., None]
    rgb = rgb_uc[None, None, :] * (1 - t) + rgb_merkez[None, None, :] * t
    rgb = rgb * (0.85 + 0.15 * bulut2[..., None])
    kaydet("samanyolu", rgb, alfa, clamp=False)


def galaksi_sarmal(rng, boyut=512, egim=0.38, kol=2, adim=0.32):
    x, y = kutup(boyut)
    aci = np.deg2rad(32)
    xr = x * np.cos(aci) + y * np.sin(aci)
    yr = (-x * np.sin(aci) + y * np.cos(aci)) / egim
    r = np.sqrt(xr ** 2 + yr ** 2) + 1e-4
    phi = np.arctan2(yr, xr)
    kollar = ((1 + np.cos(kol * (phi - np.log(r) / adim))) / 2) ** 3
    gur = fbm(boyut, boyut, 6, 6, 5, rng)
    disk = np.exp(-r / 0.28)
    kol_y = kollar * disk * (0.6 + 0.8 * gur)
    cekirdek = np.exp(-(r / 0.07) ** 2) * 1.2 + np.exp(-r / 0.12) * 0.5
    dugum = kol_y * yumusak_basamak(0.62, 0.8, fbm(boyut, boyut, 24, 24, 3, rng)) * 1.5
    toz = kollar * yumusak_basamak(0.55, 0.75, gur) * np.exp(-r / 0.35) * 0.0
    kenar = yumusak_basamak(1.0, 0.75, r)
    rgb, alfa = karistir(
        (cekirdek * kenar, renk("#FFE2A8")),
        (kol_y * kenar * 0.9, renk("#AFC4FF")),
        (disk * 0.25 * kenar, renk("#E8E4FF")),
        (dugum * kenar, renk("#FF8FB8")),
    )
    alfa = np.clip(alfa * (1 - toz), 0, 1)
    return rgb, alfa


def nebula(rng, boyut, baskin: str):
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + (y * 1.15) ** 2)
    zarf = np.exp(-(r / 0.55) ** 2) * yumusak_basamak(1.0, 0.7, r)
    n1 = fbm(boyut, boyut, 4, 4, 6, rng)
    iplik = sirtli(boyut, boyut, 5, 5, 6, rng)
    n3 = fbm(boyut, boyut, 3, 3, 4, rng)
    yog = zarf * yumusak_basamak(0.3, 0.75, n1) * (0.45 + 1.1 * iplik ** 2)
    if baskin == "kirmizi":
        oran = 0.15 + 0.2 * yumusak_basamak(0.5, 0.8, n3)
    elif baskin == "mavi":
        oran = 0.75 + 0.2 * yumusak_basamak(0.4, 0.7, n3)
    else:
        oran = yumusak_basamak(0.35, 0.65, n3)
    # Merkezde oksijen (mavi-yeşil), dışta hidrojen-alfa (kırmızı-pembe).
    oran = np.clip(oran + 0.35 * np.exp(-(r / 0.25) ** 2) * (baskin != "kirmizi"), 0, 1)
    yildizlar = nokta_yildizlar(boyut, 14, rng, sigma=0.006)
    rgb, alfa = karistir(
        (yog * (1 - oran), renk("#FF4A6E")),
        (yog * oran, renk("#4FD8E0")),
        (yog * 0.25 * iplik, renk("#FFC0D0")),
        (yildizlar * zarf * 2, renk("#FFFFFF")),
    )
    return rgb, np.clip(alfa * 1.25, 0, 1)


def kalinti(rng, boyut=2048):
    """Süpernova kalıntısı (Yengeç benzeri): dışta kırmızı-turuncu iplik kabuğu, içte mavi-beyaz ışıma."""
    x, y = kutup(boyut)
    aci = np.deg2rad(20)
    xr = x * np.cos(aci) + y * np.sin(aci)
    yr = (-x * np.sin(aci) + y * np.cos(aci)) * 1.3
    r = np.sqrt(xr ** 2 + yr ** 2)
    bozulma = fbm(boyut, boyut, 4, 4, 4, rng) - 0.5
    rb = r + 0.12 * bozulma
    kabuk = np.exp(-((rb - 0.68) / 0.2) ** 2)
    iplik = sirtli(boyut, boyut, 12, 12, 6, rng)
    iplik2 = sirtli(boyut, boyut, 28, 28, 4, rng)
    iplikler = kabuk * (yumusak_basamak(0.45, 0.85, iplik) * 1.3 + 0.5 * yumusak_basamak(0.55, 0.9, iplik2))
    ic_gur = fbm(boyut, boyut, 6, 6, 6, rng)
    ic = np.exp(-(rb / 0.42) ** 2) * (0.35 + 0.65 * ic_gur)
    oksijen = kabuk * yumusak_basamak(0.6, 0.8, fbm(boyut, boyut, 5, 5, 4, rng)) * 0.5
    kenar = yumusak_basamak(1.0, 0.85, r)
    rgb, alfa = karistir(
        (iplikler * kenar, renk("#FF5A2A")),
        (iplikler * 0.35 * iplik2 * kenar, renk("#FFB060")),
        (ic * 0.8 * kenar, renk("#9CC4FF")),
        (oksijen * kenar, renk("#5CE6B0")),
    )
    return rgb, np.clip(alfa * 0.95, 0, 1)


def gunes(rng, boyut=512):
    """Beyaz tonlu güneş: kenar kararması, granülasyon, lekeler ve korona. Renk oyunda verilir."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    R = 0.42
    ic = np.clip(1 - (r / R) ** 2, 0, 1)
    kenar_kararmasi = 0.45 + 0.55 * np.sqrt(ic)
    gran = fbm(boyut, boyut, 40, 40, 3, rng)
    leke = yumusak_basamak(0.78, 0.86, fbm(boyut, boyut, 6, 6, 3, rng)) * (r < R * 0.85)
    disk = yumusak_basamak(R + 0.01, R - 0.01, r) * kenar_kararmasi * (0.9 + 0.1 * gran) * (1 - 0.55 * leke)
    phi = np.arctan2(y, x)
    aci_gur = gurultu(512, 1, 24, 1, rng, True)[0]
    idx = ((phi + np.pi) / (2 * np.pi) * 511).astype(np.int32)
    akinti = 0.6 + 0.8 * aci_gur[idx]
    korona = np.exp(-np.maximum(r - R, 0) / 0.07) * 0.75 * akinti
    for rr, g in ((1.25, 0.18), (1.55, 0.1), (1.9, 0.06)):
        korona += g * np.exp(-((r - R * rr) / 0.02) ** 2) * akinti
    korona *= (r >= R * 0.98) * yumusak_basamak(1.0, 0.85, r)
    rgb, alfa = karistir((disk, renk("#FFFFFF")), (korona, renk("#FFF4E0")))
    kaydet("gunes", rgb, np.clip(alfa, 0, 1))


def hale(boyut=256):
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    a = (np.exp(-(r / 0.3) ** 2) * 0.75 + np.exp(-r / 0.22) * 0.35) * yumusak_basamak(1.0, 0.8, r)
    kaydet("hale", np.ones((boyut, boyut, 3), np.float32), a)


def supernova_isin(rng, boyut=512):
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2) + 1e-4
    phi = np.arctan2(y, x)
    a = np.zeros_like(r)
    for k in range(12):
        hedef = k * np.pi / 6 + rng.uniform(-0.06, 0.06)
        d = np.angle(np.exp(1j * (phi - hedef)))
        uzunluk = 0.95 if k % 2 == 0 else 0.6
        genislik = 0.0045 + 0.004 * (k % 3 == 0)
        a += np.exp(-((d * r) / genislik) ** 2) * np.exp(-r / (uzunluk * 0.35)) * yumusak_basamak(1.0, 0.7, r / uzunluk)
    a += np.exp(-(r / 0.05) ** 2) * 1.2 + np.exp(-r / 0.09) * 0.4
    rgb, alfa = karistir((a, renk("#E4EEFF")))
    kaydet("supernova_isin", rgb, np.clip(alfa, 0, 1))


def supernova_halka(rng, boyut=512):
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    phi = np.arctan2(y, x)
    aci_gur = gurultu(1024, 1, 48, 1, rng, True)[0]
    idx = ((phi + np.pi) / (2 * np.pi) * 1023).astype(np.int32)
    topak = 0.45 + 0.9 * aci_gur[idx] * (0.6 + 0.4 * fbm(boyut, boyut, 20, 20, 3, rng))
    halka = np.exp(-((r - 0.82) / 0.035) ** 2) * topak
    dolgu = np.exp(-((r - 0.7) / 0.18) ** 2) * 0.18 * topak
    rgb, alfa = karistir((halka, renk("#DDE8FF")), (dolgu, renk("#9FB8FF")))
    kaydet("supernova_halka", rgb, np.clip(alfa * yumusak_basamak(1.0, 0.92, r), 0, 1))


def yildiz_isin(boyut=64):
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    capraz = np.exp(-(y / 0.045) ** 2) * np.exp(-np.abs(x) / 0.35) + np.exp(-(x / 0.045) ** 2) * np.exp(-np.abs(y) / 0.35)
    a = np.exp(-(r / 0.12) ** 2) + 0.7 * capraz
    kaydet("yildiz_isin", np.ones((boyut, boyut, 3), np.float32), np.clip(a * yumusak_basamak(1.0, 0.85, r), 0, 1))

def izgara(w: int, h: int):
    """Dikdörtgen merkezli koordinatlar: x, y ∈ [-1, 1]."""
    xs = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    ys = (np.arange(h, dtype=np.float32) + 0.5) / h * 2 - 1
    y, x = np.meshgrid(ys, xs, indexing="ij")
    return x, y


def kara_delik(rng, boyut=256):
    """M87/EHT benzeri: karanlık gölge, ince foton halkası, alt yanı Doppler'le parlak turuncu-sarı akresyon halkası.

    Normal karışımla çizilir: gölge (siyah, alfa ~1) arkadaki yıldızları örter."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    phi = np.arctan2(y, x)
    doppler = 0.3 + 0.7 * (0.5 + 0.5 * np.cos(phi - np.pi / 2)) ** 2
    tur = 0.75 + 0.5 * fbm(boyut, boyut, 10, 10, 4, rng)
    halka = np.exp(-((r - 0.47) / 0.09) ** 2) * doppler * tur
    foton = np.exp(-((r - 0.395) / 0.022) ** 2) * (0.6 + 0.4 * doppler)
    golge = yumusak_basamak(0.37, 0.31, r)
    sicak = yumusak_basamak(0.62, 0.4, r)
    rgb = renk("#FF6A18")[None, None, :] * (1 - sicak[..., None]) + renk("#FFE7A8")[None, None, :] * sicak[..., None]
    parlak = np.clip(halka * 1.25 + foton, 0, 1)
    rgb = rgb * np.clip(0.35 + parlak, 0, 1)[..., None] * (1 - golge[..., None])
    alfa = np.clip(np.maximum(golge * 0.97, parlak), 0, 1) * yumusak_basamak(0.95, 0.8, r)
    kaydet("kara_delik", rgb, alfa)


def kara_delik_akis(rng, boyut=256):
    """Halka bölgesinde iplikçikli akış (toplamalı); oyunda yavaşça döndürülür."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    maske = yumusak_basamak(0.36, 0.42, r) * yumusak_basamak(0.66, 0.52, r)
    iplik = yumusak_basamak(0.45, 0.9, sirtli(boyut, boyut, 14, 14, 4, rng))
    kaydet("kara_delik_akis", np.ones((boyut, boyut, 3), np.float32) * renk("#FFD08A")[None, None, :],
           np.clip(maske * iplik * 0.85, 0, 1))


def kara_delik_jet(w=64, h=512):
    """İki kutuptan soluk ince jet (“gama ışınları”): merkezde boşluk, boyunca sönen çizgi ve M87 benzeri düğümler."""
    x, y = izgara(w, h)
    ay = np.abs(y)
    cizgi = np.exp(-(x / 0.12) ** 2) * yumusak_basamak(0.06, 0.16, ay) * np.exp(-np.maximum(ay - 0.16, 0) / 0.45)
    dugum = sum(0.6 * np.exp(-((ay - k) / 0.025) ** 2) * np.exp(-(x / 0.18) ** 2) for k in (0.3, 0.52, 0.78))
    a = (cizgi + dugum * np.exp(-ay / 0.6)) * yumusak_basamak(1.0, 0.85, ay)
    kaydet("kara_delik_jet", np.ones((h, w, 3), np.float32) * renk("#D4E2FF")[None, None, :], np.clip(a, 0, 1))


def main() -> None:
    print(f"Hedef: {HEDEF}")
    rng = np.random.default_rng(20261105)
    samanyolu(rng)
    rgb, a = galaksi_sarmal(rng, 512, egim=0.36, kol=2, adim=0.30)
    kaydet_olcekli("galaksi_sarmal", rgb, a, [512, 256, 128])
    rgb, a = galaksi_sarmal(rng, 256, egim=0.9, kol=3, adim=0.42)
    kaydet_olcekli("galaksi_kucuk", rgb, a, [256, 128, 64])
    for ad in ("kirmizi", "mavi", "karisik"):
        rgb, a = nebula(rng, 512, ad)
        kaydet_olcekli(f"nebula_{ad}", rgb, a, [512, 256, 128])
    rgb, a = kalinti(rng, 2048)
    kaydet_olcekli("kalinti", rgb, a, [2048, 1024, 512])
    gunes(rng)
    hale()
    supernova_isin(rng)
    supernova_halka(rng)
    yildiz_isin()
    # Gökyüzü-2 dokuları ayrı tohumla: önceki dokuların baytları değişmez.
    rng2 = np.random.default_rng(20261107)
    kara_delik(rng2)
    kara_delik_akis(rng2)
    kara_delik_jet()


if __name__ == "__main__":
    main()
