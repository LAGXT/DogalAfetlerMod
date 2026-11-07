#!/usr/bin/env python3
"""Ortak yangının görsel kanıtı (K0-21 rev. 2): taç yangını vitrini, Yüksek/Düşük kalite, yağmurla sönme, /forge tps.

Kullanım (worktree kökünden, önce tools/senaryo/yangin_sunucu.py koşmuş olmalı):
  python3 tools/senaryo/yangin_gorsel.py

Dünya: run-server/yangin-senaryo → run/saves/yangin (session.lock hariç), Quick Play tek oyunculu. Oyun içi adımları
`client.yangin.YanginOtomasyonu` (`-Potomasyon=yangin`) yürütür: en yakın orman, alacakaranlık, seyirci kamera,
taç cephesi, kareler, yağmur, söndürme, kapanış.

Ekran protokolü: istemci yalnız Hyprland exec kuralıyla açılır ([workspace 95 silent; no_initial_focus; no_focus on;
focus_on_activate off]); JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; pencere no_focus=true ve ws 95 doğrulanır; 5 sn'de bir
bekçi etkin pencere bizimse ALARM verip süreci öldürür; oyun içi tuş/fare/tekerlek sayıları protokol log'una yazılır.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Çıkış: 0 geçerli koşu, 1 geçersiz/hata, 2 kapı açılmadı.
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
import runclient_kilit  # noqa: E402

WS = 95
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/yangin"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
DUNYA_KAYNAGI = W / "run-server/yangin-senaryo"
DUNYA = W / "run/saves/yangin"
ON = "YANGIN_OTOMASYON "
KANIT.mkdir(parents=True, exist_ok=True)
GUNLUK = open(KANIT / "protokol.log", "a", encoding="utf-8")


def log(*a):
    satir = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
    print(satir, flush=True)
    GUNLUK.write(satir + "\n")
    GUNLUK.flush()


def hyprland_imzasi():
    """Ajan kabuğu Hyprland ortamını devralmayabilir: tek canlı örneğin imzası soket klasöründen alınır."""
    if os.environ.get("HYPRLAND_INSTANCE_SIGNATURE"):
        return os.environ["HYPRLAND_INSTANCE_SIGNATURE"]
    kok = Path(os.environ.get("XDG_RUNTIME_DIR", f"/run/user/{os.getuid()}")) / "hypr"
    adaylar = [p.name for p in kok.glob("*") if (p / ".socket.sock").exists()]
    if len(adaylar) != 1:
        raise SystemExit(f"Hyprland örneği belirsiz ({kok}): {adaylar}")
    os.environ["HYPRLAND_INSTANCE_SIGNATURE"] = adaylar[0]
    return adaylar[0]


def kapi():
    """Ağır süreç kapısı: Gradle sayısı < 2, boş RAM >= 4 GB ve çalışan runClient yok (en çok 20 dk)."""
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


def dunyayi_hazirla():
    if not (DUNYA_KAYNAGI / "level.dat").exists():
        raise SystemExit(f"{DUNYA_KAYNAGI}/level.dat yok: önce tools/senaryo/yangin_sunucu.py")
    if DUNYA.exists():
        shutil.rmtree(DUNYA)
    shutil.copytree(DUNYA_KAYNAGI, DUNYA, ignore=shutil.ignore_patterns("session.lock"))
    secenekler = W / "run/options.txt"
    satirlar = secenekler.read_text().splitlines() if secenekler.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:"))]
    secenekler.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false"]) + "\n")


def istemci_pid():
    """Kendi run/ klasöründe çalışan Minecraft istemci JVM'i."""
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


def girdi_sayilari(istemci_log):
    for kaynak in (W / "run/logs/latest.log", istemci_log):
        if not kaynak.exists():
            continue
        for no, satir in enumerate(kaynak.read_text(errors="replace").splitlines(), 1):
            if ON + "kapanıyor; girdi sayıları" in satir:
                return kaynak, no, satir.split("girdi sayıları: ", 1)[1].strip()
    return None


def istemci_calistir(no):
    betik = Path("/tmp/yangin_istemci.sh")
    istemci_log = KANIT / f"runClient-{no}.log"
    log(f"koşu {no}: istemci log'u {istemci_log.name}")
    eski_latest = W / "run/logs/latest.log"
    if eski_latest.exists():
        eski_latest.unlink()
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 900 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=yangin "
                     "-Potomasyon=yangin "
                     f"> '{istemci_log}' 2>&1\n")
    sahip_adres = runclient_kilit.istemci_baslat(betik, WS, log)
    baslangic = time.time()
    pid = None
    while time.time() - baslangic < 300 and pid is None:
        pid = istemci_pid()
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i 300 sn'de görünmedi")
        return False
    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        oldur(pid, neden)
        return False
    alarm = False
    yoklama = 0
    bekci = runclient_kilit.KapanisBekcisi(W, pid, (W / "run/logs/latest.log", istemci_log), ON + "kapanıyor; girdi sayıları", log)
    while not bekci.bitti_mi():
        yoklama += 1
        if runclient_kilit.odak_denetle(pid, sahip_adres, log, lambda neden: oldur(pid, neden)):
            alarm = True
            break
        time.sleep(5)
    log(f"istemci bitti ({time.time() - baslangic:.0f} sn), bekçi yoklaması={yoklama}, alarm={alarm}")
    sayim = girdi_sayilari(istemci_log)
    if sayim is None:
        log(f"koşu {no} GEÇERSİZ: girdi sayıları satırı yok")
        return False
    kaynak, satir_no, sayilar = sayim
    sifir = sayilar == "tuş=0 fare=0 tekerlek=0"
    sifir = runclient_kilit.girdi_ozeti(sifir, f"koşu {no} girdi sayıları: {sayilar} (kaynak {kaynak.relative_to(W)}:{satir_no})", log)
    if kaynak.name == "latest.log":
        shutil.copy2(kaynak, KANIT / f"latest-{no}.log")
    return not alarm and sifir


def ozet_cikar(no):
    kaynak = KANIT / f"latest-{no}.log"
    if not kaynak.exists():
        return
    satirlar = [s for s in kaynak.read_text(errors="replace").splitlines()
                if ON in s or "Mean tick time" in s or "Burning cells" in s or "Yanan hücre" in s or "yangın kısma" in s
                or "bütçe çarpanı" in s or "Exception" in s or "invalid dist" in s]
    (KANIT / f"ozet-{no}.log").write_text("\n".join(satirlar) + "\n", encoding="utf-8")
    log(f"özet-{no}.log: {len(satirlar)} satır; Exception={sum('Exception' in s for s in satirlar)}")


def kareleri_tasi(no):
    hedef = KANIT / f"kosu-{no}"
    hedef.mkdir(exist_ok=True)
    for p in sorted((W / "run/screenshots").glob("yangin_*.png")):
        shutil.copy2(p, hedef / p.name)
        log("kare:", f"kosu-{no}/{p.name}", "md5", hashlib.md5(p.read_bytes()).hexdigest()[:12])


def main():
    var = [int(p.stem.rsplit("-", 1)[1]) for p in KANIT.glob("runClient-*.log") if p.stem.rsplit("-", 1)[1].isdigit()]
    no = max(var, default=0) + 1
    if not runclient_kilit.ekran_pc2():
        log("Hyprland imzası:", hyprland_imzasi())
    (W / "run/screenshots").mkdir(parents=True, exist_ok=True)
    for eski in (W / "run/screenshots").glob("yangin_*.png"):
        eski.unlink()
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    dunyayi_hazirla()
    ok = istemci_calistir(no)
    ozet_cikar(no)
    kareleri_tasi(no)
    log(f"koşu {no} sonucu: {'GEÇERLİ' if ok else 'GEÇERSİZ'}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
