#!/usr/bin/env python3
"""Deprem ve sinkhole runServer senaryosu (oyuncusuz, dedicated; K0-18), tekrar edilebilir.

Çalıştırma alt yapısı tools/senaryo/cekirdek_sunucu.py'den gelir (argümanlar, ağır süreç kapısı, server.properties,
log denetimi); sunucu `-PoyunTestiAfeti` olmadan açılır çünkü deprem ve sinkhole artık gerçek afet olarak kayıtlıdır. Dünya
`run-server/deprem-senaryo` her koşuda sıfırdan üretilir; oyuncusuz sunucuda afet konsolun konumuna (doğuş noktası) kurulur,
sinkhole'lar yarığın üstüne düşmesin diye `execute positioned` ile doğuşun 48 blok çaprazına (doğuş chunk'ları yüklü).

Koşu 1: `start deprem 2`, `asama ileri` x2 (AKTIF) → `[deprem] yarık tamam` (değişen blok > 0) ve doğal bitiş;
`start sinkhole 3`, `asama ileri` x2 → `[sinkhole] çukur tamam` (değişen blok > 0) ve doğal bitiş; `gamerule afetBlokHasari
false` + `start sinkhole 1`, `asama ileri` x2 → çukur tamam değişen blok 0, doğal bitiş, `gamerule afetBlokHasari true`
(S5'ten önce: S5 yarığının dalları doğuş çaprazlarını kesebilir); istek 24 (afet-fizik AF-30): `start deprem 3`, AKTIF,
yarık tamam, `Afet bitti: deprem seviye 3 neden DOGAL` artçı sarsıntı satırından önce gelir (artçılar afet yuvası dışında), hemen
`start sinkhole 2` kabul edilir ve `stop` ile biter, artçı sarsıntı sonra yine gelir; `start deprem 5`, AKTIF, 15 sn sonra `stop` (neden KOMUT);
`start deprem 3`, AKTIF'te sunucuya `stop` (afet sürerken
kayıt). Koşu 2 (aynı dünya): `Aktif afet geri yüklendi: deprem seviye 3 aşama aktif`, yarık tamam, artçı sarsıntı
(`[deprem] artçı sarsıntı 1/1`, DS-14) ve doğal bitiş, `stop`.
Tepe TPS: S2 yarığı, S3 çukurun çöküşü ve S5 deprem sırasında `forge tps` (Forge'un 100 tick'lik ortalaması) örneklenir;
örnekler ve en düşük TPS `<kanıt>/tps.txt`'ye yazılır.
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, deprem/sinkhole config düzeltme WARN'ı yok,
`[basarim] ... yayıncı olarak kayıtlı değil` yok, süreçler 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/deprem_sunucu.py --kanit <dizin> [--port <n>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı.
"""
import os
import re
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "deprem-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261142", "online-mode": "false"}


class Sunucu(cs.Sunucu):
    """cs.Sunucu, sınama afeti bayrağı olmadan."""

    def __init__(self, ad, log_yolu):  # noqa: super().__init__ bilerek çağrılmaz (bayraksız komut)
        self.ad = ad
        self.satirlar = []
        self.kilit = threading.Condition()
        self.baslangic = time.monotonic()
        self.log = log_yolu.open("w", encoding="utf-8")
        self.surec = subprocess.Popen(
            ["./gradlew", "runServer", "--console=plain", "--offline"], cwd=cs.KOK,
            env=dict(os.environ, JAVA_HOME=cs.JAVA_HOME, LC_ALL="C.UTF-8"), stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", bufsize=1,
            start_new_session=True)
        threading.Thread(target=self._oku, daemon=True).start()


def aktife(sunucu, tur):
    sunucu.komut("dogalafet asama ileri", rf"Afet aşaması: {tur} on_uyari -> siren")
    sunucu.komut("dogalafet asama ileri", rf"Afet aşaması: {tur} siren -> aktif")


def degisen(satir):
    return int(re.search(r"değişen blok (\d+)", satir).group(1))


TPS = []


def tps_ornegi(sunucu, etiket):
    satir = sunucu.komut("forge tps", r"Overall: Mean tick time: [\d.]+ ms\. Mean TPS: [\d.]+")
    eslesme = re.search(r"Mean tick time: ([\d.]+) ms\. Mean TPS: ([\d.]+)", satir)
    TPS.append((etiket, float(eslesme.group(1)), float(eslesme.group(2))))


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    tps_ornegi(sunucu, "boşta")
    sunucu.komut("dogalafet start deprem 2", r"Afet başladı: deprem seviye 2")
    aktife(sunucu, "deprem")
    time.sleep(4)
    tps_ornegi(sunucu, "deprem S2 yarık açılıyor (+4 sn)")
    satir = sunucu.bekle(r"\[deprem\] yarık tamam: seviye 2", 0, 90, "seviye 2 yarık")
    if degisen(satir) <= 0:
        raise cs.SenaryoHatasi(f"yarık blok değiştirmedi: {satir.strip()}")
    sunucu.bekle(r"Afet bitti: deprem seviye 2 neden DOGAL", 0, 90, "deprem doğal bitiş")

    sunucu.komut("execute positioned ~48 ~ ~48 run dogalafet start sinkhole 3", r"Afet başladı: sinkhole seviye 3")
    aktife(sunucu, "sinkhole")
    time.sleep(9)
    tps_ornegi(sunucu, "sinkhole S3 çöküş (+9 sn)")
    time.sleep(4)
    tps_ornegi(sunucu, "sinkhole S3 geri çekilme (+13 sn)")
    satir = sunucu.bekle(r"\[sinkhole\] çukur tamam: seviye 3", 0, 60, "seviye 3 çukur")
    if degisen(satir) <= 0:
        raise cs.SenaryoHatasi(f"çukur blok değiştirmedi: {satir.strip()}")
    sunucu.bekle(r"Afet bitti: sinkhole seviye 3 neden DOGAL", 0, 60, "sinkhole doğal bitiş")

    # Hasar kapalı S1 çukuru S5 depremden önce: S5 yarığı ve ±45° dalları (yarım boy 128) doğuş çaprazlarını kesip
    # yüzeyde su açabilir, sinkhole "uygun yer yok" der (entegrasyon 4).
    sunucu.komut("gamerule afetBlokHasari false", r"afetBlokHasari is now set to: false")
    sunucu.komut("execute positioned ~-48 ~ ~48 run dogalafet start sinkhole 1", r"Afet başladı: sinkhole seviye 1")
    aktife(sunucu, "sinkhole")
    satir = sunucu.bekle(r"\[sinkhole\] çukur tamam: seviye 1", 0, 40, "seviye 1 çukur (hasar kapalı)")
    if degisen(satir) != 0:
        raise cs.SenaryoHatasi(f"blok hasarı kapalıyken blok değişti: {satir.strip()}")
    sunucu.bekle(r"Afet bitti: sinkhole seviye 1 neden DOGAL", 0, 40, "sinkhole doğal bitiş")
    sunucu.komut("gamerule afetBlokHasari true", r"afetBlokHasari is now set to: true")

    bitince_yeni_afet(sunucu)

    sunucu.komut("dogalafet start deprem 5", r"Afet başladı: deprem seviye 5")
    aktife(sunucu, "deprem")
    for saniye in (5, 10, 15):
        time.sleep(5)
        tps_ornegi(sunucu, f"deprem S5 (+{saniye} sn)")
    sunucu.komut("dogalafet stop", r"Afet bitti: deprem seviye 5 neden KOMUT")

    sunucu.komut("dogalafet start deprem 3", r"Afet başladı: deprem seviye 3")
    aktife(sunucu, "deprem")


def bitince_yeni_afet(sunucu):
    """İstek 24: deprem bitti satırı gelince yeni afet hemen kabul edilir; artçı sarsıntı afet yuvası dışında sonra gelir.

    Eski kodda artçı AKTIF'in içindeydi: ana sarsıntı bitip yer durulduğu hâlde yuva artçı bitene kadar doluydu ve `start`
    "Zaten aktif bir afet var" diyordu; o kodda bu adım bitti satırından önce artçı satırını görüp düşer.
    """
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet start deprem 3", r"Afet başladı: deprem seviye 3")
    aktife(sunucu, "deprem")
    sunucu.bekle(r"\[deprem\] yarık tamam: seviye 3", isaret, 90, "seviye 3 yarık (istek 24)")
    bitis = sunucu.bekle(r"Afet bitti: deprem seviye 3 neden DOGAL", isaret, 150, "S3 deprem doğal bitiş (istek 24)")
    with sunucu.kilit:
        satirlar = list(sunucu.satirlar[isaret:])
    once = satirlar[:satirlar.index(bitis)]
    artci = [s for s in once if re.search(r"\[deprem\] artçı sarsıntı", s)]
    if artci:
        raise cs.SenaryoHatasi(f"deprem bitişi artçıdan sonra geldi (yuva artçı boyunca dolu): {artci[0].strip()}")
    # Hasar kapalı S1 çukurunun yeri: bu tohumda uygun olduğu biliniyor ve blok değişmedi; S3 yarığı (yarım boy 48)
    # bu çapraza ulaşmaz.
    satir = sunucu.komut("execute positioned ~-48 ~ ~48 run dogalafet start sinkhole 2",
                         r"Afet başladı: sinkhole seviye 2|is already running|zaten sürüyor|No suitable place"
                         r"|uygun yer bulunamadı")
    if "Afet başladı" not in satir:
        raise cs.SenaryoHatasi(f"deprem bittikten hemen sonra yeni afet kabul edilmedi: {satir.strip()}")
    sunucu.komut("dogalafet stop", r"Afet bitti: sinkhole seviye 2 neden KOMUT")
    sunucu.bekle(r"\[deprem\] artçı sarsıntı 1/1", isaret, 150, "S3 artçı afet yuvası dışında (istek 24)")


def kosu2(sunucu):
    satir = sunucu.bekle(r"Aktif afet geri yüklendi: deprem seviye 3 aşama aktif", 0, cs.ACILIS_SURESI,
                         "AKTIF deprem kayıttan döndü")
    sunucu.yaz(f"### {satir.strip()}")
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"\[deprem\] yarık tamam: seviye 3", 0, 90, "yeniden açılışta yarık sürdü")
    sunucu.bekle(r"\[deprem\] artçı sarsıntı 1/1", 0, 150, "S3 artçı sarsıntı (DS-14)")
    sunucu.bekle(r"Afet bitti: deprem seviye 3 neden DOGAL", 0, 30, "deprem doğal bitiş")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    for desen, ad in ((r"Incorrect key (deprem|sinkhole)\b", "deprem/sinkhole config düzeltme"),
                      (r"\[basarim\].*yayıncı olarak kayıtlı değil", "başarım yayıncı uyarısı"),
                      (r"Afet kodu hata verdi \((deprem|sinkhole)", "afet kodu hatası")):
        satirlar = [s for s in metin.splitlines() if re.search(desen, s)]
        if satirlar:
            hatalar.append(f"{len(satirlar)} {ad} satırı, ilki: {satirlar[0]}")
    return hatalar


def main():
    klasor = cs.argumanlar(__doc__, varsayilan_port=25642).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        for ad, adimlar in (("kosu", kosu1), ("kosu2", kosu2)):
            sunucu = Sunucu(ad, klasor / f"{ad}.log")
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
    if TPS:
        satirlar = [f"{etiket}: mean tick {ms:.2f} ms, TPS {tps:.2f}" for etiket, ms, tps in TPS]
        en_kotu = min(TPS, key=lambda o: o[2])
        satirlar.append(f"tepe yük: en düşük TPS {en_kotu[2]:.2f} ({en_kotu[0]}), en uzun mean tick "
                        f"{max(o[1] for o in TPS):.2f} ms")
        (klasor / "tps.txt").write_text("\n".join(satirlar) + "\n", encoding="utf-8")
        print("\n".join(satirlar))
    for ad in ("kosu", "kosu2"):
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
