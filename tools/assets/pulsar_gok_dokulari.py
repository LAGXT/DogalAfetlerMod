#!/usr/bin/env python3
"""Pulsar-2 gök dokularını üretir (titreşen pulsar, kara delik yutması, ufuk ısı dalgası; PL-2, PL-4).

Çalıştırma (worktree kökünden):
    KOK/.venv/bin/python tools/assets/pulsar_gok_dokulari.py

Çıktı: src/main/resources/assets/dogalafetler/textures/environment/gokyuzu/{pulsar_*,karadelik_*,spagetti,isi_dalgasi}.png
(+ .png.mcmeta). Yardımcılar gokyuzu_dokulari.py'den gelir; tohum ayrı (20261108): eski dokuların baytları değişmez.
RGB rengi, alfa yoğunluğu taşır; "normal" diye işaretlenenler normal karışımla (örten), diğerleri toplamalı çizilir.
"""
from __future__ import annotations

import numpy as np

from gokyuzu_dokulari import (HEDEF, fbm, gurultu, izgara, kaydet, kaydet_olcekli, kutup, renk, sirtli,
                              yumusak_basamak)


def ust_uste(alt_rgb, alt_a, ust_rgb, ust_a):
    """Normal karışım (üst, altın üzerine); düz (çarpılmamış) RGB döner."""
    a = ust_a + alt_a * (1 - ust_a)
    rgb = (ust_rgb * ust_a[..., None] + alt_rgb * (alt_a * (1 - ust_a))[..., None]) / np.maximum(a, 1e-6)[..., None]
    return rgb, a


def duz(boyut_y, boyut_x, c):
    return np.ones((boyut_y, boyut_x, 3), np.float32) * c[None, None, :]


def aci_gurultusu(phi, rng, n=1024, g=32):
    """Açıya bağlı dikişsiz gürültü (ışınsal iplikler için)."""
    dizi = gurultu(n, 1, g, 1, rng, True)[0]
    return dizi[((phi + np.pi) / (2 * np.pi) * (n - 1)).astype(np.int32)]


# ---------------------------------------------------------------- pulsar

def pulsar_cekirdek(rng, boyut=256):
    """Disk yarıçapı dokunun 0,5'i (oyunda kuad yarı boyu 10° → 10° disk); dışı kısa parıltı."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    gran = fbm(boyut, boyut, 24, 24, 3, rng)
    disk = yumusak_basamak(0.52, 0.47, r) * (0.82 + 0.18 * gran)
    parilti = np.exp(-np.maximum(r - 0.5, 0) / 0.11) * 0.8 * (r >= 0.47)
    a = np.clip(disk + parilti, 0, 1) * yumusak_basamak(1.0, 0.86, r)
    sicak = yumusak_basamak(0.5, 0.0, r)
    rgb = duz(boyut, boyut, renk("#DCE6FF")) * (1 - sicak[..., None]) + duz(boyut, boyut, renk("#FFFFFF")) * sicak[..., None]
    kaydet("pulsar_cekirdek", rgb, a)


def pulsar_hale(rng, boyut=256):
    """Geniş hale: görünür alfa yarıçapın ~0,85'ine kadar (oyunda 40° kuad → ~34° hale)."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    phi = np.arctan2(y, x)
    iplik = 0.7 + 0.3 * aci_gurultusu(phi, rng, g=40)
    a = (np.exp(-(r / 0.58) ** 2) * 0.7 + np.exp(-r / 0.42) * 0.38) * iplik
    a *= yumusak_basamak(1.0, 0.84, r)
    kaydet("pulsar_hale", duz(boyut, boyut, renk("#FFFFFF")), np.clip(a, 0, 1))


def pulsar_korona(rng, boyut=512):
    """Dış korona: ışınsal flümler, görünür alfa ~0,9 yarıçapa kadar (oyunda 64° kuad → ≥ 52°)."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2) + 1e-4
    phi = np.arctan2(y, x)
    flum = aci_gurultusu(phi, rng, g=56) * 0.6 + aci_gurultusu(phi, rng, g=13) * 0.4
    flum = yumusak_basamak(0.35, 0.95, flum)
    taban = np.exp(-(r / 0.72) ** 2) * 0.32
    uzanti = flum * np.exp(-r / 0.46) * 0.55
    doku = 0.85 + 0.15 * fbm(boyut, boyut, 16, 16, 3, rng)
    a = (taban + uzanti) * doku * yumusak_basamak(1.0, 0.88, r)
    rgb = duz(boyut, boyut, renk("#E8E2FF"))
    kaydet("pulsar_korona", rgb, np.clip(a, 0, 1))


def pulsar_isin_keskin(rng, w=512, h=32):
    """Keskin ışın: u = 0 merkez → 1 uç; çok ince parlak çekirdek, soluk örtü, uzun sönüm (gök kubbesini aşar)."""
    x, y = izgara(w, h)
    u = (x + 1) / 2
    boy = (0.3 + 0.7 * np.exp(-u / 0.45)) * yumusak_basamak(1.0, 0.72, u)
    damar = 0.85 + 0.15 * gurultu(w, 1, 48, 1, rng)[0][None, :]
    en = np.exp(-(y / 0.09) ** 2) + 0.22 * np.exp(-(y / 0.45) ** 2)
    kaydet("pulsar_isin_keskin", duz(h, w, renk("#F0F4FF")), np.clip(boy * en * damar, 0, 1))


# ---------------------------------------------------------------- kara delik yutması

# Akresyon diskinin eğimi (dikey basıklık) ve yarıçapları: Java tarafı GokyuzuCizici bu oranlarla hizalar.
DISK_BASIKLIK = 0.26
GOLGE_R = 0.30
FOTON_R = 0.335
DISK_IC = 0.40
DISK_DIS = 0.92


def karadelik_buyuk(rng, boyut=512):
    """Yakın kara delik (normal karışım): örten gölge, ince foton halkası, gölgenin üstünden kırılan arka disk yayı ve
    önden geçen eğik akresyon diski; renk içte beyaz, ortada mavi, dışta mor; Doppler: sol yan (yaklaşan) parlak."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    phi = np.arctan2(y, x)
    rd = np.sqrt(x ** 2 + (y / DISK_BASIKLIK) ** 2)
    teta = np.arctan2(y / DISK_BASIKLIK, x)
    doppler = 0.28 + 0.72 * ((1 - np.cos(teta)) / 2) ** 1.4
    tur = 0.7 + 0.45 * fbm(boyut, boyut, 22, 6, 4, rng)

    def sicaklik(oran):
        ic = renk("#F6F2FF")[None, None, :]
        orta = renk("#7FA6FF")[None, None, :]
        dis = renk("#9A4CFF")[None, None, :]
        o = oran[..., None]
        return np.where(o < 0.5, ic + (orta - ic) * (o / 0.5), orta + (dis - orta) * ((o - 0.5) / 0.5))

    # Arka diskin kütleçekimle gölgenin üstüne ve altına kırılan yayı.
    yay = np.exp(-((r - 0.41) / 0.05) ** 2) * (0.35 + 0.65 * np.abs(np.sin(phi)) ** 1.5)
    yay *= (0.55 + 0.45 * ((1 - np.cos(phi)) / 2)) * tur
    yay_rgb = sicaklik(np.clip((r - 0.36) / 0.3, 0, 1))
    rgb = np.zeros((boyut, boyut, 3), np.float32)
    a = np.zeros((boyut, boyut), np.float32)
    rgb, a = ust_uste(rgb, a, yay_rgb, np.clip(yay * 1.1, 0, 1))
    golge = yumusak_basamak(GOLGE_R + 0.025, GOLGE_R, r)
    rgb, a = ust_uste(rgb, a, np.zeros_like(rgb), golge)
    foton = np.exp(-((r - FOTON_R) / 0.011) ** 2) * (0.7 + 0.3 * doppler)
    rgb, a = ust_uste(rgb, a, duz(boyut, boyut, renk("#F2EEFF")), np.clip(foton, 0, 1))
    bant = yumusak_basamak(DISK_IC, DISK_IC + 0.06, rd) * yumusak_basamak(DISK_DIS, 0.68, rd)
    arka_gizli = (y < 0) & (r < GOLGE_R + 0.01)
    disk = bant * doppler * tur * (~arka_gizli)
    disk_rgb = sicaklik(np.clip((rd - DISK_IC) / (DISK_DIS - DISK_IC), 0, 1))
    rgb, a = ust_uste(rgb, a, disk_rgb, np.clip(disk * 1.15, 0, 1))
    a *= yumusak_basamak(1.0, 0.94, r)
    return rgb, a


def karadelik_disk_akis(rng, boyut=512):
    """Akresyon akışı (toplamalı): dairesel sarmal iplikler; oyunda UV'si döndürülüp diskle aynı basıklıkta çizilir.
    İçerik yarıçap ≤ 0,92: dönen UV köşeleri şeffaf kenarda kalır."""
    na, nr = 1024, 128
    kutupsal = sirtli(na, nr, 28, 4, 4, rng, periyodik_x=True)
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    phi = np.arctan2(y, x)
    sarmal = phi + 2.2 * r
    ai = (((sarmal + np.pi) / (2 * np.pi)) % 1.0 * (na - 1)).astype(np.int32)
    ri = np.clip(r / DISK_DIS * (nr - 1), 0, nr - 1).astype(np.int32)
    iplik = yumusak_basamak(0.4, 0.9, kutupsal[ri, ai])
    maske = yumusak_basamak(DISK_IC, DISK_IC + 0.05, r) * yumusak_basamak(DISK_DIS, 0.7, r)
    kaydet("karadelik_disk_akis", duz(boyut, boyut, renk("#DCE4FF")), np.clip(iplik * maske * 0.85, 0, 1))


def karadelik_mercek(rng, boyut=512):
    """Kütleçekim merceği (normal karışım): çevredeki yıldızları söndüren koyu halka, Einstein halkası ve halkanın
    çevresinde kayan yıldızların kısa yayları."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    phi = np.arctan2(y, x)
    koyu = 0.62 * yumusak_basamak(0.22, 0.3, r) * yumusak_basamak(0.97, 0.5, r)
    rgb = np.zeros((boyut, boyut, 3), np.float32)
    halka = np.exp(-((r - 0.56) / 0.014) ** 2) * (0.55 + 0.45 * aci_gurultusu(phi, rng, g=20))
    yaylar = np.zeros_like(r)
    for _ in range(14):
        rr = rng.uniform(0.6, 0.86)
        merkez = rng.uniform(-np.pi, np.pi)
        uzunluk = rng.uniform(0.15, 0.45) * (0.6 / rr)
        fark = np.angle(np.exp(1j * (phi - merkez)))
        yaylar += rng.uniform(0.5, 1.0) * np.exp(-((r - rr) / 0.006) ** 2) * np.exp(-(fark / uzunluk) ** 2)
    isik = np.clip(halka + yaylar, 0, 1)
    rgb, a = ust_uste(rgb, koyu, duz(boyut, boyut, renk("#EDE8FF")), isik)
    kaydet("karadelik_mercek", rgb, np.clip(a, 0, 1))


def karadelik_foton(boyut=256):
    """Foton halkası parlaması (toplamalı): halka yarıçapı dokunun 0,84'ü."""
    x, y = kutup(boyut)
    r = np.sqrt(x ** 2 + y ** 2)
    a = np.exp(-((r - 0.84) / 0.025) ** 2) + 0.3 * np.exp(-((r - 0.84) / 0.11) ** 2)
    kaydet("karadelik_foton", duz(boyut, boyut, renk("#FFFFFF")), np.clip(a * yumusak_basamak(1.0, 0.95, r), 0, 1))


def spagetti(rng, w=256, h=64):
    """Spagetti kuyruğu: u = 0 pulsar → 1 kara delik; kara deliğe doğru incelen ince madde iplikleri."""
    x, y = izgara(w, h)
    u = (x + 1) / 2
    a = np.zeros_like(u)
    for _ in range(7):
        y0 = rng.uniform(-0.55, 0.55)
        dalga = rng.uniform(0.02, 0.08) * np.sin(u * rng.uniform(4, 9) + rng.uniform(0, 6.28))
        merkez = (y0 + dalga) * (1 - 0.7 * u)
        genislik = rng.uniform(0.05, 0.12) * (1 - 0.5 * u)
        a += rng.uniform(0.4, 1.0) * np.exp(-((y - merkez) / genislik) ** 2)
    a += 0.25 * np.exp(-(y / (0.6 * (1 - 0.6 * u))) ** 2)
    a *= 0.6 + 0.4 * fbm(w, h, 24, 3, 3, rng)
    a *= (1 - 0.35 * u) * yumusak_basamak(0.0, 0.06, u) * yumusak_basamak(1.0, 0.9, u)
    kaydet("spagetti", duz(h, w, renk("#FFFFFF")), np.clip(a, 0, 1))


def isi_dalgasi(rng, w=256, h=64):
    """Ufuk ısı dalgası şeridi: yatayda dikişsiz (UV kaydırılır), v = 0 üst kenar (sönük) → 1 ufuk."""
    x, y = izgara(w, h)
    n = fbm(w, h, 8, 2, 3, rng, periyodik_x=True)
    bantlar = 0.5 + 0.5 * np.sin(2 * np.pi * ((y + 1) * 3.5 + 0.9 * n))
    a = bantlar ** 3 * (0.55 + 0.45 * fbm(w, h, 16, 4, 2, rng, periyodik_x=True))
    a *= yumusak_basamak(-1.0, 0.6, y) * yumusak_basamak(1.0, 0.85, y)
    kaydet("isi_dalgasi", duz(h, w, renk("#FFFFFF")), np.clip(a, 0, 1), clamp=False)


def main() -> None:
    print(f"Hedef: {HEDEF}")
    rng = np.random.default_rng(20261108)
    pulsar_cekirdek(rng)
    pulsar_hale(rng)
    pulsar_korona(rng)
    pulsar_isin_keskin(rng)
    rgb, a = karadelik_buyuk(rng)
    kaydet_olcekli("karadelik_buyuk", rgb, a, [512, 256])
    karadelik_disk_akis(rng)
    karadelik_mercek(rng)
    karadelik_foton()
    spagetti(rng)
    isi_dalgasi(rng)


if __name__ == "__main__":
    main()
