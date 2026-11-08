#!/usr/bin/env python3
"""sira.py / sira_kosucu.py sınaması (K-E53, K-E60): python3 tools/senaryo/test_sira.py

Geçici spool (SIRA_KOK), geçici uzak.sh kilit klasörü (UZAK_MAKINE) ve geçici runClient kilidi; gerçek kuyruğa ve pc2'ye
dokunmaz. Koşucunun `baslat` döngüsü bu süreçte sürülür, işler gerçek koşturucuyla (`--kos`) `sleep` olarak koşar.
Öz-kuyruklama sınamaları betikleri iş dışından çağırır, iş kuyruğa düşünce bekleyeni keser (iş iptal olur, Gradle açılmaz).
"""
import fcntl
import os
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

GECICI = tempfile.mkdtemp(prefix="sira-sinama-")
os.environ.update(SIRA_KOK=f"{GECICI}/spool", SIRA_PANO=f"{GECICI}/pano.md", SIRA_ACLIK_SN="2",
                  UZAK_MAKINE=f"sinama-{secrets.token_hex(3)}", DOGALAFETLER_RUNCLIENT_KILIT=f"{GECICI}/runclient.lock",
                  KAPI_RAM_MB="0")
sys.path.insert(0, str(Path(__file__).resolve().parent))
import sira  # noqa: E402
import sira_kosucu  # noqa: E402

GERCEK_GRADLE = sira.ana_gradle_surecleri
sira.ana_gradle_surecleri = list
IS_DISI = {k: v for k, v in os.environ.items() if k not in ("SIRA_IS", "UZAK_ICINDE", "SIRA_SERBEST")}


def is_yaz(spool, komut, yuva=1, istemci=False, cwd=None, once_sn=0.0, **ek):
    is_yaz.sayac += 1
    kimlik = f"{sira.yeni_kimlik()}-{is_yaz.sayac}"
    veri = {"id": kimlik, "makine": spool, "komut": komut, "cwd": str(cwd or tempfile.mkdtemp(dir=GECICI)), "env": {}, "yuva": yuva,
            "istemci": istemci, "eklendi": time.time() - once_sn + is_yaz.sayac * 1e-3, "durum": "bekliyor",
            "log": f"{GECICI}/log/{kimlik}.log", "ekleyen": "", "surum": sira.SURUM, **ek}
    sira.json_yaz(sira.dizin(spool, "bekleyen") / f"{kimlik}.json", veri)
    return kimlik


is_yaz.sayac = 0


def kayit(kimlik):
    return sira.json_oku(sira.bul(kimlik)[2])


def proje():
    """Geçici proje kökü: pc2'ye düşen türsüz oto işinin sarıldığı `tools/senaryo/uzak.sh` komutu yerelde koşturur."""
    kok = Path(tempfile.mkdtemp(dir=GECICI))
    (kok / "tools/senaryo").mkdir(parents=True)
    (kok / "tools/senaryo/uzak.sh").write_text('#!/bin/sh\nexec "$@"\n')
    (kok / "tools/senaryo/uzak.sh").chmod(0o755)
    return kok


def kosan_yaz(makine, komut, cwd=None):
    """Koşturucusuz koşan iş kaydı (yalnız sayım ve kurallar için)."""
    kimlik = is_yaz(makine, komut, cwd=cwd)
    os.rename(sira.dizin(makine, "bekleyen") / f"{kimlik}.json", sira.dizin(makine, "kosan") / f"{kimlik}.json")
    return kimlik


def surdur(kosucular, kimlikler, sure=30):
    """Koşucuların döngüsü (`calis` sinyalsiz): işlerin hepsi biten/'e düşene dek."""
    son = time.monotonic() + sure
    while time.monotonic() < son:
        for k in kosucular:
            k.yetimleri_topla()
            if k.makine == "pc2":
                k.eski_pc2_islerini_tasi()
            k.baslat()
        if all(sira.bul(i)[1] == "biten" and "bitti" in kayit(i) for i in kimlikler):
            return
        time.sleep(0.1)
    raise AssertionError(f"işler {sure} sn'de bitmedi: {[(i, sira.bul(i)[:2]) for i in kimlikler]}")


class SiraTest(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(sira.SPOOL, ignore_errors=True)
        self.kilitler = []

    def tearDown(self):
        for k in self.kilitler:
            k.close()

    def kosucu_kilidi(self, makine):
        """Başka bir koşucu çalışıyormuş gibi (`kosucu_canli`). Zaten tutuluyorsa yeniden alınmaz (aynı süreçte ikinci
        flock bekler)."""
        if sira.kosucu_canli(makine):
            return
        dosya = open(sira.dizin(makine) / "kosucu.lock", "a")
        fcntl.flock(dosya, fcntl.LOCK_EX)
        self.kilitler.append(dosya)

    def test_backfill_tek_yuvali_is_one_gecer(self):
        uzun = is_yaz("ana", ["sleep", "3"])
        iki = is_yaz("ana", ["sleep", "1"], yuva=2)
        tek = is_yaz("ana", ["sleep", "1"])
        surdur([sira_kosucu.Kosucu("ana", 2)], [uzun, iki, tek])
        u, i, t = kayit(uzun), kayit(iki), kayit(tek)
        self.assertLess(t["baslangic"], u["bitti"], "tek yuvalı iş sığmayan iki yuvalının önüne geçmeliydi")
        self.assertGreaterEqual(i["baslangic"], max(u["bitti"], t["bitti"]) - 0.5)
        self.assertEqual([u["cikis"], i["cikis"], t["cikis"]], [0, 0, 0])

    def test_aclik_korumasi(self):
        uzun = is_yaz("ana", ["sleep", "3"], once_sn=10)
        iki = is_yaz("ana", ["sleep", "1"], yuva=2, once_sn=5)
        tek = is_yaz("ana", ["sleep", "1"])
        surdur([sira_kosucu.Kosucu("ana", 2)], [uzun, iki, tek])
        u, i, t = kayit(uzun), kayit(iki), kayit(tek)
        self.assertGreaterEqual(i["baslangic"], u["bitti"] - 0.5)
        self.assertGreaterEqual(t["baslangic"], i["bitti"] - 0.5,
                                f"{sira.ACLIK_SN} sn'den uzun bekleyen iki yuvalı iş varken yeni iş alınmamalıydı")

    def test_oto_pc2_bossa_pc2_doluysa_ana(self):
        self.kosucu_kilidi("pc2")
        ana = sira_kosucu.Kosucu("ana", 2)
        bir = is_yaz("oto", ["sleep", "1"])
        ana.baslat()
        self.assertEqual(sira.bul(bir)[:2], ("oto", "bekleyen"), "boş yuva eşitken (2/2) iş pc2'nin")
        dolu = is_yaz("pc2", ["sleep", "3"], yuva=2)
        pc2 = sira_kosucu.Kosucu("pc2", 2)
        pc2.baslat()
        self.assertEqual(sira.bul(dolu)[:2], ("pc2", "kosan"))
        iki = is_yaz("oto", ["sleep", "1"])
        ana.baslat()
        for kimlik in (bir, iki):
            yer, durum, _ = sira.bul(kimlik)
            self.assertEqual((yer, durum), ("ana", "kosan"), "pc2 doluyken ana oto işini almalıydı")
            v = kayit(kimlik)
            self.assertEqual((v["makine"], v["kaynak"], v["env"].get("UZAK")), ("ana", "oto", "0"))
        surdur([ana, pc2], [bir, iki, dolu])
        for kimlik in (bir, iki, dolu):
            self.assertEqual(sira.bekle(kimlik, 5, cikti=lambda *_a, **_k: None), 0)

    def dagit(self, kimlikler, tur=6):
        """İki koşucunun `baslat` turları (öteki koşucu yaşıyor sayılır); işlerin makineleri."""
        for m in ("ana", "pc2"):
            self.kosucu_kilidi(m)
        kosucular = [sira_kosucu.Kosucu("pc2", 2), sira_kosucu.Kosucu("ana", 2)]
        for _ in range(tur):
            for k in kosucular:
                k.baslat()
        return kosucular, [sira.bul(k)[0] if sira.bul(k)[1] == "kosan" else None for k in kimlikler]

    def test_dagitim_bos_yuvasi_cok_olana_esitlikte_pc2(self):
        iki = [is_yaz("oto", ["sleep", "3"], cwd=proje()) for _ in range(2)]
        kosucular, yer = self.dagit(iki)
        self.assertEqual(yer, ["pc2", "ana"], "2 iş: 1. pc2 (eşitlik), 2. ana (boş yuvası çok)")
        self.assertIn("Yuva: ana 1/2 · pc2 1/2", sira.pano_metni())
        surdur(kosucular, iki)
        dort = [is_yaz("oto", ["sleep", "3"], cwd=proje()) for _ in range(4)]
        kosucular, yer = self.dagit(dort)
        self.assertEqual(yer, ["pc2", "ana", "pc2", "ana"])
        self.assertIn("Yuva: ana 2/2 · pc2 2/2 · oto bekleyen 0", sira.pano_metni())
        surdur(kosucular, dort)

    def test_dagitim_dolu_makine_atlanir(self):
        for _ in range(2):
            kosan_yaz("pc2", ["tools/senaryo/gradle_kapisi.sh", "l", "test"])
        pc2_dolu = is_yaz("oto", ["sleep", "0"], cwd=proje())
        _, yer = self.dagit([pc2_dolu], tur=1)
        self.assertEqual(yer, ["ana"], "pc2 doluyken ana alır")
        shutil.rmtree(sira.dizin("pc2", "kosan"))
        for k in self.kilitler:
            k.close()
        self.kilitler = []
        for _ in range(2):
            kosan_yaz("ana", ["tools/senaryo/gradle_kapisi.sh", "l", "test"])
        ana_dolu = is_yaz("oto", ["sleep", "0"], cwd=proje())
        _, yer = self.dagit([ana_dolu], tur=1)
        self.assertEqual(yer, ["pc2"], "ana doluyken pc2 alır")
        sonuc = sira_kosucu.Kosucu("pc2", 2)
        surdur([sonuc], [ana_dolu])
        shutil.rmtree(sira.dizin("ana", "kosan"))
        sira.dizin("ana", "kosan")
        surdur([sira_kosucu.Kosucu("ana", 2)], [pc2_dolu])

    def test_pc2_erisilemezse_yuvalari_kapali_oto_ana_pc2_isi_bekler(self):
        """K-E60 rev. 2: sahte ssh başarısız (çıkış 255) → pc2 yuvaları kapalı, oto işi ana'ya, pc2 işi ölmeden bekler;
        ssh geçince pc2 kendiliğinden açılır ve bekleyen iş koşar."""
        sahte = Path(tempfile.mkdtemp(dir=GECICI))
        ssh = sahte / "ssh"
        ssh.write_text('#!/bin/sh\necho "ssh: connect to host 192.0.2.1 port 22: No route to host" >&2\nexit 255\n')
        ssh.chmod(0o755)
        eski_yol = os.environ["PATH"]
        os.environ["PATH"] = f"{sahte}:{eski_yol}"
        try:
            sira_kosucu.Kosucu("pc2", 2).pc2_oku()
            self.assertIn("No route to host", sira.pc2_erisim_hatasi())
            kok = proje()
            mesajlar = []
            sabit = sira.ekle("pc2", [str(kok / "tools/senaryo/uzak.sh"), "sleep", "0"], cwd=kok,
                              cikti=lambda m, **_k: mesajlar.append(m))
            self.assertTrue(any("pc2 erişilemiyor" in m for m in mesajlar), mesajlar)
            oto = is_yaz("oto", ["sleep", "2"], cwd=proje())
            kosucular, yer = self.dagit([sabit, oto])
            self.assertEqual(yer, [None, "ana"], "pc2 kapalıyken oto işi ana'ya, pc2 işi beklemede")
            self.assertEqual(sira.bul(sabit)[:2], ("pc2", "bekleyen"))
            pano = sira.pano_metni()
            self.assertIn("pc2 erişilemiyor (No route to host)", pano)
            self.assertIn("Yuva: ana 1/2 · pc2 0/0 · oto bekleyen 0", pano)
            self.assertIn("bekliyor (1.; pc2 erişilemiyor)", pano)
            ssh.write_text("#!/bin/sh\ncat >/dev/null\necho @@\n")
            kosucular[0].pc2_oku()
            self.assertIsNone(sira.pc2_erisim_hatasi())
            self.assertNotIn("erişilemiyor", sira.pano_metni())
            surdur(kosucular, [sabit, oto])
        finally:
            os.environ["PATH"] = eski_yol
        self.assertEqual((kayit(sabit)["makine"], kayit(oto)["makine"]), ("pc2", "ana"))
        for kimlik in (sabit, oto):
            self.assertEqual(sira.bekle(kimlik, 5, cikti=lambda *_a, **_k: None), 0)

    def test_eski_pc2_isi_tasinir_kimlik_sabit(self):
        kimlik = is_yaz("pc2", ["tools/senaryo/gradle_kapisi.sh", f"{GECICI}/test.log", "test"])
        yol = sira.dizin("pc2", "bekleyen") / f"{kimlik}.json"
        veri = sira.json_oku(yol)
        del veri["surum"]
        sira.json_yaz(yol, veri)
        istemci = is_yaz("pc2", ["tools/senaryo/pc2_istemci.sh", "k/", "x_gorsel.py"], yuva=2, istemci=True)
        sira_kosucu.Kosucu("pc2", 2).eski_pc2_islerini_tasi()
        self.assertEqual(sira.bul(kimlik)[:2], ("oto", "bekleyen"))
        self.assertEqual(kayit(kimlik)["makine"], "oto")
        self.assertTrue(yol.is_symlink() and yol.exists(), "eski sira.py'nin `bul`'u işi pc2/bekleyen'de görmeli")
        self.assertEqual(sira.bul(istemci)[:2], ("pc2", "bekleyen"), "iki yuvalı pc2 istemci işi pc2'de kalır")
        oto = sira.dizin("oto", "bekleyen") / f"{kimlik}.json"
        kosan = sira.dizin("ana", "kosan") / oto.name
        os.rename(oto, kosan)
        veri = sira.makineye_hazirla(sira.json_oku(kosan), "ana")
        self.assertEqual(sira.bul(kimlik)[:2], ("ana", "kosan"))
        sira_kosucu.bitir(kosan, veri, durum="bitti", cikis=0, bitti=time.time(), sure=0)
        self.assertEqual(sira.bekle(kimlik, 5, cikti=lambda *_a, **_k: None), 0)
        sira_kosucu.Kosucu("pc2", 2).eski_pc2_islerini_tasi()
        self.assertFalse(os.path.lexists(yol), "sarkan bağ silinmeliydi")

    def test_ekle_makine_secimi_ve_istemci_cevirisi(self):
        w = str(sira.W)
        sessiz = lambda *_a, **_k: None  # noqa: E731
        gradle = sira.ekle("oto", ["tools/senaryo/gradle_kapisi.sh", f"{GECICI}/t.log", "test"], cwd=w, cikti=sessiz)
        rundata = sira.ekle("oto", ["./gradlew", "runData"], cwd=w, cikti=sessiz)
        gorsel = sira.ekle("oto", ["python3", "tools/senaryo/x_gorsel.py", ".vibe/kanit/x"], istemci=True, cwd=w,
                           cikti=sessiz)
        belirsiz = sira.ekle("oto", ["python3", "tools/senaryo/x_gorsel.py"], istemci=True, cwd=w, cikti=sessiz)
        olcum = sira.ekle("oto", ["python3", "tools/senaryo/x_gorsel.py", "k/", "olc"], istemci=True, olcum=True, cwd=w,
                          cikti=sessiz)
        sabit = sira.ekle("oto", ["tools/senaryo/pc2_istemci.sh", "k/", "x_gorsel.py"], cwd=w, cikti=sessiz)
        self.assertEqual([kayit(k)["makine"] for k in (gradle, rundata, gorsel, belirsiz, olcum, sabit)],
                         ["oto", "ana", "oto", "ana", "ana", "pc2"])
        v = kayit(gorsel)
        self.assertEqual((v["yuva"], v["kanit"]), (2, ".vibe/kanit/x/"))
        pc2 = sira.makineye_hazirla(dict(v), "pc2")
        self.assertEqual(pc2["komut"], ["tools/senaryo/pc2_istemci.sh", ".vibe/kanit/x/", "x_gorsel.py", ".vibe/kanit/x"])
        self.assertEqual((sira.gerek(pc2, "pc2"), sira.gerek(v, "ana")), (2, 1))
        ana = sira.makineye_hazirla(dict(v), "ana")
        self.assertEqual((ana["komut"], ana["env"]["UZAK"]), (v["komut"], "0"))
        with self.assertRaises(ValueError):
            sira.ekle("pc2", ["python3", "tools/senaryo/x_gorsel.py", "k/"], istemci=True, olcum=True, cwd=w,
                      cikti=sessiz)
        tursuz = sira.ekle(None, ["sleep", "1"], cwd=w, cikti=sessiz)
        oto_tursuz = sira.ekle("oto", ["sleep", "1"], cwd=w, cikti=sessiz)
        bayraksiz_gorsel = sira.ekle(None, ["python3", "tools/senaryo/x_gorsel.py", "k/"], cwd=w, cikti=sessiz)
        kos = sira.ekle(None, ["python3", "tools/senaryo/runclient_kilit.py", "kos", w, "--", "sh"], cwd=w, cikti=sessiz)
        self.assertEqual([(kayit(k)["makine"], kayit(k)["istemci"]) for k in (tursuz, oto_tursuz, bayraksiz_gorsel, kos)],
                         [("ana", False), ("oto", False), ("oto", True), ("ana", True)])
        self.assertEqual(sira.makineye_hazirla(kayit(oto_tursuz), "pc2")["komut"], [f"{w}/tools/senaryo/uzak.sh", "sleep", "1"])
        with self.assertRaises(ValueError):
            sira.ekle("oto", ["sleep", "1"], cwd=GECICI, cikti=sessiz)

    def test_ana_iki_yuvayi_beklemeden_doldurur_ayni_klasor_tek(self):
        ana = sira_kosucu.Kosucu("ana", 2)
        ortak = tempfile.mkdtemp(dir=GECICI)
        bir = is_yaz("oto", ["sleep", "2"], cwd=ortak)
        ayni = is_yaz("oto", ["sleep", "0"], cwd=ortak)
        iki = is_yaz("oto", ["sleep", "2"])
        ana.baslat()
        self.assertEqual([sira.bul(k)[:2] for k in (bir, ayni, iki)],
                         [("ana", "kosan"), ("oto", "bekleyen"), ("ana", "kosan")],
                         "ana iki oto işini aynı turda almalı; aynı klasörün ikinci işi beklemeli")
        surdur([ana], [bir, ayni, iki])

    def test_kuyruk_disi_gradle_sayilir_isin_gradlei_sayilmaz(self):
        kosan = kosan_yaz("ana", ["sleep", "30"])
        sahte = [sys.executable, "-c", "import time; time.sleep(30)", sira.GRADLE_ISTEMCI]
        isin = subprocess.Popen(sahte, env={**IS_DISI, "SIRA_IS": kosan})
        disari = subprocess.Popen(sahte, env=IS_DISI)
        try:
            bizim = {isin.pid, disari.pid}
            sira.ana_gradle_surecleri = lambda: [d for d in GERCEK_GRADLE() if d["pid"] in bizim]
            son = time.monotonic() + 10
            while len(sira.ana_gradle_surecleri()) < 2 and time.monotonic() < son:
                time.sleep(0.05)
            self.assertEqual([d["pid"] for d in sira.dis_surecler("ana")], [disari.pid])
            self.assertEqual(sira.doluluk("ana"), 2, "koşan iş (1) + kuyruk dışı Gradle (1)")
            pano = sira.pano_metni()
            self.assertIn(f"| pid {disari.pid} | ana |", pano)
            self.assertIn("Yuva: ana 2/2", pano)
            self.kosucu_kilidi("pc2")
            aday = {"makine": "oto", "cwd": "/baska", "yuva": 1, "eklendi": 0, "env": {}, "komut": ["sleep"]}
            self.assertEqual(sira_kosucu.Kosucu("ana", 2).engel("ana", aday), "ana: boş yuva 0")
        finally:
            sira.ana_gradle_surecleri = list
            for surec in (isin, disari):
                surec.kill()
                surec.wait()

    def test_pc2_kuyruk_disi_cozumleme_ve_sayim(self):
        kosan = kosan_yaz("pc2", ["tools/senaryo/gradle_kapisi.sh", "l", "test"])
        jar = "/k/.worktrees/{}/gradle/wrapper/gradle-wrapper.jar"
        ps = "\n".join([
            "  100     1  50 bash -s",
            f"  101   100  50 /usr/bin/java -classpath {jar.format('a')} {sira.GRADLE_ISTEMCI} test",
            "  200     1  20 sh -c x",
            f"  201   200  20 /usr/bin/java -classpath {jar.format('b')} {sira.GRADLE_ISTEMCI} runGameTestServer",
            f"  300     1   5 /usr/bin/java -classpath /k/gradle/wrapper/gradle-wrapper.jar {sira.GRADLE_ISTEMCI} help",
            "  400     1  99 /usr/bin/java org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.8"])
        dosyalar = "/tmp/uzak-5000-1.pid 100\n/tmp/uzak-6000-kuyruk.pid 200\n"
        surecler = sira.pc2_surecleri_coz(ps, dosyalar, yerel_is={5000: kosan}.get, simdi=1000)
        self.assertEqual([(d["pid"], d["uzak"], d["is"], d["baslangic"]) for d in surecler],
                         [(101, 5000, kosan, 950), (201, 6000, None, 980), (300, None, None, 995)])
        self.assertEqual(surecler[0]["komut"], "[a] gradlew test")
        sira.json_yaz(sira.dizin("pc2") / sira.PC2_SURECLER, {"zaman": time.time(), "surecler": surecler})
        self.assertEqual([d["pid"] for d in sira.dis_surecler("pc2")], [201, 300])
        self.assertEqual(sira.doluluk("pc2"), 2, "koşan iş (1; uzak.sh kilitli dış iş kilit sayımında) + elle ssh Gradle (1)")
        self.assertIn("| pid 300 | pc2 |", sira.pano_metni())
        sira.json_yaz(sira.dizin("pc2") / sira.PC2_SURECLER, {"zaman": time.time() - sira.PC2_SUREC_TAZE_SN - 1,
                                                            "surecler": surecler})
        self.assertEqual(sira.dis_surecler("pc2"), [])
        self.assertIn("pc2 kuyruk dışı bilinmiyor", sira.pano_metni())

    def oz_kuyruk(self, komut, spool, **ek):
        """Betiği iş dışından çağırır: iş `spool`/bekleyen'e düşmeli, bekleyen kesilince iptal olmalı. Kaydı döner."""
        surec = subprocess.Popen(komut, cwd=sira.W, env={**IS_DISI, **ek}, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        try:
            son, bekleyen = time.monotonic() + 90, []
            while not bekleyen and time.monotonic() < son:
                if surec.poll() is not None:
                    self.fail(f"kuyruğa girmeden çıktı: {surec.communicate()}")
                bekleyen = sira.isler(spool, "bekleyen")
                time.sleep(0.1)
            self.assertEqual(len(bekleyen), 1, f"{komut} {spool} kuyruğuna düşmedi")
            veri = bekleyen[0][1]
            time.sleep(0.3)
            surec.send_signal(signal.SIGTERM)
            self.assertEqual(surec.wait(10), 128 + signal.SIGTERM)
            self.assertEqual(kayit(veri["id"])["durum"], "iptal", "kesilen bekleyenin işi iptal olmalı")
            return veri
        finally:
            if surec.poll() is None:
                surec.kill()
            surec.communicate()

    def test_oz_kuyruklama_betikler_is_disinda_kuyruga_girer(self):
        log = f"{GECICI}/k/x.log"
        v = self.oz_kuyruk(["tools/senaryo/gradle_kapisi.sh", log, "compileJava"], "oto")
        self.assertEqual(v["komut"], ["tools/senaryo/gradle_kapisi.sh", log, "compileJava"])
        v = self.oz_kuyruk(["tools/senaryo/uzak.sh", "true"], "pc2")
        self.assertEqual(v["komut"], ["tools/senaryo/uzak.sh", "true"])
        v = self.oz_kuyruk(["tools/senaryo/pc2_istemci.sh", "k/", "x_gorsel.py"], "pc2")
        self.assertEqual((v["istemci"], v["yuva"]), (True, 2))
        kilit = str(sira.W / "tools/senaryo/runclient_kilit.py")
        v = self.oz_kuyruk([sys.executable, kilit, "kos", str(sira.W), "--", "true"], "ana")
        self.assertEqual((v["komut"][1:], v["istemci"]), ([kilit, "kos", str(sira.W), "--", "true"], True))
        v = self.oz_kuyruk([sys.executable, "tools/senaryo/deprem_sunucu.py", "--kanit", f"{GECICI}/k"], "oto",
                           DEFTER_ZORLA="1")
        self.assertEqual(v["komut"], ["python3", "tools/senaryo/deprem_sunucu.py", "--kanit", f"{GECICI}/k"])
        v = self.oz_kuyruk([sys.executable, "tools/senaryo/radar_gorsel.py", f"{GECICI}/k"], "oto", DEFTER_ZORLA="1")
        self.assertEqual((v["istemci"], v["kanit"]), (True, f"{GECICI}/k/"))

    def test_gradlew_is_disinda_reddedilir(self):
        sonuc = subprocess.run([str(sira.W / "gradlew"), "compileJava"], cwd=sira.W, env=IS_DISI, capture_output=True,
                               text=True, timeout=30)
        self.assertEqual(sonuc.returncode, 3)
        self.assertIn("tools/senaryo/sira.py ekle -- ./gradlew compileJava", sonuc.stderr)

    def test_ekle_bekle_log_akar_cikis_kodu_isin(self):
        surec = subprocess.Popen([sys.executable, str(sira.W / "tools/senaryo/sira.py"), "ekle", "--bekle", "--makine", "ana",
                                  "--", "sh", "-c", "echo merhaba; exit 7"], cwd=GECICI, env=IS_DISI,
                                 stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        ana = sira_kosucu.Kosucu("ana", 2)
        son = time.monotonic() + 30
        while surec.poll() is None and time.monotonic() < son:
            ana.yetimleri_topla()
            ana.baslat()
            time.sleep(0.1)
        cikti, _ = surec.communicate(timeout=10)
        self.assertEqual(surec.returncode, 7)
        self.assertIn("merhaba", cikti)

    def test_canli_hyprland_eski_imzayi_atlar(self):
        calisma = Path(tempfile.mkdtemp(dir=GECICI))
        for ad in ("canli", "eski"):
            (calisma / "hypr" / ad).mkdir(parents=True)
        (calisma / "hypr/eski/.socket.sock").touch()
        os.utime(calisma / "hypr/eski", (time.time() + 10,) * 2)
        with socket.socket(socket.AF_UNIX) as dinleyen:
            dinleyen.bind(str(calisma / "hypr/canli/.socket.sock"))
            dinleyen.listen()
            self.assertEqual(sira_kosucu.runclient_kilit.canli_hyprland(calisma), "canli")
        self.assertIsNone(sira_kosucu.runclient_kilit.canli_hyprland(calisma))

    def test_ana_isi_oturumun_ekranini_alir_hyprland_yoksa_istemci_almaz(self):
        rk = sira_kosucu.runclient_kilit
        gercek = rk.oturum_ekrani
        rk.oturum_ekrani = lambda: {"HYPRLAND_INSTANCE_SIGNATURE": "canli", "WAYLAND_DISPLAY": "wayland-1"}
        try:
            eski = {"HYPRLAND_INSTANCE_SIGNATURE": "eski"}
            self.assertEqual(sira_kosucu.is_ortami({"id": "x", "makine": "ana", "env": eski})["HYPRLAND_INSTANCE_SIGNATURE"],
                             "canli")
            self.assertEqual(sira_kosucu.is_ortami({"id": "x", "makine": "pc2", "env": eski})["HYPRLAND_INSTANCE_SIGNATURE"],
                             "eski")
        finally:
            rk.oturum_ekrani = gercek
        ana = sira_kosucu.Kosucu("ana", 2)
        ana.hyprland = (time.monotonic(), None)
        kimlik = is_yaz("oto", ["sleep", "0"], istemci=True, kanit="k/")
        ana.baslat()
        self.assertEqual(sira.bul(kimlik)[:2], ("oto", "bekleyen"), "yaşayan Hyprland yokken istemci işi pc2'ye kalır")

    def test_ayni_klasor_iki_makinede_birer_is_pc2_gradlew_haric(self):
        self.kosucu_kilidi("pc2")
        ana, pc2 = sira_kosucu.Kosucu("ana", 2), sira_kosucu.Kosucu("pc2", 2)
        bir, iki = tempfile.mkdtemp(dir=GECICI), tempfile.mkdtemp(dir=GECICI)
        kosan_yaz("pc2", ["tools/senaryo/gradle_kapisi.sh", "l", "test"], cwd=bir)
        kosan_yaz("pc2", ["./gradlew", "test"], cwd=iki)
        aday = {"cwd": bir, "komut": ["tools/senaryo/gradle_kapisi.sh", "l2", "test"]}
        self.assertFalse(ana.klasor_engeli("ana", aday), "pc2'deki gradle_kapisi.sh işiyle aynı klasörden iş ana'da koşar")
        self.assertTrue(ana.klasor_engeli("ana", {**aday, "cwd": iki}), "pc2'deki ./gradlew build/ geri çeker")
        kosan_yaz("ana", ["tools/senaryo/gradle_kapisi.sh", "l3", "test"], cwd=bir)
        self.assertTrue(pc2.klasor_engeli("pc2", {"cwd": bir, "komut": ["./gradlew", "test"]}))
        self.assertFalse(pc2.klasor_engeli("pc2", {"cwd": bir, "komut": ["tools/senaryo/gradle_kapisi.sh", "l4", "test"]}))

    def test_java_home_varsayilani(self):
        os.environ.pop("JAVA_HOME", None)
        veri = {"id": "x", "makine": "ana", "env": {}}
        self.assertEqual(sira_kosucu.is_ortami(veri)["JAVA_HOME"], "/home/lagxt/.jdks/jdk-17.0.21+9")
        veri["env"]["JAVA_HOME"] = "/baska/jdk"
        self.assertEqual(sira_kosucu.is_ortami(veri)["JAVA_HOME"], "/baska/jdk", "kayıttaki JAVA_HOME ezilmemeli")
        os.environ["JAVA_HOME"] = "/kabuk/java21"
        try:
            kimlik = sira.ekle("ana", ["sleep", "0"], cwd=GECICI, cikti=lambda *_a, **_k: None)
        finally:
            del os.environ["JAVA_HOME"]
        self.assertNotIn("JAVA_HOME", kayit(kimlik)["env"], "kabuğun JAVA_HOME'u kayda geçmemeli")
        self.assertEqual(sira_kosucu.is_ortami(kayit(kimlik))["JAVA_HOME"], sira.JAVA_HOME)

    def test_ana_tek_istemci(self):
        ana = sira_kosucu.Kosucu("ana", 2)
        ana.hyprland = (time.monotonic(), "sinama")
        sira.json_yaz(Path(os.environ["DOGALAFETLER_RUNCLIENT_KILIT"]),
                      {"pid": os.getpid(), "baslangic": sira_kosucu.runclient_kilit.baslangic_saati(os.getpid())})
        kimlik = is_yaz("oto", ["sleep", "0"], istemci=True, kanit="k/")
        ana.baslat()
        self.assertEqual(sira.bul(kimlik)[:2], ("oto", "bekleyen"), "runClient kilidi tutulurken istemci işi alınmamalı")
        Path(os.environ["DOGALAFETLER_RUNCLIENT_KILIT"]).unlink()
        ana.baslat()
        self.assertEqual(sira.bul(kimlik)[:2], ("ana", "kosan"))
        surdur([ana], [kimlik])


if __name__ == "__main__":
    try:
        unittest.main()
    finally:
        shutil.rmtree(GECICI, ignore_errors=True)
        shutil.rmtree(sira.UZAK_KILITLERI, ignore_errors=True)
