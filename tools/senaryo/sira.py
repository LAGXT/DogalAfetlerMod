#!/usr/bin/env python3
"""İş kuyruğu (K-E51, K-E53, K-E60): ağır işin tek girişi; koşucu (`sira_kosucu.py`) yuva açılınca koşar.

Tek giriş (K-E60): ağır süreç açan betikler (gradle_kapisi.sh, uzak.sh, pc2_istemci.sh, *_sunucu.py, *_gorsel.py,
*_istemci.py, runclient_kilit.py kos) bir kuyruk işinin içinde değilse kendilerini `ekle --bekle` ile kuyruğa ekleyip
bekler (`kuyruga_gir`); `./gradlew` iş dışında reddedilir (çıkış 3). İşin içi: koşturucunun verdiği SIRA_IS, pc2'deki
karşılığı uzak.sh'nin UZAK_ICINDE'si; SIRA_SERBEST=1 kuyruğu bilerek atlar.
Kuyruk dışı ağır süreç: Gradle istemcisi (`GradleWrapperMain`) olup o makinede koşan bir işe ait olmayan; ana makinede
ortamındaki SIRA_IS'ten, pc2'de pc2 koşucusunun ssh ile PC2_SUREC_ARALIK_SN'de bir topladığı listeden (atası bir kuyruk
işinin uzak.sh'si mi). Pano onları `kuyruk dışı` satırı olarak gösterir, `Yuva:` satırı ve koşucular yuva sayar.

Spool: ~/.cache/dogalafet-sira/<spool>/{bekleyen,kosan,biten}/<id>.json (SIRA_KOK değiştirir), işin çıktısı
<spool>/log/<id>.log (ya da --log). Her iş: komut, cwd, env (TEST_FILTRE, KAPI_*, UZAK*, DEFTER_ZORLA, VIBE_OTURUM,
LC_ALL, GRADLE_OPTS; ana kuyrukta ekran değişkenleri de), yuva, eklenme/başlangıç/bitiş, çıkış, süre, log. Kabuğun
JAVA_HOME'u (mise Java 21 olabilir) kaydedilmez: kaydında JAVA_HOME olmayan iş JDK 17 ile koşar (K-E54, K0-13).
İki koşucu da ana makinede, makine başına 2 yuva; ana makinenin işi UZAK=0 ile yerelde koşar. Spool'lar:
  oto  iki makinede de koşabilen iş: ekransız ağır iş (gradle_kapisi.sh, ./gradlew ekransız görevler, *_sunucu.py),
       makineden bağımsız istemci işi (*_gorsel.py/*_istemci.py; kanıt klasörü betik ilk argümanı, `--kanit` argümanı ya
       da `sira.py ekle --kanit`) ve `--makine oto` ile verilen türsüz komut (pc2'de uzak.sh ile sarılır). Koşucu önce
       kendi spool'una, sonra oto'ya bakar ve işi `rename` ile kendi kosan/'ına alır (kayıttaki `makine` koşan makine olur,
       `kaynak` oto; kimlik değişmez). Dağıtım (K-E60 rev.): iş, onu alabilen makinelerden boş yuvası en çok olana gider
       (koşan + kuyruk dışı yük sayılır; eşitlikte pc2): 2/2 yuvayla 1. iş pc2, 2. ana, 3. pc2, 4. ana; bir makine
       alamıyorsa (dolu, aynı klasörden işi koşuyor, koşucusu yok) öteki alır. Ana makinede ağır süreç ≤ 2
       (kuyruğun işleri + kuyruk dışı Gradle), boş RAM >= KAPI_RAM_MB (4096); istemci işi bir yuva, tek istemci ve
       yaşayan Hyprland ister (yoksa pc2'ye kalır). pc2 istemci işini `pc2_istemci.sh <kanıt/> <betik> …` ile iki yuvada koşar.
  ana  ekranlı iş (runClient, runData, UZAK=0, --olcum, kanıtı belirsiz istemci işi, istemcili sunucu) ya da `--makine ana`.
  pc2  `--makine pc2`, pc2_istemci.sh ve uzak.sh işi: komutun kendisi pc2'ye gider (K-E44); kendiliğinden gitmeyen komut
       `tools/senaryo/uzak.sh` ile sarılır. pc2_istemci.sh işi kendiliğinden --istemci (koşturucu uzak.sh yuva kilitlerini
       tutar, UZAK_YUVA_TUTULU=1).
Aynı klasör: makine başına tek iş; iki makinede birer iş olur (pc2 kendi kopyasında koşar, yalnız log/kanıt geri gelir),
ama pc2'deki doğrudan `./gradlew` işi build/ alt klasörlerini geri çektiğinden ana makinede aynı klasörün işiyle yan yana
koşmaz.

Kullanım:
  sira.py ekle [--makine oto|ana|pc2] [--bekle] [--yuva 1|2] [--istemci] [--kanit <dizin/>] [--olcum] [--log <yol>]
               -- <komut...>        iş kimliğini basar ve hemen döner (koşucu yoksa systemd ile başlatmayı dener);
                                    --makine verilmezse komuttan seçilir (türsüz komut ana); --bekle: iş bitene dek
                                    bekler, log'unu akıtır, işin çıkış koduyla döner, bekleyen kesilirse iş iptal;
                                    --olcum: FPS/CPU ölçümü, yalnız ana makine (K0-33, K-E29 b)
  sira.py durum [<id>]              pano (bütün işler) ya da tek işin kaydı
  sira.py bekle <id> [--zaman <sn>] iş bitince onun çıkış koduyla döner; yalnız yapılacak başka iş kalmayınca
  sira.py iptal <id>                bekleyeni düşürür, koşanın oturumuna TERM gönderir
"""
import argparse
import fcntl
import hashlib
import json
import os
import re
import secrets
import shlex
import signal
import subprocess
import sys
import time
from pathlib import Path

W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
SPOOL = Path(os.environ.get("SIRA_KOK") or Path.home() / ".cache" / "dogalafet-sira")
PANO = Path(os.environ.get("SIRA_PANO") or KOK / ".vibe" / "pano.md")
MAKINELER = ("ana", "pc2")
SPOOLLAR = (*MAKINELER, "oto")
YUVA = 2
SURUM = 2
ACLIK_SN = float(os.environ.get("SIRA_ACLIK_SN", "600"))
ENV_ANAHTARI = re.compile(r"(TEST_FILTRE|KAPI_\w+|UZAK\w*|DEFTER_ZORLA|VIBE_OTURUM|LC_ALL|GRADLE_OPTS)")
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
EKRAN_ENV = ("DISPLAY", "WAYLAND_DISPLAY", "HYPRLAND_INSTANCE_SIGNATURE", "XDG_RUNTIME_DIR", "DBUS_SESSION_BUS_ADDRESS")
KENDILIGINDEN_UZAK = ("uzak.sh", "pc2_istemci.sh")
ISTEMCI_BETIGI = ("_gorsel.py", "_istemci.py")
UZAK_KILITLERI = Path.home() / ".cache" / f"uzak-{os.environ.get('UZAK_MAKINE', 'pc2')}"
GRADLE_ISTEMCI = "org.gradle.wrapper.GradleWrapperMain"
PC2_SURECLER = "surecler.json"
PC2_SUREC_ARALIK_SN = 5
PC2_SUREC_TAZE_SN = 60


def dizin(makine, durum=""):
    yol = SPOOL / makine / durum
    yol.mkdir(parents=True, exist_ok=True)
    return yol


def json_yaz(yol, veri):
    gecici = yol.with_name(f".{yol.name}.{os.getpid()}.gecici")
    gecici.write_text(json.dumps(veri, ensure_ascii=False, indent=1), encoding="utf-8")
    os.replace(gecici, yol)


def json_oku(yol):
    try:
        return json.loads(yol.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def isler(makine, durum):
    """[(yol, kayıt)], eklenme sırasıyla. Bağlar (eski sira.py için pc2/bekleyen'de oto'ya taşınan işin izi) sayılmaz."""
    liste = []
    for yol in dizin(makine, durum).glob("[!.]*.json"):
        if yol.is_symlink():
            continue
        veri = json_oku(yol)
        if veri:
            liste.append((yol, veri))
    return sorted(liste, key=lambda x: (x[1].get("eklendi", 0), x[0].name))


def bul(kimlik):
    """(spool, durum, yol); makineden bağımsız. İş spool'lar arasında `rename` edilirken kaçmasın diye üç kez bakar."""
    for _ in range(3):
        for makine in SPOOLLAR:
            for durum in ("bekleyen", "kosan", "biten"):
                yol = dizin(makine, durum) / f"{kimlik}.json"
                if yol.exists() and not yol.is_symlink():
                    return makine, durum, yol
        time.sleep(0.05)
    return None, None, None


def kosucu_canli(makine):
    try:
        fd = os.open(dizin(makine) / "kosucu.lock", os.O_RDONLY)
    except FileNotFoundError:
        return False
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        return True
    finally:
        os.close(fd)
    return False


def kilit_dolu(yol):
    """Bir flock dosyasını başkası tutuyorsa True (uzak.sh'in `exec 9>dosya; flock` kilitleri)."""
    try:
        fd = os.open(yol, os.O_RDONLY)
    except FileNotFoundError:
        return False
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        return True
    finally:
        os.close(fd)
    return False


def kendiliginden(komut):
    """(tür, argümanlar): gradle (gradle_kapisi.sh/gradlew görevleri), sunucu (*_sunucu.py), uzak ya da None."""
    for i, arg in enumerate(komut):
        ad = Path(arg).name
        if ad == "gradle_kapisi.sh":
            return "gradle", komut[i + 2:]
        if ad == "gradlew":
            return "gradle", komut[i + 1:]
        if ad.endswith("_sunucu.py"):
            return "sunucu", komut[i + 1:]
        if ad in KENDILIGINDEN_UZAK:
            return "uzak", komut[i + 1:]
    return None, []


def ekransiz_mi(komut):
    tur, arg = kendiliginden(komut)
    if tur == "gradle":
        return subprocess.run([str(W / "tools/senaryo/uzak.sh"), "--uzak-mi", *arg]).returncode == 0
    if tur == "sunucu":
        return not {"--istemci", "--yalniz-istemci"} & set(arg)
    return tur == "uzak"


def istemci_mi(komut):
    """Komut runClient açar mı: *_gorsel.py/*_istemci.py, pc2_istemci.sh, runclient_kilit.py ya da istemcili sunucu."""
    tur, arg = kendiliginden(komut)
    return (istemci_betigi(komut) is not None or any(Path(a).name in ("pc2_istemci.sh", "runclient_kilit.py") for a in komut)
            or tur == "sunucu" and bool({"--istemci", "--yalniz-istemci"} & set(arg)))


def build_geri_ceker(veri):
    """İş doğrudan `./gradlew` mi: pc2'de uzak.sh kancası build/reports|test-results|libs'i aynı klasöre geri çeker."""
    return any(Path(a).name == "gradlew" for a in veri["komut"])


def istemci_betigi(komut):
    """Komuttaki görsel/istemci betiğinin sırası (`*_gorsel.py`, `*_istemci.py`) ya da None."""
    return next((i for i, a in enumerate(komut) if Path(a).name.endswith(ISTEMCI_BETIGI)), None)


def istemci_kaniti(komut, kanit=None):
    """Makineden bağımsız istemci işinin kanıt klasörü (sonu /): `kanit`, betiğin `--kanit` argümanı ya da ilk argümanı.
    Betik yoksa ya da klasör belirsizse None (iş pc2'ye taşınamaz)."""
    i = istemci_betigi(komut)
    if i is None:
        return None
    arglar = komut[i + 1:]
    if not kanit and "--kanit" in arglar[:-1]:
        kanit = arglar[arglar.index("--kanit") + 1]
    if not kanit and arglar and not arglar[0].startswith("-"):
        kanit = arglar[0]
    return kanit.rstrip("/") + "/" if kanit else None


def gerek(veri, makine):
    """İşin `makine`de tuttuğu yuva: istemci işi pc2'de iki (runClient ~5–6 GiB), ana makinede bir (tek istemci ayrıca)."""
    if veri.get("istemci") and makine == "ana":
        return 1
    return min(veri.get("yuva", 1), YUVA)


def makineye_hazirla(veri, makine):
    """Kaydı `makine`de koşacak biçime getirir (yerinde): `makine` alanı; ana makinenin işine UZAK=0 (yerelde koşar),
    oto'dan gelene ekran ortamı; pc2'de türsüz oto işi uzak.sh ile sarılır, makineden bağımsız istemci işi
    `pc2_istemci.sh <kanıt/> <betik> …` olur (özgün komut `ozgun_komut`)."""
    if veri["makine"] == "oto":
        veri["kaynak"] = "oto"
    veri["makine"] = makine
    if makine == "ana":
        veri["env"] = {**veri["env"], **veri.get("ekran_env", {}), "UZAK": "0"}
    elif veri.get("kaynak") == "oto" and not veri.get("istemci") and kendiliginden(veri["komut"])[0] is None:
        veri["ozgun_komut"] = veri["komut"]
        veri["komut"] = [str(Path(veri["cwd"]) / "tools/senaryo/uzak.sh"), *veri["komut"]]
    i = istemci_betigi(veri["komut"])
    if makine == "pc2" and veri.get("istemci") and veri.get("kanit") and i is not None \
            and kendiliginden(veri["komut"])[0] != "uzak":
        veri["ozgun_komut"] = veri["komut"]
        veri["komut"] = ["tools/senaryo/pc2_istemci.sh", veri["kanit"], Path(veri["komut"][i]).name,
                         *veri["komut"][i + 1:]]
    return veri


def yeni_kimlik():
    simdi = time.time()
    return time.strftime("%m%d-%H%M%S", time.localtime(simdi)) + f"{int(simdi * 1000) % 1000:03d}-{secrets.token_hex(1)}"


def ekle(makine, komut, yuva=1, istemci=False, log=None, cwd=None, olcum=False, kanit=None, cikti=print):
    """İşi kuyruğa yazar, kimliğini döner. `makine` None ya da oto ise komuttan seçilir (oto spool'u, ana ya da pc2);
    türsüz komut None'da ana'ya, açıkça oto'da oto'ya girer."""
    cwd = str(Path(cwd or os.getcwd()).resolve())
    env = {k: v for k, v in os.environ.items() if ENV_ANAHTARI.fullmatch(k)}
    ekran = {k: os.environ[k] for k in EKRAN_ENV if k in os.environ}
    tur = kendiliginden(komut)[0]
    pc2_istemci = any(Path(a).name == "pc2_istemci.sh" for a in komut)
    istemci = istemci or istemci_mi(komut)
    if olcum:
        if makine == "pc2" or tur == "uzak":
            raise ValueError("--olcum işi yalnız ana makinede koşar (K0-33, K-E29 b)")
        makine = "ana"
    kanit = istemci_kaniti(komut, kanit) if istemci and not pc2_istemci else None
    if makine in (None, "oto"):
        if tur == "uzak":
            makine = "pc2"
        elif env.get("UZAK") == "0":
            makine = "ana"
        elif istemci:
            makine = "oto" if kanit else "ana"
        elif tur is None:
            makine = makine or "ana"
        else:
            makine = "oto" if ekransiz_mi(komut) else "ana"
    if makine == "oto" and not istemci and tur is None and not all(
            (Path(cwd) / y).exists() for y in ("gradlew", "tools/senaryo/uzak.sh")):
        raise ValueError(f"oto işi pc2'de uzak.sh ile sarılır ama {cwd} proje/worktree kökü değil")
    if makine == "ana":
        env.update(ekran)
    elif makine == "pc2" and istemci and not pc2_istemci and not kanit:
        raise ValueError("pc2 istemci işinin kanıt klasörü belirsiz: `--kanit <dizin/>` ya da betiğin ilk argümanı")
    elif makine == "pc2" and not istemci and tur is None:
        if not (Path(cwd) / "gradlew").exists():
            raise ValueError(f"pc2 işi uzak.sh ile sarılacak ama {cwd} proje/worktree kökü değil")
        komut = [str(Path(cwd) / "tools/senaryo/uzak.sh"), *komut]
    kimlik = yeni_kimlik()
    veri = {
        "id": kimlik, "makine": makine, "komut": list(komut), "cwd": cwd, "env": env,
        "yuva": min(2 if istemci else yuva, YUVA), "istemci": istemci, "eklendi": time.time(), "durum": "bekliyor",
        "log": str(Path(log).resolve()) if log else str(dizin(makine, "log") / f"{kimlik}.log"),
        "ekleyen": os.environ.get("VIBE_OTURUM", ""), "surum": SURUM,
    }
    if kanit:
        veri["kanit"] = kanit
    if olcum:
        veri["olcum"] = True
    if makine == "oto" and istemci:
        veri["ekran_env"] = ekran
    json_yaz(dizin(makine, "bekleyen") / f"{kimlik}.json", veri)
    kosan = sum(len(isler(m, "kosan")) for m in (MAKINELER if makine == "oto" else (makine,)))
    onde = len(isler(makine, "bekleyen")) - 1 + kosan
    cikti(f"SIRA: {kimlik} {makine} kuyruğunda (önünde {onde} iş); sonuç: tools/senaryo/sira.py bekle {kimlik}",
          file=sys.stderr)
    for m in MAKINELER if makine == "oto" else (makine,):
        if kosucu_canli(m):
            continue
        if "SIRA_KOK" not in os.environ:
            subprocess.run(["systemctl", "--user", "start", f"dogalafet-sira@{m}.service"], capture_output=True,
                           timeout=10)
        if not kosucu_canli(m):
            cikti(f"SIRA: UYARI {m} koşucusu çalışmıyor; iş bekler. Başlat: systemctl --user start "
                  f"dogalafet-sira@{m} (ya da nohup python3 tools/senaryo/sira_kosucu.py --makine {m} &)",
                  file=sys.stderr)
    return kimlik


def is_icinde():
    """Kuyruk işinin içinde mi (koşturucu SIRA_IS verir; pc2'deki karşılığı uzak.sh'nin UZAK_ICINDE'si) ya da kuyruk
    bilerek mi atlanıyor (SIRA_SERBEST=1)."""
    return bool(os.environ.get("SIRA_IS") or os.environ.get("UZAK_ICINDE")) or os.environ.get("SIRA_SERBEST") == "1"


def kuyruga_gir(komut, makine="oto", istemci=False, kanit=None):
    """K-E60 tek giriş: iş dışındaki çağrı kendini `ekle --bekle` ile kuyruğa ekler, süreç işin çıkış koduyla biter
    (exec); işin içindeyse döner, çağıran yerinde koşar."""
    if is_icinde():
        return
    arg = [sys.executable, str(Path(__file__).resolve()), "ekle", "--bekle", "--makine", makine]
    if istemci:
        arg.append("--istemci")
    if kanit:
        arg += ["--kanit", str(kanit)]
    sys.stdout.flush()
    sys.stderr.flush()
    os.execv(sys.executable, [*arg, "--", *komut])


def _hata(*a, **_k):
    print(*a, file=sys.stderr, flush=True)


def kesilince_iptal(kimlikler):
    """`ekle --bekle`: bekleyen süreç TERM/INT/HUP alırsa `kimlikler`deki iş iptal edilir ve 128 + sinyal ile çıkılır
    (doğrudan çağrının kesilmesi eskisi gibi işi de keser). İş kaydı yazılmadan önce kurulur."""
    def kes(sinyal, _c):
        for kimlik in kimlikler:
            iptal(kimlik, cikti=_hata)
        sys.exit(128 + sinyal)

    for s in (signal.SIGTERM, signal.SIGINT, signal.SIGHUP):
        signal.signal(s, kes)


def izle(kimlik, konum=0):
    """`ekle --bekle`: işin log'unu `konum`dan itibaren akıtır, iş bitince onun çıkış koduyla döner."""
    while True:
        _, durum, yol = bul(kimlik)
        if durum is None:
            _hata(f"SIRA: {kimlik} bulunamadı")
            return 2
        veri = json_oku(yol) or {}
        try:
            with open(veri.get("log", ""), "rb") as dosya:
                dosya.seek(konum)
                parca = dosya.read()
        except (FileNotFoundError, IsADirectoryError):
            parca = b""
        if parca:
            konum += len(parca)
            sys.stdout.buffer.write(parca)
            sys.stdout.flush()
        if durum == "biten" and "bitti" in veri:
            _hata(ozet(veri))
            kod = veri.get("cikis")
            return kod if isinstance(kod, int) and veri["durum"] == "bitti" else 1
        time.sleep(1)


def uzak_dolu_yuva():
    """uzak.sh'in pc2 yuva kilitlerinden (`~/.cache/uzak-pc2/yuva-<n>`) tutulanların sayısı."""
    return sum(kilit_dolu(UZAK_KILITLERI / f"yuva-{n}") for n in range(1, int(os.environ.get("UZAK_YUVA", "2")) + 1))


def uzak_klasor_dolu(cwd):
    """Bu klasörün uzak.sh klasör kilidi tutuluyorsa True (pc2'de aynı klasörden tek iş)."""
    klasor = str(Path(cwd).resolve())
    return kilit_dolu(UZAK_KILITLERI / f"klasor-{hashlib.md5(klasor.encode()).hexdigest()[:12]}")


def _surec_ortami(pid, ad):
    try:
        for satir in Path(f"/proc/{pid}/environ").read_bytes().split(b"\0"):
            if satir.startswith(ad.encode() + b"="):
                return satir.split(b"=", 1)[1].decode(errors="replace")
    except OSError:
        pass
    return None


def _gradle_metni(arglar):
    """Gradle istemcisinin pano metni: proje klasörü (wrapper jar yolundan) ve görevler."""
    proje = next((Path(a).parents[2].name for a in arglar if a.endswith("gradle/wrapper/gradle-wrapper.jar")), "?")
    gorevler = arglar[arglar.index(GRADLE_ISTEMCI) + 1:] if GRADLE_ISTEMCI in arglar else []
    return f"[{proje}] gradlew {' '.join(gorevler)}".strip()


def ana_gradle_surecleri():
    """Ana makinedeki Gradle istemcileri: [{pid, baslangic, komut, is}]; `is` ortamındaki SIRA_IS (işin içinde açılan her
    süreç onu taşır; istemci işinin Hyprland'den açılan runClient'ına `runclient_kilit.istemci_baslat` geçirir)."""
    acilis = float(next(s.split()[1] for s in Path("/proc/stat").read_text().splitlines() if s.startswith("btime")))
    tik = os.sysconf("SC_CLK_TCK")
    surecler = []
    for yol in Path("/proc").glob("[0-9]*/cmdline"):
        try:
            arglar = yol.read_bytes().decode(errors="replace").split("\0")
            if GRADLE_ISTEMCI not in arglar:
                continue
            baslangic = acilis + int((yol.parent / "stat").read_text().rsplit(")", 1)[1].split()[19]) / tik
        except (OSError, IndexError, ValueError):
            continue
        pid = int(yol.parent.name)
        surecler.append({"pid": pid, "baslangic": baslangic, "komut": _gradle_metni(arglar),
                         "is": _surec_ortami(pid, "SIRA_IS")})
    return surecler


def pc2_surecleri_coz(ps_metni, pid_dosyalari, yerel_is=lambda pid: _surec_ortami(pid, "SIRA_IS"), simdi=None):
    """pc2'deki Gradle istemcileri (`ps -eo pid=,ppid=,etimes=,args=` çıktısı ve `<dosya> <pid>` satırlarıyla
    `/tmp/uzak-<yerel pid>-<yuva>.pid`): [{pid, baslangic, komut, uzak, is}]. Atası uzak.sh'nin pc2'de başlattığı süreçse
    `uzak` o yerel uzak.sh'nin pid'i, `is` onun ortamındaki SIRA_IS; değilse (elle `ssh pc2 ./gradlew`) ikisi de None."""
    simdi = simdi or time.time()
    ust, satirlar = {}, []
    for satir in ps_metni.splitlines():
        parca = satir.split(None, 3)
        if len(parca) == 4 and parca[0].isdigit():
            ust[int(parca[0])] = int(parca[1])
            satirlar.append((int(parca[0]), int(parca[2]), parca[3].split()))
    oturumlar = {}
    for satir in pid_dosyalari.splitlines():
        eslesme = re.fullmatch(r"\S*/uzak-(\d+)-\S+\.pid (\d+)", satir.strip())
        if eslesme:
            oturumlar[int(eslesme[2])] = int(eslesme[1])
    surecler = []
    for pid, gecen, arglar in satirlar:
        if GRADLE_ISTEMCI not in arglar:
            continue
        uzak, ata = None, pid
        for _ in range(64):
            if ata in oturumlar:
                uzak = oturumlar[ata]
                break
            ata = ust.get(ata, 0)
            if ata <= 1:
                break
        surecler.append({"pid": pid, "baslangic": simdi - gecen, "komut": _gradle_metni(arglar), "uzak": uzak,
                         "is": yerel_is(uzak) if uzak else None})
    return surecler


def pc2_okumasi():
    """pc2 koşucusunun son süreç okuması ({zaman, surecler} ya da {zaman, hata}); PC2_SUREC_TAZE_SN'den eskiyse None."""
    veri = json_oku(dizin("pc2") / PC2_SURECLER)
    return veri if veri and time.time() - veri.get("zaman", 0) <= PC2_SUREC_TAZE_SN and "surecler" in veri else None


def dis_surecler(makine):
    """Kuyruk dışı ağır süreçler: o makinede koşan bir işe ait olmayan Gradle istemcileri. pc2 okuması yoksa boş."""
    kosan = {v["id"] for _, v in isler(makine, "kosan")}
    if makine == "ana":
        surecler = ana_gradle_surecleri()
    else:
        surecler = (pc2_okumasi() or {}).get("surecler", [])
    return [s for s in surecler if s.get("is") not in kosan]


def doluluk(makine, dis=None):
    """Makinenin dolu yuvası: kuyruğun koşan işleri + kuyruk dışı ağır süreçler. pc2'de uzak.sh yuva kilitleri (kuyruk
    dışından çağrılan uzak.sh'ninkiler de) kuyruğun işleriyle birlikte en az o kadar sayılır, kilitsiz dış süreç (elle
    ssh) üstüne eklenir."""
    dolu = sum(gerek(v, makine) for _, v in isler(makine, "kosan"))
    dis = dis_surecler(makine) if dis is None else dis
    if makine == "ana":
        return dolu + len(dis)
    return max(dolu, uzak_dolu_yuva()) + sum(1 for s in dis if not s.get("uzak"))


def bos_ram_mb():
    return int(Path("/proc/meminfo").read_text().split("MemAvailable:")[1].split()[0]) // 1024


def oturumu_oldur(oturum, sinyal=signal.SIGTERM):
    """Oturum kimliği `oturum` olan bütün süreçlere sinyal (timeout gibi kendi grubunu açanlar da)."""
    sayi = 0
    for stat in Path("/proc").glob("[0-9]*/stat"):
        try:
            alanlar = stat.read_text().rsplit(")", 1)[1].split()
        except OSError:
            continue
        if int(alanlar[3]) == oturum:
            try:
                os.kill(int(stat.parent.name), sinyal)
                sayi += 1
            except ProcessLookupError:
                pass
    return sayi


def iptal(kimlik, cikti=print):
    makine, durum, yol = bul(kimlik)
    if durum == "bekleyen":
        hedef = dizin(makine, "biten") / yol.name
        try:
            os.rename(yol, hedef)
        except FileNotFoundError:
            return iptal(kimlik, cikti)
        veri = json_oku(hedef)
        veri.update(durum="iptal", bitti=time.time())
        json_yaz(hedef, veri)
        cikti(f"SIRA: {kimlik} beklerken iptal edildi")
        return 0
    if durum == "kosan":
        pid = (json_oku(yol) or {}).get("kosturucu")
        sayi = oturumu_oldur(pid) if pid else 0
        cikti(f"SIRA: {kimlik} koşuyor; oturumundaki {sayi} sürece TERM gönderildi")
        return 0 if sayi else 1
    cikti(f"SIRA: {kimlik} " + ("zaten bitmiş" if durum else "bulunamadı"))
    return 1


def ozet(veri):
    return (f"SIRA: {veri['id']} {veri['makine']} {veri['durum']} çıkış={veri.get('cikis', '-')} "
            f"süre={veri.get('sure', '-')} sn log={veri['log']}")


def bekle(kimlik, zaman=None, cikti=print):
    son = time.monotonic() + zaman if zaman else None
    while True:
        makine, durum, yol = bul(kimlik)
        if durum is None:
            cikti(f"SIRA: {kimlik} bulunamadı")
            return 2
        if durum == "biten":
            veri = json_oku(yol)
            if veri and "bitti" in veri:
                cikti(ozet(veri))
                kod = veri.get("cikis")
                return kod if isinstance(kod, int) and veri["durum"] == "bitti" else 1
        if son and time.monotonic() > son:
            cikti(f"SIRA: {kimlik} {zaman} sn'de bitmedi ({durum})")
            return 124
        time.sleep(1)


def sure_metni(sn):
    sn = int(sn)
    return f"{sn // 60} dk {sn % 60} sn" if sn >= 60 else f"{sn} sn"


def komut_metni(veri):
    metin = shlex.join(veri["komut"]).replace(veri["cwd"] + "/", "")
    yer = Path(veri["cwd"]).name
    metin = f"[{yer}] {metin}"
    return (metin[:97] + "…" if len(metin) > 98 else metin).replace("|", "\\|")


def makine_metni(veri):
    return f"{veri['makine']} (oto)" if veri.get("kaynak") == "oto" else veri["makine"]


def pano_metni(son_biten=10):
    simdi = time.time()
    kosucular, yuvalar = [], []
    dislar = {m: dis_surecler(m) for m in MAKINELER}
    for makine in MAKINELER:
        bilgi = json_oku(dizin(makine) / "kosucu.json") or {}
        kosucular.append(f"{makine} {'çalışıyor' if kosucu_canli(makine) else 'YOK'}")
        yuvalar.append(f"{makine} {doluluk(makine, dislar[makine])}/{bilgi.get('yuva', YUVA)}")
    yuvalar.append(f"oto bekleyen {len(isler('oto', 'bekleyen'))}")
    if pc2_okumasi() is None:
        yuvalar.append("pc2 kuyruk dışı bilinmiyor (süreç okuması yok)")
    satirlar = ["# DogalAfetlerMod iş panosu", "",
                f"Güncellendi {time.strftime('%Y-%m-%d %H:%M:%S')} · koşucu: {' · '.join(kosucular)}", "",
                f"Yuva: {' · '.join(yuvalar)}", "",
                "| iş | makine | başlangıç | durum | süre | yuva | komut | log |", "|---|---|---|---|---|---|---|---|"]
    biten = []
    for makine in MAKINELER:
        for _, v in isler(makine, "kosan"):
            bas = v.get("baslangic", simdi)
            satirlar.append(f"| {v['id']} | {makine_metni(v)} | {time.strftime('%H:%M:%S', time.localtime(bas))} | "
                            f"koşuyor | {sure_metni(simdi - bas)} | {gerek(v, makine)} | {komut_metni(v)} | {v['log']} |")
    for makine in MAKINELER:
        for d in dislar[makine]:
            komut = d["komut"][:97] + "…" if len(d["komut"]) > 98 else d["komut"]
            satirlar.append(f"| pid {d['pid']} | {makine} | {time.strftime('%H:%M:%S', time.localtime(d['baslangic']))} | "
                            f"kuyruk dışı | {sure_metni(simdi - d['baslangic'])} | 1 | {komut.replace('|', '\\|')} | - |")
    for makine in SPOOLLAR:
        for sira, (_, v) in enumerate(isler(makine, "bekleyen"), 1):
            satirlar.append(f"| {v['id']} | {makine} | - | bekliyor ({sira}.) | {sure_metni(simdi - v['eklendi'])} | "
                            f"{v['yuva']} | {komut_metni(v)} | {v['log']} |")
        biten += [v for _, v in isler(makine, "biten")]
    for v in sorted(biten, key=lambda v: v.get("bitti", 0), reverse=True)[:son_biten]:
        durum = f"bitti, çıkış {v.get('cikis')}" if v["durum"] == "bitti" else v["durum"]
        bas = v.get("baslangic")
        satirlar.append(f"| {v['id']} | {makine_metni(v)} | "
                        f"{time.strftime('%H:%M:%S', time.localtime(bas)) if bas else '-'} | {durum} | "
                        f"{sure_metni(v.get('sure', 0))} | {gerek(v, v['makine'])} | {komut_metni(v)} | {v['log']} |")
    return "\n".join(satirlar) + "\n"


def pano_yaz():
    with open(dizin("") / ".pano.kilit", "a") as kilit:
        fcntl.flock(kilit, fcntl.LOCK_EX)
        metin = pano_metni()
        PANO.parent.mkdir(parents=True, exist_ok=True)
        gecici = PANO.with_name(f".{PANO.name}.{os.getpid()}")
        gecici.write_text(metin, encoding="utf-8")
        os.replace(gecici, PANO)
    return metin


def main():
    argv = sys.argv[1:]
    komut = []
    if "--" in argv:
        komut, argv = argv[argv.index("--") + 1:], argv[:argv.index("--")]
    ayr = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    alt = ayr.add_subparsers(dest="alt", required=True)
    e = alt.add_parser("ekle")
    e.add_argument("--makine", choices=("ana", "pc2", "oto"))
    e.add_argument("--bekle", action="store_true", help="iş bitene dek bekle, log'unu akıt, işin çıkış koduyla dön")
    e.add_argument("--yuva", type=int, choices=(1, 2), default=1)
    e.add_argument("--istemci", action="store_true", help="runClient işi: pc2'de iki yuva, ana makinede bir (tek istemci)")
    e.add_argument("--kanit", help="istemci işinin kanıt klasörü (pc2'de geri çekilir); vars. betiğin ilk argümanı")
    e.add_argument("--olcum", action="store_true", help="FPS/CPU ölçümü: yalnız ana makine")
    e.add_argument("--log")
    alt.add_parser("durum").add_argument("id", nargs="?")
    b = alt.add_parser("bekle")
    b.add_argument("id")
    b.add_argument("--zaman", type=float)
    alt.add_parser("iptal").add_argument("id")
    a = ayr.parse_args(argv)
    if a.alt == "ekle":
        if not komut:
            ayr.error("ekle: komut `--`'dan sonra verilir")
        konum = Path(a.log).stat().st_size if a.log and Path(a.log).is_file() else 0
        kimlikler = []
        if a.bekle:
            kesilince_iptal(kimlikler)
        try:
            kimlikler.append(ekle(a.makine, komut, a.yuva, a.istemci, a.log, olcum=a.olcum, kanit=a.kanit))
        except ValueError as hata:
            print(f"SIRA: {hata}", file=sys.stderr)
            return 2
        if a.bekle:
            return izle(kimlikler[0], konum)
        print(kimlikler[0])
        return 0
    if a.alt == "durum":
        if a.id:
            _, _, yol = bul(a.id)
            if not yol:
                print(f"SIRA: {a.id} bulunamadı")
                return 2
            print(json.dumps(json_oku(yol), ensure_ascii=False, indent=1))
        else:
            print(pano_metni(), end="")
        return 0
    if a.alt == "bekle":
        return bekle(a.id, a.zaman)
    return iptal(a.id)


if __name__ == "__main__":
    sys.exit(main())
