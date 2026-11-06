#!/usr/bin/env python3
"""Kasırganın runServer senaryosu (oyuncusuz, dedicated; K0-18, K-E13), tekrar edilebilir.

Çalıştırma alt yapısı tools/senaryo/cekirdek_sunucu.py'den gelir; dünya `run-server/kasirga-senaryo` her koşuda sıfırdan
üretilir. Konsolda oyuncu olmadığı için `/dogalafet start kasirga` dünya doğuş noktasında (konsol konumu) başlar.

Adımlar: doğuş çevresi forceload; `kasirga bilgi` (aktif yok, varlık 0); `start kasirga 5`, `asama ileri` ×2 (siren, aktif),
15 sn sonra `kasirga bilgi` (AKTIF, uçan blok sayısı), `forge tps` (tepe anı TPS), `stop` → `kasirga bilgi`: kasirga=0,
ucan_blok=0 (KOM.3 temiz bitiş). Doğal bitiş: `start kasirga 1`, `asama ileri` ×2, `hiz 100` → log'da
`Afet bitti: kasirga seviye 1 neden DOGAL`, `hiz 1`, varlık 0. Sonda log denetimi: `invalid dist` yok, istisna yok,
`[basarim] ... yayıncı olarak kayıtlı değil` satırı 0, `kasirga` config düzeltme WARN'ı yok, süreç 0 ile çıktı.
Kayıttan dönüş (inceleme Kn5): `start kasirga 3`, `asama ileri` ×2, 12 sn (olgun evre) → sunucu `stop`; ikinci açılışta
`Aktif afet geri yüklendi: kasirga seviye 3 aşama aktif` ve `[kasirga] kayıttan dönüş: evre OLGUN|IP`, `kasirga bilgi` aynı evre (OLGUN/IP), seviye 3, `tornado=1`; sonra
`stop` → varlık 0 (kosu2.log).

Kullanım (worktree kökünden): python3 tools/senaryo/kasirga_sunucu.py --kanit <dizin> [--port 25641]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı.
"""
import re
import signal
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "kasirga-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261107", "online-mode": "false"}

SIFIR = r"entities in world: tornado=0, flying_block=0"


def kosu(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("forceload add ~-64 ~-64 ~64 ~64", r"to be force loaded|already marked")
    sunucu.komut("dogalafet kasirga bilgi", r"No active tornado; " + SIFIR)

    sunucu.komut("dogalafet start kasirga 5", r"Tornado started")
    sunucu.komut("dogalafet asama ileri", r"Tornado phase:", "siren")
    sunucu.komut("dogalafet asama ileri", r"Tornado phase:", "aktif")
    time.sleep(15)
    satir = sunucu.komut("dogalafet kasirga bilgi", r"Tornado: stage AKTIF, phase (DOGUS|OLGUN)", "S5 aktif")
    ucan = re.search(r"flying blocks (\d+)/(\d+), lifted (\d+)", satir)
    sunucu.yaz(f"### S5 15 sn: uçan {ucan.group(1)}/{ucan.group(2)}, kaldırılan {ucan.group(3)}" if ucan else "### uçan yok")
    if ucan and int(ucan.group(1)) > int(ucan.group(2)):
        raise cs.SenaryoHatasi(f"uçan blok tavanı aşıldı: {satir}")
    tps = sunucu.komut("forge tps", r"Overall\s*:.*Mean TPS: [0-9.,]+", "tepe anı TPS")
    sunucu.yaz(f"### tepe anı (S5): {tps.strip()}")
    olcum = re.search(r"Mean tick time: ([0-9.,]+) ms\. Mean TPS: ([0-9.,]+)", tps)
    if olcum and float(olcum.group(2).replace(",", ".")) < 18:
        raise cs.SenaryoHatasi(f"tepe anında TPS 18'in altında: {tps.strip()}")
    esya = sunucu.komut("execute if entity @e[type=minecraft:item]", r"Test (passed|failed)", "eşya varlığı sayısı")
    sunucu.yaz(f"### tepe anı eşya varlığı: {esya.strip()}")
    sunucu.komut("dogalafet stop", r"Tornado stopped\.")
    time.sleep(1)
    sunucu.komut("dogalafet kasirga bilgi", r"No active tornado; " + SIFIR, "stop sonrası varlık 0")

    isaret = sunucu.isaret()
    sunucu.komut("dogalafet start kasirga 1", r"Tornado started")
    sunucu.komut("dogalafet asama ileri", r"Tornado phase:", "siren")
    sunucu.komut("dogalafet asama ileri", r"Tornado phase:", "aktif")
    sunucu.komut("dogalafet hiz 100", r"run at x100")
    sunucu.bekle(r"Afet bitti: kasirga seviye 1 neden DOGAL", isaret, 90, "S1 doğal bitiş")
    sunucu.komut("dogalafet hiz 1", r"run at x1")
    time.sleep(1)
    sunucu.komut("dogalafet kasirga bilgi", r"No active tornado; " + SIFIR, "doğal bitiş sonrası varlık 0")

    sunucu.komut("dogalafet start kasirga 3", r"Tornado started")
    sunucu.komut("dogalafet asama ileri", r"Tornado phase:", "siren")
    sunucu.komut("dogalafet asama ileri", r"Tornado phase:", "aktif")
    time.sleep(12)
    satir = sunucu.komut("dogalafet kasirga bilgi", r"Tornado: stage AKTIF, phase OLGUN", "kapanış öncesi S3 olgun")
    sunucu.yaz(f"### kapanış öncesi: {satir.strip()}")


def kosu2(sunucu):
    isaret = sunucu.isaret()
    sunucu.bekle(r"Aktif afet geri yüklendi: kasirga seviye 3 aşama aktif", isaret, cs.ACILIS_SURESI, "kayıttan dönüş")
    sunucu.bekle(r"\[kasirga\] kayıttan dönüş: evre (OLGUN|IP)", isaret, cs.ACILIS_SURESI, "evre kayıttan kuruldu")
    sunucu.bekle(r"Done \(", isaret, cs.ACILIS_SURESI, "açılış")
    time.sleep(2)
    satir = sunucu.komut("dogalafet kasirga bilgi",
                         r"Tornado: stage AKTIF, phase (OLGUN|IP) .*level 3.*entities in world: tornado=1,",
                         "geri yüklenen S3, tek kasırga varlığı")
    sunucu.yaz(f"### kayıttan dönüş sonrası: {satir.strip()}")
    sunucu.komut("dogalafet stop", r"Tornado stopped\.")
    time.sleep(1)
    sunucu.komut("dogalafet kasirga bilgi", r"No active tornado; " + SIFIR, "geri yükleme + stop sonrası varlık 0")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")


def log_denetimi(yol, beklenen_temiz):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    yayinci = [s for s in metin.splitlines() if "yayıncı olarak kayıtlı değil" in s]
    if yayinci:
        hatalar.append(f"{len(yayinci)} '[basarim] yayıncı olarak kayıtlı değil' satırı")
    config = [s for s in metin.splitlines() if re.search(r"Incorrect key kasirga\b", s)]
    if config:
        hatalar.append(f"{len(config)} kasirga config düzeltme satırı, ilki: {config[0]}")
    temiz = re.findall(r"\[kasirga\] temizlendi \(temiz=true\)", metin)
    if len(temiz) < beklenen_temiz:
        hatalar.append(f"'[kasirga] temizlendi' satırı {len(temiz)} (en az {beklenen_temiz} bekleniyordu)")
    print(f"{yol.name}: temizlik satırı {len(temiz)}, yayıncı uyarısı {len(yayinci)}")
    return hatalar


def main():
    klasor = cs.argumanlar(__doc__, varsayilan_port=25641).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        # Stop (stop + doğal bitiş) ilk koşuda, geri yüklenen kasırganın stop'u ikincide temizlik satırı yazar.
        for ad, adimlar in (("kosu", kosu), ("kosu2", kosu2)):
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
    for ad, temiz in (("kosu", 2), ("kosu2", 1)):
        yol = klasor / f"{ad}.log"
        if yol.exists():
            for hata in log_denetimi(yol, temiz):
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(k != 0 for k in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
