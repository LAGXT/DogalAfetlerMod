#!/usr/bin/env python3
"""Meteor ve termostat dokuları (N19), deterministik: aynı girdi her zaman aynı baytları üretir.

Kullanım (worktree kökünden): KÖK/.venv/bin/python tools/assets/meteor_dokular.py

Çıktılar (src/main/resources/assets/dogalafetler/):
- textures/item/meteorit_parcasi.png  16x16  koyu gözenekli kaya, turuncu-kor damarlar
- textures/item/kozmik_cekirdek.png   16x16  mor-mavi kristal, parlak çekirdek
- textures/item/meteorit_kulcesi.png  16x16  koyu demir-mavi külçe, turuncu kıvılcım benekleri
- textures/item/termostat_plakasi.png 16x16  bakır-altın plaka, devre izleri ve kızıltaş noktası
- textures/particle/meteor_isik_0..3.png   8x8   yumuşak ışık (plazma, kıvılcım; renk kodda)
- textures/particle/meteor_duman_0..3.png 16x16  dağınık duman/toz lekesi (gri tonu kodda)
- textures/particle/meteor_yildiz_0..1.png 8x8   dört kollu parıltı (ödüller)
- textures/entity/meteor_plazmasi.png  64x64  radyal ışık (additive: parlaklık = RGB)
- textures/entity/meteor_izi.png       64x16  iz şeridi: u boyunca baştan kuyruğa söner, v ortası parlak
- particles/*.json (8 parçacık tanımı)
Renkleri değiştirmek için aşağıdaki RENK sözlüklerini düzenleyip betiği yeniden çalıştırın.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
VARLIK = KOK / "src/main/resources/assets/dogalafetler"
ESYA = VARLIK / "textures/item"
PARCACIK = VARLIK / "textures/particle"
VARLIK_DOKU = VARLIK / "textures/entity"
TANIM = VARLIK / "particles"


def kaydet(resim, yol):
    yol.parent.mkdir(parents=True, exist_ok=True)
    resim.save(yol, optimize=False, compress_level=9)
    print(yol.relative_to(KOK), resim.size)


def karistir(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def meteorit_parcasi():
    r = random.Random(20261106)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    # Düzensiz kaya silueti: merkezden yarıçapı açıya göre dalgalanan leke.
    merkez = (7.6, 8.2)
    dalga = [r.uniform(4.6, 6.4) for _ in range(12)]
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - merkez[0], y + 0.5 - merkez[1]
            aci = (math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 12
            i = int(aci) % 12
            f = aci - int(aci)
            sinir = dalga[i] * (1 - f) + dalga[(i + 1) % 12] * f
            d = math.hypot(dx, dy)
            if d > sinir:
                continue
            # Koyu, gözenekli taban; kenara doğru koyulaşır, sol üst ışık.
            isik = 0.5 - (dx + dy) / 14
            taban = karistir((34, 30, 32), (86, 76, 70), max(0.0, min(1.0, isik + r.uniform(-0.15, 0.15))))
            if r.random() < 0.16:
                taban = karistir(taban, (14, 12, 14), 0.7)  # gözenek
            if d > sinir - 1.0:
                taban = karistir(taban, (16, 14, 16), 0.55)  # kenar
            p[x, y] = taban + (255,)
    # Turuncu-kor damarlar: iki kırık çizgi.
    for basla, adimlar in (((4, 6), [(1, 0), (1, 1), (1, 0), (1, 1), (1, 0), (0, 1)]),
                           ((6, 12), [(1, -1), (1, 0), (1, -1), (1, -1), (1, 0)])):
        x, y = basla
        for i, (ax, ay) in enumerate(adimlar):
            if p[x, y][3]:
                p[x, y] = karistir((255, 120, 24), (255, 214, 96), 0.5 if i % 2 else 0.15) + (255,)
            x, y = x + ax, y + ay
    for x, y in ((8, 7), (10, 9), (5, 9)):
        if p[x, y][3]:
            p[x, y] = (255, 236, 150, 255)  # sıcak kor noktaları
    return img


def kozmik_cekirdek():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    # Altıgen-elmas kristal: |dx|/5 + |dy|/7 <= 1.
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            k = abs(dx) / 5.6 + abs(dy) / 7.4
            if k > 1:
                continue
            yuz = 0.5 + 0.5 * math.sin(dx * 1.3 + dy * 0.9)  # yüzey faset parlaması
            ic = max(0.0, 1 - k)
            renk = karistir((52, 26, 120), (96, 70, 230), yuz)
            renk = karistir(renk, (150, 200, 255), ic ** 1.6)
            if k > 0.86:
                renk = karistir(renk, (30, 14, 70), 0.6)  # koyu kenar
            if dx < 0 and abs(dx + dy * 0.4) < 0.7:
                renk = karistir(renk, (220, 210, 255), 0.6)  # parlak faset çizgisi
            p[x, y] = renk + (255,)
    for x, y, c in ((7, 7, (255, 255, 255)), (8, 7, (230, 240, 255)), (7, 8, (210, 225, 255)), (8, 8, (190, 170, 255))):
        p[x, y] = c + (255,)
    return img


def kulce():
    r = random.Random(3)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    # Vanilla külçe silueti: eğik paralelkenar (üst yüz açık, ön yüz koyu).
    for y in range(4, 13):
        for x in range(1, 15):
            sol = 1 + max(0, 7 - y) if y < 7 else 1
            sag = 14 - max(0, y - 9)
            if not (sol <= x <= sag):
                continue
            if y <= 6:
                c = karistir((120, 128, 146), (168, 176, 196), (x - sol) / max(1, sag - sol))
            else:
                c = karistir((58, 62, 78), (94, 100, 120), 1 - (y - 7) / 6)
            if r.random() < 0.12:
                c = karistir(c, (255, 140, 40), 0.75)  # meteor kıvılcım beneği
            if y in (4, 12) or x in (sol, sag):
                c = karistir(c, (28, 30, 40), 0.6)
            p[x, y] = c + (255,)
    return img


def plaka():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            kenar = x in (2, 13) or y in (2, 13)
            c = karistir((176, 98, 52), (226, 170, 74), (x + y - 4) / 20)
            if kenar:
                c = karistir(c, (92, 50, 28), 0.65)
            p[x, y] = c + (255,)
    # Altın devre izleri.
    for x, y in [(4, 4), (5, 4), (6, 4), (6, 5), (6, 6), (7, 6), (8, 6), (9, 6), (9, 7), (9, 8), (10, 8), (11, 8),
                 (4, 10), (5, 10), (5, 9), (6, 9), (7, 9), (7, 10), (7, 11), (8, 11), (9, 11), (11, 4), (11, 5)]:
        p[x, y] = (255, 226, 120, 255)
    for x, y in ((4, 4), (11, 8), (4, 10), (9, 11), (11, 4)):
        p[x, y] = (255, 246, 196, 255)  # lehim noktaları
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        p[x, y] = (210, 30, 26, 255)  # kızıltaş çip
    p[8, 7] = (255, 90, 70, 255)
    return img


def isik(boy, keskinlik):
    img = Image.new("RGBA", (boy, boy), (0, 0, 0, 0))
    p = img.load()
    m = (boy - 1) / 2
    for y in range(boy):
        for x in range(boy):
            d = math.hypot(x - m, y - m) / (boy / 2)
            a = max(0.0, 1 - d) ** keskinlik
            v = int(255 * min(1.0, a * 1.2))
            p[x, y] = (255, 255, 255, v)
    return img


def duman(tohum):
    r = random.Random(tohum)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    lekeler = [(r.uniform(5, 11), r.uniform(5, 11), r.uniform(2.5, 5.0)) for _ in range(5)]
    for y in range(16):
        for x in range(16):
            a = 0.0
            for lx, ly, lr in lekeler:
                a += max(0.0, 1 - math.hypot(x + 0.5 - lx, y + 0.5 - ly) / lr) ** 1.5
            a = min(1.0, a) * (0.75 + 0.25 * r.random())
            v = int(225 + 30 * r.random())
            p[x, y] = (v, v, v, int(255 * a))
    return img


def yildiz(kol):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    p = img.load()
    for y in range(8):
        for x in range(8):
            dx, dy = x - 3.5, y - 3.5
            capraz = min(abs(dx), abs(dy))
            d = math.hypot(dx, dy)
            a = max(0.0, 1 - capraz / 0.9) * max(0.0, 1 - d / kol) + max(0.0, 1 - d / 1.6)
            p[x, y] = (255, 255, 255, int(255 * min(1.0, a)))
    return img


def plazma():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    p = img.load()
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 31.5, y - 31.5) / 32
            v = max(0.0, 1 - d) ** 2.2 + 0.6 * max(0.0, 1 - d / 0.25) ** 2
            c = int(255 * min(1.0, v))
            p[x, y] = (c, c, c, 255)
    return img


def iz():
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 255))
    p = img.load()
    for y in range(16):
        for x in range(64):
            u = x / 63
            v = (y + 0.5) / 16
            enine = math.exp(-((v - 0.5) / 0.2) ** 2)
            boyuna = (1 - u) ** 0.8
            c = int(255 * min(1.0, enine * boyuna * 1.1))
            p[x, y] = (c, c, c, 255)
    return img


def tanim(ad, dokular):
    yol = TANIM / f"{ad}.json"
    yol.parent.mkdir(parents=True, exist_ok=True)
    yol.write_text(json.dumps({"textures": [f"dogalafetler:{d}" for d in dokular]}, indent=2) + "\n")
    print(yol.relative_to(KOK))


def main():
    kaydet(meteorit_parcasi(), ESYA / "meteorit_parcasi.png")
    kaydet(kozmik_cekirdek(), ESYA / "kozmik_cekirdek.png")
    kaydet(kulce(), ESYA / "meteorit_kulcesi.png")
    kaydet(plaka(), ESYA / "termostat_plakasi.png")
    for i, k in enumerate((1.2, 1.6, 2.2, 3.0)):
        kaydet(isik(8, k), PARCACIK / f"meteor_isik_{i}.png")
    for i in range(4):
        kaydet(duman(100 + i), PARCACIK / f"meteor_duman_{i}.png")
    for i, kol in enumerate((3.8, 2.8)):
        kaydet(yildiz(kol), PARCACIK / f"meteor_yildiz_{i}.png")
    kaydet(plazma(), VARLIK_DOKU / "meteor_plazmasi.png")
    kaydet(iz(), VARLIK_DOKU / "meteor_izi.png")
    isiklar = [f"meteor_isik_{i}" for i in range(4)]
    dumanlar = [f"meteor_duman_{i}" for i in range(4)]
    yildizlar = [f"meteor_yildiz_{i}" for i in range(2)]
    for ad in ("meteor_plazmasi", "meteor_plazmasi_ay", "meteor_kivilcimi", "meteor_kivilcimi_ay"):
        tanim(ad, isiklar)
    for ad in ("meteor_dumani", "meteor_tozu"):
        tanim(ad, dumanlar)
    for ad in ("meteorit_parlamasi", "kozmik_parilti"):
        tanim(ad, yildizlar)


if __name__ == "__main__":
    main()
