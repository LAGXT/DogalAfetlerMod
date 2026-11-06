#!/usr/bin/env python3
"""Sanal suyun runServer senaryosu (oyuncusuz, dedicated), tekrar edilebilir.

Kendi dünyasını sıfırdan üretir: `run-server/su-senaryo` silinir, `run-server/server.properties` geçici olarak
`level-name=su-senaryo` yapılır ve senaryo bitince özgün içeriğine döner. Sunucu Gradle ile pty'siz, stdin'i boruya bağlı
açılır (K0-18).

Koşu 1: durum (deniz +0), denizseviyesi 3, durum (deniz +3), su bilgi, aysonrasi atla (ay var: reddedilir), denizseviyesi 8
(üst sınır 6'ya kırpılır), denizseviyesi 2 kademeli + asama ileri (hedefe ulaşır), denizseviyesi 3, `stop`.
Koşu 2 (aynı dünya): durum (deniz +3 korundu), sifirla, durum (deniz +0), `stop`.
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, config düzeltme uyarısı yok, iki süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/su_sunucu.py <log klasörü>
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı.
"""
import importlib.util
import signal
import sys
from pathlib import Path

_yol = Path(__file__).resolve().parent / "cekirdek_sunucu.py"
_spec = importlib.util.spec_from_file_location("cekirdek_sunucu", _yol)
c = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(c)

c.SENARYO_DUNYASI = "su-senaryo"
c.SENARYO_OZELLIKLERI = {"level-name": c.SENARYO_DUNYASI, "level-seed": "20261105", "online-mode": "false"}


class SuSunucu(c.Sunucu):
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
    s.komut("dogalafet durum", r"Sea level: \+0[.,]0+ blocks \(target \+0", "durum (deniz +0)")
    s.komut("dogalafet denizseviyesi 3", r"Sea level set to \+3\.00 blocks")
    s.komut("dogalafet durum", r"Sea level: \+3[.,]0+ blocks \(target \+3", "durum (deniz +3)")
    s.komut("dogalafet su bilgi", r"Virtual water \(minecraft:overworld\): sea \+3\.00", "su bilgi (deniz +3)")
    s.komut("dogalafet aysonrasi atla", r"The moon is still there", "atla: ay var, reddedilir")
    s.komut("dogalafet denizseviyesi 8", r"Sea level set to \+6\.00 blocks", "üst sınır 6")
    s.komut("dogalafet denizseviyesi 2 kademeli", r"Sea level is moving to \+2\.00 blocks")
    s.komut("dogalafet durum", r"Running era processes: .*sea level rise", "durum (deniz süreci sürüyor)")
    s.komut("dogalafet asama ileri", r"Sea level rise: reached \+2\.00 blocks")
    s.komut("dogalafet denizseviyesi 3", r"Sea level set to \+3\.00 blocks")
    s.komut("dogalafet durum", r"Sea level: \+3[.,]0+ blocks \(target \+3", "durum (deniz +3, kapanış öncesi)")


def kosu2(s):
    s.bekle(r"Done \(", 0, c.ACILIS_SURESI, "açılış")
    s.komut("dogalafet durum", r"Sea level: \+3[.,]0+ blocks \(target \+3", "durum (deniz +3 yeniden başlatmada korundu)")
    s.komut("dogalafet sifirla", r"disaster and era state was reset")
    s.komut("dogalafet durum", r"Sea level: \+0[.,]0+ blocks \(target \+0", "durum (sıfırlandı)")


def main():
    klasor = Path(sys.argv[1])
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    c.senaryo_dunyasini_sil()
    ozgun = c.ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        for ad, adimlar in (("kosu1", kosu1), ("kosu2", kosu2)):
            sunucu = SuSunucu(ad, klasor / f"{ad}.log")
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
            for hata in c.log_denetimi(yol):
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(kod != 0 for kod in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
