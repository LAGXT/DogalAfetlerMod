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

`kos` komutu kendi oturumunda (`start_new_session`) başlatır; `kos` SIGTERM/SIGINT/SIGHUP alırsa çocuğun süreç
grubuna SIGTERM gönderir (30 sn içinde ölmezse SIGKILL), bekler, kilidi bırakır ve 128 + sinyal koduyla çıkar. Böylece
öldürülen `kos`'un çocuğu kilitsiz yaşamaz.

K-E27 (asılı istemci): `kos` komutu bittiğinde (`timeout` dahil) `<worktree>/run`'da çalışan runClient JVM'i hâlâ yaşıyorsa en
çok `KAPANIS_SN` (90) sn bekler, sonra yalnız o JVM'i ve onu başlatan Gradle daemon'ını öldürür; kilit JVM ölmeden bırakılmaz.
Görsel betiklerin bekçi döngüsü `KapanisBekcisi` ile biter: JVM çıkınca hemen; otomasyonun bitiş satırından ya da kilidin
bırakılmasından `KAPANIS_SN` sn sonra JVM yaşıyorsa onu öldürerek.

`al` ile kaydedilen pid, runClient boyunca yaşayan süreç olmalı (ör. koşu betiğinin `$$`'ı); kısa ömürlü bir pid ile alınan
kilidi bir sonraki bekleyen "sahibi ölü" diye devralır. Çıkış kodları: 0 tamam, 3 bekleme süresi doldu, 4 kilit başkasının.
Yol `DOGALAFETLER_RUNCLIENT_KILIT` ortam değişkeniyle değiştirilebilir (testler).

Ekran kipi (`DOGALAFETLER_EKRAN`, `ekran_pc2()`): `pc2` ise görsel betik ikinci makinede (XFCE/X11) koşar ve istemci görünür
açılır (`pc2_istemci.sh`). Kullanıcının oturumu olmadığından K0-21 rev. 2 denetimleri (Hyprland exec kuralı (a), belirteç (b),
pencere (c), odak bekçisi (d)) uygulanmaz ve oyun içi girdi olayları (e) koşuyu geçersiz kılmaz; kareler oyun içi
(`GorselOtomasyon.kareIste` → `Screenshot.grab`, ana render hedefi) olduğundan yakalama aracı gerekmez. FPS/CPU ölçümü pc2'de
geçersizdir: log'a `MAKINE=<ad>` yazılır, `olcum_tablosu.py` o klasörün `OLCUM` satırlarını `bilgi` sayar.
Kipi tanıyan betik: `radar_gorsel.py`.
"""
import argparse
import contextlib
import fcntl
import json
import os
import signal
import subprocess
import sys
import time
from pathlib import Path

YENIDEN_DENEME_SN = 60
SINYALLER = (signal.SIGTERM, signal.SIGINT, signal.SIGHUP)
OLDURME_BEKLEME_SN = 30
KAPANIS_SN = 90


def kok():
    worktree = Path(__file__).resolve().parents[2]
    return worktree.parent.parent if worktree.parent.name == ".worktrees" else worktree


def kilit_yolu():
    ortam = os.environ.get("DOGALAFETLER_RUNCLIENT_KILIT")
    return Path(ortam) if ortam else kok() / ".vibe" / "runclient.lock"


def ekran_pc2():
    """Görsel betik pc2 kipinde mi (`DOGALAFETLER_EKRAN=pc2`, `pc2_istemci.sh` verir)."""
    return os.environ.get("DOGALAFETLER_EKRAN", "") == "pc2"


def makine():
    """Bilgisayar adı (`gradle_kapisi.sh` `MAKINE=` satırıyla aynı kaynak)."""
    with contextlib.suppress(OSError):
        return Path("/etc/hostname").read_text().strip()
    return os.uname().nodename


def pc2_baslat(betik, log):
    """pc2 kipi: başlatma betiğini `sh` ile görünür açar (`DISPLAY` ortamdan); Hyprland kuralı ve (b)/(c)/(d) denetimleri
    yoktur. Betik aynı süreç grubunda kalır: `uzak.sh` kesmesi grubu durdurunca `kos` da sinyal alır. `Popen` döner."""
    ekran = os.environ.get("DISPLAY")
    if not ekran:
        raise RuntimeError("pc2 kipi DISPLAY ister (pc2_istemci.sh DISPLAY=:0 verir)")
    log(f"(a) pc2 kipi: MAKINE={makine()} DISPLAY={ekran}; istemci görünür açılıyor, Hyprland kuralı ve (b)/(c)/(d) "
        "denetimleri uygulanmaz; kareler oyun içi, OLCUM satırları bilgi")
    return subprocess.Popen(["sh", str(betik)], stdin=subprocess.DEVNULL)


def pc2_girdi_notu(sayi):
    """pc2 kipinde (e) satırı: girdi olayları sayılır ama koşuyu geçersiz kılmaz."""
    return f"(e) oyun içi girdi olayı: {sayi} (pc2: geçersiz değil, uygulanmaz; pencere görünür, sahibin masaüstü değil)"


def _stat(pid):
    """İşlemin durum harfi, ana süreci ve başlangıç saati (saat tik'i); işlem yoksa None."""
    try:
        metin = Path(f"/proc/{pid}/stat").read_text()
    except (FileNotFoundError, ProcessLookupError, PermissionError, OSError):
        return None
    alanlar = metin[metin.rindex(")") + 2:].split()
    return alanlar[0], int(alanlar[1]), int(alanlar[19])


def baslangic_saati(pid):
    """İşlemin başlangıç saati (saat tik'i); işlem yoksa None."""
    durum = _stat(pid)
    return None if durum is None else durum[2]


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


def _komut_satiri(pid):
    try:
        return Path(f"/proc/{pid}/cmdline").read_bytes().replace(b"\0", b" ").decode(errors="replace")
    except OSError:
        return ""


def istemci_jvmleri(worktree):
    """Worktree'nin yaşayan runClient JVM'leri: komut satırında `forgeclientuserdev`, çalışma dizini `<worktree>/run`."""
    run = os.path.join(os.path.realpath(worktree), "run")
    bulunan = []
    for giris in os.scandir("/proc"):
        if not giris.name.isdigit() or "forgeclientuserdev" not in _komut_satiri(giris.name):
            continue
        with contextlib.suppress(OSError):
            if os.readlink(f"/proc/{giris.name}/cwd") == run:
                bulunan.append(int(giris.name))
    return bulunan


def istemci_oldur(worktree, neden, log=print):
    """K-E27: worktree'nin runClient JVM'lerini ve ana süreçleri `GradleDaemon` ise onu SIGKILL ile öldürür, ölmelerini en çok
    10 sn bekler; öldürülen JVM pid'lerini döner. Daemon bu JVM'in yapısını yürüten süreçtir (`--no-daemon` koşusunda tek
    kullanımlık); başka worktree'nin süreçlerine dokunulmaz."""
    oldurulen = []
    for pid in istemci_jvmleri(worktree):
        durum = _stat(pid)
        daemon = durum[1] if durum and "GradleDaemon" in _komut_satiri(durum[1]) else None
        for hedef in (pid, daemon):
            if hedef:
                with contextlib.suppress(ProcessLookupError):
                    os.kill(hedef, signal.SIGKILL)
        log(f"K-E27: asılı istemci öldürüldü pid={pid}" + (f", Gradle daemon pid={daemon}" if daemon else "") + f" ({neden})")
        oldurulen.append(pid)
    son = time.monotonic() + 10
    while any(p in istemci_jvmleri(worktree) for p in oldurulen) and time.monotonic() < son:
        time.sleep(0.1)
    return oldurulen


def _istemci_kapanisi(worktree, log, dur):
    """K-E27: `kos` komutu bittiğinde worktree'nin istemci JVM'i yaşıyorsa çıkmasını en çok `KAPANIS_SN` sn (`dur()` True ise
    hiç) bekler, sonra öldürür; kilit ancak ondan sonra bırakılır."""
    kalan = istemci_jvmleri(worktree)
    if not kalan:
        return
    log(f"K-E27: runClient komutu bitti, istemci JVM'i yaşıyor (pid={kalan}); en çok {KAPANIS_SN} sn bekleniyor")
    son = time.monotonic() + KAPANIS_SN
    while not dur() and time.monotonic() < son:
        time.sleep(0.5)
        if not istemci_jvmleri(worktree):
            log("K-E27: istemci JVM'i kendiliğinden çıktı")
            return
    istemci_oldur(worktree, "runClient komutu bittikten sonra çıkmadı", log)


class KapanisBekcisi:
    """K-E27: görsel betiğin bekçi döngüsünün kapanış kuralı; döngü `while not bekci.bitti_mi():` olur.

    `bitti_mi()` istemci JVM'i (`pid`, başlangıç saatiyle) çıkınca True döner. JVM yaşarken iki saat başlar: otomasyonun bitiş
    satırı (`desen`) loglardan birinde göründüğünde ve runClient'ın bu worktree için aldığı kilit bırakıldığında (`kos` çıktı ya
    da öldü). Önce başlayandan `KAPANIS_SN` sn sonra JVM hâlâ yaşıyorsa worktree'nin JVM'i ve Gradle daemon'ı öldürülür
    (`istemci_oldur`) ve True döner. `otomasyon_bitti` True ise otomasyon kapanış satırına kadar tamamlanmıştır: öldürme koşuyu
    geçersiz kılmaz (kareler ve sayımlar o satırdan önce yazıldı).
    """

    def __init__(self, worktree, pid, loglar, desen, log=print):
        self.worktree = str(worktree)
        self.pid = pid
        self.baslangic = baslangic_saati(pid)
        self.loglar = [Path(p) for p in loglar]
        self.desen = desen.encode("utf-8")
        self.log = log
        self.otomasyon_bitti = False
        self.olduruldu = False
        self._okunan = {}
        self._bitti_an = None
        self._kilit_an = None
        kayit = oku()
        self._kilit = kayit if kayit and kayit.get("worktree") == self.worktree and yasiyor(kayit) else None

    def _jvm_yasiyor(self):
        durum = _stat(self.pid)
        return durum is not None and durum[0] != "Z" and durum[2] == self.baslangic

    def _satir_goruldu(self):
        for yol in self.loglar:
            try:
                with open(yol, "rb") as dosya:
                    konum = self._okunan.get(yol, 0)
                    if dosya.seek(0, os.SEEK_END) < konum:
                        konum = 0
                    dosya.seek(max(0, konum - len(self.desen)))
                    parca = dosya.read()
                    self._okunan[yol] = dosya.tell()
            except OSError:
                continue
            if self.desen in parca:
                return True
        return False

    def _kilit_bizde(self):
        kayit = oku()
        return bool(kayit) and kayit.get("pid") == self._kilit.get("pid") and yasiyor(kayit)

    def bitti_mi(self):
        if not self._jvm_yasiyor():
            return True
        simdi = time.monotonic()
        if self._bitti_an is None and self._satir_goruldu():
            self._bitti_an = simdi
            self.otomasyon_bitti = True
            self.log(f"K-E27: otomasyon bitiş satırı görüldü; istemci {KAPANIS_SN} sn içinde çıkmazsa öldürülür")
        if self._kilit_an is None and self._kilit is not None and not self._kilit_bizde():
            self._kilit_an = simdi
            self.log(f"K-E27: runClient kilidi bırakıldı, istemci pid={self.pid} yaşıyor; {KAPANIS_SN} sn içinde çıkmazsa "
                     "öldürülür")
        anlar = [a for a in (self._bitti_an, self._kilit_an) if a is not None]
        if not anlar or simdi - min(anlar) < KAPANIS_SN:
            return False
        neden = "bitiş satırından" if self._bitti_an == min(anlar) else "kilit bırakıldıktan"
        self.olduruldu = bool(istemci_oldur(self.worktree, f"{neden} {KAPANIS_SN} sn sonra çıkmadı", self.log))
        return True


def kos(worktree, satir, bekle_sn=1800, log=print):
    """Kilidi alır, komutu kendi süreç grubunda çalıştırır; komut bitince worktree'nin istemci JVM'i yaşıyorsa K-E27
    kapanışını uygular, sonra bırakır. Çıkış kodu komutunki (sinyalde 128 + sinyal)."""
    if not al(worktree, os.getpid(), bekle_sn, log=log):
        return 3
    cocuk = None
    gelen = []

    def durdur(signum, _cerceve):
        # İşleyici beklemez: dıştaki wait() Popen'in waitpid kilidini tutuyor olabilir.
        if not gelen:
            log(f"kos: sinyal {signal.Signals(signum).name}, çocuk süreç grubu durduruluyor")
            gelen.append((signum, time.monotonic() + OLDURME_BEKLEME_SN))
            if cocuk is not None:
                with contextlib.suppress(ProcessLookupError):
                    os.killpg(cocuk.pid, signal.SIGTERM)

    eski = {s: signal.signal(s, durdur) for s in SINYALLER}
    try:
        signal.pthread_sigmask(signal.SIG_BLOCK, SINYALLER)
        try:
            cocuk = subprocess.Popen(satir, start_new_session=True,
                                     preexec_fn=lambda: signal.pthread_sigmask(signal.SIG_UNBLOCK, SINYALLER))
        finally:
            signal.pthread_sigmask(signal.SIG_UNBLOCK, SINYALLER)
        if gelen:
            with contextlib.suppress(ProcessLookupError):
                os.killpg(cocuk.pid, signal.SIGTERM)
        oldurulen = False
        while True:
            try:
                kod = cocuk.wait(0.5)
                break
            except subprocess.TimeoutExpired:
                if gelen and not oldurulen and time.monotonic() >= gelen[0][1]:
                    with contextlib.suppress(ProcessLookupError):
                        os.killpg(cocuk.pid, signal.SIGKILL)
                    oldurulen = True
        if gelen:
            with contextlib.suppress(ProcessLookupError):
                os.killpg(cocuk.pid, signal.SIGKILL)
        _istemci_kapanisi(worktree, log, lambda: bool(gelen))
        if gelen:
            return 128 + gelen[0][0]
        return 128 - kod if kod < 0 else kod
    finally:
        for s, h in eski.items():
            signal.signal(s, h)
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
    return kos(arg.worktree, satir, arg.bekle)


if __name__ == "__main__":
    sys.exit(main())
