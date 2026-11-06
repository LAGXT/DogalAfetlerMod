"""Donma sisteminin dedicated sunucu kabulü (K0-18).

Kullanım (worktree kökünden): python3 tools/senaryo/donma_sunucu.py

Adımlar: runServer açılır (stdin boruya bağlı, pty'siz) → `dogalafet donma 60` → kuyruk yetişene kadar `[donma] özet`
satırları izlenir → `dogalafet donma bilgi`, `forge tps` → `stop` → ikinci açılış → `dogalafet donma bilgi` (ilerleme ve
spawn chunk'ının kayıtlı seviyesi korunmuş mu) → `forge tps` → `dogalafet donma 0` → `stop`. Ham log'lar
`KOK/.vibe/kanit/donma/runServer-<n>.log`, özet `sunucu.log`. Kendi worktree'sinin `run-server/` klasörünü kullanır
(paylaşılan dosyaya dokunmaz). Ağır süreç kapısı: Gradle < 2 ve boş RAM >= 4 GB (en çok 20 dk).
"""
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/donma"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
KANIT.mkdir(parents=True, exist_ok=True)
GUNLUK = open(KANIT / "sunucu.log", "a", encoding="utf-8")


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
        log(f"kapı: gradle={gradle} bos={bos} MB")
        if gradle < 2 and bos >= 4096:
            return True
        time.sleep(60)
    return False


def hazirla():
    k = W / "run-server"
    k.mkdir(exist_ok=True)
    (k / "eula.txt").write_text("eula=true\n")
    ozellik = k / "server.properties"
    satirlar = ozellik.read_text().splitlines() if ozellik.exists() else []
    yonetilen = {"server-port": "25593", "online-mode": "false", "level-name": "test", "level-seed": "20261105"}
    satirlar = [s for s in satirlar if s.split("=")[0] not in yonetilen]
    ozellik.write_text("\n".join(satirlar + [f"{a}={d}" for a, d in yonetilen.items()]) + "\n")


class Sunucu:
    def __init__(self, no):
        self.log_yolu = KANIT / f"runServer-{no}.log"
        self.dosya = open(self.log_yolu, "w", encoding="utf-8")
        env = dict(os.environ, JAVA_HOME=JAVA_HOME)
        self.p = subprocess.Popen(["timeout", "1500", "./gradlew", "runServer", "--offline", "--console=plain",
                                   "--no-daemon"], cwd=W, env=env, stdin=subprocess.PIPE, stdout=self.dosya,
                                  stderr=subprocess.STDOUT, text=True, start_new_session=True)

    def metin(self):
        return self.log_yolu.read_text(encoding="utf-8", errors="replace")

    def bekle(self, desen, sure, sonra=0):
        son = time.time() + sure
        while time.time() < son:
            m = list(re.finditer(desen, self.metin()))
            if len(m) > sonra:
                return m[-1]
            if self.p.poll() is not None:
                return None
            time.sleep(1)
        return None

    def komut(self, k):
        log("konsol >", k)
        self.p.stdin.write(k + "\n")
        self.p.stdin.flush()

    def durdur(self):
        if self.p.poll() is None:
            try:
                self.komut("stop")
                self.p.wait(180)
            except (subprocess.TimeoutExpired, BrokenPipeError):
                os.killpg(self.p.pid, signal.SIGTERM)
        log("sunucu çıkış", self.p.poll())


def no_al():
    var = [int(p.stem.rsplit("-", 1)[1]) for p in KANIT.glob("runServer-*.log") if p.stem.rsplit("-", 1)[1].isdigit()]
    return max(var, default=0) + 1


def yetis(s, sure=600):
    """Kuyruk yetişene kadar: son özet satırında bekleyen=0 ya da 30 sn yeni özet yok."""
    son = time.time() + sure
    onceki = 0
    sessiz = time.time()
    while time.time() < son:
        ozetler = re.findall(r"\[donma\] özet: .*", s.metin())
        if len(ozetler) != onceki:
            onceki = len(ozetler)
            sessiz = time.time()
        elif time.time() - sessiz > 30:
            break
        time.sleep(2)
    ozetler = re.findall(r"\[donma\] özet: .*", s.metin())
    log(f"yetişme: {len(ozetler)} özet satırı; son: {ozetler[-1] if ozetler else 'yok'}")


def main():
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    hazirla()
    no = no_al()
    s = Sunucu(no)
    try:
        if not s.bekle(r"Done \(", 900):
            log("sunucu açılmadı")
            return 1
        s.komut("dogalafet donma 60")
        yetis(s)
        s.komut("dogalafet donma bilgi")
        s.komut("forge tps")
        if s.bekle(r"Overall: .*TPS", 30):
            log("tps:", s.bekle(r"Overall: .*", 1).group(0))
        time.sleep(3)
    finally:
        s.durdur()
    birinci = s.metin()
    if not kapi():
        return 2
    s2 = Sunucu(no + 1)
    try:
        if not s2.bekle(r"Done \(", 900):
            log("ikinci açılış başarısız")
            return 1
        s2.komut("dogalafet donma bilgi")
        bilgi = s2.bekle(r"Freeze: .*", 30)
        chunk = s2.bekle(r"Chunk \[.*last processed.*", 10)
        log("yeniden açılış bilgi:", bilgi.group(0) if bilgi else "yok")
        log("yeniden açılış chunk:", chunk.group(0) if chunk else "yok")
        time.sleep(20)
        s2.komut("forge tps")
        if s2.bekle(r"Overall: .*TPS", 30):
            log("tps:", s2.bekle(r"Overall: .*", 1).group(0))
        # D4: sifirla bizim kar ve buzu kuyrukla 0'a çözer (blokları bırakmaz).
        s2.komut("dogalafet sifirla")
        yetis(s2, 400)
        s2.komut("dogalafet donma bilgi")
        cozuldu = s2.bekle(r"Chunk \[0, 0\]: last processed 0 ", 30, sonra=0)
        log("sıfırlama sonrası chunk:", cozuldu.group(0) if cozuldu else "yok")
    finally:
        s2.durdur()
    ikinci = s2.metin()
    for ad, metin in (("birinci", birinci), ("ikinci", ikinci)):
        log(f"{ad}: invalid dist={metin.count('invalid dist')} Exception={metin.count('Exception')} "
            f"ERROR={len(re.findall(r'/ERROR\]', metin))}")
    korundu = bool(re.search(r"Freeze: 60\.00%", ikinci))
    sifirlandi = bool(re.search(r"Chunk \[0, 0\]: last processed 0 ", ikinci))
    log("ilerleme yeniden başlatmada korundu:", korundu, "| sifirla sonrası chunk 0'a çözüldü:", sifirlandi)
    return 0 if korundu and sifirlandi else 1


if __name__ == "__main__":
    sys.exit(main())
