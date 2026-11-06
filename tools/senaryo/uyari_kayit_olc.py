#!/usr/bin/env python3
"""İstemci koşusunun ses kaydını (OpenAL Soft wave yazıcısı, `uyari_istemci.py`) kaynak dosyalarla karşılaştırarak ölçer.

Yöntem: kayıt ve kaynak aynı banda süzülür (siren 250–2000 Hz, ortam 25–150 Hz); 0,5 sn'lik her kayıt penceresi için
döngülü kaynakta en iyi dairesel gecikme bulunur ve en küçük kareler kazancı `g = <x, s> / <s, s>` hesaplanır;
`r` normalize korelasyondur. Kaynakta zaten olan dalgalanma (siren wail'i, ortam katmanının 0,25 Hz nefesi) böylece
ölçümden düşer: `g` doğrudan ses örneğinin düzeyidir (× OpenAL panlama kazancı, koşu boyunca sabit). Yalnız r ≥ 0,99
pencereler kullanılır (perdesi 1 olmayan açılış/fade pencereleri ve 5 sn'lik yarım periyotla karışan gecikmeler düşer).

Ölçülenler:
- siren kazanç düzeyleri (ör. "Sirenler" %28 ve %40) ve farkları (beklenen 20·log10(40/28) = +3,10 dB);
- ortam katmanının siren yokken ve siren çalarken düzeyi (beklenen −3 dB bastırma);
- dosyanın baştan çalındığı anlar (gecikme sıfıra döner: ses motoru sireni kesti, siren yeniden çalındı);
- ses içindeki sessizlik boşlukları (boyut değişimi yükleme ekranı vb.) ve her boşluğun 4 sn öncesi / 6 sn sonrasındaki
  eşleşik siren düzeyi (boyut değişiminden sonra siren aynı düzeyle sürüyor mu, K0-27);
- sirenin son düşüşü (200–1000 Hz bant zarfı, 100 ms): süre, toplam düşüş, eğim.

Kullanım: .venv/bin/python tools/senaryo/uyari_kayit_olc.py <kayıt.wav> <çıktı klasörü>
Çıktılar: <klasör>/ses-kaydi-olcum.txt, ses-kaydi-zarf.png, ses-kaydi-kazanc.tsv
"""
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ORNEKLEME = 48_000
KOK = Path(__file__).resolve().parents[2]
SES = KOK / "src/main/resources/assets/dogalafetler/sounds"
PENCERE_SN = 0.5
ADIM_SN = 0.25
R_ESIK = 0.99
SESSIZ_DBFS = -95.0


def coz(yol, ek=()):
    sonuc = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", *ek, "-i", str(yol), "-f", "f32le", "-ac", "1",
                            "-ar", str(ORNEKLEME), "-"], capture_output=True)
    if sonuc.returncode != 0:
        raise SystemExit(f"ffmpeg çözemedi {yol}: {sonuc.stderr.decode(errors='replace')}")
    return np.frombuffer(sonuc.stdout, dtype="<f4").astype(np.float64)


def bant(sinyal, alt, ust):
    frekans = np.fft.rfftfreq(sinyal.size, 1.0 / ORNEKLEME)
    spektrum = np.fft.rfft(sinyal)
    spektrum[(frekans < alt) | (frekans > ust)] = 0
    return np.fft.irfft(spektrum, sinyal.size)


def eslesik_kazanc(kayit, kaynak, alt, ust):
    """Her pencere için (zaman, g, r, gecikme sn); kaynak döngülü kabul edilir."""
    s = bant(kaynak, alt, ust)
    x = bant(kayit, alt, ust)
    boy, n, h = s.size, int(PENCERE_SN * ORNEKLEME), int(ADIM_SN * ORNEKLEME)
    kare = np.concatenate([s, s[:n]]) ** 2
    toplam = np.concatenate([[0.0], np.cumsum(kare)])
    enerji = toplam[n:n + boy] - toplam[:boy]
    kaynak_fft = np.fft.rfft(s)
    satirlar = []
    for bas in range(0, x.size - n, h):
        parca = x[bas:bas + n]
        e = float(np.dot(parca, parca))
        zaman = bas / ORNEKLEME + PENCERE_SN / 2
        if e < 1e-12:
            satirlar.append((zaman, 0.0, 0.0, 0.0))
            continue
        dolgu = np.zeros(boy)
        dolgu[:n] = parca
        korelasyon = np.fft.irfft(np.conj(np.fft.rfft(dolgu)) * kaynak_fft, boy)
        k = int(np.argmax(korelasyon))
        satirlar.append((zaman, korelasyon[k] / enerji[k], korelasyon[k] / np.sqrt(enerji[k] * e), k / ORNEKLEME))
    return np.array(satirlar)


def db(deger):
    return 20.0 * np.log10(np.maximum(deger, 1e-12))


def duzeyler(zaman, g_db, tolerans=0.6, en_az=3):
    """Geçerli pencereleri zaman sırasıyla kümeler (ardışık, medyandan ≤ tolerans), sonra aynı düzeydeki kümeleri
    birleştirir. [(medyan dB, pencere sayısı, [(baş, son)])], en az `en_az` pencereli olanlar."""
    kumeler = []
    for t, g in zip(zaman, g_db):
        if kumeler and abs(g - np.median(kumeler[-1][1])) <= tolerans and t - kumeler[-1][0][-1] <= 1.0:
            kumeler[-1][0].append(t)
            kumeler[-1][1].append(g)
        else:
            kumeler.append(([t], [g]))
    birlesik = []
    for zamanlar, degerler in kumeler:
        for duzey in birlesik:
            if abs(np.median(duzey[1]) - np.median(degerler)) <= tolerans:
                duzey[1].extend(degerler)
                duzey[2].append((zamanlar[0], zamanlar[-1]))
                break
        else:
            birlesik.append([None, list(degerler), [(zamanlar[0], zamanlar[-1])]])
    return [(float(np.median(d[1])), len(d[1]), d[2]) for d in birlesik if len(d[1]) >= en_az]


def bant_zarfi(sinyal, alt, ust, pencere=4800, adim=2400):
    hann = np.hanning(pencere)
    olcek = np.sum(hann ** 2) * pencere / 2.0
    frekans = np.fft.rfftfreq(pencere, 1.0 / ORNEKLEME)
    maske = (frekans >= alt) & (frekans < ust)
    adet = max(0, (sinyal.size - pencere) // adim + 1)
    zaman = (np.arange(adet) * adim + pencere / 2) / ORNEKLEME
    duzey = np.empty(adet)
    for i in range(adet):
        guc = np.abs(np.fft.rfft(sinyal[i * adim:i * adim + pencere] * hann)) ** 2 / olcek
        duzey[i] = 10 * np.log10(max(guc[maske].sum(), 1e-14))
    return zaman, duzey


def bosluklar(zaman, sesli, en_kisa=0.2):
    """İlk ve son sesli kare arasında sessiz bölgeler."""
    indis = np.nonzero(sesli)[0]
    if indis.size == 0:
        return []
    sonuc, bas = [], None
    for i in range(indis[0], indis[-1] + 1):
        if not sesli[i] and bas is None:
            bas = i
        elif sesli[i] and bas is not None:
            if zaman[i] - zaman[bas] >= en_kisa:
                sonuc.append((float(zaman[bas]), float(zaman[i])))
            bas = None
    return sonuc


def kosular(maske, zaman, birlestir_sn=0.5):
    """Doğru olan ardışık kare dizileri (kısa boşluklar birleştirilir): [(baş indis, son indis)]."""
    sonuc = []
    for i in np.nonzero(maske)[0]:
        if sonuc and zaman[i] - zaman[sonuc[-1][1]] <= birlestir_sn:
            sonuc[-1][1] = i
        else:
            sonuc.append([i, i])
    return [(a, b) for a, b in sonuc]


def son_dusus(zaman, siren_db, bitis, tavan, taban):
    """`bitis`'ten geriye: siren bandı `tavan − 6`'nın altına ilk indiği andan `bitis`'e kadar doğrusal eğim (dB/sn)."""
    i_bitis = int(np.searchsorted(zaman, bitis))
    i = i_bitis - 1
    while i > 0 and siren_db[i] < tavan - 6.0:
        i -= 1
    secim = slice(i, i_bitis)
    gecerli = siren_db[secim] > taban + 6.0
    if gecerli.sum() < 5:
        return None
    t, d = zaman[secim][gecerli], siren_db[secim][gecerli]
    egim = float(np.polyfit(t, d, 1)[0])
    return float(zaman[i]), float(bitis), egim, float(tavan - d[-1])


def ciz(yol, zaman, siren_db, ortam_db, kazanc_s, kazanc_o):
    gen, yuk, sol, sag, ust, alt = 1600, 640, 70, 20, 40, 50
    resim = Image.new("RGB", (gen, yuk), "white")
    cizim = ImageDraw.Draw(resim)
    t_max = float(zaman[-1]) if zaman.size else 1.0
    db_min, db_max = -100.0, 0.0

    def xy(t, v):
        return (sol + (gen - sol - sag) * t / t_max,
                ust + (yuk - ust - alt) * (db_max - max(db_min, min(db_max, v))) / (db_max - db_min))

    for d in range(int(db_min), 1, 10):
        _, y = xy(0, d)
        cizim.line([(sol, y), (gen - sag, y)], fill=(225, 225, 225))
        cizim.text((8, y - 6), f"{d} dB", fill="black")
    for t in range(0, int(t_max) + 1, 5):
        x, _ = xy(t, db_min)
        cizim.line([(x, ust), (x, yuk - alt)], fill=(238, 238, 238))
        cizim.text((x - 8, yuk - alt + 6), f"{t}s", fill="black")
    cizim.line([xy(t, v) for t, v in zip(zaman, ortam_db)], fill=(120, 190, 120), width=1)
    cizim.line([xy(t, v) for t, v in zip(zaman, siren_db)], fill=(230, 140, 140), width=1)
    for satirlar, renk in ((kazanc_s, (180, 0, 0)), (kazanc_o, (0, 110, 0))):
        for t, g, r, _ in satirlar:
            if r >= R_ESIK:
                x, y = xy(t, db(g))
                cizim.ellipse([x - 3, y - 3, x + 3, y + 3], fill=renk)
    cizim.text((sol + 10, 8), "açık kırmızı: siren bandı 200-1000 Hz, açık yeşil: ortam bandı 30-110 Hz (dBFS, 100 ms); "
                              "koyu noktalar: kaynağa eşleşik kazanç g (dB, r >= 0,99) — kırmızı siren, yeşil ortam",
               fill="black")
    resim.save(yol)


def main():
    kayit_yolu, klasor = Path(sys.argv[1]), Path(sys.argv[2])
    klasor.mkdir(parents=True, exist_ok=True)
    kayit = coz(kayit_yolu, ("-ignore_length", "1") if kayit_yolu.suffix.lower() == ".wav" else ())
    siren_k = eslesik_kazanc(kayit, coz(SES / "siren_standart.ogg"), 250.0, 2000.0)
    ortam_k = eslesik_kazanc(kayit, coz(SES / "ortam_test.ogg"), 25.0, 150.0)
    zaman, siren_db = bant_zarfi(kayit, 200.0, 1000.0)
    _, ortam_db = bant_zarfi(kayit, 30.0, 110.0)
    # OpenAL Soft 16 bit çıkışa dither ekler: sessizlik tam bantta ~−97, siren bandında ~−111, ortam bandında ~−121 dBFS.
    sesli = (siren_db > SESSIZ_DBFS) | (ortam_db > SESSIZ_DBFS)

    satirlar = [f"Kayıt: {kayit_yolu} ({kayit.size / ORNEKLEME:.1f} sn, {ORNEKLEME} Hz, L+R ortalaması)",
                f"Eşleşik kazanç: 0,5 sn pencere, 0,25 sn adım; geçerli pencere r >= {R_ESIK}", ""]

    gecerli_s = siren_k[siren_k[:, 2] >= R_ESIK]
    satirlar.append(f"Siren (siren_standart.ogg, 250–2000 Hz): {len(gecerli_s)} geçerli pencere")
    siren_duzeyleri = duzeyler(gecerli_s[:, 0], db(gecerli_s[:, 1]))
    for g, adet, araliklar in siren_duzeyleri:
        satirlar.append(f"  düzey {g:6.2f} dB ({adet} pencere): "
                        + ", ".join(f"{a:.2f}–{b:.2f}" for a, b in araliklar) + " sn")
    if len(siren_duzeyleri) >= 2:
        ilk = min(siren_duzeyleri, key=lambda d: d[2][0][0])
        son = max(siren_duzeyleri, key=lambda d: d[2][-1][1])
        satirlar.append(f"  son düzey − ilk düzey: {son[0] - ilk[0]:+.2f} dB "
                        f"(\"Sirenler\" 28 → 40 için beklenen 20·log10(40/28) = +3.10 dB)")

    yeniden = []
    for onceki, simdiki in zip(siren_k[:-1], siren_k[1:]):
        if simdiki[2] >= R_ESIK and simdiki[3] < 0.4 and onceki[3] > 0.6 and onceki[2] > 0.3:
            yeniden.append(simdiki[0])
    satirlar.append("  dosyanın baştan çaldığı pencereler (gecikme ~0'a döndü, r >= 0,99): "
                    + (", ".join(f"{t:.2f} sn" for t in yeniden) or "yok"))
    satirlar.append("")

    siren_yok = np.interp(zaman, siren_k[:, 0], siren_k[:, 2]) < 0.5
    sizinti = float(np.median(siren_db[sesli & siren_yok])) if (sesli & siren_yok).any() else SESSIZ_DBFS
    siren_esigi = sizinti + 15.0
    siren_aktif_k = np.interp(ortam_k[:, 0], zaman, siren_db) > siren_esigi
    gecerli_o = ortam_k[:, 2] >= R_ESIK
    satirlar.append(f"Ortam katmanı (ortam_test.ogg, 25–150 Hz): {int(gecerli_o.sum())} geçerli pencere; siren bandında "
                    f"siren yokken sızıntı {sizinti:.1f} dBFS, siren çalıyor = bant > {siren_esigi:.1f} dBFS")
    ozet = {}
    for ad, maske in (("siren yokken", gecerli_o & ~siren_aktif_k), ("siren çalarken", gecerli_o & siren_aktif_k)):
        dz = duzeyler(ortam_k[maske, 0], db(ortam_k[maske, 1]))
        for g, adet, araliklar in dz:
            satirlar.append(f"  {ad}: düzey {g:6.2f} dB ({adet} pencere): "
                            + ", ".join(f"{a:.2f}–{b:.2f}" for a, b in araliklar) + " sn")
        if dz:
            ozet[ad] = max(dz, key=lambda d: d[1])[0]
    if len(ozet) == 2:
        satirlar.append(f"  bastırma (en kalabalık düzeyler): {ozet['siren çalarken'] - ozet['siren yokken']:+.2f} dB "
                        f"(beklenen −3.0 dB; diğer düzeyler açılış eğrisi, bastırmanın geri dönüşü ya da kapanış fade'idir)")
    satirlar.append("")

    bos = bosluklar(zaman, sesli)
    satirlar.append(f"Ses içindeki sessiz boşluklar (bantlar < {SESSIZ_DBFS:.0f} dBFS, ≥ 0,2 sn): "
                    + (", ".join(f"{a:.2f}–{b:.2f} sn ({b - a:.2f} sn)" for a, b in bos) or "yok"))
    for a, b in bos:
        once = gecerli_s[(gecerli_s[:, 0] >= a - 4.0) & (gecerli_s[:, 0] < a)]
        sonra = gecerli_s[(gecerli_s[:, 0] > b) & (gecerli_s[:, 0] <= b + 6.0)]
        if len(once) and len(sonra):
            d_once, d_sonra = float(np.median(db(once[:, 1]))), float(np.median(db(sonra[:, 1])))
            satirlar.append(f"  boşluk {a:.2f}–{b:.2f} sn: siren öncesi {d_once:.2f} dB ({len(once)} pencere), sonrası "
                            f"{d_sonra:.2f} dB ({len(sonra)} pencere, {sonra[0, 0]:.2f}–{sonra[-1, 0]:.2f} sn); "
                            f"fark {d_sonra - d_once:+.2f} dB")
        else:
            satirlar.append(f"  boşluk {a:.2f}–{b:.2f} sn: öncesinde {len(once)}, sonrasında {len(sonra)} geçerli siren "
                            f"penceresi (karşılaştırılamadı)")

    tavan_s = float(np.percentile(siren_db[siren_db > siren_esigi], 90)) if (siren_db > siren_esigi).any() else 0.0
    for bas, son in kosular(siren_db > siren_esigi, zaman):
        if zaman[son] - zaman[bas] < 1.0:
            continue
        j = son + 1
        while j < siren_db.size and sesli[j] and siren_db[j] > sizinti + 3.0:
            j += 1
        j = min(j, siren_db.size - 1)
        sessizlige = not sesli[j]
        sonuc = son_dusus(zaman, siren_db, float(zaman[j]), tavan_s, SESSIZ_DBFS if sessizlige else sizinti)
        aciklama = f"Siren koşusu {zaman[bas]:.2f}–{zaman[j]:.2f} sn: "
        if sonuc is None or sonuc[1] - sonuc[0] < 0.4:
            satirlar.append(aciklama + f"{'sessizliğe' if sessizlige else 'sızıntıya'} ani kesildi (fade değil)")
            continue
        fade_bas, fade_son, egim, dusus = sonuc
        satirlar.append(aciklama + f"son düşüş {fade_bas:.2f}–{fade_son:.2f} sn ({fade_son - fade_bas:.2f} sn, "
                        f"{'sessizliğe' if sessizlige else 'ortam sızıntısına'}): eğim {egim:.1f} dB/sn, görünen düşüş "
                        f"{dusus:.0f} dB (siren bandı 90. yüzdelik {tavan_s:.1f} dBFS'ten)")

    (klasor / "ses-kaydi-olcum.txt").write_text("\n".join(satirlar) + "\n", encoding="utf-8")
    np.savetxt(klasor / "ses-kaydi-kazanc.tsv",
               np.column_stack([siren_k[:, 0], db(siren_k[:, 1]), siren_k[:, 2], siren_k[:, 3], db(ortam_k[:, 1]),
                                ortam_k[:, 2], ortam_k[:, 3]]),
               fmt="%.3f", delimiter="	", comments="",
               header="zaman_sn	siren_g_dB	siren_r	siren_gecikme_sn	ortam_g_dB	ortam_r	ortam_gecikme_sn")
    ciz(klasor / "ses-kaydi-zarf.png", zaman, siren_db, ortam_db, siren_k, ortam_k)
    print("\n".join(satirlar))


if __name__ == "__main__":
    main()
