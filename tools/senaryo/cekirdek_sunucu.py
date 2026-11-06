#!/usr/bin/env python3
"""Çekirdeğin runServer senaryosu (oyuncusuz), tekrar edilebilir.

Kendi dünyasını sıfırdan üretir: `run-server/cekirdek-senaryo` silinir, `run-server/server.properties` geçici olarak
`level-name=cekirdek-senaryo` yapılır ve senaryo bitince özgün içeriğine döner (`test` dünyasına dokunulmaz). Sunucu
Gradle ile pty'siz, stdin'i boruya bağlı açılır (docs/MAKINE.md, K0-18); `-PoyunTestiAfeti=deprem,gunes_patlamasi` ile bu
türlere yaşam döngüsü sınama afeti kaydedilir (core.OyunTestiAfeti; güneş türü AKTIF'te güneşi BUYUYEN, BITIS'te SUPERNOVA
yapar) ve güneş türüyle birlikte dönem süreci taklidi `oyuntesti_gunes` (core.OyunTestiSureci: SUPERNOVA/GUNESSIZ'de
aktif) kaydedilir; kasırga kayıtsız kalır.

Koşu 1: yeni config varsayılanlarla yazılır; durum, gun 90, durum (seviye ağırlıkları), hiz 10, durum, start kasirga 3
("henüz kayıtlı değil"), asama ileri (ilerletilecek yok), sifirla, durum, gun 42, time add 72000 + durum (gün 42), time set
day + durum (gün 42), start gunes_patlamasi, asama ileri x2, durum (güneş büyüyor), stop, durum (güneş normal), stop
(durdurulacak yok); K-C41: start gunes_patlamasi, asama ileri x3 (doğal biter, SUPERNOVA), durum (süreç sürüyor),
gamerule gunesPatlamasiAfeti false + durum (durduruldu (kural)), gamerule true + durum (sürüyor), start deprem 1 + stop
(Y6: süren süreç hatırlatılır), stop (süreç durur), gamerule true + durum (durduruldu (stop), sürmez), asama ileri
(güneşsiz) + durum, asama ileri (kızıl cüce, kayıt silinir) + durum; start deprem 2, ikinci start (reddedilir), asama
ileri x2, durum, stop, start deprem 3, asama ileri x2 (aktif), durum, sunucuya `stop`.
Koşu 2 (aynı dünya): açılışta dönem okundu (gün ofseti, son afetler, bekleme) ve aktif deprem geri yüklendi; durum
(gün 42, aktif deprem seviye 3; uyku 0, durdurma kaydı 0), stop, sifirla, `stop`.
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, bizim config dosyalarımız için Forge düzeltme WARN'ı yok,
iki süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/cekirdek_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı.

Ortak komut satırı ve kapı (K-E13, diğer `*_sunucu.py` betikleri de kullanır): `--port` (varsayılan 25565) senaryo süresince
`server.properties`'e `server-port` olarak yazılır; başka worktree'deki paralel senaryo ayrı portla çakışmaz (aynı worktree'de
`run-server/` paylaşıldığı için senaryolar yine sırayla koşar). Ağır süreç kapısı: Gradle < 2, boş RAM >= 4 GB ve portı
başka süreç dinlemiyor; 60 sn'de bir, en çok 20 dk bekler.
"""
import argparse
import os
import re
import shutil
import signal
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
SUNUCU_KLASORU = KOK / "run-server"
SENARYO_DUNYASI = "cekirdek-senaryo"
SENARYO_OZELLIKLERI = {"level-name": SENARYO_DUNYASI, "level-seed": "20261105", "online-mode": "false"}
ACILIS_SURESI = 900
ADIM_SURESI = 60
KAPANIS_SURESI = 180
VARSAYILAN_PORT = 25565
PORT = VARSAYILAN_PORT


class SenaryoHatasi(Exception):
    pass


class Sunucu:
    """Bir runServer süreci: çıktıyı log'a ve belleğe yazar, komut gönderir, desen bekler."""

    def __init__(self, ad, log_yolu):
        self.ad = ad
        self.satirlar = []
        self.kilit = threading.Condition()
        self.baslangic = time.monotonic()
        self.log = log_yolu.open("w", encoding="utf-8")
        ortam = dict(os.environ, JAVA_HOME=JAVA_HOME)
        self.surec = subprocess.Popen(
            ["./gradlew", "runServer", "--console=plain", "--offline", "-PoyunTestiAfeti=deprem,gunes_patlamasi"], cwd=KOK, env=ortam,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8",
            errors="replace", bufsize=1, start_new_session=True)
        threading.Thread(target=self._oku, daemon=True).start()

    def _gecen(self):
        return time.monotonic() - self.baslangic

    def yaz(self, metin):
        self.log.write(f"[+{self._gecen():7.1f}s] {metin}\n")
        self.log.flush()

    def _oku(self):
        for satir in self.surec.stdout:
            satir = satir.rstrip("\n")
            self.yaz(satir)
            with self.kilit:
                self.satirlar.append(satir)
                self.kilit.notify_all()
        with self.kilit:
            self.kilit.notify_all()

    def isaret(self):
        with self.kilit:
            return len(self.satirlar)

    def bekle(self, desen, baslangic, sure, aciklama):
        """`baslangic` satırından sonra `desen`e uyan ilk satırı bekler."""
        ifade = re.compile(desen)
        son = time.monotonic() + sure
        with self.kilit:
            while True:
                for satir in self.satirlar[baslangic:]:
                    if ifade.search(satir):
                        return satir
                kalan = son - time.monotonic()
                if kalan <= 0 or self.surec.poll() is not None:
                    son_satirlar = "\n".join(self.satirlar[-20:])
                    raise SenaryoHatasi(f"{self.ad}: '{aciklama}' gelmedi (desen {desen!r}). Son satırlar:\n{son_satirlar}")
                self.kilit.wait(timeout=min(kalan, 1.0))

    def komut(self, komut, desen, aciklama=None):
        isaret = self.isaret()
        self.yaz(f">>> {komut}")
        self.surec.stdin.write(komut + "\n")
        self.surec.stdin.flush()
        satir = self.bekle(desen, isaret, ADIM_SURESI, aciklama or komut)
        self.yaz(f"### tamam: {komut} -> {satir.strip()}")
        return satir

    def durdur(self):
        """Sunucuya `stop` yazar ve çıkışı bekler; gerekirse süreç grubunu kapatır."""
        if self.surec.poll() is None:
            try:
                self.yaz(">>> stop")
                self.surec.stdin.write("stop\n")
                self.surec.stdin.flush()
            except (BrokenPipeError, ValueError):
                pass
            try:
                self.surec.wait(timeout=KAPANIS_SURESI)
            except subprocess.TimeoutExpired:
                self.yaz("!!! stop sonrası kapanmadı; süreç grubu SIGTERM ile kapatılıyor")
                os.killpg(self.surec.pid, signal.SIGTERM)
                self.surec.wait(timeout=60)
        self.yaz(f"### süreç çıkış kodu {self.surec.returncode}")
        self.log.close()
        return self.surec.returncode


def ozellikleri_senaryoya_cevir():
    yol = SUNUCU_KLASORU / "server.properties"
    ozgun = yol.read_bytes() if yol.exists() else None
    eski = [] if ozgun is None else ozgun.decode("iso-8859-1").splitlines()
    istenen = {**SENARYO_OZELLIKLERI, "server-port": str(PORT)}
    yeni = [s for s in eski if s.split("=", 1)[0].strip() not in istenen]
    yeni += [f"{anahtar}={deger}" for anahtar, deger in istenen.items()]
    SUNUCU_KLASORU.mkdir(parents=True, exist_ok=True)
    yol.write_bytes(("\n".join(yeni) + "\n").encode("iso-8859-1"))
    return ozgun


def ozellikleri_geri_al(ozgun):
    yol = SUNUCU_KLASORU / "server.properties"
    if ozgun is None:
        yol.unlink(missing_ok=True)
    else:
        yol.write_bytes(ozgun)


def argumanlar(aciklama=None, varsayilan_port=VARSAYILAN_PORT, varsayilan_kanit=None, ek=None):
    """Ortak komut satırı: konumsal `<log klasörü>` ya da `--kanit <dizin>`, `--port <n>`; `ek(parser)` betiğe özgü
    seçenek ekler. Port bu modülün `PORT`'una yazılır (`ozellikleri_senaryoya_cevir` onu `server-port` yapar)."""
    global PORT
    ayristirici = argparse.ArgumentParser(description=aciklama, formatter_class=argparse.RawDescriptionHelpFormatter)
    ayristirici.add_argument("klasor", nargs="?", type=Path, help="log/kanıt klasörü (--kanit ile aynı)")
    ayristirici.add_argument("--kanit", type=Path, help="log/kanıt klasörü")
    ayristirici.add_argument("--port", type=int, default=varsayilan_port, help=f"sunucu portu (varsayılan {varsayilan_port})")
    if ek:
        ek(ayristirici)
    a = ayristirici.parse_args()
    if a.klasor and a.kanit and a.klasor.resolve() != a.kanit.resolve():
        ayristirici.error("konumsal klasör ile --kanit farklı")
    a.kanit = a.kanit or a.klasor or varsayilan_kanit
    if a.kanit is None:
        ayristirici.error("log klasörü gerekli: <log klasörü> ya da --kanit <dizin>")
    if not 1024 <= a.port <= 65535:
        ayristirici.error(f"geçersiz port {a.port}")
    PORT = a.port
    return a


def port_bos(port):
    """Portta dinleyen süreç yoksa True. SO_REUSEADDR ile TIME_WAIT kalanı boş sayılır, dinleyen soket (IPv6 çift yığın
    dahil) sayılmaz."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            s.bind(("", port))
        except OSError:
            return False
    return True


def agir_surec_kapisi(port, log=print, en_cok_dk=20, ram_mb=4096):
    """Gradle < 2, boş RAM >= `ram_mb` ve `port` boş olana dek 60 sn'de bir bekler; `en_cok_dk` dakikada açılmazsa False."""
    for deneme in range(en_cok_dk + 1):
        gradle = int(subprocess.run(["pgrep", "-fc", "GradleWrapperMain"], capture_output=True, text=True).stdout.strip()
                     or 0)
        bos = int(Path("/proc/meminfo").read_text().split("MemAvailable:")[1].split()[0]) // 1024
        port_uygun = port_bos(port)
        log(f"kapı: gradle={gradle} bos={bos} MB port {port} {'boş' if port_uygun else 'DOLU'}")
        if gradle < 2 and bos >= ram_mb and port_uygun:
            return True
        if deneme < en_cok_dk:
            time.sleep(60)
    return False


def kapi_veya_cik(port):
    if not agir_surec_kapisi(port):
        print("ağır süreç kapısı 20 dk'da açılmadı (Gradle/RAM/port); koşu iptal", file=sys.stderr)
        sys.exit(3)


def senaryo_dunyasini_sil():
    hedef = SUNUCU_KLASORU / SENARYO_DUNYASI
    if hedef.parent != SUNUCU_KLASORU or hedef.name != SENARYO_DUNYASI:
        raise RuntimeError(f"beklenmeyen dünya yolu: {hedef}")
    if hedef.exists():
        shutil.rmtree(hedef)


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Config dosyası varsayılanlarla oluşturuldu: .*dogalafetler-server\.toml", 0, 5,
                 "yeni dünyada config varsayılanlarla yazıldı")
    sunucu.komut("dogalafet durum", r"Level weights \(day 0, max level 5\): S1 100\.0%", "durum (gün 0: yalnız S1)")
    sunucu.komut("dogalafet gun 90", r"Game day set to 90\. Level weights: S1 ")
    sunucu.komut("dogalafet durum", r"Level weights \(day 90, max level 5\): S1 [0-9.]+%, S2 [0-9.]+%, S3 [0-9.]+%, "
                 r"S4 [0-9.]+%, S5 [1-9][0-9.]*%", "durum (gün 90: S5 görünür)")
    sunucu.komut("dogalafet hiz 10", r"timers run at x10\.00")
    sunucu.komut("dogalafet durum", r"speed: x10\.00", "durum (hız 10)")
    sunucu.komut("dogalafet start kasirga 3", r"Tornado is not registered yet\.")
    sunucu.komut("dogalafet asama ileri", r"no disaster or era phase to advance")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")
    sunucu.komut("dogalafet durum", r"Game day: 0 \(offset 0\), speed: x1\.00", "durum (sıfırlandı)")
    sunucu.komut("dogalafet gun 42", r"Game day set to 42\.")
    sunucu.komut("time add 72000", r"Set the time to")
    sunucu.komut("dogalafet durum", r"Game day: 42 \(offset", "durum (time add sonrası gün 42)")
    sunucu.komut("time set day", r"Set the time to 1000")
    sunucu.komut("dogalafet durum", r"Game day: 42 \(offset", "durum (time set day sonrası gün 42)")
    sunucu.komut("dogalafet start gunes_patlamasi", r"Solar Flare started \(Unleveled\)")
    sunucu.komut("dogalafet asama ileri", r"Solar Flare phase: Siren")
    sunucu.komut("dogalafet asama ileri", r"Solar Flare phase: Active")
    sunucu.komut("dogalafet durum", r"sun: Swelling sun", "durum (güneş büyüyor)")
    sunucu.komut("dogalafet stop", r"Solar Flare stopped\.")
    sunucu.komut("dogalafet durum", r"sun: Normal sun", "durum (tamamlanmamış süreç durdu, güneş normal)")
    sunucu.komut("dogalafet stop", r"There is no disaster or era process to stop\.")
    sunucu.komut("dogalafet start gunes_patlamasi", r"Solar Flare started \(Unleveled\)")
    sunucu.komut("dogalafet asama ileri", r"Solar Flare phase: Siren")
    sunucu.komut("dogalafet asama ileri", r"Solar Flare phase: Active")
    sunucu.komut("dogalafet asama ileri", r"Solar Flare (phase: Ending|has ended)")
    sunucu.komut("dogalafet durum", r"Running era processes: oyuntesti_gunes", "durum (dönem süreci sürüyor)")
    isaret = sunucu.isaret()
    sunucu.komut("gamerule gunesPatlamasiAfeti false", r"gunesPatlamasiAfeti is now set to: false")
    sunucu.bekle(r"Dönem süreci durduruldu \(kural\): oyuntesti_gunes", isaret, ADIM_SURESI, "kural kapanınca süreç durdu")
    sunucu.komut("dogalafet durum", r"Stopped era processes: oyuntesti_gunes stopped \(rule\)", "durum (kural ile durdu)")
    isaret = sunucu.isaret()
    sunucu.komut("gamerule gunesPatlamasiAfeti true", r"gunesPatlamasiAfeti is now set to: true")
    sunucu.bekle(r"Dönem süreci kural açılınca sürüyor: oyuntesti_gunes", isaret, ADIM_SURESI, "kural açılınca süreç sürdü")
    sunucu.komut("dogalafet durum", r"Running era processes: oyuntesti_gunes", "durum (kural açılınca sürüyor)")
    sunucu.komut("dogalafet start deprem 1", r"Earthquake started \(Level 1 ")
    sunucu.komut("dogalafet stop", r"Era processes still running: oyuntesti_gunes; run /dogalafet stop again",
                 "stop afeti durdurur, süren süreci hatırlatır (Y6)")
    sunucu.komut("dogalafet stop", r"Era processes stopped: oyuntesti_gunes")
    sunucu.komut("gamerule gunesPatlamasiAfeti true", r"gunesPatlamasiAfeti is now set to: true")
    time.sleep(2)  # kural denetimi saniyede bir: stop ile duran süreç bu sürede sürmemeli
    sunucu.komut("dogalafet durum", r"Stopped era processes: oyuntesti_gunes stopped \(stop\)",
                 "durum (stop ile duran kural açıkken sürmez)")
    sunucu.komut("dogalafet asama ileri", r"oyuntesti_gunes -> Sunless era")
    sunucu.komut("dogalafet durum", r"sun: Sunless era", "durum (duran süreç asama ileri ile ilerledi)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet asama ileri", r"oyuntesti_gunes -> Red dwarf era")
    sunucu.bekle(r"Dönem süreci tamamlandı; durdurma kaydı silindi: oyuntesti_gunes", isaret, ADIM_SURESI,
                 "tamamlanan sürecin kaydı silindi")
    sunucu.komut("dogalafet durum", r"Stopped era processes: none", "durum (duran süreç yok)")
    sunucu.komut("dogalafet start deprem 2", r"Earthquake started \(Level 2 \(Moderate\)\)")
    sunucu.komut("dogalafet start deprem 1", r"A disaster is already active: Earthquake", "ikinci afet reddedilir")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Siren")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Active")
    sunucu.komut("dogalafet durum", r"Active disaster: Earthquake, Level 2 \(Moderate\); phase: Active")
    sunucu.komut("dogalafet stop", r"Earthquake stopped\.")
    sunucu.komut("dogalafet start deprem 3", r"Earthquake started \(Level 3 \(Severe\)\)")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Siren")
    sunucu.komut("dogalafet asama ileri", r"Earthquake phase: Active")
    sunucu.komut("dogalafet durum", r"Game day: 42 \(offset", "durum (gün 42)")


def kosu2(sunucu):
    sunucu.bekle(r"Done \(", 0, ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Aktif afet geri yüklendi: deprem seviye 3 aşama aktif", 0, 30, "aktif afet geri yüklendi")
    sunucu.bekle(r"Dünya dönemi okundu \(dogalafetler_donem\): .*gunOfseti=42 uyku=0 .*gecmis=4 genelBekleme=[1-9][0-9]* "
                 r"sonSeviye=2 aktifAfet=deprem/aktif durdurulanSurec=0", 0, 30,
                 "dönem kaydı okundu (gün ofseti 42, geçmiş, bekleme, geri yüklenmiş deprem)")
    sunucu.komut("dogalafet durum", r"Game day: 42 \(offset 42\)", "durum (gün 42 korundu)")
    sunucu.komut("dogalafet durum", r"Active disaster: Earthquake, Level 3 \(Severe\); phase: Active",
                 "durum (deprem korundu)")
    sunucu.komut("dogalafet durum", r"Recent disasters: Earthquake Level 2 \(Moderate\)", "durum (geçmiş korundu)")
    sunucu.komut("dogalafet stop", r"Earthquake stopped\.")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")


def log_denetimi(yol):
    metin = yol.read_text(encoding="utf-8")
    hatalar = []
    if "invalid dist" in metin.lower():
        hatalar.append("invalid dist")
    istisnalar = [s for s in metin.splitlines() if re.search(r"Exception|	at [a-z]", s)]
    if istisnalar:
        hatalar.append(f"{len(istisnalar)} istisna satırı, ilki: {istisnalar[0]}")
    config_uyarilari = [s for s in metin.splitlines()
                        if re.search(r"dogalafetler-(server|client)\.toml is not correct|Incorrect key (cekirdek|sicaklik|gorsel)\b", s)]
    if config_uyarilari:
        hatalar.append(f"{len(config_uyarilari)} config düzeltme satırı, ilki: {config_uyarilari[0]}")
    if "Stopping the server" not in metin:
        hatalar.append("'Stopping the server' yok")
    return hatalar


def main():
    klasor = argumanlar(__doc__).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    kapi_veya_cik(PORT)
    senaryo_dunyasini_sil()
    ozgun = ozellikleri_senaryoya_cevir()
    sonuc = 0
    kodlar = []
    try:
        for ad, adimlar in (("kosu1", kosu1), ("kosu2", kosu2)):
            sunucu = Sunucu(ad, klasor / f"{ad}.log")
            try:
                adimlar(sunucu)
            except SenaryoHatasi as hata:
                sunucu.yaz(f"!!! {hata}")
                print(hata, file=sys.stderr)
                sonuc = 1
            finally:
                kodlar.append(sunucu.durdur())
            if sonuc:
                break
    finally:
        ozellikleri_geri_al(ozgun)
    for ad in ("kosu1", "kosu2"):
        yol = klasor / f"{ad}.log"
        if yol.exists():
            for hata in log_denetimi(yol):
                print(f"{ad}: {hata}", file=sys.stderr)
                sonuc = 1
    print(f"senaryo sonucu {sonuc}, süreç kodları {kodlar}")
    if sonuc == 0 and any(kod != 0 for kod in kodlar):
        sonuc = 2
    sys.exit(sonuc)


if __name__ == "__main__":
    main()
