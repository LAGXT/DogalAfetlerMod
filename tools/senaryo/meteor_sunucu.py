#!/usr/bin/env python3
"""Meteorun runServer senaryosu (oyuncusuz, dedicated; K0-18), tekrar edilebilir.

Çalıştırma alt yapısı tools/senaryo/cekirdek_sunucu.py'den gelir; dünya `run-server/meteor-senaryo` her koşuda sıfırdan
üretilir. Konsol komut kaynağının konumu dünya doğuş noktasıdır: `meteor dusur` oyuncusuz sunucuda oraya düşer.

Adımlar: doğuşün 60 blok üstüne 25x25 meşe tahta platform (yakıt; meteor oraya çarpar), `meteor bilgi` (0),
`meteor dusur buyuk` → log'da `meteor krateri tamam: ... (BUYUK, GUNES, ...)` ve kazılan > 0, `yangin bilgi` > 0;
`meteor dusur ay` (küçük ay meteoru) → krater satırı; `meteor yagmur 15 40` → en az bir krater daha, 15 sn sonra
`showers: 0`; `meteor durdur`, `yangin sondur`, `stop`. Sonda log denetimi: `invalid dist` yok, istisna yok, `meteor`
config düzeltme WARN'ı yok, ağ kimlik listesinde `meteor/sarsinti` var, süreç 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/meteor_sunucu.py <log klasörü>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı.
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

TAVAN = 48
KRATER = r"meteor krateri tamam: \S+ \({boyut}, {gorunum}, minecraft:overworld\): kazılan (\d+), korunan \d+"


def kosu(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("forceload add ~-32 ~-32 ~32 ~32", r"to be force loaded|already marked")
    sunucu.komut("fill ~-12 ~60 ~-12 ~12 ~60 ~12 minecraft:oak_planks", r"Successfully filled 625 block")
    sunucu.komut("dogalafet meteor bilgi", rf"Flying meteors: 0/{TAVAN}, showers: 0, pending craters: 0")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet meteor dusur buyuk", r"Meteor \(buyuk/gunes\) (falling towards|struck)")
    satir = sunucu.bekle(KRATER.format(boyut="BUYUK", gorunum="GUNES"), isaret, 90, "büyük meteor krateri")
    kazilan = int(re.search(r"kazılan (\d+)", satir).group(1))
    if kazilan <= 0:
        raise cs.SenaryoHatasi(f"krater kazmadı: {satir}")
    if not re.search(r"ilk tutuşan [1-9]\d* hücre", satir):
        sunucu.yaz("### ilk çağrıda tutuşan 0 (alan sonraki tick'lerde sürebilir)")
    time.sleep(2)
    sunucu.komut("dogalafet yangin bilgi", r"Burning cells: [1-9][0-9]*/", "meteor ateşi yangın yöneticisinde")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet meteor dusur ay", r"Meteor \(kucuk/ay\) (falling towards|struck)")
    sunucu.bekle(KRATER.format(boyut="KUCUK", gorunum="AY"), isaret, 90, "küçük ay meteoru krateri")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet meteor yagmur 15 40", r"Meteor shower #\d+ started: 15 s")
    sunucu.bekle(r"meteor (krateri tamam|suya düştü)", isaret, 60, "yağmurdan bir çarpma")
    time.sleep(16)
    sunucu.komut("dogalafet meteor bilgi", rf"Flying meteors: \d+/{TAVAN}, showers: 0", "yağmur bitti")
    sunucu.komut("dogalafet meteor durdur", r"Stopped \d+ command meteor shower")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished \d+ fire cells")
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


def main():
    klasor = Path(sys.argv[1])
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kod = None
    try:
        sunucu = cs.Sunucu("kosu", klasor / "kosu.log")
        try:
            kosu(sunucu)
        except cs.SenaryoHatasi as hata:
            sunucu.yaz(f"!!! {hata}")
            print(hata, file=sys.stderr)
            sonuc = 1
        finally:
            kod = sunucu.durdur()
    finally:
        cs.ozellikleri_geri_al(ozgun)
    yol = klasor / "kosu.log"
    if yol.exists():
        for hata in log_denetimi(yol):
            print(f"kosu: {hata}", file=sys.stderr)
            sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodu {kod}")
    if sonuc == 0 and kod != 0:
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
