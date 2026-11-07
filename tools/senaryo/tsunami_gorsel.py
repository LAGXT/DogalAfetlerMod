#!/usr/bin/env python3
"""Tsunami görsel kanıt koşusu (S5/S1 vitrin, mağara suyu, FPS/TPS): K0-21 rev. 2 ekran protokolü (docs/KARARLAR.md).

(a) İstemci yalnız Hyprland exec kuralıyla açılır: `[workspace 94 silent; no_initial_focus; no_focus on; focus_on_activate off]`.
(b) Belirteç kapısı: istemci JVM'i (cwd = worktree `run/`) görününce ortamında `HL_EXEC_RULE_TOKEN` aranır; yoksa öldürülür.
(c) Pencere doğrulaması: pid'in penceresine `set_prop no_focus=1`, `focus_on_activate=0` (lock) yazılır; `getprop no_focus`
    true ve workspace 94 değilse öldürülür.
(d) Bekçi: sahibin etkin penceresi kaydedilir; 5 sn'de bir `hyprctl activewindow -j`; etkin pencere bizim pid'imizse ALARM:
    süreç öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz.
(e) Oyun içi girdi günlüğü (`client.tsunami.TsunamiOtomasyonu`): `TSUNAMI-OTOMASYON GIRDI` satırları sayılır; sıfırdan
    farklıysa geçersiz.
(i) Ağır süreç kapısı ve aynı anda tek runClient.

Kullanım (worktree kökünden): python3 tools/senaryo/tsunami_gorsel.py <kanıt klasörü> [bölüm: hepsi|s5|s1|magara|kabul|...]
Çıkış: 0 geçerli koşu, 1 protokol ihlali/iptal, 2 istemci hatası. FPS pencereleri varsa sonda `TABLO` satırları (Ts1 kabulü, tur 4).
"""
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
RUN = KOK / "run"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
WS = 94
KURAL = f"[workspace {WS} silent; no_initial_focus; no_focus on; focus_on_activate off]"
# İstemcinin kendi kapanış bekçisi (TsunamiOtomasyonu, 30 sn) tutmazsa BITTI'den bu kadar sn sonra artıklar öldürülür.
KAPANIS_SN = 90


def sh(komut, zaman=30):
    r = subprocess.run(komut, shell=isinstance(komut, str), capture_output=True, text=True, timeout=zaman)
    return (r.stdout + r.stderr).strip()


def hypr_json(ne):
    try:
        return json.loads(sh(["hyprctl", ne, "-j"]))
    except (json.JSONDecodeError, subprocess.TimeoutExpired):
        return None


class Kayit:
    def __init__(self, yol):
        self.f = yol.open("a", encoding="utf-8")

    def __call__(self, metin):
        satir = f"{time.strftime('%H:%M:%S')} {metin}"
        print(satir, flush=True)
        self.f.write(satir + "\n")
        self.f.flush()


def kapi(log):
    for _ in range(31):
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


def belirtec_var(pid):
    try:
        return b"HL_EXEC_RULE_TOKEN=" in (Path("/proc") / str(pid) / "environ").read_bytes()
    except OSError:
        return False


def pencere(pid):
    for c in hypr_json("clients") or []:
        if c.get("pid") == pid and c.get("mapped", True):
            return c
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


def artiklari_oldur(pid, log):
    """Kapanışı asılı istemci JVM'i ve onu başlatan tek kullanımlık Gradle daemon'ı (`--no-daemon`; `timeout` yalnız Gradle
    istemcisini öldürür, daemon ve oyun yetim kalır). Yalnız bu worktree'nin süreçleri: daemon'ın cwd'si KOK olmalı."""
    hedef = [pid]
    try:
        ata = int((Path("/proc") / str(pid) / "stat").read_text().rsplit(")", 1)[1].split()[1])
        if (os.readlink(f"/proc/{ata}/cwd") == str(KOK)
                and b"GradleDaemon" in Path(f"/proc/{ata}/cmdline").read_bytes()):
            hedef.append(ata)
    except (OSError, ValueError, IndexError):
        pass
    olen = []
    for h in hedef:
        try:
            os.kill(h, signal.SIGKILL)
            olen.append(h)
        except ProcessLookupError:
            pass
    log(f"artıklar öldürüldü: {olen}")


def _sayi(desen, metin, tur=float):
    r = re.search(desen, metin)
    return tur(r.group(1)) if r else None


def fps_tablosu(metin, log):
    """Ts1 kabulü (şef kararı, tur 4): her S5 sahnesinde "sonra" tabanı (aynı sahne, stop'tan 200 tick sonra) / karada ortanca
    kare süresi oranı ≥ 0,5 geçer; "önce" tabanı bilgi. Ölçüm geçerli ancak karada ve sonra pencerelerinde sunucu mspt < 50
    ve yabancı GradleWrapperMain (kendi runClient'ımız dışında) ≤ 1 ve sonra tabanı temiz (parçacık ≤ önce + 100) ise.
    Görünür bölüm (önce -> karada -> sonra) bilgi. K26-1: karada `sicrama_varlik_sn` > 1 ise sahne "K-E26b bekliyor".
    Sonuç (geçerli, hepsi geçti)."""
    sahne = {}
    for m in re.finditer(r"TSUNAMI-OTOMASYON FPS (\S+)-(afetsiz|karada|sonra) (.*)", metin):
        a = m.group(3)
        sahne.setdefault(m.group(1), {})[m.group(2)] = {
            "ms": _sayi(r"ortanca_kare_ms=([\d.]+)", a), "cpu": _sayi(r"cpu_kare_ms=([\d.]+)", a),
            "mspt": _sayi(r"sunucu_mspt=([\d.]+)", a), "yabanci": _sayi(r"yabanci=(\d+)", a, int),
            "bolum": _sayi(r"bolum=\[C: (\d+)/", a, int), "parcacik": _sayi(r"parcacik=(\d+)", a, int),
            "varlik": _sayi(r"varlik=(\d+)", a, int), "suda": _sayi(r"suda_ort=([\d.]+)", a),
            "sicrama": _sayi(r"sicrama_varlik_sn=([\d.]+)", a), "bubble": _sayi(r"bubble_varlik_tick=([\d.]+)", a),
            "splash": _sayi(r"splash_varlik_tick=([\d.]+)", a),
            "parcacik_tur": (re.search(r"parcacik_tur=\[([^]]*)\]", a) or [None, None])[1]}
    hepsi_gecerli = hepsi_gecti = bool(sahne)
    for ad, p in sahne.items():
        o, k, s = p.get("afetsiz"), p.get("karada"), p.get("sonra")
        if not o or not k or not s or not k["ms"] or not s["ms"]:
            log(f"TABLO {ad} eksik pencere: {sorted(p)}")
            hepsi_gecerli = hepsi_gecti = False
            continue
        neden = [f"{w}: mspt={p[w]['mspt']}" for w in ("karada", "sonra") if p[w]["mspt"] is None or p[w]["mspt"] >= 50]
        neden += [f"{w}: yabanci_gradle={p[w]['yabanci']}" for w in ("karada", "sonra")
                  if p[w]["yabanci"] is None or p[w]["yabanci"] > 1]
        if (s["parcacik"] or 0) > (o["parcacik"] or 0) + 100:
            neden.append(f"sonra temiz değil: parcacik={s['parcacik']}")
        gecerli = not neden
        oran = s["ms"] / k["ms"]
        gecti = oran >= 0.5
        k26b = k["sicrama"] is not None and k["sicrama"] > 1
        hepsi_gecerli &= gecerli
        hepsi_gecti &= gecti
        log(f"TABLO {ad} once={o['ms']:.2f}ms karada={k['ms']:.2f}ms sonra={s['ms']:.2f}ms oran_sonra={oran:.0%} "
            f"cpu_oran_sonra={s['cpu'] / k['cpu']:.0%} oran_once={o['ms'] / k['ms']:.0%} "
            f"mspt={o['mspt']}/{k['mspt']}/{s['mspt']} yabanci_gradle={o['yabanci']}/{k['yabanci']}/{s['yabanci']} "
            f"bolum={o['bolum']}->{k['bolum']}->{s['bolum']} parcacik={o['parcacik']}->{k['parcacik']}->{s['parcacik']} "
            f"varlik={o['varlik']}->{k['varlik']}->{s['varlik']} suda_ort={k['suda']} sicrama_varlik_sn={k['sicrama']} "
            f"bubble_varlik_tick={k['bubble']} splash_varlik_tick={k['splash']} "
            f"gecerli={'E' if gecerli else 'H ' + '; '.join(neden)} sonuc={'GECTI' if gecti else 'KALDI'}"
            f"{' K-E26b-bekliyor' if k26b else ''} | karada_parcacik_tur=[{k['parcacik_tur']}]")
    if sahne:
        log(f"TABLO ozet: gecerli={'E' if hepsi_gecerli else 'H'} hepsi_gecti={'E' if hepsi_gecti else 'H'}")
    return hepsi_gecerli, hepsi_gecti


def odak_ver(adres, log):
    for ifade in (f'hl.dsp.focus({{ window = "address:{adres}" }})', f'hl.dsp.window.focus({{ window = "address:{adres}" }})'):
        log(f"odak geri: {ifade} -> {sh(['hyprctl', 'dispatch', ifade])}")


def main():
    kanit = Path(sys.argv[1]).resolve()
    bolum = sys.argv[2] if len(sys.argv) > 2 else "hepsi"
    kanit.mkdir(parents=True, exist_ok=True)
    log = Kayit(kanit / "pencere.log")
    if not kapi(log):
        log("kapı 30 dk açılmadı; koşu yapılmadı")
        return 1
    # Her koşu ana test dünyasının temiz kopyasıyla başlar (vitrin sahnesi gerçek blok kurar; sahne belirlenimci kalır).
    hedef = RUN / "saves" / "test"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK.parents[1] / "run" / "saves" / "test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    (hedef / "serverconfig" / "dogalafetler-server.toml").unlink(missing_ok=True)
    log(f"test dünyası temiz kopyalandı: {hedef}")
    sahip = hypr_json("activewindow") or {}
    sahip_adres = sahip.get("address")
    log(f"sahibin etkin penceresi: {sahip_adres} {sahip.get('class')} ws={sahip.get('workspace', {}).get('id')}")
    oyun_log = kanit / f"runClient-{bolum}.log"
    betik = Path("/tmp/tsunami-istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{KOK}' || exit 9\n"
                     f"JAVA_HOME='{JAVA_HOME}' python3 tools/senaryo/runclient_kilit.py kos '{KOK}' --bekle 1800 -- "
                     f"timeout 1200 ./gradlew runClient --no-daemon --offline --console=plain "
                     f"-PquickPlay=test -Potomasyon=tsunami -PotomasyonBolum={bolum} > '{oyun_log}' 2>&1\n"
                     f"echo \"cikis=$?\" >> '{oyun_log}'\n")
    ifade = f'hl.dsp.exec_cmd("{KURAL} sh {betik}")'
    duvar_baslangic = time.time()
    log(f"exec kuralıyla başlatılıyor: {ifade} -> {sh(['hyprctl', 'dispatch', ifade])}")
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
    if not belirtec_var(pid):
        log(f"ALARM: pid={pid} ortamında HL_EXEC_RULE_TOKEN yok; pencere eşlenmeden öldürülüyor")
        os.kill(pid, signal.SIGKILL)
        return 1
    log(f"istemci JVM pid={pid} ({time.monotonic() - baslangic:.1f} sn); HL_EXEC_RULE_TOKEN ortamda: True")
    p = None
    while time.monotonic() - baslangic < 900:
        p = pencere(pid)
        if p:
            break
        if istemci_pid() is None:
            log("istemci pencere açmadan çıktı")
            return 2
        time.sleep(0.25)
    if not p:
        log("pencere 15 dk'da eşlenmedi")
        os.kill(pid, signal.SIGKILL)
        return 2
    sonuclar = []
    for prop, deger in (("no_focus", "1"), ("focus_on_activate", "0")):
        ifade = f'hl.dsp.window.set_prop({{ window = "pid:{pid}", prop = "{prop}", value = "{deger}", lock = true }})'
        sonuclar.append(sh(["hyprctl", "dispatch", ifade]))
    no_focus = sh(["hyprctl", "getprop", f"pid:{pid}", "no_focus"])
    p = pencere(pid) or p
    ws = p.get("workspace", {}).get("id")
    etkin = (hypr_json("activewindow") or {}).get("address")
    log(f"pencerem {p.get('address')} pid={pid} ws={ws}; set_prop -> {sonuclar}; getprop no_focus={no_focus}; etkin={etkin}")
    if "true" not in no_focus.lower() or ws != WS:
        log("ALARM: pencere doğrulaması tutmadı; süreç öldürülüyor")
        os.kill(pid, signal.SIGKILL)
        return 1
    yoklama = 0
    alarm = 0
    asili = False
    bitti_an = None
    while istemci_pid() == pid:
        time.sleep(5)
        yoklama += 1
        a = hypr_json("activewindow") or {}
        if a.get("pid") == pid:
            alarm += 1
            log(f"ALARM: etkin pencere benim istemcim ({a.get('address')}); süreç öldürülüyor, koşu geçersiz")
            os.kill(pid, signal.SIGKILL)
            if sahip_adres:
                odak_ver(sahip_adres, log)
            break
        if yoklama % 6 == 0:
            p = pencere(pid) or {}
            log(f"denetim: etkin={a.get('address')} {a.get('class')}; benim ws={p.get('workspace', {}).get('id')}; "
                f"gradle={sh('pgrep -c -f [G]radleWrapperMain')}")
        metin = oyun_log.read_text(encoding="utf-8", errors="replace")
        if bitti_an is None and "TSUNAMI-OTOMASYON BITTI" in metin:
            bitti_an = time.monotonic()
        if istemci_pid() != pid:
            break
        if "cikis=" in metin or bitti_an is not None and time.monotonic() - bitti_an > KAPANIS_SN:
            asili = True
            log(f"KAPANIS ASILI: BITTI'den {time.monotonic() - bitti_an:.0f} sn sonra istemci JVM'i yaşıyor"
                if bitti_an is not None else "KAPANIS ASILI: runClient döndü, istemci JVM'i yaşıyor")
            artiklari_oldur(pid, log)
            break
    son = time.monotonic() + 120
    while time.monotonic() < son and "cikis=" not in oyun_log.read_text(encoding="utf-8", errors="replace"):
        time.sleep(2)
    metin = oyun_log.read_text(encoding="utf-8", errors="replace")
    girdi = len(re.findall(r"TSUNAMI-OTOMASYON GIRDI", metin))
    bitti = re.search(r"TSUNAMI-OTOMASYON BITTI girdi tus=(\d+) fare=(\d+) teker=(\d+)", metin)
    cikis = re.search(r"cikis=(\d+)", metin)
    log(f"bitti: yoklama={yoklama} ALARM={alarm} girdi_satiri={girdi} kapanis_asili={'E' if asili else 'H'} "
        f"otomasyon_ozeti={bitti.groups() if bitti else None} gradle_{cikis.group(0) if cikis else 'cikis=?'}")
    desen = (r".*(?:TSUNAMI-OTOMASYON (?:FPS|FOTO|KARE|KALITE|AVCI|AYAR|KAPANIS)|Tsunami: |\[CHAT\]|"
             r"has (?:made|reached|completed) the).*")
    for satir in re.findall(desen, metin):
        log("  " + re.split(r"TSUNAMI-OTOMASYON |DogalAfetler/\]: |\]: ", satir, maxsplit=1)[-1])
    fps_tablosu(metin, log)
    for png in (RUN / "screenshots").glob("tsunami-*.png"):
        if png.stat().st_mtime > duvar_baslangic:
            shutil.copy2(png, kanit / png.name)
            log(f"kopyalandı: {png.name}")
    if alarm or girdi:
        return 1
    # Kapanışı asılı kalan istemcinin ölçümleri BITTI'den önce tamamdır; koşu geçerli, asılı kapanış log'da kalır.
    return 0 if bitti and cikis and (cikis.group(1) == "0" or asili) else 2


if __name__ == "__main__":
    sys.exit(main())
