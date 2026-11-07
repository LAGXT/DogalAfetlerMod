#!/usr/bin/env python3
"""Buz altı balıkçılık dokularını üretir (16x16 PNG, prosedürel; Mojang dokusu kopyalanmaz). Blok dokuları ve buz parçası
sabit tohumlu gürültü kullanır; balıklarda rastgelelik yoktur. Her çıktı kendi tohumuyla üretilir, biri değişince ötekiler
bayt bayt aynı kalır.

Blok (src/main/resources/assets/dogalafetler/textures/block/):
- buz_deligi_kenar.png: deliğin buz çerçevesi; açık mavi, ~%85 opak buz, beyaz çatlak ağı ve kırağı noktaları.
- buz_deligi_buzlanma_{1,2,3}.png: deliğin yeniden donma zarı; ortadaki 10x10 açıklıkta (piksel 3..12) kenarlardan içeri
  büyüyen yarı saydam beyaz-mavi buz iğneleri. Aşama 1 kenar şeridi, 2 yarısı, 3 neredeyse kapalı.

Eşya (src/main/resources/assets/dogalafetler/textures/item/):
- buz_alabaligi, kutup_morinasi, derin_soguk_somonu, buz_kefali, donmus_ringa (+ pismis_ önekli beşi): vanilla balık
  ikonları kalitesinde piksel sanat, çapraz duruş (kuyruk sol altta, baş sağ üstte). Çizim köşegen çerçevede yapılır
  (s = x - y gövde boyunca, p = x + y sırttan karına): türün sırt/karın zarfı, kuyruk yelpazesi (düz, yuvarlak, çatal,
  derin çatal) ve sırt/anal yüzgeçleri ayrı şekiller olarak çıkar; dolgu 1..14'te kalır, dışına 1 px seçici dış çizgi
  gelir (üst/sol daha açık, karın/alt-sağ en koyu). Gövde sırttan karına bantlarla boyanır (sırt gölgesi, sırt, parlak
  şerit, yan, karın, karın gölgesi; 4 tonluk sırt + 3 tonluk alt rampası), sırt ve yanda (x + 2y) % 4 sıralı pul gölgesi;
  yüzgeçler açık zar + koyu ışın (sırtta gövdeye dik, kuyrukta eksene paralel). Başta 1 px bebek + 1 px parlamalı
  göz, ağız çentiği, gövdeye dik solungaç çizgisi ve arkasında göğüs yüzgeci. Türler: alabalık zeytin-camgöbeği
  sırt, pembe yan şerit, koyu benekler (yüzgeç ve kuyrukta da); morina zeytin-kahve alacalı sırt, soluk yan çizgi, iki
  sırt yüzgeci, çene bıyığı; somon en iri, koyu lacivert sırt, geniş gümüş yan, derin çatal kuyruk; kefal gri, boyuna
  koyu çizgiler, iki sırt yüzgeci, yuvarlak kuyruk; ringa ince, parlak mavi, gümüş şerit, sırt dış çizgisinde beyaz
  kırağı ve parıltılar. Pişmişler aynı siluette altın-kahveden koyu kahveye rampa (türe göre hafif ton), kıtır koyu
  kenar bantları, opak kızarmış yüzgeçler, yanık kuyruk ucu, iki yatay ızgara izi, bulanık beyaz göz; tür izi kalır
  (alabalık benek, morina pul pul beyaz et, somon açık yağ çizgileri, kefal çizgi, ringa altın parlaklık). Betik sonunda
  pişmiş çiftlerin ortak piksellerdeki ortalama |dR|+|dG|+|dB| renk farkı ve siluet farkı (XOR piksel) yazılır.
- buz_parcasi.png: sivri, açık camgöbeği buz kristali.

Kullanım (worktree kökünden): KOK/.venv/bin/python tools/assets/balikcilik_dokular.py
"""
import math
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
BLOK = KOK / "src/main/resources/assets/dogalafetler/textures/block"
ESYA = KOK / "src/main/resources/assets/dogalafetler/textures/item"


def sinirla(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def karistir(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


# --- Blok dokuları -------------------------------------------------------------------------------------------------

def kenar():
    r = random.Random(0xB0D1)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = r.randint(-7, 7)
            mavi = karistir((142, 184, 222), (176, 208, 236), (x + y) / 30)
            px[x, y] = (*sinirla((mavi[0] + v, mavi[1] + v, mavi[2] + v)), 214 + r.randint(-6, 6))
    for _ in range(5):
        x, y = r.randrange(16), r.randrange(16)
        dx, dy = r.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        for _ in range(r.randint(4, 9)):
            px[x % 16, y % 16] = (236, 244, 252, 236)
            if r.random() < 0.3:
                dx, dy = r.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
            x, y = x + dx, y + dy
    for _ in range(10):
        px[r.randrange(16), r.randrange(16)] = (248, 252, 255, 240)
    return img


def buzlanma(asama):
    """Kenardan içeri büyüyen zar: açıklığın kenarına uzaklık + gürültü eşiğin altındaysa buz."""
    r = random.Random(0x2A0 + asama)
    esik = {1: 0.6, 2: 1.9, 3: 3.4}[asama]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            uzaklik = min(x - 3, 12 - x, y - 3, 12 - y)
            if uzaklik + r.uniform(-0.8, 0.8) <= esik:
                beyaz = r.randint(0, 18)
                alfa = 120 + int(26 * asama) + r.randint(-14, 14)
                px[x, y] = (214 + beyaz // 2, 230 + beyaz // 3, 246, min(alfa, 220))
    # buz iğneleri: kenardan merkeze uzanan kısa parlak çizgiler
    for _ in range(3 + 3 * asama):
        kenar_ = r.randrange(4)
        t = r.randrange(3, 13)
        x, y, dx, dy = [(t, 3, 0, 1), (t, 12, 0, -1), (3, t, 1, 0), (12, t, -1, 0)][kenar_]
        for _ in range(1 + asama + r.randint(0, asama)):
            if 3 <= x <= 12 and 3 <= y <= 12:
                px[x, y] = (246, 251, 255, 225)
            x += dx + (r.choice([-1, 0, 1]) if dx == 0 else 0)
            y += dy + (r.choice([-1, 0, 1]) if dy == 0 else 0)
    return img


# --- Balıklar ------------------------------------------------------------------------------------------------------

# Köşegen çerçeve: s = x - y kuyruktan (-) başa (+), p = x + y sırttan (küçük) karına (büyük); gövde ekseni p = 15.
# Sabit s köşegenleri gövdeye diktir (solungaç, sırt yüzgeci ışınları, somon yağ çizgileri), sabit p köşegenleri eksene
# paraleldir (yan şerit, boyuna çizgiler, kuyruk ışınları). Aynı s üzerindeki pikseller p'de ikişer artar.
EKSEN = 15

# Siluet: bas/sap = burun ve kuyruk sapı köşegeni (s); *_yari = eksenden sırt/karın kenarına p uzaklığı (dik 1 piksel = 2 p);
# kuyruk = (biçim, arka kenar s, en büyük yarı açıklık p); yuzgecler = (taraf, s0, s1, yükseklik p), yükseklik önde (s1)
# en büyük. Rampalar: sirt (parlak, ana, gölge, derin), alt (yan, karın, karın gölgesi), yuzgec (zar, ışın),
# pismis (parlak, ana, gölge, derin, karın).
TURLER = {
    "buz_alabaligi": dict(
        bas=11, sap=-5, sirt_yari=3.0, karin_yari=3.4, sap_yari=1.0, kuyruk=("duz", -9, 4.0),
        yuzgecler=[("sirt", 0, 4, 3.4), ("karin", -3, -1, 2.4)],
        sirt=((126, 162, 134), (84, 126, 110), (58, 96, 88), (36, 64, 62)),
        alt=((180, 194, 178), (232, 236, 226), (190, 196, 186)),
        yuzgec=((168, 184, 150), (122, 142, 114)),
        pismis=((238, 178, 128), (212, 138, 94), (166, 96, 62), (112, 62, 40), (242, 204, 160))),
    "kutup_morinasi": dict(
        bas=11, sap=-5, sirt_yari=3.4, karin_yari=3.8, sap_yari=1.0, kuyruk=("duz", -9, 3.6),
        yuzgecler=[("sirt", 4, 6, 2.6), ("sirt", -1, 2, 2.6), ("karin", -3, 0, 2.2)],
        sirt=((170, 154, 108), (132, 116, 78), (100, 86, 58), (70, 58, 40)),
        alt=((190, 178, 138), (234, 228, 206), (200, 192, 166)),
        yuzgec=((178, 162, 118), (134, 118, 82)),
        pismis=((244, 214, 160), (222, 178, 116), (180, 134, 78), (126, 88, 48), (248, 232, 194))),
    "derin_soguk_somonu": dict(
        bas=12, sap=-6, sirt_yari=3.8, karin_yari=4.0, sap_yari=1.0, kuyruk=("derin", -10, 5.0),
        yuzgecler=[("sirt", 1, 4, 3.0), ("karin", -4, -2, 2.0)],
        sirt=((78, 102, 148), (46, 64, 108), (32, 44, 80), (20, 28, 54)),
        alt=((192, 204, 216), (240, 242, 246), (200, 208, 220)),
        yuzgec=((128, 142, 168), (84, 98, 128)),
        pismis=((240, 156, 94), (212, 114, 62), (164, 78, 40), (110, 48, 24), (244, 188, 132))),
    "buz_kefali": dict(
        bas=11, sap=-5, sirt_yari=3.0, karin_yari=3.2, sap_yari=1.2, kuyruk=("yuvarlak", -8, 3.6),
        yuzgecler=[("sirt", 3, 5, 2.6), ("sirt", -2, 0, 2.2), ("karin", -3, -1, 2.2)],
        sirt=((156, 164, 172), (114, 122, 132), (86, 92, 102), (60, 64, 74)),
        alt=((178, 184, 190), (226, 230, 234), (190, 196, 202)),
        yuzgec=((168, 174, 180), (122, 128, 136)),
        pismis=((212, 176, 134), (176, 138, 100), (134, 100, 70), (92, 66, 46), (224, 200, 164))),
    "donmus_ringa": dict(
        bas=11, sap=-5, sirt_yari=2.2, karin_yari=2.6, sap_yari=0.8, kuyruk=("catal", -11, 4.4),
        yuzgecler=[("sirt", 0, 2, 2.6)],
        sirt=((108, 176, 232), (58, 126, 200), (40, 92, 162), (26, 62, 122)),
        alt=((178, 214, 240), (240, 248, 254), (196, 218, 236)),
        yuzgec=((164, 206, 236), (104, 156, 206)),
        pismis=((244, 202, 104), (214, 158, 66), (168, 112, 40), (116, 74, 26), (248, 222, 150))),
}
GOZ_BEBEK = (16, 18, 26)
GOZ_ISIK = (250, 252, 255)
PISMIS_GOZ = (236, 230, 212)
KIRAGI = (234, 246, 255)


def zarf(tur, s):
    """Gövdenin s köşegenindeki sırt ve karın kenarı (p): saptan genişleyen gövde, küt baş."""
    t = (s - tur["sap"]) / (tur["bas"] - tur["sap"])
    if t < 0.55:
        e = math.sin(math.pi / 2 * t / 0.55) ** 0.8
    else:
        e = 1 - 0.62 * ((t - 0.55) / 0.45) ** 1.8
    return EKSEN - tur["sap_yari"] - tur["sirt_yari"] * e, EKSEN + tur["sap_yari"] + tur["karin_yari"] * e


def kuyrukta(tur, s, p):
    """Kuyruk yelpazesi: saptan geriye açılır, arka kenarı biçime göre düz, yuvarlak ya da çatal."""
    bicim, arka, acik = tur["kuyruk"]
    d = abs(p - EKSEN)
    if s >= tur["sap"] or d > min(acik, tur["sap_yari"] + (tur["sap"] - s) * 1.1):
        return False
    oran = d / acik
    if bicim == "yuvarlak":
        arka -= 1.6 * (1 - oran ** 2)
    elif bicim == "catal":
        arka += 3.2 * (1 - oran)
    elif bicim == "derin":
        arka += 4.4 * (1 - oran)
    return s >= arka


def bolgeler(tur):
    """(x, y) -> 'govde' | 'kuyruk' | 'yuzgec'; yüzgeçler gövdenin dışına taşan ayrı şekillerdir. Dolgu 1..14'te
    kalır ki dış çizgi doku içine sığsın."""
    b = {}
    for y in range(1, 15):
        for x in range(1, 15):
            s, p = x - y, x + y
            if tur["sap"] <= s <= tur["bas"]:
                sirt, karin = zarf(tur, s)
                if sirt <= p <= karin:
                    b[x, y] = "govde"
                    continue
                for taraf, s0, s1, yuk in tur["yuzgecler"]:
                    if s0 <= s <= s1:
                        h = yuk * (s - s0 + 1) / (s1 - s0 + 1)
                        if (taraf == "sirt" and sirt - h <= p < sirt) or (taraf == "karin" and karin < p <= karin + h):
                            b[x, y] = "yuzgec"
            elif kuyrukta(tur, s, p):
                b[x, y] = "kuyruk"
    return b


def ton(tur, s, p):
    """Sırttan karına konum (0..1), gövde zarfına göre; aynı p satırı komşu köşegenlerde aynı bandı verir."""
    sirt, karin = zarf(tur, min(max(s, tur["sap"]), tur["bas"]))
    return (p - sirt) / max(karin - sirt, 0.1)


def govdede_desen(tur, s):
    """Tür deseni başın (solungaçtan önce) ve kuyruk sapının dışında kalır."""
    return tur["sap"] < s < tur["bas"] - 4


def ciy_renk(ad, tur, x, y, bolge):
    s, p = x - y, x + y
    sirt, alt, yz = tur["sirt"], tur["alt"], tur["yuzgec"]
    benek = ad == "buz_alabaligi" and (x * 5 + y * 3) % 7 == 0
    if bolge == "yuzgec":
        if benek:
            return sirt[3]
        return yz[1] if s % 2 == 0 else yz[0]  # zar arasında dik ışınlar
    if bolge == "kuyruk":
        if tur["sap"] - s == 1:
            return sirt[1] if p < EKSEN else alt[0]
        if benek:
            return sirt[3]
        return yz[1] if (p - EKSEN) % 4 in (0, 1) else yz[0]  # eksene paralel ışınlar
    t = ton(tur, s, p)
    pul = (x + 2 * y) % 4 == 0  # sıralı pul gölgesi
    if t < 0.16:
        renk = sirt[2]
    elif t < 0.36:
        renk = sirt[2] if pul else sirt[1]
    elif t < 0.48:
        renk = sirt[0]
    elif t < 0.64:
        renk = karistir(alt[0], sirt[0], 0.35) if pul else alt[0]
    elif t < 0.88:
        renk = alt[1]
    else:
        renk = alt[2]
    if not govdede_desen(tur, s):
        return renk
    if ad == "buz_alabaligi":
        if 0.4 <= t < 0.56:
            renk = (204, 104, 122) if pul else (226, 126, 140)  # pembe yan şerit
        elif t < 0.4 and benek:
            renk = sirt[3]
    elif ad == "kutup_morinasi":
        if 0.36 <= t < 0.48:
            renk = (232, 224, 190)  # soluk yan çizgi
        elif t < 0.36:
            m = math.sin(x * 1.3 + 0.4) + math.sin(y * 1.7 + x * 0.6)
            if m > 0.9:
                renk = sirt[3]
            elif m < -1.1:
                renk = sirt[0]  # alacalı sırt
        elif t < 0.64 and (x + 3 * y) % 5 == 0:
            renk = sirt[1]
    elif ad == "derin_soguk_somonu":
        if 0.36 <= t < 0.48:
            renk = (104, 124, 158)  # lacivertten gümüşe geçiş
        elif 0.48 <= t < 0.76:
            renk = (170, 182, 198) if pul else alt[0]  # geniş gümüş yan
        elif t < 0.36 and (x * 3 + y * 5) % 9 == 0:
            renk = sirt[3]
    elif ad == "buz_kefali":
        if 0.1 <= t < 0.72 and p % 3 == 0:
            renk = (62, 68, 80)  # boyuna koyu çizgiler
    elif ad == "donmus_ringa":
        if 0.36 <= t < 0.48:
            renk = (204, 234, 252)  # gümüş parlak şerit
    return renk


def pismis_renk(ad, tur, x, y, bolge):
    s, p = x - y, x + y
    pis = tur["pismis"]
    if bolge == "yuzgec":
        return pis[3] if s % 2 == 0 else pis[2]
    if bolge == "kuyruk":
        g = tur["sap"] - s
        if g == 1:
            return pis[1]
        if g >= 4:
            return karistir(pis[3], (40, 22, 12), 0.35)  # kıtır yanık uç
        return pis[3] if (p - EKSEN) % 4 in (0, 1) else pis[2]
    t = ton(tur, s, p)
    if t < 0.16 or t >= 0.9:
        renk = pis[2]  # kıtır koyu kenar
    elif t < 0.4:
        renk = pis[1]
    elif t < 0.54:
        renk = pis[0]
    else:
        renk = pis[4]
    if not govdede_desen(tur, s):
        return renk
    if 0.24 < t < 0.78 and y in (6, 9):
        return pis[3]  # ızgara izi
    if ad == "buz_alabaligi" and t < 0.4 and (x * 5 + y * 3) % 7 == 0:
        renk = pis[3]
    elif ad == "kutup_morinasi" and t >= 0.4 and (x + 3 * y) % 5 == 0:
        renk = (252, 240, 210)  # pul pul beyaz et
    elif ad == "derin_soguk_somonu" and 0.3 <= t < 0.9 and s % 3 == 0:
        renk = (248, 200, 156)  # açık yağ çizgileri
    elif ad == "buz_kefali" and 0.1 <= t < 0.72 and p % 3 == 0:
        renk = pis[2]
    elif ad == "donmus_ringa" and 0.4 <= t < 0.54:
        renk = (252, 226, 140)  # altın parlaklık
    return renk


def kosegen(bolge, s):
    """s köşegenindeki gövde pikselleri, sırttan karına."""
    return sorted((k for k, b in bolge.items() if b == "govde" and k[0] - k[1] == s), key=lambda k: k[0] + k[1])


def balik(ad, pismis):
    tur = TURLER[ad]
    bolge = bolgeler(tur)
    renk = {k: (pismis_renk if pismis else ciy_renk)(ad, tur, *k, b) for k, b in bolge.items()}
    bas = tur["bas"]
    pis = tur["pismis"]
    derin = pis[3] if pismis else tur["sirt"][3]
    # solungaç çizgisi: başın arkasında gövdeye dik köşegen, sırt kenarından boğaza
    sg = bas - 4
    for k in kosegen(bolge, sg)[1:]:
        renk[k] = karistir(renk[k], derin, 0.6)
    # göğüs yüzgeci: solungacın arkasında, eksenin karın tarafında kuyruğa doğru yatan kısa şekil
    p0 = EKSEN + 1 if (sg + EKSEN + 1) % 2 == 0 else EKSEN + 2
    gx, gy = (sg + p0) // 2, (p0 - sg) // 2
    for i, (dx, dy) in enumerate(((-1, 0), (-1, 1), (-2, 1), (-2, 2))):
        k = (gx + dx, gy + dy)
        if bolge.get(k) == "govde":
            renk[k] = pis[2 + i % 2] if pismis else tur["yuzgec"][i % 2]
    # göz: 1 px koyu bebek + 1 px parlama (pişmişte bulanık beyaz); ağız: burnun karın ucunda koyu çentik
    ex, ey = kosegen(bolge, bas - 2)[1]
    renk[ex, ey] = PISMIS_GOZ if pismis else GOZ_BEBEK
    if not pismis:
        renk[ex - 1, ey] = GOZ_ISIK
    renk[kosegen(bolge, bas)[-1]] = derin
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for (x, y), c in renk.items():
        px[x, y] = (*sinirla(c), 255)
    # seçici dış çizgi: üst/sol kenar daha açık (ışık sol üstten), alt/sağ kenar (karın tarafı) en koyu
    for y in range(16):
        for x in range(16):
            if (x, y) in bolge:
                continue
            altta = [k for k in ((x - 1, y), (x, y - 1)) if k in bolge]
            ustte = [k for k in ((x + 1, y), (x, y + 1)) if k in bolge]
            if not altta and not ustte:
                continue
            ince = bolge[(altta or ustte)[0]] != "govde"
            if altta:
                taban, koyu = (pis[3], 0.72) if pismis else (tur["yuzgec"][1], 0.62) if ince else (tur["sirt"][3], 0.74)
            else:
                taban, koyu = (pis[3], 0.45) if pismis else (tur["yuzgec"][1], 0.45) if ince else (tur["sirt"][2], 0.5)
            px[x, y] = (*sinirla(karistir(taban, (8, 6, 12), koyu)), 255)
    if ad == "kutup_morinasi":
        # çene bıyığı: çenenin altındaki dış çizgiden sarkan iki piksel
        x, y = kosegen(bolge, bas - 3)[-1]
        while (x, y) in bolge:
            y += 1
        for k in ((x, y + 1), (x, y + 2)):
            if k not in bolge and k[1] < 16:
                px[k] = (*(pis[3] if pismis else (110, 94, 62)), 255)
    if ad == "donmus_ringa" and not pismis:
        # kırağı kenarı: sırt tarafı dış çizgide aralıklı beyaz; gövdede iki parıltı, sırtın üstünde dört kollu yıldız
        for y in range(16):
            for x in range(16):
                if (x, y) not in bolge and px[x, y][3] and ((x + 1, y) in bolge or (x, y + 1) in bolge) \
                        and (x, y - 1) not in bolge and (x - 1, y) not in bolge and (x + y) % 3 != 0:
                    px[x, y] = (*KIRAGI, 255)
        for k in ((8, 6), (5, 9)):
            if bolge.get(k) == "govde":
                px[k] = (255, 255, 255, 255)
        px[3, 3] = (255, 255, 255, 255)
        for k in ((2, 3), (4, 3), (3, 2), (3, 4)):
            px[k] = (*KIRAGI, 170)
    return img


def buz_parcasi():
    r = random.Random(0x5A4D)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Sivri kristal: dikey eksene göre eğik, alt geniş üst sivri.
    for y in range(1, 15):
        t = (y - 1) / 13
        yari = 0.6 + 3.6 * math.sin(math.pi * min(1, t * 1.05)) ** 0.8
        merkez = 8.5 - (y - 8) * 0.28
        for x in range(16):
            d = x + 0.5 - merkez
            if abs(d) <= yari:
                isik = 0.5 + 0.5 * (-d / max(yari, 0.1))
                renk = karistir((112, 176, 214), (226, 244, 252), isik)
                n = r.randint(-5, 5)
                px[x, y] = (*sinirla((renk[0] + n, renk[1] + n, renk[2] + n)), 236)
    kenar_renk = (54, 92, 126, 255)
    dolu = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for y in range(16):
        for x in range(16):
            if (x, y) not in dolu and any((x + a, y + b) in dolu for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = kenar_renk
    for x, y in ((7, 4), (7, 5), (6, 8)):
        px[x, y] = (255, 255, 255, 255)
    return img


def main():
    BLOK.mkdir(parents=True, exist_ok=True)
    ESYA.mkdir(parents=True, exist_ok=True)
    yazilan = []
    kenar().save(BLOK / "buz_deligi_kenar.png")
    yazilan.append(BLOK / "buz_deligi_kenar.png")
    for a in (1, 2, 3):
        yol = BLOK / f"buz_deligi_buzlanma_{a}.png"
        buzlanma(a).save(yol)
        yazilan.append(yol)
    for ad in TURLER:
        for pismis in (False, True):
            yol = ESYA / (("pismis_" if pismis else "") + ad + ".png")
            balik(ad, pismis).save(yol)
            yazilan.append(yol)
    buz_parcasi().save(ESYA / "buz_parcasi.png")
    yazilan.append(ESYA / "buz_parcasi.png")
    for y in yazilan:
        print(y.relative_to(KOK))
    ayrim_raporu()


def ayrim_raporu():
    """Pişmiş balık çiftleri: ortak opak piksellerde ortalama |dR|+|dG|+|dB| ve siluet farkı (XOR piksel)."""
    adlar = list(TURLER)
    resimler = {a: Image.open(ESYA / f"pismis_{a}.png").convert("RGBA").load() for a in adlar}
    for i, a in enumerate(adlar):
        for b in adlar[i + 1:]:
            pa, pb = resimler[a], resimler[b]
            fark, ortak, xor = 0, 0, 0
            for y in range(16):
                for x in range(16):
                    oa, ob = pa[x, y][3] > 0, pb[x, y][3] > 0
                    if oa and ob:
                        ortak += 1
                        fark += sum(abs(pa[x, y][k] - pb[x, y][k]) for k in range(3))
                    elif oa != ob:
                        xor += 1
            print(f"pismis {a} / {b}: renk farkı {fark / ortak:.1f}, siluet farkı {xor} px")


if __name__ == "__main__":
    main()
