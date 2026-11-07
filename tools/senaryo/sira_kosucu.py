#!/usr/bin/env python3
"""İş kuyruğu koşucusu (K-E51): bir makinenin spool'unu (`sira.py`) izler, en çok --yuva iş koşar, biten işi biten/'e
yazar ve KÖK/.vibe/pano.md'yi günceller (pc2 koşucusu panoyu pc2'deki aynı yola da kopyalar: pc2'de `watch cat`).

Kullanım: sira_kosucu.py --makine ana|pc2 [--yuva 2]
Ayakta kalma: systemd --user `dogalafet-sira@<makine>` (tools/senaryo/dogalafet-sira@.service, KillMode=process: koşucu
yeniden başlarken koşan işler sürer). İki koşucu da ana makinede koşar: pc2'de depo (.git, .vibe) ve spool yok, işi
komutun kendisi K-E44 ile pc2'ye taşır (uzak.sh eşitler, yuva/klasör kilidi alır, log ve kanıtı geri çeker).
Sıra eklenme sırasıdır; baştaki iş sığmıyorsa arkadakiler öne geçmez (iki yuvalı istemci işi aç kalmasın); yalnız
pc2'de koşan bir işle aynı klasörden gelen iş (uzak.sh klasör kilidi) atlanır. Her iş kendi oturumunda `--kos` kipindeki
koşturucuyla koşar; koşturucu çıkışı kendisi yazar, koşucu ölse de kayıt tamamlanır. pc2'nin `--istemci` işi süresince
koşturucu ana makinedeki uzak.sh yuva kilitlerinin hepsini tutar ve işe UZAK_YUVA_TUTULU=1 verir.
"""
import argparse
import fcntl
import os
import shlex
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sira  # noqa: E402

PANO_YENILEME_SN = 30


def canli(pid):
    if not pid:
        return False
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    return True


def bitir(yol, veri, **alanlar):
    veri.update(alanlar)
    sira.json_yaz(sira.dizin(veri["makine"], "biten") / yol.name, veri)
    yol.unlink(missing_ok=True)


def kostur(yol):
    """`--kos` kipi: tek iş. Oturum lideri (koşucu `start_new_session` ile açar); `sira.py iptal` oturuma TERM yollar."""
    veri = sira.json_oku(yol)
    veri.update(kosturucu=os.getpid(), durum="kosuyor", baslangic=time.time())
    sira.json_yaz(yol, veri)
    Path(veri["log"]).parent.mkdir(parents=True, exist_ok=True)
    log = open(veri["log"], "ab", buffering=0)
    env = {**os.environ, **veri["env"], "SIRA_ICINDE": "1", "SIRA_IS": veri["id"], "SIRA_MAKINE": veri["makine"]}
    log.write(f"SIRA: {veri['id']} {veri['makine']} başladı {time.strftime('%Y-%m-%d %H:%M:%S')} cwd={veri['cwd']}\n"
              f"SIRA: komut {shlex.join(veri['komut'])}\n".encode())
    iptal = []
    for s in (signal.SIGTERM, signal.SIGINT, signal.SIGHUP):
        signal.signal(s, lambda n, _c: iptal.append(n))
    kilitler = []
    if veri["makine"] == "pc2" and veri.get("istemci"):
        for n in range(1, int(env.get("UZAK_YUVA", "2")) + 1):
            sira.UZAK_KILITLERI.mkdir(parents=True, exist_ok=True)
            kilit = open(sira.UZAK_KILITLERI / f"yuva-{n}", "w")
            fcntl.flock(kilit, fcntl.LOCK_EX)
            kilitler.append(kilit)
        env["UZAK_YUVA_TUTULU"] = "1"
        log.write(f"SIRA: istemci işi, uzak.sh yuva kilitleri tutuluyor ({len(kilitler)})\n".encode())
    try:
        surec = subprocess.Popen(veri["komut"], cwd=veri["cwd"], env=env, stdin=subprocess.DEVNULL, stdout=log,
                                 stderr=subprocess.STDOUT)
    except OSError as hata:
        log.write(f"SIRA: başlatılamadı: {hata}\n".encode())
        kod = 127
    else:
        kod = surec.wait()
    bitti = time.time()
    sure = round(bitti - veri["baslangic"])
    log.write(f"SIRA: {veri['id']} çıkış={kod} süre={sure} sn\n".encode())
    bitir(yol, veri, durum="iptal" if iptal else "bitti", cikis=kod, bitti=bitti, sure=sure)


class Kosucu:
    def __init__(self, makine, yuva):
        self.makine, self.yuva = makine, yuva
        self.cocuklar = {}
        self.dur = False
        self.pc2_metin = None
        self.pc2_olay = threading.Event()

    def kilitle(self):
        self.kilit = open(sira.dizin(self.makine) / "kosucu.lock", "a")
        try:
            fcntl.flock(self.kilit, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            print(f"SIRA: {self.makine} koşucusu zaten çalışıyor", file=sys.stderr)
            sys.exit(1)
        sira.json_yaz(sira.dizin(self.makine) / "kosucu.json",
                      {"pid": os.getpid(), "yuva": self.yuva, "baslangic": time.time()})

    def yetimleri_topla(self):
        """Koşturucusu ölmüş ama kaydı kosan/'da kalan iş yarım kalır."""
        for yol, veri in sira.isler(self.makine, "kosan"):
            surec = self.cocuklar.get(veri["id"])
            if surec is not None:
                if surec.poll() is None:
                    continue
                del self.cocuklar[veri["id"]]
            elif canli(veri.get("kosturucu")) or time.time() - veri.get("baslangic", 0) < 10:
                continue
            if yol.exists():
                bitir(yol, veri, durum="yarim", bitti=time.time(), sure=round(time.time() - veri.get("baslangic", 0)))
        for kimlik in [k for k, s in self.cocuklar.items() if s.poll() is not None]:
            del self.cocuklar[kimlik]

    def baslat(self):
        kosan = [v for _, v in sira.isler(self.makine, "kosan")]
        dolu = sum(v.get("yuva", 1) for v in kosan)
        klasorler = {v["cwd"] for v in kosan}
        for yol, veri in sira.isler(self.makine, "bekleyen"):
            gerek = min(veri.get("yuva", 1), self.yuva)
            if self.makine == "pc2" and veri["cwd"] in klasorler:
                continue
            if dolu + gerek > self.yuva:
                break
            hedef = sira.dizin(self.makine, "kosan") / yol.name
            try:
                os.rename(yol, hedef)
            except FileNotFoundError:
                continue
            veri.update(durum="basliyor", baslangic=time.time())
            sira.json_yaz(hedef, veri)
            self.cocuklar[veri["id"]] = subprocess.Popen(
                [sys.executable, str(Path(__file__).resolve()), "--kos", str(hedef)], start_new_session=True,
                stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            dolu += gerek
            klasorler.add(veri["cwd"])

    def imza(self):
        return tuple(sorted(str(p.relative_to(sira.SPOOL)) for p in sira.SPOOL.glob("*/*/[!.]*.json")))

    def pc2_kopyala(self):
        hedef = str(sira.PANO)
        while not self.dur:
            self.pc2_olay.wait()
            self.pc2_olay.clear()
            metin = self.pc2_metin
            betik = (f"mkdir -p {shlex.quote(str(sira.PANO.parent))} && cat > {shlex.quote(hedef + '.gecici')} && "
                     f"mv {shlex.quote(hedef + '.gecici')} {shlex.quote(hedef)}")
            try:
                subprocess.run(["ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=5",
                                os.environ.get("UZAK_MAKINE", "pc2"), betik], input=metin.encode(),
                               capture_output=True, timeout=20)
            except subprocess.TimeoutExpired:
                pass

    def calis(self):
        self.kilitle()
        for s in (signal.SIGTERM, signal.SIGINT):
            signal.signal(s, lambda _n, _c: setattr(self, "dur", True))
        pc2_kopya = self.makine == "pc2" and "SIRA_KOK" not in os.environ and os.environ.get("SIRA_PC2_PANO") != "0"
        if pc2_kopya:
            threading.Thread(target=self.pc2_kopyala, daemon=True).start()
        son_imza, son_pano = None, 0.0
        print(f"SIRA: {self.makine} koşucusu başladı ({self.yuva} yuva, spool {sira.dizin(self.makine)}, pano {sira.PANO})",
              flush=True)
        while not self.dur:
            self.yetimleri_topla()
            self.baslat()
            imza = self.imza()
            kosan_var = any(sira.dizin(m, "kosan").glob("[!.]*.json") for m in sira.MAKINELER)
            if imza != son_imza or (kosan_var and time.monotonic() - son_pano > PANO_YENILEME_SN):
                metin = sira.pano_yaz()
                son_imza, son_pano = imza, time.monotonic()
                if pc2_kopya:
                    self.pc2_metin = metin
                    self.pc2_olay.set()
            time.sleep(0.5)
        print(f"SIRA: {self.makine} koşucusu durdu; koşan {len(self.cocuklar)} iş sürüyor", flush=True)


def main():
    ayr = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ayr.add_argument("--makine", choices=sira.MAKINELER)
    ayr.add_argument("--yuva", type=int, default=sira.YUVA)
    ayr.add_argument("--kos", type=Path, help=argparse.SUPPRESS)
    a = ayr.parse_args()
    if a.kos:
        kostur(a.kos)
        return 0
    if not a.makine:
        ayr.error("--makine gerekli")
    Kosucu(a.makine, a.yuva).calis()
    return 0


if __name__ == "__main__":
    sys.exit(main())
