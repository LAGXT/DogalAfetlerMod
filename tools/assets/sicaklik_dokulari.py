"""Sıcaklık sisteminin dokularını üretir (madde 7c, 7d; S3-07; docs/tasarim/sicaklik.md).

Çıktılar (src/main/resources/assets/dogalafetler/textures/):
  gui/sicaklik/hud.png        256x64  HUD atlası (client/sicaklik/SicaklikHud, HudCizimi):
                                      (0,0) soğuk tüp 74x5, (0,5) dolgusu (hap kenarında buz -> dışta koyu mavi);
                                      (80,0) sıcak tüp 74x5, (80,5) dolgusu (hap kenarında turuncu -> dışta koyu kırmızı);
                                      v=16 haplar 34x11 (u = 36 * sıra: normal, üşüme, donma, bunalma, yanma; 180 beyaz);
                                      v=32 ikonlar 9x9 (0 boş damla, 9 dolu damla, 18 üşüme, 27 donma, 36 bunalma,
                                      45 yanma, 54 normal termometre); v=48 şevron 5x4 (0 aşağı, 6 yukarı), (12,48) boş
                                      üçgen 5x3
  misc/sicaklik_buz.png       256x256 buz kenarı (alfa): kenarlardan içeri dallanan buz kristalleri + kırağı
  misc/sicaklik_bunalma.png   256x256 buz kenarının kırmızı karşılığı: kenarlardan içeri süzülen ter izleri + ısı lekeleri
  misc/sicaklik_sicak.png     256x256 sıcak hava vinyeti; dikey yönde kesintisiz (efekt onu yukarı kaydırır)
  mob_effect/islak.png        18x18   Islak gösterge efekti: su damlası
  particle/nefes_buhari_0..3  8x8     nefes buharı bulutçuğu (yaşla dağılır)
  particle/ter_damlasi_0..3   8x8     ter damlası (0-2 düşen damla çeşitleri, 3 zeminde sıçrama)

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
# HudCizimi.renk ile aynı sıra ve renk: normal, üşüme, donma, bunalma, yanma.
DURUM_RENK = [(232, 230, 222), (166, 221, 248), (94, 174, 255), (255, 181, 74), (255, 90, 54)]
# Tüp dolgusu: konum 0 = hapın kenarı (nötre yakın), 1 = ölçek ucu (±100).
SOGUK_DURAK = [(0.0, (200, 240, 255)), (0.35, (122, 198, 246)), (0.7, (52, 118, 214)), (1.0, (28, 48, 140))]
SICAK_DURAK = [(0.0, (255, 224, 132)), (0.35, (248, 168, 64)), (0.7, (230, 92, 40)), (1.0, (146, 22, 20))]
CERCEVE = (16, 17, 22, 235)
CAM = (34, 40, 54, 165)
CAM_UST = (58, 66, 84, 165)


def gecis(duraklar, f):
    for (a, ca), (b, cb) in zip(duraklar, duraklar[1:]):
        if f <= b:
            t = (f - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))
    return duraklar[-1][1]


def olcekle(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def karistir(c, d, t):
    return tuple(round(c[i] + (d[i] - c[i]) * t) for i in range(3))


def tup(px, u0, sol_dis):
    """74x5 boş cam tüp; dış ucu yuvarlatılmış, hap tarafı düz."""
    for x in range(74):
        for y in range(5):
            kenar = x in (0, 73) or y in (0, 4)
            px[u0 + x, y] = CERCEVE if kenar else (CAM_UST if y == 1 else CAM)
    dis = 0 if sol_dis else 73
    px[u0 + dis, 0] = SEFFAF
    px[u0 + dis, 4] = SEFFAF


def dolgu(px, u0, sol_dis, duraklar):
    """Tüpün içi (72x3, v=6..8): renk hap kenarından dışa doğru koyulaşır; üst satır parlak, alt satır gölgeli."""
    for i in range(72):
        c = gecis(duraklar, i / 71)
        x = u0 + (72 - i if sol_dis else 1 + i)
        px[x, 6] = olcekle(c, 1.2) + (255,)
        px[x, 7] = c + (255,)
        px[x, 8] = olcekle(c, 0.8) + (255,)


def hap_icinde(x, y):
    girinti = 2 if y in (0, 10) else 1 if y in (1, 9) else 0
    return 0 <= y < 11 and girinti <= x < 34 - girinti


def hap_kenari(x, y):
    return hap_icinde(x, y) and not all(hap_icinde(x + a, y + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def hap(px, u0, renk):
    """Koyu cam hap, durum renginde çerçeve; iç hafifçe durum rengine boyalı, tepeye doğru açılır."""
    taban = karistir((20, 22, 28), renk, 0.16)
    for y in range(11):
        ic = olcekle(taban, 1.25 - 0.05 * y)
        if y == 1:
            ic = karistir(ic, (255, 255, 255), 0.14)
        for x in range(34):
            if hap_kenari(x, y):
                px[u0 + x, 16 + y] = (olcekle(renk, 0.8) if y >= 6 else renk) + (255,)
            elif hap_icinde(x, y):
                px[u0 + x, 16 + y] = ic + (228,)


def beyaz_hap(px, u0):
    """Parlama/nabız katmanı: çerçeve tam, iç yarı saydam (sayı okunur kalsın)."""
    for y in range(11):
        for x in range(34):
            if hap_kenari(x, y):
                px[u0 + x, 16 + y] = (255, 255, 255, 255)
            elif hap_icinde(x, y):
                px[u0 + x, 16 + y] = (255, 255, 255, 110)


def piksel_sanati(px, u0, v0, satirlar, palet):
    for y, satir in enumerate(satirlar):
        for x, k in enumerate(satir):
            if k != " ":
                px[u0 + x, v0 + y] = palet[k]


DAMLA = ["    o    ",
         "   o_o   ",
         "  o___o  ",
         " o_____o ",
         "o_h_____o",
         "o_h_____o",
         "o_______o",
         " o_____o ",
         "  ooooo  "]
IKONLAR = {
    0: (DAMLA, {"o": (29, 90, 140, 255), "_": (110, 170, 225, 70), "h": (110, 170, 225, 70)}),
    9: (DAMLA, {"o": (29, 90, 140, 255), "_": (70, 150, 255, 255), "h": (210, 238, 255, 255)}),
    18: (["         ",
          "  ooo    ",
          " o...oo  ",
          "o......o ",
          "o..h....o",
          " o......o",
          "  oooooo ",
          "    .  . ",
          "  .      "],
         {"o": (84, 146, 198, 255), ".": (226, 243, 255, 255), "h": (255, 255, 255, 255)}),
    27: (["    o    ",
          " d  o  d ",
          "  d o d  ",
          "   ooo   ",
          "ooooWoooo",
          "   ooo   ",
          "  d o d  ",
          " d  o  d ",
          "    o    "],
         {"o": (130, 200, 255, 255), "d": (70, 140, 230, 255), "W": (255, 255, 255, 255)}),
    36: (["    o   w",
          "   o_o w ",
          "  o___o w",
          " o_____o ",
          "o_h_____o",
          "o_h_____o",
          "o_______o",
          " o_____o ",
          "  ooooo  "],
         {"o": (150, 78, 12, 255), "_": (255, 180, 70, 255), "h": (255, 236, 170, 255),
          "w": (255, 214, 120, 200)}),
    45: (["    r    ",
          "   rr    ",
          "   rOr r ",
          "  rOOr rr",
          " rOOYOrOr",
          " rOYYYOOr",
          "rOYYWYYOr",
          " rOYYYOr ",
          "  rrrrr  "],
         {"r": (170, 30, 10, 255), "O": (255, 110, 30, 255), "Y": (255, 200, 60, 255),
          "W": (255, 250, 210, 255)}),
    54: (["   ooo   ",
          "   o.o   ",
          "   o.o   ",
          "   oRo   ",
          "   oRo   ",
          "  oRRRo  ",
          " oRRhRRo ",
          " oRRRRRo ",
          "  ooooo  "],
         {"o": (60, 62, 72, 255), ".": (236, 236, 242, 255), "R": (232, 64, 52, 255), "h": (255, 170, 160, 255)}),
}
SEVRON_ASAGI = ["B   B",
                "BB BB",
                " BDB ",
                "  D  "]
SEVRON_YUKARI = ["  L  ",
                 " LOL ",
                 "OO OO",
                 "O   O"]
UCGEN = ["  w  ",
         " w w ",
         "wwwww"]


def hud_dokusu():
    img = Image.new("RGBA", (256, 64), SEFFAF)
    px = img.load()
    tup(px, 0, True)
    dolgu(px, 0, True, SOGUK_DURAK)
    tup(px, 80, False)
    dolgu(px, 80, False, SICAK_DURAK)
    for i, renk in enumerate(DURUM_RENK):
        hap(px, i * 36, renk)
    beyaz_hap(px, 180)
    for u, (satirlar, palet) in IKONLAR.items():
        piksel_sanati(px, u, 32, satirlar, palet)
    piksel_sanati(px, 0, 48, SEVRON_ASAGI, {"B": (124, 198, 255, 255), "D": (64, 134, 224, 255)})
    piksel_sanati(px, 6, 48, SEVRON_YUKARI, {"O": (255, 168, 64, 255), "L": (255, 222, 140, 255)})
    piksel_sanati(px, 12, 48, UCGEN, {"w": (245, 245, 245, 235)})
    return img


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
    """Aşağı süzülen ter izi: hafif kıvrılarak iner, ucunda parlak bir damla birikir."""
    noktalar = [(x, y)]
    adim = 6
    for i in range(1, int(uzunluk / adim) + 1):
        x += rnd.uniform(-1.6, 1.6)
        noktalar.append((x, y + i * adim))
    for (x1, y1), (x2, y2) in zip(noktalar, noktalar[1:]):
        t = y1 / 256
        d.line([(x1, y1), (x2, y2)], fill=(232, 52, 34, int(170 - 60 * t)), width=kalinlik)
    ux, uy = noktalar[-1]
    r = kalinlik + 1.2
    d.ellipse([ux - r, uy - r * 0.8, ux + r, uy + r * 1.3], fill=(255, 96, 70, 215))
    d.ellipse([ux - r * 0.45, uy - r * 0.5, ux + r * 0.05, uy], fill=(255, 196, 170, 200))


def bunalma_dokusu():
    n = 256
    rnd = random.Random(20261108)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    dx = (xx - (n - 1) / 2) / (n / 2)
    dy = (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx ** 2 * 0.9 + dy ** 2 * 1.1)
    kenar = np.clip((r - 0.6) / 0.5, 0, 1) ** 1.5
    # Isı lekeleri: 16x16 gürültü yumuşakça büyütülür, kenar kızarması lekeli olur.
    leke = np.random.default_rng(11).random((16, 16))
    leke = np.array(Image.fromarray((leke * 255).astype(np.uint8), "L").resize((n, n), Image.BICUBIC)) / 255.0
    taban = np.zeros((n, n, 4), dtype=np.uint8)
    taban[..., 0] = np.clip(150 + 70 * leke, 0, 255).astype(np.uint8)
    taban[..., 1] = (14 + 20 * leke).astype(np.uint8)
    taban[..., 2] = 8
    taban[..., 3] = (np.clip(kenar * (0.5 + 0.5 * leke), 0, 1) * 185).astype(np.uint8)
    img = Image.fromarray(taban, "RGBA")
    iz = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(iz)
    # Üst kenardan süzülen izler.
    for _ in range(24):
        ter_izi(d, rnd, rnd.uniform(0, n), -2, rnd.uniform(26, 84), rnd.choice((1, 1, 2)))
    # Yan kenarlarda içeri doğru kısa, aşağı inen izler.
    for _ in range(18):
        sol = rnd.random() < 0.5
        x = rnd.uniform(1, 18) if sol else n - 1 - rnd.uniform(1, 18)
        ter_izi(d, rnd, x, rnd.uniform(0, n - 40), rnd.uniform(14, 46), 1)
    # Alt kenardan yükselen soluk ısı kıvrımları.
    for _ in range(14):
        x0 = rnd.uniform(0, n)
        boy = rnd.uniform(20, 50)
        faz = rnd.uniform(0, 2 * math.pi)
        noktalar = [(x0 + 3 * math.sin(faz + t / 6), n + 1 - t) for t in range(0, int(boy), 2)]
        d.line(noktalar, fill=(255, 120, 60, 90), width=1)
    iz = iz.filter(ImageFilter.GaussianBlur(0.5))
    img = Image.alpha_composite(img, iz)
    a = np.array(img).astype(np.float64)
    # Ekranın ortası açık kalır.
    a[..., 3] *= np.clip((r - 0.32) / 0.45, 0, 1)
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


def buhar_karesi(i):
    """8x8 yumuşak buhar bulutçuğu; kare ilerledikçe yayılır ve seyrelir."""
    rng = np.random.default_rng(300 + i)
    yy, xx = np.mgrid[0:8, 0:8].astype(np.float64) + 0.5
    sigma = 1.5 + 0.35 * i
    yogunluk = np.zeros((8, 8))
    for _ in range(3):
        cx, cy = 4 + rng.uniform(-0.9, 0.9), 4 + rng.uniform(-0.9, 0.9)
        yogunluk += np.exp(-((xx - cx) ** 2 + (yy - cy) ** 2) / (2 * sigma ** 2))
    yogunluk /= yogunluk.max()
    alfa = np.clip(yogunluk * (1.0 - 0.12 * i) * (0.85 + 0.15 * rng.random((8, 8))), 0, 1)
    img = np.zeros((8, 8, 4), dtype=np.uint8)
    img[..., 0] = 255
    img[..., 1] = 255
    img[..., 2] = 255
    img[..., 3] = (alfa * 235).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


TER_KARELERI = [
    ["        ",
     "   o    ",
     "  oho   ",
     "  o_o   ",
     "  ooo   ",
     "        ",
     "        ",
     "        "],
    ["        ",
     "   o    ",
     "   o    ",
     "  oho   ",
     "  o_o   ",
     "  o_o   ",
     "   o    ",
     "        "],
    ["        ",
     "        ",
     "   oo   ",
     "  oh_o  ",
     "  o__o  ",
     "   oo   ",
     "        ",
     "        "],
    ["        ",
     "        ",
     "        ",
     " o    o ",
     "        ",
     "  oooo  ",
     " o_hh_o ",
     "  oooo  "],
]
TER_PALET = {"o": (150, 196, 236, 235), "_": (214, 236, 255, 225), "h": (255, 255, 255, 255)}


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
    kaydet(hud_dokusu(), "gui/sicaklik/hud.png")
    kaydet(buz_dokusu(), "misc/sicaklik_buz.png")
    kaydet(bunalma_dokusu(), "misc/sicaklik_bunalma.png")
    kaydet(sicak_dokusu(), "misc/sicaklik_sicak.png")
    kaydet(islak_ikonu(), "mob_effect/islak.png")
    for k in range(4):
        kaydet(buhar_karesi(k), f"particle/nefes_buhari_{k}.png")
        kaydet(ter_karesi(k), f"particle/ter_damlasi_{k}.png")
