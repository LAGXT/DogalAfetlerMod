#!/usr/bin/env python3
"""Güneş patlaması + kızıl cücenin görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k); GOREV §5 Faz 4 kareleri).

Kullanım (worktree kökünden): python3 tools/senaryo/gunes_patlamasi_gorsel.py

Tek oyunculu Quick Play dünyası `run/saves/gunes` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=gunes_patlamasi` ile açılır ve `client.gunes_patlamasi.GunesPatlamasiOtomasyonu`
adımlarını yürütür (ön-uyarı, siren aurora + büyüyen güneş, AKTIF 1. gün, 4. gün meteor yağmuru + krater/ateş, kavrulma,
süpernova çökme/parlama/kabuk, güneşsiz, kızıl cüce doğuşu ve gündüzü). Her kare satırı FPS, TPS ve mspt taşır;
betik bunları `kareler-<no>.tsv`'ye yazar.

Protokol: (a) açılış yalnız Hyprland exec kuralıyla ([workspace 96 silent; no_initial_focus; no_focus on;
focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere no_focus=true ve ws 96 doğrulanır,
tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç öldürülür, odak kaydedilen pencereye
verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur, sıfırdan farklıysa koşu geçersiz; (k)
runClient `runclient_kilit.py kos` ile (aynı anda tek istemci), kilit satırları protokole kopyalanır. Kare eksik ya da
bayatsa (iki kare bayt bayt aynı: gizli workspace'te çizilmemiş kare) en çok bir tekrar.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Sayılar ve adımlar
KOK/.vibe/kanit/gunes-patlamasi-kizil-cuce/gorsel/protokol.log'a, kareler aynı yerdeki kareler/'e.
Çıkış: 0 geçerli ve tam, 1 geçersiz/eksik, 2 ağır süreç kapısı açılmadı.
"""
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
import runclient_kilit  # noqa: E402

WS = 96
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/gunes-patlamasi-kizil-cuce/gorsel"
KARELER = KANIT / "kareler"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
ISARET = "GUNES_OTOMASYON"
BEKLENEN = ["on_uyari", "siren_aurora", "siren_gunes", "aktif_gun1", "kavrulma_gun1", "meteor_yagmuru", "meteor_krater_ates",
            "kavrulma_gun4", "supernova_cokme", "supernova_parlama", "supernova_kabuk", "gunessiz",
            "kizil_cuce_dogus", "kizil_cuce_gunduz"]
KARE_SATIRI = re.compile(rf"{ISARET} kare gunes_(\w+)\.png \(FPS (\d+), TPS ([\d.?]+), mspt ([\d.?]+), evre (\w+), "
                         r"ilerleme ([\d.]+), evre sn ([\d.]+), uçan meteor (\d+), parçacık (\d+), kuruluk basamağı (\d+)")
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
        bos = int(open("/proc/meminfo").read().split("MemAvailable:")[1].split()[0]) // 1024
        log(f"kapı: gradle={gradle} bos={bos} MB (runClient sırası kilitte)")
        if gradle < 2 and bos >= 4096:
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
    hedef = run / "saves/gunes"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK / "run/saves/test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    yol = run / "options.txt"
    satirlar = yol.read_text().splitlines() if yol.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:", "renderDistance:",
                                                         "simulationDistance:"))]
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10"]) + "\n")
    for eski in (run / "screenshots").glob("gunes_*.png"):
        eski.unlink()
    latest = run / "logs/latest.log"
    if latest.exists():
        latest.unlink()


def girdi_sayilari(kaynaklar):
    for kaynak in kaynaklar:
        if kaynak.exists():
            for no, satir in enumerate(kaynak.read_text(errors="replace").splitlines(), 1):
                if f"{ISARET} kapanıyor; girdi sayıları" in satir:
                    return kaynak, no, satir.split("girdi sayıları: ", 1)[1].strip()
    return None


def kosu(no):
    """Bir istemci koşusu. Döner: (geçerli: alarm 0 ve girdi 0, kareler tam ve taze, açıklama)."""
    hazirla()
    istemci_log = KANIT / f"runClient-{no}.log"
    betik = Path("/tmp/gunes_patlamasi_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 3000 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=gunes "
                     f"-Potomasyon=gunes_patlamasi > '{istemci_log}' 2>&1\n")
    sahip_adres = runclient_kilit.istemci_baslat(betik, WS, log)
    baslangic = time.time()
    pid = None
    while time.time() - baslangic < 2100 and pid is None:
        pid = istemci_pid()
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i 35 dk'da görünmedi (kilit beklemesi dahil)")
        return False, False, "JVM yok"
    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        oldur(pid, neden)
        return False, False, neden
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
    log(f"istemci bitti ({time.time() - baslangic:.0f} sn), bekçi yoklaması={yoklama}, ALARM={1 if alarm else 0}")
    # runclient_kilit.py çıktısı istemci log'una gider; kilit satırları protokole kopyalanır.
    if istemci_log.exists():
        for satir in istemci_log.read_text(errors="replace").splitlines():
            if satir.startswith(("runClient kilidi", "kilit ", "kos: ")):
                log("kilit:", satir.strip())
    latest = W / "run/logs/latest.log"
    if latest.exists():
        shutil.copy2(latest, KANIT / f"latest-{no}.log")
    kareler = {}
    for p in sorted((W / "run/screenshots").glob("gunes_*.png")):
        hedef = KARELER / f"{p.stem}-{no}.png"
        shutil.copy2(p, hedef)
        kareler[p.stem.removeprefix("gunes_")] = hashlib.md5(p.read_bytes()).hexdigest()
        log("kare:", hedef.name, "md5", kareler[p.stem.removeprefix("gunes_")][:12])
    satirlar = (KANIT / f"latest-{no}.log").read_text(errors="replace") if latest.exists() else ""
    tablo = ["kare	FPS	TPS	mspt	evre	ilerleme	evre_sn	ucan_meteor	parcacik	kuruluk"]
    for m in KARE_SATIRI.finditer(satirlar):
        tablo.append("	".join(m.groups()))
    (KANIT / f"kareler-{no}.tsv").write_text("\n".join(tablo) + "\n", encoding="utf-8")
    for satir in tablo:
        log("tablo:", satir)
    for satir in satirlar.splitlines():
        if f"{ISARET} " in satir and ("zaman aşımı" in satir or "kavrulma:" in satir or "en düşük TPS" in satir):
            log("otomasyon:", satir.split(f"{ISARET} ", 1)[1])
    sayim = girdi_sayilari((latest, istemci_log))
    if sayim is None:
        log(f"koşu {no} GEÇERSİZ: girdi sayıları satırı yok")
        return False, False, "girdi satırı yok"
    kaynak, satir_no, sayilar = sayim
    sifir = sayilar == "tuş=0 fare=0 tekerlek=0"
    sifir = runclient_kilit.girdi_ozeti(sifir, f"koşu {no} girdi sayıları: {sayilar} ({kaynak.name}:{satir_no})", log)
    eksik = [k for k in BEKLENEN if k not in kareler]
    ayni = sorted({a for a in kareler for b in kareler if a < b and kareler[a] == kareler[b]})
    if eksik:
        log(f"koşu {no}: eksik kare {eksik}")
    if ayni:
        log(f"koşu {no}: bayat kare (bayt bayt aynı) {ayni}")
    return sifir and not alarm, not eksik and not ayni, "tamam" if not eksik and not ayni else "kare eksik/bayat"


def main():
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    for deneme in (1, 2):
        no = kosu_no()
        gecerli, tam, aciklama = kosu(no)
        log(f"koşu {no} sonuç: geçerli={gecerli} kareler tam={tam} ({aciklama})")
        if not gecerli:
            return 1
        if tam:
            return 0
        if deneme == 1:
            log("kare eksik ya da bayat: bir tekrar")
    return 1


if __name__ == "__main__":
    sys.exit(main())
