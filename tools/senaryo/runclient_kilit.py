#!/usr/bin/env python3
"""Aynı anda tek runClient kilidi (K0-21 rev. 2 (i), MAKINE.md): `KÖK/.vibe/runclient.lock`.

`pgrep -fa runClient` kapısı yarış koşuludur (iki işçi aynı anda "boş" görüp açabilir). Kilit dosyası `O_CREAT | O_EXCL`
ile atomik oluşturulur ve sahibin pid'ini, işlem başlangıç saatini (`/proc/<pid>/stat` 22. alan; pid yeniden kullanımına
karşı), worktree adını ve zamanı JSON olarak taşır. Kilit varsa 60 sn'de bir yeniden denenir; sahibi ölüyse (pid yok ya da
başlangıç saati tutmuyor) devralınır. Devralma ve bırakma ayrı bir koruma dosyasındaki `flock` altında yapılır: iki bekleyen
aynı ölü kilidi birlikte devralamaz.

Kullanım (worktree kökünden ya da KÖK'ten):
    python3 tools/senaryo/runclient_kilit.py kos <worktree> [--bekle SN] -- sh kosu.sh   # al, çalıştır, bırak
    python3 tools/senaryo/runclient_kilit.py al <worktree> --pid <uzun ömürlü pid> [--bekle SN]
    python3 tools/senaryo/runclient_kilit.py birak --pid <pid>
    python3 tools/senaryo/runclient_kilit.py durum
Python'dan: `al(worktree, pid)` / `birak(pid)` ya da `with kilit(worktree): ...`.

`al` ile kaydedilen pid, runClient boyunca yaşayan süreç olmalı (ör. koşu betiğinin `$$`'ı); kısa ömürlü bir pid ile alınan
kilidi bir sonraki bekleyen "sahibi ölü" diye devralır. Çıkış kodları: 0 tamam, 3 bekleme süresi doldu, 4 kilit başkasının.
Yol `DOGALAFETLER_RUNCLIENT_KILIT` ortam değişkeniyle değiştirilebilir (testler).
"""
import argparse
import contextlib
import fcntl
import json
import os
import subprocess
import sys
import time
from pathlib import Path

YENIDEN_DENEME_SN = 60


def kok():
    worktree = Path(__file__).resolve().parents[2]
    return worktree.parent.parent if worktree.parent.name == ".worktrees" else worktree


def kilit_yolu():
    ortam = os.environ.get("DOGALAFETLER_RUNCLIENT_KILIT")
    return Path(ortam) if ortam else kok() / ".vibe" / "runclient.lock"


def baslangic_saati(pid):
    """İşlemin başlangıç saati (saat tik'i); işlem yoksa None."""
    try:
        metin = Path(f"/proc/{pid}/stat").read_text()
    except (FileNotFoundError, ProcessLookupError, PermissionError, OSError):
        return None
    return int(metin[metin.rindex(")") + 2:].split()[19])


def yasiyor(kayit):
    pid = kayit.get("pid")
    if not isinstance(pid, int) or pid <= 0:
        return False
    simdi = baslangic_saati(pid)
    return simdi is not None and simdi == kayit.get("baslangic")


@contextlib.contextmanager
def _koruma(yol):
    yol.parent.mkdir(parents=True, exist_ok=True)
    with open(str(yol) + ".koruma", "a") as dosya:
        fcntl.flock(dosya, fcntl.LOCK_EX)
        try:
            yield
        finally:
            fcntl.flock(dosya, fcntl.LOCK_UN)


def oku(yol=None):
    yol = yol or kilit_yolu()
    try:
        return json.loads(yol.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None
    except (ValueError, OSError):
        return {}


def _dene(worktree, pid, yol, log):
    """Tek deneme: True alındı, aksi halde sahibin kaydı."""
    with _koruma(yol):
        mevcut = oku(yol)
        if mevcut is not None:
            if mevcut.get("pid") == pid and yasiyor(mevcut):
                return True
            if yasiyor(mevcut):
                return mevcut
            log(f"kilit sahibi ölü, devralınıyor: {mevcut}")
            yol.unlink(missing_ok=True)
        kayit = {"pid": pid, "baslangic": baslangic_saati(pid), "worktree": worktree,
                 "zaman": time.strftime("%Y-%m-%dT%H:%M:%S")}
        fd = os.open(yol, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o644)
        with os.fdopen(fd, "w", encoding="utf-8") as dosya:
            json.dump(kayit, dosya)
        return True


def al(worktree, pid=None, bekle_sn=1800, aralik=YENIDEN_DENEME_SN, log=print):
    """Kilidi alır; alındıysa True, {@code bekle_sn} dolduysa False."""
    pid = pid or os.getpid()
    if baslangic_saati(pid) is None:
        raise ValueError(f"pid {pid} yaşamıyor")
    yol = kilit_yolu()
    son = time.monotonic() + bekle_sn
    while True:
        sonuc = _dene(worktree, pid, yol, log)
        if sonuc is True:
            log(f"runClient kilidi alındı: {worktree} pid={pid} ({yol})")
            return True
        if time.monotonic() >= son:
            log(f"runClient kilidi alınamadı, sahibi: {sonuc}")
            return False
        log(f"runClient kilidi dolu ({sonuc.get('worktree')} pid={sonuc.get('pid')}); {aralik} sn sonra yeniden")
        time.sleep(min(aralik, max(0.0, son - time.monotonic())))


def birak(pid=None, log=print):
    """Kilit bu pid'inse siler; değilse dokunmaz ve False döner."""
    pid = pid or os.getpid()
    yol = kilit_yolu()
    with _koruma(yol):
        mevcut = oku(yol)
        if mevcut is None:
            return True
        if mevcut.get("pid") != pid:
            log(f"kilit başkasının, bırakılmadı: {mevcut}")
            return False
        yol.unlink(missing_ok=True)
        log(f"runClient kilidi bırakıldı: pid={pid}")
        return True


@contextlib.contextmanager
def kilit(worktree, bekle_sn=1800, log=print):
    if not al(worktree, os.getpid(), bekle_sn, log=log):
        raise TimeoutError("runClient kilidi alınamadı")
    try:
        yield
    finally:
        birak(os.getpid(), log=log)


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    satir = []
    if "--" in argv:
        ayrac = argv.index("--")
        argv, satir = argv[:ayrac], argv[ayrac + 1:]
    ayr = argparse.ArgumentParser(description="Aynı anda tek runClient kilidi")
    alt = ayr.add_subparsers(dest="komut", required=True)
    a = alt.add_parser("al")
    a.add_argument("worktree")
    a.add_argument("--pid", type=int, required=True)
    a.add_argument("--bekle", type=float, default=1800)
    b = alt.add_parser("birak")
    b.add_argument("--pid", type=int, required=True)
    k = alt.add_parser("kos")
    k.add_argument("worktree")
    k.add_argument("--bekle", type=float, default=1800)
    alt.add_parser("durum")
    arg = ayr.parse_args(argv)
    if arg.komut == "al":
        return 0 if al(arg.worktree, arg.pid, arg.bekle) else 3
    if arg.komut == "birak":
        return 0 if birak(arg.pid) else 4
    if arg.komut == "durum":
        mevcut = oku()
        print("kilit yok" if mevcut is None else f"{mevcut} yaşıyor={yasiyor(mevcut)}")
        return 0
    if not satir:
        ayr.error("kos: komut satırı gerekli (-- sonrası)")
    if not al(arg.worktree, os.getpid(), arg.bekle):
        return 3
    try:
        return subprocess.call(satir)
    finally:
        birak(os.getpid())


if __name__ == "__main__":
    sys.exit(main())
