#!/usr/bin/env python3
"""Sanal su görsel kanıt koşusu: K0-21 rev. 2 ekran protokolü (docs/KARARLAR.md).

(a) İstemci yalnız Hyprland exec kuralıyla açılır: `[workspace 91 silent; no_initial_focus; no_focus on; focus_on_activate off]`.
(b) Belirteç kapısı: istemci JVM'i (cwd = worktree `run/`) görününce ortamında `HL_EXEC_RULE_TOKEN` aranır; yoksa öldürülür.
(c) Pencere doğrulaması: pid'in penceresine `set_prop no_focus=1`, `focus_on_activate=0` (lock) yazılır; `getprop no_focus`
    true ve workspace 91 değilse öldürülür.
(d) Bekçi: sahibin etkin penceresi kaydedilir; 5 sn'de bir `hyprctl activewindow -j`; etkin pencere bizim pid'imizse ALARM:
    süreç öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz.
(e) Oyun içi girdi günlüğü (`client.su.SuOtomasyon`): `SU-OTOMASYON GIRDI` satırları sayılır; sıfırdan farklıysa geçersiz.
(f) A-34 kare kapısı ortak `client.core.GorselOtomasyon`'da: `KARE-KAPISI su ... kapi=GECERSIZ` satırı varsa koşu geçersiz;
    `OLCUM su` satırları `olcum_tablosu.py` ile `<kanıt>/olcum.tsv`'ye (K-E29 (b)).
(i) Ağır süreç kapısı ve aynı anda tek runClient.

Kullanım (worktree kökünden): python3 tools/senaryo/su_istemci.py <kanıt klasörü> [bölüm]
Çıkış: 0 geçerli koşu, 1 protokol ihlali/iptal, 2 istemci hatası.
"""
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import defter  # noqa: E402
import olcum_tablosu  # noqa: E402
import runclient_kilit  # noqa: E402

KOK = Path(__file__).resolve().parents[2]
RUN = KOK / "run"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
WS = 91


def sh(komut, zaman=30):
    r = subprocess.run(komut, shell=isinstance(komut, str), capture_output=True, text=True, timeout=zaman)
    return (r.stdout + r.stderr).strip()


class Kayit:
    def __init__(self, yol):
        self.f = yol.open("a", encoding="utf-8")

    def __call__(self, metin):
        satir = f"{time.strftime('%H:%M:%S')} {metin}"
        print(satir, flush=True)
        self.f.write(satir + "\n")
        self.f.flush()


def kapi(log):
    for _ in range(11):
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


def main():
    kanit = Path(sys.argv[1]).resolve()
    bolum = sys.argv[2] if len(sys.argv) > 2 else "hepsi"
    kanit.mkdir(parents=True, exist_ok=True)
    log = Kayit(kanit / "pencere.log")
    if not kapi(log):
        log("kapı 10 dk açılmadı; koşu yapılmadı")
        return 1
    # Her koşu ana test dünyasının temiz kopyasıyla başlar (vitrin sahnesi gerçek blok kurar; sahne belirlenimci kalır).
    hedef = RUN / "saves" / "test"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK.parents[1] / "run" / "saves" / "test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    (hedef / "serverconfig" / "dogalafetler-server.toml").unlink(missing_ok=True)
    log(f"test dünyası temiz kopyalandı: {hedef}")
    oyun_log = kanit / f"runClient-{bolum}.log"
    betik = Path("/tmp/sanal-su-istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{KOK}' || exit 9\n"
                     f"JAVA_HOME='{JAVA_HOME}' python3 tools/senaryo/runclient_kilit.py kos '{KOK}' --bekle 1800 -- "
                     f"timeout 1200 ./gradlew runClient --no-daemon --offline --console=plain "
                     f"-PquickPlay=test -Potomasyon=su -PotomasyonBolum={bolum} > '{oyun_log}' 2>&1\n"
                     f"echo \"cikis=$?\" >> '{oyun_log}'\n")
    duvar_baslangic = time.time()
    sahip_adres = runclient_kilit.istemci_baslat(betik, WS, log)
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
    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        log(f"ALARM: {neden}; süreç öldürülüyor")
        os.kill(pid, signal.SIGKILL)
        return 1
    yoklama = 0
    alarm = 0
    bekci = runclient_kilit.KapanisBekcisi(KOK, pid, (oyun_log,), "SU-OTOMASYON BITTI", log)
    while not bekci.bitti_mi():
        time.sleep(5)
        yoklama += 1
        if runclient_kilit.odak_denetle(pid, sahip_adres, log, lambda _neden: os.kill(pid, signal.SIGKILL)):
            alarm += 1
            break
        if yoklama % 6 == 0:
            log(f"denetim {yoklama}: istemci pid={pid} yaşıyor")
    son = time.monotonic() + 120
    while time.monotonic() < son and "cikis=" not in oyun_log.read_text(encoding="utf-8", errors="replace"):
        time.sleep(2)
    metin = oyun_log.read_text(encoding="utf-8", errors="replace")
    girdi = len(re.findall(r"SU-OTOMASYON GIRDI", metin))
    girdi_temiz = runclient_kilit.girdi_ozeti(girdi == 0, f"(e) oyun içi girdi satırı: {girdi}", log)
    kapi_uyari = len(re.findall(r"KARE-KAPISI su \S+ kapi=GECERSIZ", metin))
    bitti = re.search(r"SU-OTOMASYON BITTI girdi tus=(\d+) fare=(\d+) teker=(\d+)", metin)
    cikis = re.search(r"cikis=(\d+)", metin)
    log(f"bitti: yoklama={yoklama} ALARM={alarm} girdi_satiri={girdi} kapi_uyari={kapi_uyari} "
        f"otomasyon_ozeti={bitti.groups() if bitti else None} gradle_{cikis.group(0) if cikis else 'cikis=?'}")
    for satir in re.findall(r".*(?:SU-OTOMASYON (?:FPS|FOTO|OYUNCU)|OLCUM su |KARE-KAPISI su |SU-TANI |Sanal su: kurulum).*",
                            metin):
        log("  " + re.split(r"SU-OTOMASYON |DogalAfetler/\]: |\]: ", satir, maxsplit=1)[-1])
    satirlar, ozet = olcum_tablosu.tablo([oyun_log])
    (kanit / "olcum.tsv").write_text("\n".join(satirlar) + "\n", encoding="utf-8")
    log(f"OLCUM tablosu: {len(satirlar) - 1} satır → olcum.tsv")
    for o in ozet:
        log("  " + o)
    for png in (RUN / "screenshots").glob("sanal-su-*.png"):
        if png.stat().st_mtime > duvar_baslangic:
            shutil.copy2(png, kanit / png.name)
            log(f"kopyalandı: {png.name}")
    # A-34: kapısı açılmadan alınan kare geçersiz, koşu da.
    if alarm or not girdi_temiz or kapi_uyari:
        return 1
    return 0 if bitti and cikis and cikis.group(1) == "0" else 2


if __name__ == "__main__":
    sys.exit(defter.sarmala(main))
