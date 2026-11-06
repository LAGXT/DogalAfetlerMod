#!/usr/bin/env python3
"""Uyarı alt sisteminin dedicated server senaryosu, çekirdeğin `cekirdek_sunucu.py` altyapısıyla.

Sunucu Gradle ile pty'siz, stdin'i boruya bağlı açılır (K0-18); `-PoyunTestiAfeti=deprem,gunes_patlamasi`. Kendi dünyası
(`run-server/cekirdek-senaryo`) silinip yeniden üretilir, `server.properties` sonda geri döner.

Oyuncusuz (varsayılan): start deprem 1, asama ileri (SIREN: uyarı log'u "SIREN aşaması ... 0 oyuncuya"; oyuncusuz sunucuda
çekirdek siren olayı yayınlamaz), asama ileri (AKTIF), stop; start gunes_patlamasi, asama ileri (SIREN), stop (siren
sürerken durdurma); sunucuya `stop`.

`--istemci`: sunucu açılınca istemci `uyari_istemci.kos` ile (K0-21 rev. 2 protokolü, ses yalnız wave yazıcıya) açılır
ve `--quickPlayMultiplayer localhost:<port>` ile `Uyari` adıyla bağlanır (otomasyon `uyari-sunucu`: komut vermez). İstemci
sesi açınca konsoldan start deprem 1 ve asama ileri (SIREN) verilir: sunucu log'unda "siren paketi gönderildi ... ->
[Uyari]"; 6 sn sonra asama ileri (AKTIF): "siren durdurma paketi gönderildi"; istemci fade-out'tan sonra çıkar, stop.
Önce ağır süreç kapısı: kullanılabilir RAM ≥ 10 GiB, Gradle sayısı < 2, başka runClient yok (docs/MAKINE.md), port boş;
oyuncusuz koşuda ortak kapı (cekirdek_sunucu.py, K-E13).

Log denetimi: `invalid dist` yok (ortak kod istemci sınıfı yüklemez, N15), istisna yok, config düzeltme WARN'ı yok,
`uyari/siren` paketleri kayıtlı.

Kullanım (worktree kökünden): python3 tools/senaryo/uyari_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>] [--istemci]
Çıkış: 0 geçti, 1 senaryo/denetim hatası ya da istemcili kapı açılmadı, 2 sunucu süreci sıfır dışı kodla çıktı, 3 kapı açılmadı.
"""
import re
import signal
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as c  # noqa: E402
import uyari_istemci as ui  # noqa: E402

OYUNCU = "Uyari"
ISTEMCI_WAV = Path("/tmp/uyari-ses-sunucu.wav")


def kosu(sunucu):
    sunucu.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"ağ paketi kaydedildi .*uyari/ortam, .*uyari/ortam_durdur, .*uyari/siren, .*uyari/siren_durdur", 0, 5,
                 "uyarı paketleri kayıtlı (ada göre sıralı kimlik)")
    sunucu.komut("dogalafet start deprem 1", r"Earthquake started \(Level 1 ")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Siren")
    sunucu.bekle(r"\[uyari\] SIREN aşaması: deprem afetNo -?\d+, 0 oyuncuya siren paketi gitti", isaret, c.ADIM_SURESI,
                 "uyarı SIREN aşamasını gördü (oyuncusuz: 0 alıcı)")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Active")
    sunucu.komut("dogalafet stop", r"Earthquake stopped\.")
    sunucu.komut("dogalafet start gunes_patlamasi", r"Solar Flare started \(Unleveled\)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Solar Flare phase: Siren")
    sunucu.bekle(r"\[uyari\] SIREN aşaması: gunes_patlamasi afetNo -?\d+, 0 oyuncuya siren paketi gitti", isaret,
                 c.ADIM_SURESI, "güneş sireni aşaması")
    sunucu.komut("dogalafet stop", r"Solar Flare stopped\.")


def istemcili_kosu(sunucu, klasor):
    sunucu.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    sonuc = {}
    istemci_log = klasor / "runClient-uyari-sunucu.log"
    istemci_log.unlink(missing_ok=True)

    def istemci():
        sonuc["gecerli"] = ui.kos(klasor, "uyari-sunucu", (f"-PquickPlaySunucu=localhost:{c.PORT}", f"-PkullaniciAdi={OYUNCU}"),
                                  ISTEMCI_WAV)

    is_parcacigi = threading.Thread(target=istemci, daemon=True)
    isaret = sunucu.isaret()
    is_parcacigi.start()
    sunucu.bekle(rf"{OYUNCU} joined the game", isaret, 1500, "istemci bağlandı")
    son = time.monotonic() + 300
    while "[uyari-otomasyon] ses: aygıt=" not in (istemci_log.read_text(encoding="utf-8", errors="replace")
                                                  if istemci_log.exists() else ""):
        if time.monotonic() > son or not is_parcacigi.is_alive():
            raise c.SenaryoHatasi("istemci sesi açmadı (aygıt denetimi log'u yok)")
        time.sleep(1)
    sunucu.yaz("### istemci sesi açtı (wave yazıcı)")
    sunucu.komut("dogalafet start deprem 1", r"Earthquake started \(Level 1 ")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Siren")
    sunucu.bekle(rf"\[uyari\] siren paketi gönderildi: deprem standart toplam 200 kalan 200 tick, afetNo -?\d+ -> "
                 rf"\[{OYUNCU}\]", isaret, c.ADIM_SURESI, "siren paketi oyuncuya gönderildi")
    time.sleep(6)
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Active")
    sunucu.bekle(rf"\[uyari\] siren durdurma paketi gönderildi: deprem fade-out 40 tick, afetNo -?\d+ -> \[{OYUNCU}\]",
                 isaret, c.ADIM_SURESI, "durdurma paketi oyuncuya gönderildi")
    sunucu.bekle(rf"{OYUNCU} left the game", isaret, 300, "istemci fade-out'tan sonra çıktı")
    sunucu.komut("dogalafet stop", r"Earthquake stopped\.")
    is_parcacigi.join(timeout=180)
    if not sonuc.get("gecerli"):
        raise c.SenaryoHatasi("istemci koşusu geçersiz (pencere-uyari.log)")


def ek_denetim(yol):
    metin = yol.read_text(encoding="utf-8")
    hatalar = []
    if re.search(r"Incorrect key uyari\b|dogalafetler-server\.toml is not correct", metin):
        hatalar.append("uyari config bölümü düzeltildi")
    if "client.uyari" in metin:
        hatalar.append("log'da client.uyari geçiyor (istemci sınıfı yüklenmiş olabilir)")
    return hatalar


def agir_surec_kapisi():
    for _ in range(10):
        if ui.gradle_sayisi() < 2 and ui.bos_ram_gib() >= 10 and not ui.baska_istemci() and c.port_bos(c.PORT):
            return True
        print(f"ağır süreç kapısı: gradle={ui.gradle_sayisi()} boş={ui.bos_ram_gib():.1f} GiB "
              f"istemci={ui.baska_istemci()} port {c.PORT} boş={c.port_bos(c.PORT)}; 60 sn", flush=True)
        time.sleep(60)
    return False


def main():
    a = c.argumanlar(__doc__, ek=lambda p: p.add_argument("--istemci", action="store_true", help="istemciyle bağlan"))
    klasor = a.kanit.resolve()
    istemcili = a.istemci
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    if istemcili and not agir_surec_kapisi():
        print("kapı 10 dk'da açılmadı; koşu iptal", file=sys.stderr)
        sys.exit(1)
    if not istemcili:
        c.kapi_veya_cik(c.PORT)
    c.senaryo_dunyasini_sil()
    ozgun = c.ozellikleri_senaryoya_cevir()
    sonuc = 0
    yol = klasor / ("runServer-uyari-istemcili.log" if istemcili else "runServer-uyari.log")
    try:
        sunucu = c.Sunucu("uyari", yol)
        try:
            istemcili_kosu(sunucu, klasor) if istemcili else kosu(sunucu)
        except c.SenaryoHatasi as hata:
            sunucu.yaz(f"!!! {hata}")
            print(hata, file=sys.stderr)
            sonuc = 1
        finally:
            kod = sunucu.durdur()
    finally:
        c.ozellikleri_geri_al(ozgun)
        c.senaryo_dunyasini_sil()
    for hata in c.log_denetimi(yol) + ek_denetim(yol):
        print(f"uyari: {hata}", file=sys.stderr)
        sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodu {kod}")
    if sonuc == 0 and kod != 0:
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
