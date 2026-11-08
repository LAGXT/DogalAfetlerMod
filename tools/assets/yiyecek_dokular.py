"""Sıcaklık yiyeceklerinin ve Buz Kovası'nın dokularını üretir (madde 14, S3-05, S3-06; docs/tasarim/esyalar.md "Sıcaklık yiyecekleri").

Çıktılar (src/main/resources/assets/dogalafetler/textures/):
  item/<ad>.png          16x16  madde 14'ün dokuz yiyeceği: şişeler (salep, çikolata, çay, ayran, şerbet), kaseler (tavuk
                                çorbası, ateşli güveç, kutup balığı çorbası), buzlu karpuz dilimi; sıcaklık-3'ün on iki yiyeceği:
                                şişeler (soğuk elma suyu, soğuk komposto, buzlu kakao, elma çayı), kaseler (maraş dondurması, kar
                                helvası, parıltılı sorbe, tarhana, kuzu haşlama, bal kabağı çorbası, sucuklu yumurta), fırında elma;
                                buz kovası (vanilla kova silueti, ağzından taşan buz blokları)
  mob_effect/<ad>.png    18x18  "İçten Isınma" (alev) ve "Serinlik" (kar tanesi) durum efekti ikonları

Görsel dil vanilla'ya yakın: vanilla iksir şişesi ve kase silueti, koyu kenar çizgisi, sol üstten ışık. Sıcaklar buhar
(ve güveçte alev dili), soğuklar kırağı ve buz parlaması taşır: envanterde bir bakışta ısıtan/serinleten ayrılır. Kutup
balığı çorbası kendi kasesini çizer (parlayan ön ağız, damarlı gölgeli ahşap, krem çorbada somon/beyaz balık ve dereotu)
ve buharsızdır. Dondurma, helva ve sorbe kasenin ağzından kubbe gibi taşar.
Çalıştırma: KÖK/.venv/bin/python tools/assets/yiyecek_dokular.py (rastgelelik yok; aynı kod → aynı bayt).
"""
from pathlib import Path

import numpy as np
from PIL import Image

KOK = Path(__file__).resolve().parents[2]
DOKU = KOK / "src/main/resources/assets/dogalafetler/textures"


def rgba(hex_renk, a=255):
    h = hex_renk.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


class Tuval:
    def __init__(self, boyut):
        self.n = boyut
        self.px = np.zeros((boyut, boyut, 4), dtype=np.float64)

    def koy(self, x, y, renk):
        """Alfa üstüne bindirme (src-over)."""
        if not (0 <= x < self.n and 0 <= y < self.n):
            return
        r, g, b, a = renk
        a /= 255.0
        alt = self.px[y, x]
        aa = alt[3] / 255.0
        cikis = a + aa * (1 - a)
        if cikis <= 0:
            return
        for i, c in enumerate((r, g, b)):
            alt[i] = (c * a + alt[i] * aa * (1 - a)) / cikis
        alt[3] = cikis * 255.0

    def sil(self, x, y):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.px[y, x] = 0

    def dolu(self, x, y):
        return 0 <= x < self.n and 0 <= y < self.n and self.px[y, x, 3] > 0

    def kaydet(self, yol):
        yol.parent.mkdir(parents=True, exist_ok=True)
        Image.fromarray(np.clip(np.rint(self.px), 0, 255).astype(np.uint8), "RGBA").save(yol, optimize=True)


def sekil(satirlar):
    """{y: (x0, x1)} satırlarından hücre kümesi; kenar = 4 komşusundan biri dışarıda olan hücre."""
    hucreler = {(x, y) for y, (x0, x1) in satirlar.items() for x in range(x0, x1 + 1)}
    kenar = {(x, y) for (x, y) in hucreler
             if any((x + dx, y + dy) not in hucreler for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    return hucreler, kenar, hucreler - kenar


# --- Şişe (vanilla iksir şişesi silueti) ---------------------------------------------------------------------
SISE = {3: (6, 9), 4: (6, 9), 5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (3, 12),
        12: (3, 12), 13: (4, 11), 14: (5, 10)}
CAM_KENAR = rgba("#2E3746")
CAM_BOS = rgba("#CFE6F2", 70)
CAM_PARLAK = rgba("#FFFFFF", 150)
MANTAR = (rgba("#B07A43"), rgba("#8A5A2B"), rgba("#5E3B1A"))


def sise(t, sivi, yuzey, koyu, seviye=8):
    hucreler, kenar, ic = sekil(SISE)
    for (x, y) in kenar:
        t.koy(x, y, CAM_KENAR)
    for (x, y) in ic:
        if y < seviye:
            t.koy(x, y, CAM_BOS)
        elif y == seviye:
            t.koy(x, y, yuzey)
        elif y >= 12 or x >= SISE[y][1] - 1:
            t.koy(x, y, koyu)
        else:
            t.koy(x, y, sivi)
    # cam parlaması: sol üstten ışık
    for (x, y) in ((4, 8), (4, 9), (4, 10), (5, 7), (7, 4)):
        t.koy(x, y, CAM_PARLAK)
    # mantar tıpa
    for x in range(6, 10):
        t.koy(x, 1, MANTAR[0] if x < 8 else MANTAR[1])
        t.koy(x, 2, MANTAR[1] if x < 9 else MANTAR[2])
    return hucreler


def buhar(t, noktalar, a=150):
    for i, (x, y) in enumerate(noktalar):
        t.koy(x, y, rgba("#F4F4F4", a - 12 * (i % 3)))


def kiragi(t, noktalar):
    """Camda kırağı ve buz parlaması (soğuk içecekler)."""
    for x, y in noktalar:
        t.koy(x, y, rgba("#EAF9FF", 235))


def parilti(t, x, y, renk="#FFFFFF"):
    t.koy(x, y, rgba(renk))
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.koy(x + dx, y + dy, rgba(renk, 120))


SICAK_BUHAR = [(3, 4), (2, 3), (3, 2), (2, 1), (12, 5), (13, 4), (12, 3), (13, 2), (12, 1)]


def sicak_salep():
    t = Tuval(16)
    sise(t, rgba("#EBD8B4"), rgba("#F8EEDA"), rgba("#CDB58C"))
    for x, y in ((5, 8), (8, 8), (10, 8)):  # tarçın
        t.koy(x, y, rgba("#9A5B2E"))
    t.koy(7, 9, rgba("#B9773F", 200))
    buhar(t, SICAK_BUHAR)
    return t


def sicak_cikolata():
    t = Tuval(16)
    sise(t, rgba("#5C3520"), rgba("#8A5634"), rgba("#3E2214"))
    for x, y in ((6, 8), (7, 8), (9, 8)):  # köpük / krema
        t.koy(x, y, rgba("#F3E6D6"))
    t.koy(8, 8, rgba("#D9C2A8"))
    buhar(t, SICAK_BUHAR)
    return t


def isitici_cay():
    t = Tuval(16)
    sise(t, rgba("#BD4A22"), rgba("#E27A3E"), rgba("#8A2E12"), seviye=9)
    for x, y in ((6, 11), (9, 10)):  # kuru meyve
        t.koy(x, y, rgba("#6E1A1A"))
    t.koy(7, 9, rgba("#F5B06A"))
    buhar(t, [(3, 4), (2, 3), (3, 2), (12, 4), (13, 3)], a=130)
    return t


def ayran():
    t = Tuval(16)
    sise(t, rgba("#F2F5F1"), rgba("#FFFFFF"), rgba("#D3DDE2"), seviye=7)
    for x, y in ((5, 7), (7, 7), (10, 7), (6, 6), (9, 6)):  # köpük
        t.koy(x, y, rgba("#FFFFFF"))
    kiragi(t, [(3, 12), (12, 9), (12, 10), (4, 13), (11, 13)])
    parilti(t, 13, 2, "#DFF6FF")
    return t


def meyve_serbeti():
    t = Tuval(16)
    sise(t, rgba("#E04B6B"), rgba("#F28BA2"), rgba("#A8273F"))
    for x, y in ((5, 10), (6, 10), (5, 11)):  # buz küpü
        t.koy(x, y, rgba("#E6F7FF", 230))
    t.koy(6, 11, rgba("#B8E2F5", 230))
    for x, y in ((9, 9), (10, 11), (8, 12)):  # çilek tanesi
        t.koy(x, y, rgba("#8E1426"))
    kiragi(t, [(3, 9), (12, 11), (12, 12), (4, 13)])
    parilti(t, 13, 2, "#DFF6FF")
    return t


# --- Kase (vanilla kase silueti: arka ağız, içinde çorba, açık ön ağız bandı, gövde) -------------------------------
KASE = {5: (4, 11), 6: (2, 13), 7: (1, 14), 8: (1, 14), 9: (1, 14), 10: (2, 13), 11: (3, 12), 12: (4, 11), 13: (5, 10)}
AHSAP = (rgba("#B07842"), rgba("#8A5A30"), rgba("#663F1F"), rgba("#3A2310"))


def kase(t, sivi, yuzey, malzemeler):
    hucreler, kenar, ic = sekil(KASE)
    for (x, y) in hucreler:
        if (x, y) in kenar:
            t.koy(x, y, AHSAP[3])
        elif y <= 8:
            t.koy(x, y, yuzey if y == 6 or (y == 7 and 3 <= x <= 6) else sivi)
        elif y == 9:
            t.koy(x, y, rgba("#C99460") if x <= 4 else AHSAP[0])  # ön ağız bandı
        elif y == 12 or x >= 11:
            t.koy(x, y, AHSAP[2])
        else:
            t.koy(x, y, AHSAP[1])
    for (x, y, renk) in malzemeler:
        t.koy(x, y, rgba(renk))


KASE_BUHAR = [(5, 3), (4, 2), (5, 1), (4, 0), (10, 3), (11, 2), (10, 1), (11, 0)]


def sicak_tavuk_corbasi():
    t = Tuval(16)
    kase(t, rgba("#E2B04A"), rgba("#F5D27A"), [
        (5, 7, "#F4E2BC"), (6, 8, "#F4E2BC"), (9, 7, "#F4E2BC"),  # tavuk
        (8, 8, "#E8731F"), (11, 8, "#E8731F"), (4, 8, "#E8731F"),  # havuç
        (7, 7, "#5BA03A"), (10, 6, "#5BA03A"),  # maydanoz
        (12, 8, "#D9C38A")])  # patates
    buhar(t, KASE_BUHAR)
    return t


def atesli_guvec():
    t = Tuval(16)
    kase(t, rgba("#B3301B"), rgba("#D9541F"), [
        (5, 7, "#FFD34A"), (9, 8, "#FFD34A"), (12, 8, "#FFB238"),  # alev tozu korları
        (7, 8, "#6E2A16"), (10, 7, "#6E2A16"), (4, 8, "#6E2A16"),  # et
        (8, 7, "#D9C38A")])  # patates
    # alev dilleri
    for x, y, renk in ((6, 5, "#F07A1E"), (6, 4, "#FFC23A"), (7, 5, "#FFC23A"), (9, 5, "#F07A1E"), (9, 4, "#F07A1E"),
                       (9, 3, "#FFE38A"), (10, 5, "#FFC23A"), (8, 5, "#E8531A"), (6, 3, "#FFE38A"), (10, 4, "#F07A1E")):
        t.koy(x, y, rgba(renk))
    buhar(t, [(4, 2), (3, 1), (4, 0), (12, 2), (13, 1), (12, 0)], a=140)
    return t


def kutup_baligi_corbasi():
    """Kendi kasesi (vanilla güveç kalitesi): arka ağız, iç duvar gölgesi, sol üstten parlayan ön ağız bandı, damarlı ve
    sağa doğru koyulan ahşap gövde, ayak; içinde serin krem çorba, somon ve beyaz balık parçaları, dereotu. Buharsız."""
    t = Tuval(16)
    hucreler, kenar, _ = sekil({4: (4, 11), 5: (2, 13), 6: (1, 14), 7: (1, 14), 8: (1, 14), 9: (1, 14), 10: (1, 14),
                               11: (2, 13), 12: (3, 12), 13: (4, 11), 14: (5, 10)})
    corba = {6: (5, 10), 7: (3, 12), 8: (3, 12)}
    agac = ("#C98E54", "#A86E3A", "#8A562A", "#6A3E1C")
    for (x, y) in sorted(hucreler):
        if (x, y) in kenar:
            alt_sag = (x + 1, y) not in hucreler or (x, y + 1) not in hucreler
            renk = "#2C180A" if alt_sag else "#56341A" if y == 4 else "#4A2C14"
        elif y in corba and corba[y][0] <= x <= corba[y][1]:
            renk = "#B4C6CC" if y == 6 or x >= 11 else "#F4FAFA" if (x, y) == (6, 7) else "#D6E4E6"
        elif y <= 8:
            if y == 6 and 3 <= x <= 12:
                renk = "#7A4C24"  # arka iç duvar
            else:
                renk = "#DEAA6C" if x <= 5 else "#B07A44" if x >= 11 else "#C8925A"  # ağız üstü
        elif y == 9:
            renk = "#EEC082" if x <= 4 else "#D6A064" if x <= 9 else "#B8804A"  # ön ağız parlaması
        elif y == 13:
            renk = agac[2] if x <= 6 else agac[3]  # ayak
        else:
            v = (x - 7.5) / 6 + (y - 10) * 0.25
            i = 0 if v < -0.5 else 1 if v < 0.2 else 2 if v < 0.6 else 3
            if (y == 11 and 5 <= x <= 7) or (y == 12 and 8 <= x <= 9):
                i = min(i + 1, 3)  # ahşap damarı
            renk = agac[i]
        t.koy(x, y, rgba(renk))
    for x, y, renk in ((4, 7, "#F09C6E"), (5, 7, "#C8704A"), (8, 8, "#F09C6E"), (9, 8, "#C8704A"),  # somon
                       (9, 6, "#FBF7EE"), (10, 6, "#D8D0C0"), (11, 7, "#FBF7EE"), (12, 7, "#D8D0C0"),  # beyaz balık
                       (7, 6, "#5E9A3C"), (8, 7, "#3E6E2A"), (3, 8, "#5E9A3C"), (11, 8, "#3E6E2A"),  # dereotu
                       (6, 8, "#4A3A30")):  # karabiber
        t.koy(x, y, rgba(renk))
    return t


# --- Buzlu karpuz dilimi (vanilla karpuz dilimi silueti) -----------------------------------------------------
def buzlu_karpuz():
    t = Tuval(16)
    # dilim: merkezi (1, 14) olan çeyrek daire; dıştan içe kabuk, beyaz band, kırmızı et
    for y in range(16):
        for x in range(16):
            dx, dy = x - 1.5, 14.5 - y
            if dx < 0 or dy < 0:
                continue
            r = (dx * dx + dy * dy) ** 0.5
            if r > 13.6:
                continue
            if r > 12.6:
                renk = rgba("#24501A")
            elif r > 11.4:
                renk = rgba("#4F9A2E")
            elif r > 10.4:
                renk = rgba("#D9EDB8")
            else:
                renk = rgba("#E8414C") if (x + y) % 5 else rgba("#F0646D")
            t.koy(x, y, renk)
    for y in range(5, 15):  # kesik yüzlerin (sol ve alt) koyu kenarı, yalnız kırmızı ette
        t.koy(2, y, rgba("#B32C38"))
    for x in range(2, 12):
        t.koy(x, 14, rgba("#B32C38"))
    for x, y in ((4, 11), (7, 9), (5, 7), (9, 12), (3, 5)):  # çekirdek
        t.koy(x, y, rgba("#2A1A12"))
    # kırağı: kabuğun üstünde ve kesik yüzde buz kristalleri
    for x, y in ((2, 1), (4, 1), (6, 2), (8, 3), (10, 4), (11, 6), (12, 8), (13, 10), (3, 2), (9, 3), (12, 7), (13, 12)):
        t.koy(x, y, rgba("#EAF8FF", 240))
    for x, y in ((5, 2), (7, 3), (11, 5), (13, 11)):
        t.koy(x, y, rgba("#BFE6F7", 230))
    for x, y in ((2, 6), (2, 9), (5, 13), (8, 13)):
        t.koy(x, y, rgba("#F4FBFF", 200))
    parilti(t, 13, 2, "#FFFFFF")
    return t


# --- Durum efekti ikonları (18x18) ---------------------------------------------------------------------------
def icten_isinma():
    """Sıcak bir kalp içinde titreyen alev: dış kenar koyu kızıl, içe doğru turuncu → sarı → açık çekirdek."""
    t = Tuval(18)
    katmanlar = [("#7A1A0C", 1.00), ("#D8461A", 0.80), ("#F28A22", 0.60), ("#FFC94A", 0.40), ("#FFF3B0", 0.20)]
    for renk, k in katmanlar:
        for y in range(18):
            for x in range(18):
                if alev_icinde(x + 0.5, y + 0.5, k):
                    t.koy(x, y, rgba(renk))
    for x, y in ((3, 6), (14, 4), (2, 11), (15, 10)):  # kıvılcım
        t.koy(x, y, rgba("#FFB238", 220))
    return t


def alev_icinde(x, y, k):
    """Alev silueti: altı daire, üstü sola kıvrılan sivri uç; k ölçek (1 dış, küçüldükçe iç katman)."""
    cx, cy = 9.0, 12.0
    r = 5.2 * k
    if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
        return True
    tepe_y = cy - 10.5 * k
    if y < tepe_y or y > cy:
        return False
    f = (cy - y) / (cy - tepe_y)
    egri = 1.6 * k * np.sin(f * np.pi)
    yari = r * (1 - f) ** 0.8
    merkez = cx - egri + 0.6 * k * f
    return abs(x - merkez) <= yari


def serinlik():
    """Altı kollu kar tanesi: koyu mavi kenar, buz mavisi kollar, beyaz merkez."""
    t = Tuval(18)
    cx, cy = 8.5, 8.5
    kollar = []
    for i in range(6):
        aci = np.pi / 2 + i * np.pi / 3
        kollar.append((np.cos(aci), -np.sin(aci)))

    def kolda(x, y, kalinlik, uzunluk):
        for ux, uy in kollar:
            px, py = x - cx, y - cy
            boy = px * ux + py * uy
            dik = abs(px * uy - py * ux)
            if 0 <= boy <= uzunluk and dik <= kalinlik:
                return True
            for dal in (0.55, 0.78):  # yan dallar
                bx, by = cx + ux * uzunluk * dal, cy + uy * uzunluk * dal
                for sx in (1, -1):
                    vx = ux * 0.5 - sx * uy * 0.866
                    vy = uy * 0.5 + sx * ux * 0.866
                    qx, qy = x - bx, y - by
                    b2 = qx * vx + qy * vy
                    d2 = abs(qx * vy - qy * vx)
                    if 0 <= b2 <= 2.4 * (1.2 - dal) / 0.65 and d2 <= kalinlik:
                        return True
        return False

    for y in range(18):
        for x in range(18):
            if kolda(x, y, 1.25, 8.2):
                t.koy(x, y, rgba("#1D5A8C"))
    for y in range(18):
        for x in range(18):
            if kolda(x, y, 0.5, 7.6):
                t.koy(x, y, rgba("#9FE2FF"))
    for x, y in ((8, 8), (9, 8), (8, 9), (9, 9)):
        t.koy(x, y, rgba("#FFFFFF"))
    return t


# --- Sıcaklık-3 yiyecekleri (S3-06) -----------------------------------------------------------------------------
def buz_kupu(t, x, y):
    """2x2 buz küpü: sol üst parlak, sağ alt gölgeli."""
    t.koy(x, y, rgba("#F2FBFF", 235))
    t.koy(x + 1, y, rgba("#CFEFFC", 230))
    t.koy(x, y + 1, rgba("#CFEFFC", 230))
    t.koy(x + 1, y + 1, rgba("#9ED3EE", 230))


def kubbe(t, kenar, ic, parlak):
    """Kasenin ağzından taşan yuvarlak kubbe (dondurma, helva, sorbe); ışık sol üstten."""
    hucreler, kenar_h, ic_h = sekil({2: (6, 9), 3: (4, 11), 4: (3, 12), 5: (3, 12)})
    for (x, y) in sorted(hucreler):
        if (x, y) in kenar_h and y < 5:
            t.koy(x, y, rgba(kenar))
        elif y <= 3 and x <= 7 or (x, y) == (4, 4):
            t.koy(x, y, rgba(parlak))
        else:
            t.koy(x, y, rgba(ic))


def soguk_elma_suyu():
    t = Tuval(16)
    sise(t, rgba("#E8B84A"), rgba("#F7DA84"), rgba("#B8862A"))
    buz_kupu(t, 5, 9)
    buz_kupu(t, 8, 10)
    t.koy(10, 9, rgba("#C8402C"))  # elma dilimi kabuğu
    t.koy(10, 8, rgba("#F4E4B0"))
    kiragi(t, [(3, 10), (12, 9), (12, 11), (4, 13), (11, 13)])
    parilti(t, 13, 2, "#DFF6FF")
    return t


def soguk_komposto():
    t = Tuval(16)
    sise(t, rgba("#9A3B5C"), rgba("#C66A86"), rgba("#6A2240"))
    for x, y, renk in ((5, 10, "#F1DFA0"), (6, 10, "#E6CD80"),  # elma dilimi
                       (9, 9, "#5E0E22"), (8, 11, "#5E0E22"), (10, 11, "#7E1A32")):  # dut
        t.koy(x, y, rgba(renk))
    buz_kupu(t, 6, 12)
    kiragi(t, [(3, 9), (12, 10), (12, 12), (4, 13)])
    parilti(t, 13, 2, "#DFF6FF")
    return t


def buzlu_kakao():
    t = Tuval(16)
    sise(t, rgba("#8A5636"), rgba("#B5825A"), rgba("#5C3520"), seviye=7)
    for x, y in ((5, 7), (6, 7), (8, 7), (9, 7), (7, 6)):  # krema
        t.koy(x, y, rgba("#F6ECDD"))
    buz_kupu(t, 5, 9)
    buz_kupu(t, 9, 10)
    kiragi(t, [(3, 10), (12, 9), (4, 13), (11, 13), (12, 12)])
    parilti(t, 13, 2, "#DFF6FF")
    return t


def elma_cayi():
    t = Tuval(16)
    sise(t, rgba("#D9822B"), rgba("#F2AE58"), rgba("#A35A16"), seviye=9)
    for x, y, renk in ((6, 11, "#C8402C"), (7, 11, "#F1DFA0"), (9, 10, "#F1DFA0")):  # elma parçaları
        t.koy(x, y, rgba(renk))
    t.koy(5, 10, rgba("#FFD9A0", 200))
    buhar(t, [(3, 4), (2, 3), (3, 2), (12, 4), (13, 3), (12, 2)], a=135)
    return t


def maras_dondurmasi():
    """Sütlü, sakızlı beyaz dondurma kubbesi, üstünde Antep fıstığı."""
    t = Tuval(16)
    kase(t, rgba("#F4EEE2"), rgba("#FFFFFF"), [])
    kubbe(t, "#B9B2A2", "#F2ECDF", "#FFFFFF")
    for x, y in ((6, 2), (9, 3), (5, 4), (10, 4), (8, 4)):  # fıstık
        t.koy(x, y, rgba("#7DB043"))
    t.koy(7, 3, rgba("#A6D06A"))
    kiragi(t, [(2, 6), (13, 7)])
    parilti(t, 13, 1, "#FFFFFF")
    return t


def kar_helvasi():
    """Kar yığını üstüne üzüm pekmezi gezdirilmiş."""
    t = Tuval(16)
    kase(t, rgba("#E8F3F9"), rgba("#FFFFFF"), [])
    kubbe(t, "#9FB8C8", "#E6F2F8", "#FFFFFF")
    for x, y in ((6, 2), (7, 3), (8, 3), (9, 4), (10, 4), (5, 4), (4, 5), (11, 5)):  # pekmez
        t.koy(x, y, rgba("#6E2A2A"))
    t.koy(7, 2, rgba("#9A4436"))
    parilti(t, 13, 1, "#FFFFFF")
    return t


def parlak_sorbe():
    """Parıltılı meyveden turuncu-altın sorbe, sıkıştırılmış buz kristalleri, içten ışıyan tanecikler."""
    t = Tuval(16)
    kase(t, rgba("#F2A33A"), rgba("#FFD36A"), [])
    kubbe(t, "#B0661A", "#F4A83C", "#FFD36A")
    for x, y in ((6, 3), (9, 2), (10, 4), (5, 4), (8, 5)):  # parlayan tanecik
        t.koy(x, y, rgba("#FFF6C0"))
    for x, y in ((3, 5), (12, 5), (7, 4)):  # buz kristali
        t.koy(x, y, rgba("#CDEFFF", 235))
    kiragi(t, [(2, 7), (13, 6)])
    parilti(t, 13, 1, "#FFFFFF")
    parilti(t, 2, 1, "#FFF0A8")
    return t


def tarhana_corbasi():
    t = Tuval(16)
    kase(t, rgba("#C2502E"), rgba("#E07A4A"), [
        (5, 7, "#5E9A3C"), (9, 8, "#5E9A3C"), (11, 7, "#3E6E2A"),  # nane
        (7, 8, "#8E2A1C"), (4, 8, "#8E2A1C"),  # pul biber
        (8, 7, "#F0A070")])  # yoğurt izi
    buhar(t, KASE_BUHAR)
    return t


def kuzu_haslama():
    t = Tuval(16)
    kase(t, rgba("#D8B878"), rgba("#EED29A"), [
        (5, 7, "#8A5A3A"), (6, 8, "#6E4428"), (10, 7, "#8A5A3A"),  # kuzu eti
        (8, 8, "#E8731F"), (12, 8, "#E8731F"),  # havuç
        (9, 6, "#E6D29A"), (4, 8, "#E6D29A"),  # patates
        (7, 7, "#5BA03A")])  # maydanoz
    buhar(t, KASE_BUHAR)
    return t


def kabak_corbasi():
    t = Tuval(16)
    kase(t, rgba("#E8892A"), rgba("#F6B04E"), [
        (6, 7, "#FFF4E0"), (7, 7, "#FFF4E0"), (8, 8, "#FFF4E0"), (9, 8, "#F8E2C0"),  # krema kıvrımı
        (4, 8, "#5E7A2C"), (11, 7, "#5E7A2C"),  # kabak çekirdeği
        (10, 6, "#FFE3A0")])
    buhar(t, KASE_BUHAR)
    return t


def sucuklu_yumurta():
    t = Tuval(16)
    kase(t, rgba("#FBF6EA"), rgba("#FFFFFF"), [
        (5, 7, "#F6B524"), (6, 7, "#E89A10"), (10, 7, "#F6B524"), (11, 7, "#E89A10"),  # sarılar
        (8, 6, "#8E2A1E"), (8, 8, "#8E2A1E"), (3, 8, "#8E2A1E"), (12, 8, "#8E2A1E"),  # sucuk
        (9, 6, "#B8463A"), (4, 8, "#B8463A")])
    buhar(t, KASE_BUHAR)
    return t


def firinda_elma():
    """Fırında elma: koyulmuş kırmızı kabuk, tepesinde karamelleşmiş bal, sap; buhar."""
    t = Tuval(16)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8.0, y + 0.5 - 9.5
            r = (dx * dx + dy * dy * 1.15) ** 0.5
            if r > 5.9:
                continue
            if r > 5.0:
                renk = "#5A140E"
            elif dx + dy < -4.0:
                renk = "#E0663A"  # sol üst parlama
            elif dx + dy > 3.0:
                renk = "#8A2416"
            else:
                renk = "#B8361F"
            t.koy(x, y, rgba(renk))
    for x, y in ((6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6), (6, 6), (9, 6), (10, 6)):  # karamel bal
        t.koy(x, y, rgba("#E0A23C"))
    for x, y in ((7, 7), (9, 7), (10, 8)):  # süzmesi
        t.koy(x, y, rgba("#C98222"))
    t.koy(8, 4, rgba("#5E3B1A"))  # sap
    t.koy(8, 3, rgba("#5E3B1A"))
    t.koy(6, 9, rgba("#F4A070", 200))
    buhar(t, [(4, 3), (3, 2), (4, 1), (11, 3), (12, 2), (11, 1)], a=140)
    return t


# --- Buz Kovası (S3-05; vanilla kova silueti) ----------------------------------------------------------------
def buz_kovasi():
    """Vanilla kova: sap, ağız bandı, aşağı daralan metal gövde; ağzından buz blokları taşar, gövdede kırağı."""
    t = Tuval(16)
    for x, y in ((2, 4), (2, 3), (3, 2), (4, 1), (11, 1), (12, 2), (13, 3), (13, 4)):  # sap
        t.koy(x, y, rgba("#3A3A3A"))
    govde = {4: (2, 13), 5: (2, 13), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (4, 11), 11: (4, 11),
             12: (4, 11), 13: (4, 11), 14: (5, 10)}
    hucreler, kenar, _ = sekil(govde)
    for (x, y) in sorted(hucreler):
        if (x, y) in kenar:
            renk = "#2E2E2E"
        elif y == 5:
            renk = "#E2E2E2" if x <= 6 else "#BDBDBD"  # ağız bandı
        elif x <= 4 or (x == 5 and y <= 9):
            renk = "#D0D0D0"
        elif x >= 10 or y >= 13:
            renk = "#787878"
        else:
            renk = "#A6A6A6"
        t.koy(x, y, rgba(renk))
    # ağızdan taşan buz blokları (üç eksenli: üst yüz parlak, ön yüz mavi, derz koyu)
    for x0, y0 in ((3, 2), (7, 1), (9, 3)):
        for dx in range(4):
            for dy in range(3):
                x, y = x0 + dx, y0 + dy
                if dx == 3 or dy == 2:
                    renk = "#4F95C4"
                elif dy == 0:
                    renk = "#E8F7FF"
                else:
                    renk = "#9CD3F2"
                t.koy(x, y, rgba(renk))
    t.koy(4, 3, rgba("#FFFFFF"))
    t.koy(8, 2, rgba("#FFFFFF"))
    kiragi(t, [(4, 8), (6, 11), (5, 12), (9, 7)])
    parilti(t, 14, 1, "#DFF6FF")
    return t


ESYALAR = {
    "sicak_salep": sicak_salep,
    "sicak_cikolata": sicak_cikolata,
    "sicak_tavuk_corbasi": sicak_tavuk_corbasi,
    "atesli_guvec": atesli_guvec,
    "isitici_cay": isitici_cay,
    "ayran": ayran,
    "buzlu_karpuz": buzlu_karpuz,
    "meyve_serbeti": meyve_serbeti,
    "kutup_baligi_corbasi": kutup_baligi_corbasi,
    "maras_dondurmasi": maras_dondurmasi,
    "soguk_elma_suyu": soguk_elma_suyu,
    "kar_helvasi": kar_helvasi,
    "soguk_komposto": soguk_komposto,
    "parlak_sorbe": parlak_sorbe,
    "buzlu_kakao": buzlu_kakao,
    "tarhana_corbasi": tarhana_corbasi,
    "kuzu_haslama": kuzu_haslama,
    "kabak_corbasi": kabak_corbasi,
    "sucuklu_yumurta": sucuklu_yumurta,
    "firinda_elma": firinda_elma,
    "elma_cayi": elma_cayi,
    "buz_kovasi": buz_kovasi,
}
EFEKTLER = {"icten_isinma": icten_isinma, "serinlik": serinlik}


def main():
    for ad, ciz in ESYALAR.items():
        ciz().kaydet(DOKU / "item" / f"{ad}.png")
    for ad, ciz in EFEKTLER.items():
        ciz().kaydet(DOKU / "mob_effect" / f"{ad}.png")
    print(f"{len(ESYALAR)} eşya, {len(EFEKTLER)} efekt dokusu yazıldı: {DOKU}")


if __name__ == "__main__":
    main()
