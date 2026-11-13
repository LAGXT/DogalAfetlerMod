#!/usr/bin/env python3
"""Kutup balıkçılığı ve soğutucu eşya dokuları (16x16, deterministik: aynı kod → aynı bayt).

Kullanım (kökünden): KOK/.venv/bin/python tools/assets/kutup_dokular.py   (önizleme için: ... --onizleme /tmp/kutup.png)

Çıktılar (src/main/resources/assets/dogalafetler/textures/item/):
  kutup_oltasi.png, kutup_oltasi_cast.png      Kutup Oltası (buz mavisi kristal; atılmış halinde misina yok, varlık çizicisi çizer)
  kristal_levrek / kutup_yilanbaligi / aurora_baligi (+ pismis_ önekli pişmiş halleri)   özel kutup balıkları
  buzlu_inci.png, tuz_kristali.png            buz altı/deniz eşyaları
  sogutma_parcasi / sogutma_kulcesi / sogutma_plakasi / serin_kolye / soguk_iksiri   soğutucu eşyalar
Varlık dokusu (src/main/resources/assets/dogalafetler/textures/entity/):
  sari_samandira.png                          nadir bir şey gelirken Kutup Oltası'nın sarı şamandırası (vanilla fishing_hook.png düzeni, 8x8)

Görsel dil vanilla'ya yakın: koyu dış çizgi (otomatik), sol üstten ışık. Soğutucular buz mavisi/gümüş, ısıtan
(Termostat Plakası) bakır-altının tersi paletidir.
"""
import math
import sys
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
ESYA = KOK / "src/main/resources/assets/dogalafetler/textures/item"
VARLIK = KOK / "src/main/resources/assets/dogalafetler/textures/entity"
N = 16


def hx(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def karistir(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def koyulastir(c, f):
    return (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])


class Tuval:
    def __init__(self):
        self.p = [[None] * N for _ in range(N)]

    def koy(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.p[y][x] = c

    def al(self, x, y):
        return self.p[y][x] if 0 <= x < N and 0 <= y < N else None

    def dolgu(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.koy(x, y, c)

    def cizgi(self, x0, y0, x1, y1, renkler):
        """Bresenham; renkler listesi çizgi boyunca döngüsel kullanılır."""
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        hata = dx + dy
        i = 0
        while True:
            self.koy(x0, y0, renkler[i % len(renkler)])
            i += 1
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * hata
            if e2 >= dy:
                hata += dy
                x0 += sx
            if e2 <= dx:
                hata += dx
                y0 += sy

    def poligon(self, noktalar, acik, koyu, orta=None):
        """Çokgen (tek-çift kuralı); sol yarı açık, sağ yarı koyu, üst kenar bir ton açık."""
        xs = [p[0] for p in noktalar]
        ys = [p[1] for p in noktalar]
        mx = (min(xs) + max(xs)) / 2.0
        for y in range(N):
            for x in range(N):
                if not _icinde(x + 0.5, y + 0.5, noktalar):
                    continue
                c = acik if x + 0.5 < mx - 0.2 else (koyu if x + 0.5 > mx + 0.2 else (orta or karistir(acik, koyu, 0.5)))
                ust = not _icinde(x + 0.5, y - 0.5, noktalar)
                if ust:
                    c = karistir(c, hx("#ffffff"), 0.35)
                self.koy(x, y, c)

    def kenar(self, f=0.42):
        """Dolu piksellerin boş 4-komşulu piksellerine koyu dış çizgi (komşu rengin koyusu)."""
        yeni = {}
        for y in range(N):
            for x in range(N):
                if self.p[y][x] is not None:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    k = self.al(x + dx, y + dy)
                    if k is not None:
                        yeni[(x, y)] = koyulastir(k, f)
                        break
        for (x, y), c in yeni.items():
            self.p[y][x] = c

    def kaydet(self, ad):
        img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        px = img.load()
        for y in range(N):
            for x in range(N):
                if self.p[y][x] is not None:
                    px[x, y] = self.p[y][x]
        yol = ESYA / f"{ad}.png"
        yol.parent.mkdir(parents=True, exist_ok=True)
        img.save(yol, optimize=True)
        print(yol.relative_to(KOK))
        return img


def _icinde(px, py, nokta):
    icerde = False
    j = len(nokta) - 1
    for i in range(len(nokta)):
        xi, yi = nokta[i]
        xj, yj = nokta[j]
        if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
            icerde = not icerde
        j = i
    return icerde


# ----------------------------------------------------------------------------------------------------------------
# Olta
# ----------------------------------------------------------------------------------------------------------------
# K-E77: Buzlu Olta, vanilla olta dokusunun BİREBİR piksel maskesi üzerine yeniden renklendirilir (yalnız renk değişir, şekil değil).
# Sebep: şamandıra misinasının birinci şahıstaki bağlanma noktası oltanın modelinden hesaplanır (Punchy: model köşelerinden uç
# ortalaması; vanilla: sabit ekran noktası). Önceki serbest çizim vanilla'dan farklı bir uç/gövde şekli verdiği için ip oltanın
# ucuna oturmuyordu. Harf = vanilla rengi: F beyaz tel boncuğu, a/b makara, c/d tel ve uç, e/f/i gövde (açıktan koyuya), j gövdenin
# koyu kenarı, g/h kenar çizgisi. Atılmış dokuda misina ve şamandıra YOKTUR (varlık çizicisi çizer).
OLTA_MASKE = [
    "................",
    ".............dg.",
    "............dij.",
    "...........gifj.",
    "..........giejg.",
    ".........giej.g.",
    "........giej..d.",
    ".......giej...d.",
    "......gifj....c.",
    ".....gifj.....d.",
    ".....ifj.....d..",
    "....ifaah....g..",
    "...iijbah...g...",
    "..iij.hh..F.F...",
    "..jj......bF....",
    "................",
]
OLTA_RENK = {
    "F": "#f4fdff", "a": "#d4f0fb", "b": "#8cc6e4", "c": "#7fc3e6", "d": "#6db4dd",
    "e": "#e6fbff", "f": "#9ddcf6", "g": "#1f4a7a", "h": "#2d5f94", "i": "#4aa3dd", "j": "#173e6a",
}


# Vanilla atılmamış dokunun atılmış dokudan fazla piksellerinin tam listesi (tel ve boncuk).
OLTA_TEL = {(14, 4), (14, 5), (14, 6), (14, 7), (14, 8), (14, 9), (13, 10), (13, 11), (12, 12), (10, 13), (12, 13), (10, 14),
            (11, 14)}

def olta(atilmis):
    """Buzlu Olta: mavi-beyaz kristal gövde, lacivert kenar, gümüş-buz makara; şekil vanilla oltayla aynı."""
    t = Tuval()
    for y, satir in enumerate(OLTA_MASKE):
        for x, k in enumerate(satir):
            if k == ".":
                continue
            if atilmis and (x, y) in OLTA_TEL:
                continue  # vanilla atılmış doku: tel ve boncuk pikselleri yok (misina varlık çizicisinde)
            t.koy(x, y, hx(OLTA_RENK[k]))
    return t


# ----------------------------------------------------------------------------------------------------------------
# Balıklar
# ----------------------------------------------------------------------------------------------------------------
PALETLER = {
    "kristal_levrek": {
        "cig": dict(govde="#7fc8e8", acik="#d8f6ff", koyu="#3d86ad", yuzgec="#a9e4f7", karin="#eafcff", goz="#103040",
                    parilti="#ffffff"),
        "pismis": dict(govde="#c88a4a", acik="#e8b36f", koyu="#8a5528", yuzgec="#d99a58", karin="#f0cc94", goz="#2a1608",
                       parilti="#ffe9b0"),
    },
    "kutup_yilanbaligi": {
        "cig": dict(govde="#3f8f95", acik="#7fd0d0", koyu="#245a66", yuzgec="#2f7078", karin="#cfe9e4", goz="#0c2028",
                    parilti="#d8ffff"),
        "pismis": dict(govde="#9a6a3a", acik="#c99a62", koyu="#5c3a1c", yuzgec="#7a4f28", karin="#e5c69a", goz="#1c1006",
                       parilti="#ffe0a0"),
    },
    "aurora_baligi": {
        "cig": dict(govde="#4ff0b0", govde2="#9a6bff", acik="#c9fff0", koyu="#2a6aa0", yuzgec="#7be6ff", yuzgec2="#d49bff",
                    karin="#f1fff9", goz="#10203a", parilti="#ffffff"),
        "pismis": dict(govde="#e0a85a", govde2="#b0703a", acik="#f2cf8a", koyu="#7a4824", yuzgec="#d9a050",
                       yuzgec2="#a86a30", karin="#f6dcae", goz="#24120a", parilti="#ffeaa0"),
    },
}


def elips_balik(p, levrek):
    """Sola bakan gövdeli balık: elips gövde, üçgen kuyruk, sırt ve karın yüzgeci, göz. levrek=True: kristal faset deseni."""
    t = Tuval()
    govde, acik, koyu = hx(p["govde"]), hx(p["acik"]), hx(p["koyu"])
    yuzgec, karin, goz = hx(p["yuzgec"]), hx(p["karin"]), hx(p["goz"])
    govde2 = hx(p["govde2"]) if "govde2" in p else None
    yuzgec2 = hx(p["yuzgec2"]) if "yuzgec2" in p else yuzgec
    cx, cy, rx, ry = 6.6, 8.0, 5.4, 3.3
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            if dx * dx + dy * dy > 1.0:
                continue
            u = (x - 1.0) / 11.0
            c = govde if govde2 is None else karistir(govde, govde2, max(0.0, min(1.0, u)))
            isik = -(dx * 0.5 + dy * 0.9)
            if isik > 0.55:
                c = karistir(c, acik, 0.6)
            elif isik < -0.35:
                c = karistir(c, koyu, 0.55)
            if dy > 0.35:
                c = karistir(c, karin, 0.75)
            if levrek and (x + y) % 4 == 0 and dy < 0.35:
                c = karistir(c, acik, 0.55)
            t.koy(x, y, c)
    # Kuyruk (sağda): iki dişli üçgen.
    for i, y in enumerate(range(5, 12)):
        genislik = 1 + abs(y - 8) * 0.6
        for x in range(int(11.2), min(15, int(11.2 + genislik + 1.6))):
            t.koy(x, y, yuzgec if (x + y) % 2 == 0 else yuzgec2)
    for y in (7, 8, 9):
        t.koy(11, y, karistir(govde, yuzgec, 0.5))
    # Sırt yüzgeci ve karın yüzgeci.
    for x, h in ((5, 1), (6, 2), (7, 2), (8, 1)):
        for k in range(h):
            t.koy(x, 4 - k, yuzgec if k == 0 else yuzgec2)
    for x in (6, 7):
        t.koy(x, 11, yuzgec)
    t.koy(3, 7, goz)
    t.kenar()
    t.koy(4, 9, hx(p["parilti"]))
    return t


def yilanbaligi(p):
    """Kıvrımlı uzun gövde (sinüs), sırt boyunca yüzgeç şeridi, solda baş."""
    t = Tuval()
    govde, acik, koyu = hx(p["govde"]), hx(p["acik"]), hx(p["koyu"])
    yuzgec, karin, goz = hx(p["yuzgec"]), hx(p["karin"]), hx(p["goz"])
    for x in range(2, 15):
        yc = 8.0 + 3.0 * math.sin((x - 2) * 0.62)
        y0 = int(math.floor(yc - 1.0))
        t.koy(x, y0 - 1, yuzgec)
        t.koy(x, y0, acik if x % 3 else govde)
        t.koy(x, y0 + 1, govde if x % 4 else koyu)
        t.koy(x, y0 + 2, karin)
    # Baş biraz kalın.
    yc = 8.0
    for y in (6, 7, 8, 9, 10):
        t.koy(2, y, govde if y < 9 else karin)
    t.koy(2, 5, acik)
    t.koy(3, 7, goz)
    t.kenar()
    t.koy(7, 6, hx(p["parilti"]))
    t.koy(11, 10, hx(p["parilti"]))
    return t


# ----------------------------------------------------------------------------------------------------------------
# Eşyalar
# ----------------------------------------------------------------------------------------------------------------
def buzlu_inci():
    t = Tuval()
    cx, cy, r = 7.5, 8.5, 4.6
    kenar = hx("#5fa8cc")
    orta = hx("#bfe6f5")
    parlak = hx("#f4ffff")
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d > r:
                continue
            # Sol üstten ışık: parlak nokta merkezden sol üste kaymış.
            h = math.hypot(x + 0.5 - (cx - 1.6), y + 0.5 - (cy - 1.8))
            c = karistir(orta, kenar, max(0.0, min(1.0, (d / r) ** 2)))
            c = karistir(c, parlak, max(0.0, 1.0 - h / 3.2) * 0.9)
            t.koy(x, y, c)
    # Buz kristali uçları (dört yöne).
    for x, y in ((7, 2), (8, 2), (7, 1)):
        t.koy(x, y, hx("#e8ffff"))
    for x, y in ((12, 8), (13, 8), (13, 7)):
        t.koy(x, y, hx("#bfe6f5"))
    for x, y in ((2, 9), (1, 9), (1, 10)):
        t.koy(x, y, hx("#9fd8f0"))
    t.kenar()
    t.koy(5, 6, hx("#ffffff"))
    t.koy(12, 3, hx("#ffffff"))
    t.koy(3, 12, hx("#d8f6ff"))
    return t


def tuz_kristali():
    t = Tuval()
    acik, koyu = hx("#f2f2f6"), hx("#b9bccf")
    t.poligon([(2, 13), (3, 8), (5, 4), (7, 8), (7, 13)], acik, koyu)
    t.poligon([(7, 13), (7, 6), (9, 2), (12, 6), (12, 13)], hx("#ffffff"), hx("#c7c9da"))
    t.poligon([(11, 13), (11, 9), (13, 7), (14, 10), (14, 13)], hx("#e6e7f2"), hx("#a9acc2"))
    t.kenar()
    t.koy(8, 4, hx("#ffffff"))
    t.koy(4, 8, hx("#ffffff"))
    # Pembemsi tuz damarı.
    t.koy(9, 9, hx("#e9c7d2"))
    t.koy(9, 10, hx("#e9c7d2"))
    return t


def sogutma_parcasi():
    t = Tuval()
    t.poligon([(3, 12), (2, 7), (5, 3), (8, 2), (11, 5), (13, 9), (11, 13), (7, 13)], hx("#a9e4f7"), hx("#4c8fb5"))
    # Kristal yüzey çizgileri ve parıltı.
    t.cizgi(5, 5, 8, 11, [hx("#d8f6ff")])
    t.cizgi(9, 4, 10, 10, [hx("#3d7aa3")])
    t.kenar()
    t.koy(6, 4, hx("#ffffff"))
    t.koy(4, 8, hx("#e8ffff"))
    t.koy(12, 4, hx("#ffffff"))
    return t


def sogutma_kulcesi():
    t = Tuval()
    t.poligon([(2, 8), (8, 5), (14, 7), (8, 10)], hx("#d8f6ff"), hx("#a9e4f7"))
    t.poligon([(2, 8), (8, 10), (8, 13), (2, 11)], hx("#6fb6d8"), hx("#6fb6d8"))
    t.poligon([(8, 10), (14, 7), (14, 10), (8, 13)], hx("#4c8fb5"), hx("#4c8fb5"))
    t.kenar()
    t.cizgi(3, 8, 8, 6, [hx("#ffffff")])
    t.koy(5, 10, hx("#a9e4f7"))
    t.koy(11, 8, hx("#d8f6ff"))
    return t


def sogutma_plakasi():
    t = Tuval()
    taban = hx("#7fb3d1")
    t.dolgu(2, 2, 13, 13, taban)
    for i in range(2, 14):
        t.koy(i, 2, hx("#d8f6ff"))
        t.koy(2, i, hx("#cfeefa"))
        t.koy(i, 13, hx("#3b6f94"))
        t.koy(13, i, hx("#4a80a8"))
    t.koy(13, 2, hx("#a9e4f7"))
    t.koy(2, 13, hx("#5a93b8"))
    # Devre izleri (merkeze).
    iz = hx("#2a5a85")
    for x, y in ((4, 4), (5, 4), (6, 4), (6, 5), (6, 6), (11, 4), (10, 4), (9, 4), (9, 5), (9, 6),
                 (4, 11), (5, 11), (6, 11), (6, 10), (6, 9), (11, 11), (10, 11), (9, 11), (9, 10), (9, 9)):
        t.koy(x, y, iz)
    # Merkez buz kristali (elmas).
    for x, y, c in ((7, 6, "#e8ffff"), (8, 6, "#a9e4f7"), (6, 7, "#d8f6ff"), (7, 7, "#ffffff"), (8, 7, "#bfe6f5"),
                    (9, 7, "#6fb6d8"), (6, 8, "#a9e4f7"), (7, 8, "#d8f6ff"), (8, 8, "#9fd8f0"), (9, 8, "#4c8fb5"),
                    (7, 9, "#6fb6d8"), (8, 9, "#4c8fb5")):
        t.koy(x, y, hx(c))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.koy(x, y, hx("#e8ffff"))
    t.kenar()
    return t


def serin_kolye():
    t = Tuval()
    zincir = [hx("#d6e0e6"), hx("#8ea2ad")]
    # Parabolik zincir (2,2) -> (13,2), tepe noktası y=9.
    onceki = None
    for x in range(2, 14):
        y = int(round(2 + 7.0 * (1 - ((x - 7.5) / 5.5) ** 2)))
        t.koy(x, y, zincir[x % 2])
        if onceki is not None and abs(y - onceki) > 1:
            for k in range(min(y, onceki) + 1, max(y, onceki)):
                t.koy(x if y > onceki else x - 1, k, zincir[k % 2])
        onceki = y
    # Zincir kenarlarını koyu çizgiyle çevreleme: yalnız zincir çizilmişken kenar.
    t.kenar(0.5)
    # Sarkıt: buzlu inci + kristal uç.
    cx, cy, r = 7.5, 11.5, 2.4
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d > r:
                continue
            h = math.hypot(x + 0.5 - (cx - 0.8), y + 0.5 - (cy - 0.9))
            c = karistir(hx("#bfe6f5"), hx("#5fa8cc"), min(1.0, (d / r) ** 2))
            c = karistir(c, hx("#ffffff"), max(0.0, 1.0 - h / 1.8) * 0.9)
            t.koy(x, y, c)
    t.koy(7, 8, hx("#e8ffff"))
    t.koy(8, 8, hx("#a9e4f7"))
    t.koy(7, 9, hx("#9fd8f0"))
    t.koy(8, 9, hx("#6fb6d8"))
    t.kenar(0.45)
    return t


def soguk_iksiri():
    t = Tuval()
    cam = hx("#dff3f8", 190)
    # Mantar tıpa ve boyun.
    t.dolgu(6, 1, 9, 2, hx("#a1763f"))
    t.koy(6, 1, hx("#c29557"))
    t.koy(7, 1, hx("#c29557"))
    t.dolgu(7, 3, 8, 4, cam)
    # Gövde: yuvarlak karın.
    satirlar = {5: (5, 10), 6: (4, 11), 7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (4, 11),
                13: (5, 10)}
    for y, (a, b) in satirlar.items():
        for x in range(a, b + 1):
            if y < 7:
                t.koy(x, y, cam)
            else:
                derin = (y - 7) / 6.0
                c = karistir(hx("#8fe3ff"), hx("#2f98d8"), derin)
                if x >= 10:
                    c = karistir(c, hx("#1f6aa8"), 0.35)
                t.koy(x, y, c)
    # Sıvı yüzeyi parlak, cam parıltısı, kırağı.
    for x in range(3, 13):
        t.koy(x, 7, hx("#d8f6ff"))
    t.koy(4, 8, hx("#ffffff"))
    t.koy(4, 9, hx("#e8ffff"))
    t.koy(5, 6, hx("#ffffff"))
    for x, y in ((8, 10), (7, 11), (9, 11), (8, 12), (8, 11)):
        t.koy(x, y, hx("#ffffff"))
    t.kenar(0.35)
    return t


def sari_samandira():
    """8x8 şamandıra dokusu; vanilla fishing_hook.png ile aynı piksel düzeni (UV'ler aynı kalır), kırmızı gövde sarı."""
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    gri_a, gri_k = hx("#d3d3d3"), hx("#919191")
    sari_a, sari_k = hx("#ffd21f"), hx("#c78f00")
    for x, y, c in ((3, 1, gri_a), (4, 1, gri_a), (5, 1, gri_k),
                    (3, 2, sari_a), (4, 2, sari_a), (5, 2, sari_k),
                    (3, 3, gri_a), (4, 3, gri_k), (5, 3, gri_k)):
        px[x, y] = c
    # Alttaki bağlantı (vanilla'da siyah): misina ucu ve kanca.
    for x, y in ((4, 4), (4, 5), (4, 6), (2, 6), (3, 7), (4, 7)):
        px[x, y] = hx("#000000")
    yol = VARLIK / "sari_samandira.png"
    yol.parent.mkdir(parents=True, exist_ok=True)
    img.save(yol, optimize=True)
    print(yol.relative_to(KOK))
    return img


def olustur():
    sonuc = {}
    sonuc["kutup_oltasi"] = olta(False).kaydet("kutup_oltasi")
    sonuc["kutup_oltasi_cast"] = olta(True).kaydet("kutup_oltasi_cast")
    sari_samandira()
    for ad, p in PALETLER.items():
        for tur, onek in (("cig", ""), ("pismis", "pismis_")):
            if ad == "kutup_yilanbaligi":
                t = yilanbaligi(p[tur])
            else:
                t = elips_balik(p[tur], levrek=(ad == "kristal_levrek"))
            if tur == "pismis":
                # Izgara izleri: gövdeye iki koyu çapraz çizgi.
                for x, y in ((5, 7), (6, 8), (8, 7), (9, 8)):
                    c = t.al(x, y)
                    if c is not None and ad != "kutup_yilanbaligi":
                        t.koy(x, y, koyulastir(c, 0.7))
            sonuc[onek + ad] = t.kaydet(onek + ad)
    sonuc["buzlu_inci"] = buzlu_inci().kaydet("buzlu_inci")
    sonuc["tuz_kristali"] = tuz_kristali().kaydet("tuz_kristali")
    sonuc["sogutma_parcasi"] = sogutma_parcasi().kaydet("sogutma_parcasi")
    sonuc["sogutma_kulcesi"] = sogutma_kulcesi().kaydet("sogutma_kulcesi")
    sonuc["sogutma_plakasi"] = sogutma_plakasi().kaydet("sogutma_plakasi")
    sonuc["serin_kolye"] = serin_kolye().kaydet("serin_kolye")
    sonuc["soguk_iksiri"] = soguk_iksiri().kaydet("soguk_iksiri")
    return sonuc


def onizleme(sonuc, yol):
    """Tüm dokuları 8x büyütülmüş, damalı zeminli tek şeritte yazar (yalnız göz kontrolü için; depoya girmez)."""
    ad_sayisi = len(sonuc)
    sutun = 8
    satir = (ad_sayisi + sutun - 1) // sutun
    olcek = 8
    sayfa = Image.new("RGBA", (sutun * N * olcek, satir * N * olcek), (70, 70, 80, 255))
    for i, img in enumerate(sonuc.values()):
        buyuk = img.resize((N * olcek, N * olcek), Image.NEAREST)
        sayfa.alpha_composite(buyuk, ((i % sutun) * N * olcek, (i // sutun) * N * olcek))
    sayfa.save(yol)
    print("önizleme:", yol)


if __name__ == "__main__":
    dokular = olustur()
    if "--onizleme" in sys.argv:
        onizleme(dokular, sys.argv[sys.argv.index("--onizleme") + 1])
