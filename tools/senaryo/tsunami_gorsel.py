#!/usr/bin/env python3
"""Tsunami görsel kanıt koşusu (S5/S1 vitrin, mağara suyu, FPS/TPS): K0-21 rev. 2 ekran protokolü (docs/KARARLAR.md).

(a) İstemci yalnız Hyprland exec kuralıyla açılır: `[workspace 94 silent; no_initial_focus; no_focus on; focus_on_activate off]`.
(b) Belirteç kapısı: istemci JVM'i (cwd = worktree `run/`) görününce ortamında `HL_EXEC_RULE_TOKEN` aranır; yoksa öldürülür.
(c) Pencere doğrulaması: pid'in penceresine `set_prop no_focus=1`, `focus_on_activate=0` (lock) yazılır; `getprop no_focus`
    true ve workspace 94 değilse öldürülür.
(d) Bekçi: sahibin etkin penceresi kaydedilir; 5 sn'de bir `hyprctl activewindow -j`; etkin pencere bizim pid'imizse ALARM:
    süreç öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz.
(e) Oyun içi girdi günlüğü (`client.tsunami.TsunamiOtomasyonu`): `TSUNAMI-OTOMASYON GIRDI` satırları sayılır; sıfırdan
    farklıysa geçersiz.
(i) Ağır süreç kapısı ve aynı anda tek runClient.

Kullanım (worktree kökünden): python3 tools/senaryo/tsunami_gorsel.py <kanıt klasörü> [bölüm: hepsi|s5|s1|magara]
Çıkış: 0 geçerli koşu, 1 protokol ihlali/iptal, 2 istemci hatası.
"""
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
RUN = KOK / "run"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
WS = 94
KURAL = f"[workspace {WS} silent; no_initial_focus; no_focus on; focus_on_activate off]"


def sh(komut, zaman=30):
    r = subprocess.run(komut, shell=isinstance(komut, str), capture_output=True, text=True, timeout=zaman)
    return (r.stdout + r.stderr).strip()


def hypr_json(ne):
    try:
        return json.loads(sh(["hyprctl", ne, "-j"]))
    except (json.JSONDecodeError, subprocess.TimeoutExpired):
        return None


class Kayit:
    def __init__(self, yol):
        self.f = yol.open("a", encoding="utf-8")

    def __call__(self, metin):
        satir = f"{time.strftime('%H:%M:%S')} {metin}"
        print(satir, flush=True)
        self.f.write(satir + "\n")
        self.f.flush()


def kapi(log):
    for _ in range(31):
        istemci = [s for s in sh("pgrep -fa runClient").splitlines() if s.strip() and "pgrep" not in s]
        gradle = int(sh("pgrep -c -f GradleWrapperMain") or 0)
        bos = int(next(s for s in sh("free -g").splitlines() if s.startswith("Mem")).split()[6])
        if not istemci and gradle < 2 and bos >= 4:
            log(f"kapı açık: gradle={gradle} boş={bos} GB, runClient yok")
            return True
        log(f"bekleme: gradle={gradle} boş={bos} GB runClient={len(istemci)}")
        time.sleep(60)
    return False


def istemci_pid():
    for p in Path("/proc").iterdir():
        if not p.name.isdigit():
            continue
        try:
            if os.readlink(p / "cwd") == str(RUN) and (p / "comm").read_text().strip() == "java":
                return int(p.name)
        except OSError:
            continue
    return None


def belirtec_var(pid):
    try:
        return b"HL_EXEC_RULE_TOKEN=" in (Path("/proc") / str(pid) / "environ").read_bytes()
    except OSError:
        return False


def pencere(pid):
    for c in hypr_json("clients") or []:
        if c.get("pid") == pid and c.get("mapped", True):
            return c
    return None


def oldur(gradle):
    try:
        os.killpg(gradle.pid, signal.SIGTERM)
    except ProcessLookupError:
        pass
    pid = istemci_pid()
    if pid:
        try:
            os.kill(pid, signal.SIGKILL)
        except ProcessLookupError:
            pass


def odak_ver(adres, log):
    for ifade in (f'hl.dsp.focus({{ window = "address:{adres}" }})', f'hl.dsp.window.focus({{ window = "address:{adres}" }})'):
        log(f"odak geri: {ifade} -> {sh(['hyprctl', 'dispatch', ifade])}")


def main():
    kanit = Path(sys.argv[1]).resolve()
    bolum = sys.argv[2] if len(sys.argv) > 2 else "hepsi"
    kanit.mkdir(parents=True, exist_ok=True)
    log = Kayit(kanit / "pencere.log")
    if not kapi(log):
        log("kapı 30 dk açılmadı; koşu yapılmadı")
        return 1
    # Her koşu ana test dünyasının temiz kopyasıyla başlar (vitrin sahnesi gerçek blok kurar; sahne belirlenimci kalır).
    hedef = RUN / "saves" / "test"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK.parents[1] / "run" / "saves" / "test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    (hedef / "serverconfig" / "dogalafetler-server.toml").unlink(missing_ok=True)
    log(f"test dünyası temiz kopyalandı: {hedef}")
    sahip = hypr_json("activewindow") or {}
    sahip_adres = sahip.get("address")
    log(f"sahibin etkin penceresi: {sahip_adres} {sahip.get('class')} ws={sahip.get('workspace', {}).get('id')}")
    oyun_log = kanit / f"runClient-{bolum}.log"
    betik = Path("/tmp/tsunami-istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{KOK}' || exit 9\n"
                     f"JAVA_HOME='{JAVA_HOME}' python3 tools/senaryo/runclient_kilit.py kos '{KOK}' --bekle 1800 -- "
                     f"timeout 1200 ./gradlew runClient --no-daemon --offline --console=plain "
                     f"-PquickPlay=test -Potomasyon=tsunami -PotomasyonBolum={bolum} > '{oyun_log}' 2>&1\n"
                     f"echo \"cikis=$?\" >> '{oyun_log}'\n")
    ifade = f'hl.dsp.exec_cmd("{KURAL} sh {betik}")'
    duvar_baslangic = time.time()
    log(f"exec kuralıyla başlatılıyor: {ifade} -> {sh(['hyprctl', 'dispatch', ifade])}")
    baslangic = time.monotonic()

    class Grup:
        pid = None

    gradle = Grup()
    pid = None
    while time.monotonic() - baslangic < 600:
        pid = istemci_pid()
        if pid:
            break
        time.sleep(0.5)
    if not pid:
        log("istemci JVM'i 10 dk'da görünmedi")
        return 2
    try:
        gradle.pid = os.getpgid(pid)
    except ProcessLookupError:
        gradle.pid = pid
    if not belirtec_var(pid):
        log(f"ALARM: pid={pid} ortamında HL_EXEC_RULE_TOKEN yok; pencere eşlenmeden öldürülüyor")
        os.kill(pid, signal.SIGKILL)
        return 1
    log(f"istemci JVM pid={pid} ({time.monotonic() - baslangic:.1f} sn); HL_EXEC_RULE_TOKEN ortamda: True")
    p = None
    while time.monotonic() - baslangic < 900:
        p = pencere(pid)
        if p:
            break
        if istemci_pid() is None:
            log("istemci pencere açmadan çıktı")
            return 2
        time.sleep(0.25)
    if not p:
        log("pencere 15 dk'da eşlenmedi")
        os.kill(pid, signal.SIGKILL)
        return 2
    sonuclar = []
    for prop, deger in (("no_focus", "1"), ("focus_on_activate", "0")):
        ifade = f'hl.dsp.window.set_prop({{ window = "pid:{pid}", prop = "{prop}", value = "{deger}", lock = true }})'
        sonuclar.append(sh(["hyprctl", "dispatch", ifade]))
    no_focus = sh(["hyprctl", "getprop", f"pid:{pid}", "no_focus"])
    p = pencere(pid) or p
    ws = p.get("workspace", {}).get("id")
    etkin = (hypr_json("activewindow") or {}).get("address")
    log(f"pencerem {p.get('address')} pid={pid} ws={ws}; set_prop -> {sonuclar}; getprop no_focus={no_focus}; etkin={etkin}")
    if "true" not in no_focus.lower() or ws != WS:
        log("ALARM: pencere doğrulaması tutmadı; süreç öldürülüyor")
        os.kill(pid, signal.SIGKILL)
        return 1
    yoklama = 0
    alarm = 0
    while istemci_pid() == pid:
        time.sleep(5)
        yoklama += 1
        a = hypr_json("activewindow") or {}
        if a.get("pid") == pid:
            alarm += 1
            log(f"ALARM: etkin pencere benim istemcim ({a.get('address')}); süreç öldürülüyor, koşu geçersiz")
            os.kill(pid, signal.SIGKILL)
            if sahip_adres:
                odak_ver(sahip_adres, log)
            break
        if yoklama % 6 == 0:
            p = pencere(pid) or {}
            log(f"denetim: etkin={a.get('address')} {a.get('class')}; benim ws={p.get('workspace', {}).get('id')}")
    son = time.monotonic() + 120
    while time.monotonic() < son and "cikis=" not in oyun_log.read_text(encoding="utf-8", errors="replace"):
        time.sleep(2)
    metin = oyun_log.read_text(encoding="utf-8", errors="replace")
    girdi = len(re.findall(r"TSUNAMI-OTOMASYON GIRDI", metin))
    bitti = re.search(r"TSUNAMI-OTOMASYON BITTI girdi tus=(\d+) fare=(\d+) teker=(\d+)", metin)
    cikis = re.search(r"cikis=(\d+)", metin)
    log(f"bitti: yoklama={yoklama} ALARM={alarm} girdi_satiri={girdi} "
        f"otomasyon_ozeti={bitti.groups() if bitti else None} gradle_{cikis.group(0) if cikis else 'cikis=?'}")
    desen = r".*(?:TSUNAMI-OTOMASYON (?:FPS|FOTO|KARE|KALITE|AVCI|AYAR)|Tsunami: |\[CHAT\]|has (?:made|reached|completed) the).*"
    for satir in re.findall(desen, metin):
        log("  " + re.split(r"TSUNAMI-OTOMASYON |DogalAfetler/\]: |\]: ", satir, maxsplit=1)[-1])
    for png in (RUN / "screenshots").glob("tsunami-*.png"):
        if png.stat().st_mtime > duvar_baslangic:
            shutil.copy2(png, kanit / png.name)
            log(f"kopyalandı: {png.name}")
    if alarm or girdi:
        return 1
    return 0 if bitti and cikis and cikis.group(1) == "0" else 2


if __name__ == "__main__":
    sys.exit(main())
