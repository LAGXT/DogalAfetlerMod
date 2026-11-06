#!/usr/bin/env python3
"""Başarımların runServer senaryosu (oyuncusuz, dedicated; K0-18), tekrar edilebilir.

Çalıştırma alt yapısı (Sunucu, server.properties geçişi, log denetimi) tools/senaryo/cekirdek_sunucu.py'den gelir; dünya
`run-server/basarim-senaryo` her koşuda sıfırdan üretilir, `test` dünyasına dokunulmaz (pty'siz, stdin boruda). Sunucu
`-PoyunTestiAfeti=!deprem` ile açılır (yaşam döngüsü sınama afeti, core.OyunTestiAfeti; K-E23: gerçek deprem
birleşse de taklit onun yerine geçer, üç `asama ileri` ile doğal bitiş korunur).

Koşu: açılışta vanilla `Loaded N advancements` satırı; `dogalafet basarim bilgi` → sekmenin 27/27 başarımı yüklü;
`forceload add 0 0` + `loot spawn 0 100 0 loot dogalafetler:rewards/hedef` → hedef ödül tablosu 1–2 eşya düşürür;
`dogalafet unvan Kimse` → oyuncu yok (komut kayıtlı, OP konsolu); `dogalafet start deprem 5` + `asama ileri` x3 → doğal
bitiş, başarım dinleyicisi oyuncusuz olayda hata vermez; `dogalafet sifirla` → başarımın K-E11 döngü işareti (A-27) hatasız
yazılır; `dogalafet basarim bilgi` yeniden. Sonda log denetimi: `invalid
dist` yok, istisna yok, advancement/loot/etiket yükleme hatası yok, config düzeltme WARN'ı yok, yeni config'te `[basarim]`
ve `[basarim.odul]` bölümleri var, süreç 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/basarim_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
(varsayılan port 25595)
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı
(port ve kapı: cekirdek_sunucu.py, K-E13).
"""
import re
import signal
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "basarim-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}
VARSAYILAN_PORT = 25595

BASARIM_SAYISI = 27
YUKLEME_HATALARI = re.compile(
    r"(Couldn't load advancement|Parsing error loading custom advancement|Couldn't parse element|"
    r"Couldn't load tag|Failed to load .*dogalafetler).*dogalafetler|dogalafetler.*(Couldn't load|Parsing error)")


def kosu(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    satir = sunucu.bekle(r"Loaded \d+ advancements", 0, 5, "vanilla advancement yükleme satırı")
    sunucu.yaz(f"### açılış: {satir.strip()}")
    sunucu.komut("dogalafet basarim bilgi", rf"Natural Disasters tab: {BASARIM_SAYISI}/{BASARIM_SAYISI} advancements "
                 r"loaded", "sekmenin 27 başarımı yüklü")
    sunucu.komut("forceload add 0 0", r"Marked chunk|is already marked")
    sunucu.komut("loot spawn 0 100 0 loot dogalafetler:rewards/hedef", r"Dropped [12] ", "hedef ödülü 1-2 sıcak yiyecek")
    sunucu.komut("dogalafet unvan Kimse", r"No player was found", "unvan komutu kayıtlı (oyuncu yok)")
    sunucu.komut("dogalafet start deprem 5", r"Earthquake started \(Level 5 ")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Siren")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Active")
    isaret = sunucu.isaret()
    # BİTİŞ sönümü sınama afetinde hemen biter (bitisTamam varsayılanı): dördüncü "asama ileri" gönderilmez.
    sunucu.komut("dogalafet asama ileri", r"Earthquake (phase: Ending|has ended)")
    sunucu.bekle(r"Afet bitti: deprem seviye 5 neden DOGAL", isaret, 10, "doğal bitiş olayı (oyuncusuz)")
    sunucu.komut("dogalafet basarim bilgi", rf"Natural Disasters tab: {BASARIM_SAYISI}/{BASARIM_SAYISI}",
                 "bitiş sonrası sekme")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset", "sifirla (K-E11 döngü işareti)")
    sunucu.komut("dogalafet basarim bilgi", rf"Natural Disasters tab: {BASARIM_SAYISI}/{BASARIM_SAYISI}",
                 "sıfırlama sonrası sekme")


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    yukleme = [s for s in metin.splitlines() if YUKLEME_HATALARI.search(s)]
    if yukleme:
        hatalar.append(f"{len(yukleme)} veri yükleme hatası, ilki: {yukleme[0]}")
    duzeltme = [s for s in metin.splitlines() if re.search(r"Incorrect key basarim\b", s)]
    if duzeltme:
        hatalar.append(f"{len(duzeltme)} basarim config düzeltme satırı, ilki: {duzeltme[0]}")
    return hatalar


def config_denetimi():
    yol = cs.SUNUCU_KLASORU / cs.SENARYO_DUNYASI / "serverconfig" / "dogalafetler-server.toml"
    if not yol.exists():
        return [f"config yok: {yol}"]
    metin = yol.read_text(encoding="utf-8")
    eksik = [b for b in ("[basarim]", "[basarim.odul]", "etkiAlaniPayi = 0", "unvanGoster = true", "meydanOkumaXp = 100")
             if b not in metin]
    return [f"config'te eksik: {', '.join(eksik)}"] if eksik else []


def main():
    klasor = cs.argumanlar(__doc__, VARSAYILAN_PORT).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    eula = cs.SUNUCU_KLASORU / "eula.txt"
    if not eula.exists():
        eula.write_text("eula=true\n", encoding="utf-8")
    sonuc = 0
    kod = None
    yol = klasor / "kosu.log"
    try:
        sunucu = cs.Sunucu("kosu", yol, "!deprem")
        try:
            kosu(sunucu)
        except cs.SenaryoHatasi as hata:
            sunucu.yaz(f"!!! {hata}")
            print(hata, file=sys.stderr)
            sonuc = 1
        finally:
            kod = sunucu.durdur()
    finally:
        cs.ozellikleri_geri_al(ozgun)
    for hata in log_denetimi(yol) + config_denetimi():
        print(f"kosu: {hata}", file=sys.stderr)
        sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodu {kod}")
    if sonuc == 0 and kod != 0:
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
