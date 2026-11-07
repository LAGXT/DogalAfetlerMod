#!/usr/bin/env python3
"""İş kuyruğu koşucusu (K-E51, K-E53, K-E60): bir makinenin spool'unu ve ortak `oto` spool'unu (`sira.py`) izler, en çok
--yuva iş koşar (kuyruk dışı ağır süreçler de yuva tutar), biten işi biten/'e yazar ve KÖK/.vibe/pano.md'yi günceller
(değişiklikte ve 30 sn'de bir; pc2 koşucusu panoyu pc2'deki aynı yola da kopyalar: pc2'de `watch cat`). pc2 koşucusu ayrıca
PC2_SUREC_ARALIK_SN'de bir pc2'deki Gradle istemcilerini ssh ile okur (`sira.pc2_surecleri_coz`, kuyruk dışı sayimı).

Kullanım: sira_kosucu.py --makine ana|pc2 [--yuva 2]
Ayakta kalma: systemd --user `dogalafet-sira@<makine>` (tools/senaryo/dogalafet-sira@.service, KillMode=process: koşucu
yeniden başlarken koşan işler sürer; başlayınca kosan/'daki kayıtlar koşturucu süreçleriyle bağdaştırılır). İki koşucu da
ana makinede koşar: pc2'de depo (.git, .vibe) ve spool yok, işi komutun kendisi K-E44 ile pc2'ye taşır (uzak.sh eşitler,
yuva/klasör kilidi alır, log ve kanıtı geri çeker).
Sıra (K-E53): adaylar önce bu makinenin bekleyenleri, sonra oto'nunkiler (ikisi de eklenme sırasıyla); koşucu sırayı gezer
ve sığan işi alır (backfill: sığmayan iki yuvalı işin arkasındaki tek yuvalı iş öne geçer). Açlık koruması: sığmayan
iki yuvalı iş SIRA_ACLIK_SN (600) sn'den uzun bekliyorsa arkasındaki hiçbir iş alınmaz, yuvalar boşalınca o koşar. pc2'de
aynı klasörden koşan işi olan makine o klasörün ikinci işini almaz (pc2'de uzak.sh klasör kilidi de); pc2 kuyruğu kuyruk
dışından tutulan uzak.sh yuvalarını da sayar. oto işinin dağıtımı (pc2 öncelikli, ana taşanı alır) `sira.py`
başındaki açıklamada. Ana makinenin işi kullanıcı oturumunun ekranını alır (yaşayan Hyprland imzası; kayıttaki imza eski
olabilir), yaşayan Hyprland yoksa ana koşucu istemci işi almaz. Her iş kendi
oturumunda `--kos` kipindeki koşturucuyla koşar; koşturucu çıkışı kendisi yazar, koşucu ölse de kayıt tamamlanır. pc2'nin
`--istemci` işi süresince koşturucu ana makinedeki uzak.sh yuva kilitlerinin hepsini tutar ve işe UZAK_YUVA_TUTULU=1 verir.
Geçiş: pc2 koşucusu eski sira.py'nin (`surum` alanı yok) pc2'ye koyduğu tek yuvalı ekransız işi oto'ya taşır (kimlik ve log
aynı); eski `sira.py bekle` oto'yu aramadığı için pc2/bekleyen'de oto'daki kayda bağ kalır, iş başlayınca bağ silinir.
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
import runclient_kilit  # noqa: E402
import sira  # noqa: E402

PANO_YENILEME_SN = 30
HYPRLAND_BAKIM_SN = 30


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


def is_ortami(veri):
    """İşin süreç ortamı: koşucununki + kayıt + kuyruk değişkenleri; JAVA_HOME yoksa JDK 17 (K-E54: kuyruktan giren
    `./gradlew` Java 21 daemon'u doğurmaz), varsa ezilmez. Ana makinenin işine kullanıcı oturumunun ekranı (K-E60: işçinin
    kabuğundaki Hyprland imzası eski bir örneğe ait olabilir)."""
    env = {**os.environ, **veri["env"], "SIRA_ICINDE": "1", "SIRA_IS": veri["id"], "SIRA_MAKINE": veri["makine"]}
    env.setdefault("JAVA_HOME", sira.JAVA_HOME)
    if veri["makine"] == "ana":
        env.update(runclient_kilit.oturum_ekrani())
    return env


def kostur(yol):
    """`--kos` kipi: tek iş. Oturum lideri (koşucu `start_new_session` ile açar); `sira.py iptal` oturuma TERM yollar."""
    veri = sira.json_oku(yol)
    veri.update(kosturucu=os.getpid(), durum="kosuyor", baslangic=time.time())
    sira.json_yaz(yol, veri)
    Path(veri["log"]).parent.mkdir(parents=True, exist_ok=True)
    log = open(veri["log"], "ab", buffering=0)
    env = is_ortami(veri)
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
        self.tasinmaz = set()
        self.hyprland = (0.0, None)

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

    def eski_pc2_islerini_tasi(self):
        """K-E53 geçişi (yalnız pc2 koşucusu; pc2/bekleyen'den iş alan tek süreç o): eski sira.py'nin pc2'ye koyduğu tek
        yuvalı ekransız iş oto'ya, yerine oto'daki kayda bağ. Sarkan bağlar (iş başladı) silinir."""
        bekleyen = sira.dizin("pc2", "bekleyen")
        for yol in bekleyen.glob("[!.]*.json"):
            if yol.is_symlink():
                if not yol.exists():
                    yol.unlink(missing_ok=True)
                continue
            if yol.name in self.tasinmaz:
                continue
            veri = sira.json_oku(yol)
            if not veri:
                continue
            if ("surum" in veri or veri.get("istemci") or veri.get("yuva", 1) != 1 or veri["env"].get("UZAK") == "0"
                    or sira.kendiliginden(veri["komut"])[0] not in ("gradle", "sunucu")
                    or not sira.ekransiz_mi(veri["komut"])):
                self.tasinmaz.add(yol.name)
                continue
            veri.update(makine="oto", tasindi=time.time())
            hedef = sira.dizin("oto", "bekleyen") / yol.name
            sira.json_yaz(hedef, veri)
            bag = yol.with_name(f".{yol.name}.bag")
            bag.unlink(missing_ok=True)
            os.symlink(hedef, bag)
            os.replace(bag, yol)
            print(f"SIRA: {veri['id']} pc2 → oto taşındı (eski kayıt)", flush=True)

    def pc2_alamaz(self, veri):
        """pc2 bu oto işini şimdi alamaz: koşucusu yok, aynı klasörden işi koşuyor ya da yükü (koşan + uzak.sh yuvaları +
        kuyruk dışı + pc2'de bekleyen) işe yer bırakmıyor."""
        if not sira.kosucu_canli("pc2"):
            return True
        kosan = [v for _, v in sira.isler("pc2", "kosan")]
        if veri["cwd"] in {v["cwd"] for v in kosan} or sira.uzak_klasor_dolu(veri["cwd"]):
            return True
        yuk = sira.doluluk("pc2") + sum(sira.gerek(v, "pc2") for _, v in sira.isler("pc2", "bekleyen"))
        return yuk + sira.gerek(veri, "pc2") > sira.YUVA

    def hyprland_var(self):
        """Yaşayan Hyprland örneği var mı (HYPRLAND_BAKIM_SN önbellekli)."""
        if time.monotonic() - self.hyprland[0] > HYPRLAND_BAKIM_SN:
            self.hyprland = (time.monotonic(), runclient_kilit.canli_hyprland())
        return self.hyprland[1] is not None

    def ana_engeli(self, veri, kosan, simdi):
        """Ana makine bu işi şimdi neden alamaz (None: alır). İstemci işi: yaşayan Hyprland ve tek istemci. oto işi: pc2
        öncelikli, ağır süreç ≤ 2 (kuyruk dışı Gradle da), boş RAM >= KAPI_RAM_MB."""
        if veri.get("istemci"):
            if not self.hyprland_var():
                return "yaşayan Hyprland yok"
            if any(v.get("istemci") for v in kosan):
                return "kuyruğun istemci işi koşuyor"
            kayit = runclient_kilit.oku()
            if kayit and runclient_kilit.yasiyor(kayit):
                return "runClient kilidi tutuluyor"
        if veri.get("makine") != "oto":
            return None
        if simdi - veri.get("eklendi", simdi) < sira.OTO_BEKLE_SN and not self.pc2_alamaz(veri):
            return "pc2 öncelikli"
        agir = sira.doluluk("ana")
        if agir + sira.gerek(veri, "ana") > sira.YUVA:
            return f"ana makinede {agir} ağır süreç"
        if sira.bos_ram_mb() < int(os.environ.get("KAPI_RAM_MB", "4096")):
            return "boş RAM az"
        return None

    def klasor_engeli(self, veri):
        """Öteki makinede aynı klasörün işi varken: pc2'deki doğrudan `./gradlew` işi build/ alt klasörlerini geri
        çektiğinden ana makinedeki işle yan yana koşmaz; öteki işler pc2'de kendi kopyasında koşar, yalnız log/kanıt
        geri gelir."""
        if self.makine == "ana":
            return any(v["cwd"] == veri["cwd"] and sira.build_geri_ceker(v) for _, v in sira.isler("pc2", "kosan"))
        return sira.build_geri_ceker(veri) and any(v["cwd"] == veri["cwd"] for _, v in sira.isler("ana", "kosan"))

    def adaylar(self):
        return [*sira.isler(self.makine, "bekleyen"), *sira.isler("oto", "bekleyen")]

    def baslat(self):
        simdi = time.time()
        kosan = [v for _, v in sira.isler(self.makine, "kosan")]
        dolu = sira.doluluk(self.makine)
        klasorler = {v["cwd"] for v in kosan}
        for yol, veri in self.adaylar():
            if veri.get("tasindi") and (sira.dizin("pc2", "biten") / yol.name).exists():
                yol.unlink(missing_ok=True)
                continue
            gerek = sira.gerek(veri, self.makine)
            if veri["cwd"] in klasorler or self.makine == "pc2" and sira.uzak_klasor_dolu(veri["cwd"]):
                continue
            if self.klasor_engeli(veri):
                continue
            if dolu + gerek > self.yuva:
                if gerek > 1 and simdi - veri.get("eklendi", simdi) > sira.ACLIK_SN:
                    break
                continue
            if self.makine == "ana" and self.ana_engeli(veri, kosan, simdi):
                continue
            hedef = sira.dizin(self.makine, "kosan") / yol.name
            try:
                os.rename(yol, hedef)
            except FileNotFoundError:
                continue
            sira.makineye_hazirla(veri, self.makine)
            veri.update(durum="basliyor", baslangic=time.time())
            sira.json_yaz(hedef, veri)
            self.cocuklar[veri["id"]] = subprocess.Popen(
                [sys.executable, str(Path(__file__).resolve()), "--kos", str(hedef)], start_new_session=True,
                stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print(f"SIRA: {veri['id']} başladı ({self.makine}, {gerek} yuva, kaynak {veri.get('kaynak', self.makine)})",
                  flush=True)
            dolu += gerek
            klasorler.add(veri["cwd"])
            kosan.append(veri)

    def imza(self):
        spool = tuple(sorted(str(p.relative_to(sira.SPOOL)) for p in sira.SPOOL.glob("*/*/[!.]*.json")))
        return spool, tuple(d["pid"] for m in sira.MAKINELER for d in sira.dis_surecler(m))

    def pc2_surec_topla(self):
        """pc2'deki Gradle istemcilerini ve uzak.sh oturum dosyalarını ssh ile okuyup `pc2/surecler.json`'a yazar."""
        betik = ('ps -eo pid=,ppid=,etimes=,args=; echo @@; '
                 'for f in /tmp/uzak-*.pid; do [ -f "$f" ] && echo "$f $(cat "$f")"; done; true')
        while not self.dur:
            try:
                sonuc = subprocess.run(["ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=5",
                                        os.environ.get("UZAK_MAKINE", "pc2"), betik], capture_output=True, text=True,
                                       timeout=20)
                if sonuc.returncode != 0:
                    raise OSError(sonuc.stderr.strip()[:200] or f"ssh çıkış {sonuc.returncode}")
                ps_metni, _, dosyalar = sonuc.stdout.partition("@@")
                veri = {"zaman": time.time(), "surecler": sira.pc2_surecleri_coz(ps_metni, dosyalar)}
            except (OSError, subprocess.TimeoutExpired) as hata:
                veri = {"zaman": time.time(), "hata": str(hata)}
            sira.json_yaz(sira.dizin("pc2") / sira.PC2_SURECLER, veri)
            time.sleep(sira.PC2_SUREC_ARALIK_SN)

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
        if self.makine == "pc2" and "SIRA_KOK" not in os.environ:
            threading.Thread(target=self.pc2_surec_topla, daemon=True).start()
        son_imza, son_pano = None, 0.0
        print(f"SIRA: {self.makine} koşucusu başladı ({self.yuva} yuva, spool {sira.dizin(self.makine)}, pano {sira.PANO})",
              flush=True)
        while not self.dur:
            self.yetimleri_topla()
            if self.makine == "pc2":
                self.eski_pc2_islerini_tasi()
            self.baslat()
            imza = self.imza()
            if imza != son_imza or time.monotonic() - son_pano > PANO_YENILEME_SN:
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
