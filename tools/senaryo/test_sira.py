#!/usr/bin/env python3
"""sira.py / sira_kosucu.py sınaması (K-E53): python3 tools/senaryo/test_sira.py

Geçici spool (SIRA_KOK), geçici uzak.sh kilit klasörü (UZAK_MAKINE) ve geçici runClient kilidi; gerçek kuyruğa ve pc2'ye
dokunmaz. Koşucunun `baslat` döngüsü bu süreçte sürülür, işler gerçek koşturucuyla (`--kos`) `sleep` olarak koşar.
"""
import fcntl
import os
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

GECICI = tempfile.mkdtemp(prefix="sira-sinama-")
os.environ.update(SIRA_KOK=f"{GECICI}/spool", SIRA_PANO=f"{GECICI}/pano.md", SIRA_ACLIK_SN="2", SIRA_OTO_BEKLE_SN="60",
                  UZAK_MAKINE=f"sinama-{secrets.token_hex(3)}", DOGALAFETLER_RUNCLIENT_KILIT=f"{GECICI}/runclient.lock",
                  KAPI_RAM_MB="0")
sys.path.insert(0, str(Path(__file__).resolve().parent))
import sira  # noqa: E402
import sira_kosucu  # noqa: E402

GERCEK_GRADLE = sira.gradle_istemcileri
sira.gradle_istemcileri = list


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
        """Başka bir koşucu çalışıyormuş gibi (`kosucu_canli`)."""
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
        self.assertEqual(sira.bul(bir)[:2], ("oto", "bekleyen"), "pc2 boşken ana oto işini 60 sn içinde almamalı")
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

    def test_oto_uzun_bekleyeni_ana_alir(self):
        self.kosucu_kilidi("pc2")
        kimlik = is_yaz("oto", ["sleep", "0"], once_sn=sira.OTO_BEKLE_SN + 1)
        sira_kosucu.Kosucu("ana", 2).baslat()
        self.assertEqual(sira.bul(kimlik)[:2], ("ana", "kosan"))

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
        self.assertEqual(sira.dolu_mu("pc2", GECICI), "pc2+oto kuyruğunda 2 bekleyen iş")
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

    def test_ana_iki_yuvayi_beklemeden_doldurur_ayni_klasor_tek(self):
        self.kosucu_kilidi("pc2")
        ana = sira_kosucu.Kosucu("ana", 2)
        ortak = tempfile.mkdtemp(dir=GECICI)
        bir = is_yaz("oto", ["sleep", "2"], cwd=ortak, once_sn=sira.OTO_BEKLE_SN + 3)
        ayni = is_yaz("oto", ["sleep", "0"], cwd=ortak, once_sn=sira.OTO_BEKLE_SN + 2)
        iki = is_yaz("oto", ["sleep", "2"], once_sn=sira.OTO_BEKLE_SN + 1)
        ana.baslat()
        self.assertEqual([sira.bul(k)[:2] for k in (bir, ayni, iki)],
                         [("ana", "kosan"), ("oto", "bekleyen"), ("ana", "kosan")],
                         "ana iki oto işini aynı turda almalı; aynı klasörün ikinci işi beklemeli")
        surdur([ana], [bir, ayni, iki])

    def test_kuyruk_isinin_kendi_oturumundaki_gradle_sayilmaz(self):
        kod = ("import subprocess, sys; subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)', "
               "'org.gradle.wrapper.GradleWrapperMain'], start_new_session=True).wait()")
        is_sureci = subprocess.Popen([sys.executable, "-c", kod], start_new_session=True)
        bizim = []
        try:
            son = time.monotonic() + 10
            while not bizim and time.monotonic() < son:
                bizim = [c for c in GERCEK_GRADLE() if is_sureci.pid in c[1]]
                time.sleep(0.05)
            self.assertEqual(len(bizim), 1, "işin kendi oturumunu açan Gradle istemcisi atalarından bulunmalı")
            sira.gradle_istemcileri = lambda: bizim
            self.kosucu_kilidi("pc2")
            ana = sira_kosucu.Kosucu("ana", 2)
            aday = {"makine": "oto", "cwd": "/baska", "yuva": 1, "eklendi": 0, "env": {}, "komut": ["sleep"]}
            kosan = {"id": "x", "cwd": "/yok", "yuva": 1, "kaynak": "oto", "baslangic": 0}
            self.assertIsNone(ana.ana_engeli(aday, [{**kosan, "kosturucu": is_sureci.pid}], time.time()))
            self.assertEqual(ana.ana_engeli(aday, [{**kosan, "kosturucu": None}], time.time()),
                             "ana makinede 2 ağır süreç")
        finally:
            sira.gradle_istemcileri = list
            for pid, _ in bizim:
                os.kill(pid, 9)
            is_sureci.kill()
            is_sureci.wait()

    def test_ana_tek_istemci(self):
        self.kosucu_kilidi("pc2")
        ana = sira_kosucu.Kosucu("ana", 2)
        sira.json_yaz(Path(os.environ["DOGALAFETLER_RUNCLIENT_KILIT"]),
                      {"pid": os.getpid(), "baslangic": sira_kosucu.runclient_kilit.baslangic_saati(os.getpid())})
        kimlik = is_yaz("oto", ["sleep", "0"], istemci=True, once_sn=sira.OTO_BEKLE_SN + 1, kanit="k/")
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
