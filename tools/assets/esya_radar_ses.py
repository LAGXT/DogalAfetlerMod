#!/usr/bin/env python3
"""Sismografın öncü sarsıntı sesi (SARTNAME madde 13b, madde 5 ses düzeyi, N19).

Tek seferlik 2,4 sn'lik kısık zemin gürlemesi: 25–110 Hz pembe gürültü, 3–7 Hz düzensiz titreşim zarfı, 0,35 sn'de
 yükselip yavaşça sönen genlik ve çok düşük düzeyde çakıl çıtırtısı; konuşma bandına −6 dB çentik (ses.md §4 kural 1).
Hedef −27 LUFS, gerçek tepe ≤ −6 dBTP (tek seferlik darbe kuralı, uyarı-ses §A4). Oyunda ayrıca 0,08–0,30 hacimle
yalnız sismografı taşıyan oyuncunun istemcisinde çalar.

Üretim, ölçüm ve Ogg yazımı `ses_uret.py`'nin yardımcılarıyla yapılır (aynı deterministik ayarlar). Ölçüm metni ve
spektrogram `docs/` yerine `KOK/.vibe/kanit/radar-sismograf/ses/` altına yazılır.

Kullanım (worktree kökünden):
    ../../.venv/bin/python tools/assets/esya_radar_ses.py

Çıktı: src/main/resources/assets/dogalafetler/sounds/esya_oncu_sarsinti.ogg (mono, 48 kHz, Vorbis q5).
"""

from __future__ import annotations

import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ses_uret as su  # noqa: E402

AD = "esya_oncu_sarsinti"
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W


def oncu_sarsinti() -> np.ndarray:
    uretec = np.random.default_rng(20261106)
    sure = 2.4
    t = np.arange(int(sure * su.ORNEKLEME)) / su.ORNEKLEME
    gurleme = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=25.0, ust=160.0), 110.0)
    # 3–7 Hz arasında kayan düzensiz titreşim (zemin 'kıpırdanır').
    titresim_frekansi = 3.0 + 4.0 * su.yumusak(t / sure)
    faz = 2.0 * np.pi * np.cumsum(titresim_frekansi) / su.ORNEKLEME
    titresim = 0.65 + 0.35 * np.sin(faz) * np.sin(2.0 * np.pi * 1.3 * t + 0.7)
    zarf = su.yumusak(t / 0.35) * np.exp(-np.maximum(t - 0.35, 0.0) / 0.9) * su.yumusak((sure - t) / 0.25)
    taban = gurleme * titresim * zarf
    # Çakıl çıtırtısı: seyrek kısa darbeler, 400–1500 Hz, tabandan ~30 dB aşağıda.
    citirti = np.zeros_like(t)
    for an in np.sort(uretec.uniform(0.2, 1.8, 14)):
        bas = int(an * su.ORNEKLEME)
        uzunluk = int(0.012 * su.ORNEKLEME)
        parca = uretec.standard_normal(uzunluk) * np.hanning(uzunluk)
        citirti[bas:bas + uzunluk] += parca[: max(0, min(uzunluk, t.size - bas))]
    citirti = su.alcak_geciren(citirti, 1500.0) - su.alcak_geciren(citirti, 400.0)
    citirti *= np.sqrt(np.mean(taban**2)) * su.db(-30) / max(np.sqrt(np.mean(citirti**2)), 1e-12)
    return su.konusma_centigi(taban + citirti * zarf)


TANIM = su.SesTanimi(oncu_sarsinti, -27.0, -6.0, (25.0, 120.0), 2500.0, -30.0)


def main() -> int:
    su.KANIT_KLASORU = KOK / ".vibe/kanit/radar-sismograf/ses"
    su.SES_KLASORU.mkdir(parents=True, exist_ok=True)
    su.KANIT_KLASORU.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as gecici:
        hatalar = su.uret(AD, TANIM, Path(gecici))
    if hatalar:
        raise SystemExit("hedef dışında:\n" + "\n".join(hatalar))
    return 0


if __name__ == "__main__":
    sys.exit(main())
