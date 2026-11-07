#!/usr/bin/env python3
"""Pulsar-2'nin TPS senaryosu (runServer, oyuncusuz, dedicated; K0-18), tekrar edilebilir.

Çalıştırma alt yapısı tools/senaryo/cekirdek_sunucu.py'den gelir; dünya `run-server/pulsar-senaryo` her koşuda sıfırdan
üretilir. Kalıp `meteor_sunucu.py --tps`: oyuncusuz sunucuda tek hedef doğuş noktasıdır; çevresi ±160 blok forceload (bir
oyuncunun 10 chunk'lık simülasyon alanı).

Adımlar: diğer afet kuralları kapalı, `afetBlokHasari true`, forceload, 20 sn bekleme ve boşta `forge tps`;
`dogalafet pulsar baslat` + `dogalafet asama ileri pulsar` (ısınma tamam, tam güç) ve `dogalafet durum` (evre pulsar);
`dogalafet meteor yagmur 200 12` (pulsar meteor yoğunluğu `gokyuzu.pulsarMeteorDakikaBasina` = 12/dk tek hedefe: pulsarın
kendi yağmuru oyuncu başınadır ve oyuncusuz sunucuda hedefsiz kalır); 200 sn boyunca ve krater işleri bitene dek 10 sn'de
bir `forge tps`; sonda `yangin bilgi`, `meteor durdur`, `yangin sondur`, `sifirla`. `<kanıt>/tps.txt`'ye MAKINE, örnekler,
ortalama tick ms, en düşük TPS, krater ve blok işlemi sayısı; özet satırı `PULSAR-TPS ...` hem log'a hem çıktıya.
Ölçüt (pulsar-2 kanıtı): en düşük TPS > 18 ve pulsar/yağmur örneklerinin ortalama tick'i < 10 ms. Log denetimi: `invalid
dist` yok, istisna yok, config düzeltme WARN'ı yok, ağ kimlik listesinde `gokyuzu/yutma` var, süreç 0 ile çıktı.
Bilinen sınır: kavurma, yangın ve lav oyuncu çevresinde çalışır; oyuncusuz sunucuda yükü gök, sıcaklık, kuyruk taraması
ve meteor/krater/meteor ateşidir. Oyunculu yük (entegre sunucu) `pulsar_gorsel.py` kare satırlarındaki TPS/mspt'dedir.

Kullanım (worktree kökünden): python3 tools/senaryo/pulsar_sunucu.py --kanit <dizin> [--port <n>] [--olcum]
`--olcum`: yalnız ana makinede koşar (kuyrukta `UZAK=0`, ana makinenin işi; K0-33, K-E29 b); verilmezse kuyruk işi
hangi makineye düşerse orada (pc2'de `uzak.sh`). Test defteri ve kuyruk `cekirdek_sunucu.argumanlar` ile (K-E51, K-E60).
Çıkış: 0 geçti, 1 senaryo/denetim hatası (TPS ya da tick ölçütü tutmadı), 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır
süreç kapısı açılmadı (port ve kapı: cekirdek_sunucu.py, K-E13).
"""
import os
import re
import signal
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402
import runclient_kilit  # noqa: E402

cs.SENARYO_DUNYASI = "pulsar-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

TPS_SURESI = 200
METEOR_DAKIKA_BASINA = 12
TPS_ARALIGI = 10
TPS_ESIGI = 18.0
TICK_ESIGI_MS = 10.0
KAPALI_AFETLER = ("kasirgaAfeti", "tsunamiAfeti", "depremAfeti", "asitYagmuruAfeti", "sinkholeAfeti", "ormanYanginiAfeti",
                  "ayCarpmasiAfeti")


def tps_ornegi(sunucu, etiket, ornekler):
    # Sunucu makinenin yerel ayarıyla yazar: tr_TR'de ondalık virgül ("3,754 ms").
    satir = sunucu.komut("forge tps", r"Overall: Mean tick time: [\d.,]+ ms\. Mean TPS: [\d.,]+")
    eslesme = re.search(r"Mean tick time: ([\d.,]+) ms\. Mean TPS: ([\d.,]+)", satir)
    ornekler.append((etiket, float(eslesme.group(1).replace(",", ".")), float(eslesme.group(2).replace(",", "."))))


def tps_kosusu(sunucu, klasor):
    """Pulsar tam güç + blok hasarı + 12/dk meteorun sunucu yükü (tek hedef, 200 sn)."""
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    for kural in KAPALI_AFETLER:
        sunucu.komut(f"gamerule {kural} false", rf"{kural} is now set to: false")
    sunucu.komut("gamerule afetBlokHasari true", r"afetBlokHasari is now set to: true")
    for alan in ("~-160 ~-160 ~-1 ~-1", "~0 ~-160 ~160 ~-1", "~-160 ~0 ~-1 ~160", "~0 ~0 ~160 ~160"):
        sunucu.komut(f"forceload add {alan}", r"to be force loaded|already marked")
    time.sleep(20)
    ornekler = []
    tps_ornegi(sunucu, "boşta (forceload sonrası)", ornekler)
    sunucu.komut("dogalafet pulsar baslat", r"pulsar is born|pulsar era has already begun")
    sunucu.komut("dogalafet asama ileri pulsar", r"(?i)pulsar", "pulsar tam güç")
    sunucu.komut("dogalafet durum", r"sun: Pulsar era", "durum (evre pulsar)")
    isaret = sunucu.isaret()
    sunucu.komut(f"dogalafet meteor yagmur {TPS_SURESI} {METEOR_DAKIKA_BASINA}",
                 rf"Meteor shower #\d+ started: {TPS_SURESI} s")
    baslangic = time.monotonic()
    while time.monotonic() - baslangic < TPS_SURESI:
        time.sleep(TPS_ARALIGI)
        tps_ornegi(sunucu, f"pulsar +{time.monotonic() - baslangic:.0f} sn", ornekler)
    for _ in range(12):
        if "pending craters: 0 " in sunucu.komut("dogalafet meteor bilgi", r"Flying meteors: \d+/\d+"):
            break
        time.sleep(TPS_ARALIGI)
        tps_ornegi(sunucu, f"krater kuyruğu +{time.monotonic() - baslangic:.0f} sn", ornekler)
    yangin = sunucu.komut("dogalafet yangin bilgi", r"Burning cells: \d+/").strip()
    sunucu.komut("dogalafet durum", r"sun: Pulsar era", "durum (200 sn sonra hâlâ pulsar)")
    bitis = sunucu.isaret()
    sunucu.komut("dogalafet meteor durdur", r"Stopped \d+ command meteor shower")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished \d+ fire cells")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")
    with sunucu.kilit:
        satirlar = sunucu.satirlar[isaret:bitis]
    krater = blok = suya = 0
    for s in satirlar:
        e = re.search(r"meteor krateri tamam: .*kazılan (\d+), korunan \d+, astar (\d+), kenar (\d+)", s)
        if e:
            krater += 1
            blok += sum(int(g) for g in e.groups())
        suya += "meteor suya düştü" in s
    yuklu = ornekler[1:]
    ort_ms = sum(o[1] for o in yuklu) / len(yuklu)
    en_dusuk = min(o[2] for o in yuklu)
    makine = runclient_kilit.makine()
    ozet = (f"MAKINE={makine}\n"
            f"pulsar tam güç + afetBlokHasari true + meteor {TPS_SURESI} sn × {METEOR_DAKIKA_BASINA}/dk (tek hedef), "
            f"±160 blok forceload\n"
            f"ortalama tick {ort_ms:.2f} ms (ölçüt < {TICK_ESIGI_MS:.0f}), en düşük TPS {en_dusuk:.2f} (ölçüt > {TPS_ESIGI:.0f})\n"
            f"krater {krater}, suya {suya}; krater blok işlemi {blok}; {yangin}\n")
    (klasor / "tps.txt").write_text(ozet + "\n".join(f"{e}	{ms:.2f} ms	TPS {t:.2f}" for e, ms, t in ornekler) + "\n",
                                    encoding="utf-8")
    gecti = en_dusuk > TPS_ESIGI and ort_ms < TICK_ESIGI_MS
    satir = (f"PULSAR-TPS {'GEÇTİ' if gecti else 'KALDI'} makine={makine} en_dusuk_tps={en_dusuk:.2f} "
             f"ort_tick_ms={ort_ms:.2f} ornek={len(yuklu)} krater={krater} blok={blok}")
    sunucu.yaz("### " + satir)
    print(satir, flush=True)
    if en_dusuk <= TPS_ESIGI:
        raise cs.SenaryoHatasi(f"en düşük TPS {en_dusuk:.2f} ≤ {TPS_ESIGI:.0f}")
    if ort_ms >= TICK_ESIGI_MS:
        raise cs.SenaryoHatasi(f"ortalama tick {ort_ms:.2f} ms ≥ {TICK_ESIGI_MS:.0f} ms")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    duzeltme = [s for s in metin.splitlines() if re.search(r"Incorrect key (gokyuzu|meteor)\b", s)]
    if duzeltme:
        hatalar.append(f"{len(duzeltme)} gokyuzu/meteor config düzeltme satırı, ilki: {duzeltme[0]}")
    if not re.search(r"\d+=gokyuzu/yutma", metin):
        hatalar.append("ağ kimlik listesinde gokyuzu/yutma yok")
    return hatalar


def main():
    if "--olcum" in sys.argv[1:]:
        # Ana makinenin işi: kuyruk `UZAK=0`'ı ana spool'a koyar, işin içinde senaryo yerelde koşar (pc2'ye gitmez).
        os.environ["UZAK"] = "0"
    a = cs.argumanlar(__doc__, ek=lambda p: p.add_argument("--olcum", action="store_true",
                                                          help="yalnız ana makinede koş (K0-33, K-E29 b)"))
    klasor = a.kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        sunucu = cs.Sunucu("tps", klasor / "tps.log")
        try:
            tps_kosusu(sunucu, klasor)
        except cs.SenaryoHatasi as hata:
            sunucu.yaz(f"!!! {hata}")
            print(hata, file=sys.stderr)
            sonuc = 1
        finally:
            kodlar.append(sunucu.durdur())
    finally:
        cs.ozellikleri_geri_al(ozgun)
    yol = klasor / "tps.log"
    if yol.exists():
        for hata in log_denetimi(yol):
            print(f"tps: {hata}", file=sys.stderr)
            sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(kod != 0 for kod in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
