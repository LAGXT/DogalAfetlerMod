#!/usr/bin/env python3
"""Güneş patlaması + kızıl cüce dedicated server senaryosu (bölüm D, GOREV §6 #4, #8, #12, #14), gerçek afetle.

Çekirdeğin `cekirdek_sunucu.py` altyapısı kullanılır; tek fark sunucunun `-PoyunTestiAfeti` **olmadan** açılmasıdır (ortak
`Sunucu` taklidi `gunes_patlamasi`'na kaydeder ve gerçek afet o zaman kaydedilmez). Kendi dünyası silinip yeniden üretilir,
`server.properties` sonda geri döner. Üç açılış:

koşu 1: start gunes_patlamasi, durum (normal güneş, ön-uyarıda gök değişmez), asama ileri (siren: güneş büyümeye başlar),
durum (büyüyen güneş), asama ileri (AKTIF: meteorlar), meteor bilgi (yağmur var), gamerule gunesPatlamasiAfeti false (KURAL:
afet durur, tamamlanmamış büyüme NORMAL'e döner), gamerule true; yeniden start, asama ileri x4 (siren, AKTIF, 2. ve 3. gün),
sunucu `stop`.
koşu 2: aktif güneş patlaması geri yüklendi, durum (büyüyen güneş), meteor bilgi (yağmur yeniden başladı), asama ileri x3
(4., 5. gün, doğal bitiş: süpernova), durum (süpernova, güneş zinciri sürüyor), hiz 1000 (süpernova söner: güneşsiz),
donma 100, kizilcuce baslat, kizilcuce bilgi (doğuş tamam), hiz 1, start gunes_patlamasi (kızıl cücede tekrarlanamaz ama elle
başlar) + stop (ön-uyarıda: kızıl cüce kalır), `stop`.
koşu 3: kizilcuce bilgi (kızıl cüce kalıcı), sifirla, `stop`.

Log denetimi (her koşu): `invalid dist` yok, istisna yok, config düzeltme WARN'ı yok, `yayıncı olarak kayıtlı değil` yok, gerçek
afet kayıtlı (taklit uyarısı yok).

Kullanım (worktree kökünden): python3 tools/senaryo/gunes_patlamasi_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
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
import cekirdek_sunucu as c  # noqa: E402

GUNES = "Solar Flare"


class GercekAfetSunucusu(c.Sunucu):
    """`cekirdek_sunucu.Sunucu` ile aynı; yalnız `-PoyunTestiAfeti` verilmez (gerçek güneş patlaması kayıtlı olsun)."""

    def __init__(self, ad, log_yolu):  # noqa: super().__init__ taklitli Gradle başlatır; bilerek çağrılmaz
        self.ad = ad
        self.satirlar = []
        self.kilit = threading.Condition()
        self.baslangic = time.monotonic()
        self.log = log_yolu.open("w", encoding="utf-8")
        ortam = dict(os.environ, JAVA_HOME=c.JAVA_HOME)
        self.surec = subprocess.Popen(
            ["./gradlew", "runServer", "--console=plain", "--offline"], cwd=c.KOK, env=ortam,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8",
            errors="replace", bufsize=1, start_new_session=True)
        threading.Thread(target=self._oku, daemon=True).start()


def bekle_komut(sunucu, komut, desen, aciklama, sure=60, aralik=2):
    """Komutu, çıktısında desen görülene kadar `aralik` saniyede bir yineler (sunucu durumu tick'le değişir)."""
    son = time.monotonic() + sure
    while True:
        isaret = sunucu.isaret()
        sunucu.yaz(f">>> {komut}")
        sunucu.surec.stdin.write(komut + "\n")
        sunucu.surec.stdin.flush()
        try:
            satir = sunucu.bekle(desen, isaret, 5, aciklama)
            sunucu.yaz(f"### tamam: {aciklama} -> {satir.strip()}")
            return satir
        except c.SenaryoHatasi:
            if time.monotonic() > son:
                raise
            time.sleep(aralik)


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    sunucu.komut("dogalafet start gunes_patlamasi", rf"{GUNES} started \(Unleveled\)")
    sunucu.komut("dogalafet durum", r"sun: Normal sun", "durum (ön-uyarıda gök değişmez)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Siren")
    sunucu.bekle(r"Güneş patlaması: siren, güneş büyümeye başladı", isaret, c.ADIM_SURESI, "siren: güneş büyüyor")
    sunucu.komut("dogalafet durum", r"sun: Swelling sun", "durum (büyüyen güneş)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Active")
    sunucu.bekle(r"Güneş patlaması: meteorlar başlıyor, süreç 5 gün \(120000 ölçekli tick\)", isaret, c.ADIM_SURESI,
                 "AKTIF: 5 günlük süreç")
    bekle_komut(sunucu, "dogalafet meteor bilgi", r"showers: [1-9]", "meteor yağmuru başladı")
    isaret = sunucu.isaret()
    sunucu.komut("gamerule gunesPatlamasiAfeti false", r"gunesPatlamasiAfeti is now set to: false")
    sunucu.bekle(r"Afet bitti: gunes_patlamasi seviye 0 neden KURAL", isaret, c.ADIM_SURESI, "kural kapalı: afet durdu")
    sunucu.bekle(r"Güneş patlaması durdu \(temiz false\)", isaret, c.ADIM_SURESI, "yumuşak durdurma")
    sunucu.komut("dogalafet durum", r"sun: Normal sun", "durum (tamamlanmamış büyüme geri alındı, K-C26)")
    sunucu.komut("gamerule gunesPatlamasiAfeti true", r"gunesPatlamasiAfeti is now set to: true")
    sunucu.komut("dogalafet start gunes_patlamasi", rf"{GUNES} started \(Unleveled\)")
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Siren")
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Active")
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Active", "asama ileri (2. gün)")
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Active", "asama ileri (3. gün)")


def kosu2(sunucu):
    sunucu.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Aktif afet geri yüklendi: gunes_patlamasi seviye 0 aşama aktif", 0, 30, "aktif güneş patlaması geri yüklendi")
    sunucu.komut("dogalafet durum", r"sun: Swelling sun", "durum (büyüyen güneş korundu)")
    bekle_komut(sunucu, "dogalafet meteor bilgi", r"showers: [1-9]", "yağmur yüklenince yeniden başladı")
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Active", "asama ileri (4. gün)")
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} phase: Active", "asama ileri (5. gün)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", rf"{GUNES} (phase: Ending|has ended)", "asama ileri (doğal bitiş)")
    sunucu.bekle(r"Güneş patlaması doğal bitti: süpernova", isaret, c.ADIM_SURESI, "doğal bitiş: süpernova")
    bekle_komut(sunucu, "dogalafet durum", r"sun: Supernova", "durum (süpernova)")
    sunucu.bekle(r"Afet bitti: gunes_patlamasi seviye 0 neden DOGAL", isaret, c.ADIM_SURESI, "afet doğal bitti")
    sunucu.bekle(r"Güneş zinciri yeni döngüde", isaret, c.ADIM_SURESI, "güneş zinciri devraldı")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet hiz 1000", r"run at x1000")
    sunucu.bekle(r"Güneş zinciri: supernova -> gunessiz \(süpernova söndü\)", isaret, c.ADIM_SURESI, "süpernova söndü")
    sunucu.komut("dogalafet donma 100", r"Freeze progress set to 100")
    sunucu.komut("dogalafet kizilcuce baslat", r"The red dwarf (is rising|has risen)")
    bekle_komut(sunucu, "dogalafet kizilcuce bilgi", r"Red dwarf era established", "kızıl cüce doğuşu tamam")
    sunucu.komut("dogalafet hiz 1", r"run at x1")
    sunucu.komut("dogalafet start gunes_patlamasi", rf"{GUNES} started \(Unleveled\)", "elle başlatma (kızıl cüce tamam)")
    sunucu.komut("dogalafet stop", rf"{GUNES} stopped\.")
    sunucu.komut("dogalafet durum", r"sun: Red dwarf era", "durum (ön-uyarıda durdurma kızıl cüceye dokunmaz)")


def kosu3(sunucu):
    sunucu.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Sun: Red dwarf era", "kızıl cüce kalıcı (yeniden açılış)")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")
    sunucu.komut("dogalafet durum", r"sun: Normal sun", "durum (sıfırlandı)")


def ek_denetim(yol):
    metin = yol.read_text(encoding="utf-8")
    hatalar = []
    if "yayıncı olarak kayıtlı değil" in metin:
        hatalar.append("[basarim] yayıncı olarak kayıtlı değil satırı var")
    if "gerçek afet kaydedilmedi" in metin:
        hatalar.append("güneş patlaması taklitle kayıtlı (gerçek afet yok)")
    if re.search(r"Incorrect key gunes_patlamasi\b", metin):
        hatalar.append("gunes_patlamasi config bölümü düzeltildi")
    return hatalar


def main():
    klasor = c.argumanlar(__doc__, varsayilan_port=25646).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    c.kapi_veya_cik(c.PORT)
    c.senaryo_dunyasini_sil()
    ozgun = c.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    kosular = (("kosu1", kosu1), ("kosu2", kosu2), ("kosu3", kosu3))
    try:
        for ad, adimlar in kosular:
            sunucu = GercekAfetSunucusu(ad, klasor / f"gunes-{ad}.log")
            try:
                adimlar(sunucu)
            except c.SenaryoHatasi as hata:
                sunucu.yaz(f"!!! {hata}")
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
            finally:
                kodlar.append(sunucu.durdur())
            if sonuc:
                break
    finally:
        c.ozellikleri_geri_al(ozgun)
        c.senaryo_dunyasini_sil()
    for ad, _ in kosular:
        yol = klasor / f"gunes-{ad}.log"
        if yol.exists():
            for hata in c.log_denetimi(yol) + ek_denetim(yol):
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(kod != 0 for kod in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
