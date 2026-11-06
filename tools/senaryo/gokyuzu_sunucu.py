#!/usr/bin/env python3
"""Gökyüzü parçasının dedicated runServer senaryosu (oyuncusuz), tekrar edilebilir (K0-18).

Kendi dünyasını sıfırdan üretir: `run-server/gokyuzu-senaryo` silinir, `run-server/server.properties` geçici olarak
`level-name=gokyuzu-senaryo` yapılır ve senaryo bitince özgün içeriğine döner. Sunucu Gradle ile pty'siz, stdin'i boruya
bağlı açılır (docs/MAKINE.md, K0-18). Oyun testi afeti kaydedilmez: güneş evresini yalnız `gokyuzu.GunesZinciriSureci`
ilerletir; `/dogalafet gokyuzutest` yalnız geliştirme ortamında vardır.

Koşu 1: kizilcuce bilgi (zincir başlamadı), gokyuzutest evre supernova, kizilcuce bilgi (kalan 1,50 gün), hiz 1000 →
süpernova kendiliğinden söner (GÜNEŞSİZ), hiz 1, gokyuzutest ay yok, kizilcuce bilgi (donma bekleniyor), gokyuzutest
donma 100, kizilcuce bilgi (7 gün), hiz 1000 → kapı açılır, time set 22900 → ilk şafakta kızıl cüce doğar, hiz 1,
durum; gün döngüsü kapalı doğuş (şef kararı G19): donma 99, gamerule doDaylightCycle false, time set 6000,
gokyuzutest evre gunessiz (donma %100 değil: önceki döngünün kapı sayacı sıfırlanır), donma 100, kizilcuce bilgi (şafak beklenmez), hiz 1000 → "gün döngüsü kapalı,
şafak beklenmedi" ve öğlen doğuş, time query daytime (6000, gün saatine dokunulmadı), hiz 1, gamerule geri, sunucuya `stop`.
Koşu 2 (aynı dünya): açılışta dönem okundu (ay yok, kızıl cüce; donma sistemi kızıl cücede çözülme dönemini açtığı için
donma %100'den çözülme tabanına (%40) doğru iner), kizilcuce bilgi (ay yok, kızıl cüce), sifirla, `stop`.
Sonda iki log da denetlenir: `invalid dist` yok, istisna yok, istemci sınıfı (`client.gokyuzu`, `[gokyuzu] kanca`) yok,
iki süreç de 0 ile çıktı.

Kullanım (worktree kökünden): python3 tools/senaryo/gokyuzu_sunucu.py <log klasörü> | --kanit <dizin> [--port <n>]
Çıkış: 0 geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı, 3 ağır süreç kapısı açılmadı
(port ve kapı: cekirdek_sunucu.py, K-E13).
"""
import os
import re
import shutil
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import cekirdek_sunucu as cs  # noqa: E402

KOK = Path(__file__).resolve().parents[2]
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
SUNUCU_KLASORU = KOK / "run-server"
SENARYO_DUNYASI = "gokyuzu-senaryo"
SENARYO_OZELLIKLERI = {"level-name": SENARYO_DUNYASI, "level-seed": "20261105", "online-mode": "false"}
ACILIS_SURESI = 900
ADIM_SURESI = 60
KAPANIS_SURESI = 180


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
            ["./gradlew", "runServer", "--console=plain", "--offline"], cwd=KOK, env=ortam, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", bufsize=1,
            start_new_session=True)
        threading.Thread(target=self._oku, daemon=True).start()

    def yaz(self, metin):
        self.log.write(f"[+{time.monotonic() - self.baslangic:7.1f}s] {metin}\n")
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

    def komut(self, komut, desen, aciklama=None, sure=ADIM_SURESI):
        isaret = self.isaret()
        self.yaz(f">>> {komut}")
        self.surec.stdin.write(komut + "\n")
        self.surec.stdin.flush()
        satir = self.bekle(desen, isaret, sure, aciklama or komut)
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
    istenen = {**SENARYO_OZELLIKLERI, "server-port": str(cs.PORT)}
    yeni = [s for s in eski if s.split("=", 1)[0].strip() not in istenen]
    yeni += [f"{anahtar}={deger}" for anahtar, deger in istenen.items()]
    SUNUCU_KLASORU.mkdir(parents=True, exist_ok=True)
    yol.write_bytes(("\n".join(yeni) + "\n").encode("iso-8859-1"))
    eula = SUNUCU_KLASORU / "eula.txt"
    if not eula.exists():
        eula.write_text("eula=true\n", encoding="utf-8")
    return ozgun


def ozellikleri_geri_al(ozgun):
    yol = SUNUCU_KLASORU / "server.properties"
    if ozgun is None:
        yol.unlink(missing_ok=True)
    else:
        yol.write_bytes(ozgun)


def senaryo_dunyasini_sil():
    hedef = SUNUCU_KLASORU / SENARYO_DUNYASI
    if hedef.parent != SUNUCU_KLASORU or hedef.name != SENARYO_DUNYASI:
        raise RuntimeError(f"beklenmeyen dünya yolu: {hedef}")
    if hedef.exists():
        shutil.rmtree(hedef)


def kosu1(sunucu):
    sunucu.bekle(r"Done \(", 0, ACILIS_SURESI, "açılış")
    sunucu.komut("dogalafet kizilcuce bilgi", r"The sun chain has not started")
    sunucu.komut("dogalafet gokyuzutest evre supernova", r"\[test\] güneş = supernova")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Supernova fades into the sunless era in 1\.50 days")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet hiz 1000", r"timers run at x1000")
    sunucu.bekle(r"Güneş zinciri: supernova -> gunessiz \(süpernova söndü\)", isaret, ADIM_SURESI,
                 "süpernova süresi dolunca kendiliğinden güneşsiz dönem")
    sunucu.komut("dogalafet hiz 1", r"timers run at x1\.00")
    sunucu.komut("dogalafet gokyuzutest ay yok", r"\[test\] ay = false")
    sunucu.komut("dogalafet kizilcuce bilgi", r"waiting for freeze to reach 100%")
    sunucu.komut("dogalafet gokyuzutest donma 100", r"\[test\] donma = 100")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Red dwarf rises 7\.00 days from now")
    sunucu.komut("time set 1000", r"Set the time to 1000")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet hiz 1000", r"timers run at x1000")
    sunucu.bekle(r"Kızıl cüce kapısı açıldı", isaret, ADIM_SURESI, "donma %100 + 7 gün: kapı açıldı")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Red dwarf rises at the next dawn", "bilgi (şafak bekleniyor)")
    sunucu.komut("dogalafet durum", r"sun: Sunless era", "durum (şafaktan önce doğmadı)")
    isaret = sunucu.isaret()
    sunucu.komut("time set 22900", r"Set the time to 22900")
    sunucu.bekle(r"Güneş zinciri: gunessiz -> kizil_cuce \(ilk şafak\)", isaret, ADIM_SURESI, "ilk şafakta doğuş")
    sunucu.komut("dogalafet hiz 1", r"timers run at x1\.00")
    sunucu.komut("dogalafet durum", r"Moon: none; sun: Red dwarf era", "durum (ay yok, kızıl cüce)")
    sunucu.komut("dogalafet gokyuzutest donma 99", r"\[test\] donma = 99")
    sunucu.komut("gamerule doDaylightCycle false", r"doDaylightCycle is now set to: false")
    sunucu.komut("time set 6000", r"Set the time to 6000")
    sunucu.komut("dogalafet gokyuzutest evre gunessiz", r"\[test\] güneş = gunessiz")
    sunucu.komut("dogalafet kizilcuce bilgi", r"waiting for freeze to reach 100%", "bilgi (kapı sayacı sıfır)")
    sunucu.komut("dogalafet gokyuzutest donma 100", r"\[test\] donma = 100")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Red dwarf rises 7\.00 days from now \(daylight cycle is off",
                 "bilgi (gün döngüsü kapalı: şafak beklenmez)")
    isaret = sunucu.isaret()
    sunucu.komut("dogalafet hiz 1000", r"timers run at x1000")
    sunucu.bekle(r"Kızıl cüce: gün döngüsü kapalı, şafak beklenmedi \(gün saati 6000\)", isaret, ADIM_SURESI,
                 "gün döngüsü kapalı log'u")
    sunucu.bekle(r"Güneş zinciri: gunessiz -> kizil_cuce \(gün döngüsü kapalı\)", isaret, ADIM_SURESI,
                 "gün döngüsü kapalıyken o anki saatte doğuş")
    sunucu.komut("time query daytime", r"The time is 6000", "gün saatine dokunulmadı")
    sunucu.komut("dogalafet hiz 1", r"timers run at x1\.00")
    sunucu.komut("gamerule doDaylightCycle true", r"doDaylightCycle is now set to: true")
    sunucu.komut("dogalafet durum", r"Moon: none; sun: Red dwarf era", "durum (gün döngüsü kapalı doğuş sonrası)")


def kosu2(sunucu):
    sunucu.bekle(r"Done \(", 0, ACILIS_SURESI, "açılış")
    sunucu.bekle(r"Dünya dönemi okundu \(dogalafetler_donem\): .*ay=false gunes=kizil_cuce .*donma=(?:100|[4-9]\d)\.\d\d "
                 r"cozulme=true", 0, 30, "dönem kaydı okundu (ay yok, kızıl cüce, çözülme döneminde donma 40–100)")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Sun: Red dwarf era", "bilgi (kızıl cüce korundu)")
    sunucu.komut("dogalafet kizilcuce bilgi", r"Moon: gone", "bilgi (ay yok korundu)")
    sunucu.komut("dogalafet sifirla", r"disaster and era state was reset")
    sunucu.komut("dogalafet kizilcuce bilgi", r"The sun chain has not started", "bilgi (sıfırlandı)")


def log_denetimi(yol):
    metin = yol.read_text(encoding="utf-8")
    hatalar = []
    if "invalid dist" in metin.lower():
        hatalar.append("invalid dist")
    istisnalar = [s for s in metin.splitlines() if re.search(r"Exception|\	at [a-z]", s)]
    if istisnalar:
        hatalar.append(f"{len(istisnalar)} istisna satırı, ilki: {istisnalar[0]}")
    istemci = [s for s in metin.splitlines() if re.search(r"client\.gokyuzu|\[gokyuzu\] kanca|\[gokyuzu-oto\]", s)]
    if istemci:
        hatalar.append(f"{len(istemci)} istemci sınıfı satırı, ilki: {istemci[0]}")
    if "Stopping the server" not in metin:
        hatalar.append("'Stopping the server' yok")
    return hatalar


def main():
    klasor = cs.argumanlar(__doc__).kanit
    klasor.mkdir(parents=True, exist_ok=True)
    signal.signal(signal.SIGTERM, lambda _i, _c: (_ for _ in ()).throw(SystemExit(143)))
    cs.kapi_veya_cik(cs.PORT)
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
