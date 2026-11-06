#!/usr/bin/env python3
"""İstemci koşusunun ses kaydını (OpenAL Soft wave yazıcısı, `uyari_istemci.py`) ölçer: oyunun gerçekten çaldığı
karışımda siren ve ortam katmanının düzeyi, sirenin fade-out eğimi, "Sirenler" kaydırıcısının etkisi ve siren
sırasında ortam bastırması.

Bantlar: siren 200–1000 Hz (280↔420 Hz wail + 2. harmonik; fade'de perde %20 iner), ortam 30–110 Hz (45/67,5 Hz uğultu).
100 ms pencere, 50 ms adım. Siren bölümleri eşikle (taban + 15 dB) bulunur; 3 sn'den kısa boşluklar (boyut değişimi,
/stopsound sonrası yeniden çalma) birleştirilir ve ayrıca raporlanır. Daha uzun boşluk (ses ayarları ekranı tek oyunculuda
oyunu ve sesleri duraklatır) yeni bölüm açar. Ortam bastırması bölümün ilk boşluğuna kadar ölçülür (sonra oyuncu
Nether'e geçebilir).

Kullanım: .venv/bin/python tools/senaryo/uyari_kayit_olc.py <kayıt.wav> <çıktı klasörü>
Çıktılar: <klasör>/ses-kaydi-olcum.txt, <klasör>/ses-kaydi-zarf.png
"""
import re
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ORNEKLEME = 48_000
PENCERE = 4800
ADIM = 2400
SIREN_BANDI = (200.0, 1000.0)
ORTAM_BANDI = (30.0, 110.0)


def coz(yol):
    sonuc = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-ignore_length", "1", "-i", str(yol), "-f", "f32le",
                            "-ac", "1", "-ar", str(ORNEKLEME), "-"], capture_output=True)
    if sonuc.returncode != 0:
        raise SystemExit(f"ffmpeg çözemedi: {sonuc.stderr.decode(errors='replace')}")
    return np.frombuffer(sonuc.stdout, dtype="<f4").astype(np.float64)


def bant_zarfi(sinyal):
    """(zaman sn, siren bandi dBFS, ortam bandi dBFS)."""
    pencere = np.hanning(PENCERE)
    olcek = np.sum(pencere ** 2) * PENCERE / 2.0
    frekans = np.fft.rfftfreq(PENCERE, 1.0 / ORNEKLEME)
    s_maske = (frekans >= SIREN_BANDI[0]) & (frekans < SIREN_BANDI[1])
    o_maske = (frekans >= ORTAM_BANDI[0]) & (frekans < ORTAM_BANDI[1])
    adet = max(0, (sinyal.size - PENCERE) // ADIM + 1)
    zaman = np.empty(adet)
    siren = np.empty(adet)
    ortam = np.empty(adet)
    for i in range(adet):
        parca = sinyal[i * ADIM:i * ADIM + PENCERE] * pencere
        guc = np.abs(np.fft.rfft(parca)) ** 2 / olcek
        zaman[i] = (i * ADIM + PENCERE / 2) / ORNEKLEME
        siren[i] = 10 * np.log10(max(guc[s_maske].sum(), 1e-14))
        ortam[i] = 10 * np.log10(max(guc[o_maske].sum(), 1e-14))
    return zaman, siren, ortam


def bolumler(zaman, siren, esik, bosluk_sn=3.0):
    """Eşiği aşan ardışık bölgeler; kısa boşluklar birleştirilir. [(baş, son, [boşluklar])] indeks."""
    aktif = siren > esik
    ham = []
    i = 0
    while i < aktif.size:
        if aktif[i]:
            j = i
            while j + 1 < aktif.size and aktif[j + 1]:
                j += 1
            ham.append([i, j])
            i = j + 1
        else:
            i += 1
    birlesik = []
    for bas, son in ham:
        if birlesik and zaman[bas] - zaman[birlesik[-1][1]] < bosluk_sn:
            birlesik[-1][2].append((zaman[birlesik[-1][1]], zaman[bas]))
            birlesik[-1][1] = son
        else:
            birlesik.append([bas, son, []])
    return [(b, s, g) for b, s, g in birlesik if zaman[s] - zaman[b] >= 1.0]


def fade_egimi(zaman, siren, son, kararli, taban):
    """Bölüm sonundaki düşüş: kararlı −6 dB'den −36 dB'e (ya da taban +6'ya) inen kısımda doğrusal eğim (dB/sn)."""
    k = son
    while k > 0 and siren[k] < kararli - 6.0:
        k -= 1
    bas = k
    bit = bas
    alt = max(kararli - 36.0, taban + 6.0)
    while bit + 1 < siren.size and siren[bit + 1] > alt and zaman[bit + 1] - zaman[bas] < 6.0:
        bit += 1
    if bit - bas < 4:
        return None, None, None
    egim, _ = np.polyfit(zaman[bas:bit + 1], siren[bas:bit + 1], 1)
    return float(egim), float(zaman[bas]), float(zaman[bit] - zaman[bas])


def lufs(sinyal, bas, son):
    with tempfile.NamedTemporaryFile(suffix=".wav") as gecici:
        parca = (np.clip(sinyal[int(bas * ORNEKLEME):int(son * ORNEKLEME)], -1, 1) * 32767).astype("<i2")
        import wave
        with wave.open(gecici.name, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(ORNEKLEME)
            w.writeframes(parca.tobytes())
        cikti = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", gecici.name, "-af", "ebur128=peak=true",
                                "-f", "null", "-"], capture_output=True, text=True).stderr
    i = re.search(r"I:\s+(-?[\d.]+|-inf) LUFS", cikti.split("Summary:")[-1])
    tepe = re.search(r"Peak:\s+(-?[\d.]+|-inf) dBFS", cikti.split("Summary:")[-1])
    return (i.group(1) if i else "?"), (tepe.group(1) if tepe else "?")


def ciz(yol, zaman, siren, ortam, isaretler):
    gen, yuk = 1600, 600
    sol, sag, ust, alt = 70, 20, 30, 50
    resim = Image.new("RGB", (gen, yuk), "white")
    d = ImageDraw.Draw(resim)
    t_max = float(zaman[-1]) if zaman.size else 1.0
    db_min, db_max = -90.0, 0.0

    def xy(t, v):
        x = sol + (gen - sol - sag) * t / t_max
        y = ust + (yuk - ust - alt) * (db_max - max(db_min, min(db_max, v))) / (db_max - db_min)
        return x, y

    for db_ in range(int(db_min), 1, 10):
        _, y = xy(0, db_)
        d.line([(sol, y), (gen - sag, y)], fill=(225, 225, 225))
        d.text((8, y - 6), f"{db_} dB", fill="black")
    for t in range(0, int(t_max) + 1, 10):
        x, _ = xy(t, db_min)
        d.line([(x, ust), (x, yuk - alt)], fill=(235, 235, 235))
        d.text((x - 8, yuk - alt + 6), f"{t}s", fill="black")
    for t, renk in isaretler:
        x, _ = xy(t, 0)
        d.line([(x, ust), (x, yuk - alt)], fill=renk)
    d.line([xy(t, v) for t, v in zip(zaman, ortam)], fill=(30, 120, 30), width=1)
    d.line([xy(t, v) for t, v in zip(zaman, siren)], fill=(200, 30, 30), width=1)
    d.text((sol + 10, 8), "kırmızı: siren bandı 200-1000 Hz, yeşil: ortam bandı 30-110 Hz (dBFS, 100 ms); "
                          "mavi: bölüm başı, turuncu: fade başı", fill="black")
    resim.save(yol)


def main():
    kayit = Path(sys.argv[1])
    klasor = Path(sys.argv[2])
    klasor.mkdir(parents=True, exist_ok=True)
    sinyal = coz(kayit)
    zaman, siren, ortam = bant_zarfi(sinyal)
    taban = float(np.percentile(siren, 5))
    satirlar = [f"Kayıt: {kayit} ({sinyal.size / ORNEKLEME:.1f} sn, {ORNEKLEME} Hz, mono karışım)",
                f"Siren bandı tabanı (5. yüzdelik): {taban:.1f} dBFS; eşik {taban + 15:.1f} dBFS", ""]
    isaretler = []
    sonuclar = []
    for n, (bas, son, bosluklar) in enumerate(bolumler(zaman, siren, taban + 15.0), 1):
        t0, t1 = float(zaman[bas]), float(zaman[son])
        govde = (zaman >= t0 + 1.5) & (zaman <= t1 - 2.5)
        kararli = float(np.median(siren[govde])) if govde.any() else float(np.max(siren[bas:son + 1]))
        egim, fade_bas, fade_sure = fade_egimi(zaman, siren, son, kararli, taban)
        isaretler.append((t0, (40, 60, 220)))
        if fade_bas is not None:
            isaretler.append((fade_bas, (240, 140, 0)))
        oncesi = (zaman >= t0 - 4.0) & (zaman < t0 - 0.5)
        ilk_bosluk = bosluklar[0][0] if bosluklar else t1 - 2.5
        bastirma_penceresi = (zaman >= t0 + 1.5) & (zaman <= min(ilk_bosluk, t1 - 2.5))
        ortam_once = float(np.median(ortam[oncesi])) if oncesi.any() else float("nan")
        ortam_sira = float(np.median(ortam[bastirma_penceresi])) if bastirma_penceresi.any() else float("nan")
        lufs_degeri, tepe = lufs(sinyal, t0 + 1.5, max(t0 + 2.0, t1 - 2.5)) if govde.any() else ("?", "?")
        sonuclar.append(kararli)
        satirlar.append(f"Bölüm {n}: {t0:.2f}–{t1:.2f} sn (süre {t1 - t0:.2f} sn); boşluklar: "
                        + (", ".join(f"{a:.2f}–{b:.2f} sn ({b - a:.2f} sn)" for a, b in bosluklar) or "yok"))
        satirlar.append(f"  kararlı siren bandı (medyan): {kararli:.1f} dBFS; gövde ebur128 I = {lufs_degeri} LUFS, "
                        f"tepe {tepe} dBFS")
        if egim is not None:
            tur = "ani kesilme (duraklatma ya da çıkış; fade değil)" if egim < -150.0 else "fade-out"
            satirlar.append(f"  sonu: {tur}, {fade_bas:.2f} sn'de başladı, eğim {egim:.1f} dB/sn "
                            f"(üstel sönüm düz çizgi; 40 tick'te −60 dB = −30 dB/sn), ölçülen aralık {fade_sure:.2f} sn")
        satirlar.append(f"  ortam bandı: siren öncesi {ortam_once:.1f} dBFS, siren sırasında (ilk boşluğa kadar) "
                        f"{ortam_sira:.1f} dBFS (fark {ortam_sira - ortam_once:+.1f} dB; bastırma hedefi −3 dB)")
    if len(sonuclar) >= 2:
        satirlar += ["", "Bölüm kararlı düzeyleri: " + ", ".join(f"{d:.1f}" for d in sonuclar) + " dBFS",
                     f"Son bölüm − ilk bölüm: {sonuclar[-1] - sonuclar[0]:+.1f} dB (son bölüm 'Sirenler' 28 → 40 "
                     f"sonrasıysa beklenen 20·log10(40/28) = +3.1 dB)"]
    (klasor / "ses-kaydi-olcum.txt").write_text("\n".join(satirlar) + "\n", encoding="utf-8")
    ciz(klasor / "ses-kaydi-zarf.png", zaman, siren, ortam, isaretler)
    np.savetxt(klasor / "ses-kaydi-zarf.tsv", np.column_stack([zaman, siren, ortam]), fmt="%.3f", delimiter="	",
               header="zaman_sn	siren_200_1000_dBFS	ortam_30_110_dBFS", comments="")
    print("\n".join(satirlar))


if __name__ == "__main__":
    main()
