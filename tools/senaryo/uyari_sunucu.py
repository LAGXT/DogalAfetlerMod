#!/usr/bin/env python3
"""Uyarı alt sisteminin dedicated server senaryosu (oyuncusuz), çekirdeğin `cekirdek_sunucu.py` altyapısıyla.

Sunucu Gradle ile pty'siz, stdin'i boruya bağlı açılır (K0-18); `-PoyunTestiAfeti=deprem,gunes_patlamasi`. Kendi dünyası
(`run-server/cekirdek-senaryo`) silinip yeniden üretilir, `server.properties` sonda geri döner.

Adımlar: start deprem 1, asama ileri (SIREN: uyarı log'u "SIREN aşaması ... 0 oyuncuya"; oyuncusuz sunucuda çekirdek siren
olayı yayınlamaz), asama ileri (AKTIF), stop; start gunes_patlamasi, asama ileri (SIREN), stop (siren sürerken durdurma);
sunucuya `stop`. Log denetimi: `invalid dist` yok (ortak kod istemci sınıfı yüklemez, N15), istisna yok, config düzeltme
WARN'ı yok, `uyari/siren` paketleri kayıtlı.

Kullanım (worktree kökünden): python3 tools/senaryo/uyari_sunucu.py <log klasörü>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı.
"""
import re
import signal
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as c  # noqa: E402


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


def ek_denetim(yol):
    metin = yol.read_text(encoding="utf-8")
    hatalar = []
    if re.search(r"Incorrect key uyari\b|dogalafetler-server\.toml is not correct", metin):
        hatalar.append("uyari config bölümü düzeltildi")
    if "client.uyari" in metin:
        hatalar.append("log'da client.uyari geçiyor (istemci sınıfı yüklenmiş olabilir)")
    return hatalar


def main():
    klasor = Path(sys.argv[1])
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    c.senaryo_dunyasini_sil()
    ozgun = c.ozellikleri_senaryoya_cevir()
    sonuc = 0
    yol = klasor / "runServer-uyari.log"
    try:
        sunucu = c.Sunucu("uyari", yol)
        try:
            kosu(sunucu)
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
