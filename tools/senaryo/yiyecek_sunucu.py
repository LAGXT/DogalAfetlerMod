#!/usr/bin/env python3
"""Sıcaklık yiyeceklerinin runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Çalıştırma alt yapısı (Sunucu, server.properties geçişi, log denetimi) tools/senaryo/cekirdek_sunucu.py'den gelir; dünya
`run-server/yiyecek-senaryo` her koşuda sıfırdan üretilir, `test` dünyasına dokunulmaz (K0-18: pty'siz, stdin boruda).

Koşu: açılış; yeni config varsayılanlarla yazıldı (`yiyecek` bölümü dahil); açılış log'unda tarif sayısı satırı. Etiket
`#dogalafetler:pismis_kutup_baligi` boşken "20 ... yüklendi; koşullu atlanan 1 [kutup_baligi_corbasi]; ... 0 eşya" (F-17), balıkçılık
birleşip etiket dolunca "21 ... koşullu atlanan 0 []; ... N eşya" (N > 0); adetler: salep/çikolata/ayran/buzlu kakao/tarhana 3,
çay/elma çayı/elma suyu/dondurma/kabak çorbası 2, diğerleri 1; `dogalafet yiyecek bilgi`: yiyecek tablosu config'le kurulmuş
haliyle (salep orta 20 birim 90 sn, güveç çok büyük 45 birim 180 sn, sorbe çok büyük serinlik, tarhana büyük 35 birim 150 sn
...) ve vanilla listeleri (ısınma açık 13 yiyecek, serinlik açık 10 yiyecek);
`stop`. Sonda log denetlenir: `invalid dist` yok, istisna yok, config düzeltme WARN'ı yok, süreç 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/yiyecek_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı
(port ve kapı: cekirdek_sunucu.py, K-E13).
"""
import re
import signal
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

cs.SENARYO_DUNYASI = "yiyecek-senaryo"
cs.SENARYO_OZELLIKLERI = {"level-name": cs.SENARYO_DUNYASI, "level-seed": "20261106", "online-mode": "false"}

YIRMI = (r"\[atesli_guvec→1, ayran→3, buzlu_kakao→3, buzlu_karpuz→1, elma_cayi→2, firinda_elma→1, isitici_cay→2, "
         r"kabak_corbasi→2, kar_helvasi→1, kuzu_haslama→1, maras_dondurmasi→2, meyve_serbeti→1, parlak_sorbe→1, "
         r"sicak_cikolata→3, sicak_salep→3, sicak_tavuk_corbasi→1, soguk_elma_suyu→2, soguk_komposto→1, "
         r"sucuklu_yumurta→1, tarhana_corbasi→3\]")
YIRMI_BIR = (r"\[atesli_guvec→1, ayran→3, buzlu_kakao→3, buzlu_karpuz→1, elma_cayi→2, firinda_elma→1, isitici_cay→2, "
             r"kabak_corbasi→2, kar_helvasi→1, kutup_baligi_corbasi→1, kuzu_haslama→1, maras_dondurmasi→2, "
             r"meyve_serbeti→1, parlak_sorbe→1, sicak_cikolata→3, sicak_salep→3, sicak_tavuk_corbasi→1, "
             r"soguk_elma_suyu→2, soguk_komposto→1, sucuklu_yumurta→1, tarhana_corbasi→3\]")
ETIKET = r"#dogalafetler:pismis_kutup_baligi etiketinde "
TARIF_SATIRI = (r"Yiyecek: (20 sıcaklık yiyeceği tarifi yüklendi " + YIRMI + r"; koşullu atlanan 1 \[kutup_baligi_corbasi\]; "
                + ETIKET + r"0 eşya|21 sıcaklık yiyeceği tarifi yüklendi " + YIRMI_BIR + r"; koşullu atlanan 0 \[\]; " + ETIKET
                + r"[1-9][0-9]* eşya); vanillaIsinma=true \(13 yiyecek\), vanillaSerinlik=true \(10 yiyecek\)")
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
    (r"Maraş Ice Cream: Coolness \(strong\), -35 units over 120 s", "maraş dondurması"),
    (r"Cold Apple Juice: Coolness \(mild\), -8 units over 90 s", "soğuk elma suyu"),
    (r"Snow Halva: Coolness \(medium\), -20 units over 60 s", "kar helvası"),
    (r"Chilled Compote: Coolness \(medium\), -20 units over 120 s", "soğuk komposto"),
    (r"Glowing Sorbet: Coolness \(very strong\), -45 units over 150 s", "parıltılı sorbe"),
    (r"Iced Cocoa: Coolness \(medium\), -20 units over 90 s", "buzlu kakao"),
    (r"Tarhana Soup: Inner Warmth \(strong\), 35 units over 150 s", "tarhana çorbası"),
    (r"Lamb Broth: Inner Warmth \(medium\), 20 units over 150 s", "kuzu haşlama"),
    (r"Pumpkin Soup: Inner Warmth \(medium\), 20 units over 120 s", "bal kabağı çorbası"),
    (r"Sucuk and Eggs: Inner Warmth \(medium\), 20 units over 90 s", "sucuklu yumurta"),
    (r"Baked Apple: Inner Warmth \(mild\), 8 units over 90 s", "fırında elma"),
    (r"Apple Tea: Inner Warmth \(mild\), 8 units over 120 s", "elma çayı"),
    (r"Vanilla foods: warming on \(13 foods\), cooling on \(10 foods\)", "vanilla listeleri"),
)


def kosu(sunucu):
    sunucu.bekle(r"Done \(", 0, cs.ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Config dosyası varsayılanlarla oluşturuldu: .*dogalafetler-server\.toml", 0, 5,
                 "yeni dünyada config varsayılanlarla yazıldı")
    sunucu.bekle(TARIF_SATIRI, 0, 5, "tarif sayısı log'u (boş etikette çorba koşullu atlandı)")
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
    klasor = cs.argumanlar(__doc__).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
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
