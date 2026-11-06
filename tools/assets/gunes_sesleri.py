#!/usr/bin/env python3
"""Güneş patlaması ve kızıl cüce sesleri (bölüm D, tasarım `gunes_patlamasi.md` §4, madde 5 ses düzeyi, N19).

- `gunes_kavrulma_ruzgar`: döngülü sıcak rüzgâr (20 sn), 30–500 Hz kahverengimsi gürültü, 0,2 Hz nefes ve 0,05 Hz uzak yangın
  uğultusu; `OrtamSesi` katmanı (WEATHER), hedef −26 LUFS, tepe ≤ −6 dBTP, döngü tam sayı tur.
- `gunes_supernova_basinc`: tek seferlik 8 sn "basınç": 25 Hz'den 60 Hz'e çıkan sinüs + yavaşça açılan alçak gürültü.
- `gunes_supernova_kristal`: tek seferlik 20 sn uzak kristal uğultu (330/495/660/880 Hz, çiftler 0,6–0,9 Hz vuruşlu).
- `gunes_kizil_cuce_akor`: tek seferlik 4,5 sn yumuşak, sıcak akor (A2–E3–A3–C#4), düşük düzey.

Konuşma bandına −6 dB çentik (ses.md §4 kural 1). Üretim, ölçüm ve Ogg yazımı `ses_uret.py`'nin yardımcılarıyla
(deterministik); ölçüm ve spektrogram `docs/kanit/ses/` altında, kopyası `KOK/.vibe/kanit/gunes-patlamasi-kizil-cuce/ses/`.

Kullanım (worktree kökünden): ../../.venv/bin/python tools/assets/gunes_sesleri.py
Çıktı: src/main/resources/assets/dogalafetler/sounds/gunes_*.ogg (mono, 48 kHz, Vorbis q5).
"""

from __future__ import annotations

import shutil
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ses_uret as su  # noqa: E402

W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W


def zaman(sure: float) -> np.ndarray:
    return np.arange(int(sure * su.ORNEKLEME)) / su.ORNEKLEME


def kavrulma_ruzgar() -> np.ndarray:
    uretec = np.random.default_rng(20261108)
    t = zaman(20.0)
    ruzgar = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=30.0, ust=900.0), 320.0)
    ruzgar = su.alcak_geciren(ruzgar, 500.0)
    nefes = 0.62 + 0.38 * np.sin(2.0 * np.pi * 0.2 * t) * (0.7 + 0.3 * np.sin(2.0 * np.pi * 0.05 * t + 1.1))
    ugultu = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=40.0, ust=260.0), 150.0)
    ugultu *= (0.75 + 0.25 * np.sin(2.0 * np.pi * 0.05 * t)) * su.db(-5)
    hisirti = su.pembe_gurultu(t.size, uretec, alt=1200.0, ust=4000.0) * su.db(-34)
    sinyal = su.konusma_centigi(ruzgar * nefes + ugultu + hisirti * nefes)
    return su.sifir_gecisine_dondur(sinyal)


def supernova_basinc() -> np.ndarray:
    uretec = np.random.default_rng(20261109)
    sure = 8.0
    t = zaman(sure)
    frekans = 25.0 * (60.0 / 25.0) ** su.yumusak(t / sure)
    faz = 2.0 * np.pi * np.cumsum(frekans) / su.ORNEKLEME
    ton = np.sin(faz) + su.db(-10) * np.sin(2.0 * faz + 0.3)
    gurultu = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=25.0, ust=400.0), 160.0)
    gurultu *= su.yumusak(t / 6.0) * su.db(-7)
    zarf = su.yumusak(t / 0.6) * su.yumusak((sure - t) / 2.2)
    return su.konusma_centigi((ton + gurultu) * zarf)


def supernova_kristal() -> np.ndarray:
    uretec = np.random.default_rng(20261110)
    sure = 20.0
    t = zaman(sure)
    sinyal = np.zeros_like(t)
    for frekans, genlik, vurus in ((330.0, 1.0, 0.7), (495.0, 0.55, 0.9), (660.0, 0.42, 0.6), (880.0, 0.25, 0.8)):
        cift = np.sin(2.0 * np.pi * frekans * t) + np.sin(2.0 * np.pi * (frekans + vurus) * t + 1.3)
        sinyal += genlik * 0.5 * cift
    parilti = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=900.0, ust=2200.0), 2000.0) * su.db(-30)
    zarf = su.yumusak(t / 3.0) * su.yumusak((17.5 - t) / 9.0)
    kuru = (sinyal + parilti) * zarf
    return su.konusma_centigi(su.dairesel_yanki(kuru, uretec, rt60=2.2, islak=0.35))


def kizil_cuce_akor() -> np.ndarray:
    sure = 4.5
    t = zaman(sure)
    sinyal = np.zeros_like(t)
    for frekans, genlik in ((110.0, 1.0), (164.81, 0.6), (220.0, 0.5), (277.18, 0.38)):
        sinyal += genlik * (np.sin(2.0 * np.pi * frekans * t) + su.db(-14) * np.sin(4.0 * np.pi * frekans * t + 0.5))
    zarf = su.yumusak(t / 0.9) * np.exp(-np.maximum(t - 0.9, 0.0) / 2.2) * su.yumusak((sure - t) / 0.6)
    return su.konusma_centigi(sinyal * zarf)


SESLER = {
    "gunes_kavrulma_ruzgar": su.SesTanimi(kavrulma_ruzgar, -26.0, -6.0, (25.0, 260.0), 2500.0, -30.0),
    "gunes_supernova_basinc": su.SesTanimi(supernova_basinc, -25.0, -6.0, (20.0, 80.0), 2500.0, -30.0),
    "gunes_supernova_kristal": su.SesTanimi(supernova_kristal, -30.0, -6.0, (310.0, 700.0), 2500.0, -30.0),
    "gunes_kizil_cuce_akor": su.SesTanimi(kizil_cuce_akor, -27.0, -6.0, (90.0, 300.0), 2500.0, -30.0),
}


def main() -> int:
    su.SES_KLASORU.mkdir(parents=True, exist_ok=True)
    su.KANIT_KLASORU.mkdir(parents=True, exist_ok=True)
    hatalar: list[str] = []
    with tempfile.TemporaryDirectory() as gecici:
        for ad, tanim in SESLER.items():
            hatalar += su.uret(ad, tanim, Path(gecici))
    kopya = KOK / ".vibe/kanit/gunes-patlamasi-kizil-cuce/ses"
    kopya.mkdir(parents=True, exist_ok=True)
    for ad in SESLER:
        for dosya in su.KANIT_KLASORU.glob(f"{ad}_*"):
            shutil.copy2(dosya, kopya / dosya.name)
    if hatalar:
        raise SystemExit("hedef dışında:\n" + "\n".join(hatalar))
    return 0


if __name__ == "__main__":
    sys.exit(main())
