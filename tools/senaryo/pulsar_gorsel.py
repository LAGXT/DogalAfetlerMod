#!/usr/bin/env python3
"""Pulsar-2'nin görsel kanıtı (K0-21 rev. 2 ekran protokolü, kilit (k); pulsar-2 PL-1…PL-5 kareleri).

Kullanım (worktree kökünden): python3 tools/senaryo/pulsar_gorsel.py [<kanıt klasörü>]
  pc2'de görünür istemciyle: tools/senaryo/pc2_istemci.sh <kanıt klasörü>/ pulsar_gorsel.py <kanıt klasörü>
Kanıt klasörü verilmezse KOK/.vibe/kanit/pulsar-2/gorsel-4 (tur 4).

Tek oyunculu Quick Play dünyası `run/saves/pulsar` her koşuda KOK/run/saves/test'ten (seed 20261105, allowCommands=1) taze
kopyalanır; istemci `-Potomasyon=pulsar` ile açılır ve `client.gokyuzu.PulsarOtomasyonu` adımlarını yürütür (büyük
pulsar, iki ardışık titreşim karesi, 0,5 sn içinde 6 karelik titreşim serisi, blok hasarı açık 3 eşdeğer günlük kavurma
sonrası cehennem + meteor izi, 7x7 hava kuyusunda ses derinliği ölçümü, pulsar gecesi, ~30° yandan 3B paralaks karesi,
kara delik yutmasının beş anı tam evre saniyesinde (mercek karesi FOV 30) ve Ultra kademede bir dörtgen ölçümü karesi,
güneşsiz buzul, kızıl cüce doğuşu, eski güneşin dönüşü). Her kare satırı FPS, TPS, mspt, evre, evre saniyesi, kül
yağışı, uçan meteor, 3B dörtgen sayısı (Pulsar3B + KaraDelik3B), kavurmanın eşdeğer günü, titreşim ofseti, çekirdek
merkez pikseli, FOV'u ve efekt kademesini taşır; betik bunları `kareler-<no>.tsv`'ye, `PULSAR_SES` satırlarını
`ses.tsv`'ye, `PULSAR_TITRESIM` satırlarını `titresim.tsv`'ye yazar ve denetler: her karenin evresi (kara delik kareleri
`karadelik_yutma`, kızıl cüce ilerlemesi >= 0,3, cehennemde
kül yağışı > 0, uçan meteor >= 1 ve eşdeğer gün >= 3, mercek FOV 30, ölçüm karesi ULTRA), ses ölçümleri (açık gök
kuyusunda y 64 ya da yüzey+1 / 48 / 16 / -20 ve üstü kapalı tünel), titreşim serisi (6 kare, 500 ms içinde, her karede
çekirdek pikseli). K-E29: ana makinede her kare FPS >= 20; pc2'de FPS geçersizdir, yalnız bilgi olarak yazılır.

Protokol (runclient_kilit.py ortak yolu): (a) açılış yalnız Hyprland exec kuralıyla ([workspace 93 silent;
no_initial_focus; no_focus on; focus_on_activate off]); (b) JVM'de HL_EXEC_RULE_TOKEN yoksa öldürülür; (c) pencere
no_focus=true ve ws 93 doğrulanır, tutmazsa öldürülür; (d) 5 sn'de bir bekçi: etkin pencere bizimse ALARM, süreç
öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz; (e) oyun içi girdi sayıları kapanış satırından okunur,
sıfırdan farklıysa koşu geçersiz; (k) runClient `runclient_kilit.py kos` ile (aynı anda tek istemci), kilit satırları
protokole kopyalanır. pc2 kipinde (`DOGALAFETLER_EKRAN=pc2`) (b)–(e) uygulanmaz. Kare eksik, bayat (iki kare bayt bayt
aynı) ya da yanlış evredeyse en çok bir tekrar. Test defteri ve kuyruk `defter.sarmala` ile (K-E51, K-E60): iş dışından
çağrı kendini istemci işi olarak kuyruğa ekler ve bekler.
Sahibin oturumunda tıklama/odak/workspace değişimi yok. Sayılar ve adımlar <kanıt>/protokol.log'a, kareler <kanıt>/kareler/'e.
Çıkış: 0 geçerli ve tam, 1 geçersiz/eksik/FPS < 20, 2 ağır süreç kapısı açılmadı.
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
import defter  # noqa: E402
import runclient_kilit  # noqa: E402

WS = 93
W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
KANIT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else KOK / ".vibe/kanit/pulsar-2/gorsel-4"
KARELER = KANIT / "kareler"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
ISARET = "PULSAR_OTOMASYON"
ONEK = "pulsar2_"
FPS_ESIGI = 20
SERI = "titresim_seri"
SERI_KARE = 6
SERI_MS = 500
CEHENNEM_GUN = 3.0
MERCEK_FOV = 30
SES_Y = [48, 16, -20]
BEKLENEN = ["pulsar_buyuk", "pulsar_titresim_a", "pulsar_titresim_b", *(f"{SERI}_{i}" for i in range(SERI_KARE)),
            "pulsar_cehennem", "pulsar_gece", "pulsar_3b_yan", "karadelik_yaklasma", "karadelik_mercek",
            "karadelik_spagetti", "karadelik_ultra", "karadelik_parlama", "karadelik_karanlik", "gunessiz_buz",
            "kizil_cuce_dogus", "gunes_dondu"]
EVRE = {"pulsar_buyuk": "pulsar", "pulsar_titresim_a": "pulsar", "pulsar_titresim_b": "pulsar",
        **{f"{SERI}_{i}": "pulsar" for i in range(SERI_KARE)}, "pulsar_cehennem": "pulsar", "pulsar_gece": "pulsar",
        "pulsar_3b_yan": "pulsar", "karadelik_yaklasma": "karadelik_yutma", "karadelik_mercek": "karadelik_yutma",
        "karadelik_spagetti": "karadelik_yutma", "karadelik_ultra": "karadelik_yutma",
        "karadelik_parlama": "karadelik_yutma", "karadelik_karanlik": "karadelik_yutma", "gunessiz_buz": "gunessiz",
        "kizil_cuce_dogus": "kizil_cuce", "gunes_dondu": "normal"}
KARE_SATIRI = re.compile(rf"{ISARET} kare {ONEK}(\w+)\.png \(FPS (\d+), TPS ([\d.?]+), mspt ([\d.?]+), evre (\w+), "
                         r"ilerleme ([\d.]+), evre sn ([\d.]+), sabit sn ([\d.-]+), kül yağışı (\d+), uçan meteor (\d+), "
                         r"parçacık (\d+), dönüş yolu (\w+), blok hasarı (\w+), güneş döndü (\w+), "
                         r"dortgen=(\d+) \(pulsar (\d+) \+ kara delik (\d+)\), eşdeğer gün ([^,]+), "
                         r"ofset ([-\d.]+) ([-\d.]+), çekirdek px ([-\d.]+) ([-\d.]+), FOV (\d+), kalite (\w+)")
SUTUNLAR = ["kare", "FPS", "TPS", "mspt", "evre", "ilerleme", "evre_sn", "sabit_sn", "kul_yagisi", "ucan_meteor",
            "parcacik", "donus_yolu", "blok_hasari", "gunes_dondu", "dortgen", "dortgen_pulsar", "dortgen_karadelik",
            "esdeger_gun", "ofset_x", "ofset_y", "px_x", "px_y", "FOV", "kalite"]
SES_SATIRI = re.compile(r"PULSAR_SES (kapali )?y=([-\d.]+) k=([\d.]+) kuru=([\d.]+) yankı=([\d.]+) gök=([\d.]+) "
                        r"hedef=(-?\d+)")
SES_SUTUNLARI = ["olcum", "y", "k", "kuru", "yanki", "gok", "hedef"]
TITRESIM_KARESI = re.compile(r"PULSAR_TITRESIM kare=(\w+) t_ms=([\d.]+) ofset_x=([-\d.]+) ofset_y=([-\d.]+) "
                             r"px_x=([-\d.]+) px_y=([-\d.]+)")
TITRESIM_SERISI = re.compile(r"PULSAR_TITRESIM seri ad=(\w+) n=(\d+) ofset_x=(\S+) ofset_y=(\S+) px_x=(\S+) "
                             r"px_y=(\S+) aralik_ms=(\S*) toplam_ms=([\d.]+)")
TITRESIM_SUTUNLARI = ["satir", "ad", "t_ms", "ofset_x", "ofset_y", "px_x", "px_y", "aralik_ms"]
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
    hedef = run / "saves/pulsar"
    if hedef.exists():
        shutil.rmtree(hedef)
    shutil.copytree(KOK / "run/saves/test", hedef, ignore=shutil.ignore_patterns("session.lock"))
    yol = run / "options.txt"
    satirlar = yol.read_text().splitlines() if yol.exists() else []
    satirlar = [s for s in satirlar if not s.startswith(("tutorialStep:", "pauseOnLostFocus:", "renderDistance:",
                                                         "simulationDistance:"))]
    yol.write_text("\n".join(satirlar + ["tutorialStep:none", "pauseOnLostFocus:false", "renderDistance:12",
                                         "simulationDistance:10"]) + "\n")
    for eski in (run / "screenshots").glob(f"{ONEK}*.png"):
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


def kare_denetimi(satirlar):
    """Kare satırlarının evre/içerik denetimi ve K-E29 FPS denetimi. Döner: (yanlış kareler, FPS < 20 kareler)."""
    yanlis, dusuk = [], []
    for s in satirlar:
        kare, evre = s["kare"], s["evre"]
        if kare in EVRE and evre != EVRE[kare]:
            yanlis.append(f"{kare}: evre {evre} (beklenen {EVRE[kare]})")
        if kare == "kizil_cuce_dogus" and float(s["ilerleme"]) < 0.3:
            yanlis.append(f"{kare}: doğuş ilerlemesi {s['ilerleme']} < 0,3")
        if kare == "pulsar_cehennem" and (int(s["kul_yagisi"]) == 0 or int(s["ucan_meteor"]) == 0):
            yanlis.append(f"{kare}: kül yağışı {s['kul_yagisi']}, uçan meteor {s['ucan_meteor']} (ikisi de > 0 olmalı)")
        if kare == "pulsar_cehennem" and not sayi(s["esdeger_gun"]) >= CEHENNEM_GUN:
            yanlis.append(f"{kare}: eşdeğer gün {s['esdeger_gun']} < {CEHENNEM_GUN:g}")
        if kare == "karadelik_mercek" and int(s["FOV"]) != MERCEK_FOV:
            yanlis.append(f"{kare}: FOV {s['FOV']} (beklenen {MERCEK_FOV})")
        if kare == "karadelik_ultra" and s["kalite"] != "ULTRA":
            yanlis.append(f"{kare}: kalite {s['kalite']} (beklenen ULTRA)")
        if kare in ("gunessiz_buz", "kizil_cuce_dogus") and s["donus_yolu"] != "true":
            yanlis.append(f"{kare}: dönüş yolu {s['donus_yolu']} (pulsar yolu bekleniyordu)")
        if int(s["FPS"]) < FPS_ESIGI:
            dusuk.append(f"{kare} {s['FPS']}")
    return yanlis, dusuk


def sayi(metin):
    try:
        return float(metin)
    except ValueError:
        return float("nan")


def ses_denetimi(sesler):
    """Açık gök kuyusunda 4 ölçüm (y 64 ya da yüzey+1, sonra 48 / 16 / -20) ve bir üstü kapalı tünel ölçümü."""
    acik = [s for s in sesler if s["olcum"] == "acik"]
    kapali = [s for s in sesler if s["olcum"] == "kapali"]
    yanlis = []
    hedefler = [int(s["hedef"]) for s in acik]
    if len(acik) != 4 or hedefler[1:] != SES_Y or hedefler[0] > 64:
        yanlis.append(f"ses: açık gök ölçümleri hedef {hedefler} (beklenen [64 ya da yüzey+1] + {SES_Y})")
    if len(kapali) != 1:
        yanlis.append(f"ses: üstü kapalı ölçüm {len(kapali)} (beklenen 1)")
    return yanlis


def titresim_denetimi(seriler, kareler):
    """Ps6: titresim_seri 6 kare, ilk ile son kare arası <= 500 ms, her karede ofset ve çekirdek pikseli."""
    seri = [s for s in seriler if s["ad"] == SERI]
    if not seri:
        return [f"titreşim: {SERI} seri satırı yok"]
    s = seri[-1]
    yanlis = []
    if int(s["n"]) != SERI_KARE:
        yanlis.append(f"titreşim: seri {s['n']} kare (beklenen {SERI_KARE})")
    if not sayi(s["t_ms"]) <= SERI_MS:
        yanlis.append(f"titreşim: seri {s['t_ms']} ms (beklenen <= {SERI_MS})")
    eksik = [k["ad"] for k in kareler if k["ad"].startswith(SERI) and "-" in (k["ofset_x"], k["px_x"])]
    if eksik:
        yanlis.append(f"titreşim: ofset ya da çekirdek pikseli yok {eksik}")
    return yanlis


def kosu(no):
    """Bir istemci koşusu. Döner: (geçerli: alarm 0 ve girdi 0, kareler tam/taze/doğru evrede, FPS kuralı, açıklama)."""
    hazirla()
    istemci_log = KANIT / f"runClient-{no}.log"
    betik = Path("/tmp/pulsar_istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{W}'\n"
                     f"export JAVA_HOME='{JAVA_HOME}'\n"
                     f"exec python3 tools/senaryo/runclient_kilit.py kos '{W}' --bekle 1800 -- "
                     "timeout 3000 ./gradlew runClient --offline --no-daemon --console=plain -PquickPlay=pulsar "
                     f"-Potomasyon=pulsar > '{istemci_log}' 2>&1\n")
    sahip_adres = runclient_kilit.istemci_baslat(betik, WS, log)
    baslangic = time.time()
    pid = None
    while time.time() - baslangic < 2100 and pid is None:
        pid = istemci_pid()
        time.sleep(1)
    if pid is None:
        log("istemci JVM'i 35 dk'da görünmedi (kilit beklemesi dahil)")
        return False, False, False, "JVM yok"
    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        oldur(pid, neden)
        return False, False, False, neden
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
    for p in sorted((W / "run/screenshots").glob(f"{ONEK}*.png")):
        ad = p.stem.removeprefix(ONEK)
        hedef = KARELER / f"{ad}-{no}.png"
        shutil.copy2(p, hedef)
        kareler[ad] = hashlib.md5(p.read_bytes()).hexdigest()
        log("kare:", hedef.name, "md5", kareler[ad][:12])
    metin = (KANIT / f"latest-{no}.log").read_text(errors="replace") if latest.exists() else ""
    satirlar = [dict(zip(SUTUNLAR, m.groups())) for m in KARE_SATIRI.finditer(metin)]
    tablo = ["	".join(SUTUNLAR)] + ["	".join(s[k] for k in SUTUNLAR) for s in satirlar]
    (KANIT / f"kareler-{no}.tsv").write_text("\n".join(tablo) + "\n", encoding="utf-8")
    for satir in tablo:
        log("tablo:", satir)
    sesler = [dict(zip(SES_SUTUNLARI, ("kapali" if m.group(1) else "acik",) + m.groups()[1:]))
              for m in SES_SATIRI.finditer(metin)]
    ses_tablo = ["\t".join(["kosu"] + SES_SUTUNLARI)] + ["\t".join([str(no)] + [s[k] for k in SES_SUTUNLARI])
                                                      for s in sesler]
    (KANIT / "ses.tsv").write_text("\n".join(ses_tablo) + "\n", encoding="utf-8")
    titresim_kareleri = [dict(zip(TITRESIM_SUTUNLARI, ("kare",) + m.groups() + ("",)))
                         for m in TITRESIM_KARESI.finditer(metin)]
    seriler = [dict(zip(["ad", "n", "ofset_x", "ofset_y", "px_x", "px_y", "aralik_ms", "t_ms"], m.groups()))
               for m in TITRESIM_SERISI.finditer(metin)]
    titresim_tablo = ["\t".join(["kosu"] + TITRESIM_SUTUNLARI)]
    titresim_tablo += ["\t".join([str(no)] + [k[c] for c in TITRESIM_SUTUNLARI]) for k in titresim_kareleri]
    titresim_tablo += ["\t".join([str(no), "seri"] + [s[c] for c in TITRESIM_SUTUNLARI[1:]]) for s in seriler]
    (KANIT / "titresim.tsv").write_text("\n".join(titresim_tablo) + "\n", encoding="utf-8")
    for satir in ses_tablo[1:] + titresim_tablo[1:]:
        log("ölçüm:", satir)
    for satir in metin.splitlines():
        if f"{ISARET} " in satir and any(k in satir for k in ("zaman aşımı", "cehennem:", "en düşük TPS", "meteor kameranın",
                                                            "kadraj ", "3B yan:", "ses kuyusu:", "kavurma birikimi:",
                                                            "kamera ayarı:", "gök kamerası:")):
            log("otomasyon:", satir.split(f"{ISARET} ", 1)[1])
        elif "kara delik yutma patlaması" in satir or "[pulsar] yutma sesi" in satir:
            log("yutma:", satir.strip())
        elif "PULSAR_SES " in satir and "yok:" in satir:
            log("ses:", satir.split("PULSAR_SES ", 1)[1])
    sayim = girdi_sayilari((latest, istemci_log))
    if sayim is None:
        log(f"koşu {no} GEÇERSİZ: girdi sayıları satırı yok")
        return False, False, False, "girdi satırı yok"
    kaynak, satir_no, sayilar = sayim
    sifir = sayilar == "tuş=0 fare=0 tekerlek=0"
    sifir = runclient_kilit.girdi_ozeti(sifir, f"koşu {no} girdi sayıları: {sayilar} ({kaynak.name}:{satir_no})", log)
    eksik = [k for k in BEKLENEN if k not in kareler]
    ayni = sorted({a for a in kareler for b in kareler if a < b and kareler[a] == kareler[b]})
    yanlis, dusuk = kare_denetimi(satirlar)
    yanlis += ses_denetimi(sesler) + titresim_denetimi(seriler, titresim_kareleri)
    if eksik:
        log(f"koşu {no}: eksik kare {eksik}")
    if ayni:
        log(f"koşu {no}: bayat kare (bayt bayt aynı) {ayni}")
    for y in yanlis:
        log(f"koşu {no}: yanlış kare {y}")
    fps_tamam = True
    if dusuk and runclient_kilit.ekran_pc2():
        log(f"koşu {no}: bilgi (pc2, MAKINE={runclient_kilit.makine()}; FPS geçersiz, K-E29 b): FPS < {FPS_ESIGI} kareler {dusuk}")
    elif dusuk:
        log(f"koşu {no}: K-E29 FPS < {FPS_ESIGI} kareler {dusuk}")
        fps_tamam = False
    else:
        log(f"koşu {no}: K-E29 bütün kareler FPS >= {FPS_ESIGI}")
    tam = not eksik and not ayni and not yanlis
    return sifir and not alarm, tam, fps_tamam, "tamam" if tam else "kare eksik/bayat/yanlış evre"


def main():
    if not kapi():
        log("kapı 20 dk'da açılmadı")
        return 2
    for deneme in (1, 2):
        no = kosu_no()
        gecerli, tam, fps_tamam, aciklama = kosu(no)
        log(f"koşu {no} sonuç: geçerli={gecerli} kareler tam={tam} FPS kuralı={fps_tamam} ({aciklama})")
        if not gecerli:
            return 1
        if tam:
            return 0 if fps_tamam else 1
        if deneme == 1:
            log("kare eksik, bayat ya da yanlış evrede: bir tekrar")
    return 1


if __name__ == "__main__":
    sys.exit(defter.sarmala(main, KANIT))
