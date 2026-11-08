"""Sıcaklık görsel kanıt karelerinin piksel ölçümü (S3-10). Ağır iş değil: yalnız PNG ve log okur.

Kullanım: python3 tools/senaryo/sicaklik_olcum.py <kanıt/>   (sicaklik_gorsel.py hud <kanıt/> koşusundan sonra)

Çıktı <kanıt/>/olcum.txt ve ekrana. Ölçüler:
  kenar   ekran kenarı bandı (kısa kenarın %12'si; alttaki %25 HUD/sıcak çubuğu için dışarıda) aynı kameranın normal
          karesine göre: ortalama CIELAB ΔE (görünür renk farkı), ΔE > 10 olan piksel oranı, ortalama RGB farkı ve
          kızıllık (R - (G+B)/2) ile beyazlık (en küçük kanal) değişimi. Birinci şahıs kareleri `normal`, üçüncü şahıs
          kareleri `normal_3p` ile karşılaştırılır.
  ter     lacivert kontürlü, parlak gövdeli damla lekeleri (ikisini de içeren bağlı bileşen, en az 12 piksel): sayı ve
          her damlanın çevresindeki zemine WCAG karşıtlığı (kontür ya da gövdeden yüksek olanı).
  buhar   `normal_3p`'ye göre ekranın orta üst bölgesinde açılan (L* +8, ΔE > 10) piksel sayısı ve ortalama ΔE.
  hap     HUD hapının içinde sayı pikselleri (log'daki sayı rengine yakın) ile hap içi zemininin WCAG karşıtlığı.
          Kare satırında hap konumu yoksa (S3-10 öncesi koşu) yerleşimden hesaplanır ve sayı durum rengindedir.
"""
import re
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

KARE = re.compile(r"SICAKLIK_OTOMASYON kare (sicaklik_\w+)\.png \((.*)\)\s*$")
KONTUR = (18, 14, 12)
# S3-10 öncesi hap sayısı durum rengindeydi (HudCizimi.renk).
ESKI_SAYI_RENGI = {"NORMAL": "e8e6de", "USUME": "a6ddf8", "DONMA": "5eaeff", "BUNALMA": "ffb54a", "YANMA": "ff5a36"}
# HudCizimi: bant genişliği 182, hap sol kenarı 74, hap 34x11; bantın tepesi ekranın altından 47 GUI pikseli.
GENISLIK, HAP_X, HAP_G, HAP_Y, ALTTAN = 182, 74, 34, 11, 47


def kare_satirlari(kanit):
    loglar = sorted(kanit.glob("latest-hud-*.log"), key=lambda p: int(p.stem.rsplit("-", 1)[1]))
    if not loglar:
        sys.exit(f"{kanit}: latest-hud-*.log yok")
    satirlar = {}
    for satir in loglar[-1].read_text(errors="replace").splitlines():
        m = KARE.search(satir)
        if m:
            satirlar[m.group(1).removeprefix("sicaklik_")] = m.group(2)
    return loglar[-1], satirlar


def alan(metin, desen, donustur=str):
    m = re.search(desen, metin)
    return donustur(m.group(1)) if m else None


def rgb(yol):
    return np.asarray(Image.open(yol).convert("RGB")).astype(np.float64)


def lab(r):
    c = r / 255.0
    c = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    m = np.array([[0.4124, 0.3576, 0.1805], [0.2126, 0.7152, 0.0722], [0.0193, 0.1192, 0.9505]])
    x = (c @ m.T) / np.array([0.95047, 1.0, 1.08883])
    f = np.where(x > 0.008856, np.cbrt(x), 7.787 * x + 16 / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]), 200 * (f[..., 1] - f[..., 2])], -1)


def parlaklik(c):
    s = np.asarray(c, dtype=np.float64) / 255.0
    s = np.where(s <= 0.04045, s / 12.92, ((s + 0.055) / 1.055) ** 2.4)
    return 0.2126 * s[..., 0] + 0.7152 * s[..., 1] + 0.0722 * s[..., 2]


def karsitlik(a, b):
    la, lb = parlaklik(a), parlaklik(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def bant(h, w):
    m = np.zeros((h, w), bool)
    b = int(min(h, w) * 0.12)
    m[:b, :] = m[:, :b] = m[:, -b:] = m[-b:, :] = True
    m[int(h * 0.75):, :] = False
    return m


def kenar(kare, ref):
    m = bant(*kare.shape[:2])
    de = np.linalg.norm(lab(kare) - lab(ref), axis=-1)[m]
    fark = np.abs(kare - ref).mean(axis=2)[m]
    kizil = lambda r: r[..., 0] - (r[..., 1] + r[..., 2]) / 2  # noqa: E731
    return {"ΔE": de.mean(), "ΔE>10 %": (de > 10).mean() * 100, "ΔRGB": fark.mean(),
            "kızıllık Δ": (kizil(kare) - kizil(ref))[m].mean(), "beyazlık Δ": (kare.min(axis=2) - ref.min(axis=2))[m].mean()}


def bilesenler(maske):
    """4-komşulu bağlı bileşenler: (y dizisi, x dizisi) listesi."""
    gorulen = np.zeros_like(maske)
    h, w = maske.shape
    for y0, x0 in zip(*np.nonzero(maske)):
        if gorulen[y0, x0]:
            continue
        kuyruk = deque([(y0, x0)])
        gorulen[y0, x0] = True
        ys, xs = [], []
        while kuyruk:
            y, x = kuyruk.popleft()
            ys.append(y)
            xs.append(x)
            for ny, nx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                if 0 <= ny < h and 0 <= nx < w and maske[ny, nx] and not gorulen[ny, nx]:
                    gorulen[ny, nx] = True
                    kuyruk.append((ny, nx))
        yield np.array(ys), np.array(xs)


def ter(kare):
    r, g, b = kare[..., 0], kare[..., 1], kare[..., 2]
    kontur = (r < 70) & (g < 90) & (b >= 45) & (b < 170) & (b - r >= 30)
    govde = (r >= 140) & (g >= 200) & (b >= 220) & (g - r >= 25)
    h, w = r.shape
    damlalar = []
    for ys, xs in bilesenler(kontur | govde):
        if len(ys) < 12 or not kontur[ys, xs].any() or not govde[ys, xs].any():
            continue
        y0, y1 = max(0, ys.min() - 4), min(h, ys.max() + 5)
        x0, x1 = max(0, xs.min() - 4), min(w, xs.max() + 5)
        kutu = np.ones((y1 - y0, x1 - x0), bool)
        kutu[ys - y0, xs - x0] = False
        zemin = np.median(kare[y0:y1, x0:x1][kutu], axis=0)
        k = max(karsitlik(np.median(kare[ys, xs][kontur[ys, xs]], axis=0), zemin),
                karsitlik(np.median(kare[ys, xs][govde[ys, xs]], axis=0), zemin))
        damlalar.append((len(ys), k, zemin))
    return damlalar


def buhar(kare, ref):
    h, w = kare.shape[:2]
    bolge = (slice(int(h * 0.08), int(h * 0.6)), slice(int(w * 0.3), int(w * 0.7)))
    lk, lr = lab(kare[bolge]), lab(ref[bolge])
    de = np.linalg.norm(lk - lr, axis=-1)
    acilan = (de > 10) & (lk[..., 0] - lr[..., 0] > 8)
    return int(acilan.sum()), float(de[acilan].mean()) if acilan.any() else 0.0


def hap(kare, satir):
    olcek = alan(satir, r"ölçek=([\d.]+)", float)
    hx, hy = alan(satir, r"hap x=(-?\d+)", int), alan(satir, r"hap x=-?\d+ y=(-?\d+)", int)
    renk = alan(satir, r"sayı rengi=([0-9a-f]{6})")
    h, w = kare.shape[:2]
    if hx is None:
        olcek = olcek or 4.0
        gw, gh = int(w / olcek), int(h / olcek)
        hx = (gw - GENISLIK) // 2 + HAP_X + (alan(satir, r"dx=(-?\d+)", int) or 0)
        hy = gh - ALTTAN + (alan(satir, r"dy=(-?\d+)", int) or 0)
        renk = ESKI_SAYI_RENGI[alan(satir, r"durum (\w+)")]
    s = int(olcek)
    ic = kare[(hy + 1) * s:(hy + HAP_Y - 1) * s, (hx + 2) * s:(hx + HAP_G - 2) * s]
    hedef = np.array([int(renk[i:i + 2], 16) for i in (0, 2, 4)], np.float64)
    sayi = np.abs(ic - hedef).max(axis=2) <= 40
    kontur = np.abs(ic - np.array(KONTUR)).max(axis=2) <= 30
    zemin = ~sayi & ~kontur
    if not sayi.any():
        return None
    return sayi.sum(), karsitlik(np.median(ic[sayi], axis=0), np.median(ic[zemin], axis=0))


def main():
    kanit = Path(sys.argv[1]).resolve()
    log, satirlar = kare_satirlari(kanit)
    cikti = [f"# {kanit.name}: kare satırları {log.name}"]
    kareler = {ad: rgb(kanit / f"sicaklik_{ad}.png") for ad in satirlar if (kanit / f"sicaklik_{ad}.png").exists()}
    for ad, satir in satirlar.items():
        if ad not in kareler:
            continue
        kare = kareler[ad]
        kamera = alan(satir, r"kamera (\w+)")
        parcacik = alan(satir, r"kendi parçacığı (\d+)")
        durum, vucut = alan(satir, r"durum (\w+)"), alan(satir, r"vücut ([\d.]+)")
        parcalar = [f"{ad}: durum {durum}, vücut {vucut} °C, kamera {kamera}, kendi parçacığı {parcacik}"]
        ref = kareler.get("normal" if kamera == "FIRST_PERSON" else "normal_3p")
        if ref is not None and ad not in ("normal", "normal_3p", "menu") and ref.shape == kare.shape:
            parcalar.append("kenar " + " ".join(f"{k}={v:.1f}" for k, v in kenar(kare, ref).items()))
        if kamera != "FIRST_PERSON" and ad != "menu":
            damlalar = ter(kare)
            if damlalar:
                k = sorted(d[1] for d in damlalar)
                parcalar.append(f"ter damlası {len(damlalar)} (piksel {sum(d[0] for d in damlalar)}), karşıtlık en az "
                                f"{k[0]:.1f} ortanca {k[len(k) // 2]:.1f}")
            else:
                parcalar.append("ter damlası 0")
            if ref is not None and ref.shape == kare.shape and ad != "normal_3p":
                n, de = buhar(kare, ref)
                parcalar.append(f"buhar (orta üst, açılan) {n} piksel ΔE {de:.1f}")
        if ad != "menu":
            sonuc = hap(kare, satir)
            parcalar.append("hap sayısı bulunamadı" if sonuc is None
                            else f"hap sayısı {sonuc[0]} piksel, zemine karşıtlık {sonuc[1]:.2f}:1")
        cikti.append(" | ".join(parcalar))
    metin = "\n".join(cikti) + "\n"
    (kanit / "olcum.txt").write_text(metin, encoding="utf-8")
    print(metin, end="")


if __name__ == "__main__":
    main()
