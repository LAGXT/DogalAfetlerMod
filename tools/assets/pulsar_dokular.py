#!/usr/bin/env python3
"""Pulsar cehenneminin blok ve parçacık dokularını üretir (16x16 PNG, prosedürel, sabit tohumlu; Mojang dokusu kopyalanmaz).

- kul.png: gri-siyah kül; ince taneli gürültü, seyrek açık gri pul ve koyu kömür kırıntısı. Kül katmanı ve kül bloğu
  aynı dokuyu kullanır. Kenarlar sarmal (karo tekrarında dikis yok).
- komurlesmis_kutuk.png (yan): dikey lifli neredeyse siyah kömür; lifler boyunca ve arasında kızıl-turuncu parlayan
  çatlaklar (iç ateş). Çatlak rengi kenardan içe koyu kızıldan turuncu-sarıya.
- komurlesmis_kutuk_top.png (kesit): kömür halkaları, merkezde ve halkalar arasında kor gibi parlayan çekirdek, ışınsal
  çatlaklar.
- komurlesmis_tahta.png: dört yatay kömür tahtası (her biri 4 piksel); yatay lif, tahta aralarında koyu derz, tahta
  başına kaydırılmış dikey ek yeri ve seyrek sönük kor parıltısı (kütükten daha soluk: tahta soğumuş kömürdür).
- Parçacıklar (textures/particle):
  - kul_yagisi_0..3: düzensiz kül pulları (küçük/büyük, kıvrık kenar); açık gri tonda, istemci rengi koyulaştırır.
  - kor_0..3: yaşla küçülen parlak kıvılcım (merkez beyaz-sarı, kenar turuncu-kızıl, yumuşak hale).
  - kul_tozu_0..3: yumuşak, dağınık toz bulutu lekeleri.
  Saydam piksellerin rengi parçacığın ortalama rengi kalır (süzmede koyu hale olmasın).

Kullanım (worktree kökünden): KOK/.venv/bin/python tools/assets/pulsar_dokular.py
Çıktı: src/main/resources/assets/dogalafetler/textures/block/{kul,komurlesmis_kutuk,komurlesmis_kutuk_top,
komurlesmis_tahta}.png,
src/main/resources/assets/dogalafetler/textures/particle/{kul_yagisi,kor,kul_tozu}_{0..3}.png
"""
import math
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
BLOK_KLASORU = KOK / "src/main/resources/assets/dogalafetler/textures/block"
PARCACIK_KLASORU = KOK / "src/main/resources/assets/dogalafetler/textures/particle"
BOY = 16
ALT_ORNEK = 4


def _sinirla(v):
    return max(0, min(255, int(round(v))))


def _deger_gurultusu(r, olcek):
    """Sarmal (16 piksellik) yumuşak değer gürültüsü: olcek x olcek ızgarada rastgele değerler, çiftdoğrusal ara."""
    izgara = [[r.random() for _ in range(olcek)] for _ in range(olcek)]
    adim = BOY / olcek

    def oku(x, y):
        gx, gy = x / adim, y / adim
        x0, y0 = int(gx) % olcek, int(gy) % olcek
        x1, y1 = (x0 + 1) % olcek, (y0 + 1) % olcek
        tx, ty = gx - int(gx), gy - int(gy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = izgara[y0][x0] * (1 - tx) + izgara[y0][x1] * tx
        b = izgara[y1][x0] * (1 - tx) + izgara[y1][x1] * tx
        return a * (1 - ty) + b * ty

    return oku


def kul():
    r = random.Random(0x4B55)
    kaba = _deger_gurultusu(r, 4)
    ince = _deger_gurultusu(r, 8)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    for y in range(BOY):
        for x in range(BOY):
            v = 52 + 34 * kaba(x, y) + 18 * ince(x, y) + r.randint(-7, 7)
            px[x, y] = (_sinirla(v + 2), _sinirla(v), _sinirla(v - 2), 255)
    for _ in range(14):
        x, y = r.randrange(BOY), r.randrange(BOY)
        v = r.randint(118, 150)
        px[x, y] = (v, v - 2, v - 4, 255)
        if r.random() < 0.4:
            px[(x + 1) % BOY, y] = (v - 18, v - 20, v - 22, 255)
    for _ in range(12):
        x, y = r.randrange(BOY), r.randrange(BOY)
        v = r.randint(16, 28)
        px[x, y] = (v + 2, v, v, 255)
    return img


def _kor_rengi(t):
    """0 (çatlak kenarı, koyu kızıl) → 1 (çatlak ortası, turuncu-sarı)."""
    return (_sinirla(120 + 135 * t), _sinirla(18 + 150 * t * t), _sinirla(8 + 40 * t ** 3))


def komurlesmis_kutuk():
    r = random.Random(0xC0A1)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    lif = [r.randint(-5, 5) for _ in range(BOY)]
    for y in range(BOY):
        for x in range(BOY):
            v = 24 + lif[x] + r.randint(-4, 4) + (6 if x % 4 == 0 else 0)
            px[x, y] = (_sinirla(v + 3), _sinirla(v), _sinirla(v - 1), 255)
    # Dikey ana çatlaklar: lif boyunca yer yer yana kayan, parlaklığı ortada en yüksek.
    for _ in range(3):
        x = r.randrange(BOY)
        y = r.randrange(BOY)
        boy = r.randint(7, 13)
        for k in range(boy):
            t = math.sin(math.pi * (k + 0.5) / boy)
            px[x % BOY, (y + k) % BOY] = (*_kor_rengi(t), 255)
            if t > 0.6 and r.random() < 0.5:
                px[(x + 1) % BOY, (y + k) % BOY] = (*_kor_rengi(t * 0.45), 255)
            if r.random() < 0.18:
                x += r.choice((-1, 1))
    # Kısa yatay kırılmalar (kavrulmuş odunun pul pul çatlağı).
    for _ in range(4):
        x, y = r.randrange(BOY), r.randrange(BOY)
        for k in range(r.randint(2, 4)):
            px[(x + k) % BOY, y] = (*_kor_rengi(0.35 + 0.3 * r.random()), 255)
    return img


def komurlesmis_kutuk_top():
    r = random.Random(0xC0A2)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    m = (BOY - 1) / 2
    for y in range(BOY):
        for x in range(BOY):
            d = math.hypot(x - m, y - m)
            if d > 7.2:
                v = 18 + r.randint(-3, 3)
                px[x, y] = (_sinirla(v + 2), _sinirla(v), _sinirla(v), 255)
                continue
            halka = 0.5 + 0.5 * math.cos(d * 2.1 + r.uniform(-0.25, 0.25))
            v = 26 + 14 * halka + r.randint(-4, 4)
            px[x, y] = (_sinirla(v + 3), _sinirla(v), _sinirla(v - 1), 255)
            sicak = max(0.0, 1.0 - d / 3.2)
            if sicak > 0:
                c = _kor_rengi(min(1.0, sicak * 1.2))
                px[x, y] = tuple(_sinirla(a * sicak + b * (1 - sicak)) for a, b in zip(c, px[x, y][:3])) + (255,)
            elif halka > 0.93 and r.random() < 0.35:
                px[x, y] = (*_kor_rengi(0.3 + 0.3 * r.random()), 255)
    # Işınsal çatlaklar.
    for _ in range(5):
        aci = r.uniform(0, 2 * math.pi)
        for k in range(3, 7):
            x = int(round(m + math.cos(aci) * k))
            y = int(round(m + math.sin(aci) * k))
            px[x, y] = (*_kor_rengi(1.0 - (k - 3) / 5), 255)
    return img


def komurlesmis_tahta():
    r = random.Random(0xC0A3)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    for tahta in range(4):
        ust = tahta * 4
        ton = r.randint(-4, 4)
        lif = [r.randint(-4, 4) for _ in range(4)]
        ek = (r.randrange(BOY), r.random() < 0.5)
        for dy in range(4):
            y = ust + dy
            for x in range(BOY):
                v = 34 + ton + lif[dy] + r.randint(-3, 3)
                if dy == 3:
                    v = 16 + r.randint(-2, 2)
                elif dy == 0:
                    v += 5
                if x == ek[0] and dy < 3:
                    v = 18 + r.randint(-2, 2)
                px[x, y] = (_sinirla(v + 3), _sinirla(v), _sinirla(v - 1), 255)
        if ek[1]:
            x = (ek[0] + r.randint(4, 11)) % BOY
            px[x, ust + r.randint(1, 2)] = (*_kor_rengi(0.15 + 0.2 * r.random()), 255)
    return img


def _kare(ortu, renk, kenar_alfa=1.0):
    """ortu(x, y) → 0..1 (merkez 0,0, piksel birimi); renk(x, y, a) → (r, g, b); 4x4 alt örnekli alfa."""
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    m = BOY / 2
    for y in range(BOY):
        for x in range(BOY):
            toplam = 0.0
            for sy in range(ALT_ORNEK):
                for sx in range(ALT_ORNEK):
                    toplam += ortu(x + (sx + 0.5) / ALT_ORNEK - m, y + (sy + 0.5) / ALT_ORNEK - m)
            a = toplam / (ALT_ORNEK * ALT_ORNEK) * kenar_alfa
            cx, cy = x + 0.5 - m, y + 0.5 - m
            px[x, y] = (*renk(cx, cy, a), _sinirla(255 * a))
    return img


def kul_pullari():
    r = random.Random(0x4B59)
    kareler = []
    for i in range(4):
        yaricap = (2.2, 3.4, 4.6, 5.8)[i]
        koseler = r.randint(5, 8)
        fazlar = [r.uniform(0.55, 1.0) for _ in range(koseler)]
        donus = r.uniform(0, 2 * math.pi)
        sikisma = r.uniform(0.45, 0.75)

        def ortu(x, y, yaricap=yaricap, koseler=koseler, fazlar=fazlar, donus=donus, sikisma=sikisma):
            ca, sa = math.cos(donus), math.sin(donus)
            u, v = x * ca + y * sa, (-x * sa + y * ca) / sikisma
            aci = (math.atan2(v, u) % (2 * math.pi)) / (2 * math.pi) * koseler
            k0 = int(aci) % koseler
            t = aci - int(aci)
            sinir = yaricap * (fazlar[k0] * (1 - t) + fazlar[(k0 + 1) % koseler] * t)
            return max(0.0, min(1.0, (sinir - math.hypot(u, v)) / 0.8 + 0.5))

        def renk(cx, cy, a, yaricap=yaricap):
            t = min(1.0, math.hypot(cx, cy) / max(1.0, yaricap))
            v = 210 - 50 * t
            return (_sinirla(v), _sinirla(v - 4), _sinirla(v - 8))

        kareler.append(_kare(ortu, renk))
    return kareler


def kor_kareleri():
    kareler = []
    for i in range(4):
        cekirdek = (2.4, 1.9, 1.4, 0.9)[i]
        hale = (5.5, 4.6, 3.6, 2.6)[i]

        def ortu(x, y, cekirdek=cekirdek, hale=hale):
            d = math.hypot(x, y)
            ic = max(0.0, min(1.0, (cekirdek - d) / 0.7 + 0.5))
            dis = max(0.0, 1.0 - d / hale) ** 2 * 0.55
            return max(ic, dis)

        def renk(cx, cy, a, cekirdek=cekirdek, hale=hale):
            d = math.hypot(cx, cy)
            t = max(0.0, min(1.0, 1.0 - d / hale))
            return (255, _sinirla(150 + 105 * t ** 1.5), _sinirla(60 + 180 * t ** 3))

        kareler.append(_kare(ortu, renk))
    return kareler


def toz_kareleri():
    r = random.Random(0x4B54)
    kareler = []
    for _ in range(4):
        lekeler = [(r.uniform(-3.5, 3.5), r.uniform(-3.5, 3.5), r.uniform(1.6, 3.6), r.uniform(0.35, 0.8))
                   for _ in range(r.randint(4, 7))]

        def ortu(x, y, lekeler=lekeler):
            a = 0.0
            for lx, ly, lr, lg in lekeler:
                a = max(a, lg * max(0.0, 1.0 - math.hypot(x - lx, y - ly) / lr) ** 1.5)
            return a

        def renk(cx, cy, a):
            return (196, 192, 186)

        kareler.append(_kare(ortu, renk))
    return kareler


def main():
    BLOK_KLASORU.mkdir(parents=True, exist_ok=True)
    for ad, img in (("kul", kul()), ("komurlesmis_kutuk", komurlesmis_kutuk()),
                    ("komurlesmis_kutuk_top", komurlesmis_kutuk_top()), ("komurlesmis_tahta", komurlesmis_tahta())):
        yol = BLOK_KLASORU / f"{ad}.png"
        img.save(yol, optimize=True)
        print(yol)
    PARCACIK_KLASORU.mkdir(parents=True, exist_ok=True)
    for ad, kareler in (("kul_yagisi", kul_pullari()), ("kor", kor_kareleri()), ("kul_tozu", toz_kareleri())):
        for i, img in enumerate(kareler):
            yol = PARCACIK_KLASORU / f"{ad}_{i}.png"
            img.save(yol, optimize=True)
            print(yol)


if __name__ == "__main__":
    main()
