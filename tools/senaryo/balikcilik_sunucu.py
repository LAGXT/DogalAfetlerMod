"""Buz altı balıkçılığın dedicated sunucu kabulü (K0-18).

Kullanım (worktree kökünden): python3 tools/senaryo/balikcilik_sunucu.py

Adımlar: runServer açılır (stdin boruya bağlı, pty'siz) → `[balikcilik] loot tablosu yüklendi` satırı aranır →
`forceload add 0 0` ve y=200'de küçük sahne (taş, su, üstünde son donma aşamasında `buz_deligi[asama=3]`) → aşama süresi
(15 sn) dolunca `execute if block ... minecraft:ice` (delik dedicated'da yeniden donar) → `loot spawn ... loot
dogalafetler:gameplay/buz_deligi_balikcilik` (özel kategori girdisi sunucuda çalışır) → temizlik → `forge tps` → `stop`.
Ham log `KOK/.vibe/kanit/balikcilik/runServer-<n>.log`, özet `sunucu.log`. Kendi worktree'sinin `run-server/` klasörünü
kullanır. Ağır süreç kapısı: Gradle < 2 ve boş RAM >= 4 GB (en çok 20 dk).
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
KANIT = KOK / ".vibe/kanit/balikcilik"
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
    yonetilen = {"server-port": "25596", "online-mode": "false", "level-name": "test", "level-seed": "20261105"}
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


def main():
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    hazirla()
    no = no_al()
    s = Sunucu(no)
    sonuc = {}
    try:
        if not s.bekle(r"Done \(", 900):
            log("sunucu açılmadı")
            return 1
        loot = s.bekle(r"\[balikcilik\] loot tablosu yüklendi: .*", 5)
        sonuc["loot"] = bool(loot)
        log("loot satırı:", loot.group(0) if loot else "yok")
        for k in ("forceload add 0 0", "fill 0 199 0 2 202 2 minecraft:air", "fill 0 199 0 2 199 2 minecraft:stone",
                  "fill 0 200 0 2 201 2 minecraft:stone", "setblock 1 200 1 minecraft:water",
                  "setblock 1 201 1 minecraft:water", "setblock 1 202 1 dogalafetler:buz_deligi[asama=3]",
                  "execute if block 1 202 1 dogalafetler:buz_deligi"):
            s.komut(k)
            time.sleep(0.5)
        delik = s.bekle(r"Test passed", 10)
        sonuc["delik"] = bool(delik)
        log("delik kondu:", bool(delik))
        time.sleep(18)
        s.komut("execute if block 1 202 1 minecraft:ice")
        dondu = s.bekle(r"Test passed", 10, sonra=1)
        sonuc["dondu"] = bool(dondu)
        log("aşama süresinden sonra buz:", bool(dondu))
        s.komut("execute if block 1 201 1 minecraft:water")
        su = s.bekle(r"Test passed", 10, sonra=2)
        sonuc["su"] = bool(su)
        log("alttaki su kaldı:", bool(su))
        s.komut("loot spawn 1 204 1 loot dogalafetler:gameplay/buz_deligi_balikcilik")
        dusen = s.bekle(r"Dropped \d+ .*", 10)
        sonuc["lootSpawn"] = bool(dusen)
        log("loot spawn:", dusen.group(0) if dusen else "yok")
        for k in ("kill @e[type=minecraft:item]", "fill 0 199 0 2 204 2 minecraft:air", "forceload remove 0 0"):
            s.komut(k)
            time.sleep(0.5)
        s.komut("forge tps")
        if s.bekle(r"Overall: .*TPS", 30):
            log("tps:", s.bekle(r"Overall: .*", 1).group(0))
        time.sleep(2)
    finally:
        s.durdur()
    metin = s.metin()
    sonuc["invalidDist"] = metin.count("invalid dist")
    sonuc["exception"] = metin.count("Exception")
    sonuc["error"] = len(re.findall(r"/ERROR\]", metin))
    log(f"özet: {sonuc}")
    tamam = (sonuc.get("loot") and sonuc.get("delik") and sonuc.get("dondu") and sonuc.get("su")
             and sonuc.get("lootSpawn") and sonuc["invalidDist"] == 0)
    return 0 if tamam else 1


if __name__ == "__main__":
    sys.exit(main())
