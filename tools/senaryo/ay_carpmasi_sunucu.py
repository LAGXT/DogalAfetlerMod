#!/usr/bin/env python3
"""Ay çarpmasının runServer senaryosu (oyuncusuz, dedicated; K0-18, K-E13), tekrar edilebilir.

Çalıştırma alt yapısı tools/senaryo/cekirdek_sunucu.py'den gelir; dünya `run-server/ay-carpmasi-senaryo` her koşuda sıfırdan
üretilir. Oyuncusuz sunucuda ay parçası yağmuru dünya doğuş noktasına düşer (MeteorAPI sözleşmesi).

Koşu 1 (zincir, §6 #12): `forceload add ~-32 ~-32 ~32 ~32` (oyuncusuz dünya 300 tick sonra varlık işlemeyi bırakır; zorlanan
chunk varken sürer, meteor_sunucu.py gibi) → `start ay_carpmasi` → `asama ileri` (SIREN: "ay yaklaşıyor", global yaklaşma ve çıtırtı katmanları)
→ `asama ileri` (çarpma: "ay çarptı ... tsunami çarpanı x3.50", deniz sürecinin 10 dk beklemesi) → 15 sn içinde
"sarsıntı bitti" ve 10. sn'de "ay parçası yağmuru" → doğuş noktasına en az bir AY görünümlü krater → `asama ileri` x2
(doğal bitiş) → `start ay_carpmasi` reddedilir (ay yok, madde 4) → `aysonrasi atla` (deniz yükseliyor) → `asama ileri`
(hedefe) → `durum` (ay yok, deniz +4) → `sifirla` → `start` + `asama ileri` (ikinci yaklaşma, SIREN'de kalır) → sunucu `stop`.
Koşu 2 (aynı dünya): dönem `ay=true ... ayOlay=<n>` (yaklaşma kalıcı), aktif ay çarpması SIREN'de geri yüklendi, yaklaşma
eğrisi kaldığı yerden yeniden kuruldu; `asama ileri` (çarpma), `stop` (çarpmadan sonra: ay yok kalır) → sunucu `stop`.
Koşu 3 (aynı dünya): dönem `ay=false ... ayOlay=<n>` (ayın yokluğu kalıcı), `durum` "Moon: none", `sifirla`.
Sonda log denetimi: `invalid dist` yok, istisna yok, `ay_carpmasi` config düzeltme WARN'ı yok, "yayıncı olarak kayıtlı
değil" satırı yok, süreçler 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/ay_carpmasi_sunucu.py --port 25645 --kanit <dizin>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı.
"""
import re
import signal
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "ay-carpmasi-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

AY_KRATERI = r"meteor krateri tamam: -?\d+, -?\d+, -?\d+ \((KUCUK|BUYUK), AY, minecraft:overworld\)"


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("forceload add ~-32 ~-32 ~32 ~32", r"to be force loaded|already marked")
    sunucu.komut("dogalafet start ay_carpmasi", r"Moon Impact started")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Moon Impact phase")
    sunucu.bekle(r"\[ay_carpmasi\] ay yaklaşıyor: olay saati \d+", isaret, 10, "yaklaşma başladı")
    sunucu.bekle(r"ortam katmanı başladı: ay_carpmasi/yaklasma .*global", isaret, 10, "yaklaşma uğultusu")
    sunucu.bekle(r"ortam katmanı başladı: ay_carpmasi/citirti .*global", isaret, 10, "parçalanma çıtırtısı")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Moon Impact phase")
    sunucu.bekle(r"\[ay_carpmasi\] ay çarptı: saat \d+, tsunami çarpanı x3\.50", isaret, 10, "çarpma")
    sunucu.bekle(r"Sanal su: ay yok oldu, deniz yükselmesi \d+ ölçekli tick sonra", isaret, 10, "deniz beklemesi")
    sunucu.bekle(r"ortam katmanı durdu: ay_carpmasi/yaklasma", isaret, 10, "çarpmada sessizlik")
    sunucu.bekle(r"\[ay_carpmasi\] ay parçası yağmuru #\d+ başladı: 1800 tick", isaret, 30, "10. sn'de yağmur")
    sunucu.bekle(r"\[ay_carpmasi\] sarsıntı bitti: 0 blok düştü", isaret, 30, "sarsıntı penceresi (oyuncusuz: 0 blok)")
    sunucu.bekle(AY_KRATERI, isaret, 120, "doğuş noktasına ay görünümlü krater")
    sunucu.komut("dogalafet asama ileri", r"Moon Impact phase")
    sunucu.komut("dogalafet asama ileri", r"Moon Impact has ended")
    sunucu.bekle(r"Afet bitti: ay_carpmasi seviye 0 neden DOGAL", isaret, 10, "doğal bitiş")
    sunucu.komut("dogalafet start ay_carpmasi", r"No suitable place found for Moon Impact", "ay yokken yeniden başlamaz")
    sunucu.komut("dogalafet aysonrasi atla", r"Waiting skipped: the sea is rising")
    sunucu.komut("dogalafet asama ileri", r"\S")
    sunucu.komut("dogalafet durum", r"Sea level: \+4(\.0+)? blocks")
    sunucu.komut("dogalafet durum", r"Moon: none")
    sunucu.komut("dogalafet sifirla", r"was reset")
    sunucu.komut("dogalafet durum", r"Moon: present")
    sunucu.komut("dogalafet start ay_carpmasi", r"Moon Impact started")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Moon Impact phase")
    sunucu.bekle(r"\[ay_carpmasi\] ay yaklaşıyor", isaret, 10, "ikinci yaklaşma (kayıtta kalacak)")
    sunucu.komut("forceload remove all", r"Unmarked all force loaded chunks")


def kosu2(sunucu):
    satir = sunucu.bekle(r"Dünya dönemi okundu \(dogalafetler_donem\): .*ay=true .*ayOlay=\d+", 0, cs.ACILIS_SURESI,
                         "yaklaşan ay kayıttan okundu")
    sunucu.yaz(f"### {satir.strip()}")
    sunucu.bekle(r"Aktif afet geri yüklendi: ay_carpmasi seviye 0 aşama siren", 0, cs.ACILIS_SURESI, "SIREN geri yüklendi")
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"ortam katmanı başladı: ay_carpmasi/yaklasma", 0, 10, "yaklaşma sesi kaldığı yerden")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Moon Impact phase")
    sunucu.bekle(r"\[ay_carpmasi\] ay çarptı", isaret, 10, "geri yüklenen yaklaşmadan çarpma")
    sunucu.komut("dogalafet stop", r"Moon Impact stopped")
    sunucu.komut("dogalafet durum", r"Moon: none", "çarpmadan sonra stop ayı geri getirmez")


def kosu3(sunucu):
    satir = sunucu.bekle(r"Dünya dönemi okundu \(dogalafetler_donem\): .*ay=false .*ayOlay=\d+", 0, cs.ACILIS_SURESI,
                         "ayın yokluğu ve çarpma saati kayıttan okundu")
    sunucu.yaz(f"### {satir.strip()}")
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.komut("dogalafet durum", r"Moon: none")
    sunucu.komut("dogalafet sifirla", r"was reset")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    for desen, aciklama in ((r"Incorrect key ay_carpmasi\b", "ay_carpmasi config düzeltme satırı"),
                            (r"yayıncı olarak kayıtlı değil", "başarım yayıncı uyarısı")):
        sayi = len(re.findall(desen, metin))
        if sayi:
            hatalar.append(f"{sayi} {aciklama}")
    print(f"{yol.name}: ay krateri {len(re.findall(AY_KRATERI, metin))}")
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
    kosular = (("kosu", kosu1), ("kosu2", kosu2), ("kosu3", kosu3))
    try:
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
    for ad, _ in kosular:
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
