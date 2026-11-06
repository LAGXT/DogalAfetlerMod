#!/usr/bin/env python3
"""Tsunami sesleri (şartname B, docs/tasarim/tsunami.md §4, ses.md §4, N19).

- `tsunami_ugultu`: yaklaşan dalganın jet/tren uğultusu, 12 sn kesintisiz döngü (ortam katmanı, `uyari.OrtamSesi`).
  40–150 Hz pembe gürültü gürlemesi, altında 0,5 Hz'de yuvarlanan dalga kütlesi (türbülans) ve 70 Hz civarında yavaşça
  kayan kaba bir ton; üstte çok düşük 2–5 kHz köpük hışıltısı; konuşma bandına −6 dB çentik (ses.md §4 kural 1).
  Hedef −26 LUFS, gerçek tepe ≤ −6 dBTP (uyarı-ses §A4 kural 1); bütün süzgeçler dairesel, döngü tam sayı tur.
- `tsunami_carpma`: dalganın kıyıya vurduğu an, 3 sn tek seferlik darbe (`level.playSound`, kural 2): 30–80 Hz derin güm
  (0,6 sn'de söner), kırılan dalganın kısa çıtırtısı ve 0,4–4 kHz sıçrama patlaması (üstel sönüm), arkasında geri
  çekilen suyun hışırtısı. Tepeler yumuşak doyumla (tanh) bastırılır; hedef −23 LUFS, gerçek tepe ≤ −6 dBTP.

Üretim, ölçüm ve Ogg yazımı `ses_uret.py`'nin yardımcılarıyla (aynı deterministik ayarlar). Ölçüm metni ve spektrogram
`docs/kanit/ses/` altına (GOREV §6 #11); kopyası `KOK/.vibe/kanit/tsunami/ses/`.

Kullanım (worktree kökünden): ../../.venv/bin/python tools/assets/tsunami_ses.py
Çıktı: src/main/resources/assets/dogalafetler/sounds/tsunami_{ugultu,carpma}.ogg (mono, 48 kHz, Vorbis q5).
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


def ugultu() -> np.ndarray:
    uretec = np.random.default_rng(20261108)
    sure = 12.0
    t = np.arange(int(sure * su.ORNEKLEME)) / su.ORNEKLEME
    gurleme = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=35.0, ust=400.0), 150.0)
    # Dalga kütlesinin yuvarlanması: 0,5 Hz (6 tur) ve 0,25 Hz (3 tur) nefes; döngü boyunca tam sayı tur.
    yuvarlanma = 0.72 + 0.18 * np.sin(2.0 * np.pi * 0.5 * t) + 0.10 * np.sin(2.0 * np.pi * 0.25 * t + 1.1)
    # Jet/tren gibi kaba ton: 62–78 Hz arasında yavaşça kayan temel + ikinci harmonik, gürlemeden ~8 dB alçak.
    frekans = 70.0 + 8.0 * np.sin(2.0 * np.pi * t / sure)
    faz = su.tam_turlu_faz(frekans)
    ton = np.sin(faz) + su.db(-9) * np.sin(2.0 * faz + 0.6)
    ton *= np.sqrt(np.mean(gurleme**2)) * su.db(-8) / np.sqrt(np.mean(ton**2))
    hisirti = su.pembe_gurultu(t.size, uretec, alt=2000.0, ust=5000.0) * np.sqrt(np.mean(gurleme**2)) * su.db(-34)
    sinyal = su.konusma_centigi((gurleme + ton) * yuvarlanma + hisirti * yuvarlanma)
    return su.sifir_gecisine_dondur(sinyal)


def carpma() -> np.ndarray:
    uretec = np.random.default_rng(20261109)
    sure = 3.0
    n = int(sure * su.ORNEKLEME)
    t = np.arange(n) / su.ORNEKLEME
    # Derin güm: 55 → 32 Hz kayan süpürme, 8 ms'de yükselir, 0,6 sn'de söner.
    frekans = 32.0 + 23.0 * np.exp(-t / 0.25)
    gum = np.sin(2.0 * np.pi * np.cumsum(frekans) / su.ORNEKLEME) * su.yumusak(t / 0.008) * np.exp(-t / 0.6)
    gum += su.alcak_geciren(uretec.standard_normal(n), 90.0) * su.db(-6) * np.exp(-t / 0.35) * su.yumusak(t / 0.01)
    # Kırılan dalga ve sıçrama: 0,4–4 kHz geniş bant, hızlı atış, 0,35 sn üstel sönüm; kısa çıtırtı darbeleri.
    gurultu = uretec.standard_normal(n)
    sicrama = su.alcak_geciren(gurultu, 4000.0) - su.alcak_geciren(gurultu, 400.0)
    sicrama *= su.yumusak(t / 0.02) * np.exp(-t / 0.35)
    for an in np.sort(uretec.uniform(0.02, 0.5, 18)):
        bas = int(an * su.ORNEKLEME)
        boy = int(0.006 * su.ORNEKLEME)
        sicrama[bas:bas + boy] += uretec.standard_normal(boy) * np.hanning(boy) * 0.8 * np.exp(-an / 0.3)
    sicrama = su.alcak_geciren(sicrama, 3500.0)
    # Geri çekilen suyun hışırtısı: 200–1500 Hz, 0,4 sn'de girer, sona doğru söner.
    hisirti = su.alcak_geciren(gurultu[::-1].copy(), 1500.0) - su.alcak_geciren(gurultu[::-1].copy(), 200.0)
    hisirti *= su.yumusak((t - 0.3) / 0.5) * su.yumusak((sure - t) / 1.6)
    rms = np.sqrt(np.mean(gum**2))
    sicrama *= rms * su.db(-4) / max(np.sqrt(np.mean(sicrama**2)), 1e-12)
    hisirti *= rms * su.db(-14) / max(np.sqrt(np.mean(hisirti**2)), 1e-12)
    sinyal = su.alcak_geciren(su.konusma_centigi(gum + sicrama + hisirti), 5000.0) * su.yumusak((sure - t) / 0.05)
    # Gümün tepe/RMS oranı yüksek: yumuşak doyum tepeyi indirir, ses yüksekliği (LUFS) korunur.
    sinyal = sinyal / np.max(np.abs(sinyal))
    return np.tanh(3.0 * sinyal) / np.tanh(3.0)


SESLER = {
    # Ortam katmanı: −26 LUFS, tepe ≤ −6 dBTP; enerji 40–150 Hz'de (tasarım notu "50–150 Hz").
    "tsunami_ugultu": su.SesTanimi(ugultu, -26.0, -6.0, (35.0, 150.0), 2500.0, -30.0),
    # Tek seferlik darbe: baskin güm alt basta, sıçrama tizi 6 kHz üstünde −20 dB altında.
    "tsunami_carpma": su.SesTanimi(carpma, -23.0, -6.0, (25.0, 120.0), 6000.0, -20.0),
}


def main() -> int:
    su.SES_KLASORU.mkdir(parents=True, exist_ok=True)
    su.KANIT_KLASORU.mkdir(parents=True, exist_ok=True)
    hatalar = []
    with tempfile.TemporaryDirectory() as gecici:
        for ad, tanim in SESLER.items():
            hatalar += su.uret(ad, tanim, Path(gecici))
    kopya = KOK / ".vibe/kanit/tsunami/ses"
    kopya.mkdir(parents=True, exist_ok=True)
    for ad in SESLER:
        for dosya in su.KANIT_KLASORU.glob(f"{ad}_*"):
            shutil.copy2(dosya, kopya / dosya.name)
    if hatalar:
        raise SystemExit("hedef dışında:\n" + "\n".join(hatalar))
    return 0


if __name__ == "__main__":
    sys.exit(main())
