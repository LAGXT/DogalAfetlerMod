#!/usr/bin/env python3
"""Tsunami görsel kanıt koşusu (S5/S1 vitrin, mağara suyu, FPS/TPS): K0-21 rev. 2 ekran protokolü (docs/KARARLAR.md).

(a) İstemci yalnız Hyprland exec kuralıyla açılır: `[workspace 94 silent; no_initial_focus; no_focus on; focus_on_activate off]`.
(b) Belirteç kapısı: istemci JVM'i (cwd = worktree `run/`) görününce ortamında `HL_EXEC_RULE_TOKEN` aranır; yoksa öldürülür.
(c) Pencere doğrulaması: pid'in penceresine `set_prop no_focus=1`, `focus_on_activate=0` (lock) yazılır; `getprop no_focus`
    true ve workspace 94 değilse öldürülür.
(d) Bekçi: sahibin etkin penceresi kaydedilir; 5 sn'de bir `hyprctl activewindow -j`; etkin pencere bizim pid'imizse ALARM:
    süreç öldürülür, odak kaydedilen pencereye verilir, koşu geçersiz. Kapanış K-E27
    `runclient_kilit.KapanisBekcisi` ile: istemcinin kendi kapanış bekçisi (`TsunamiOtomasyonu`, 30 sn `Runtime.halt`)
    tutmazsa `BITTI`'den ya da kilit bırakıldıktan 90 sn sonra yaşayan istemci ve tek kullanımlık Gradle daemon'ı
    öldürülür; ölçümler BITTI'den önce tamamdır.
(e) Oyun içi girdi günlüğü (`client.tsunami.TsunamiOtomasyonu`): `TSUNAMI-OTOMASYON GIRDI` satırları sayılır; sıfırdan
    farklıysa geçersiz.
(i) Ağır süreç kapısı ve aynı anda tek runClient.

Kullanım (worktree kökünden): python3 tools/senaryo/tsunami_gorsel.py <kanıt klasörü> [bölüm: hepsi|s5|s1|kopuk|magara|kabul|...]
`kopuk` (hepsi'de de): Ts2-1 kabul karesi, ayrı S5 koşusunda köpük yakından ve alçaktan (göz tepenin 1,5 blok üstünde, 10° aşağı, cepheye 8–12 blok;
`tsunami-s5-kopuk-yakin.png` Yüksek, `-yakin-dusuk.png` Düşük; `KAMERA kopuk` ve `DALGA` satırları özette). Ts7 kameraları değişmez.
Çıkış: 0 geçerli koşu ve ölçütler geçti, 1 protokol ihlali/iptal, 2 istemci hatası, 3 geçerli koşu ama bir kabul ölçütü kaldı
(`KABUL KALDI` satırı; defter `kaldi` yazar). Ölçütler: FPS pencereleri varsa sonda `TABLO` satırları (Ts1 kabulü, TS-16 rev. 3:
geçerli ölçüm ve her sahne geçti, CPU bütçesi dahil); `KARE` satırlarındaki serpinti/köpük konum sayıları `SERPINTI`
satırlarında (havada, blok içinde, çizimsiz su üstü 0); tsunami yağmuru `YAGMUR` satırlarında (ayar kapalıyken 0 damla, 0
halka; açıkken halka ölçütü).
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
import runclient_kilit  # noqa: E402

KOK = Path(__file__).resolve().parents[2]
RUN = KOK / "run"
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
# Workspace ortamdan değişir (aynı anda çalışan işçiler ayrı gizli workspace kullanır; K0-21 rev. 2).
WS = int(os.environ.get("DOGALAFETLER_WS", "94"))


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


def _sayi(desen, metin, tur=float):
    r = re.search(desen, metin)
    return tur(r.group(1)) if r else None


def fps_tablosu(metin, log):
    """Ts1 kabulü (TS-16 rev. 3, K0-33 tsunami istisnası): her S5 sahnesinde Yüksek'te "sonra" tabanı (aynı sahne, stop'tan
    200 tick sonra) < 10 ms ise mutlak ölçü: karada render thread CPU ms/kare ≤ 33,3; değilse sonra / karada oranı ≥ 0,5.
    Duvar saati FPS, "önce" ve "sonra" oranları bilgi (≥ 30 FPS duvar saati Faz 5 görünür koşu tetikleyicisi). Ölçüm geçerli
    ancak karada ve sonra pencerelerinde sunucu mspt < 50, yabancı GradleWrapperMain (kendi runClient'ımız dışında) ≤ 1 ve
    sonra tabanı temiz (parçacık ≤ önce + 100) ise. Kırılan blok ve tick başına en
    çok bölüm karada penceresinden sonraki ilk `Tsunami: temizlendi` satırından. K26-1: karada `sicrama_varlik_sn` > 1 ise
    sahne "K-E26b bekliyor" (bilgi; çıkış kodu geçerli ölçüm ve bütün sahnelerin geçmesini ister). Sonuç: geçerli ve hepsi geçti
    (bool) ya da FPS penceresi yoksa None."""
    sahne = {}
    temiz = [(m.start(), m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)) for m in re.finditer(
        r"Tsunami: temizlendi \(temiz=\w+, doğal=\w+\): kırılan (\d+) blok, değişen bölüm (\d+) "
        r"\(tick başına en çok (\d+) birim\), kirletilen bölüm (\d+), taranan \d+ sütun, bekleyen yıkım (\d+)", metin)]
    for m in re.finditer(r"TSUNAMI-OTOMASYON FPS (\S+)-(afetsiz|karada|sonra) (.*)", metin):
        a = m.group(3)
        w = sahne.setdefault(m.group(1), {})[m.group(2)] = {
            "ms": _sayi(r"ortanca_kare_ms=([\d.]+)", a), "cpu": _sayi(r"cpu_kare_ms=([\d.]+)", a),
            "mspt": _sayi(r"sunucu_mspt=([\d.]+)", a), "yabanci": _sayi(r"yabanci=(\d+)", a, int),
            "bolum": _sayi(r"bolum=\[C: (\d+)/", a, int), "parcacik": _sayi(r"parcacik=(\d+)", a, int),
            "varlik": _sayi(r"varlik=(\d+)", a, int), "suda": _sayi(r"suda_ort=([\d.]+)", a),
            "sicrama": _sayi(r"sicrama_varlik_sn=([\d.]+)", a), "bubble": _sayi(r"bubble_varlik_tick=([\d.]+)", a),
            "splash": _sayi(r"splash_varlik_tick=([\d.]+)", a), "pc": _sayi(r"pC: (\d+)", a, int),
            "pencere": (re.search(r"yikim=\[([^]]*)\]", a) or [None, None])[1],
            "parcacik_tur": (re.search(r"parcacik_tur=\[([^]]*)\]", a) or [None, None])[1]}
        if m.group(2) == "karada":
            sonraki = next((t for t in temiz if t[0] > m.start()), None)
            w["yikim"] = "/".join(sonraki[1:]) if sonraki else None
    hepsi_gecerli = hepsi_gecti = bool(sahne)
    bekleyen_disi = bool(sahne)
    for ad, p in sahne.items():
        o, k, s = p.get("afetsiz"), p.get("karada"), p.get("sonra")
        if not o or not k or not s or not k["ms"] or not s["ms"]:
            log(f"TABLO {ad} eksik pencere: {sorted(p)}")
            hepsi_gecerli = hepsi_gecti = bekleyen_disi = False
            continue
        neden = [f"{w}: mspt={p[w]['mspt']}" for w in ("karada", "sonra") if p[w]["mspt"] is None or p[w]["mspt"] >= 50]
        neden += [f"{w}: yabanci_gradle={p[w]['yabanci']}" for w in ("karada", "sonra")
                  if p[w]["yabanci"] is None or p[w]["yabanci"] > 1]
        if (s["parcacik"] or 0) > (o["parcacik"] or 0) + 100:
            neden.append(f"sonra temiz değil: parcacik={s['parcacik']}")
        gecerli = not neden
        oran = s["ms"] / k["ms"]
        mutlak = s["ms"] < 10
        gecti = k["cpu"] is not None and k["cpu"] <= 1000 / 30 if mutlak else oran >= 0.5
        k26b = k["sicrama"] is not None and k["sicrama"] > 1
        hepsi_gecerli &= gecerli
        hepsi_gecti &= gecti
        bekleyen_disi &= gecti or k26b
        log(f"TABLO {ad} once={o['ms']:.2f}ms karada={k['ms']:.2f}ms ({1000 / k['ms']:.1f} FPS) sonra={s['ms']:.2f}ms "
            f"cpu={o['cpu']}/{k['cpu']}/{s['cpu']}ms "
            f"olcu={'mutlak_cpu<=33.3ms' if mutlak else 'oran_sonra>=50%'} oran_sonra={oran:.0%} "
            f"cpu_oran_sonra={s['cpu'] / k['cpu']:.0%} oran_once={o['ms'] / k['ms']:.0%} "
            f"mspt={o['mspt']}/{k['mspt']}/{s['mspt']} yabanci_gradle={o['yabanci']}/{k['yabanci']}/{s['yabanci']} "
            f"afet_kirilan/bolum/birim_tick_encok/kirli/bekleyen={k.get('yikim')} pencere=[{k['pencere']}] "
            f"bekleyen_derleme_karada={k['pc']} "
            f"bolum={o['bolum']}->{k['bolum']}->{s['bolum']} parcacik={o['parcacik']}->{k['parcacik']}->{s['parcacik']} "
            f"varlik={o['varlik']}->{k['varlik']}->{s['varlik']} suda_ort={k['suda']} sicrama_varlik_sn={k['sicrama']} "
            f"bubble_varlik_tick={k['bubble']} splash_varlik_tick={k['splash']} "
            f"gecerli={'E' if gecerli else 'H ' + '; '.join(neden)} sonuc={'GECTI' if gecti else 'KALDI'}"
            f"{' K-E26b-bekliyor' if k26b else ''} | karada_parcacik_tur=[{k['parcacik_tur']}]")
    for m in re.finditer(r"TSUNAMI-OTOMASYON PROFIL (\S+) (.*)", metin):
        log(f"TABLO profil {m.group(1)} {m.group(2)}")
    if sahne:
        log(f"TABLO ozet: gecerli={'E' if hepsi_gecerli else 'H'} hepsi_gecti={'E' if hepsi_gecti else 'H'} "
            f"K-E26b_disi_gecti={'E' if bekleyen_disi else 'H'}")
    return hepsi_gecerli and hepsi_gecti if sahne else None


def serpinti_ozeti(metin, log):
    """Serpinti ve köpük kabulü: her `KARE` satırında `serpinti_dogan` (son KARE'den beri doğan dalga serpintisi parçacığı),
    `kopuk_nokta` (karenin köpük noktası) ve bunların son konumuna göre `serpinti_havada` (görünen su üstünden
    `SerpintiKarari.HAVA_PAYI` = 0,5 bloktan yüksekte ya da altında su yok), `serpinti_blok_icinde` (blok çarpışma şeklinin içinde),
    `serpinti_cizimsiz_su` (yakın su ağı noktayı kapsıyor ama orada su çizilmiyor); sınıflama istemcide
    (`SerpintiNoktasi.olcut`). Kabul: üç toplamın hepsi 0 (pay Java'daki HAVA_PAYI'nda, burada sayılar tam). Sonuç: geçti
    (bool) ya da satır yoksa None."""
    anahtar = ("havada", "blok_icinde", "cizimsiz_su")
    toplam = dict.fromkeys(anahtar, 0)
    dogan = kopuk = kare = 0
    for m in re.finditer(r"TSUNAMI-OTOMASYON KARE (\S+) .*?serpinti_dogan=(\d+) kopuk_nokta=(\d+) serpinti_havada=(\d+) "
                         r"serpinti_blok_icinde=(\d+) serpinti_cizimsiz_su=(\d+)", metin):
        kare += 1
        dogan += int(m.group(2))
        kopuk += int(m.group(3))
        sayi = dict(zip(anahtar, (int(m.group(i)) for i in (4, 5, 6))))
        for k, v in sayi.items():
            toplam[k] += v
        if any(sayi.values()):
            log(f"SERPINTI {m.group(1)} " + " ".join(f"{k}={v}" for k, v in sayi.items()))
    if not kare:
        log("SERPINTI ozet: KARE satırlarında serpinti sayısı yok")
        return None
    gecti = not any(toplam.values())
    log(f"SERPINTI ozet: kare={kare} serpinti_dogan={dogan} kopuk_nokta={kopuk} "
        + " ".join(f"{k}={v}" for k, v in toplam.items()) + f" kabul={'GECTI' if gecti else 'KALDI'}")
    return gecti


def yagmur_ozeti(metin, log):
    """Tsunami yağmuru (AF3-14): her `KARE` satırının `yagmur_ayari` (CLIENT `tsunami.yagmur`, otomasyonun `y:` adımı).
    Ayar kapalıyken (varsayılan) kabul her karede `damla=0 halka=0`; açıkken AF-01 halka ölçütü: `goz_ustu`, `cizimsiz`,
    `kenarsiz` toplamı 0 (`TsunamiYagmuru.olcut`). Sonuç: geçti (bool) ya da satır yoksa None."""
    sayi = {"kapali": 0, "acik": 0}
    kapali = dict.fromkeys(("damla", "halka"), 0)
    acik = dict.fromkeys(("goz_ustu", "cizimsiz", "kenarsiz"), 0)
    for m in re.finditer(r"TSUNAMI-OTOMASYON KARE (\S+) yagmur_ayari=(acik|kapali) damla=(\d+) halka=(\d+) .*?"
                         r"goz_ustu=(\d+) cizimsiz=(\d+) kenarsiz=(\d+)", metin):
        sayi[m.group(2)] += 1
        if m.group(2) == "kapali":
            d = dict(zip(kapali, (int(m.group(3)), int(m.group(4)))))
            hedef = kapali
        else:
            d = dict(zip(acik, (int(m.group(i)) for i in (5, 6, 7))))
            hedef = acik
        for k, v in d.items():
            hedef[k] += v
        if any(d.values()):
            log(f"YAGMUR {m.group(1)} {m.group(2)} " + " ".join(f"{k}={v}" for k, v in d.items()))
    if not sum(sayi.values()):
        log("YAGMUR ozet: KARE satırlarında yağmur ayarı yok")
        return None
    gecti = not any(kapali.values()) and not any(acik.values())
    log(f"YAGMUR ozet: kapali_kare={sayi['kapali']} " + " ".join(f"{k}={v}" for k, v in kapali.items())
        + f" acik_kare={sayi['acik']} " + " ".join(f"{k}={v}" for k, v in acik.items())
        + f" kabul={'GECTI' if gecti else 'KALDI'}")
    return gecti


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
    oyun_log = kanit / f"runClient-{bolum}.log"
    betik = Path("/tmp/tsunami-istemci.sh")
    betik.write_text("#!/bin/sh\n"
                     f"cd '{KOK}' || exit 9\n"
                     f"JAVA_HOME='{JAVA_HOME}' python3 tools/senaryo/runclient_kilit.py kos '{KOK}' --bekle 1800 -- "
                     f"timeout 1200 ./gradlew runClient --no-daemon --offline --console=plain "
                     f"-PquickPlay=test -Potomasyon=tsunami -PotomasyonBolum={bolum} > '{oyun_log}' 2>&1\n"
                     f"echo \"cikis=$?\" >> '{oyun_log}'\n")
    duvar_baslangic = time.time()
    sahip_adres = runclient_kilit.istemci_baslat(betik, WS, log)
    baslangic = time.monotonic()

    pid = None
    while time.monotonic() - baslangic < 600:
        pid = istemci_pid()
        if pid:
            break
        time.sleep(0.5)
    if not pid:
        log("istemci JVM'i 10 dk'da görünmedi")
        return 2
    neden = runclient_kilit.pencere_dogrula(pid, WS, log)
    if neden:
        log(f"ALARM: {neden}; süreç öldürülüyor")
        os.kill(pid, signal.SIGKILL)
        return 1
    yoklama = 0
    alarm = 0
    bekci = runclient_kilit.KapanisBekcisi(KOK, pid, (oyun_log,), "TSUNAMI-OTOMASYON BITTI", log)
    while not bekci.bitti_mi():
        time.sleep(5)
        yoklama += 1
        if runclient_kilit.odak_denetle(pid, sahip_adres, log, lambda _neden: os.kill(pid, signal.SIGKILL)):
            alarm += 1
            break
        if yoklama % 6 == 0:
            log(f"denetim {yoklama}: istemci pid={pid} yaşıyor; gradle={sh('pgrep -c -f [G]radleWrapperMain')}")
    son = time.monotonic() + 120
    while time.monotonic() < son and "cikis=" not in oyun_log.read_text(encoding="utf-8", errors="replace"):
        time.sleep(2)
    metin = oyun_log.read_text(encoding="utf-8", errors="replace")
    asili = bekci.olduruldu or "K-E27: asılı istemci öldürüldü" in metin
    girdi = len(re.findall(r"TSUNAMI-OTOMASYON GIRDI", metin))
    girdi_temiz = runclient_kilit.girdi_ozeti(girdi == 0, f"(e) oyun içi girdi satırı: {girdi}", log)
    bitti = re.search(r"TSUNAMI-OTOMASYON BITTI girdi tus=(\d+) fare=(\d+) teker=(\d+)", metin)
    cikis = re.search(r"cikis=(\d+)", metin)
    log(f"bitti: yoklama={yoklama} ALARM={alarm} girdi_satiri={girdi} kapanis_asili={'E' if asili else 'H'} "
        f"otomasyon_ozeti={bitti.groups() if bitti else None} gradle_{cikis.group(0) if cikis else 'cikis=?'}")
    desen = (r".*(?:TSUNAMI-OTOMASYON (?:FPS|FOTO|KARE|HALKA|KAMERA|DALGA|EVRE|KALITE|AVCI|AYAR|KAPANIS|PROFIL)|OLCUM tsunami |"
             r"KARE-KAPISI tsunami |Tsunami: |\[CHAT\]|SU-TANI |Sanal su: kurulum|Tsunami köpüğü: |"
             r"has (?:made|reached|completed) the).*")
    for satir in re.findall(desen, metin):
        log("  " + re.split(r"TSUNAMI-OTOMASYON |DogalAfetler/\]: |\]: ", satir, maxsplit=1)[-1])
    kabul = {"TABLO": fps_tablosu(metin, log), "SERPINTI": serpinti_ozeti(metin, log), "YAGMUR": yagmur_ozeti(metin, log)}
    kalan = [ad for ad, sonuc in kabul.items() if sonuc is False]
    for png in (RUN / "screenshots").glob("tsunami-*.png"):
        if png.stat().st_mtime > duvar_baslangic:
            shutil.copy2(png, kanit / png.name)
            log(f"kopyalandı: {png.name}")
    # inceleme-matris-5 P2-2: avcı dünyanın altına düşer ya da yerleştirilemezse sonraki kareler ve başarım denetimi anlamsız.
    avci_hata = re.findall(r"fell out of the world|AVCI GECERSIZ", metin)
    if avci_hata:
        log(f"GEÇERSİZ: avcı satırı {avci_hata[0]!r} ({len(avci_hata)} kez)")
    if alarm or not girdi_temiz or avci_hata:
        return 1
    # Kapanışı asılı kalan istemcinin ölçümleri BITTI'den önce tamamdır; koşu geçerli, asılı kapanış log'da kalır.
    if not (bitti and cikis and (cikis.group(1) == "0" or asili)):
        return 2
    if kalan:
        log(f"KABUL KALDI: {', '.join(kalan)} (çıkış 3)")
        return 3
    return 0


if __name__ == "__main__":
    sys.exit(defter.sarmala(main))
