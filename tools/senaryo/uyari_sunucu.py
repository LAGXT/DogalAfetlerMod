#!/usr/bin/env python3
"""Uyarı alt sisteminin dedicated server senaryosu, çekirdeğin `cekirdek_sunucu.py` altyapısıyla.

Sunucu Gradle ile pty'siz, stdin'i boruya bağlı açılır (K0-18); `-PoyunTestiAfeti=!deprem,!gunes_patlamasi` (K-E23: gerçek afetler
birleşse de taklit yerlerine geçer; siren sırası afetin süresinden bağımsız kalır). Kendi dünyası
(`run-server/cekirdek-senaryo`) silinip yeniden üretilir, `server.properties` sonda geri döner.

Oyuncusuz (varsayılan): start deprem 1, asama ileri (SIREN: uyarı log'u "SIREN aşaması ... 0 oyuncuya"; oyuncusuz sunucuda
çekirdek siren olayı yayınlamaz), asama ileri (AKTIF), stop; start gunes_patlamasi, asama ileri (SIREN), stop (siren
sürerken durdurma); sunucuya `stop`.

İstemcili koşu iki makineye bölünür (K-E46; ana makinede sunucu + istemci RAM kapısı ≥ 10 GiB açılmıyordu):
`--yalniz-sunucu [--dinle 0.0.0.0]`: dedicated sunucu (K-E44 gereği kendiliğinden pc2'de) `Uyari` adındaki oyuncunun
bağlanmasını bekler (en çok 25 dk); oyuncu katıldıktan 10 sn sonra (`GIRIS_PAYI_SN`) (istemci dünyaya girdikten 0,5 sn sonra sesi açar,
sireni 40 sn bekler) konsoldan start deprem 1 ve asama ileri (SIREN): sunucu log'unda "siren paketi gönderildi ... -> [Uyari]";
6 sn sonra asama ileri (AKTIF): "siren durdurma paketi gönderildi"; istemci fade-out'tan sonra çıkar ("left the game"), stop.
`--dinle` `server.properties` `server-ip` olur (vars. dokunulmaz: bütün arabirimler).
`--yalniz-istemci --sunucu-adres <makine:port>`: yerelde yalnız istemci; sunucu portuna TCP bağlantısı açılana dek bekler
(en çok 25 dk), sonra `uyari_istemci.kos` ile (K0-21 rev. 2 protokolü, ses yalnız wave yazıcıya, ağır süreç kapısı ≥ 6 GiB,
Gradle < 2, başka runClient yok) `--quickPlayMultiplayer <makine:port>` ve `Uyari` adıyla bağlanır (otomasyon
`uyari-sunucu`: komut vermez). İki komut aynı anda verilebilir:
  python3 tools/senaryo/uyari_sunucu.py --kanit <k>/sunucu --port 25612 --yalniz-sunucu --dinle 0.0.0.0
  python3 tools/senaryo/uyari_sunucu.py --kanit <k>/istemci --yalniz-istemci --sunucu-adres 192.168.1.105:25612
pc2'nin güvenlik duvarı (ufw) portu yerel ağa açmalı (docs/MAKINE.md pc2). Oyuncusuz koşuda ve sunucu tarafında ortak kapı
(cekirdek_sunucu.py, K-E13).

Log denetimi: `invalid dist` yok (ortak kod istemci sınıfı yüklemez, N15), istisna yok, config düzeltme WARN'ı yok,
`uyari/siren` paketleri kayıtlı.

Kullanım (worktree kökünden): python3 tools/senaryo/uyari_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
    [--yalniz-sunucu [--dinle <ip>] | --yalniz-istemci --sunucu-adres <makine:port>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, istemci geçersiz ya da sunucuya ulaşılamadı, 2 sunucu süreci sıfır dışı kodla
çıktı, 3 kapı açılmadı, 4 pc2 kapalı (uzak.sh).
"""
import re
import signal
import socket
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as c  # noqa: E402
import uyari_istemci as ui  # noqa: E402

OYUNCU = "Uyari"
ISTEMCI_WAV = Path("/tmp/uyari-ses-sunucu.wav")
GIRIS_PAYI_SN = 10
BAGLANTI_SURESI = 1500


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


def istemcili_kosu(sunucu):
    """`--yalniz-sunucu`: uzaktaki istemcinin (`--yalniz-istemci`) bağlanmasını bekler, sireni başlatır ve durdurur."""
    sunucu.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    sunucu.yaz(f"### istemci bekleniyor: port {c.PORT}, oyuncu {OYUNCU}")
    isaret = sunucu.isaret()
    sunucu.bekle(rf"{OYUNCU} joined the game", isaret, BAGLANTI_SURESI, "istemci bağlandı")
    time.sleep(GIRIS_PAYI_SN)
    sunucu.yaz(f"### girişten {GIRIS_PAYI_SN} sn sonra (istemci sesi açtı)")
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


def sunucuya_ulasilir(adres, sure):
    """`makine:port`'a TCP bağlantısı `sure` sn içinde açılırsa True (sunucu `Done` olmadan istemci bağlanmasın)."""
    makine, port = adres.rsplit(":", 1)
    son = time.monotonic() + sure
    while time.monotonic() < son:
        try:
            with socket.create_connection((makine, int(port)), timeout=5):
                return True
        except OSError:
            time.sleep(10)
    return False


def yalniz_istemci(klasor, adres):
    """`--yalniz-istemci`: sunucu portu açılınca yerelde istemciyi protokolle açar; koşu geçerli ve senaryo tamamsa 0."""
    print(f"sunucu bekleniyor: {adres} (en çok {BAGLANTI_SURESI // 60} dk)", flush=True)
    if not sunucuya_ulasilir(adres, BAGLANTI_SURESI):
        print(f"{adres} portuna ulaşılamadı (sunucu açılmadı ya da güvenlik duvarı)", file=sys.stderr)
        return 1
    gecerli = ui.kos(klasor, "uyari-sunucu", (f"-PquickPlaySunucu={adres}", f"-PkullaniciAdi={OYUNCU}"), ISTEMCI_WAV)
    print(f"istemci koşusu {'GEÇERLİ' if gecerli else 'GEÇERSİZ'} (pencere-uyari.log)")
    return 0 if gecerli else 1


def ek_denetim(yol):
    metin = yol.read_text(encoding="utf-8")
    hatalar = []
    if re.search(r"Incorrect key uyari\b|dogalafetler-server\.toml is not correct", metin):
        hatalar.append("uyari config bölümü düzeltildi")
    if "client.uyari" in metin:
        hatalar.append("log'da client.uyari geçiyor (istemci sınıfı yüklenmiş olabilir)")
    return hatalar


def main():
    def ek(p):
        kip = p.add_mutually_exclusive_group()
        kip.add_argument("--yalniz-sunucu", action="store_true", help="istemcili koşunun sunucu tarafı (pc2)")
        kip.add_argument("--yalniz-istemci", action="store_true", help="istemcili koşunun istemci tarafı (yerel)")
        p.add_argument("--sunucu-adres", help="--yalniz-istemci için <makine:port>")
        p.add_argument("--dinle", help="--yalniz-sunucu için server-ip (ör. 0.0.0.0)")

    a = c.argumanlar(__doc__, ek=ek)
    if a.yalniz_istemci != bool(a.sunucu_adres):
        sys.exit("--yalniz-istemci ile --sunucu-adres birlikte verilir")
    if a.dinle and not a.yalniz_sunucu:
        sys.exit("--dinle yalnız --yalniz-sunucu ile")
    klasor = a.kanit.resolve()
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    if a.yalniz_istemci:
        sys.exit(yalniz_istemci(klasor, a.sunucu_adres))
    istemcili = a.yalniz_sunucu
    if a.dinle:
        c.SENARYO_OZELLIKLERI["server-ip"] = a.dinle
    c.kapi_veya_cik(c.PORT)
    c.senaryo_dunyasini_sil()
    ozgun = c.ozellikleri_senaryoya_cevir()
    sonuc = 0
    yol = klasor / ("runServer-uyari-istemcili.log" if istemcili else "runServer-uyari.log")
    try:
        sunucu = c.Sunucu("uyari", yol, "!deprem,!gunes_patlamasi")
        try:
            istemcili_kosu(sunucu) if istemcili else kosu(sunucu)
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
