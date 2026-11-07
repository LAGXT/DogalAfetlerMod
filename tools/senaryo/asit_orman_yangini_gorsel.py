#!/usr/bin/env python3
"""Asit yağmuru ve orman yangınının görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k)).

Kullanım (worktree kökünden): python3 tools/senaryo/asit_orman_yangini_gorsel.py

Tek oyunculu Quick Play dünyası `run/saves/asit_orman` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=asit_orman` ile açılır ve `client.asit_yagmuru.AsitOrmanOtomasyonu` adımları yürütür: asit
yağmuru S1 aktif, S5 ön-uyarı (radar), siren, aktif, vitrin, bitiş; orman yangını aynı dizide (alacakaranlık).

Protokol: (a) açılış yalnız Hyprland exec kuralıyla ([workspace 93 silent; no_initial_focus; no_focus on;
focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere no_focus=true ve ws 93 doğrulanır,
tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç öldürülür, odak kaydedilen pencereye
verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur, sıfırdan farklıysa koşu geçersiz.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Aynı anda tek runClient (runclient_kilit.py kos); ağır süreç kapısı.
Sayılar ve adımlar KOK/.vibe/kanit/asit-orman-yangini/gorsel/protokol.log'a, kareler .../gorsel/kareler/'e.
"""
import hashlib
import json
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import runclient_kilit  # noqa: E402

WS = 93
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/asit-orman-yangini/gorsel"
KARELER = KANIT / "kareler"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
ISARET = "ASIT_ORMAN_OTOMASYON"
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
    hedef = run / "saves/asit_orman"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK / "run/saves/test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    yol = run / "options.txt"
    satirlar = yol.read_text().splitlines() if yol.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:", "renderDistance:",
                                                         "simulationDistance:", "enableVsync:", "maxFps:"))]
    # Orman yangını hedefin 40–90 blok uzağında: simülasyon 10 chunk. Gizli workspace'te vsync takası kareyi bekleyip FPS'i
    # 1'e düşürüyordu (koşu 2; kasırga K-13): vsync kapalı. Üst sınır 260: FPS tabanı 120 tavanında kalıyordu (AOi-3); otomasyon
    # açılışta aynı ayarı çalışma anında da verir (`GorselOtomasyon.cizimAyari`).
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10", "enableVsync:false", "maxFps:260"]) + "\n")
    for eski in (run / "screenshots").glob("ao_*.png"):
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
    betik = Path("/tmp/asit_orman_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 3000 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=asit_orman -Potomasyon=asit_orman "
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
    bekci = runclient_kilit.KapanisBekcisi(W, pid, (W / "run/logs/latest.log", istemci_log),
                                           f"{ISARET} kapanıyor; girdi sayıları", log)
    while not bekci.bitti_mi():
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
    for p in sorted((W / "run/screenshots").glob("ao_*.png")):
        hedef = KARELER / f"{p.stem}-{no}.png"
        shutil.copy2(p, hedef)
        log("kare:", hedef.name, "md5", hashlib.md5(p.read_bytes()).hexdigest()[:12])
    latest = W / "run/logs/latest.log"
    if latest.exists():
        shutil.copy2(latest, KANIT / f"latest-{no}.log")
        # K-E47 eki (P2-4): orman bitiş karesi yangın söndükten sonra yanık kamerasından; sönmeyip söndürülen koşu satırdan okunur.
        for satir in latest.read_text(errors="replace").splitlines():
            if any(k in satir for k in ("yangın söndü", "BITIS-KAPISI", "yanık kamerası")):
                log("bitiş:", satir.split(f"{ISARET} ", 1)[-1])
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
