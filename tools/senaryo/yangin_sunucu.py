#!/usr/bin/env python3
"""Ortak yangının runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Çalıştırma alt yapısı (Sunucu, server.properties geçişi, log denetimi) tools/senaryo/cekirdek_sunucu.py'den gelir; dünya
`run-server/yangin-senaryo` her koşuda sıfırdan üretilir, `test` dünyasına dokunulmaz (K0-18: pty'siz, stdin boruda).

Koşu 1: forceload, y=199 meşe tahta ve y=200 kalıcı yaprak döşemesi (13x13), yangin bilgi (0), y=201'den `yangin tutustur 6`,
yangin bilgi (aktif > 0), `stop`.
Koşu 2 (aynı dünya): açılışta SavedData'dan aktif yangın sürer (bilgi > 0), `yangin sondur`, bilgi (0), döşemede
`dogalafetler:afet_atesi` kalmadı (fill replace "No blocks were filled"), ikinci sondur 0, `stop`.
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, config düzeltme WARN'ı yok, iki süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/yangin_sunucu.py <log klasörü>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı.
"""
import re
import signal
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "yangin-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

TAVAN = 1500


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("forceload add -16 -16 16 16", r"to be force loaded")
    sunucu.komut("fill -6 199 -6 6 199 6 minecraft:oak_planks", r"Successfully filled 169 block")
    sunucu.komut("fill -6 200 -6 6 200 6 minecraft:oak_leaves[persistent=true]", r"Successfully filled 169 block")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}, budget 64/64 per tick", "bilgi (yangın yok)")
    sunucu.komut("execute positioned 0 201 0 run dogalafet yangin tutustur 6", r"Ignited [1-9][0-9]* fire cells")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (aktif > 0)")


def kosu2(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (yangın kayıttan sürdü)")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished [1-9][0-9]* fire cells")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}", "bilgi (aktif 0)")
    sunucu.komut("fill -6 199 -6 6 202 6 minecraft:air replace dogalafetler:afet_atesi", r"No blocks were filled",
                 "söndürme ateş bloğu bırakmadı")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished 0 fire cells")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    yangin = [s for s in metin.splitlines() if re.search(r"Incorrect key yangin\b", s)]
    if yangin:
        hatalar.append(f"{len(yangin)} yangin config düzeltme satırı, ilki: {yangin[0]}")
    return hatalar


def main():
    klasor = Path(sys.argv[1])
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        for ad, adimlar in (("kosu1", kosu1), ("kosu2", kosu2)):
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
    for ad in ("kosu1", "kosu2"):
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
