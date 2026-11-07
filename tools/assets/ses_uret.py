#!/usr/bin/env python3
"""Siren ve örnek ortam seslerini sentezler (docs/tasarim/ses.md §2, §4; SARTNAME madde 5, N19).

Her ses deterministik seed'le 48 kHz'te numpy ile üretilir, 16 bit WAV'a yazılır ve ffmpeg ile mono Ogg Vorbis'e
(`-c:a libvorbis -q:a 5`) çevrilir. Döngülü dosyalar matematiksel olarak periyodiktir: osilatörler döngü boyunca tam
sayı tur atar, pembe gürültü FFT'de üretilir, filtreler ve yankı FFT alanında dairesel uygulanır; sonra sinyal yükselen
bir sıfır geçişinden başlayacak şekilde döndürülür. Böylece dosyanın sonu başına dikişsiz bağlanır. Açılış ve fade-out
dosyada değil oyundaki zarftadır (uyari.SirenZarfi, uyari.HacimEgrisi).

Çıktılar bayt düzeyinde tekrarlanır: Ogg akış seri numarası sabit (`-serial_offset`, `+bitexact`), etiket yok
(`-map_metadata -1`); ölçüm metnine ffmpeg'in süreç adresi ve hız satırları girmez. Determinizm araç sürümüne bağlıdır
(doğrulanan: ffmpeg n9.0.2 + libvorbis, numpy 2.5.4).

Kullanım (worktree kökünden):
    ../../.venv/bin/python tools/assets/ses_uret.py               # bütün sesler, ölçüm ve spektrogram
    ../../.venv/bin/python tools/assets/ses_uret.py siren_gunes   # yalnız biri

Çıktılar:
    src/main/resources/assets/dogalafetler/sounds/<ad>.ogg
    docs/kanit/ses/<ad>_ebur128.txt   (ffmpeg ebur128 özeti, baskın frekans, bant enerjileri, tasarım hedefi)
    docs/kanit/ses/<ad>_spektrogram.png
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import tempfile
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

import numpy as np

ORNEKLEME = 48_000
# Ogg akış seri numarası (sabit: aynı girdi → aynı bayt). Tek akışlı dosyalarda değerin kendisi önemsizdir.
OGG_SERI_NUMARASI = 20261105
KOK = Path(__file__).resolve().parents[2]
SES_KLASORU = KOK / "src/main/resources/assets/dogalafetler/sounds"
KANIT_KLASORU = KOK / "docs/kanit/ses"


def db(deger: float) -> float:
    """Desibeli doğrusal genliğe çevirir."""
    return 10.0 ** (deger / 20.0)


def yumusak(u: np.ndarray) -> np.ndarray:
    """0→1 kosinüs geçişi; uçlarda eğim sıfır, perde kayması ani sıçrama yapmaz."""
    return (1.0 - np.cos(np.pi * np.clip(u, 0.0, 1.0))) / 2.0


def parcali_egri(t: np.ndarray, noktalar: list[tuple[float, float]], periyot: float) -> np.ndarray:
    """(zaman, değer) noktaları arasında kosinüs geçişli periyodik eğri. Son nokta ilk değere dönmelidir."""
    yerel = np.mod(t, periyot)
    sonuc = np.full_like(t, noktalar[0][1])
    for (t0, d0), (t1, d1) in zip(noktalar, noktalar[1:]):
        maske = (yerel >= t0) & (yerel < t1)
        sonuc[maske] = d0 + (d1 - d0) * yumusak((yerel[maske] - t0) / (t1 - t0))
    return sonuc


def tam_turlu_faz(frekans: np.ndarray) -> np.ndarray:
    """Anlık frekanstan faz üretir; frekansı döngü boyunca tam sayı tur atacak kadar (binde birden az) ölçekler."""
    tur = frekans.sum() / ORNEKLEME
    frekans = frekans * (round(tur) / tur)
    return 2.0 * np.pi * np.cumsum(frekans) / ORNEKLEME


def pembe_gurultu(n: int, uretec: np.random.Generator, alt: float = 60.0, ust: float = 8000.0) -> np.ndarray:
    """FFT'de 1/sqrt(f) eğimli, bant sınırlı pembe gürültü; ters FFT'den geldiği için kendiliğinden periyodik."""
    frekanslar = np.fft.rfftfreq(n, 1.0 / ORNEKLEME)
    spektrum = uretec.normal(size=frekanslar.size) + 1j * uretec.normal(size=frekanslar.size)
    egim = np.zeros_like(frekanslar)
    bant = (frekanslar >= alt) & (frekanslar <= ust)
    egim[bant] = 1.0 / np.sqrt(frekanslar[bant])
    gurultu = np.fft.irfft(spektrum * egim, n)
    return gurultu / np.sqrt(np.mean(gurultu**2))


def alcak_geciren(sinyal: np.ndarray, kesim: float) -> np.ndarray:
    """2. derece Butterworth genlik yanıtı (−12 dB/oktav), sıfır fazlı ve dairesel: döngü dikişini bozmaz."""
    frekanslar = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    yanit = 1.0 / np.sqrt(1.0 + (frekanslar / kesim) ** 4)
    return np.fft.irfft(np.fft.rfft(sinyal) * yanit, sinyal.size)


def konusma_centigi(sinyal: np.ndarray, merkez: float = 1000.0, derinlik_db: float = -6.0,
                    genislik_oktav: float = 2.5) -> np.ndarray:
    """Konuşma bandına (300–3400 Hz) geniş çentik (ses.md §4 kural 1: 1 kHz merkezli, Q 0,5 ≈ 2,5 oktav, −6 dB).
    Log-frekansta Gauss biçimli kazanç; yarı yükseklikte tam genişlik `genislik_oktav`. Sıfır fazlı ve dairesel."""
    frekanslar = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    oktav = np.log2(np.maximum(frekanslar, 1.0) / merkez)
    sigma = genislik_oktav / 2.355
    kazanc_db = derinlik_db * np.exp(-0.5 * (oktav / sigma) ** 2)
    return np.fft.irfft(np.fft.rfft(sinyal) * 10.0 ** (kazanc_db / 20.0), sinyal.size)


def dairesel_yanki(sinyal: np.ndarray, uretec: np.random.Generator, rt60: float, islak: float,
                   on_gecikme: float = 0.06) -> np.ndarray:
    """Üstel sönen gürültü dürtü yanıtıyla dairesel evrişim (sentetik vadi yankısı); döngü sonundaki kuyruk başa
    sarılır, böylece döngü noktasında yankı kesilmez."""
    n = sinyal.size
    uzunluk = int(rt60 * ORNEKLEME)
    t = np.arange(uzunluk) / ORNEKLEME
    durtu = uretec.normal(size=uzunluk) * np.exp(-6.91 * t / rt60)
    durtu = alcak_geciren(np.pad(durtu, (0, n - uzunluk)), 1500.0)
    durtu = np.roll(durtu, int(on_gecikme * ORNEKLEME))
    yankili = np.fft.irfft(np.fft.rfft(sinyal) * np.fft.rfft(durtu), n)
    yankili *= np.sqrt(np.mean(sinyal**2) / np.mean(yankili**2))
    return (1.0 - islak) * sinyal + islak * yankili


def sifir_gecisine_dondur(sinyal: np.ndarray) -> np.ndarray:
    """Periyodik sinyali ilk yükselen sıfır geçişinden başlayacak şekilde döndürür."""
    gecisler = np.nonzero((sinyal[:-1] < 0.0) & (sinyal[1:] >= 0.0))[0]
    return np.roll(sinyal, -(int(gecisler[0]) + 1))


def siren_standart() -> np.ndarray:
    """Standart siren: 280↔420 Hz wail, 5 sn periyot (2,0 sn yükseliş, 0,5 sn tepe, 2,5 sn iniş), 10 sn döngü."""
    uretec = np.random.default_rng(20261105)
    sure, periyot, alt, ust = 10.0, 5.0, 280.0, 420.0
    t = np.arange(int(sure * ORNEKLEME)) / ORNEKLEME
    perde = parcali_egri(t, [(0.0, alt), (2.0, ust), (2.5, ust), (5.0, alt)], periyot)

    # Ana osilatör + 2. ve 3. harmonik (yumuşak üfleme tonu); ikinci osilatör +0,3 Hz sapmalı, -10 dB (canlılık).
    faz = tam_turlu_faz(perde)
    faz2 = tam_turlu_faz(perde + 0.3)
    ton = (np.sin(faz) + db(-12) * np.sin(2 * faz) + db(-22) * np.sin(3 * faz)
           + db(-10) * np.sin(faz2))
    # Mekanik sirende devir arttıkça ses biraz güçlenir.
    ton *= 0.8 + 0.2 * (perde - alt) / (ust - alt)

    hava = pembe_gurultu(t.size, uretec) * np.sqrt(np.mean(ton**2)) * db(-30)
    sinyal = alcak_geciren(ton + hava, 2500.0)
    return sifir_gecisine_dondur(sinyal)


def siren_gunes() -> np.ndarray:
    """Güneş patlaması sireni: 180 → 300 → 240 → 180 Hz, 10 sn periyot, 0,7 Hz vuruşlu ikinci osilatör, 55 Hz
    drone, 2,5 sn yankı kuyruğu; 20 sn döngü."""
    uretec = np.random.default_rng(20261106)
    sure, periyot = 20.0, 10.0
    t = np.arange(int(sure * ORNEKLEME)) / ORNEKLEME
    perde = parcali_egri(t, [(0.0, 180.0), (3.5, 300.0), (4.0, 300.0), (6.5, 240.0), (8.0, 240.0), (10.0, 180.0)],
                         periyot)

    faz = tam_turlu_faz(perde)
    faz2 = tam_turlu_faz(perde + 0.7)
    ton = (np.sin(faz) + db(-10) * np.sin(2 * faz) + db(-20) * np.sin(3 * faz)
           + db(-3) * (np.sin(faz2) + db(-10) * np.sin(2 * faz2)))
    ton *= 0.85 + 0.15 * (perde - 180.0) / 120.0
    drone = db(-18) * np.sqrt(2.0 * np.mean(ton**2)) * np.sin(tam_turlu_faz(np.full_like(t, 55.0)))

    kuru = ton + drone
    hava = pembe_gurultu(t.size, uretec, alt=40.0, ust=6000.0) * np.sqrt(np.mean(kuru**2)) * db(-32)
    sinyal = dairesel_yanki(kuru + hava, uretec, rt60=2.5, islak=0.35)
    sinyal = alcak_geciren(sinyal, 2000.0)
    return sifir_gecisine_dondur(sinyal)


def ortam_test() -> np.ndarray:
    """Örnek ortam katmanı ("uzak bir uğultu", uyari.OrtamSesi API'sinin sınama sesi): 45 ve 67,5 Hz kısmi tonlar
    0,25 Hz'de nefes alır, altında 30–180 Hz pembe gürültü gürlemesi, çok düşük 4–9 kHz hışırtı; konuşma bandına −6 dB
    çentik (ses.md §4 kural 1). 12 sn döngü (0,25 Hz × 12 sn = 3 tam nefes)."""
    uretec = np.random.default_rng(20261107)
    sure = 12.0
    t = np.arange(int(sure * ORNEKLEME)) / ORNEKLEME
    nefes = 0.7 + 0.3 * np.sin(2.0 * np.pi * 0.25 * t)
    ton = (np.sin(tam_turlu_faz(np.full_like(t, 45.0)))
           + db(-6) * np.sin(tam_turlu_faz(np.full_like(t, 67.5)) + 0.4)) * nefes
    gurleme = alcak_geciren(pembe_gurultu(t.size, uretec, alt=30.0, ust=400.0), 180.0)
    gurleme *= np.sqrt(np.mean(ton**2)) * db(-4) / np.sqrt(np.mean(gurleme**2))
    hisirti = pembe_gurultu(t.size, uretec, alt=4000.0, ust=9000.0) * np.sqrt(np.mean(ton**2)) * db(-30)
    sinyal = konusma_centigi(ton + gurleme + hisirti)
    return sifir_gecisine_dondur(sinyal)


def kasirga_ugultu() -> np.ndarray:
    """Kasırga uzak katmanı (docs/tasarim/kasirga.md §4): "yük treni" uğultusu. 40–120 Hz pembe gürültü gürlemesi,
    0,25 ve 0,375 Hz'de yavaş genlik dalgalanması, 52 ve 78 Hz kısmi tonlar çok hafif perde sallanımıyla; çok düşük
    300–900 Hz hışırtı (ray sürtünmesi), konuşma bandına −6 dB çentik. 16 sn döngü (bütün modülasyonlar tam sayı tur)."""
    uretec = np.random.default_rng(20261108)
    sure = 16.0
    t = np.arange(int(sure * ORNEKLEME)) / ORNEKLEME
    gurleme = alcak_geciren(pembe_gurultu(t.size, uretec, alt=35.0, ust=160.0), 120.0)
    gurleme /= np.sqrt(np.mean(gurleme**2))
    dalga = 0.72 + 0.18 * np.sin(2.0 * np.pi * 0.25 * t) + 0.10 * np.sin(2.0 * np.pi * 0.375 * t + 1.1)
    ton = (np.sin(tam_turlu_faz(52.0 + 1.5 * np.sin(2.0 * np.pi * 0.125 * t)))
           + db(-5) * np.sin(tam_turlu_faz(78.0 + 2.0 * np.sin(2.0 * np.pi * 0.125 * t + 0.7))))
    ton *= db(-8) / np.sqrt(np.mean(ton**2))
    hisirti = pembe_gurultu(t.size, uretec, alt=300.0, ust=900.0)
    hisirti *= db(-24) / np.sqrt(np.mean(hisirti**2))
    sinyal = konusma_centigi((gurleme + ton) * dalga + hisirti * dalga)
    return sifir_gecisine_dondur(sinyal)


def kasirga_ruzgar() -> np.ndarray:
    """Kasırga yakın katmanı (§4 yakın/orta): derin rüzgâr kükremesi (30–220 Hz) 2 Hz basınç "pompasıyla", üstünde 0,25 Hz
    şiddetlenen rüzgâr uğultusu (500–1500 Hz, düşük düzey) ve 4–8 kHz enkaz hışırtısı; konuşma bandına −6 dB çentik
    (orta frekans bilinçli oyuk, ses.md). 12 sn döngü (2 Hz × 12 = 24, 0,25 Hz × 12 = 3 tur)."""
    uretec = np.random.default_rng(20261109)
    sure = 12.0
    t = np.arange(int(sure * ORNEKLEME)) / ORNEKLEME
    kukreme = alcak_geciren(pembe_gurultu(t.size, uretec, alt=30.0, ust=300.0), 220.0)
    kukreme /= np.sqrt(np.mean(kukreme**2))
    pompa = 0.75 + 0.25 * np.sin(2.0 * np.pi * 2.0 * t)
    ruzgar_zarfi = 0.6 + 0.4 * np.sin(2.0 * np.pi * 0.25 * t + 0.5)
    ruzgar = pembe_gurultu(t.size, uretec, alt=500.0, ust=1500.0)
    ruzgar *= db(-14) / np.sqrt(np.mean(ruzgar**2))
    hisirti = pembe_gurultu(t.size, uretec, alt=4000.0, ust=8000.0)
    hisirti *= db(-31) / np.sqrt(np.mean(hisirti**2))
    sinyal = konusma_centigi(kukreme * pompa + (ruzgar + hisirti) * ruzgar_zarfi)
    return sifir_gecisine_dondur(sinyal)


def meteor_ucus() -> np.ndarray:
    """Meteor uçuşu (tasarım notu "Meteor" → "Ses", kullanıcı isteği 2026-11-07): vanilla rüzgâr ıslığı yerine alçak yanma
    kükremesi. 35–90 Hz ve 90–220 Hz pembe gürültü bantları 0,25 Hz'de çapraz karışır (spektrum ağırlık merkezi aşağı-yukarı
    kayar: geçen cismin kayması), altında 70 Hz'den 95 Hz'e süzülüp dönen kısmi ton, 6 Hz plazma çalkantısı ve çok düşük
    3–6 kHz yanma cızırtısı; konuşma bandına −6 dB çentik. Doppler perdesi oyunda koddadır (dinleyiciye yaklaşma hızı).
    8 sn döngü (0,25 Hz × 8 = 2, 6 Hz × 8 = 48 tur)."""
    uretec = np.random.default_rng(20261110)
    sure = 8.0
    t = np.arange(int(sure * ORNEKLEME)) / ORNEKLEME
    alt = alcak_geciren(pembe_gurultu(t.size, uretec, alt=35.0, ust=120.0), 90.0)
    alt /= np.sqrt(np.mean(alt**2))
    ust = alcak_geciren(pembe_gurultu(t.size, uretec, alt=90.0, ust=320.0), 220.0)
    ust /= np.sqrt(np.mean(ust**2))
    kayma = 0.5 + 0.5 * np.sin(2.0 * np.pi * 0.25 * t)
    kukreme = np.sqrt(1.0 - kayma) * alt + np.sqrt(kayma) * ust * db(-3)
    ton = np.sin(tam_turlu_faz(70.0 + 25.0 * kayma)) * db(-9)
    calkanti = 0.85 + 0.15 * np.sin(2.0 * np.pi * 6.0 * t)
    cizirti = pembe_gurultu(t.size, uretec, alt=3000.0, ust=6000.0) * db(-30)
    sinyal = konusma_centigi((kukreme + ton) * calkanti + cizirti * kayma)
    return sifir_gecisine_dondur(sinyal)


def meteor_patlama() -> np.ndarray:
    """Meteor çarpması (tek seferlik, 7 sn): 4 ms kırılma darbesi, 90 → 32 Hz'e çöken göğüs vuruşu, 30–300 Hz gövde
    gürültüsü, 0,3 sn'de dolup 2,2 sn zaman sabitiyle sönen 20–120 Hz uğultu kuyruğu (0,7 Hz yuvarlanma) ve 0,2–2,5 sn'ye
    dağılmış seyrek çakıl düşüşü; son 1 sn kosinüsle sıfıra iner. Yumuşak sınırlayıcı (tanh) tepe/ses yüksekliği oranını
    −6 dBTP / −23 LUFS'a sığdırır (ses.md §4 kural 2)."""
    uretec = np.random.default_rng(20261111)
    sure = 7.0
    n = int(sure * ORNEKLEME)
    t = np.arange(n) / ORNEKLEME
    darbe = alcak_geciren(uretec.normal(size=n) * (t < 0.004), 4000.0)
    darbe *= db(-4) / np.max(np.abs(darbe))
    vurus = np.sin(tam_turlu_faz(32.0 + 58.0 * np.exp(-t / 0.08))) * (1.0 - np.exp(-t / 0.003)) * np.exp(-t / 0.35)
    govde = alcak_geciren(pembe_gurultu(n, uretec, alt=30.0, ust=300.0), 150.0)
    govde *= db(-6) / np.sqrt(np.mean(govde[: ORNEKLEME // 2] ** 2)) * (1.0 - np.exp(-t / 0.01)) * np.exp(-t / 0.6)
    kuyruk = alcak_geciren(pembe_gurultu(n, uretec, alt=20.0, ust=160.0), 120.0)
    kuyruk /= np.sqrt(np.mean(kuyruk**2))
    kuyruk *= db(-9) * (1.0 - np.exp(-t / 0.3)) * np.exp(-t / 2.2) * (0.8 + 0.2 * np.sin(2.0 * np.pi * 0.7 * t))
    cakil = np.zeros(n)
    for an in uretec.uniform(0.2, 2.5, size=60):
        i = int(an * ORNEKLEME)
        boy = int(0.006 * ORNEKLEME)
        cakil[i:i + boy] += uretec.normal(size=boy) * np.exp(-np.arange(boy) / (0.0015 * ORNEKLEME)) * np.exp(-an / 1.2)
    cakil = bant_gecirgen(cakil, 1000.0, 4000.0) * db(-24) / max(np.max(np.abs(cakil)), 1e-9)
    sinyal = darbe + vurus + govde + kuyruk + cakil
    sinyal *= np.where(t > sure - 1.0, yumusak((sure - t) / 1.0), 1.0)
    sinyal /= np.max(np.abs(sinyal))
    return np.tanh(2.5 * sinyal) / np.tanh(2.5)


def bant_gecirgen(sinyal: np.ndarray, alt: float, ust: float) -> np.ndarray:
    """Sinyali [alt, ust] Hz bandına sınırlar (sıfır fazlı FFT maskesi; tek seferlik seslerde döngü gerekmez)."""
    frekanslar = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    maske = (frekanslar >= alt) & (frekanslar <= ust)
    return np.fft.irfft(np.fft.rfft(sinyal) * maske, sinyal.size)


@dataclass(frozen=True)
class SesTanimi:
    uret: Callable[[], np.ndarray]
    hedef_lufs: float
    tepe_tavani_dbtp: float
    # Tasarım notundaki hedef (ses.md §4): baskın frekans bandı ve tiz sınırı; ölçümle karşılaştırılır.
    baskin_bant_hz: tuple[float, float]
    tiz_esik_hz: float
    tiz_tavani_db: float
    # Tek seferlik seste (çarpma) döngü dikişi ölçülmez.
    dongu: bool = True


SESLER: dict[str, SesTanimi] = {
    # "Sakin ama fark edilir": baskın 280–420 Hz, 2,5 kHz üstü en az 30 dB aşağıda.
    "siren_standart": SesTanimi(siren_standart, -20.0, -3.0, (280.0, 420.0), 2500.0, -30.0),
    # Daha alçak bant 180–300 Hz, 2 kHz alçak geçiren.
    "siren_gunes": SesTanimi(siren_gunes, -21.0, -3.0, (180.0, 300.0), 2500.0, -30.0),
    # Döngülü ortam sesi: −26 LUFS, tepe ≤ −6 dBTP (kural 2); enerji alt basta.
    "ortam_test": SesTanimi(ortam_test, -26.0, -6.0, (30.0, 120.0), 2500.0, -30.0),
    # Kasırga uzak uğultusu: döngülü ortam sesi kuralı (−6 dBTP), "yük treni" 40–120 Hz.
    "kasirga_ugultu": SesTanimi(kasirga_ugultu, -26.0, -6.0, (40.0, 120.0), 2500.0, -30.0),
    # Kasırga yakın rüzgârı: derin kükreme 30–220 Hz baskın; 2,5 kHz üstü en az 30 dB aşağıda.
    "kasirga_ruzgar": SesTanimi(kasirga_ruzgar, -26.0, -6.0, (30.0, 220.0), 2500.0, -30.0),
    # Meteor uçuşu: döngülü ortam sesi kuralı (−26 LUFS, −6 dBTP), alçak kükreme 35–220 Hz baskın; cızırtı 2,5 kHz üstü ≤30 dB aşağıda.
    "meteor_ucus": SesTanimi(meteor_ucus, -26.0, -6.0, (35.0, 220.0), 2500.0, -30.0),
    # Meteor çarpması: tek seferlik darbe (−23 LUFS, tepe ≤ −6 dBTP), vuruş ve uğultu 20–120 Hz baskın.
    "meteor_patlama": SesTanimi(meteor_patlama, -23.0, -6.0, (20.0, 120.0), 2500.0, -25.0, dongu=False),
}


def wav_yaz(yol: Path, sinyal: np.ndarray, uretec: np.random.Generator) -> None:
    """16 bit mono WAV; TPDF dither ile nicemleme."""
    dither = (uretec.random(sinyal.size) - uretec.random(sinyal.size)) / 32768.0
    tamsayi = np.clip(np.round((sinyal + dither) * 32767.0), -32768, 32767).astype("<i2")
    with wave.open(str(yol), "wb") as dosya:
        dosya.setnchannels(1)
        dosya.setsampwidth(2)
        dosya.setframerate(ORNEKLEME)
        dosya.writeframes(tamsayi.tobytes())


def ffmpeg(*argumanlar: str) -> str:
    sonuc = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", *argumanlar], capture_output=True, text=True)
    if sonuc.returncode != 0:
        raise RuntimeError(f"ffmpeg başarısız ({sonuc.returncode}):\n{sonuc.stderr}")
    return sonuc.stderr


def ebur128(yol: Path) -> tuple[float, float, str]:
    """(entegre LUFS, gerçek tepe dBTP, ffmpeg özet metni)."""
    cikti = ffmpeg("-i", str(yol), "-af", "ebur128=peak=true", "-f", "null", "-")
    # Özetin ardındaki `[out#0/null @ 0x...]` ve `speed=... elapsed=...` satırları her çalıştırmada değişir; alınmaz.
    ozet = re.search(r"Summary:.*?True peak:\s+Peak:\s+\S+ dBFS", cikti, re.S).group(0)
    lufs = float(re.search(r"I:\s+(-?[\d.]+) LUFS", ozet).group(1))
    tepe = float(re.search(r"True peak:\s+Peak:\s+(-?[\d.]+|-inf) dBFS", ozet).group(1))
    return lufs, tepe, ozet


def ogg_coz(yol: Path) -> np.ndarray:
    """Ogg dosyasını ffmpeg ile 48 kHz mono float örneklere çözer (ölçüm oyunun çalacağı dosyadan yapılır)."""
    sonuc = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(yol), "-f", "f32le", "-ac", "1",
                            "-ar", str(ORNEKLEME), "-"], capture_output=True)
    if sonuc.returncode != 0:
        raise RuntimeError(f"ffmpeg çözme başarısız: {sonuc.stderr.decode(errors='replace')}")
    return np.frombuffer(sonuc.stdout, dtype="<f4").astype(np.float64)


def baskin_frekanslar(sinyal: np.ndarray, adet: int = 3) -> list[float]:
    """Uzun süreli spektrumda en güçlü bileşenlerin frekansları (Hz, 20 Hz'lik kümelere ayrılmış)."""
    spektrum = np.abs(np.fft.rfft(sinyal * np.hanning(sinyal.size)))
    frekanslar = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    kume = np.round(frekanslar / 20.0).astype(int)
    enerji = np.bincount(kume, weights=spektrum**2)
    en_iyi = np.argsort(enerji)[::-1][:adet]
    return [float(k * 20) for k in en_iyi]


def pencere_baskinlari(sinyal: np.ndarray, pencere: float = 0.5) -> list[float]:
    """Ardışık `pencere` sn'lik parçalarda en güçlü frekans (perde izinin kaba ölçümü)."""
    boy = int(pencere * ORNEKLEME)
    frekanslar = np.fft.rfftfreq(boy, 1.0 / ORNEKLEME)
    sonuc = []
    for bas in range(0, sinyal.size - boy + 1, boy):
        spektrum = np.abs(np.fft.rfft(sinyal[bas:bas + boy] * np.hanning(boy)))
        sonuc.append(float(frekanslar[int(np.argmax(spektrum))]))
    return sonuc


def bant_enerjisi_db(sinyal: np.ndarray, alt: float, ust: float) -> float:
    """[alt, ust) Hz bandındaki enerjinin toplam enerjiye oranı (dB)."""
    spektrum = np.abs(np.fft.rfft(sinyal)) ** 2
    frekanslar = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    bant = (frekanslar >= alt) & (frekanslar < ust)
    return float(10.0 * np.log10(max(spektrum[bant].sum(), 1e-30) / spektrum.sum()))


def uret(ad: str, tanim: SesTanimi, gecici: Path) -> list[str]:
    sinyal = tanim.uret()
    sinyal = sinyal / np.max(np.abs(sinyal)) * db(-12)
    uretec = np.random.default_rng(1)
    wav = gecici / f"{ad}.wav"

    # Kazanç ffmpeg'in K ağırlıklı ölçümüne göre iki turda hedef LUFS'a oturtulur.
    for _ in range(2):
        wav_yaz(wav, sinyal, uretec)
        lufs, _, _ = ebur128(wav)
        sinyal = sinyal * db(tanim.hedef_lufs - lufs)
    wav_yaz(wav, sinyal, uretec)

    ogg = SES_KLASORU / f"{ad}.ogg"
    # Ogg muxer seri numarasını aksi halde rastgele seçer ve sayfa CRC'leri de ona göre değişir.
    ffmpeg("-y", "-i", str(wav), "-map_metadata", "-1", "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
           "-fflags", "+bitexact", "-flags:a", "+bitexact", "-serial_offset", str(OGG_SERI_NUMARASI), str(ogg))

    lufs, tepe, ozet = ebur128(ogg)
    sure = sinyal.size / ORNEKLEME
    # Dikişteki adım (son → ilk örnek), sinyalin içindeki en büyük örnekten örneğe adımdan küçükse döngüde tıkırtı yoktur.
    dikis = abs(float(sinyal[0] - sinyal[-1])) / float(np.max(np.abs(np.diff(sinyal))))
    cozulmus = ogg_coz(ogg)
    baskin = baskin_frekanslar(cozulmus)
    iz = pencere_baskinlari(cozulmus)
    tiz = bant_enerjisi_db(cozulmus, tanim.tiz_esik_hz, ORNEKLEME / 2)
    ust_tiz = bant_enerjisi_db(cozulmus, 8000.0, ORNEKLEME / 2)
    konusma = bant_enerjisi_db(cozulmus, 300.0, 3400.0)
    hedef_bant = bant_enerjisi_db(cozulmus, *tanim.baskin_bant_hz)
    alt, ust = tanim.baskin_bant_hz
    baskin_uygun = alt - 20.0 <= baskin[0] <= ust + 20.0
    hatalar = []
    if abs(lufs - tanim.hedef_lufs) > 1.0:
        hatalar.append(f"I={lufs:.1f} LUFS hedef {tanim.hedef_lufs:.1f} ±1")
    if tepe > tanim.tepe_tavani_dbtp:
        hatalar.append(f"tepe {tepe:.1f} dBTP > {tanim.tepe_tavani_dbtp:.1f}")
    if not baskin_uygun:
        hatalar.append(f"baskın {baskin[0]:.0f} Hz hedef bant {alt:.0f}–{ust:.0f} Hz dışında")
    if tiz > tanim.tiz_tavani_db:
        hatalar.append(f"{tanim.tiz_esik_hz:.0f} Hz üstü {tiz:.1f} dB > {tanim.tiz_tavani_db:.0f} dB")
    rapor = (
        f"Dosya: {ogg.relative_to(KOK)} ({ogg.stat().st_size} bayt, {sure:.1f} sn, mono, {ORNEKLEME} Hz, Vorbis q5)\n"
        f"Hedef (docs/tasarim/ses.md §4): {tanim.hedef_lufs:.1f} LUFS ±1, gerçek tepe <= {tanim.tepe_tavani_dbtp:.1f} dBTP, "
        f"baskın frekans {alt:.0f}–{ust:.0f} Hz, {tanim.tiz_esik_hz:.0f} Hz üstü <= {tanim.tiz_tavani_db:.0f} dB\n"
        f"Ölçülen (ogg): I = {lufs:.1f} LUFS, gerçek tepe = {tepe:.1f} dBTP\n"
        f"Baskın frekanslar (20 Hz kümeleri, güçlüden zayıfa): {', '.join(f'{f:.0f} Hz' for f in baskin)}\n"
        f"Perde izi (0,5 sn pencerelerde en güçlü frekans, ilk 20): {', '.join(f'{f:.0f}' for f in iz[:20])} Hz\n"
        f"Bant enerjisi / toplam: {alt:.0f}–{ust:.0f} Hz {hedef_bant:.1f} dB; 300–3400 Hz (konuşma) {konusma:.1f} dB; "
        f">{tanim.tiz_esik_hz:.0f} Hz {tiz:.1f} dB; >8000 Hz {ust_tiz:.1f} dB\n"
        + (f"Döngü dikişi: |ilk - son| / en büyük iç adım = {dikis:.3f} (<= 1 ise süreksizlik yok); ilk örnek = {sinyal[0]:+.5f}\n"
           if tanim.dongu else "Tek seferlik: döngü dikişi ölçülmez (son 1 sn kosinüsle sıfıra iner).\n")
        + f"Hedefle karşılaştırma: {'UYGUN' if not hatalar else 'UYGUN DEĞİL: ' + '; '.join(hatalar)}\n\n"
        f"ffmpeg -i {ogg.name} -af ebur128=peak=true -f null -\n{ozet}\n"
    )
    (KANIT_KLASORU / f"{ad}_ebur128.txt").write_text(rapor, encoding="utf-8")
    ffmpeg("-y", "-i", str(ogg), "-lavfi",
           "showspectrumpic=s=1200x600:legend=1:fscale=log:start=20:stop=10000",
           str(KANIT_KLASORU / f"{ad}_spektrogram.png"))
    print(rapor)
    return [f"{ad}: {hata}" for hata in hatalar]


def main() -> None:
    ayristirici = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ayristirici.add_argument("adlar", nargs="*", metavar="ad",
                             help=f"üretilecek sesler (varsayılan hepsi): {', '.join(SESLER)}")
    adlar = ayristirici.parse_args().adlar or list(SESLER)
    bilinmeyen = [ad for ad in adlar if ad not in SESLER]
    if bilinmeyen:
        ayristirici.error(f"bilinmeyen ses: {', '.join(bilinmeyen)}")

    SES_KLASORU.mkdir(parents=True, exist_ok=True)
    KANIT_KLASORU.mkdir(parents=True, exist_ok=True)
    hatalar: list[str] = []
    with tempfile.TemporaryDirectory() as gecici:
        for ad in adlar:
            hatalar += uret(ad, SESLER[ad], Path(gecici))
    if hatalar:
        raise SystemExit("hedef dışında:\n" + "\n".join(hatalar))


if __name__ == "__main__":
    sys.exit(main())
