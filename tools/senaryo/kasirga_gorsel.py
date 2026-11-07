#!/usr/bin/env python3
"""Kasırganın görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k); GOREV §6 #6, #10).

Kullanım (worktree kökünden): python3 tools/senaryo/kasirga_gorsel.py

Tek oyunculu Quick Play dünyası `run/saves/kasirga` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=kasirga` ile açılır ve `client.kasirga.KasirgaOtomasyonu` adımlarını yürütür (ön-uyarı radarı,
siren, aktif, vitrin ve kontrol karesi, yakın kare, Düşük/Yüksek/Ultra/kasırgasız/afetsiz FPS, stop sayımı, S1, ip, bitiş izi).

Protokol: (a) açılış yalnız Hyprland exec kuralıyla ([workspace 91 silent; no_initial_focus; no_focus on;
focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere no_focus=true ve ws 91 doğrulanır,
tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç öldürülür, odak kaydedilen pencereye
verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur, sıfırdan farklıysa koşu geçersiz.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Aynı anda tek runClient (runclient_kilit.py kos); ağır süreç kapısı.
Sayılar ve adımlar KOK/.vibe/kanit/kasirga/protokol.log'a, kareler KOK/.vibe/kanit/kasirga/kareler/'e.

S5 sahnesi kara zeminde (K-E45, K-17 taban rengi kararlı olsun): otomasyon oyuncuyu dünyanın doğuş noktasına ışınlar ve
kasırga oyuncunun 53,5–107 blok uzağında rastgele bir merkezde doğar (`Kasirga.merkezSec`). Betik `test` dünyasının bölge
dosyalarından kara haritası çıkarır; "uygun merkez" = yüzey ≥ deniz seviyesi + 4 (67) ve 24 blok yarıçapında su sütunu yok.
Kopyanın doğuş noktası (`level.dat` SpawnX/Y/Z) doğuş halkasında en çok uygun merkez bulunan kara noktaya taşınır. S5 başlayınca
log'daki merkez denetlenir: uygun değilse istemci hemen kapatılır ve koşu yeniden başlar (en çok `KASIRGA_DENEME`, vars. 5;
haritanın dışındaki merkez koşu sonunda kopyanın kendi bölge dosyalarıyla doğrulanır). K-17 eşiği aynı.
"""
import gzip
import hashlib
import io
import json
import os
import re
import shutil
import struct
import signal
import subprocess
import sys
import time
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import defter  # noqa: E402
import runclient_kilit  # noqa: E402

WS = 91
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = KOK / ".vibe/kanit/kasirga"
KARELER = KANIT / "kareler"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
ISARET = "KASIRGA_OTOMASYON"
KANIT.mkdir(parents=True, exist_ok=True)
KARELER.mkdir(exist_ok=True)
GUNLUK = open(KANIT / "protokol.log", "a", encoding="utf-8")
# K-E45: uygun S5 merkezi.
DENIZ_USTU = 63 + 4
SUSUZ_YARICAP = 24
DOGUS_HALKASI = (53.5, 107.0)
DENEME = int(os.environ.get("KASIRGA_DENEME", "5"))
MERKEZ = re.compile(r"\[kasirga\] başladı: seviye 5 merkez (-?\d+), (-?\d+), (-?\d+)")


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


def _nbt(veri):
    """Sıkıştırılmamış NBT'yi Python nesnesine çevirir (yalnız okuma)."""
    f = io.BytesIO(veri)

    def oku(t):
        if t in (1, 2, 3, 4, 5, 6):
            bicim, n = {1: (">b", 1), 2: (">h", 2), 3: (">i", 4), 4: (">q", 8), 5: (">f", 4), 6: (">d", 8)}[t]
            return struct.unpack(bicim, f.read(n))[0]
        if t == 7:
            return f.read(struct.unpack(">i", f.read(4))[0])
        if t == 8:
            return f.read(struct.unpack(">H", f.read(2))[0]).decode("utf-8", "replace")
        if t == 9:
            et, n = f.read(1)[0], struct.unpack(">i", f.read(4))[0]
            return [oku(et) for _ in range(n)]
        if t == 10:
            d = {}
            while (tt := f.read(1)[0]) != 0:
                ad = f.read(struct.unpack(">H", f.read(2))[0]).decode("utf-8", "replace")
                d[ad] = oku(tt)
            return d
        if t in (11, 12):
            n = struct.unpack(">i", f.read(4))[0]
            return struct.unpack(f">{n}{'i' if t == 11 else 'q'}", f.read((4 if t == 11 else 8) * n))
        raise ValueError(f"NBT etiketi {t}")

    t = f.read(1)[0]
    f.read(struct.unpack(">H", f.read(2))[0])
    return oku(t)


def arazi(dunya):
    """Dünyanın tam üretilmiş chunk'larından sütun haritası: {(x, z): (yüzey Y, su var mı)}. Yüzey MOTION_BLOCKING (sıvı dahil),
    su = MOTION_BLOCKING > OCEAN_FLOOR (sütunun üstünde sıvı)."""
    harita = {}
    for yol in Path(dunya, "region").glob("r.*.mca"):
        b = yol.read_bytes()
        for i in range(1024):
            ofset = int.from_bytes(b[i * 4:i * 4 + 3], "big") * 4096
            if ofset == 0:
                continue
            boy = int.from_bytes(b[ofset:ofset + 4], "big")
            ham = b[ofset + 5:ofset + 4 + boy]
            ch = _nbt(zlib.decompress(ham) if b[ofset + 4] == 2 else gzip.decompress(ham))
            h = ch.get("Heightmaps", {})
            if ch.get("Status", "").split(":")[-1] != "full" or "MOTION_BLOCKING" not in h or "OCEAN_FLOOR" not in h:
                continue
            mb, of = (([(v & (1 << 64) - 1) >> 9 * k & 511 for v in h[ad] for k in range(7)])[:256]
                      for ad in ("MOTION_BLOCKING", "OCEAN_FLOOR"))
            for k in range(256):
                harita[(ch["xPos"] * 16 + (k & 15), ch["zPos"] * 16 + (k >> 4))] = (mb[k] - 65, mb[k] != of[k])
    return harita


def uygun_merkez(harita, x, z, y=None):
    """K-E45: yüzey ≥ 67 ve 24 blok yarıçapında su yok; haritada eksik sütun varsa None (doğrulanamadı)."""
    if (y if y is not None else harita.get((x, z), (0,))[0]) < DENIZ_USTU:
        return False
    r = SUSUZ_YARICAP
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if dx * dx + dz * dz <= r * r:
                sutun = harita.get((x + dx, z + dz))
                if sutun is None:
                    return None
                if sutun[1]:
                    return False
    return True


def hedef_sec(harita):
    """Doğuş halkasında (53,5–107 blok, 72 yön × 12 uzaklık) uygun merkez payı en yüksek kara sütun: (x, z, y, pay)."""
    import numpy as np
    xs = [k[0] for k in harita]
    zs = [k[1] for k in harita]
    x0, z0 = min(xs), min(zs)
    nx, nz = max(xs) - x0 + 1, max(zs) - z0 + 1
    yuk = np.full((nx, nz), -999)
    kuru = np.zeros((nx, nz), bool)
    for (x, z), (y, su) in harita.items():
        yuk[x - x0, z - z0] = y
        kuru[x - x0, z - z0] = not su
    r = SUSUZ_YARICAP
    dolgu = np.pad(kuru, r)
    susuz = np.ones((nx, nz), bool)
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if dx * dx + dz * dz <= r * r:
                susuz &= dolgu[r + dx:r + dx + nx, r + dz:r + dz + nz]
    uygun = susuz & (yuk >= DENIZ_USTU)
    halka = {(round(np.cos(a) * u), round(np.sin(a) * u)) for a in np.linspace(0, 2 * np.pi, 72, endpoint=False)
             for u in np.linspace(*DOGUS_HALKASI, 12)}
    m = int(DOGUS_HALKASI[1]) + 1
    dolgu = np.pad(uygun, m)
    sayi = np.zeros((nx, nz), int)
    for dx, dz in halka:
        sayi += dolgu[m + dx:m + dx + nx, m + dz:m + dz + nz]
    sayi[~kuru | (yuk < DENIZ_USTU)] = -1
    i, j = np.unravel_index(int(sayi.argmax()), sayi.shape)
    return int(i + x0), int(j + z0), int(yuk[i, j]), sayi[i, j] / len(halka)


def dogus_yaz(level_dat, x, y, z):
    """Kopyanın `level.dat` SpawnX/Y/Z int etiketlerini yerinde değiştirir (aynı uzunluk, gzip)."""
    veri = gzip.decompress(level_dat.read_bytes())
    for ad, deger in (("SpawnX", x), ("SpawnY", y), ("SpawnZ", z)):
        etiket = b"\x03" + struct.pack(">H", len(ad)) + ad.encode()
        if veri.count(etiket) != 1:
            raise RuntimeError(f"level.dat {ad} etiketi {veri.count(etiket)} kez")
        i = veri.index(etiket) + len(etiket)
        veri = veri[:i] + struct.pack(">i", deger) + veri[i + 4:]
    level_dat.write_bytes(gzip.compress(veri))


def hazirla(hedef):
    run = W / "run"
    (run / "saves").mkdir(parents=True, exist_ok=True)
    hedef_dunya = run / "saves/kasirga"
    if hedef_dunya.exists():
        shutil.rmtree(hedef_dunya)
    shutil.copytree(KOK / "run/saves/test", hedef_dunya, ignore=shutil.ignore_patterns("session.lock"))
    dogus_yaz(hedef_dunya / "level.dat", hedef[0], hedef[2], hedef[1])
    yol = run / "options.txt"
    satirlar = yol.read_text().splitlines() if yol.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:", "renderDistance:",
                                                         "simulationDistance:", "enableVsync:", "maxFps:"))]
    # Kasırga hedefin 54-107 blok uzağında doğar, kamera 70 bloğa kadar uzakta: simülasyon 10 chunk. Gizli workspace'te
    # vsync takası kare geri çağrısı bekleyip FPS'i 1'e düşürüyordu (koşu 1): vsync kapalı. Üst sınır 260 (K-E29 (b) tabanı
    # tavana yapışmasın; otomasyon açılışta aynı ayarı çalışma anında da verir, `GorselOtomasyon.cizimAyari`).
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10", "enableVsync:false", "maxFps:260"]) + "\n")
    for eski in (run / "screenshots").glob("kasirga_*.png"):
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


def taban_rengi(no):
    """K-17: vitrin ↔ kontrol fark maskesinin (|ΔR|+|ΔG|+|ΔB| > 60) alt üçte birinde vitrin pikselleri ortalaması.

    K-E49: kontrol karesi yalnız huni, toz/etek, duvar bulutu ve sis gizlenerek alınır (uçan bloklar ve enkaz iki karede de
    çizilir); kip istemci log'undaki `kontrol kipi yalnizToz` satırından okunur, yoksa `gizli` (eski koşular: her şey gizli).
    Ölçü ve eşik aynı; ΔR/ΔG/ΔB (maskede vitrin − kontrol ortalaması) bilgi.
    """
    import numpy as np
    from PIL import Image
    istemci = KANIT / f"latest-{no}.log"
    kip = ("yalnizToz" if "kontrol kipi yalnizToz" in istemci.read_text(errors="replace") else "gizli") \
        if istemci.exists() else "bilinmiyor"
    sonuc = True
    for ad in ("s5_vitrin", "s5_taban_uzak"):
        vitrin, kontrol = KARELER / f"kasirga_{ad}-{no}.png", KARELER / f"kasirga_{ad}_kontrol-{no}.png"
        if not (vitrin.exists() and kontrol.exists()):
            log(f"taban rengi {ad}: kare eksik")
            sonuc = False
            continue
        v = np.asarray(Image.open(vitrin).convert("RGB")).astype(int)
        k = np.asarray(Image.open(kontrol).convert("RGB")).astype(int)
        maske = np.abs(v - k).sum(2) > 60
        maske[: 2 * maske.shape[0] // 3] = False
        piksel = v[maske]
        if len(piksel) == 0:
            log(f"taban rengi {ad}: fark pikseli yok, kontrol={kip}")
            sonuc = False
            continue
        r, g, b = piksel.mean(0)
        dr, dg, db = piksel.mean(0) - k[maske].mean(0)
        pay = ((piksel[:, 0] > piksel[:, 1]) & (piksel[:, 1] > piksel[:, 2])).mean()
        tutar = r > g > b
        sonuc = sonuc and tutar
        log(f"taban rengi {ad}: R {r:.1f} G {g:.1f} B {b:.1f}, maske {len(piksel)} piksel, R>G>B payı %{100 * pay:.0f}, "
            f"ΔR {dr:+.1f} ΔG {dg:+.1f} ΔB {db:+.1f}, kontrol={kip} → " + ("R>G>B tuttu" if tutar else "TUTMADI"))
    return sonuc


def kosu(hedef, harita):
    """Tek koşu: çıkış kodu ya da S5 merkezi uygun değilse "tekrar" (istemci kapatıldı)."""
    no = kosu_no()
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    hazirla(hedef)
    istemci_log = KANIT / f"runClient-{no}.log"
    betik = Path("/tmp/kasirga_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 3000 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=kasirga -Potomasyon=kasirga "
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
    merkez = None
    merkez_uygun = None
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
        if merkez is None and (W / "run/logs/latest.log").exists():
            bulunan = MERKEZ.search((W / "run/logs/latest.log").read_text(errors="replace"))
            if bulunan:
                merkez = tuple(int(v) for v in bulunan.groups())
                merkez_uygun = uygun_merkez(harita, merkez[0], merkez[2], merkez[1])
                log(f"S5 merkezi {merkez}: " + {True: "uygun (kara)", False: "UYGUN DEĞİL (su/kıyı ya da alçak)",
                                                 None: "harita dışı, koşu sonunda doğrulanacak"}[merkez_uygun])
                if merkez_uygun is False:
                    oldur(pid, "S5 merkezi uygun değil (K-E45); yeniden denenecek")
                    return "tekrar"
        etkin = hypr_json("activewindow")
        yoklama += 1
        if yoklama % 6 == 0:
            # K0-21 rev. 2 (i): FPS yalnız Gradle sayacı 1 iken (yalnız bu koşu) geçerli; ölçüm anıyla eşleştirilir.
            gradle = subprocess.run(["pgrep", "-fc", "GradleWrapperMain"], capture_output=True, text=True).stdout.strip()
            log(f"gradle sayacı={gradle}")
        if isinstance(etkin, dict) and etkin.get("pid") == pid:
            alarm = True
            log("ALARM: etkin pencere bizim istemci")
            oldur(pid, "odak alarmı")
            if sahip_adres:
                log("odak geri:", hypr("dispatch", f'hl.dsp.focus({{ window = "address:{sahip_adres}" }})'))
            break
        time.sleep(5)
    log(f"istemci bitti ({time.time() - baslangic:.0f} sn), bekçi yoklaması={yoklama}, alarm={alarm}")
    for p in sorted((W / "run/screenshots").glob("kasirga_*.png")):
        hedef = KARELER / f"{p.stem}-{no}.png"
        shutil.copy2(p, hedef)
        log("kare:", hedef.name, "md5", hashlib.md5(p.read_bytes()).hexdigest()[:12])
    latest = W / "run/logs/latest.log"
    if latest.exists():
        shutil.copy2(latest, KANIT / f"latest-{no}.log")
    if merkez is None:
        log(f"koşu {no} GEÇERSİZ: S5 merkezi log'da yok")
        return 1
    if merkez_uygun is None:
        merkez_uygun = uygun_merkez(arazi(W / "run/saves/kasirga"), merkez[0], merkez[2], merkez[1])
        log(f"S5 merkezi {merkez} koşu dünyasıyla: " + {True: "uygun", False: "UYGUN DEĞİL", None: "doğrulanamadı"}[merkez_uygun])
        if not merkez_uygun:
            return "tekrar"
    renk = taban_rengi(no)
    if not renk:
        log(f"koşu {no} GEÇERSİZ: taban rengi (K-17) tutmadı ya da kare eksik")
    sayim = girdi_sayilari(istemci_log)
    if sayim is None:
        log(f"koşu {no} GEÇERSİZ: girdi sayıları satırı yok")
        return 1
    kaynak, satir_no, sayilar = sayim
    sifir = sayilar == "tuş=0 fare=0 tekerlek=0"
    log(f"koşu {no} girdi sayıları: {sayilar} ({kaynak.name}:{satir_no})" + ("" if sifir else " → GEÇERSİZ"))
    return 0 if sifir and not alarm and renk else 1


def main():
    harita = arazi(KOK / "run/saves/test")
    x, z, y, pay = hedef_sec(harita)
    log(f"K-E45: doğuş noktası ({x}, {y}, {z}); halkada uygun merkez payı %{100 * pay:.0f}, en çok {DENEME} deneme")
    for deneme in range(1, DENEME + 1):
        sonuc = kosu((x, z, y), harita)
        if sonuc != "tekrar":
            return sonuc
        log(f"deneme {deneme}/{DENEME}: S5 merkezi uygun değil, yeniden")
    log(f"{DENEME} denemede uygun S5 merkezi gelmedi; koşu GEÇERSİZ")
    return 1


if __name__ == "__main__":
    sys.exit(defter.sarmala(main, KANIT))
