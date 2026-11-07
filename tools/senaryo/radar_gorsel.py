#!/usr/bin/env python3
"""Hava Radarı ve Sismograf görsel kanıtı: `runClient -Potomasyon=radar` K0-21 rev. 2 ekran protokolü ve (k) kilidiyle.

Ayını protokol `uyari_istemci.py`'deki gibidir: (a) yalnız Hyprland exec kuralı `[workspace 91 silent; no_initial_focus;
no_focus on; focus_on_activate off]`; (b) `HL_EXEC_RULE_TOKEN` kapısı; (c) `no_focus=true` ve ws 91 doğrulaması; (d) 5 sn'de
bir bekçi, etkin pencere bizimse ALARM; (e) oyun içi girdi olayları log'dan sayılır; (f) `run/options.txt` bayt bayt geri
yazılır; (g) senaryo kapanış satırına ulaşmadıysa "tamamlanmadı". Ses sahibin hoparlörüne gitmez: OpenAL yalnız "Wave File
Writer" aygıtını açar, Ana Ses %0. runClient `runclient_kilit.py kos` ile kilitlidir.

Senaryo (`client.esya.radar.RadarOtomasyonu`): tek oyunculu `test` dünyası, geçmiş için kısa kasırga S2 + sinkhole S4, radar
sakin → deprem S3 (Hassas Sismograf erken, radar sakin) → normal radar → Gelişmiş radar → normal sismograf action bar →
60 sn grafik ekranı → güneş patlaması SIREN'de gökyüzü anomalisi. Her karede panel dikdörtgeni ve sıcaklık barı/action bar
çakışma denetimi istemci log'undadır.

Kullanım (worktree kökünden): python3 tools/senaryo/radar_gorsel.py [kanıt klasörü]
(varsayılan KÖK/.vibe/kanit/radar-sismograf/gorsel). Çıkış: 0 geçerli koşu, 1 geçersiz/iptal.
pc2'de görünür istemciyle (yalnız kare; (a)–(d) Hyprland denetimleri yok, (e) geçersiz kılmaz; `runclient_kilit.ekran_pc2`):
tools/senaryo/pc2_istemci.sh <kanıt>/ radar_gorsel.py <kanıt>
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
import runclient_kilit  # noqa: E402

KOK = Path(__file__).resolve().parents[2]
KOK_ANA = KOK.parents[1] if KOK.parent.name == ".worktrees" else KOK
RUN = KOK / "run"
WORKSPACE = 91
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
BASLATMA_BETIGI = Path("/tmp/radar-sismograf-kosu.sh")
TOPLAM_SURE = 20 * 60
WAV = Path("/tmp/radar-sismograf-cikis.wav")
ALSOFT_CONF = Path("/tmp/radar-sismograf-alsoft.conf")
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
        satir = f"{time.strftime('%H:%M:%S')} [radar-sismograf] {metin}"
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


def dunyayi_hazirla(log):
    """Quick Play `test` dünyası (MAKINE.md seed 20261105) worktree'nin run/saves'inde yoksa KÖK'ten kopyalanır."""
    hedef = RUN / "saves/test"
    kaynak = KOK_ANA / "run/saves/test"
    if not hedef.exists() and kaynak.exists():
        shutil.copytree(kaynak, hedef, ignore=shutil.ignore_patterns("session.lock"))
        log(f"test dünyası kopyalandı: {kaynak} -> {hedef}")
    return hedef.exists()


def secenekleri_hazirla():
    RUN.mkdir(parents=True, exist_ok=True)
    yol = RUN / "options.txt"
    satirlar = yol.read_text(encoding="utf-8").splitlines() if yol.exists() else []
    ayar = {"pauseOnLostFocus": "false", "showSubtitles": "false", "tutorialStep": "none",
            "onboardAccessibility": "false", "narrator": "0", "soundDevice": '""',
            "soundCategory_master": "0.0", "soundCategory_weather": "0.0", "lang": "tr_tr"}
    for kategori in ("music", "record", "ambient", "block", "hostile", "neutral", "player", "voice"):
        ayar[f"soundCategory_{kategori}"] = "0.0"
    satirlar = [s for s in satirlar if s.split(":", 1)[0] not in ayar] + [f"{k}:{v}" for k, v in ayar.items()]
    yol.write_text("\n".join(satirlar) + "\n", encoding="utf-8")


def kos(kanit, otomasyon="radar", gradle_ek=("-PquickPlay=test", "-PoyunTestiAfeti=!deprem,!sinkhole,!kasirga,!gunes_patlamasi"),
        wav=WAV):
    """İstemciyi protokolle açar, kapanışına kadar bekler, kanıtları `kanit`'a kopyalar; koşu geçerli ve senaryo
    tamamlandıysa True. `run/options.txt` koşudan önce saklanır ve sonunda bayt bayt geri yazılır (U-17)."""
    kanit = Path(kanit).resolve()
    kanit.mkdir(parents=True, exist_ok=True)
    log = Gunluk(kanit / "pencere-radar.log")

    # Ağır süreç kapısı (görev: en çok 20 dk). Başka runClient'ı kilit bekler (K0-21 rev. 2 (k)); pgrep yalnız bilgi.
    for _ in range(20):
        if gradle_sayisi() < 2 and bos_ram_gib() >= 4:
            break
        log(f"ağır süreç kapısı: gradle={gradle_sayisi()} boş={bos_ram_gib():.1f} GiB istemci={baska_istemci()}; 60 sn")
        time.sleep(60)
    else:
        log("kapı 20 dk'da açılmadı; koşu iptal")
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


def _pencere_dogrula(log, java_pid, basla):
    """(b) belirteç ve (c) pencere doğrulaması (Hyprland); tutmazsa istemci öldürülür ve False döner."""
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
    return True



def _kos(kanit, log, otomasyon, gradle_ek, wav):
    if not dunyayi_hazirla(log):
        log("Quick Play dünyası run/saves/test yok; iptal")
        return False
    secenekleri_hazirla()
    for eski in (RUN / "radar-kanit").glob("*") if (RUN / "radar-kanit").exists() else []:
        eski.unlink()
    (RUN / "radar-kanit").mkdir(parents=True, exist_ok=True)
    for eski in (RUN / "screenshots").glob("radar-*.png") if (RUN / "screenshots").exists() else []:
        eski.unlink()
    istemci_log = kanit / f"runClient-{otomasyon}.log"
    wav.unlink(missing_ok=True)
    ALSOFT_CONF.write_text(f"[general]\ndrivers = wave\nfrequency = 48000\nchannels = stereo\nsample-type = int16\n"
                           f"[wave]\nfile = {wav}\n", encoding="utf-8")
    pc2 = runclient_kilit.ekran_pc2()
    BASLATMA_BETIGI.write_text(
        f"#!/bin/sh\ncd '{KOK}' || exit 1\nexport JAVA_HOME='{JAVA_HOME}' DISPLAY={os.environ.get('DISPLAY') if pc2 else ':0'} "
        f"ALSOFT_DRIVERS=wave ALSOFT_CONF='{ALSOFT_CONF}'\n"
        f"exec python3 tools/senaryo/runclient_kilit.py kos '{KOK}' --bekle 1800 -- "
        f"timeout {TOPLAM_SURE} ./gradlew runClient --no-daemon --offline --console=plain "
        f"-Potomasyon={otomasyon} {' '.join(gradle_ek)} > '{istemci_log}' 2>&1\n", encoding="utf-8")

    if pc2:
        log(f"gradle={gradle_sayisi()} boş={bos_ram_gib():.1f} GiB")
        runclient_kilit.pc2_baslat(BASLATMA_BETIGI, log)
        sahip_adres = None
    else:
        sahibin = json.loads(hyprctl("activewindow", "-j") or "{}")
        sahip_adres = sahibin.get("address")
        log(f"(a) sahibin etkin penceresi: {sahip_adres} {sahibin.get('class')}; gradle={gradle_sayisi()} "
            f"boş={bos_ram_gib():.1f} GiB")
        kural = f"[workspace {WORKSPACE} silent; no_initial_focus; no_focus on; focus_on_activate off]"
        sonuc = hyprctl("dispatch", f'hl.dsp.exec_cmd("{kural} sh {BASLATMA_BETIGI}")')
        log(f"(a) exec kuralıyla başlatıldı: {kural} -> {sonuc}")
    basla = time.monotonic()

    java_pid = None
    jvm_sn = 900 if pc2 else 300
    while time.monotonic() - basla < jvm_sn and java_pid is None:
        time.sleep(0.5)
        java_pid = istemci_jvm()
    if java_pid is None:
        log(f"istemci JVM'i {jvm_sn} sn'de görünmedi; iptal")
        oldur(log, None, "JVM yok")
        return False
    if pc2:
        log(f"(b) istemci JVM pid={java_pid} ({time.monotonic() - basla:.1f} sn); pc2: belirteç ve pencere denetimi yok")
    elif not _pencere_dogrula(log, java_pid, basla):
        return False

    gecerli = True
    yoklama = 0
    cihaz = None
    bekci = runclient_kilit.KapanisBekcisi(KOK, java_pid, (istemci_log,), "[radar-otomasyon] kapanıyor; girdi sayıları", log)
    while not bekci.bitti_mi():
        if time.monotonic() - basla > TOPLAM_SURE:
            oldur(log, java_pid, "toplam süre aşıldı")
            gecerli = False
            break
        etkin = {} if pc2 else json.loads(hyprctl("activewindow", "-j") or "{}")
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
    girdi = len(re.findall(r"\[radar-otomasyon\] GİRDİ", metin))
    if pc2:
        log(runclient_kilit.pc2_girdi_notu(girdi))
    else:
        log(f"(e) oyun içi girdi olayı: {girdi}")
        if girdi:
            gecerli = False
    for png in sorted((RUN / "screenshots").glob("radar-*.png")):
        shutil.copy2(png, kanit / png.name)
        log(f"kanıt: {png.name}")
    for dosya in sorted((RUN / "radar-kanit").glob("*")):
        shutil.copy2(dosya, kanit / dosya.name)
        log(f"kanıt: {dosya.name}")
    tamam = re.search(r"\[radar-otomasyon\] kapanıyor; girdi sayıları", metin) is not None
    log(f"(g) senaryo son adıma ulaştı: {'evet' if tamam else 'hayır (tamamlanmadı)'}")
    log(f"koşu {'GEÇERLİ' if gecerli else 'GEÇERSİZ'}" + ("" if tamam else ", senaryo tamamlanmadı"))
    return gecerli and tamam


def main():
    kanit = sys.argv[1] if len(sys.argv) > 1 else str(KOK_ANA / ".vibe/kanit/radar-sismograf/gorsel")
    return 0 if kos(kanit) else 1


if __name__ == "__main__":
    sys.exit(main())
