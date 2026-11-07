#!/usr/bin/env python3
"""Meteor ve termostatın görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k)).

Kullanım (worktree kökünden): python3 tools/senaryo/meteor_gorsel.py [hepsi|odul|yagmur] [kanıt klasörü]
(`odul`: yalnız ödül ve ipucu kareleri, `-PotomasyonBolum=odul`, M-22; `yagmur`: güneş patlaması ön-uyarı/siren yağmuru, yakın/uzak
ses denemesi, ay parçası yağmuru (meteor-2); argümansız: bütün vitrin. Kanıt klasörü KOK'a göredir.)

Tek oyunculu Quick Play dünyası `run/saves/meteor` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=meteor` (K-E3) ile açılır ve `client.meteor.MeteorOtomasyonu`
adımları yürütür (meteor izleri + çarpma, krater + ateş, parıldayan ödüller, ipuçları).

Protokol: (a) açılış yalnız Hyprland exec kuralıyla ([workspace 92 silent; no_initial_focus; no_focus on;
focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere no_focus=true ve ws 92 doğrulanır,
tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç öldürülür, odak kaydedilen pencereye
verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur, sıfırdan farklıysa koşu geçersiz.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Aynı anda tek runClient; ağır süreç kapısı (en çok 20 dk).
Sayılar ve adımlar <kanıt>/protokol.log'a, kareler <kanıt>/kareler/'e (vars. KOK/.vibe/kanit/meteor-termostat).
"""
import hashlib
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import defter  # noqa: E402
import runclient_kilit  # noqa: E402

WS = 92
BOLUM = sys.argv[1] if len(sys.argv) > 1 else "hepsi"
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / (sys.argv[2] if len(sys.argv) > 2 else ".vibe/kanit/meteor-termostat")
KARELER = KANIT / "kareler"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
ISARET = "METEOR_OTOMASYON"
KANIT.mkdir(parents=True, exist_ok=True)
KARELER.mkdir(exist_ok=True)
GUNLUK = open(KANIT / "protokol.log", "a", encoding="utf-8")


def log(*a):
    satir = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
    print(satir, flush=True)
    GUNLUK.write(satir + "\n")
    GUNLUK.flush()


def kapi():
    for _ in range(21):
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
    hedef = run / "saves/meteor"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK / "run/saves/test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    yol = run / "options.txt"
    satirlar = yol.read_text().splitlines() if yol.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:", "renderDistance:",
                                                         "simulationDistance:"))]
    # Meteor başlangıcı hedefin ~100 blok yanında: simülasyon 10 chunk.
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10"]) + "\n")
    for eski in (run / "screenshots").glob("meteor_*.png"):
        eski.unlink()
    latest = run / "logs/latest.log"
    if latest.exists():
        latest.unlink()


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
        log("kapı 20 dk'da açılmadı")
        return 2
    hazirla()
    istemci_log = KANIT / f"runClient-{no}.log"
    betik = Path("/tmp/meteor_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 3000 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=meteor -Potomasyon=meteor "
                     f"-PotomasyonBolum={BOLUM} "
                     f"> '{istemci_log}' 2>&1\n")
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
    for p in sorted((W / "run/screenshots").glob("meteor_*.png")):
        hedef = KARELER / f"{p.stem}-{no}.png"
        shutil.copy2(p, hedef)
        log("kare:", hedef.name, "md5", hashlib.md5(p.read_bytes()).hexdigest()[:12])
    latest = W / "run/logs/latest.log"
    if latest.exists():
        shutil.copy2(latest, KANIT / f"latest-{no}.log")
    sayim = girdi_sayilari(istemci_log)
    if sayim is None:
        log(f"koşu {no} GEÇERSİZ: girdi sayıları satırı yok")
        return 1
    kaynak, satir_no, sayilar = sayim
    sifir = sayilar == "tuş=0 fare=0 tekerlek=0"
    sifir = runclient_kilit.girdi_ozeti(sifir, f"koşu {no} girdi sayıları: {sayilar} ({kaynak.name}:{satir_no})", log)
    return 0 if sifir and not alarm else 1


if __name__ == "__main__":
    sys.exit(defter.sarmala(main, KANIT))
