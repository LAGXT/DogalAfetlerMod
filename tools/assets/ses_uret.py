#!/usr/bin/env python3
"""Siren seslerini sentezler (docs/tasarim/ses.md §2, §4; SARTNAME madde 5, N19).

Her ses deterministik seed'le 48 kHz'te numpy ile üretilir, 16 bit WAV'a yazılır ve ffmpeg ile mono Ogg Vorbis'e
(`-c:a libvorbis -q:a 5`) çevrilir. Döngülü dosyalar matematiksel olarak periyodiktir: osilatörler döngü boyunca tam
sayı tur atar, pembe gürültü FFT'de üretilir, alçak geçiren filtre ve yankı FFT alanında dairesel uygulanır; sonra
sinyal yükselen bir sıfır geçişinden başlayacak şekilde döndürülür. Böylece dosyanın sonu başına dikişsiz bağlanır.

Kullanım (KÖK'ten):
    .venv/bin/python tools/assets/ses_uret.py                # iki siren, ölçüm ve spektrogram
    .venv/bin/python tools/assets/ses_uret.py siren_gunes    # yalnız biri

Çıktılar:
    src/main/resources/assets/dogalafetler/sounds/<ad>.ogg
    docs/kanit/ses/<ad>_ebur128.txt   (ffmpeg ebur128 özeti + baskın frekans)
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


@dataclass(frozen=True)
class SesTanimi:
    uret: Callable[[], np.ndarray]
    hedef_lufs: float
    tepe_tavani_dbtp: float = -3.0


SESLER: dict[str, SesTanimi] = {
    "siren_standart": SesTanimi(siren_standart, -20.0),
    "siren_gunes": SesTanimi(siren_gunes, -21.0),
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
    ozet = cikti[cikti.rfind("Summary:"):]
    lufs = float(re.search(r"I:\s+(-?[\d.]+) LUFS", ozet).group(1))
    tepe = float(re.search(r"True peak:\s+Peak:\s+(-?[\d.]+|-inf) dBFS", ozet).group(1))
    return lufs, tepe, ozet.strip()


def baskin_frekanslar(sinyal: np.ndarray, adet: int = 3) -> list[float]:
    """Uzun süreli spektrumda en güçlü bileşenlerin frekansları (Hz, 20 Hz'lik kümelere ayrılmış)."""
    spektrum = np.abs(np.fft.rfft(sinyal * np.hanning(sinyal.size)))
    frekanslar = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    kume = np.round(frekanslar / 20.0).astype(int)
    enerji = np.bincount(kume, weights=spektrum**2)
    en_iyi = np.argsort(enerji)[::-1][:adet]
    return [float(k * 20) for k in en_iyi]


def uret(ad: str, tanim: SesTanimi, gecici: Path) -> None:
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
    ffmpeg("-y", "-i", str(wav), "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", str(ogg))

    lufs, tepe, ozet = ebur128(ogg)
    sure = sinyal.size / ORNEKLEME
    # Dikişteki adım (son → ilk örnek), sinyalin içindeki en büyük örnekten örneğe adımdan küçükse döngüde tıkırtı yoktur.
    dikis = abs(float(sinyal[0] - sinyal[-1])) / float(np.max(np.abs(np.diff(sinyal))))
    baskin = baskin_frekanslar(sinyal)
    rapor = (
        f"Dosya: {ogg.relative_to(KOK)} ({ogg.stat().st_size} bayt, {sure:.1f} sn, mono, {ORNEKLEME} Hz, Vorbis q5)\n"
        f"Hedef: {tanim.hedef_lufs:.1f} LUFS, tepe <= {tanim.tepe_tavani_dbtp:.1f} dBTP\n"
        f"Ölçülen (ogg): I = {lufs:.1f} LUFS, gerçek tepe = {tepe:.1f} dBTP\n"
        f"Baskın frekanslar (20 Hz kümeleri, güçlüden zayıfa): {', '.join(f'{f:.0f} Hz' for f in baskin)}\n"
        f"Döngü dikişi: |ilk - son| / en büyük iç adım = {dikis:.3f} (<= 1 ise süreksizlik yok); ilk örnek = {sinyal[0]:+.5f}\n\n"
        f"ffmpeg -i {ogg.name} -af ebur128=peak=true -f null -\n{ozet}\n"
    )
    (KANIT_KLASORU / f"{ad}_ebur128.txt").write_text(rapor, encoding="utf-8")
    ffmpeg("-y", "-i", str(ogg), "-lavfi",
           "showspectrumpic=s=1200x600:legend=1:fscale=log:start=40:stop=8000",
           str(KANIT_KLASORU / f"{ad}_spektrogram.png"))
    print(rapor)

    if abs(lufs - tanim.hedef_lufs) > 1.0 or tepe > tanim.tepe_tavani_dbtp:
        raise SystemExit(f"{ad}: hedef dışında (I={lufs:.1f} LUFS, tepe={tepe:.1f} dBTP)")


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
    with tempfile.TemporaryDirectory() as gecici:
        for ad in adlar:
            uret(ad, SESLER[ad], Path(gecici))


if __name__ == "__main__":
    sys.exit(main())
