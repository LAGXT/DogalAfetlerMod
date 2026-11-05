#!/usr/bin/env python3
"""Donma spike'ının runServer senaryosu (oyuncusuz), tekrar edilebilir.

Her koşu kendi dünyasını sıfırdan üretir: `run-server/donma-senaryo` klasörü silinir, `run-server/server.properties`
geçici olarak `level-name=donma-senaryo` (seed 20261105) yapılır ve koşu bitince özgün içeriğine geri döndürülür.
`test` dünyasına dokunulmaz. Sunucu açılınca başlangıç durumu yine de zorlanır (`forceload remove all`, G=0, çözülme
kapalı, yapay yük 0, bütçe 256).

Sunucu pty'siz, stdin'i boruya bağlı açılır (docs/MAKINE.md, KARARLAR K0-18); komutlar stdin'den yazılır, her adım log
desenine bakılarak süre sınırıyla beklenir. Beklenen satır gelmezse adımın adı ve son 20 satır log'a ve stderr'e
yazılır; `stop` her durumda gönderilir. Senaryo:
  0. Başlangıç durumunu zorla.
  1. G=60; spawn chunk'ları hedefe yetişir, /forge tps ve bilgi çıktısı.
  2. Uzak chunk (2000, 2000) izlenir, forceload add -> 0'dan 60'a yetişir -> forceload remove (seviye NBT'ye yazılır).
  3. G=100; spawn yetişir; forceload add -> kayıtlı seviye 60 okunur, 60'tan 100'e adım adım yetişir.
  4. Yapay yükle TPS düşürülür -> bütçe 1/2 ve 1/4; yük kalkınca 1/1.
  5. Çözülme dönemi + G=40 için kış maskesi çıktıları; stop.
Sonda log'dan denetlenir: 0→60 ve 60→100 adım dizileri kesintisiz, `Stopping the server` var.

Kullanım (worktree kökünden): python3 tools/spike/donma_sunucu_senaryosu.py <log yolu>
Çıkış: 0 senaryo ve denetimler geçti, 1 senaryo/denetim hatası, 2 sunucu süreci sıfır dışı kodla çıktı.
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

KOK = Path(__file__).resolve().parents[2]
JAVA_HOME = "/home/lagxt/.jdks/jdk-17.0.21+9"
TOPLAM_SURE = 1800
UZAK = (2000, 2000)
SUNUCU_KLASORU = KOK / "run-server"
SENARYO_DUNYASI = "donma-senaryo"
SENARYO_OZELLIKLERI = {"level-name": SENARYO_DUNYASI, "level-seed": "20261105", "online-mode": "false"}

satirlar = []
kilit = threading.Condition()
baslangic = time.monotonic()


class SenaryoHatasi(Exception):
    pass


def gecen():
    return time.monotonic() - baslangic


def ozellikleri_senaryoya_cevir():
    """server.properties'i senaryo dünyasına çevirir; özgün baytları (dosya yoksa None) döndürür."""
    yol = SUNUCU_KLASORU / "server.properties"
    ozgun = yol.read_bytes() if yol.exists() else None
    eski = [] if ozgun is None else ozgun.decode("iso-8859-1").splitlines()
    yeni = [s for s in eski if s.split("=", 1)[0].strip() not in SENARYO_OZELLIKLERI]
    yeni += [f"{anahtar}={deger}" for anahtar, deger in SENARYO_OZELLIKLERI.items()]
    SUNUCU_KLASORU.mkdir(parents=True, exist_ok=True)
    yol.write_bytes(("\n".join(yeni) + "\n").encode("iso-8859-1"))
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


def main():
    log_yolu = Path(sys.argv[1])
    log_yolu.parent.mkdir(parents=True, exist_ok=True)
    log = log_yolu.open("w", encoding="utf-8")
    adim = ["hazırlık"]

    def yaz_log(metin):
        log.write(f"[+{gecen():7.1f}s] {metin}\n")
        log.flush()

    def adima_gec(ad):
        adim[0] = ad
        yaz_log(f"### {ad}")

    def sonlandir(_isaret, _cerceve):
        raise SystemExit(143)

    # SIGTERM (ör. `timeout`) Python'da varsayılan olarak finally'yi atlar; SystemExit'e çevrilince stop ve geri yazma çalışır.
    signal.signal(signal.SIGTERM, sonlandir)
    senaryo_dunyasini_sil()
    ozgun_ozellikler = ozellikleri_senaryoya_cevir()
    yaz_log(f"senaryo dünyası run-server/{SENARYO_DUNYASI} silindi; server.properties geçici: {SENARYO_OZELLIKLERI}")

    ortam = dict(os.environ, JAVA_HOME=JAVA_HOME)
    try:
        surec = subprocess.Popen(
            ["./gradlew", "runServer", "--console=plain"], cwd=KOK, env=ortam, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", bufsize=1,
            start_new_session=True)
    except BaseException:
        ozellikleri_geri_al(ozgun_ozellikler)
        raise

    def okuyucu():
        for satir in surec.stdout:
            satir = satir.rstrip("\n")
            yaz_log(satir)
            with kilit:
                satirlar.append((gecen(), satir))
                kilit.notify_all()

    threading.Thread(target=okuyucu, daemon=True).start()

    def bekci():
        time.sleep(TOPLAM_SURE)
        if surec.poll() is None:
            yaz_log("!!! toplam süre aşıldı, süreç grubu SIGTERM")
            os.killpg(surec.pid, signal.SIGTERM)

    threading.Thread(target=bekci, daemon=True).start()

    def komut(metin):
        yaz_log(f">>> {metin}")
        surec.stdin.write(metin + "\n")
        surec.stdin.flush()

    def bekle(desen, sure, baslangic_indeksi=0):
        """İndeksten sonra desene uyan ilk satırın indeksini döndürür; süre dolarsa ya da süreç biterse hata."""
        rx = re.compile(desen)
        son = time.monotonic() + sure
        i = baslangic_indeksi
        with kilit:
            while True:
                while i < len(satirlar):
                    if rx.search(satirlar[i][1]):
                        return i
                    i += 1
                if surec.poll() is not None:
                    raise SenaryoHatasi(f"sunucu süreci {surec.returncode} koduyla bitti; beklenen satır: {desen}")
                kalan = son - time.monotonic()
                if kalan <= 0:
                    raise SenaryoHatasi(f"beklenen satır {sure} sn içinde gelmedi: {desen}")
                kilit.wait(min(kalan, 1.0))

    def indeks():
        with kilit:
            return len(satirlar)

    def satir(i):
        with kilit:
            return satirlar[i][1]

    def sakinlesmeyi_bekle(baslangic_indeksi, gerekli=True, sessiz=15, en_cok=420, tps_araligi=20):
        """Özet satırları (100 tick'te bir, yalnız iş varken) {sessiz} sn kesilene kadar bekler; arada /forge tps.

        {gerekli}: en az bir özet görülmeli (iş başlatılan adımlar). Kapalıyken hiç iş yoksa {sessiz} sn sonra döner.
        """
        rx = re.compile(r"\[donma\] özet:")
        bas = time.monotonic()
        son = bas + en_cok
        son_tps = bas
        son_ozet = bas
        goruldu = False
        i = baslangic_indeksi
        while time.monotonic() < son:
            with kilit:
                kilit.wait(1.0)
                while i < len(satirlar):
                    if rx.search(satirlar[i][1]):
                        goruldu = True
                        son_ozet = time.monotonic()
                    i += 1
            if surec.poll() is not None:
                raise SenaryoHatasi(f"sunucu süreci {surec.returncode} koduyla bitti (sakinleşme beklenirken)")
            if time.monotonic() - son_tps >= tps_araligi:
                komut("forge tps")
                komut("dogalafet spike donma bilgi")
                son_tps = time.monotonic()
            if (goruldu or not gerekli) and time.monotonic() - son_ozet >= sessiz:
                return
        raise SenaryoHatasi(f"özet satırları {en_cok} sn içinde kesilmedi" if goruldu
                            else f"{en_cok} sn içinde hiç [donma] özet satırı gelmedi")

    def adimlari_dogrula(baslangic_indeksi, ilk, hedef):
        """İzlenen chunk'ın {ilk}'ten {hedef}'e 5'lik adımları kesintisiz mi; son adımın indeksini döndürür."""
        bitis = bekle(rf"izlenen chunk .* seviye \d+ → {hedef} \(hedef {hedef}\)", 240, baslangic_indeksi)
        rx = re.compile(rf"izlenen chunk .* seviye (\d+) → (\d+) \(hedef {hedef}\)")
        with kilit:
            bulunan = [tuple(map(int, m.groups())) for _, s in satirlar[baslangic_indeksi:bitis + 1]
                       if (m := rx.search(s))]
        beklenen = [(s, s + 5) for s in range(ilk, hedef, 5)]
        if bulunan != beklenen:
            raise SenaryoHatasi(f"adım dizisi {ilk}→{hedef} beklenenden farklı: {bulunan}")
        yaz_log(f"--- denetim: izlenen chunk {ilk}→{hedef} {len(bulunan)} adımda, kesintisiz")
        return bitis

    def forceload_ekle(x, z, baslangic_indeksi):
        komut(f"forceload add {x} {z}")
        i = bekle(r"Marked chunk .* to be force loaded|No chunks were marked for force loading", 60,
                  baslangic_indeksi)
        if "No chunks were marked" in satir(i):
            raise SenaryoHatasi("chunk zaten forceload'lu: senaryo dünyası temiz değil")

    def hata_ozeti(hata):
        with kilit:
            son = [s for _, s in satirlar[-20:]]
        mesaj = [f"!!! senaryo hatası, adım '{adim[0]}': {hata}", "!!! son 20 satır:"] + [f"!!!   {s}" for s in son]
        for m in mesaj:
            yaz_log(m)
        print("\n".join(mesaj), file=sys.stderr)

    sonuc = 1
    try:
        adima_gec("sunucu açılışı")
        bekle(r"Done \(", 900)
        # server.properties yalnız açılışta okunur; hemen geri yazılır, böylece betik nasıl biterse bitsin dosya bozuk kalmaz.
        ozellikleri_geri_al(ozgun_ozellikler)
        yaz_log("server.properties özgün haline döndürüldü (sunucu açıldı)")

        adima_gec("0. başlangıç durumunu zorla")
        i = indeks()
        komut("forceload remove all")
        bekle(r"Unmarked all force loaded chunks", 60, i)
        komut("dogalafet spike donma yapayyuk 0")
        komut("dogalafet spike donma butce 256")
        komut("dogalafet spike donma cozulme false")
        komut("dogalafet spike donma 0")
        bekle(r"global ilerleme \d+ → 0;", 60, i)
        sakinlesmeyi_bekle(i, gerekli=False)
        i = indeks()
        komut("dogalafet spike donma bilgi")
        bekle(r"Donma: G=0 çözülme=false \|", 60, i)
        komut("forge tps")
        komut("dogalafet spike donma kis")

        adima_gec("1. G=60, spawn chunk'ları yetişiyor")
        i = indeks()
        komut("dogalafet spike donma 60")
        bekle(r"global ilerleme 0 → 60;", 60, i)
        sakinlesmeyi_bekle(i)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")

        adima_gec("2. uzak chunk: forceload add -> 0'dan 60'a")
        x, z = UZAK
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma izle")
        i = indeks()
        forceload_ekle(x, z, i)
        bekle(r"izlenen chunk .* yüklendi \(yeni=true\): kayıtlı seviye 0", 180, i)
        adimlari_dogrula(i, 0, 60)
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma bilgi")
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma kis")

        adima_gec("2b. forceload remove -> chunk boşaltılır, seviye 60 kaydedilir")
        i = indeks()
        komut(f"forceload remove {x} {z}")
        bekle(r"izlenen chunk .* boşaltıldı: seviye 60", 180, i)
        komut("save-all flush")
        bekle(r"Saved the game", 120, i)

        adima_gec("3. G=100, spawn chunk'ları 60'tan 100'e")
        i = indeks()
        komut("dogalafet spike donma 100")
        bekle(r"global ilerleme 60 → 100;", 60, i)
        sakinlesmeyi_bekle(i)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")

        adima_gec("3b. uzak chunk'a dönüş: forceload add -> kayıtlı 60'tan 100'e adım adım")
        i = indeks()
        forceload_ekle(x, z, i)
        bekle(r"izlenen chunk .* yüklendi \(yeni=false\): kayıtlı seviye 60", 180, i)
        adimlari_dogrula(i, 60, 100)
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma bilgi")
        komut("forge tps")

        adima_gec("4. TPS kısması: yapay yük")
        i = indeks()
        komut("dogalafet spike donma yapayyuk 58")
        yari = bekle(r"\[donma\] TPS .* bütçe 1/2", 60, i)
        komut("dogalafet spike donma 80")
        time.sleep(8)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")
        komut("dogalafet spike donma yapayyuk 80")
        # 58 ms yük donma işiyle birlikte TPS'i 15'in altına kendisi indirebilir; 1/4 satırı 1/2'den sonra aranır.
        bekle(r"\[donma\] TPS .* bütçe 1/4", 60, yari)
        time.sleep(8)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")
        i = indeks()
        komut("dogalafet spike donma yapayyuk 0")
        bekle(r"\[donma\] TPS .* bütçe 1/1", 60, i)
        sakinlesmeyi_bekle(i, gerekli=False)
        komut("forge tps")
        komut("dogalafet spike donma bilgi")

        adima_gec("5. çözülme dönemi ve kış maskesi")
        i = indeks()
        komut("dogalafet spike donma cozulme true")
        komut("dogalafet spike donma 40")
        bekle(r"global ilerleme 80 → 40;", 60, i)

        adima_gec("5b. dağ chunk'ı: çözülme döneminde ilk kez yüklenir, Y üst sınırındaki sütunların hedefi 100")
        i = indeks()
        komut("dogalafet spike donma dagbul 0 0 140")
        j = bekle(r"\[donma\] dağ bulundu: -?\d+ -?\d+ -?\d+", 120, i)
        dx, dy, dz = map(int, re.search(r"dağ bulundu: (-?\d+) (-?\d+) (-?\d+)", satir(j)).groups())
        cx, cz = dx // 16, dz // 16
        komut(f"execute positioned {dx} 64 {dz} run dogalafet spike donma izle")
        i = indeks()
        forceload_ekle(dx, dz, i)
        bekle(rf"izlenen chunk \[{cx}, {cz}\] yüklendi \(yeni=true\): kayıtlı seviye 0", 180, i)
        bekle(rf"izlenen chunk \[{cx}, {cz}\] seviye \d+ → 40 \(hedef 40\)", 300, i)
        i = indeks()
        komut(f"execute positioned {dx} {dy} {dz} run dogalafet spike donma ornek")
        j = bekle(r"\[donma\] w_sütun özeti", 60, i)
        esik, ust, tam = map(int, re.search(r"Y≥(\d+): (\d+) sütun, hedefi 100: (\d+)", satir(j)).groups())
        if ust == 0 or ust != tam:
            raise SenaryoHatasi(f"dağ chunk'ında Y≥{esik} {ust} sütunun yalnız {tam} tanesinin hedefi 100")
        yaz_log(f"--- denetim: dağ chunk'ı [{cx}, {cz}] Y≥{esik} {ust} sütunun hepsinin hedefi 100")
        komut(f"execute positioned {dx} {dy} {dz} run dogalafet spike donma kis")
        komut(f"forceload remove {dx} {dz}")
        for kx, kz in [(0, 0), (x, z), (-3000, 1500), (4000, -2500), (8000, 8000)]:
            komut(f"execute positioned {kx} 64 {kz} run dogalafet spike donma kis")
        time.sleep(10)
        komut("dogalafet spike donma bilgi")
        komut(f"execute positioned {x} 64 {z} run dogalafet spike donma bilgi")
        komut("forge tps")
        time.sleep(3)
        sonuc = 0
    except Exception as hata:  # senaryo hatası log'a yazılır, sunucu yine temiz kapatılır
        hata_ozeti(hata)
    finally:
        adima_gec("kapanış")
        if surec.poll() is None:
            komut("stop")
            try:
                surec.wait(timeout=180)
            except subprocess.TimeoutExpired:
                yaz_log("!!! stop sonrası kapanmadı, SIGTERM")
                os.killpg(surec.pid, signal.SIGTERM)
                try:
                    surec.wait(timeout=60)
                except subprocess.TimeoutExpired:
                    yaz_log("!!! SIGTERM sonrası da kapanmadı, SIGKILL")
                    os.killpg(surec.pid, signal.SIGKILL)
                    surec.wait()
        time.sleep(1)
        ozellikleri_geri_al(ozgun_ozellikler)
        yaz_log("server.properties özgün haline döndürüldü")
        with kilit:
            durdu = any("Stopping the server" in s for _, s in satirlar)
        if not durdu:
            yaz_log("!!! denetim: 'Stopping the server' satırı yok")
            print("!!! 'Stopping the server' satırı yok", file=sys.stderr)
            sonuc = 1
        yaz_log(f"### süreç çıkış kodu {surec.returncode}, senaryo sonucu {sonuc}")
        log.close()
    sys.exit(sonuc if surec.returncode == 0 else 2)


if __name__ == "__main__":
    main()
