#!/usr/bin/env python3
"""DogalAfetlerShader'ın oyun içi denemesi: adım dosyasını (client.core.AdimOtomasyonu) Oculus + paketle yürütür, kareleri toplar.

Kullanım (KOK'ten, kuyruk işinin içinde): python3 tools/senaryo/shader_adim.py <kanıt klasörü> <adım dosyası>

Dünya `run/saves/shadertest` her koşuda `run/saves/adim`'e taze kopyalanır. Paket `DogalAfetlerShader/shaders`'tan, ayarları
kullanıcının SKlauncher örneğindeki dosyadan (`ADIM_AYAR` ile değiştirilebilir) kopyalanır; Oculus gölge mesafesi sınırı da
kullanıcınınki gibi 5. Ekran protokolü (K0-21 rev. 2 (a)–(e)) `runclient_kilit` ortak yolundan. Paket GL 4.1 ister: yalnız ana
makinede (`sira.py ekle --makine ana --istemci`).
"""
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import runclient_kilit  # noqa: E402

WS = 97
W = Path(__file__).resolve().parents[2]
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
SHADER = Path("/home/lagxt/LagOS/DogalAfetlerShader/shaders")
AYAR = Path(os.environ.get("ADIM_AYAR", "/home/lagxt/.sklauncher/instances/dglftlr/shaderpacks/DogalAfetlerShader.txt"))
ISARET = "ADIM-OTOMASYON BITTI"


def main():
    kanit = Path(sys.argv[1]).resolve()
    adimlar = Path(sys.argv[2]).resolve()
    kanit.mkdir(parents=True, exist_ok=True)
    gunluk = open(kanit / "protokol.log", "a", encoding="utf-8")

    def log(*a):
        satir = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
        print(satir, flush=True)
        gunluk.write(satir + "\n")
        gunluk.flush()

    run = W / "run"
    hedef = run / "saves/adim"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(run / "saves/shadertest", hedef, ignore=shutil.ignore_patterns("session.lock"))
    paket = run / "shaderpacks/DogalAfetlerShader"
    if paket.exists():
        shutil.rmtree(paket)
    shutil.copytree(SHADER, paket / "shaders")
    shutil.copy2(AYAR, run / "shaderpacks/DogalAfetlerShader.txt")
    (run / "config/oculus.properties").write_text("shaderPack=DogalAfetlerShader\nenableShaders=true\ncolorSpace=SRGB\n"
                                                "disableUpdateMessage=true\nmaxShadowRenderDistance=5\n")
    yol = run / "options.txt"
    satirlar = [s for s in yol.read_text().splitlines() if not s.startswith(
        ("tutorialStep:", "pauseOnLostFocus:", "renderDistance:", "simulationDistance:", "fov:"))]
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:10",
                                         "simulationDistance:8", "fov:0.0"]) + "\n")
    for eski in (run / "screenshots").glob("adim_*.png"):
        eski.unlink()
    latest = run / "logs/latest.log"
    if latest.exists():
        latest.unlink()
    istemci_log = kanit / "runClient.log"
    betik = Path("/tmp/shader_adim_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 2400 ./gradlew runClient --offline --console=plain -PquickPlay=adim -Potomasyon=adim "
                     f"-PotomasyonBolum='{adimlar}' -Puyum=iris > '{istemci_log}' 2>&1\n")
    sahip = runclient_kilit.istemci_baslat(betik, WS, log)
    pid = None
    bas = time.time()
    while time.time() - bas < 2100 and pid is None:
        for p in subprocess.run(["pgrep", "-f", "forgeclientuserdev"], capture_output=True, text=True).stdout.split():
            try:
                if os.readlink(f"/proc/{p}/cwd") == str(run):
                    pid = int(p)
            except OSError:
                pass
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i görünmedi")
        return 1

    def oldur(neden):
        log("istemci öldürülüyor:", neden)
        try:
            os.kill(pid, signal.SIGKILL)
        except OSError:
            pass

    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        oldur(neden)
        return 1
    alarm = False
    bekci = runclient_kilit.KapanisBekcisi(W, pid, (latest, istemci_log), ISARET, log)
    while not bekci.bitti_mi():
        if runclient_kilit.odak_denetle(pid, sahip, log, oldur):
            alarm = True
            break
        time.sleep(5)
    log(f"istemci bitti ({time.time() - bas:.0f} sn), alarm={alarm}")
    for p in sorted((run / "screenshots").glob("adim_*.png")):
        shutil.copy2(p, kanit / p.name)
        log("kare:", p.name)
    if latest.exists():
        shutil.copy2(latest, kanit / "latest.log")
    metin = latest.read_text(errors="replace") if latest.exists() else ""
    satir = next((s for s in metin.splitlines() if ISARET in s), "")
    log("kapanış:", satir.split("BITTI", 1)[-1].strip() or "YOK")
    return 0 if satir and "tus=0 fare=0 teker=0" in satir and not alarm else 1


if __name__ == "__main__":
    sys.exit(main())
