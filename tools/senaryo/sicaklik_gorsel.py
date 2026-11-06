"""Sıcaklık sistemi görsel kanıtı ve dedicated sunucu kabulü (K0-18, K0-21 rev. 2).

Kullanım (worktree kökünden):
  python3 tools/senaryo/sicaklik_gorsel.py hud         # runServer + runClient (A), HUD/efekt kareleri, set/bilgi konsolda
  python3 tools/senaryo/sicaklik_gorsel.py coldsweat   # runClient -Puyum=coldsweat, Quick Play tek oyunculu dünya

Ekran protokolü: istemci yalnız Hyprland exec kuralıyla açılır ([workspace 92 silent; no_initial_focus; no_focus on;
focus_on_activate off]); JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; pencere no_focus=true ve ws 92 doğrulanır; 5 sn'de bir
bekçi etkin pencere bizimse ALARM verip süreci öldürür. Sunucu pty'siz, stdin boruya bağlı (K0-18). Sahibin oturumunda
tıklama/odak/workspace değişimi yok.
"""
import json
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

WS = 92
PORT = 25592
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/sicaklik"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
KANIT.mkdir(parents=True, exist_ok=True)
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
    """Ağır süreç kapısı: Gradle sayısı < 2 ve boş RAM >= 4 GB, tek runClient (en çok 10 dk)."""
    for _ in range(11):
        gradle = int(subprocess.run(["pgrep", "-fc", "GradleWrapperMain"], capture_output=True, text=True).stdout
                     .strip() or 0)
        istemci = subprocess.run(["pgrep", "-fa", "runClient"], capture_output=True, text=True).stdout.strip()
        bos = int(open("/proc/meminfo").read().split("MemAvailable:")[1].split()[0]) // 1024
        log(f"kapı: gradle={gradle} runClient={'var' if istemci else 'yok'} bos={bos} MB")
        if gradle < 2 and not istemci and bos >= 4096:
            return True
        time.sleep(60)
    return False


def ortam():
    e = dict(os.environ)
    e["JAVA_HOME"] = JAVA_HOME
    return e


def sunucu_hazirla():
    k = W / "run-server"
    k.mkdir(exist_ok=True)
    (k / "eula.txt").write_text("eula=true\n")
    ozellik = k / "server.properties"
    satirlar = ozellik.read_text().splitlines() if ozellik.exists() else []
    yonetilen = {"server-port": str(PORT), "online-mode": "false", "level-name": "test",
                 "level-seed": "20261105", "spawn-protection": "0", "enable-command-block": "false"}
    satirlar = [s for s in satirlar if s.split("=")[0] not in yonetilen]
    satirlar += [f"{a}={d}" for a, d in yonetilen.items()]
    ozellik.write_text("\n".join(satirlar) + "\n")


class Sunucu:
    def __init__(self):
        self.log_yolu = KANIT / "runServer.log"
        self.dosya = open(self.log_yolu, "w", encoding="utf-8")
        self.p = subprocess.Popen(["timeout", "1500", "./gradlew", "runServer", "--offline", "--console=plain",
                                   "--no-daemon"], cwd=W, env=ortam(), stdin=subprocess.PIPE, stdout=self.dosya,
                                  stderr=subprocess.STDOUT, text=True, start_new_session=True)

    def metin(self):
        return self.log_yolu.read_text(encoding="utf-8", errors="replace")

    def bekle(self, aranan, sure):
        son = time.time() + sure
        while time.time() < son:
            if aranan in self.metin():
                return True
            if self.p.poll() is not None:
                return False
            time.sleep(1)
        return False

    def komut(self, k):
        log("konsol >", k)
        self.p.stdin.write(k + "\n")
        self.p.stdin.flush()

    def durdur(self):
        if self.p.poll() is None:
            try:
                self.komut("stop")
                self.p.wait(120)
            except (subprocess.TimeoutExpired, BrokenPipeError):
                os.killpg(self.p.pid, signal.SIGTERM)
        log("sunucu çıkış", self.p.poll())


def istemci_pid():
    """Kendi run/ klasöründe çalışan Minecraft istemci JVM'i."""
    r = subprocess.run(["pgrep", "-f", "forgeclientuserdev"], capture_output=True, text=True).stdout.split()
    for pid in r:
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


def istemci_calistir(kip, ek_ozellikler, bitince=None, her_turda=None):
    """İstemciyi protokolle açar; otomasyon 'bitti' logunu yazınca bitince bir kez çağrılır."""
    betik = Path(f"/tmp/sicaklik_istemci_{kip}.sh")
    istemci_log = KANIT / f"runClient-{kip}.log"
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"export JAVA_TOOL_OPTIONS='-Ddogalafetler.otomasyon={'sicaklik' if kip == 'hud' else 'sicaklik_coldsweat'}'\n"
                     f"exec timeout 900 ./gradlew runClient --offline --no-daemon --console=plain {ek_ozellikler} "
                     f"> '{istemci_log}' 2>&1\n")
    sahip = hypr_json("activewindow")
    sahip_adres = sahip.get("address") if isinstance(sahip, dict) else None
    log("sahibin etkin penceresi:", sahip_adres, sahip.get("class") if isinstance(sahip, dict) else None)
    kural = f"[workspace {WS} silent; no_initial_focus; no_focus on; focus_on_activate off] sh {betik}"
    log("exec:", hypr("dispatch", f'hl.dsp.exec_cmd("{kural}")'))
    baslangic = time.time()
    pid = None
    while time.time() - baslangic < 300 and pid is None:
        pid = istemci_pid()
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i 300 sn'de görünmedi")
        return False
    if not belirtec_var(pid):
        oldur(pid, "HL_EXEC_RULE_TOKEN yok (belirteç kapısı)")
        return False
    log(f"istemci pid={pid}, belirteç var")
    dogrulandi = False
    alarm = False
    bitti_cagrildi = False
    while True:
        if not Path(f"/proc/{pid}").exists():
            break
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
                return False
            dogrulandi = True
        etkin = hypr_json("activewindow")
        if isinstance(etkin, dict) and etkin.get("pid") == pid:
            alarm = True
            log("ALARM: etkin pencere bizim istemci")
            oldur(pid, "odak alarmı")
            if sahip_adres:
                log("odak geri:", hypr("dispatch", f'hl.dsp.focus({{ window = "address:{sahip_adres}" }})'))
            break
        if her_turda:
            her_turda()
        if bitince and not bitti_cagrildi and "SICAKLIK_OTOMASYON bitti" in istemci_log.read_text(errors="replace"):
            bitti_cagrildi = True
            bitince()
        time.sleep(5)
    log(f"istemci bitti ({time.time() - baslangic:.0f} sn), alarm={alarm}")
    return not alarm


def kareleri_tasi():
    for p in sorted((W / "run/screenshots").glob("sicaklik_*.png")):
        shutil.copy2(p, KANIT / p.name)
        log("kare:", p.name)


def main():
    kip = sys.argv[1] if len(sys.argv) > 1 else "hud"
    for eski in (W / "run/screenshots").glob("sicaklik_*.png"):
        eski.unlink()
    if not kapi():
        log("kapı 10 dk'da açılmadı")
        return 2
    if kip == "hud":
        sunucu_hazirla()
        sunucu = Sunucu()
        if not sunucu.bekle("Done (", 600):
            log("sunucu açılmadı")
            sunucu.durdur()
            return 1
        # op oyuncu girdikten sonra: offline modda girmemiş oyuncunun profili bulunamaz ("Made a a server operator").
        for k in ("gamerule sendCommandFeedback false", "time set 6000", "weather clear 6000",
                  "gamerule doDaylightCycle false", "gamerule doWeatherCycle false"):
            sunucu.komut(k)
        if not kapi():
            sunucu.durdur()
            return 2
        def konsol():
            sunucu.komut("dogalafet sicaklik set A 10")
            time.sleep(2)
            sunucu.komut("dogalafet sicaklik bilgi A")

        op_verildi = []

        def op_ver():
            if not op_verildi and "A joined the game" in sunucu.metin():
                op_verildi.append(True)
                sunucu.komut("op A")

        ok = istemci_calistir("hud", f"-PkullaniciAdi=A -PquickPlaySunucu=localhost:{PORT}", konsol, op_ver)
        time.sleep(3)
        sunucu.durdur()
        metin = sunucu.metin()
        log("sunucu log: invalid dist =", metin.count("invalid dist"), "| Exception =", metin.count("Exception"))
    else:
        ok = istemci_calistir("coldsweat", "-Puyum=coldsweat -PquickPlay=test")
    kareleri_tasi()
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
