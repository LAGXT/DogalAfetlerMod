"""Sıcaklık görsel kanıt karelerinin piksel ölçümü (S3-10, S3-11). Ağır iş değil: yalnız PNG ve log okur.

Kullanım: python3 tools/senaryo/sicaklik_olcum.py <kanıt/>   (sicaklik_gorsel.py hud <kanıt/> koşusundan sonra)

Çıktı <kanıt/>/olcum.txt ve ekrana. Ölçüler:
  kenar   ekran kenarı bandı (kısa kenarın %12'si; alttaki %25 HUD/sıcak çubuğu için dışarıda) aynı kameranın normal
          karesine göre: ortalama CIELAB ΔE (görünür renk farkı), ΔE > 10 olan piksel oranı, ortalama RGB farkı ve
          kızıllık (R - (G+B)/2) ile beyazlık (en küçük kanal) değişimi. Birinci şahıs kareleri `normal` ile, üçüncü
          şahıs kareleri aynı kamera açısındaki buharsız kontrol kare ile karşılaştırılır: `<durum>_<zemin>` için
          `normal_<zemin>` (ör. `usume_gok` → `normal_gok`); eski adlı `usume` için `normal_3p`. Ter sahneleri (`bunalma`,
          `ter_*`) düşen damlayı bekleyip başı başka pozda yakaladığı için karşılaştırılmaz.
  ter     ter dokusunun tam paleti (lacivert kontür 10,30,84 ve buz beyazı gövde 196,240,255, ±12): ikisini de içeren
          bağlı bileşen (6–800 piksel) bir damla; sayı ve her damlanın çevresindeki zemine WCAG karşıtlığı (kontür ya
          da gövdeden yüksek olanı).
  buhar   yalnız ağız penceresinde (önden 3. şahıs kamerası gözü ekranın ortasında tutar; pencere ortanın
          0,1·Y sağı/solu, 0,08·Y üstü ile 0,12·Y altı) kontrol kareye göre beyaza doğru tek tip karışım: her kanalda
          α = (kare − kontrol) / (255 − kontrol), en az iki geçerli kanal (255 − kontrol > 30) ve kanallar arası fark
          < 0,15; sonuç açık renk (L* ≥ 40; modelin koyu piksellerindeki poz/ışık oynaması sayılmaz); ter damlasının
          lacivert kontürünün 4 piksel çevresi (beyaz parlaması) dışarıda; kontrol karede 0,02·Y yarıçap içinde aynı renk
          (kanal farkı ≤ 4) varsa piksel kaymış dokudur (model biraz farklı pozda), sayılmaz. α ≥ 0,15 piksel sayısı ile
          bu piksellerde α ortalama / p90 / en çok. Beyaz zemin (kar; kontrol L* ≥ 85) beyazlaşmayı ölçmeye yer bırakmaz:
          orada buharı gri kenarı seçtirir, bu yüzden ayrıca açık zeminde kontrole göre ΔE > 10 olan (kaymış doku hariç)
          piksel sayısı ve ortalama ΔE. Yalnız kontrolle aynı yolla çekilen kareler (buhar ve düz 3. şahıs HUD sahneleri)
          ölçülür: ter sahneleri düşen damlayı beklediği için başı başka pozda yakalar, başın başka yüzü beyazlaşma gibi
          görünür. Buharsız `bunalma_<zemin>` (düz 3. şahıs HUD karesi) negatif kontroldur: ≈ 0 çıkmalı.
  hap     HUD hapının içinde sayı pikselleri (log'daki sayı rengine yakın) ile hap içi zemininin WCAG karşıtlığı ve
          zeminin ortanca rengi. Kare satırında hap konumu yoksa (S3-10 öncesi koşu) yerleşimden hesaplanır ve sayı
          durum rengindedir.
"""
import re
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

KARE = re.compile(r"SICAKLIK_OTOMASYON kare (sicaklik_\w+)\.png \((.*)\)\s*$")
KONTUR = (18, 14, 12)
TER_KONTUR, TER_GOVDE = (10, 30, 84), (196, 240, 255)
# Kontrolüyle aynı yolla çekilen eski adlı 3. şahıs karesi; ter sahneleri (`bunalma`, `ter_*`) karşılaştırılmaz.
ESKI_3P = ("usume",)
TER_SAHNELERI = ("bunalma",)
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
    kontur = np.abs(kare - np.array(TER_KONTUR)).max(axis=2) <= 12
    govde = np.abs(kare - np.array(TER_GOVDE)).max(axis=2) <= 12
    h, w = kontur.shape
    damlalar = []
    for ys, xs in bilesenler(kontur | govde):
        if not 6 <= len(ys) <= 800 or not kontur[ys, xs].any() or not govde[ys, xs].any():
            continue
        y0, y1 = max(0, ys.min() - 4), min(h, ys.max() + 5)
        x0, x1 = max(0, xs.min() - 4), min(w, xs.max() + 5)
        kutu = np.ones((y1 - y0, x1 - x0), bool)
        kutu[ys - y0, xs - x0] = False
        zemin = np.median(kare[y0:y1, x0:x1][kutu], axis=0)
        k = max(karsitlik(TER_KONTUR, zemin), karsitlik(TER_GOVDE, zemin))
        damlalar.append((len(ys), k, zemin))
    return damlalar


def agiz_penceresi(h, w):
    """Yüz önü: önden 3. şahıs kamerası gözü ekranın ortasında tutar, ağız ~0,12 blok altında; buhar kameraya doğru
    savrulur. Kollar ve omuzlar (pozu kareden kareye oynar) pencerenin dışında kalır."""
    return slice(int(h / 2 - 0.08 * h), int(h / 2 + 0.12 * h)), slice(int(w / 2 - 0.1 * h), int(w / 2 + 0.1 * h))


def kaymis_doku(k, ref, p):
    """Penceredeki her piksel için: kontrol karede 0,02·Y yarıçap içinde aynı renk (kanal farkı ≤ 4) var mı."""
    h, w = ref.shape[:2]
    rad = max(4, round(0.02 * h))
    y0, y1, x0, x1 = p[0].start, p[0].stop, p[1].start, p[1].stop
    genis = ref[max(0, y0 - rad):min(h, y1 + rad), max(0, x0 - rad):min(w, x1 + rad)]
    oy, ox = y0 - max(0, y0 - rad), x0 - max(0, x0 - rad)
    var = np.zeros(k.shape[:2], bool)
    for dy in range(-rad, rad + 1):
        for dx in range(-rad, rad + 1):
            ys, xs = oy + dy, ox + dx
            if ys < 0 or xs < 0 or ys + k.shape[0] > genis.shape[0] or xs + k.shape[1] > genis.shape[1]:
                continue
            var |= np.abs(k - genis[ys:ys + k.shape[0], xs:xs + k.shape[1]]).max(axis=2) <= 4
    return var


def buhar(kare, ref):
    """Ağız penceresinde kontrole göre beyaza tek tip karışım: (piksel, α ort, α p90, α en çok) ve açık zeminde (kontrol
    L* ≥ 85) ΔE > 10 olan (piksel, ΔE ort)."""
    p = agiz_penceresi(*kare.shape[:2])
    k, r = kare[p], ref[p]
    pay = 255 - r
    gecerli = pay > 30
    a = np.where(gecerli, (k - r) / np.maximum(pay, 1), np.inf)
    ort = np.where(gecerli, a, 0).sum(axis=2) / np.maximum(gecerli.sum(axis=2), 1)
    fark = np.where(gecerli, a, -np.inf).max(axis=2) - a.min(axis=2)
    ter_cevresi = np.abs(k - np.array(TER_KONTUR)).max(axis=2) <= 12
    for _ in range(4):
        t = ter_cevresi.copy()
        t[1:] |= ter_cevresi[:-1]
        t[:-1] |= ter_cevresi[1:]
        t[:, 1:] |= ter_cevresi[:, :-1]
        t[:, :-1] |= ter_cevresi[:, 1:]
        ter_cevresi = t
    lk, lr = lab(k), lab(r)
    acik = lk[..., 0] >= 40
    kaymis = kaymis_doku(k, ref, p)
    buharli = (gecerli.sum(axis=2) >= 2) & (fark < 0.15) & (ort >= 0.15) & acik & ~ter_cevresi & ~kaymis
    de = np.linalg.norm(lk - lr, axis=-1)
    beyaz_zeminde = (lr[..., 0] >= 85) & (de > 10) & ~ter_cevresi & ~kaymis
    kar = (int(beyaz_zeminde.sum()), float(de[beyaz_zeminde].mean()) if beyaz_zeminde.any() else 0.0)
    if not buharli.any():
        return 0, 0.0, 0.0, 0.0, kar
    v = ort[buharli]
    return int(buharli.sum()), float(v.mean()), float(np.percentile(v, 90)), float(v.max()), kar


def kontrol_adi(ad, birinci, kareler):
    if birinci:
        return "normal"
    if ad in TER_SAHNELERI or ad.startswith("ter_"):
        return None
    zemin = ad.rsplit("_", 1)[1] if "_" in ad else None
    if zemin and f"normal_{zemin}" in kareler:
        return f"normal_{zemin}"
    return "normal_3p" if ad in ESKI_3P else None


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
    taban = np.median(ic[zemin], axis=0)
    return sayi.sum(), karsitlik(np.median(ic[sayi], axis=0), taban), "%02x%02x%02x" % tuple(int(c) for c in taban)


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
        birinci = kamera == "FIRST_PERSON"
        kontrol = kontrol_adi(ad, birinci, kareler)
        ref = kareler.get(kontrol) if kontrol else None
        karsilastir = ref is not None and ref.shape == kare.shape and ad != kontrol and ad != "menu"
        if karsilastir:
            parcalar.append("kenar " + " ".join(f"{k}={v:.1f}" for k, v in kenar(kare, ref).items()))
        if not birinci:
            damlalar = ter(kare)
            if damlalar:
                k = sorted(d[1] for d in damlalar)
                parcalar.append(f"ter damlası {len(damlalar)} (piksel {sum(d[0] for d in damlalar)}), karşıtlık en az "
                                f"{k[0]:.1f} ortanca {k[len(k) // 2]:.1f}")
            else:
                parcalar.append("ter damlası 0")
            if karsilastir:
                n, ort, p90, enc, kar = buhar(kare, ref)
                parcalar.append(f"buhar (ağız penceresi, {kontrol} karşısı) {n} piksel α ort {ort:.2f} p90 {p90:.2f} "
                                f"en çok {enc:.2f}; açık zeminde ΔE>10 {kar[0]} piksel ΔE ort {kar[1]:.1f}")
        if ad != "menu":
            sonuc = hap(kare, satir)
            parcalar.append("hap sayısı bulunamadı" if sonuc is None
                            else f"hap sayısı {sonuc[0]} piksel, zemine karşıtlık {sonuc[1]:.2f}:1 (hap içi #{sonuc[2]})")
        cikti.append(" | ".join(parcalar))
    metin = "\n".join(cikti) + "\n"
    (kanit / "olcum.txt").write_text(metin, encoding="utf-8")
    print(metin, end="")


if __name__ == "__main__":
    main()
