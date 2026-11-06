#!/usr/bin/env python3
"""Sıcaklık yiyeceklerinin runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Çalıştırma alt yapısı (Sunucu, server.properties geçişi, log denetimi) tools/senaryo/cekirdek_sunucu.py'den gelir; dünya
`run-server/yiyecek-senaryo` her koşuda sıfırdan üretilir, `test` dünyasına dokunulmaz (K0-18: pty'siz, stdin boruda).

Koşu: açılış; yeni config varsayılanlarla yazıldı (`yiyecek` bölümü dahil); açılış log'unda "Yiyecek: 9 sıcaklık yiyeceği
tarifi yüklendi" (dokuz tarif ve adetleri: salep/çikolata/ayran 3, çay 2, diğerleri 1; Kutup Balığı Çorbası etiketli);
`dogalafet yiyecek bilgi`: madde 14 tablosu config'le kurulmuş haliyle (salep orta 20 birim 90 sn, güveç çok büyük 45 birim
180 sn, ayran serinlik orta, kutup balığı çorbası büyük 35 birim 150 sn) ve vanilla listesi (açık, 11 yiyecek, 3 birim);
`stop`. Sonda log denetlenir: `invalid dist` yok, istisna yok, config düzeltme WARN'ı yok, süreç 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/yiyecek_sunucu.py <log klasörü>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı.
"""
import re
import signal
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "yiyecek-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

TARIFLER = (r"\[atesli_guvec→1, ayran→3, buzlu_karpuz→1, isitici_cay→2, kutup_baligi_corbasi→1, meyve_serbeti→1, "
            r"sicak_cikolata→3, sicak_salep→3, sicak_tavuk_corbasi→1\]")
TABLO = (
    (r"Hot Salep: Inner Warmth \(medium\), 20 units over 90 s", "salep"),
    (r"Hot Chocolate: Inner Warmth \(medium\), 20 units over 90 s", "çikolata"),
    (r"Hot Chicken Soup: Inner Warmth \(strong\), 35 units over 150 s", "tavuk çorbası"),
    (r"Fiery Stew: Inner Warmth \(very strong\), 45 units over 180 s", "ateşli güveç"),
    (r"Warming Tea: Inner Warmth \(mild\), 8 units over 180 s", "ısıtıcı çay"),
    (r"Ayran: Coolness \(medium\), -20 units over 90 s", "ayran"),
    (r"Iced Melon Slice: Coolness \(mild\), -8 units over 60 s", "buzlu karpuz"),
    (r"Fruit Sherbet: Coolness \(strong\), -35 units over 120 s", "meyve şerbeti"),
    (r"Polar Fish Soup: Inner Warmth \(strong\), 35 units over 150 s", "kutup balığı çorbası"),
    (r"Vanilla cooked foods: on \(11 foods, 3 units each\)", "vanilla listesi"),
)


def kosu(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Config dosyası varsayılanlarla oluşturuldu: .*dogalafetler-server\.toml", 0, 5,
                 "yeni dünyada config varsayılanlarla yazıldı")
    sunucu.bekle(r"Yiyecek: 9 sıcaklık yiyeceği tarifi yüklendi " + TARIFLER
                 + r"; #dogalafetler:pismis_kutup_baligi etiketinde [0-9]+ eşya; vanillaIsinma=true \(11 yiyecek\)", 0, 5,
                 "dokuz tarif yüklendi (tarif sayısı log'u)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet yiyecek bilgi", r"Temperature foods \(instant share 25%, same food again within 60 s: x0\.5\):",
                 "tablo başlığı")
    for desen, aciklama in TABLO:
        sunucu.bekle(desen, isaret, cs.ADIM_SURESI, aciklama)


def log_denetimi(yol):
    hatalar = cs.log_denetimi(yol)
    metin = yol.read_text(encoding="utf-8")
    yiyecek = [s for s in metin.splitlines() if re.search(r"Incorrect key yiyecek\b", s)]
    if yiyecek:
        hatalar.append(f"{len(yiyecek)} yiyecek config düzeltme satırı, ilki: {yiyecek[0]}")
    return hatalar


def main():
    klasor = Path(sys.argv[1])
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.senaryo_dunyasini_sil()
    ozgun = cs.ozellikleri_senaryoya_cevir()
    sonuc = 0
    yol = klasor / "kosu.log"
    try:
        sunucu = cs.Sunucu("kosu", yol)
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
    for hata in log_denetimi(yol):
        print(f"kosu: {hata}", file=sys.stderr)
        sonuc = 1
    metin = yol.read_text(encoding="utf-8")
    print(f"senaryo sonucu {sonuc}, süreç kodu {kod}, invalid dist {metin.lower().count('invalid dist')}")
    if sonuc == 0 and kod != 0:
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
