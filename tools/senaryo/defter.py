#!/usr/bin/env python3
"""Test defteri (K-E51): geçen test aynı girdi hash'inde bir daha koşmaz; kabul = defterde yeşil satır.

Defter `KÖK/.vibe/test-defteri.tsv` (ana kopya, git'te tutulur; worktree'ler de oraya yazar). Sütunlar (sekmeyle):
  test_kimligi  girdi_hash  sonuc  makine  HEAD  tarih  log
Girdi hash'i: kimliğin yollarının HEAD ağaç/blob hash'leri (`git ls-tree HEAD -- <yollar>`), o yollardaki commit'lenmemiş
fark (`git diff HEAD --binary -- <yollar>`) ve izlenmeyen dosyaların hash'leri; birleşik sha256'nın ilk 12 hanesi. HEAD
sütunu yollarda commit'lenmemiş fark varsa `+` ile biter.

Kimlik (ilk `:`'dan sonrası filtre/argüman; önce `+` ile birleşen parçalar, `@<uyum>` uyum modu) → yollar:
  build, test                   src/main src/generated src/test build.gradle settings.gradle gradle.properties gradle
  gametest[:<filtre>]           src/main src/generated build.gradle
  <ad>_sunucu[:<arg>=<değer>]   src/main src/generated tools/senaryo/<ad>_sunucu.py tools/senaryo/cekirdek_sunucu.py
  <ad>_gorsel, <ad>_istemci     src/main src/generated tools/senaryo/<betik>.py tools/senaryo/runclient_kilit.py

Kullanım (proje ya da worktree kökünden):
  defter.py hash <kimlik>                                 girdi hash'i
  defter.py bak <kimlik> [--hash H]                       aynı hash'te `gecti` varsa "DEFTER: geçti @…", çıkış 0; yoksa 1
  defter.py yaz <kimlik> <sonuc|çıkış kodu> <log> [--hash H] [--makine M]   satır ekler, aynı kimlik+hash varsa günceller
  defter.py kimlik -- <gradle argümanları>                gradle_kapisi.sh için kimlik (TEST_FILTRE dahil); defterlik değilse boş
Çıkış kodu 0 → gecti; 2, 3, 4, 130, 143 (kullanım, kapı, pc2 kapalı, kesildi) yazılmaz; diğerleri kaldi.
DEFTER_ZORLA=1: `bak` hep 1 döner (koşu zorlanır), sonuç yine yazılır.
"""
import argparse
import fcntl
import hashlib
import os
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path

W = Path(__file__).resolve().parents[2]
KOK = W.parents[1] if W.parent.name == ".worktrees" else W
DEFTER = KOK / ".vibe" / "test-defteri.tsv"
BASLIK = "test_kimligi	girdi_hash	sonuc	makine	HEAD	tarih	log\n"
MOD = ["src/main", "src/generated"]
DERLEME = MOD + ["src/test", "build.gradle", "settings.gradle", "gradle.properties", "gradle"]
YAZILMAYAN_KODLAR = {2, 3, 4, 130, 143}

# gradle_kapisi.sh görevleri: defterli olanlar ve onların yanında yutulanlar (başka görev varsa koşu defterlenmez).
DEFTERLI = {"build": "build", "test": "test", "check": "test", "runGameTestServer": "gametest"}
SIRA = ("build", "test", "gametest")
YUTULAN = {"clean", "cleanTest", "compileJava", "compileTestJava", "classes", "testClasses", "processResources",
           "assemble", "jar", "reobfJar"}
DEGERLI_SECENEK = {"-p", "--project-dir", "-I", "--init-script", "-g", "--gradle-user-home", "-c", "--settings-file",
                   "-b", "--build-file", "--include-build", "-P", "--project-prop", "-D", "--system-prop"}


def git(*arg, girdi=None):
    return subprocess.run(["/usr/bin/git", "-C", str(W), *arg], input=girdi, capture_output=True, check=True).stdout


def yollar(kimlik):
    secilen = []
    for parca in kimlik.split(":", 1)[0].split("+"):
        ad = parca.split("@", 1)[0]
        if ad in ("build", "test"):
            ek = DERLEME
        elif ad == "gametest":
            ek = MOD + ["build.gradle"]
        elif ad.endswith("_sunucu"):
            ek = MOD + [f"tools/senaryo/{ad}.py", "tools/senaryo/cekirdek_sunucu.py"]
        elif ad.endswith(("_gorsel", "_istemci")):
            ek = MOD + [f"tools/senaryo/{ad}.py", "tools/senaryo/runclient_kilit.py"]
        else:
            raise ValueError(f"bilinmeyen test kimliği: {parca!r} (build, test, gametest, <ad>_sunucu, <ad>_gorsel, <ad>_istemci)")
        secilen += [y for y in ek if y not in secilen]
    return secilen


def girdi_durumu(kimlik):
    """(hash, kirli): kirli = yollarda commit'lenmemiş fark ya da izlenmeyen dosya var."""
    secilen = yollar(kimlik)
    h = hashlib.sha256(git("ls-tree", "HEAD", "--", *secilen))
    fark = git("diff", "HEAD", "--binary", "--", *secilen)
    izsiz = git("ls-files", "-o", "--exclude-standard", "-z", "--", *secilen)
    h.update(b"\0fark\0" + fark)
    if izsiz:
        h.update(b"\0izsiz\0" + izsiz + git("hash-object", "--stdin-paths", girdi=izsiz.replace(b"\0", b"\n")))
    return h.hexdigest()[:12], bool(fark or izsiz)


def girdi_hash(kimlik):
    return girdi_durumu(kimlik)[0]


def satirlar():
    try:
        metin = DEFTER.read_text(encoding="utf-8")
    except FileNotFoundError:
        return []
    return [s.split("	") for s in metin.splitlines()[1:] if s.count("	") == 6]


def kayit(kimlik, girdi):
    return next((s for s in satirlar() if s[0] == kimlik and s[1] == girdi), None)


def bak(kimlik, girdi=None, cikti=print):
    """Aynı kimlik ve girdi hash'inde `gecti` satırı varsa True (koşma). DEFTER_ZORLA=1 hep False."""
    girdi = girdi or girdi_hash(kimlik)
    s = kayit(kimlik, girdi)
    if os.environ.get("DEFTER_ZORLA") == "1":
        cikti(f"DEFTER: DEFTER_ZORLA=1, {kimlik} @{girdi} koşulacak")
        return False
    if s and s[2] == "gecti":
        cikti(f"DEFTER: geçti @{girdi} ({s[5]}, {s[6]})")
        return True
    cikti(f"DEFTER: {kimlik} @{girdi} " + (f"önceki sonuç {s[2]} ({s[5]}), yeniden koşulacak" if s else "kayıt yok, koşulacak"))
    return False


def log_makinesi(log):
    try:
        with open(log, encoding="utf-8", errors="replace") as f:
            for _ in range(3):
                satir = f.readline()
                if satir.startswith("MAKINE="):
                    return satir[7:].strip()
    except OSError:
        pass
    return None


def yaz(kimlik, sonuc, log, girdi=None, makine=None, cikti=print):
    """Sonucu yazar (aynı kimlik+hash satırı güncellenir). `sonuc` çıkış kodu da olabilir; YAZILMAYAN_KODLAR yazılmaz."""
    if isinstance(sonuc, int) or str(sonuc).lstrip("-").isdigit():
        kod = int(sonuc)
        if kod in YAZILMAYAN_KODLAR:
            cikti(f"DEFTER: çıkış {kod} (koşu tamamlanmadı), yazılmadı")
            return
        sonuc = "gecti" if kod == 0 else "kaldi"
    if girdi:
        kirli = None
    else:
        girdi, kirli = girdi_durumu(kimlik)
    if kirli is None:
        kirli = bool(git("status", "--porcelain", "--", *yollar(kimlik)).strip())
    bas = git("rev-parse", "--short", "HEAD").decode().strip() + ("+" if kirli else "")
    log_yolu = Path(log).resolve() if log not in (None, "-") else None
    makine = makine or (log_makinesi(log_yolu) if log_yolu else None) or socket.gethostname()
    if log_yolu is None:
        log_metni = "-"
    elif log_yolu.is_relative_to(KOK):
        log_metni = str(log_yolu.relative_to(KOK))
    else:
        log_metni = str(log_yolu)
    yeni = [kimlik, girdi, sonuc, makine, bas, time.strftime("%Y-%m-%dT%H:%M"), log_metni]
    if any("	" in a or "\n" in a for a in yeni):
        raise ValueError("defter alanında sekme ya da satır sonu olamaz")
    DEFTER.parent.mkdir(parents=True, exist_ok=True)
    with open(DEFTER.parent / ".test-defteri.kilit", "a") as kilit:
        fcntl.flock(kilit, fcntl.LOCK_EX)
        eski = satirlar()
        kalan = [s for s in eski if not (s[0] == kimlik and s[1] == girdi)]
        gecici = DEFTER.with_name(f".{DEFTER.name}.{os.getpid()}")
        gecici.write_text(BASLIK + "".join("	".join(s) + "\n" for s in kalan + [yeni]), encoding="utf-8")
        os.replace(gecici, DEFTER)
    cikti(f"DEFTER: {kimlik} @{girdi} {sonuc} yazıldı ({makine}, {bas}, {log_metni})")


def gradle_kimligi(argumanlar, filtre=None):
    """gradle_kapisi.sh argümanlarından kimlik; defterli görev yoksa, tanınmayan görev ya da `-x` varsa None."""
    parcalar, uyum, atla = set(), None, None
    for g in argumanlar:
        if atla:
            if atla in ("-P", "--project-prop"):
                uyum, filtre = _ozellik(g, uyum, filtre)
            atla = None
            continue
        if g in ("-x", "--exclude-task"):
            return None
        if g in DEGERLI_SECENEK:
            atla = g
        elif g.startswith("-P"):
            uyum, filtre = _ozellik(g[2:], uyum, filtre)
        elif not g.startswith("-"):
            ad = g.rsplit(":", 1)[-1]
            if ad in DEFTERLI:
                parcalar.add(DEFTERLI[ad])
            elif ad not in YUTULAN:
                return None
    if not parcalar:
        return None
    ek = f"@{uyum}" if uyum and uyum != "yok" else ""
    kimlik = "+".join(p + ek for p in SIRA if p in parcalar)
    return kimlik + (f":{filtre}" if filtre and "gametest" in parcalar else "")


def _ozellik(atama, uyum, filtre):
    ad, _, deger = atama.partition("=")
    if ad == "uyum":
        uyum = deger
    elif ad == "test_filtre":
        filtre = deger
    return uyum, filtre


def senaryo_kimligi(ayristirici, a):
    """`<ad>_sunucu` + varsayılandan farklı betik seçenekleri (kanıt klasörü ve port kimliğe girmez)."""
    ekler = []
    for eylem in ayristirici._actions:
        if eylem.dest in ("help", "klasor", "kanit", "port"):
            continue
        deger = getattr(a, eylem.dest, None)
        if deger != eylem.default:
            ekler.append(f"{eylem.dest}={deger}")
    return Path(sys.argv[0]).stem + (":" + ",".join(ekler) if ekler else "")


def betik_kimligi():
    """`<betik>` + yol olmayan argümanlar (`tsunami_gorsel:hepsi`); kanıt yolları kimliğe girmez."""
    ekler = [a for a in sys.argv[1:] if "/" not in a]
    return Path(sys.argv[0]).stem + (":" + ",".join(ekler) if ekler else "")


def alt_surec(komut, env=None):
    """Komutu koşar, TERM/HUP'u ona iletir (iptal uzak.sh'e ulaşsın), çıkış kodunu döner."""
    sys.stdout.flush()
    p = subprocess.Popen(komut, env=env)
    eski = {s: signal.signal(s, lambda n, _c: p.send_signal(n)) for s in (signal.SIGTERM, signal.SIGHUP)}
    try:
        while True:
            try:
                return p.wait()
            except KeyboardInterrupt:
                continue
    finally:
        for s, h in eski.items():
            signal.signal(s, h)


def sarmala(main, log=None):
    """`*_gorsel.py`/`*_istemci.py` ana bloğu: `sys.exit(defter.sarmala(main, KANIT))`. Aynı girdi hash'inde `gecti`
    varsa `main` koşmaz (0); yoksa koşar ve sonucu yazar. Log verilmezse ilk yol argümanı."""
    if os.environ.get("DEFTER_ICINDE"):
        return main()
    kimlik = betik_kimligi()
    girdi = girdi_hash(kimlik)
    if bak(kimlik, girdi):
        return 0
    log = log or next((a for a in sys.argv[1:] if "/" in a), None)
    os.environ["DEFTER_ICINDE"] = "1"
    try:
        kod = main()
    except SystemExit as e:
        kod = e.code
    except KeyboardInterrupt:
        raise
    except Exception:
        yaz(kimlik, "kaldi", log, girdi=girdi)
        raise
    kod = 0 if kod is None else kod if isinstance(kod, int) else 1
    yaz(kimlik, kod, log, girdi=girdi)
    return kod


def main():
    ayr = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    alt = ayr.add_subparsers(dest="komut", required=True)
    alt.add_parser("hash").add_argument("kimlik")
    b = alt.add_parser("bak")
    b.add_argument("kimlik")
    b.add_argument("--hash")
    y = alt.add_parser("yaz")
    y.add_argument("kimlik")
    y.add_argument("sonuc")
    y.add_argument("log")
    y.add_argument("--hash")
    y.add_argument("--makine")
    k = alt.add_parser("kimlik")
    k.add_argument("gradle", nargs=argparse.REMAINDER)
    a = ayr.parse_args()
    try:
        if a.komut == "hash":
            print(girdi_hash(a.kimlik))
        elif a.komut == "bak":
            return 0 if bak(a.kimlik, a.hash) else 1
        elif a.komut == "yaz":
            yaz(a.kimlik, a.sonuc, a.log, girdi=a.hash, makine=a.makine)
        else:
            arg = a.gradle[1:] if a.gradle[:1] == ["--"] else a.gradle
            print(gradle_kimligi(arg, os.environ.get("TEST_FILTRE") or None) or "")
    except (ValueError, subprocess.CalledProcessError) as e:
        print(f"DEFTER: {e}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
