#!/usr/bin/env python3
"""Uyarı/ses istemci kanıtı: `runClient -Potomasyon=uyari` K0-21 rev. 2 ekran protokolüyle (docs/KARARLAR.md).

(a) İstemci yalnız Hyprland exec kuralıyla açılır: `[workspace 94 silent; no_initial_focus; no_focus on; focus_on_activate off]`.
(b) İstemci JVM'i (cwd = bu worktree'nin run/) görününce ortamında `HL_EXEC_RULE_TOKEN` aranır; yoksa süreç öldürülür.
(c) Pencere eşlenince `set_prop` (no_focus=1, focus_on_activate=0, lock) yazılır; `getprop no_focus` true ve workspace 94
    doğrulanır, tutmazsa süreç öldürülür.
(d) Bekçi: açılıştan önce sahibin etkin penceresi kaydedilir; 5 sn'de bir `activewindow`. Etkin pencere bizim pid'imizse
    ALARM: süreç öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz.
(e) Oyun içi girdi olayları log'dan sayılır; sıfırdan farklıysa koşu geçersiz.
Ses: sahibin hoparlörüne hiç ses gitmez. Oyunun OpenAL Soft'u `ALSOFT_DRIVERS=wave` ile yalnız "Wave File Writer" aygıtını
açar ve karışımı /tmp/uyari-ses-cikis.wav'a yazar (ölçüm dosyadan, `tools/senaryo/uyari_kayit_olc.py`). Oyun Ana Ses
%0 ile açılır; otomasyon açık aygıtın adını OpenAL'dan okur, "Wave File Writer" ise Ana Ses'i %100 yapar (vanilla Ana Ses 0
iken hiçbir sesi başlatmaz), değilse oyunu kapatır. Bu betik de log'daki "OpenAL initialized on device" satırını denetler,
başka aygıtta süreci öldürür. Kategoriler: WEATHER %100 (örnek ortam katmanı; açık havada başka WEATHER sesi yok), diğerleri
0 (kayıtta yalnız siren ve ortam katmanı kalır).
(f) U-17: worktree'nin `run/options.txt`'i koşudan önce bayt bayt saklanır, sonunda (hata, SIGTERM ve SIGINT'te de) geri
    yazılır; önce/sonra sha256 log'a girer. Java kapısı (`UyariOtomasyonu`) da dosyaya güvenmez: istemci kurulumunda (ses
    motorundan önce) Ana Ses 0, seçenek ekranı kapanırken ve kapanışta 0 kaydeder.
(g) Senaryo son adımına ("çıkış") ulaşmadıysa koşu "tamamlanmadı" sayılır (protokol temiz olsa da).

Kullanım (worktree kökünden): python3 tools/senaryo/uyari_istemci.py <kanıt klasörü>
Çıkış: 0 geçerli koşu, 1 geçersiz/iptal.
"""
import hashlib
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import defter  # noqa: E402
import runclient_kilit  # noqa: E402

KOK = Path(__file__).resolve().parents[2]
RUN = KOK / "run"
WORKSPACE = 94
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
BASLATMA_BETIGI = Path("/tmp/uyari-ses-kosu.sh")
TOPLAM_SURE = 20 * 60
WAV = Path("/tmp/uyari-ses-cikis.wav")
ALSOFT_CONF = Path("/tmp/uyari-alsoft.conf")
CIHAZ = "Wave File Writer"


def ortam():
    imza = os.listdir("/run/user/1000/hypr")[0]
    return dict(os.environ, HYPRLAND_INSTANCE_SIGNATURE=imza, XDG_RUNTIME_DIR="/run/user/1000")


def hyprctl(*arg):
    return subprocess.run(["hyprctl", *arg], capture_output=True, text=True, env=ortam(), timeout=10).stdout.strip()


class Gunluk:
    def __init__(self, yol):
        self.dosya = yol.open("a", encoding="utf-8")

    def __call__(self, metin):
        satir = f"{time.strftime('%H:%M:%S')} [uyari-ses] {metin}"
        print(satir, flush=True)
        self.dosya.write(satir + "\n")
        self.dosya.flush()


def bos_ram_gib():
    for satir in Path("/proc/meminfo").read_text().splitlines():
        if satir.startswith("MemAvailable:"):
            return int(satir.split()[1]) / 1024 / 1024
    return 0.0


def gradle_sayisi():
    return int(subprocess.run(["pgrep", "-c", "-f", "[G]radleWrapperMain"], capture_output=True, text=True).stdout or 0)


def baska_istemci():
    return subprocess.run(["pgrep", "-f", "[r]unClient"], capture_output=True, text=True).stdout.split()


def cwd(pid):
    try:
        return os.readlink(f"/proc/{pid}/cwd")
    except OSError:
        return ""


def istemci_jvm():
    for pid in subprocess.run(["pgrep", "-x", "java"], capture_output=True, text=True).stdout.split():
        if cwd(pid) == str(RUN):
            return int(pid)
    return None


def oldur(log, java_pid, neden):
    log(f"SÜREÇ ÖLDÜRÜLÜYOR: {neden}")
    if java_pid:
        try:
            os.kill(java_pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
    for pid in subprocess.run(["pgrep", "-f", "[G]radleWrapperMain|[G]radleDaemon"], capture_output=True,
                              text=True).stdout.split():
        if cwd(pid) == str(KOK):
            try:
                os.kill(int(pid), signal.SIGKILL)
            except ProcessLookupError:
                pass


def secenekleri_hazirla():
    yol = RUN / "options.txt"
    satirlar = yol.read_text(encoding="utf-8").splitlines() if yol.exists() else []
    ayar = {"pauseOnLostFocus": "false", "showSubtitles": "false", "tutorialStep": "none",
            "onboardAccessibility": "false", "narrator": "0", "soundDevice": '""',
            "soundCategory_master": "0.0", "soundCategory_weather": "1.0", "lang": "tr_tr"}
    for kategori in ("music", "record", "ambient", "block", "hostile", "neutral", "player", "voice"):
        ayar[f"soundCategory_{kategori}"] = "0.0"
    satirlar = [s for s in satirlar if s.split(":", 1)[0] not in ayar] + [f"{k}:{v}" for k, v in ayar.items()]
    yol.write_text("\n".join(satirlar) + "\n", encoding="utf-8")


def kos(kanit, otomasyon="uyari", gradle_ek=("-PquickPlay=test", "-PoyunTestiAfeti=!deprem"), wav=WAV):
    """İstemciyi protokolle açar, kapanışına kadar bekler, kanıtları `kanit`'a kopyalar; koşu geçerli ve senaryo
    tamamlandıysa True. `run/options.txt` koşudan önce saklanır ve sonunda bayt bayt geri yazılır (U-17)."""
    kanit = Path(kanit).resolve()
    kanit.mkdir(parents=True, exist_ok=True)
    log = Gunluk(kanit / "pencere-uyari.log")

    for _ in range(10):
        if gradle_sayisi() < 2 and bos_ram_gib() >= 6 and not baska_istemci():
            break
        log(f"ağır süreç kapısı: gradle={gradle_sayisi()} boş={bos_ram_gib():.1f} GiB istemci={baska_istemci()}; 60 sn")
        time.sleep(60)
    else:
        log("kapı 10 dk'da açılmadı; koşu iptal")
        return False

    secenekler = RUN / "options.txt"
    ozgun = secenekler.read_bytes() if secenekler.exists() else None
    log(f"(f) options.txt önce: sha256={sha256(ozgun)}")
    gecerli = False
    # SIGTERM (timeout, kill) varsayılan olarak `finally`'yi atlar; SystemExit'e çevrilince geri yazım ve istemciyi kapatma
    # çalışır. Sinyal işleyicisi yalnız ana thread'de kurulabilir (uyari_sunucu.py `kos`'u yan thread'den çağırır).
    eski_isleyiciler = {}
    if threading.current_thread() is threading.main_thread():
        for sinyal, kod in ((signal.SIGTERM, 143), (signal.SIGINT, 130)):
            eski_isleyiciler[sinyal] = signal.signal(sinyal, lambda _s, _c, kod=kod: sys.exit(kod))
    try:
        gecerli = _kos(kanit, log, otomasyon, gradle_ek, wav)
    finally:
        for sinyal in eski_isleyiciler:
            signal.signal(sinyal, signal.SIG_IGN)
        acik = istemci_jvm()
        if acik is not None:
            oldur(log, acik, "betik bitti, istemci açık (options.txt geri yazılmadan önce)")
            time.sleep(2)
        if ozgun is None:
            secenekler.unlink(missing_ok=True)
        else:
            secenekler.write_bytes(ozgun)
        sonra = secenekler.read_bytes() if secenekler.exists() else None
        log(f"(f) options.txt geri yazıldı: sha256={sha256(sonra)} "
            f"({'öncekiyle AYNI' if sonra == ozgun else 'FARKLI'})")
        for sinyal, isleyici in eski_isleyiciler.items():
            signal.signal(sinyal, isleyici)
    return gecerli


def sha256(veri):
    return "yok" if veri is None else hashlib.sha256(veri).hexdigest()


def _kos(kanit, log, otomasyon, gradle_ek, wav):
    secenekleri_hazirla()
    for eski in (RUN / "uyari-kanit").glob("*") if (RUN / "uyari-kanit").exists() else []:
        eski.unlink()
    (RUN / "uyari-kanit").mkdir(parents=True, exist_ok=True)
    for eski in (RUN / "screenshots").glob("uyari-*.png") if (RUN / "screenshots").exists() else []:
        eski.unlink()
    istemci_log = kanit / f"runClient-{otomasyon}.log"
    wav.unlink(missing_ok=True)
    ALSOFT_CONF.write_text(f"[general]\ndrivers = wave\nfrequency = 48000\nchannels = stereo\nsample-type = int16\n"
                           f"[wave]\nfile = {wav}\n", encoding="utf-8")
    BASLATMA_BETIGI.write_text(
        f"#!/bin/sh\ncd '{KOK}' || exit 1\nexport JAVA_HOME='{JAVA_HOME}' DISPLAY=:0 ALSOFT_DRIVERS=wave "
        f"ALSOFT_CONF='{ALSOFT_CONF}'\n"
        f"exec python3 tools/senaryo/runclient_kilit.py kos '{KOK}' --bekle 1800 -- "
        f"timeout {TOPLAM_SURE} ./gradlew runClient --no-daemon --offline --console=plain "
        f"-Potomasyon={otomasyon} {' '.join(gradle_ek)} > '{istemci_log}' 2>&1\n", encoding="utf-8")

    sahibin = json.loads(hyprctl("activewindow", "-j") or "{}")
    sahip_adres = sahibin.get("address")
    log(f"(a) sahibin etkin penceresi: {sahip_adres} {sahibin.get('class')}; gradle={gradle_sayisi()} "
        f"boş={bos_ram_gib():.1f} GiB")
    kural = f"[workspace {WORKSPACE} silent; no_initial_focus; no_focus on; focus_on_activate off]"
    sonuc = hyprctl("dispatch", f'hl.dsp.exec_cmd("{kural} sh {BASLATMA_BETIGI}")')
    log(f"(a) exec kuralıyla başlatıldı: {kural} -> {sonuc}")
    basla = time.monotonic()

    java_pid = None
    while time.monotonic() - basla < 300 and java_pid is None:
        time.sleep(0.5)
        java_pid = istemci_jvm()
    if java_pid is None:
        log("istemci JVM'i 300 sn'de görünmedi; iptal")
        oldur(log, None, "JVM yok")
        return False
    environ = Path(f"/proc/{java_pid}/environ").read_bytes()
    belirtec = b"HL_EXEC_RULE_TOKEN=" in environ
    log(f"(b) istemci JVM pid={java_pid} ({time.monotonic() - basla:.1f} sn); HL_EXEC_RULE_TOKEN ortamda: {belirtec}")
    if not belirtec:
        oldur(log, java_pid, "belirteç yok")
        return False

    pencere = None
    while time.monotonic() - basla < 600 and pencere is None:
        if istemci_jvm() is None:
            break
        for istemci in json.loads(hyprctl("clients", "-j") or "[]"):
            if istemci.get("pid") == java_pid:
                pencere = istemci
        time.sleep(0.25)
    if pencere is None:
        log("pencere eşlenmedi; iptal")
        oldur(log, java_pid, "pencere yok")
        return False
    yazim = [hyprctl("dispatch", f'hl.dsp.window.set_prop({{ window = "pid:{java_pid}", prop = "{p}", value = "{v}", '
                                 f'lock = true }})') for p, v in (("no_focus", "1"), ("focus_on_activate", "0"))]
    no_focus = hyprctl("getprop", f"pid:{java_pid}", "no_focus")
    ws = pencere.get("workspace", {}).get("id")
    log(f"(c) pencerem {pencere.get('address')} ws={ws} ({time.monotonic() - basla:.1f} sn); set_prop -> {yazim}; "
        f"getprop no_focus={no_focus!r}; etkin={json.loads(hyprctl('activewindow', '-j') or '{}').get('address')}")
    if "true" not in no_focus.lower() or ws != WORKSPACE:
        oldur(log, java_pid, "pencere doğrulaması tutmadı")
        return False

    gecerli = True
    yoklama = 0
    cihaz = None
    bekci = runclient_kilit.KapanisBekcisi(KOK, java_pid, (istemci_log,), "options.txt'e yazıldı; kapanış", log)
    while not bekci.bitti_mi():
        if time.monotonic() - basla > TOPLAM_SURE:
            oldur(log, java_pid, "toplam süre aşıldı")
            gecerli = False
            break
        etkin = json.loads(hyprctl("activewindow", "-j") or "{}")
        yoklama += 1
        if etkin.get("pid") == java_pid:
            oldur(log, java_pid, "ALARM: etkin pencere istemci")
            log(f"(d) odak geri: {hyprctl('dispatch', f'hl.dsp.focus({{ window = "address:{sahip_adres}" }})')}")
            gecerli = False
            break
        if cihaz is None and istemci_log.exists():
            bulunan = re.search(r"OpenAL initialized on device (.+)", istemci_log.read_text(encoding="utf-8",
                                                                                            errors="replace"))
            if bulunan:
                cihaz = bulunan.group(1).strip()
                log(f"ses aygıtı: {cihaz!r} (beklenen {CIHAZ!r})")
                if CIHAZ not in cihaz:
                    oldur(log, java_pid, "ses aygıtı wave yazıcı değil (hoparlöre ses gidebilir)")
                    gecerli = False
                    break
        if yoklama % 6 == 1:
            log(f"(d) denetim {yoklama}: etkin={etkin.get('address')} {etkin.get('class')}; gradle={gradle_sayisi()}")
        time.sleep(5)
    if wav.exists():
        log(f"ses kaydı: {wav} ({wav.stat().st_size} bayt)")
    time.sleep(3)
    log(f"istemci kapandı; süre={time.monotonic() - basla:.0f} sn; yoklama={yoklama}; ALARM={0 if gecerli else 1}")

    metin = istemci_log.read_text(encoding="utf-8", errors="replace") if istemci_log.exists() else ""
    girdi = len(re.findall(r"\[uyari-otomasyon\] GİRDİ", metin))
    log(f"(e) oyun içi girdi olayı: {girdi}")
    if girdi:
        gecerli = False
    for png in sorted((RUN / "screenshots").glob("uyari-*.png")):
        shutil.copy2(png, kanit / png.name)
        log(f"kanıt: {png.name}")
    for dosya in sorted((RUN / "uyari-kanit").glob("*")):
        shutil.copy2(dosya, kanit / dosya.name)
        log(f"kanıt: {dosya.name}")
    tamam = re.search(r"\[uyari-otomasyon\] t=\d+ ms .*: çıkış", metin) is not None
    log(f"(g) senaryo son adıma ulaştı: {'evet' if tamam else 'hayır (tamamlanmadı)'}")
    log(f"koşu {'GEÇERLİ' if gecerli else 'GEÇERSİZ'}" + ("" if tamam else ", senaryo tamamlanmadı"))
    return gecerli and tamam


def main():
    return 0 if kos(sys.argv[1]) else 1


if __name__ == "__main__":
    sys.exit(defter.sarmala(main))
