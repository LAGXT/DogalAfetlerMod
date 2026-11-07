#!/usr/bin/env python3
"""Deprem ve sinkhole görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k); bekleyen Faz 4 görsel kabulü).

Kullanım (worktree kökünden): python3 tools/senaryo/deprem_gorsel.py [--bolum deprem_s5,sinkhole_s5] [--kanit DİZİN]

`--bolum` otomasyona `-PotomasyonBolum` olarak gider (`hepsi` ya da virgülle `deprem_s1`, `deprem_s5`, `sinkhole_s1`,
`sinkhole_s5`); `--kanit` kanıt dizinini değiştirir (duman koşusu, 5-matris tekrarları). Ortak `OLCUM` satırları koşu sonunda
`olcum_tablosu.py` ile `<kanıt>/olcum-<n>.tsv`'ye yazılır.

Tek oyunculu Quick Play dünyası `run/saves/deprem` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=deprem` ile açılır ve `client.deprem.DepremOtomasyonu` adımları yürütür: deprem S1 (Hassas
Sismograf ön-uyarısı, siren, sarsıntı, bitiş), deprem S5 (vitrin, S1 ile aynı göreli bakış, geniş bakış, artçılar, bitiş),
sinkhole S1 (çatlama, çukur) ve sinkhole S5 (çatlama, çöküş vitrini, üstten çukur, bitiş). Her kare FPS ile log'a düşer;
kareler.txt kare/FPS tablosudur. Tepe TPS dedicated senaryodan gelir (`deprem_sunucu.py`, `<kanıt>/tps.txt`). Vsync kapalı,
`maxFps` 260 (gizli workspace'te vsync takası istemciyi 1 FPS'e kısar, K-13; FPS tabanı tavana yapışmaz, AOi-3).

Protokol: (a) açılış yalnız Hyprland exec kuralıyla ([workspace 92 silent; no_initial_focus; no_focus on;
focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere no_focus=true ve ws 92 doğrulanır,
tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç öldürülür, odak kaydedilen pencereye
verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur, sıfırdan farklıysa koşu geçersiz.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Aynı anda tek runClient (`runclient_kilit.py kos`; kilit satırları
protokole kopyalanır); ağır süreç kapısı (en çok 60 dk: altı Faz 4 işçisi aynı anda Gradle/runClient koşuyor). Sayılar ve adımlar
KOK/.vibe/kanit/deprem-sinkhole/gorsel/protokol.log'a, kareler KOK/.vibe/kanit/deprem-sinkhole/gorsel/kareler/'e.
"""
import argparse
import hashlib
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import olcum_tablosu  # noqa: E402
import runclient_kilit  # noqa: E402

_ap = argparse.ArgumentParser(description="Deprem ve sinkhole görsel kanıtı (K0-21 rev. 2).")
_ap.add_argument("--bolum", default="hepsi", help="hepsi ya da virgülle deprem_s1,deprem_s5,sinkhole_s1,sinkhole_s5")
_ap.add_argument("--kanit", type=Path, help="kanıt dizini (varsayılan KOK/.vibe/kanit/deprem-sinkhole/gorsel)")
ARG = _ap.parse_args()

WS = 92
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = ARG.kanit.resolve() if ARG.kanit else KOK / ".vibe/kanit/deprem-sinkhole/gorsel"
KARELER = KANIT / "kareler"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
ISARET = "DEPREM_OTOMASYON"
KANIT.mkdir(parents=True, exist_ok=True)
KARELER.mkdir(exist_ok=True)
GUNLUK = open(KANIT / "protokol.log", "a", encoding="utf-8")


def log(*a):
    satir = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
    print(satir, flush=True)
    GUNLUK.write(satir + "\n")
    GUNLUK.flush()


def kapi():
    for _ in range(61):
        gradle = int(subprocess.run(["pgrep", "-fc", "GradleWrapperMain"], capture_output=True, text=True).stdout
                     .strip() or 0)
        istemci = subprocess.run(["pgrep", "-fa", "runClient"], capture_output=True, text=True).stdout.strip()
        bos = int(open("/proc/meminfo").read().split("MemAvailable:")[1].split()[0]) // 1024
        log(f"kapı: gradle={gradle} runClient={'var' if istemci else 'yok'} bos={bos} MB")
        if gradle < 2 and not istemci and bos >= 4096:
            return True
        time.sleep(60)
    return False


def istemci_pid():
    for pid in subprocess.run(["pgrep", "-f", "forgeclientuserdev"], capture_output=True, text=True).stdout.split():
        try:
            if os.readlink(f"/proc/{pid}/cwd") == str(W / "run"):
                return int(pid)
        except OSError:
            continue
    return None


def oldur(pid, neden):
    log("istemci öldürülüyor:", neden)
    try:
        os.kill(pid, signal.SIGKILL)
    except OSError:
        pass


def kosu_no():
    var = [int(p.stem.rsplit("-", 1)[1]) for p in KANIT.glob("runClient-*.log") if p.stem.rsplit("-", 1)[1].isdigit()]
    return max(var, default=0) + 1


def hazirla():
    run = W / "run"
    (run / "saves").mkdir(parents=True, exist_ok=True)
    hedef = run / "saves/deprem"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK / "run/saves/test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    yol = run / "options.txt"
    satirlar = yol.read_text().splitlines() if yol.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:", "renderDistance:",
                                                         "simulationDistance:", "enableVsync:", "maxFps:"))]
    # S5 yarığı merkezden 128 blok: görüş 12 chunk (192 blok) kameradan uçları kapsar.
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10", "enableVsync:false", "maxFps:260"]) + "\n")
    for eski in (run / "screenshots").glob("ds_*.png"):
        eski.unlink()
    latest = run / "logs/latest.log"
    if latest.exists():
        latest.unlink()


def kare_tablosu(latest, no):
    """Otomasyonun kare satırlarından kare/FPS tablosu (kareler-<no>.txt) ve en düşük FPS."""
    satirlar = []
    for satir in latest.read_text(errors="replace").splitlines():
        m = re.search(rf"{ISARET} kare (\S+) \(FPS (\d+), (.*)\)$", satir)
        if m:
            satirlar.append((m.group(1), int(m.group(2)), m.group(3)))
    metin = [f"{ad}: FPS {fps}; {ayrinti}" for ad, fps, ayrinti in satirlar]
    if satirlar:
        metin.append(f"{len(satirlar)} kare, en düşük FPS {min(f for _, f, _ in satirlar)}")
    (KANIT / f"kareler-{no}.txt").write_text("\n".join(metin) + "\n", encoding="utf-8")
    for s in metin:
        log("kare/FPS:", s)


def girdi_sayilari(istemci_log):
    for kaynak in (W / "run/logs/latest.log", istemci_log):
        if kaynak.exists():
            for no, satir in enumerate(kaynak.read_text(errors="replace").splitlines(), 1):
                if f"{ISARET} kapanıyor; girdi sayıları" in satir:
                    return kaynak, no, satir.split("girdi sayıları: ", 1)[1].strip()
    return None


def main():
    no = kosu_no()
    if not kapi():
        log("kapı 60 dk'da açılmadı")
        return 2
    hazirla()
    istemci_log = KANIT / f"runClient-{no}.log"
    betik = Path("/tmp/deprem_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 3000 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=deprem -Potomasyon=deprem "
                     f"-PotomasyonBolum={ARG.bolum} > '{istemci_log}' 2>&1\n")
    sahip_adres = runclient_kilit.istemci_baslat(betik, WS, log)
    baslangic = time.time()
    pid = None
    while time.time() - baslangic < 300 and pid is None:
        pid = istemci_pid()
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i 300 sn'de görünmedi")
        return 1
    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        oldur(pid, neden)
        return 1
    alarm = False
    yoklama = 0
    bekci = runclient_kilit.KapanisBekcisi(W, pid, (W / "run/logs/latest.log", istemci_log),
                                           f"{ISARET} kapanıyor; girdi sayıları", log)
    while not bekci.bitti_mi():
        yoklama += 1
        if runclient_kilit.odak_denetle(pid, sahip_adres, log, lambda neden: oldur(pid, neden)):
            alarm = True
            break
        time.sleep(5)
    log(f"istemci bitti ({time.time() - baslangic:.0f} sn), bekçi yoklaması={yoklama}, alarm={alarm}")
    for p in sorted((W / "run/screenshots").glob("ds_*.png")):
        hedef = KARELER / f"{p.stem}-{no}.png"
        shutil.copy2(p, hedef)
        log("kare:", hedef.name, "md5", hashlib.md5(p.read_bytes()).hexdigest()[:12])
    latest = W / "run/logs/latest.log"
    if latest.exists():
        shutil.copy2(latest, KANIT / f"latest-{no}.log")
        kare_tablosu(latest, no)
        satirlar, ozet = olcum_tablosu.tablo([KANIT / f"latest-{no}.log"])
        (KANIT / f"olcum-{no}.tsv").write_text("\n".join(satirlar) + "\n", encoding="utf-8")
        log(f"OLCUM tablosu: {len(satirlar) - 1} satır → olcum-{no}.tsv", *ozet)
    if istemci_log.exists():
        for satir in istemci_log.read_text(errors="replace").splitlines():
            if re.search(r"runClient kilidi|kilit sahibi|kilit başkasının|^kos: ", satir):
                log("kilit:", satir.strip())
    sayim = girdi_sayilari(istemci_log)
    if sayim is None:
        log(f"koşu {no} GEÇERSİZ: girdi sayıları satırı yok")
        return 1
    kaynak, satir_no, sayilar = sayim
    sifir = sayilar == "tuş=0 fare=0 tekerlek=0"
    sifir = runclient_kilit.girdi_ozeti(sifir, f"koşu {no} girdi sayıları: {sayilar} ({kaynak.name}:{satir_no})", log)
    return 0 if sifir and not alarm else 1


if __name__ == "__main__":
    sys.exit(main())
