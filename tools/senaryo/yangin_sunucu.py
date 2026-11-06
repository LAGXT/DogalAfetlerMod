#!/usr/bin/env python3
"""Ortak yangının runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Çalıştırma alt yapısı (Sunucu, server.properties geçişi, log denetimi) tools/senaryo/cekirdek_sunucu.py'den gelir; dünya
`run-server/yangin-senaryo` her koşuda sıfırdan üretilir, `test` dünyasına dokunulmaz (K0-18: pty'siz, stdin boruda).

Koşu 1: forceload, y=199 meşe tahta ve y=200 kalıcı yaprak döşemesi (13x13), yangin bilgi (0), y=201'den `yangin tutustur 6`,
yangin bilgi (aktif > 0), `stop`.
Koşu 2 (aynı dünya): açılışta SavedData'dan aktif yangın sürer (bilgi > 0), `yangin sondur`, bilgi (0), döşemede
`dogalafetler:afet_atesi` kalmadı (fill replace "No blocks were filled"), ikinci sondur 0. §6 #10: döşeme yeniden döşenir
ve tutuşturulur, yangın sürerken 144 uzak chunk forceload ile üretilir; TPS düşünce yönetici `yangın kısma: ... bütçe
64 -> 32|16` satırını yazar; bilgi (> 0), sondur, bilgi (0), `forceload remove all`. Y-19 (bekleyen silme): spawn chunk'ları
hiç boşalmadığı için 1000,1000'de (chunk 62,62) forceload ile döşeme ve `tutustur 4`; `forceload remove` sonrası
`execute if loaded` başarısız olana dek beklenir (chunk gerçekten boşaldı), `sondur` → log'da "ateş bloğu yüklü olmayan
chunk'ta"; `forceload add` → "bekleyen silme: N ateş bloğu kaldırıldı"; `fill … replace dogalafetler:afet_atesi` → "No blocks
were filled"; `forceload remove`. K-E11 (`sifirla` yangını söndürür): spawn döşemesi yeniden tutuşturulur, `dogalafet sifirla` →
log'da "yangın: dünya dönemi sıfırlandı, N hücre söndürüldü", bilgi (0), döşemede `afet_atesi` kalmadı; `stop`.
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, config düzeltme WARN'ı yok, iki süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/yangin_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı
(port ve kapı: cekirdek_sunucu.py, K-E13).
"""
import re
import signal
import sys
import time
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
    # Yeni dünyada chunk üretimi TPS'i düşürebilir: bütçe 64, 32 ya da 16 olabilir; burada yalnız aktif 0 aranır.
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}, budget (64|32|16)/64 per tick", "bilgi (yangın yok)")
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
    sunucu.komut("fill -6 199 -6 6 199 6 minecraft:oak_planks", r"Successfully filled [0-9]+ block|No blocks were filled")
    sunucu.komut("fill -6 200 -6 6 200 6 minecraft:oak_leaves[persistent=true]",
                 r"Successfully filled [0-9]+ block|No blocks were filled")
    sunucu.komut("execute positioned 0 201 0 run dogalafet yangin tutustur 6", r"Ignited [1-9][0-9]* fire cells")
    isaret = sunucu.isaret()
    sunucu.komut("forceload add 320 320 511 511", r"to be force loaded")
    sunucu.bekle(r"yangın kısma: minecraft:overworld bütçe 64 -> (32|16) hücre/tick", isaret, 180,
                 "yangın sürerken TPS düşünce bütçe kısıldı (§6 #10)")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (kısma sırasında yangın sürüyor)")
    sunucu.komut("dogalafet yangin sondur", r"Extinguished [1-9][0-9]* fire cells")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}", "bilgi (aktif 0)")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")
    bekleyen_silme(sunucu)
    sifirla_sondurur(sunucu)


def sifirla_sondurur(sunucu):
    """K-E11: `/dogalafet sifirla` komutla yakılanlar dahil bütün yangın hücrelerini iz bırakmadan söndürür."""
    sunucu.komut("forceload add -16 -16 16 16", r"to be force loaded|already marked")
    sunucu.komut("fill -6 199 -6 6 199 6 minecraft:oak_planks", r"Successfully filled [0-9]+ block|No blocks were filled")
    sunucu.komut("fill -6 200 -6 6 200 6 minecraft:oak_leaves[persistent=true]",
                 r"Successfully filled [0-9]+ block|No blocks were filled")
    sunucu.komut("execute positioned 0 201 0 run dogalafet yangin tutustur 6", r"Ignited [1-9][0-9]* fire cells")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (sıfırlamadan önce aktif > 0)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")
    sunucu.bekle(r"yangın: dünya dönemi sıfırlandı, [1-9][0-9]* hücre söndürüldü", isaret, 10, "sifirla yangını söndürdü")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}", "bilgi (sifirla sonrası aktif 0)")
    sunucu.komut("fill -6 199 -6 6 202 6 minecraft:air replace dogalafetler:afet_atesi", r"No blocks were filled",
                 "sifirla ateş bloğu bırakmadı")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")


def bekleyen_silme(sunucu):
    """Y-19: yüklü olmayan chunk'taki ateş söndürülür, chunk yüklenince bloğu kalkar."""
    sunucu.komut("forceload add 992 992 1007 1007", r"to be force loaded")
    sunucu.komut("fill 995 199 995 1004 199 1004 minecraft:oak_planks", r"Successfully filled 100 block")
    sunucu.komut("fill 995 200 995 1004 200 1004 minecraft:oak_leaves[persistent=true]", r"Successfully filled 100 block")
    sunucu.komut("execute positioned 1000 201 1000 run dogalafet yangin tutustur 4", r"Ignited [1-9][0-9]* fire cells")
    sunucu.komut("forceload remove 992 992 1007 1007", r"Unmarked")
    for _ in range(30):
        if "failed" in sunucu.komut("execute if loaded 1000 201 1000", r"Test (passed|failed)"):
            break
        time.sleep(1)
    else:
        raise cs.SenaryoHatasi("chunk 62,62 30 sn'de boşalmadı")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet yangin sondur", r"Extinguished [1-9][0-9]* fire cells")
    sunucu.bekle(r"yangın sondur: [0-9]+ hücre söndü; [1-9][0-9]* ateş bloğu yüklü olmayan chunk'ta", isaret, 10,
                 "yüklü olmayan ateş bekleyen silmeye girdi")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}", "bilgi (aktif 0)")
    isaret = sunucu.isaret()
    sunucu.komut("forceload add 992 992 1007 1007", r"to be force loaded")
    sunucu.bekle(r"yangın bekleyen silme: [1-9][0-9]* ateş bloğu kaldırıldı", isaret, 60,
                 "chunk yüklenince bekleyen ateş kalktı")
    sunucu.komut("fill 992 199 992 1007 202 1007 minecraft:air replace dogalafetler:afet_atesi", r"No blocks were filled",
                 "yüklü olmayan chunk'ta ateş kalmadı")
    sunucu.komut("forceload remove 992 992 1007 1007", r"Unmarked")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    yangin = [s for s in metin.splitlines() if re.search(r"Incorrect key yangin\b", s)]
    if yangin:
        hatalar.append(f"{len(yangin)} yangin config düzeltme satırı, ilki: {yangin[0]}")
    return hatalar


def main():
    klasor = cs.argumanlar(__doc__).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
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
