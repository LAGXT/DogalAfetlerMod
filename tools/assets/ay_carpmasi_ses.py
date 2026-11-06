#!/usr/bin/env python3
"""Ay çarpmasının sesleri (bölüm H, docs/tasarim/ay_carpmasi.md §4, madde 5 ses düzeyi, N19).

- ay_carpmasi_yaklasma (12 sn döngü, global ortam katmanı, sirenin 40. sn'sinden): çok alçak, yükselip alçalan uğultu;
  22,5 / 30 / 45 Hz kısmi tonlar + 90 Hz harmonik, altında 18–120 Hz pembe gürleme, 0,25 Hz nefes. Hissedilen ama
  konuşmayı bastırmayan (konuşma bandına −6 dB çentik).
- ay_carpmasi_citirti (6 sn döngü, sirenin son 5 sn'si): ayın parçalanması, uzak buz kırılması benzeri seyrek çatırtılar
  (300–1200 Hz kısa patlamalar + alçak gıcırtı), döngü dikişsiz (dairesel yerleştirme).
- ay_carpmasi_carpma (tek seferlik, 12 sn; parlamadan 1,5 sn sonra her oyuncuya): derin güm (60 → 28 Hz inen, 3 sn üstel
  sönüm) ve 10 sn süren, iki kez kabaran gerilen gürleme (20–150 Hz) ile düşük düzeyde çakıl.

Üretim, ölçüm ve Ogg yazımı `ses_uret.py`'nin yardımcılarıyla yapılır (aynı deterministik ayarlar); ölçüm metni ve
spektrogram `docs/kanit/ses/` altına, kopyası `KÖK/.vibe/kanit/ay-carpmasi/ses/` altına yazılır.

Kullanım (worktree kökünden):
    ../../.venv/bin/python tools/assets/ay_carpmasi_ses.py

Çıktı: src/main/resources/assets/dogalafetler/sounds/ay_carpmasi_{yaklasma,citirti,carpma}.ogg (mono, 48 kHz, Vorbis q5).
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


def yaklasma() -> np.ndarray:
    uretec = np.random.default_rng(20261111)
    sure = 12.0
    t = np.arange(int(sure * su.ORNEKLEME)) / su.ORNEKLEME
    nefes = 0.72 + 0.28 * np.sin(2.0 * np.pi * 0.25 * t)
    ton = np.zeros_like(t)
    for frekans, duzey, faz in ((22.5, 0.0, 0.0), (30.0, -2.0, 0.9), (45.0, -3.0, 1.7), (90.0, -7.0, 0.4)):
        ton += su.db(duzey) * np.sin(su.tam_turlu_faz(np.full_like(t, frekans)) + faz)
    ton *= nefes
    gurleme = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=18.0, ust=200.0), 120.0)
    gurleme *= np.sqrt(np.mean(ton**2)) * su.db(-3) / np.sqrt(np.mean(gurleme**2))
    return su.sifir_gecisine_dondur(su.konusma_centigi(ton + gurleme))


def citirti() -> np.ndarray:
    uretec = np.random.default_rng(20261112)
    sure = 6.0
    n = int(sure * su.ORNEKLEME)
    t = np.arange(n) / su.ORNEKLEME
    sinyal = np.zeros(n)
    for an in uretec.uniform(0.0, sure, 140):
        bas = int(an * su.ORNEKLEME)
        uzunluk = int(uretec.uniform(0.004, 0.03) * su.ORNEKLEME)
        parca = uretec.standard_normal(uzunluk) * np.exp(-np.arange(uzunluk) / (uzunluk / 4.0))
        parca *= uretec.uniform(0.3, 1.0)
        indis = (bas + np.arange(uzunluk)) % n
        sinyal[indis] += parca
    # Dört kez 1,2 kHz alçak geçiren (−48 dB/oktav): çatırtı tizleşip konuşmayı delmesin.
    citir = sinyal - su.alcak_geciren(sinyal, 300.0)
    for _ in range(4):
        citir = su.alcak_geciren(citir, 1200.0)
    # Gıcırtı: 70–140 Hz arasında kayan, düzensiz zarflı alçak ınıltı (kabuğun gerilmesi).
    perde = 70.0 + 70.0 * su.parcali_egri(t, [(0.0, 0.0), (2.2, 1.0), (3.4, 0.4), (6.0, 0.0)], sure)
    zarf = 0.5 + 0.5 * su.parcali_egri(t, [(0.0, 0.2), (1.5, 1.0), (3.0, 0.3), (4.5, 0.9), (6.0, 0.2)], sure)
    gicirti = np.sin(su.tam_turlu_faz(perde)) * zarf
    gicirti *= np.sqrt(np.mean(citir**2)) * su.db(2) / np.sqrt(np.mean(gicirti**2))
    return su.sifir_gecisine_dondur(su.konusma_centigi(citir + gicirti))


def carpma() -> np.ndarray:
    uretec = np.random.default_rng(20261113)
    sure = 12.0
    t = np.arange(int(sure * su.ORNEKLEME)) / su.ORNEKLEME
    # Güm: 60 → 28 Hz inen, üstel sönen; ilk 15 ms yumuşak giriş (tıkırtı olmasın).
    perde = 28.0 + 32.0 * np.exp(-t / 0.6)
    faz = 2.0 * np.pi * np.cumsum(perde) / su.ORNEKLEME
    gum = np.sin(faz) * np.exp(-t / 3.0) * su.yumusak(t / 0.015)
    # Gürleme: 10 sn, iki kez kabarır (gerilen hava şoku), sonda söner.
    gurleme = su.alcak_geciren(su.pembe_gurultu(t.size, uretec, alt=20.0, ust=260.0), 150.0)
    kabarma = su.yumusak(t / 0.4) * (0.55 + 0.25 * np.sin(2 * np.pi * t / 4.5 - 1.2) ** 2) \
        * np.exp(-np.maximum(t - 6.0, 0.0) / 2.0) * su.yumusak((sure - t) / 0.8)
    gurleme *= kabarma
    gurleme *= np.sqrt(np.mean(gum**2)) * su.db(-2) / np.sqrt(np.mean(gurleme**2))
    # Çakıl: seyrek kısa darbeler, tabandan ~28 dB aşağıda.
    cakil = np.zeros_like(t)
    for an in np.sort(uretec.uniform(1.0, 9.0, 40)):
        bas = int(an * su.ORNEKLEME)
        uzunluk = int(0.01 * su.ORNEKLEME)
        parca = uretec.standard_normal(uzunluk) * np.hanning(uzunluk)
        cakil[bas:bas + uzunluk] += parca[: max(0, min(uzunluk, t.size - bas))]
    cakil = su.alcak_geciren(cakil, 1500.0) - su.alcak_geciren(cakil, 400.0)
    taban = gum + gurleme
    cakil *= np.sqrt(np.mean(taban**2)) * su.db(-28) / max(np.sqrt(np.mean(cakil**2)), 1e-12)
    return su.konusma_centigi(taban + cakil)


SESLER = {
    # Döngülü ortam: −26 LUFS hedefinin altı (K ağırlıklı ölçüm alt basta kör: tepe ≤ −6 dBTP kalır), enerji alt basta.
    "ay_carpmasi_yaklasma": su.SesTanimi(yaklasma, -30.0, -6.0, (18.0, 100.0), 2500.0, -30.0),
    "ay_carpmasi_citirti": su.SesTanimi(citirti, -30.0, -6.0, (60.0, 1500.0), 2500.0, -24.0),
    # Tek seferlik darbe: tepe ≤ −6 dBTP (uyarı-ses §A4 kural 2).
    "ay_carpmasi_carpma": su.SesTanimi(carpma, -26.0, -6.0, (18.0, 100.0), 2500.0, -30.0),
}


def main() -> int:
    su.SES_KLASORU.mkdir(parents=True, exist_ok=True)
    su.KANIT_KLASORU.mkdir(parents=True, exist_ok=True)
    hatalar: list[str] = []
    with tempfile.TemporaryDirectory() as gecici:
        for ad, tanim in SESLER.items():
            hatalar += su.uret(ad, tanim, Path(gecici))
    kopya = KOK / ".vibe/kanit/ay-carpmasi/ses"
    kopya.mkdir(parents=True, exist_ok=True)
    for ad in SESLER:
        for dosya in su.KANIT_KLASORU.glob(f"{ad}_*"):
            shutil.copy2(dosya, kopya / dosya.name)
    if hatalar:
        raise SystemExit("hedef dışında:\n" + "\n".join(hatalar))
    return 0


if __name__ == "__main__":
    sys.exit(main())
