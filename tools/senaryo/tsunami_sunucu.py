#!/usr/bin/env python3
"""Tsunaminin runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Kendi dünyasını sıfırdan üretir: `run-server/tsunami-senaryo` silinir, `server.properties` geçici olarak senaryo dünyasına
ve `--port`'a (vars. 25644, workspace 94) çevrilir, sonda özgün içeriğine döner (K-E13).

Koşu 1: dünya doğuşunun üstünde (y 200..208) sahne: batıda duvarlı deniz (29x39 sütun, 3 derin), doğuda toprak kara,
ahşap kulübe + cam + yaprak, taş yarda dalgaya açılan mağara (doğuşun batısı gerçek kıyıdır: deniz aynı yönde). Yüklü
olmayan uzak konumda `start tsunami` → yer uygun değil (N14: yüklü olmayan chunk deniz sayılmaz).
Sahnede `start tsunami 3`, `asama ileri` x2 → çekilme ve dalga, kıyıya vurur, mağara dolar, `su bilgi`, `stop` → dalga,
çekilme ve mağara kalmaz, yıkılan blok > 0. Sonra `start tsunami 1` AKTIF'e ilerletilir ve sunucu kapatılır (kalıcılık).
Koşu 2 (aynı dünya): afet kayıttan yüklenir, dalga aynı kimlikle sürüyor, kendiliğinden biter (doğal bitiş), `su bilgi`
(dalga 0), `sifirla`, `su bilgi` (dalga 0, mağara 0).
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, config düzeltme uyarısı yok, `[basarim] ... yayıncı olarak
kayıtlı değil` yok, iki süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/tsunami_sunucu.py --port 25644 --kanit <dizin>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı.
"""
import importlib.util
import signal
import sys
from pathlib import Path

_yol = Path(__file__).resolve().parent / "cekirdek_sunucu.py"
_spec = importlib.util.spec_from_file_location("cekirdek_sunucu", _yol)
c = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(c)

c.SENARYO_DUNYASI = "tsunami-senaryo"
c.SENARYO_OZELLIKLERI = {"level-name": c.SENARYO_DUNYASI, "level-seed": "20261105", "online-mode": "false"}
MERKEZ = "~10 204 ~"

SAHNE = [
    "fill ~-30 200 ~-20 ~20 200 ~20 stone",
    "fill ~-30 201 ~-20 ~-30 203 ~20 stone",
    "fill ~-29 201 ~-20 ~-1 203 ~-20 stone",
    "fill ~-29 201 ~20 ~-1 203 ~20 stone",
    "fill ~-29 201 ~-19 ~-1 203 ~19 water",
    "fill ~0 201 ~-20 ~20 203 ~20 dirt",
    "fill ~8 204 ~-2 ~12 207 ~2 oak_planks hollow",
    "setblock ~10 205 ~-2 glass",
    "setblock ~10 205 ~2 glass",
    "fill ~4 204 ~6 ~5 205 ~7 oak_leaves[persistent=true]",
    "fill ~14 204 ~-9 ~20 209 ~-3 stone",
    "fill ~16 204 ~-8 ~19 206 ~-4 air",
    "fill ~14 206 ~-6 ~15 207 ~-6 air",
]


class TsunamiSunucu(c.Sunucu):
    """Çekirdeğin `Sunucu`'su; oyun testi afeti olmadan açılır."""

    def __init__(self, ad, log_yolu):
        import os
        import subprocess
        import threading
        import time
        self.ad = ad
        self.satirlar = []
        self.kilit = threading.Condition()
        self.baslangic = time.monotonic()
        self.log = log_yolu.open("w", encoding="utf-8")
        ortam = dict(os.environ, JAVA_HOME=c.JAVA_HOME)
        self.surec = subprocess.Popen(
            ["timeout", "1500", "./gradlew", "runServer", "--console=plain", "--offline"], cwd=c.KOK, env=ortam,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8",
            errors="replace", bufsize=1, start_new_session=True)
        threading.Thread(target=self._oku, daemon=True).start()


def kosu1(s):
    s.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    s.komut("execute positioned ~3000 100 ~3000 run dogalafet start tsunami 3", r"No suitable place found for Tsunami",
            "denizsiz yerde başlamaz (N14)")
    for komut in SAHNE:
        s.komut(komut, r"Successfully filled \d+ block|Changed the block", komut)
    s.komut(f"execute positioned {MERKEZ} run dogalafet start tsunami 3", r"Tsunami started", "start tsunami 3")
    s.komut("dogalafet asama ileri", r"Tsunami phase: Siren")
    isaret = s.isaret()
    s.komut("dogalafet asama ileri", r"Tsunami phase: Active")
    s.bekle(r"Tsunami: çekilme #\d+ .*dalga #\d+", isaret, 30, "çekilme ve dalga")
    s.bekle(r"Tsunami: dalga kıyıya vurdu", isaret, 90, "dalga kıyıya vurdu")
    s.bekle(r"Tsunami: mağara #\d+ dolduruldu", isaret, 60, "mağara doldu")
    s.komut("dogalafet su bilgi", r"waves [12], caves [1-9]", "su bilgi (dalga ve mağara var)")
    s.komut("dogalafet durum", r"Tsunami", "durum (tsunami aktif)")
    isaret = s.isaret()
    s.komut("dogalafet stop", r"Tsunami stopped\.")
    s.bekle(r"Tsunami: temizlendi \(temiz=true, doğal=false\): kırılan [1-9]\d* blok.*boyutta kalan dalga 0, çekilme 0, "
            r"mağara 0", isaret, 10, "stop temiz, yıkım > 0")
    s.komut("dogalafet su bilgi", r"waves 0, caves 0", "su bilgi (stop sonrası boş)")
    # Kalıcılık: AKTIF'teki S1 tsunamisi kayıtla kapanır, koşu 2'de aynı dalga kimliğiyle sürer.
    s.komut(f"execute positioned {MERKEZ} run dogalafet start tsunami 1", r"Tsunami started", "start tsunami 1")
    s.komut("dogalafet asama ileri", r"Tsunami phase: Siren")
    isaret = s.isaret()
    s.komut("dogalafet asama ileri", r"Tsunami phase: Active")
    s.bekle(r"Tsunami: çekilme #\d+ .*dalga #\d+", isaret, 30, "S1 çekilme ve dalga")


def kosu2(s):
    s.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    s.bekle(r"Tsunami: kayıttan yüklendi, dalga #\d+ sürüyor=true", 0, 30, "afet kayıttan, dalga aynı kimlikle")
    s.komut("dogalafet durum", r"Tsunami", "durum (tsunami sürüyor)")
    s.bekle(r"Tsunami: temizlendi \(temiz=true, doğal=true\).*boyutta kalan dalga 0, çekilme 0", 0, 300,
            "doğal bitiş")
    s.komut("dogalafet su bilgi", r"waves 0,", "su bilgi (dalga kalmadı)")
    s.komut("dogalafet sifirla", r"disaster and era state was reset")
    s.komut("dogalafet su bilgi", r"waves 0, caves 0", "su bilgi (sıfırla sonrası boş)")


def main():
    klasor = c.argumanlar(__doc__, varsayilan_port=25644).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    c.kapi_veya_cik(c.PORT)
    c.senaryo_dunyasini_sil()
    ozgun = c.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        for ad, adimlar in (("kosu1", kosu1), ("kosu2", kosu2)):
            sunucu = TsunamiSunucu(ad, klasor / f"{ad}.log")
            try:
                adimlar(sunucu)
            except c.SenaryoHatasi as hata:
                sunucu.yaz(f"!!! {hata}")
                print(hata, file=sys.stderr)
                sonuc = 1
            finally:
                kodlar.append(sunucu.durdur())
            if sonuc:
                break
    finally:
        c.ozellikleri_geri_al(ozgun)
    for ad in ("kosu1", "kosu2"):
        yol = klasor / f"{ad}.log"
        if yol.exists():
            hatalar = c.log_denetimi(yol)
            if "yayıncı olarak kayıtlı değil" in yol.read_text(encoding="utf-8"):
                hatalar.append("[basarim] yayıncı olarak kayıtlı değil satırı var")
            for hata in hatalar:
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(kod != 0 for kod in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
