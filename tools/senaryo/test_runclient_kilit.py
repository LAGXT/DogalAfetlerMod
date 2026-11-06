#!/usr/bin/env python3
"""runclient_kilit.py sınaması: python3 tools/senaryo/test_runclient_kilit.py"""
import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import runclient_kilit as rk  # noqa: E402


def sessiz(_):
    pass


def olu_mu(pid, sure=10):
    """pid {@code sure} sn içinde yok ya da zombi olursa True."""
    son = time.monotonic() + sure
    while time.monotonic() < son:
        try:
            metin = Path(f"/proc/{pid}/stat").read_text()
        except (FileNotFoundError, ProcessLookupError):
            return True
        if metin[metin.rindex(")") + 2] == "Z":
            return True
        time.sleep(0.05)
    return False


class KilitTest(unittest.TestCase):
    def setUp(self):
        self.klasor = tempfile.TemporaryDirectory()
        os.environ["DOGALAFETLER_RUNCLIENT_KILIT"] = str(Path(self.klasor.name) / "runclient.lock")
        self.uyku = subprocess.Popen(["sleep", "60"])

    def tearDown(self):
        self.uyku.kill()
        self.uyku.wait()
        os.environ.pop("DOGALAFETLER_RUNCLIENT_KILIT")
        self.klasor.cleanup()

    def test_al_birak_ve_yeniden_giris(self):
        self.assertTrue(rk.al("a", os.getpid(), 0, log=sessiz))
        self.assertTrue(rk.al("a", os.getpid(), 0, log=sessiz), "aynı sahip yeniden alır")
        kayit = rk.oku()
        self.assertEqual((kayit["pid"], kayit["worktree"]), (os.getpid(), "a"))
        self.assertTrue(rk.birak(os.getpid(), log=sessiz))
        self.assertIsNone(rk.oku())

    def test_canli_sahip_varken_alinamaz_ve_birakilamaz(self):
        self.assertTrue(rk.al("a", self.uyku.pid, 0, log=sessiz))
        bas = time.monotonic()
        self.assertFalse(rk.al("b", os.getpid(), 0.3, aralik=0.1, log=sessiz))
        self.assertGreaterEqual(time.monotonic() - bas, 0.3)
        self.assertFalse(rk.birak(os.getpid(), log=sessiz), "başkasının kilidi bırakılmaz")
        self.assertEqual(rk.oku()["worktree"], "a")

    def test_olu_sahip_devralinir(self):
        self.assertTrue(rk.al("a", self.uyku.pid, 0, log=sessiz))
        self.uyku.kill()
        self.uyku.wait()
        self.assertTrue(rk.al("b", os.getpid(), 0, log=sessiz))
        self.assertEqual(rk.oku()["worktree"], "b")

    def test_pid_yeniden_kullanimi_ve_bozuk_dosya_olu_sayilir(self):
        yol = rk.kilit_yolu()
        yol.write_text('{"pid": %d, "baslangic": 1, "worktree": "x"}' % os.getpid())
        self.assertTrue(rk.al("b", self.uyku.pid, 0, log=sessiz), "başlangıç saati tutmuyor: ölü")
        yol.write_text("bozuk")
        self.assertTrue(rk.al("c", os.getpid(), 0, log=sessiz))

    def test_bekleyen_sahip_birakinca_alir(self):
        self.assertTrue(rk.al("a", self.uyku.pid, 0, log=sessiz))
        threading.Timer(0.3, lambda: rk.birak(self.uyku.pid, log=sessiz)).start()
        self.assertTrue(rk.al("b", os.getpid(), 5, aralik=0.1, log=sessiz))

    def test_iki_bekleyen_olu_kilidi_birlikte_alamaz(self):
        self.assertTrue(rk.al("a", self.uyku.pid, 0, log=sessiz))
        self.uyku.kill()
        self.uyku.wait()
        ikinci = subprocess.Popen(["sleep", "60"])
        try:
            sonuclar = []
            iplikler = [threading.Thread(target=lambda p=p: sonuclar.append(rk.al("x%d" % p, p, 0, log=sessiz)))
                        for p in (os.getpid(), ikinci.pid)]
            for i in iplikler:
                i.start()
            for i in iplikler:
                i.join()
            self.assertEqual(sorted(sonuclar), [False, True])
        finally:
            ikinci.kill()
            ikinci.wait()

    def test_kos_komutu_alir_calistirir_birakir(self):
        betik = Path(__file__).resolve().parent / "runclient_kilit.py"
        cikti = subprocess.run([sys.executable, str(betik), "kos", "t", "--bekle", "0", "--", "sh", "-c",
                                f"{sys.executable} {betik} durum"], capture_output=True, text=True)
        self.assertEqual(cikti.returncode, 0, cikti.stderr)
        self.assertIn("'worktree': 't'", cikti.stdout)
        self.assertIsNone(rk.oku())


    def test_kos_sinyalde_cocugu_oldurur_kilidi_birakir(self):
        betik = Path(__file__).resolve().parent / "runclient_kilit.py"
        for sinyal in (signal.SIGTERM, signal.SIGINT):
            with self.subTest(sinyal=sinyal.name):
                pidler = Path(self.klasor.name) / f"pidler-{sinyal.name}"
                kos = subprocess.Popen([sys.executable, str(betik), "kos", "t", "--bekle", "0", "--", "sh", "-c",
                                        f"sleep 60 & echo $$ $! > {pidler}.tmp && mv {pidler}.tmp {pidler}; wait"],
                                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                son = time.monotonic() + 10
                while not pidler.exists() and time.monotonic() < son:
                    time.sleep(0.05)
                try:
                    cocuklar = [int(p) for p in pidler.read_text().split()]
                    self.assertEqual(rk.oku()["pid"], kos.pid)
                    kos.send_signal(sinyal)
                    self.assertEqual(kos.wait(15), 128 + sinyal)
                    self.assertIsNone(rk.oku(), "sinyalde kilit bırakılır")
                    for pid in cocuklar:
                        self.assertTrue(olu_mu(pid), f"çocuk/torun {pid} sinyalden sonra yaşıyor")
                finally:
                    if kos.poll() is None:
                        kos.kill()
                        kos.wait()
                    rk.kilit_yolu().unlink(missing_ok=True)


if __name__ == "__main__":
    unittest.main()
