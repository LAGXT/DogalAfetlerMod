#!/usr/bin/env python3
"""Deprem ve sinkhole görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k); bekleyen Faz 4 görsel kabulü).

Kullanım (worktree kökünden): python3 tools/senaryo/deprem_gorsel.py

Tek oyunculu Quick Play dünyası `run/saves/deprem` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=deprem` ile açılır ve `client.deprem.DepremOtomasyonu` adımları yürütür: deprem S1 (Hassas
Sismograf ön-uyarısı, siren, sarsıntı, bitiş), deprem S5 (vitrin, S1 ile aynı göreli bakış, geniş bakış, artçılar, bitiş),
sinkhole S1 (çatlama, çukur) ve sinkhole S5 (çatlama, çöküş vitrini, üstten çukur, bitiş). Her kare FPS ile log'a düşer;
kareler.txt kare/FPS tablosudur. Tepe TPS dedicated senaryodan gelir (`deprem_sunucu.py`, `<kanıt>/tps.txt`).

Protokol: (a) açılış yalnız Hyprland exec kuralıyla ([workspace 92 silent; no_initial_focus; no_focus on;
focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere no_focus=true ve ws 92 doğrulanır,
tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç öldürülür, odak kaydedilen pencereye
verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur, sıfırdan farklıysa koşu geçersiz.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Aynı anda tek runClient (`runclient_kilit.py kos`; kilit satırları
protokole kopyalanır); ağır süreç kapısı (en çok 60 dk: altı Faz 4 işçisi aynı anda Gradle/runClient koşuyor). Sayılar ve adımlar
KOK/.vibe/kanit/deprem-sinkhole/gorsel/protokol.log'a, kareler KOK/.vibe/kanit/deprem-sinkhole/gorsel/kareler/'e.
"""
import hashlib
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

WS = 92
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/deprem-sinkhole/gorsel"
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


def hypr(*a):
    r = subprocess.run(["hyprctl", *a], capture_output=True, text=True, timeout=10)
    return r.stdout.strip()


def hypr_json(*a):
    try:
        return json.loads(hypr(*a, "-j"))
    except (json.JSONDecodeError, subprocess.TimeoutExpired):
        return None


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


def belirtec_var(pid):
    try:
        return b"HL_EXEC_RULE_TOKEN=" in Path(f"/proc/{pid}/environ").read_bytes()
    except OSError:
        return False


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
                                                         "simulationDistance:"))]
    # S5 yarığı merkezden 128 blok: görüş 12 chunk (192 blok) kameradan uçları kapsar.
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10"]) + "\n")
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
                     f"> '{istemci_log}' 2>&1\n")
    sahip = hypr_json("activewindow")
    sahip_adres = sahip.get("address") if isinstance(sahip, dict) else None
    log(f"koşu {no}: sahibin etkin penceresi {sahip_adres} ({sahip.get('class') if isinstance(sahip, dict) else None})")
    kural = f"[workspace {WS} silent; no_initial_focus; no_focus on; focus_on_activate off] sh {betik}"
    log("exec:", hypr("dispatch", f'hl.dsp.exec_cmd("{kural}")'))
    baslangic = time.time()
    pid = None
    while time.time() - baslangic < 300 and pid is None:
        pid = istemci_pid()
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i 300 sn'de görünmedi")
        return 1
    if not belirtec_var(pid):
        oldur(pid, "HL_EXEC_RULE_TOKEN yok (belirteç kapısı)")
        return 1
    log(f"istemci pid={pid}, belirteç var")
    dogrulandi = False
    alarm = False
    yoklama = 0
    while Path(f"/proc/{pid}").exists():
        pencereler = [c for c in (hypr_json("clients") or []) if c.get("pid") == pid]
        if pencereler and not dogrulandi:
            for ozellik, deger in (("no_focus", "1"), ("focus_on_activate", "0")):
                hypr("dispatch", f'hl.dsp.window.set_prop({{ window = "pid:{pid}", prop = "{ozellik}", '
                                 f'value = "{deger}", lock = true }})')
            nf = hypr("getprop", f"pid:{pid}", "no_focus")
            ws = [c.get("workspace", {}).get("id") for c in pencereler]
            log(f"pencere doğrulaması: no_focus={nf!r} ws={ws}")
            if "true" not in nf.lower() or any(w != WS for w in ws):
                oldur(pid, "pencere doğrulanamadı")
                return 1
            dogrulandi = True
        etkin = hypr_json("activewindow")
        yoklama += 1
        if isinstance(etkin, dict) and etkin.get("pid") == pid:
            alarm = True
            log("ALARM: etkin pencere bizim istemci")
            oldur(pid, "odak alarmı")
            if sahip_adres:
                log("odak geri:", hypr("dispatch", f'hl.dsp.focus({{ window = "address:{sahip_adres}" }})'))
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
    log(f"koşu {no} girdi sayıları: {sayilar} ({kaynak.name}:{satir_no})" + ("" if sifir else " → GEÇERSİZ"))
    return 0 if sifir and not alarm else 1


if __name__ == "__main__":
    sys.exit(main())
