#!/usr/bin/env python3
"""Asit yağmuru ve orman yangını afetlerinin runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Çalıştırma alt yapısı (Sunucu, server.properties geçişi, log denetimi, ağır süreç kapısı, --port/--kanit) ortak
tools/senaryo/cekirdek_sunucu.py'den gelir (K-E13); dünya `run-server/asit-orman-senaryo` her koşuda sıfırdan üretilir.

Koşu 1:
- Asit yağmuru S1: start, asama ileri x2 (AKTIF, log "asit yağmuru: seviye 1 her yerde" ve "vanilla yağmuru açıldı (önceki
  hava …)", AF2-01/02), `weather clear` → "vanilla havası yağmuru kesmişti, yağmur yeniden açıldı", 15 sn yağış, durum,
  stop ("Acid Rain stopped.", log "asit yağmuru bitti: seviye 1 N blok aşındı" ve "önceki hava geri yüklendi (…)" açılıştaki
  özetle aynı). Asit dayanıklı cam ve taş yerinde.
- Asit yağmuru S5: start, AKTIF, 15 sn, `forge tps` (tepe TPS > 18), asama ileri (doğal bitiş, hava geri yüklendi), durum
  (aktif afet yok); vanilla yağmurunun sönmesi (0,01/tick) 5 sn beklenir: orman yangını yağmurda söner.
- Orman yangını S3: y=199 meşe tahta + y=200 kalıcı yaprak döşemesi (13x13) ve ortada yanmaz taş; 0 201 0'da start,
  AKTIF (log "orman yangını: seviye 3 başlangıç alanı"), yangin bilgi (> 0), stop ("Wildfire stopped.", yangının temiz
  kaynağı kapandı), bilgi (0), döşemede `afet_atesi` kalmadı, taş yerinde.
- Orman yangını S5: döşeme yeniden, start, AKTIF, `forge tps`; §6 #10: yangın sürerken 144 uzak chunk forceload ile
  üretilir, yönetici `yangın kısma: ... bütçe 64 -> 32|16` yazar; asama ileri (doğal bitiş: yayılma durur, hücreler en
  geç 30 sn'de yanıp biter), bilgi 0'a iner. Son olarak asit yağmuru S2 AKTIF bırakılır ve sunucu kapanır.
Koşu 2 (aynı dünya): açılışta aktif asit yağmuru geri yüklenir (seviye 2, AKTIF), durum, stop (saklanan hava kayıttan
dönüşte de geri yüklenir: "önceki hava geri yüklendi"), 5 sn yağmur sönümü, sifirla; döşeme yeniden, orman yangını S2
AKTIF'e getirilir (bilgi > 0) ve sunucu kapanır.
Koşu 3 (aynı dünya, inceleme AOi-8, kasırga Kn5 deseni): açılışta aktif orman yangını geri yüklenir (seviye 2, AKTIF), durum,
stop (yangının temiz kaynağı kapanır), bilgi 0, döşemede `afet_atesi` kalmadı, sifirla.
Sonda üç log denetlenir: `invalid dist` yok, istisna yok, config düzeltme WARN'ı yok, `[basarim] ... yayıncı olarak kayıtlı
değil` satırı yok, üç süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/asit_orman_yangini_sunucu.py --kanit <dizin> [--port <n>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı.
"""
import re
import signal
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "asit-orman-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

TAVAN = 1500
YAGIS_SANIYE = 15


def asit_s1(sunucu):
    sunucu.komut("setblock 3 200 3 minecraft:glass", r"Changed the block|Could not set the block")
    sunucu.komut("setblock 4 200 3 minecraft:stone", r"Changed the block|Could not set the block")
    sunucu.komut("dogalafet start asit_yagmuru 1", r"Acid Rain started \(Level 1 ")
    sunucu.komut("dogalafet asama ileri", r"Acid Rain phase: Siren")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Acid Rain phase: Active")
    sunucu.bekle(r"asit yağmuru: seviye 1 her yerde, 60 sn", isaret, 10, "S1 tablo değerleri")
    acilis = sunucu.bekle(r"asit yağmuru: vanilla yağmuru açıldı \(önceki hava (.*)\)", isaret, 10, "AF2-02 yağmur açıldı")
    onceki = re.search(r"önceki hava (.*)\)", acilis).group(1)
    isaret = sunucu.isaret()
    sunucu.komut("weather clear", r"Set the weather to clear")
    sunucu.bekle(r"asit yağmuru: vanilla havası yağmuru kesmişti, yağmur yeniden açıldı", isaret, 10,
                 "AF2-02 /weather clear asit sürerken geri alındı")
    time.sleep(YAGIS_SANIYE)
    sunucu.komut("dogalafet durum", r"Active disaster: Acid Rain, Level 1 .*phase: Active", "durum (asit S1 aktif)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet stop", r"Acid Rain stopped\.")
    sunucu.bekle(r"asit yağmuru bitti: seviye 1 [0-9]+ blok aşındı, [0-9]+ çatlak örtüsü kaldırıldı", isaret, 10,
                 "stop: çatlak kuyruğu temizlendi")
    sunucu.bekle(r"asit yağmuru: önceki hava geri yüklendi \(" + re.escape(onceki) + r"\)", isaret, 10,
                 "AF2-02 bitişte önceki hava aynen geri yüklendi")
    sunucu.komut("execute if block 3 200 3 minecraft:glass", r"Test passed", "cam (asit dayanıklı) yerinde")
    sunucu.komut("execute if block 4 200 3 minecraft:stone", r"Test passed", "taş (asit dayanıklı) yerinde")


def asit_s5(sunucu):
    sunucu.komut("dogalafet start asit_yagmuru 5", r"Acid Rain started \(Level 5 ")
    sunucu.komut("dogalafet asama ileri", r"Acid Rain phase: Siren")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Acid Rain phase: Active")
    sunucu.bekle(r"asit yağmuru: seviye 5 her yerde, 240 sn", isaret, 10, "S5 tablo değerleri")
    time.sleep(YAGIS_SANIYE)
    tps = sunucu.komut("forge tps", r"Overall.*Mean TPS", "tepe anında TPS (asit S5)")
    sunucu.yaz(f"### tepe anı (asit S5, küresel): {tps.strip()}")
    olcum = re.search(r"Mean TPS: ([0-9.,]+)", tps)
    if olcum and float(olcum.group(1).replace(",", ".")) <= 18:
        raise cs.SenaryoHatasi(f"asit S5 tepe anında TPS 18'in altında: {tps.strip()}")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Acid Rain (phase: Ending|has ended)")
    sunucu.bekle(r"asit yağmuru bitti: seviye 5 [0-9]+ blok aşındı", isaret, 10, "S5 doğal bitiş")
    sunucu.bekle(r"asit yağmuru: önceki hava geri yüklendi", isaret, 10, "S5 bitişte hava geri yüklendi")
    sunucu.komut("dogalafet durum", r"No active disaster\.", "durum (afet yok)")
    time.sleep(5)


def doseme(sunucu):
    sunucu.komut("fill -6 199 -6 6 199 6 minecraft:oak_planks", r"Successfully filled [0-9]+ block|No blocks were filled")
    sunucu.komut("fill -6 200 -6 6 200 6 minecraft:oak_leaves[persistent=true]",
                 r"Successfully filled [0-9]+ block|No blocks were filled")
    sunucu.komut("setblock 0 200 3 minecraft:stone", r"Changed the block")


def orman_s3(sunucu):
    doseme(sunucu)
    sunucu.komut("execute positioned 0 201 0 run dogalafet start orman_yangini 3", r"Wildfire started \(Level 3 ")
    sunucu.komut("dogalafet asama ileri", r"Wildfire phase: Siren")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Wildfire phase: Active")
    sunucu.bekle(r"orman yangını: seviye 3 başlangıç alanı 0, 201, 0 r=10 yoğunluk 0\.4", isaret, 10,
                 "başlangıç alanı afetin kaynağıyla tutuştu")
    time.sleep(5)
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (orman yangını yanıyor)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet stop", r"Wildfire stopped\.")
    sunucu.bekle(r"yangın kaynağı bitti \(temiz\): YanginKaynagi\[tur=ORMAN_YANGINI, afetNo=[0-9]+\] [1-9][0-9]* hücre",
                 isaret, 10, "stop: yangın hemen söndü (Y-27)")
    sunucu.bekle(r"orman yangını bitti: seviye 3 en çok [1-9][0-9]* hücre", isaret, 10, "orman özeti")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}", "bilgi (stop sonrası 0)")
    sunucu.komut("fill -16 195 -16 16 215 16 minecraft:air replace dogalafetler:afet_atesi", r"No blocks were filled",
                 "stop ateş bloğu bırakmadı")
    sunucu.komut("execute if block 0 200 3 minecraft:stone", r"Test passed", "yanmaz taş yerinde")


def orman_s5(sunucu):
    doseme(sunucu)
    sunucu.komut("execute positioned 0 201 0 run dogalafet start orman_yangini 5", r"Wildfire started \(Level 5 ")
    sunucu.komut("dogalafet asama ileri", r"Wildfire phase: Siren")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Wildfire phase: Active")
    sunucu.bekle(r"orman yangını: seviye 5 başlangıç alanı 0, 201, 0 r=24 yoğunluk 1\.0", isaret, 10, "S5 başlangıç")
    time.sleep(4)
    sunucu.komut("forge tps", r"Overall.*Mean TPS", "tepe anında TPS (orman S5)")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (S5 yanıyor)")
    isaret = sunucu.isaret()
    sunucu.komut("forceload add 320 320 511 511", r"to be force loaded")
    sunucu.bekle(r"yangın kısma: minecraft:overworld bütçe 64 -> (32|16) hücre/tick", isaret, 180,
                 "orman yangını sürerken TPS düşünce bütçe kısıldı (§6 #10)")
    sunucu.komut("forceload remove 320 320 511 511", r"Unmarked")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Wildfire (phase: Ending|has ended)")
    sunucu.bekle(r"yangın kaynağı bitti: YanginKaynagi\[tur=ORMAN_YANGINI, afetNo=[0-9]+\] [0-9]+ hücre yayılmadan", isaret,
                 10, "doğal bitiş: hücreler yayılmadan yanıp bitecek (Y-05)")
    for _ in range(50):
        if re.search(rf"Burning cells: 0/{TAVAN}", sunucu.komut("dogalafet yangin bilgi", r"Burning cells: ")):
            break
        time.sleep(1)
    else:
        raise cs.SenaryoHatasi("doğal bitişten sonra yangın 50 sn'de bitmedi")


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("forceload add -64 -64 64 64", r"to be force loaded")
    asit_s1(sunucu)
    asit_s5(sunucu)
    orman_s3(sunucu)
    orman_s5(sunucu)
    sunucu.komut("dogalafet start asit_yagmuru 2", r"Acid Rain started \(Level 2 ")
    sunucu.komut("dogalafet asama ileri", r"Acid Rain phase: Siren")
    sunucu.komut("dogalafet asama ileri", r"Acid Rain phase: Active")


def kosu2(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Aktif afet geri yüklendi: asit_yagmuru seviye 2 aşama aktif", 0, 30, "aktif asit yağmuru geri yüklendi")
    sunucu.komut("dogalafet durum", r"Active disaster: Acid Rain, Level 2 .*phase: Active", "durum (asit korundu)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet stop", r"Acid Rain stopped\.")
    sunucu.bekle(r"asit yağmuru: önceki hava geri yüklendi \(yağmur=", isaret, 10,
                 "AF2-02 kayıttan dönen asit saklanan havayı geri yükledi")
    time.sleep(5)
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")
    doseme(sunucu)
    sunucu.komut("execute positioned 0 201 0 run dogalafet start orman_yangini 2", r"Wildfire started \(Level 2 ")
    sunucu.komut("dogalafet asama ileri", r"Wildfire phase: Siren")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Wildfire phase: Active")
    sunucu.bekle(r"orman yangını: seviye 2 başlangıç alanı 0, 201, 0", isaret, 10, "S2 başlangıç (kayıttan dönüş için)")
    time.sleep(3)
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: [1-9][0-9]*/{TAVAN}", "bilgi (S2 yanıyor, kapanmadan önce)")


def kosu3(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Aktif afet geri yüklendi: orman_yangini seviye 2 aşama aktif", 0, 30, "aktif orman yangını geri yüklendi")
    sunucu.komut("dogalafet durum", r"Active disaster: Wildfire, Level 2 .*phase: Active", "durum (orman korundu)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet stop", r"Wildfire stopped\.")
    sunucu.bekle(r"orman yangını bitti: seviye 2 en çok [0-9]+ hücre", isaret, 10, "kayıttan dönen orman temiz durdu")
    sunucu.komut("dogalafet yangin bilgi", rf"Burning cells: 0/{TAVAN}", "bilgi (stop sonrası 0)")
    sunucu.komut("fill -16 195 -16 16 215 16 minecraft:air replace dogalafetler:afet_atesi", r"No blocks were filled",
                 "kayıttan dönen yangın stop sonrası ateş bloğu bırakmadı")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    for desen, ad in ((r"Incorrect key (asit_yagmuru|orman_yangini)\b", "config düzeltme"),
                      (r"yayıncı olarak kayıtlı değil", "başarım yayıncı uyarısı")):
        satirlar = [s for s in metin.splitlines() if re.search(desen, s)]
        if satirlar:
            hatalar.append(f"{len(satirlar)} {ad} satırı, ilki: {satirlar[0]}")
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
        for ad, adimlar in (("kosu1", kosu1), ("kosu2", kosu2), ("kosu3", kosu3)):
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
    for ad in ("kosu1", "kosu2", "kosu3"):
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
