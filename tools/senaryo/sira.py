#!/usr/bin/env python3
"""İş kuyruğu (K-E51, K-E53): ağır iş kuyruğa atılır, ekleyen bloklanmaz; koşucu (`sira_kosucu.py`) yuva açılınca koşar.

Spool: ~/.cache/dogalafet-sira/<spool>/{bekleyen,kosan,biten}/<id>.json (SIRA_KOK değiştirir), işin çıktısı
<spool>/log/<id>.log (ya da --log). Her iş: komut, cwd, env (TEST_FILTRE, KAPI_*, UZAK*, DEFTER_ZORLA, VIBE_OTURUM,
JAVA_HOME, LC_ALL, GRADLE_OPTS; ana kuyrukta ekran değişkenleri de), yuva, eklenme/başlangıç/bitiş, çıkış, süre, log.
İki koşucu da ana makinede, makine başına 2 yuva. Spool'lar:
  oto  (varsayılan, K-E53) iki makinede de koşabilen iş: ekransız ağır iş (gradle_kapisi.sh, ./gradlew ekransız görevler,
       *_sunucu.py) ve makineden bağımsız istemci işi (`--istemci -- python3 tools/senaryo/x_gorsel.py <kanıt/> …`; kanıt
       klasörü betiğin ilk argümanı, `--kanit` argümanı ya da `sira.py ekle --kanit`). Koşucu önce kendi spool'una, sonra
       oto'ya bakar ve işi `rename` ile kendi kosan/'ına alır (kayıttaki `makine` koşan makine olur, `kaynak` oto; kimlik
       değişmez). pc2 öncelikli: ana koşucu oto işini pc2 alamıyorsa (pc2'nin koşan + bekleyen yükü işe yer bırakmıyor, iş
       pc2'de koşan bir işle aynı klasörden ya da pc2 koşucusu yok) ya da iş SIRA_OTO_BEKLE_SN (60) sn beklediyse alır. Ana
       makinede ekransız iş UZAK=0 ile yerelde koşar (ağır süreç ≤ 2: kuyruğun işleri + kuyruk dışı Gradle; boş RAM >=
       KAPI_RAM_MB, vars. 4096; aynı klasörden tek oto işi); istemci işi betiğin kendisiyle, 1 yuva (ana makinede tek istemci: kuyruğun istemci işi ya da
       yaşayan runClient kilidi varken alınmaz). pc2 istemci işini `pc2_istemci.sh <kanıt/> <betik> …` ile iki yuvada koşar.
  ana  ekranlı iş (runClient, runData, UZAK=0, --olcum, kanıtı belirsiz istemci işi) ya da `--makine ana`.
  pc2  `--makine pc2`, pc2_istemci.sh ve uzak.sh işi: komutun kendisi pc2'ye gider (K-E44); kendiliğinden gitmeyen komut
       `tools/senaryo/uzak.sh` ile sarılır. pc2_istemci.sh işi kendiliğinden --istemci (koşturucu uzak.sh yuva kilitlerini
       tutar, UZAK_YUVA_TUTULU=1).

Kullanım:
  sira.py ekle [--makine oto|ana|pc2] [--yuva 1|2] [--istemci] [--kanit <dizin/>] [--olcum] [--log <yol>] -- <komut...>
                                    iş kimliğini basar ve hemen döner (koşucu yoksa systemd ile başlatmayı dener);
                                    --olcum: FPS/CPU ölçümü, yalnız ana makine (K0-33, K-E29 b)
  sira.py durum [<id>]              pano (bütün işler) ya da tek işin kaydı
  sira.py bekle <id> [--zaman <sn>] iş bitince onun çıkış koduyla döner; yalnız yapılacak başka iş kalmayınca
  sira.py iptal <id>                bekleyeni düşürür, koşanın oturumuna TERM gönderir
  sira.py dolu-mu --makine ana|pc2  çıkış 0 + gerekçe: kapı dolu (gradle_kapisi.sh ve cekirdek_sunucu bununla kuyruğa atar)
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
import socket
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
OTO_BEKLE_SN = float(os.environ.get("SIRA_OTO_BEKLE_SN", "60"))
ACLIK_SN = float(os.environ.get("SIRA_ACLIK_SN", "600"))
ENV_ANAHTARI = re.compile(r"(TEST_FILTRE|KAPI_\w+|UZAK\w*|DEFTER_ZORLA|VIBE_OTURUM|JAVA_HOME|LC_ALL|GRADLE_OPTS)")
EKRAN_ENV = ("DISPLAY", "WAYLAND_DISPLAY", "HYPRLAND_INSTANCE_SIGNATURE", "XDG_RUNTIME_DIR", "DBUS_SESSION_BUS_ADDRESS")
KENDILIGINDEN_UZAK = ("uzak.sh", "pc2_istemci.sh")
ISTEMCI_BETIGI = ("_gorsel.py", "_istemci.py")
UZAK_KILITLERI = Path.home() / ".cache" / f"uzak-{os.environ.get('UZAK_MAKINE', 'pc2')}"


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
    """Kaydı `makine`de koşacak biçime getirir (yerinde): `makine` alanı; oto'dan ana makineye gelen işe UZAK=0 ve ekran
    ortamı; pc2'de makineden bağımsız istemci işi `pc2_istemci.sh <kanıt/> <betik> …` olur (özgün komut `ozgun_komut`)."""
    if veri["makine"] == "oto":
        veri["kaynak"] = "oto"
    veri["makine"] = makine
    if makine == "ana" and veri.get("kaynak") == "oto":
        veri["env"] = {**veri["env"], **veri.get("ekran_env", {}), "UZAK": "0"}
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
    """İşi kuyruğa yazar, kimliğini döner. `makine` oto ise komuttan seçilir (oto spool'u, ana ya da pc2)."""
    cwd = str(Path(cwd or os.getcwd()).resolve())
    env = {k: v for k, v in os.environ.items() if ENV_ANAHTARI.fullmatch(k)}
    ekran = {k: os.environ[k] for k in EKRAN_ENV if k in os.environ}
    tur = kendiliginden(komut)[0]
    pc2_istemci = any(Path(a).name == "pc2_istemci.sh" for a in komut)
    istemci = istemci or pc2_istemci
    if olcum:
        if makine == "pc2" or tur == "uzak":
            raise ValueError("--olcum işi yalnız ana makinede koşar (K0-33, K-E29 b)")
        makine = "ana"
    kanit = istemci_kaniti(komut, kanit) if istemci and not pc2_istemci else None
    if makine == "oto":
        if tur == "uzak":
            makine = "pc2"
        elif env.get("UZAK") == "0":
            makine = "ana"
        elif istemci:
            makine = "oto" if kanit else "ana"
        else:
            makine = "oto" if ekransiz_mi(komut) else "ana"
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
    onde = len(isler(makine, "bekleyen")) - 1 + len(isler(makine, "kosan"))
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


def uzak_dolu_yuva():
    """uzak.sh'in pc2 yuva kilitlerinden (`~/.cache/uzak-pc2/yuva-<n>`) tutulanların sayısı."""
    return sum(kilit_dolu(UZAK_KILITLERI / f"yuva-{n}") for n in range(1, int(os.environ.get("UZAK_YUVA", "2")) + 1))


def uzak_klasor_dolu(cwd):
    """Bu klasörün uzak.sh klasör kilidi tutuluyorsa True (pc2'de aynı klasörden tek iş)."""
    klasor = str(Path(cwd).resolve())
    return kilit_dolu(UZAK_KILITLERI / f"klasor-{hashlib.md5(klasor.encode()).hexdigest()[:12]}")


def doluluk(makine):
    """Makinenin dolu yuvası: kuyruğun koşan işleri; pc2'de kuyruk dışından tutulan uzak.sh yuvaları da."""
    dolu = sum(gerek(v, makine) for _, v in isler(makine, "kosan"))
    return max(dolu, uzak_dolu_yuva()) if makine == "pc2" else dolu


def gradle_oturumlari():
    """Ana makinede çalışan Gradle istemcilerinin (GradleWrapperMain) oturum kimlikleri."""
    oturumlar = []
    for yol in Path("/proc").glob("[0-9]*/cmdline"):
        try:
            if b"GradleWrapperMain" not in yol.read_bytes():
                continue
            oturumlar.append(int((yol.parent / "stat").read_text().rsplit(")", 1)[1].split()[3]))
        except (OSError, IndexError, ValueError):
            continue
    return oturumlar


def bos_ram_mb():
    return int(Path("/proc/meminfo").read_text().split("MemAvailable:")[1].split()[0]) // 1024


def dolu_mu(makine, cwd=None):
    """Kapı doluysa gerekçe, değilse None. pc2: kuyrukta bekleyen (pc2 ve oto), kuyruğun yuvaları, uzak.sh yuva ve klasör
    kilitleri; ana: kuyrukta bekleyen, Gradle >= 2, boş RAM < KAPI_RAM_MB (vars. 4096), KAPI_PORT dinleniyor."""
    bekleyen = len(isler(makine, "bekleyen")) + (len(isler("oto", "bekleyen")) if makine == "pc2" else 0)
    if bekleyen:
        return f"{makine}{'+oto' if makine == 'pc2' else ''} kuyruğunda {bekleyen} bekleyen iş"
    if sum(gerek(v, makine) for _, v in isler(makine, "kosan")) >= YUVA:
        return f"{makine} kuyruğunun {YUVA} yuvası dolu"
    if makine == "pc2":
        yuvalar = int(os.environ.get("UZAK_YUVA", "2"))
        if uzak_dolu_yuva() >= yuvalar:
            return f"pc2'nin {yuvalar} yuvası dolu (uzak.sh)"
        if uzak_klasor_dolu(cwd or os.getcwd()):
            return f"bu klasörden pc2'de koşan iş var ({Path(cwd or os.getcwd()).resolve()})"
        return None
    gradle = len(gradle_oturumlari())
    if gradle >= 2:
        return f"ana makinede {gradle} Gradle çalışıyor"
    bos = bos_ram_mb()
    if bos < int(os.environ.get("KAPI_RAM_MB", "4096")):
        return f"boş RAM {bos} MB"
    port = os.environ.get("KAPI_PORT")
    if port:
        with socket.socket() as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                s.bind(("", int(port)))
            except OSError:
                return f"port {port} dolu"
    return None


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
    for makine in MAKINELER:
        bilgi = json_oku(dizin(makine) / "kosucu.json") or {}
        kosucular.append(f"{makine} {'çalışıyor' if kosucu_canli(makine) else 'YOK'}")
        yuvalar.append(f"{makine} {doluluk(makine)}/{bilgi.get('yuva', YUVA)}")
    yuvalar.append(f"oto bekleyen {len(isler('oto', 'bekleyen'))}")
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
    e.add_argument("--makine", choices=("ana", "pc2", "oto"), default="oto")
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
    d = alt.add_parser("dolu-mu")
    d.add_argument("--makine", choices=MAKINELER, required=True)
    d.add_argument("--cwd")
    a = ayr.parse_args(argv)
    if a.alt == "ekle":
        if not komut:
            ayr.error("ekle: komut `--`'dan sonra verilir")
        try:
            print(ekle(a.makine, komut, a.yuva, a.istemci, a.log, olcum=a.olcum, kanit=a.kanit))
        except ValueError as hata:
            print(f"SIRA: {hata}", file=sys.stderr)
            return 2
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
    if a.alt == "iptal":
        return iptal(a.id)
    neden = dolu_mu(a.makine, a.cwd)
    if neden:
        print(neden)
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
