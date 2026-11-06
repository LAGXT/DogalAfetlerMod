#!/usr/bin/env python3
"""Radar ve sismograf dedicated sunucu kabulü (K0-18): `runServer -PoyunTestiAfeti=deprem`, oyuncusuz.

Ayrı `run-server/esya` dünyası (eski serverconfig uyarıları karışmasın, K-E8). Konsoldan: `give @a dogalafetler:hava_radari`
(oyuncu yok), `dogalafet start deprem 2`, `durum`, iki kez `asama ileri`, `stop`, sunucu `stop`. Beklenen: açılış ve kayıt
satırları (ağ paketleri listesinde `esya/radar`, `esya/sismograf`; oyun testi afeti deprem), `invalid dist` 0, `Exception` 0,
çıkış 0. Log KÖK/.vibe/kanit/radar-sismograf/sunucu/.

Kullanım (worktree kökünden): python3 tools/senaryo/esya_sunucu.py [--kanit <dizin>] [--port <n>] (varsayılan port 25591).
Ağır süreç kapısı: Gradle < 2, boş RAM >= 4 GB ve port boş (en çok 20 dk; cekirdek_sunucu.py, K-E13).
"""
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/radar-sismograf/sunucu"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
PORT = 25591


def log(*a):
    satir = time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a)
    print(satir, flush=True)
    with open(KANIT / "protokol.log", "a", encoding="utf-8") as f:
        f.write(satir + "\n")


def kapi():
    return cs.agir_surec_kapisi(PORT, log)


def hazirla():
    k = W / "run-server"
    k.mkdir(exist_ok=True)
    (k / "eula.txt").write_text("eula=true\n")
    ozellik = k / "server.properties"
    satirlar = ozellik.read_text().splitlines() if ozellik.exists() else []
    yonetilen = {"server-port": str(PORT), "online-mode": "false", "level-name": "esya", "level-seed": "20261105",
                 "spawn-protection": "0", "enable-command-block": "false"}
    satirlar = [s for s in satirlar if s.split("=")[0] not in yonetilen] + [f"{a}={d}" for a, d in yonetilen.items()]
    ozellik.write_text("\n".join(satirlar) + "\n")


def main():
    global KANIT, PORT
    a = cs.argumanlar(__doc__, PORT, KANIT)
    KANIT, PORT = a.kanit, a.port
    KANIT.mkdir(parents=True, exist_ok=True)
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    hazirla()
    no = len(list(KANIT.glob("runServer-*.log"))) + 1
    yol = KANIT / f"runServer-{no}.log"
    dosya = open(yol, "w", encoding="utf-8")
    p = subprocess.Popen(["timeout", "1500", "./gradlew", "runServer", "--offline", "--console=plain", "--no-daemon",
                          "-PoyunTestiAfeti=deprem"], cwd=W, env=dict(os.environ, JAVA_HOME=JAVA_HOME),
                         stdin=subprocess.PIPE, stdout=dosya, stderr=subprocess.STDOUT, text=True, start_new_session=True)

    def metin():
        return yol.read_text(encoding="utf-8", errors="replace")

    def bekle(aranan, sure):
        son = time.time() + sure
        while time.time() < son and p.poll() is None:
            if re.search(aranan, metin()):
                return True
            time.sleep(1)
        return bool(re.search(aranan, metin()))

    def komut(k, aranan=None, sure=20):
        log("konsol >", k)
        p.stdin.write(k + "\n")
        p.stdin.flush()
        if aranan:
            log(f"  {aranan!r}:", "var" if bekle(aranan, sure) else "YOK")
        else:
            time.sleep(3)

    kod = 1
    try:
        if not bekle(r"Done \(", 900):
            log("sunucu açılmadı")
            return 1
        komut("give @a dogalafetler:hava_radari", r"No player was found")
        komut("dogalafet start deprem 2", r"Afet başladı: deprem seviye 2")
        komut("dogalafet durum")
        komut("dogalafet asama ileri")
        komut("dogalafet asama ileri")
        komut("dogalafet stop", r"Afet bitti|bitti: deprem|deprem.*KOMUT")
        komut("stop")
        p.wait(180)
        kod = p.returncode
    except (subprocess.TimeoutExpired, BrokenPipeError):
        os.killpg(p.pid, signal.SIGTERM)
    finally:
        if p.poll() is None:
            os.killpg(p.pid, signal.SIGTERM)
        dosya.close()
    m = metin()
    paket = re.search(r"\d+ ağ paketi kaydedildi.*", m)
    log("ağ paketleri:", paket.group(0) if paket else "YOK")
    log("esya/radar:", "esya/radar" in m, "| esya/sismograf:", "esya/sismograf" in m)
    log("oyun testi afeti:", "Oyun testi afeti deprem için kaydedildi" in m)
    log("invalid dist =", m.count("invalid dist"), "| Exception =", m.count("Exception"), "| ERROR =",
        len(re.findall(r"/ERROR\]", m)), "| çıkış =", kod)
    return 0 if kod == 0 and "invalid dist" not in m and "Exception" not in m and paket else 1


if __name__ == "__main__":
    sys.exit(main())
