#!/usr/bin/env python3
"""Deprem ve sinkhole sesleri (SARTNAME bölüm C, F, madde 5 ses düzeyi, N19; docs/tasarim/ses.md §4, deprem.md §4).

- deprem_gurleme.ogg: 10 sn döngülü ana gürleme (uyari.OrtamSesi katmanı "deprem/gurleme", sinkhole çatlamasında da
  "sinkhole/gurleme"). 20–80 Hz pembe gürültü gürlemesi 0,4 Hz'de dalgalanır, altında 31 ve 47 Hz kısmi tonlar (döngüde tam
  sayı tur), üstünde 200 Hz'e kadar seyrek kaya çıtırtısı (dairesel yerleştirilmiş kısa darbeler); konuşma bandına −6 dB
  çentik. Hedef −26 LUFS, gerçek tepe ≤ −6 dBTP (döngülü ortam sesi kuralı, uyarı-ses §A4).
- sinkhole_cokus.ogg: 6 sn tek seferlik çöküş: 35 Hz'den 22 Hz'e inen derin güm, ardından uzun kum/çakıl akması (band
  geçiren gürültü, sönen zarf) ve boşluk yankısı; tepe yumuşak sıkıştırmayla (tanh) bastırılır. Hedef −22 LUFS, gerçek tepe ≤ −6 dBTP (tek seferlik darbe kuralı).

Üretim, ölçüm ve Ogg yazımı `ses_uret.py`'nin yardımcılarıyla (aynı deterministik ayarlar); ölçüm metni ve spektrogram
`docs/kanit/ses/` altına yazılır. Hedef dışı dosyada betik hatayla durur.

Kullanım (worktree kökünden):
    ../../.venv/bin/python tools/assets/deprem_ses.py
"""

from __future__ import annotations

import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ses_uret as su  # noqa: E402


def deprem_gurleme() -> np.ndarray:
    uretec = np.random.default_rng(20261142)
    sure = 10.0
    n = int(sure * su.ORNEKLEME)
    t = np.arange(n) / su.ORNEKLEME
    dalga = 0.75 + 0.25 * np.sin(2.0 * np.pi * 0.4 * t)
    gurleme = su.alcak_geciren(su.pembe_gurultu(n, uretec, alt=18.0, ust=200.0), 80.0) * dalga
    ton = (np.sin(su.tam_turlu_faz(np.full_like(t, 31.0)))
           + su.db(-5) * np.sin(su.tam_turlu_faz(np.full_like(t, 47.0)) + 0.8)) * dalga
    ton *= np.sqrt(np.mean(gurleme**2)) * su.db(-3) / np.sqrt(np.mean(ton**2))
    citirti = np.zeros(n)
    darbe_boy = int(0.012 * su.ORNEKLEME)
    darbe = uretec.standard_normal(darbe_boy) * np.exp(-np.linspace(0, 6, darbe_boy))
    for bas in uretec.integers(0, n, 60):
        idx = (bas + np.arange(darbe_boy)) % n
        citirti[idx] += darbe * uretec.uniform(0.3, 1.0)
    citirti = su.alcak_geciren(citirti, 200.0)
    citirti *= np.sqrt(np.mean(gurleme**2)) * su.db(-12) / max(np.sqrt(np.mean(citirti**2)), 1e-12)
    sinyal = su.konusma_centigi(gurleme + ton + citirti)
    return su.sifir_gecisine_dondur(sinyal)


def sinkhole_cokus() -> np.ndarray:
    uretec = np.random.default_rng(20261143)
    sure = 6.0
    n = int(sure * su.ORNEKLEME)
    t = np.arange(n) / su.ORNEKLEME
    frekans = 22.0 + 13.0 * np.exp(-t / 0.5)
    gum = np.sin(2.0 * np.pi * np.cumsum(frekans) / su.ORNEKLEME) * np.exp(-t / 0.9) * np.minimum(1.0, t / 0.01)
    akma = su.alcak_geciren(su.pembe_gurultu(n, uretec, alt=40.0, ust=1500.0), 700.0)
    akma *= np.clip((t - 0.2) / 0.4, 0, 1) * np.exp(-np.maximum(t - 0.6, 0) / 1.8)
    akma *= np.sqrt(np.mean(gum**2)) * su.db(-10) / np.sqrt(np.mean(akma**2))
    sinyal = su.dairesel_yanki(gum + akma, uretec, rt60=1.6, islak=0.25)
    sinyal = su.konusma_centigi(su.alcak_geciren(sinyal, 2000.0))
    sinyal[-int(0.3 * su.ORNEKLEME):] *= np.linspace(1, 0, int(0.3 * su.ORNEKLEME))
    # Tepe/etkin oranını düşüren yumuşak sıkıştırma: gümün ilk darbesi −6 dBTP tavanını aşmasın.
    sinyal = sinyal / np.max(np.abs(sinyal))
    return np.tanh(4.0 * sinyal) / np.tanh(4.0)


TANIMLAR = {
    "deprem_gurleme": su.SesTanimi(deprem_gurleme, -26.0, -6.0, (18.0, 80.0), 2500.0, -30.0),
    "sinkhole_cokus": su.SesTanimi(sinkhole_cokus, -22.0, -6.0, (18.0, 60.0), 2500.0, -30.0),
}


def main() -> None:
    su.SES_KLASORU.mkdir(parents=True, exist_ok=True)
    su.KANIT_KLASORU.mkdir(parents=True, exist_ok=True)
    hatalar: list[str] = []
    with tempfile.TemporaryDirectory() as gecici:
        for ad, tanim in TANIMLAR.items():
            hatalar += su.uret(ad, tanim, Path(gecici))
    if hatalar:
        raise SystemExit("hedef dışında:\n" + "\n".join(hatalar))


if __name__ == "__main__":
    sys.exit(main())
