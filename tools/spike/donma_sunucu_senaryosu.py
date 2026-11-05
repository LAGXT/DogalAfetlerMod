#!/usr/bin/env python3
"""Donma spike'ının runServer senaryosu (oyuncusuz).

Sunucuyu pty'siz, stdin'i boruya bağlı açar (docs/MAKINE.md, KARARLAR K0-18), komutları stdin'den yazar ve her adımı
log satırına bakarak, süre sınırıyla bekler. Senaryo:
  1. G=60; spawn chunk'ları hedefe yetişir, /forge tps ve bilgi çıktısı.
  2. Uzak chunk (2000, 2000) izlenir, forceload add -> 0'dan 60'a yetişir -> forceload remove (seviye NBT'ye yazılır).
  3. G=100; spawn yetişir; forceload add -> kayıtlı seviye 60 okunur, 60'tan 100'e adım adım yetişir.
  4. Yapay yükle TPS düşürülür -> bütçe 1/2 ve 1/4; yük kalkınca 1/1.
  5. Çözülme dönemi + kalan G=40 için kış maskesi çıktıları; stop.

Kullanım (worktree kökünden): python3 tools/spike/donma_sunucu_senaryosu.py <log yolu>
"""
import os
import re
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
TOPLAM_SURE = 1500
UZAK = (2000, 2000)

satirlar = []
kilit = threading.Condition()
baslangic = time.monotonic()


def gecen():
    return time.monotonic() - baslangic


def main():
    log_yolu = Path(sys.argv[1])
    log_yolu.parent.mkdir(parents=True, exist_ok=True)
    log = log_yolu.open("w", encoding="utf-8")

    def yaz_log(metin):
        log.write(f"[+{gecen():7.1f}s] {metin}\n")
        log.flush()

    ortam = dict(os.environ, JAVA_HOME=JAVA_HOME)
    surec = subprocess.Popen(
        ["./gradlew", "runServer", "--console=plain"], cwd=KOK, env=ortam, stdin=subprocess.PIPE,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", bufsize=1,
        start_new_session=True)

    def okuyucu():
        for satir in surec.stdout:
            satir = satir.rstrip("\n")
            yaz_log(satir)
            with kilit:
                satirlar.append((gecen(), satir))
                kilit.notify_all()

    threading.Thread(target=okuyucu, daemon=True).start()

    def bekci():
        time.sleep(TOPLAM_SURE)
        if surec.poll() is None:
            yaz_log("!!! toplam süre aşıldı, süreç grubu SIGTERM")
            os.killpg(surec.pid, signal.SIGTERM)

    threading.Thread(target=bekci, daemon=True).start()

    def komut(metin):
        yaz_log(f">>> {metin}")
        surec.stdin.write(metin + "\n")
        surec.stdin.flush()

    def bekle(desen, sure, baslangic_indeksi=0):
        """İndeksten sonra desene uyan ilk satırın indeksini döndürür; süre dolarsa hata."""
        rx = re.compile(desen)
        son = time.monotonic() + sure
        i = baslangic_indeksi
        with kilit:
            while True:
                while i < len(satirlar):
                    if rx.search(satirlar[i][1]):
                        return i
                    i += 1
                kalan = son - time.monotonic()
                if kalan <= 0 or surec.poll() is not None:
                    raise TimeoutError(f"beklenen satır gelmedi ({sure}s): {desen}")
                kilit.wait(min(kalan, 1.0))

    def indeks():
        with kilit:
            return len(satirlar)

    def sakinlesmeyi_bekle(baslangic_indeksi, sessiz=15, en_cok=420, tps_araligi=20):
        """Özet satırları (100 tick'te bir, yalnız iş varken) {sessiz} saniye kesilene kadar bekler; arada /forge tps."""
        rx = re.compile(r"\[donma\] özet:")
        son = time.monotonic() + en_cok
        son_tps = time.monotonic()
        goruldu = False
        son_ozet = time.monotonic()
        i = baslangic_indeksi
        while time.monotonic() < son:
            with kilit:
                kilit.wait(1.0)
                while i < len(satirlar):
                    if rx.search(satirlar[i][1]):
                        goruldu = True
                        son_ozet = time.monotonic()
                    i += 1
            if time.monotonic() - son_tps >= tps_araligi:
                komut("forge tps")
                komut("dogalafet spike donma bilgi")
                son_tps = time.monotonic()
            if goruldu and time.monotonic() - son_ozet >= sessiz:
                return
            if surec.poll() is not None:
                raise RuntimeError("sunucu beklenmedik şekilde kapandı")
        raise TimeoutError("özet satırları kesilmedi")

    sonuc = 1
    try:
        bekle(r"Done \(", 900)
        yaz_log("### 1. başlangıç durumu")
        komut("dogalafet spike donma bilgi")
        komut("forge tps")
        komut("dogalafet spike donma kis")

        yaz_log("### 2. G=60, spawn chunk'ları yetişiyor")
        i = indeks()
        komut("dogalafet spike donma 60")
        sakinlesmeyi_bekle(i)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")

        yaz_log("### 3. uzak chunk: forceload add -> 0'dan 60'a")
        x, z = UZAK
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma izle")
        i = indeks()
        komut(f"forceload add {x} {z}")
        bekle(r"izlenen chunk .* yüklendi", 180, i)
        bekle(r"izlenen chunk .* seviye \d+ → 60 \(hedef 60\)", 240, i)
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma bilgi")
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma kis")

        yaz_log("### 4. forceload remove -> chunk boşaltılır, seviye 60 kaydedilir")
        i = indeks()
        komut(f"forceload remove {x} {z}")
        bekle(r"izlenen chunk .* boşaltıldı", 180, i)
        komut("save-all flush")
        bekle(r"Saved the game", 120, i)

        yaz_log("### 5. G=100, spawn chunk'ları 60'tan 100'e")
        i = indeks()
        komut("dogalafet spike donma 100")
        sakinlesmeyi_bekle(i)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")

        yaz_log("### 6. uzak chunk'a dönüş: forceload add -> kayıtlı 60'tan 100'e adım adım")
        i = indeks()
        komut(f"forceload add {x} {z}")
        bekle(r"izlenen chunk .* yüklendi \(yeni=false\): kayıtlı seviye 60", 180, i)
        bekle(r"izlenen chunk .* seviye \d+ → 100 \(hedef 100\)", 240, i)
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma bilgi")
        komut("forge tps")

        yaz_log("### 7. TPS kısması: yapay yük")
        i = indeks()
        komut("dogalafet spike donma yapayyuk 58")
        bekle(r"\[donma\] TPS .* bütçe 1/2", 60, i)
        komut("dogalafet spike donma 80")
        time.sleep(8)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")
        i = indeks()
        komut("dogalafet spike donma yapayyuk 80")
        bekle(r"\[donma\] TPS .* bütçe 1/4", 60, i)
        time.sleep(8)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")
        i = indeks()
        komut("dogalafet spike donma yapayyuk 0")
        bekle(r"\[donma\] TPS .* bütçe 1/1", 60, i)
        sakinlesmeyi_bekle(i)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")

        yaz_log("### 8. çözülme dönemi ve kış maskesi")
        komut("dogalafet spike donma cozulme true")
        komut("dogalafet spike donma 40")
        for kx, kz in [(0, 0), (x, z), (-3000, 1500), (4000, -2500), (8000, 8000)]:
            komut(f"execute positioned {kx} 64 {kz} run dogalafet spike donma kis")
        time.sleep(10)
        komut("dogalafet spike donma bilgi")
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma bilgi")
        komut("forge tps")
        time.sleep(3)
        sonuc = 0
    except Exception as hata:  # senaryo hatası log'a yazılır, sunucu yine temiz kapatılır
        yaz_log(f"!!! senaryo hatası: {hata}")
    finally:
        if surec.poll() is None:
            komut("stop")
            try:
                surec.wait(timeout=180)
            except subprocess.TimeoutExpired:
                yaz_log("!!! stop sonrası kapanmadı, SIGTERM")
                os.killpg(surec.pid, signal.SIGTERM)
                surec.wait(timeout=60)
        time.sleep(1)
        yaz_log(f"### süreç çıkış kodu {surec.returncode}, senaryo sonucu {sonuc}")
        log.close()
    sys.exit(sonuc if surec.returncode == 0 else 2)


if __name__ == "__main__":
    main()
