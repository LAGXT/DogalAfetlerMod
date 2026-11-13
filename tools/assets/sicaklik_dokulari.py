"""Sıcaklık sisteminin dokularını üretir (madde 7c, 7d; S3-07; docs/tasarim/sicaklik.md).

Çıktılar (src/main/resources/assets/dogalafetler/textures/):
  misc/sicaklik_buz.png       256x256 buz kenarı (alfa): kenarlardan içeri dallanan buz kristalleri + kırağı
  misc/sicaklik_bunalma.png   256x256 buz kenarının kırmızı karşılığı (aynı kenar maskesi): süzülen ter izleri + ısı lekeleri
  misc/sicaklik_sicak.png     256x256 sıcak hava vinyeti; dikey yönde kesintisiz (efekt onu yukarı kaydırır)
  mob_effect/islak.png        18x18   Islak gösterge efekti: su damlası
  particle/nefes_buhari_0..3  16x16   nefes buharı bulutçuğu: yoğun beyaz iç, açık gri kenar (yaşla yayılıp seyrelir)
  particle/ter_damlasi_0..3   8x8     ter damlası: lacivert kontür, parlak gövde, düşüş izi (0-2 düşen, 3 sıçrama)

Çalıştırma: KÖK/.venv/bin/python tools/assets/sicaklik_dokulari.py (aynı tohum -> aynı bayt).
"""
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

KOK = Path(__file__).resolve().parents[2]
DOKU = KOK / "src/main/resources/assets/dogalafetler/textures"

SEFFAF = (0, 0, 0, 0)


def karistir(c, d, t):
    return tuple(round(c[i] + (d[i] - c[i]) * t) for i in range(3))


def piksel_sanati(px, u0, v0, satirlar, palet):
    for y, satir in enumerate(satirlar):
        for x, k in enumerate(satir):
            if k != " ":
                px[u0 + x, v0 + y] = palet[k]


def dal(d, rnd, x, y, aci, uzunluk, kalinlik, derinlik):
    if derinlik == 0 or uzunluk < 3:
        return
    x2 = x + math.cos(aci) * uzunluk
    y2 = y + math.sin(aci) * uzunluk
    alfa = int(120 + 90 * (derinlik / 5))
    d.line([(x, y), (x2, y2)], fill=(235, 250, 255, alfa), width=max(1, kalinlik))
    adim = rnd.randint(2, 4)
    for i in range(1, adim + 1):
        t = i / (adim + 1)
        bx = x + (x2 - x) * t
        by = y + (y2 - y) * t
        for isaret in (-1, 1):
            dal(d, rnd, bx, by, aci + isaret * math.radians(60), uzunluk * (0.45 - 0.05 * i) * (1 - t * 0.3),
                max(1, kalinlik - 1), derinlik - 1)
    dal(d, rnd, x2, y2, aci + rnd.uniform(-0.2, 0.2), uzunluk * 0.55, max(1, kalinlik - 1), derinlik - 1)


def buz_dokusu():
    n = 256
    rnd = random.Random(20261106)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    dx = (xx - (n - 1) / 2) / (n / 2)
    dy = (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx ** 2 * 0.9 + dy ** 2 * 1.1)
    kenar = np.clip((r - 0.62) / 0.5, 0, 1) ** 1.6
    gurultu = np.random.default_rng(7).random((n, n))
    kiragi = np.clip(kenar * (0.55 + 0.45 * gurultu), 0, 1)
    taban = np.zeros((n, n, 4), dtype=np.uint8)
    taban[..., 0] = 200
    taban[..., 1] = 235
    taban[..., 2] = 255
    taban[..., 3] = (kiragi * 200).astype(np.uint8)
    img = Image.fromarray(taban, "RGBA")
    kristal = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(kristal)
    for _ in range(26):
        kenar_no = rnd.randrange(4)
        p = rnd.uniform(0, n)
        x, y, aci = [(p, -2, math.pi / 2), (p, n + 1, -math.pi / 2), (-2, p, 0.0), (n + 1, p, math.pi)][kenar_no]
        aci += rnd.uniform(-0.5, 0.5)
        dal(d, rnd, x, y, aci, rnd.uniform(28, 62), 2, 5)
    kristal = kristal.filter(ImageFilter.GaussianBlur(0.4))
    img = Image.alpha_composite(img, kristal)
    a = np.array(img).astype(np.float64)
    # Ortaya doğru kristaller de sönsün: ekranın ortası açık kalır.
    a[..., 3] *= np.clip((r - 0.35) / 0.45, 0, 1)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def ter_izi(d, rnd, x, y, uzunluk, kalinlik):
    """Aşağı süzülen ter izi: hafif kıvrılarak iner, ucunda parlak bir damla birikir. Buz kristali kadar opak."""
    noktalar = [(x, y)]
    adim = 6
    for i in range(1, int(uzunluk / adim) + 1):
        x += rnd.uniform(-1.6, 1.6)
        noktalar.append((x, y + i * adim))
    for (x1, y1), (x2, y2) in zip(noktalar, noktalar[1:]):
        t = y1 / 256
        d.line([(x1, y1), (x2, y2)], fill=(240, 44, 30, int(225 - 50 * t)), width=kalinlik)
    ux, uy = noktalar[-1]
    r = kalinlik + 1.4
    d.ellipse([ux - r, uy - r * 0.8, ux + r, uy + r * 1.3], fill=(255, 70, 46, 235))
    d.ellipse([ux - r * 0.45, uy - r * 0.5, ux + r * 0.05, uy], fill=(255, 200, 176, 225))


def bunalma_dokusu():
    """Buz kenarının kırmızı karşılığı (S3-10): aynı kenar maskesi ve kırağı yoğunluğunda lekeli kızarma, kristallerin
    yerinde aşağı süzülen ter izleri, alttan ısı kıvrımları; ortası aynı ölçüde açık kalır."""
    n = 256
    rnd = random.Random(20261108)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    dx = (xx - (n - 1) / 2) / (n / 2)
    dy = (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx ** 2 * 0.9 + dy ** 2 * 1.1)
    kenar = np.clip((r - 0.62) / 0.5, 0, 1) ** 1.6
    # Isı lekeleri: 16x16 gürültü yumuşakça büyütülür; buzdaki kırağı gürültüsünün yerini alır.
    leke = np.random.default_rng(11).random((16, 16))
    leke = np.array(Image.fromarray((leke * 255).astype(np.uint8), "L").resize((n, n), Image.BICUBIC)) / 255.0
    taban = np.zeros((n, n, 4), dtype=np.uint8)
    taban[..., 0] = np.clip(215 + 40 * leke, 0, 255).astype(np.uint8)
    taban[..., 1] = (34 + 40 * leke).astype(np.uint8)
    taban[..., 2] = (22 + 20 * leke).astype(np.uint8)
    taban[..., 3] = (np.clip(kenar * (0.55 + 0.45 * leke), 0, 1) * 200).astype(np.uint8)
    img = Image.fromarray(taban, "RGBA")
    iz = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(iz)
    # Üst kenardan süzülen izler.
    for _ in range(28):
        ter_izi(d, rnd, rnd.uniform(0, n), -2, rnd.uniform(30, 90), rnd.choice((1, 2, 2)))
    # Yan kenarlarda içeri doğru kısa, aşağı inen izler.
    for _ in range(22):
        sol = rnd.random() < 0.5
        x = rnd.uniform(1, 20) if sol else n - 1 - rnd.uniform(1, 20)
        ter_izi(d, rnd, x, rnd.uniform(0, n - 40), rnd.uniform(16, 50), rnd.choice((1, 2)))
    # Alt kenardan yükselen ısı kıvrımları.
    for _ in range(16):
        x0 = rnd.uniform(0, n)
        boy = rnd.uniform(24, 56)
        faz = rnd.uniform(0, 2 * math.pi)
        noktalar = [(x0 + 3 * math.sin(faz + t / 6), n + 1 - t) for t in range(0, int(boy), 2)]
        d.line(noktalar, fill=(255, 96, 50, 170), width=2)
    iz = iz.filter(ImageFilter.GaussianBlur(0.4))
    img = Image.alpha_composite(img, iz)
    a = np.array(img).astype(np.float64)
    # Ekranın ortası açık kalır (buz kenarıyla aynı sönme).
    a[..., 3] *= np.clip((r - 0.35) / 0.45, 0, 1)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def sicak_dokusu():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    dx = (xx - (n - 1) / 2) / (n / 2)
    dy = (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx ** 2 + dy ** 2 * 0.8)
    kenar = np.clip((r - 0.45) / 0.7, 0, 1) ** 1.3
    # Dikeyde 256 periyotlu dalgalar: doku kaydırılınca dikis görünmez.
    faz = 2 * math.pi * yy / n
    dalga = (0.5 + 0.5 * np.sin(6 * faz + 2.2 * np.sin(2 * math.pi * xx / n * 3 + 3 * faz))) ** 3
    alfa = np.clip(kenar * (0.55 + 0.45 * dalga) + 0.10 * dalga * (1 - kenar), 0, 1)
    sicaklik = np.clip(kenar * 1.2, 0, 1)
    img = np.zeros((n, n, 4), dtype=np.uint8)
    img[..., 0] = 255
    img[..., 1] = (170 - 120 * sicaklik).astype(np.uint8)
    img[..., 2] = (60 - 50 * sicaklik).astype(np.uint8)
    img[..., 3] = (alfa * 190).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def damla_maskesi(boyut, cx, cy, yaricap, tepe):
    """Alttan yuvarlak, tepeye doğru sivrilen damla; piksel merkezleriyle sınanan maske."""
    m = np.zeros((boyut, boyut), dtype=bool)
    for y in range(boyut):
        for x in range(boyut):
            px, py = x + 0.5, y + 0.5
            if math.hypot(px - cx, py - cy) <= yaricap:
                m[y, x] = True
            elif tepe <= py < cy:
                m[y, x] = abs(px - cx) <= yaricap * (py - tepe) / (cy - tepe)
    return m


def islak_ikonu():
    """18x18 su damlası; mevcut efekt ikonlarının diliyle: koyu kontür, açık dolgu, beyaz parlama."""
    m = damla_maskesi(18, 9.0, 11.2, 5.6, 1.5)
    img = Image.new("RGBA", (18, 18), SEFFAF)
    px = img.load()
    for y in range(18):
        for x in range(18):
            if not m[y, x]:
                continue
            kenar = any(not (0 <= x + a < 18 and 0 <= y + b < 18 and m[y + b, x + a])
                        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if kenar:
                px[x, y] = (29, 90, 140, 255)
            else:
                t = y / 17
                px[x, y] = karistir((150, 210, 255), (64, 140, 235), t) + (255,)
    for x, y in ((6, 9), (6, 10), (6, 11), (7, 8), (7, 12)):
        px[x, y] = (255, 255, 255, 255)
    return img


BUHAR_KENAR = (92, 102, 128)
BUHAR_IC = (226, 234, 246)


def buhar_karesi(i):
    """16x16 nefes bulutçuğu (S3-11): dört yumuşak topak; iç yoğun, açık mavimsi gri (karda da beyaz zeminden ayrılır),
    geniş koyu gri kenar en az 0,6 opak (beyaz gökte ve karda bulutu kenar seçtirir). Kare ilerledikçe yayılır ve
    seyrelir (son iki kare ömrün sönüm yarısında görünür)."""
    n = 16
    rng = np.random.default_rng(300 + i)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64) + 0.5
    sigma = 3.0 + 0.4 * i
    yogunluk = np.zeros((n, n))
    for _ in range(4):
        cx, cy = n / 2 + rng.uniform(-2.2, 2.2), n / 2 + rng.uniform(-1.8, 1.8)
        yogunluk += np.exp(-((xx - cx) ** 2 + (yy - cy) ** 2) / (2 * sigma ** 2))
    yogunluk /= yogunluk.max()
    esik = 0.22 + 0.03 * i
    ic = yogunluk > esik
    derin = np.clip((yogunluk - esik) / (1 - esik), 0, 1)
    alfa = np.where(ic, np.clip(0.6 + 0.4 * derin, 0, 1) * (1.0 - 0.12 * i), 0)
    t = np.clip((yogunluk - esik) / 0.3, 0, 1)[..., None]
    renk = np.array(BUHAR_KENAR) * (1 - t) + np.array(BUHAR_IC) * t
    img = np.zeros((n, n, 4), dtype=np.uint8)
    img[..., :3] = np.round(renk).astype(np.uint8)
    img[..., 3] = np.round(alfa * 255).astype(np.uint8)
    img[~ic] = 0
    return Image.fromarray(img, "RGBA")


# Düşen damla: koyu lacivert kontür (açık gökte ve kumda seçilir), parlak buz beyazı gövde ve beyaz parlama (koyu
# zeminde seçilir), üstte düşüş izi. 3: zeminde sıçrama.
TER_KARELERI = [
    ["   t    ",
     "   t    ",
     "   oo   ",
     "  ohLo  ",
     "  oLLo  ",
     "  oLLo  ",
     "   oo   ",
     "        "],
    ["   t    ",
     "   tt   ",
     "   tt   ",
     "   oo   ",
     "  ohLo  ",
     "  oLLo  ",
     "   oo   ",
     "        "],
    ["        ",
     "    t   ",
     "   oo   ",
     "  ohLo  ",
     " ohLLLo ",
     " oLLLLo ",
     "  oooo  ",
     "        "],
    ["        ",
     "        ",
     " h    h ",
     "  o  o  ",
     "        ",
     "  oooo  ",
     " oLhLLo ",
     "  oooo  "],
]
TER_PALET = {"o": (10, 30, 84, 255), "L": (196, 240, 255, 255), "h": (255, 255, 255, 255),
             "t": (214, 244, 255, 150)}


def ter_karesi(i):
    img = Image.new("RGBA", (8, 8), SEFFAF)
    piksel_sanati(img.load(), 0, 0, TER_KARELERI[i], TER_PALET)
    return img


def kaydet(img, yol):
    hedef = DOKU / yol
    hedef.parent.mkdir(parents=True, exist_ok=True)
    img.save(hedef, optimize=True)
    print(f"{hedef.relative_to(KOK)} {img.size[0]}x{img.size[1]}")


if __name__ == "__main__":
    kaydet(buz_dokusu(), "misc/sicaklik_buz.png")
    kaydet(bunalma_dokusu(), "misc/sicaklik_bunalma.png")
    kaydet(sicak_dokusu(), "misc/sicaklik_sicak.png")
    kaydet(islak_ikonu(), "mob_effect/islak.png")
    for k in range(4):
        kaydet(buhar_karesi(k), f"particle/nefes_buhari_{k}.png")
        kaydet(ter_karesi(k), f"particle/ter_damlasi_{k}.png")
