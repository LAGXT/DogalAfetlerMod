#!/usr/bin/env python3
"""Meteorun runServer senaryosu (oyuncusuz, dedicated; K0-18), tekrar edilebilir.

Çalıştırma alt yapısı tools/senaryo/cekirdek_sunucu.py'den gelir; dünya `run-server/meteor-senaryo` her koşuda sıfırdan
üretilir. Konsol komut kaynağının konumu dünya doğuş noktasıdır: `meteor dusur` oyuncusuz sunucuda oraya düşer.

Adımlar: doğuşün 60 blok üstüne 25x25 meşe tahta platform (yakıt; meteor oraya çarpar), `meteor bilgi` (0),
`meteor dusur buyuk` → log'da `meteor krateri tamam: ... (BUYUK, GUNES, ...)` ve kazılan > 0, `yangin bilgi` > 0;
`meteor dusur ay` (küçük ay meteoru) → krater satırı; `meteor yagmur 15 40` → en az bir krater daha, 15 sn sonra
`showers: 0`; `meteor durdur`, `yangin sondur`. KOM.13 (K-E18): `odul meteor cekirdek 1 Kimse` → "No player was found" (komut
kayıtlı ve ayrıştırılıyor; konsolda oyuncu yok; verme yolu GameTest `odulKomutuMeteorEsyalariniVerir`).
Bekleyen krater (M-25): 992..1007 (chunk 62,62) forceload, chunk 64'te y 120'de 11x11 tahta platform (altı okyanus); iki halka ötesi (chunk 64) yüklü ama varlık işlemez, üçüncü
halka (chunk 65, x ≥ 1040) yüklü değil. 1037,1000'e `meteor dusur buyuk` → hemen çarpar, kraterın x ≥ 1040 kısmı bekler
(`pending craters: 1 (1 waiting`); `forceload remove` (chunk'lar boşalır), `stop`. Koşu 2 (aynı dünya): açılışta
`meteor kaydı ... 1 bekleyen krater işi`, `forceload add` chunk 64–65 → `meteor krateri tamam: 1037, …` ve `pending
craters: 0`, `stop`. Sonda log denetimi: `invalid dist` yok, istisna yok, `meteor` config düzeltme WARN'ı yok, ağ kimlik
listesinde `meteor/sarsinti` var, süreçler 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/meteor_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>] [--tps]
`--tps` (meteor-2, inceleme-meteor-2-2 Mt9; GOREV §6 madde 10): yalnız yük ölçümü. `afetBlokHasari true`, doğuşün çevresi
±160 blok forceload (bir oyuncunun 10 chunk'lık simülasyon alanı; oyuncusuz sunucuda yağmurun tek hedefi doğuş noktasıdır),
`meteor yagmur 200 90` (200 sn gerçek zaman, 90/dk), yağmur boyunca ve krater işleri bitene dek 10 sn'de bir `forge tps`;
`<kanıt>/tps.txt`'ye örnekler, ortalama tick ms, en düşük TPS, krater ve blok işlemi sayısı. Ölçüt en düşük TPS > 18.
Çıkış: 0 geçti, 1 senaryo/denetim hatası (--tps: TPS ölçütü tutmadı), 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç
kapısı açılmadı (port ve kapı: cekirdek_sunucu.py, K-E13).
"""
import re
import signal
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "meteor-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

TAVAN = 240
KRATER = (r"meteor krateri tamam: -?\d+, -?\d+, -?\d+ \(ölçek {olcek}, {gorunum}, minecraft:overworld\): kazılan (\d+), "
          r"korunan \d+")
TPS_SURESI = 200
TPS_ARALIGI = 10
TPS_ESIGI = 18.0


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("forceload add ~-32 ~-32 ~32 ~32", r"to be force loaded|already marked")
    sunucu.komut("fill ~-12 ~60 ~-12 ~12 ~60 ~12 minecraft:oak_planks", r"Successfully filled 625 block")
    sunucu.komut("dogalafet meteor bilgi", rf"Flying meteors: 0/{TAVAN}, showers: 0, pending craters: 0")
    sunucu.komut("dogalafet odul meteor cekirdek 1 Kimse", r"No player was found", "KOM.13 odul meteor kayıtlı")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet meteor dusur buyuk", r"Meteor \(buyuk/gunes\) (falling towards|struck)")
    satir = sunucu.bekle(KRATER.format(olcek=r"2\.0", gorunum="GUNES"), isaret, 90, "büyük meteor krateri")
    kazilan = int(re.search(r"kazılan (\d+)", satir).group(1))
    if kazilan <= 0:
        raise cs.SenaryoHatasi(f"krater kazmadı: {satir}")
    if not re.search(r"ilk tutuşan [1-9]\d* hücre", satir):
        sunucu.yaz("### ilk çağrıda tutuşan 0 (alan sonraki tick'lerde sürebilir)")
    time.sleep(2)
    sunucu.komut("dogalafet yangin bilgi", r"Burning cells: [1-9][0-9]*/", "meteor ateşi yangın yöneticisinde")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet meteor dusur ay", r"Meteor \(kucuk/ay\) (falling towards|struck)")
    sunucu.bekle(KRATER.format(olcek=r"0\.7", gorunum="AY"), isaret, 90, "küçük ay meteoru krateri")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet meteor yagmur 15 40", r"Meteor shower #\d+ started: 15 s")
    sunucu.bekle(r"meteor (krateri tamam|suya düştü)", isaret, 60, "yağmurdan bir çarpma")
    time.sleep(16)
    sunucu.komut("dogalafet meteor bilgi", rf"Flying meteors: \d+/{TAVAN}, showers: 0", "yağmur bitti")
    sunucu.komut("dogalafet meteor durdur", r"Stopped \d+ command meteor shower")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished \d+ fire cells")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")
    bekleyen_krater(sunucu)


def bekleyen_krater(sunucu):
    """M-25: yüklü olmayan chunk'a taşan krater işi bekler ve kayıtta kalır."""
    sunucu.komut("forceload add 992 992 1007 1007", r"to be force loaded")
    # `execute if loaded` yalnız varlık işleyen chunk'ta geçer: zorlanan 62,62 yoklanır, sınır chunk'ı 64 onunla yüklenir.
    for _ in range(30):
        if "passed" in sunucu.komut("execute if loaded 1000 64 1000", r"Test (passed|failed)"):
            break
        time.sleep(1)
    else:
        raise cs.SenaryoHatasi("zorlanan chunk 62,62 30 sn'de yüklenmedi")
    time.sleep(2)
    # Tohumda 1037,1000 okyanus: chunk 64'e katı platform, meteor ona çarpar (suya düşen krater açmaz).
    sunucu.komut("fill 1029 120 995 1039 120 1005 minecraft:oak_planks", r"Successfully filled 121 block")
    sunucu.komut("execute positioned 1037 0 1000 run dogalafet meteor dusur buyuk",
                 r"Meteor \(buyuk/gunes\) struck 1037, 1000 at once", "sınır chunk'ında hemen çarpma")
    time.sleep(2)
    sunucu.komut("dogalafet meteor bilgi", r"pending craters: 1 \(1 waiting for unloaded chunks\)",
                 "krater işi yüklü olmayan chunk'ta bekliyor")
    sunucu.komut("forceload remove 992 992 1007 1007", r"Unmarked")
    for _ in range(30):
        if "failed" in sunucu.komut("execute if loaded 1000 64 1000", r"Test (passed|failed)"):
            break
        time.sleep(1)
    else:
        raise cs.SenaryoHatasi("chunk 62,62 30 sn'de boşalmadı")
    time.sleep(2)
    sunucu.komut("dogalafet meteor bilgi", r"pending craters: 1 ", "chunk boşalınca iş kayıtta")


def kosu2(sunucu):
    satir = sunucu.bekle(r"meteor kaydı \(minecraft:overworld\): 1 bekleyen krater işi", 0, cs.ACILIS_SURESI,
                         "yeniden açılışta bekleyen krater kayıttan okundu")
    sunucu.yaz(f"### {satir.strip()}")
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("dogalafet meteor bilgi", r"pending craters: 1 ", "kayıttan okunan iş")
    isaret = sunucu.isaret()
    sunucu.komut("forceload add 1024 992 1055 1007", r"to be force loaded")
    sunucu.bekle(KRATER.format(olcek=r"2\.0", gorunum="GUNES").replace(r"-?\d+, -?\d+, -?\d+", r"1037, -?\d+, 1000"),
                 isaret, 60, "chunk yüklenince bekleyen krater tamamlandı")
    sunucu.komut("dogalafet meteor bilgi", r"pending craters: 0 ", "bekleyen krater yok")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    meteor = [s for s in metin.splitlines() if re.search(r"Incorrect key meteor\b", s)]
    if meteor:
        hatalar.append(f"{len(meteor)} meteor config düzeltme satırı, ilki: {meteor[0]}")
    if not re.search(r"\d+=meteor/sarsinti", metin):
        hatalar.append("ağ kimlik listesinde meteor/sarsinti yok")
    kraterler = len(re.findall(r"meteor krateri tamam:", metin))
    print(f"krater satırı: {kraterler}")
    return hatalar


def tps_ornegi(sunucu, etiket, ornekler):
    # Sunucu makinenin yerel ayarıyla yazar: tr_TR'de ondalık virgül ("3,754 ms").
    satir = sunucu.komut("forge tps", r"Overall: Mean tick time: [\d.,]+ ms\. Mean TPS: [\d.,]+")
    eslesme = re.search(r"Mean tick time: ([\d.,]+) ms\. Mean TPS: ([\d.,]+)", satir)
    ornekler.append((etiket, float(eslesme.group(1).replace(",", ".")), float(eslesme.group(2).replace(",", "."))))


def tps_kosusu(sunucu, klasor):
    """Mt9: blok hasarı açık 90/dk yağmurun sunucu yükü (tek hedef, 200 sn)."""
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("gamerule afetBlokHasari true", r"afetBlokHasari is now set to: true")
    for alan in ("~-160 ~-160 ~-1 ~-1", "~0 ~-160 ~160 ~-1", "~-160 ~0 ~-1 ~160", "~0 ~0 ~160 ~160"):
        sunucu.komut(f"forceload add {alan}", r"to be force loaded|already marked")
    time.sleep(20)
    ornekler = []
    tps_ornegi(sunucu, "boşta (forceload sonrası)", ornekler)
    isaret = sunucu.isaret()
    sunucu.komut(f"dogalafet meteor yagmur {TPS_SURESI} 90", rf"Meteor shower #\d+ started: {TPS_SURESI} s")
    baslangic = time.monotonic()
    while time.monotonic() - baslangic < TPS_SURESI:
        time.sleep(TPS_ARALIGI)
        tps_ornegi(sunucu, f"yağmur +{time.monotonic() - baslangic:.0f} sn", ornekler)
    for _ in range(12):
        if "pending craters: 0 " in sunucu.komut("dogalafet meteor bilgi", r"Flying meteors: \d+/\d+"):
            break
        time.sleep(TPS_ARALIGI)
        tps_ornegi(sunucu, f"krater kuyruğu +{time.monotonic() - baslangic:.0f} sn", ornekler)
    bitis = sunucu.isaret()
    sunucu.komut("dogalafet meteor durdur", r"Stopped \d+ command meteor shower")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished \d+ fire cells")
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
    yagmur = ornekler[1:]
    ort_ms = sum(o[1] for o in yagmur) / len(yagmur)
    en_dusuk = min(o[2] for o in yagmur)
    tick = TPS_SURESI * 20
    ozet = (f"yağmur {TPS_SURESI} sn × 90/dk (tek hedef), afetBlokHasari true, ±160 blok forceload\n"
            f"ortalama tick {ort_ms:.2f} ms, en düşük TPS {en_dusuk:.2f} (ölçüt > {TPS_ESIGI:.0f})\n"
            f"krater {krater}, suya {suya}; krater blok işlemi {blok} (yağmur tick'i başına {blok / tick:.1f}, "
            f"krater başına {blok / max(1, krater):.0f})\n")
    (klasor / "tps.txt").write_text(ozet + "\n".join(f"{e}	{ms:.2f} ms	TPS {t:.2f}" for e, ms, t in ornekler) + "\n",
                                    encoding="utf-8")
    sunucu.yaz("### " + ozet.replace("\n", " | "))
    if en_dusuk <= TPS_ESIGI:
        raise cs.SenaryoHatasi(f"en düşük TPS {en_dusuk:.2f} ≤ {TPS_ESIGI:.0f} (GOREV §6 madde 10)")


def main():
    a = cs.argumanlar(__doc__, ek=lambda p: p.add_argument("--tps", action="store_true",
                                                          help="yalnız 90/dk yağmurun TPS ölçümü (Mt9)"))
    klasor = a.kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        kosular = (("tps", lambda s: tps_kosusu(s, klasor)),) if a.tps else (("kosu", kosu1), ("kosu2", kosu2))
        for ad, adimlar in kosular:
            sunucu = cs.Sunucu(ad, klasor / f"{ad}.log")
            try:
                adimlar(sunucu)
            except cs.SenaryoHatasi as hata:
                sunucu.yaz(f"!!! {hata}")
                print(hata, file=sys.stderr)
                sonuc = 1
            finally:
                kodlar.append(sunucu.durdur())
            if sonuc:
                break
    finally:
        cs.ozellikleri_geri_al(ozgun)
    for ad in ("tps",) if a.tps else ("kosu", "kosu2"):
        yol = klasor / f"{ad}.log"
        if yol.exists():
            for hata in log_denetimi(yol):
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(kod != 0 for kod in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
