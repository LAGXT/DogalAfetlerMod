#!/usr/bin/env python3
"""Kömürleşmiş kütüğün ısı kademesi dokularını ve soyulmuş kütüğü üretir (16x16 PNG, prosedürel, sabit tohumlu).

İsı kademesi (blok durumu `isi` 0..6) dört görünüme eşlenir (`KutukIsisi.Gorunum`):
- sönmüş (isi 0): `komurlesmis_kutuk_sonuk[_top]` — kül grişi, parlayan çatlak yok, çatlaklar koyu gri.
- ılık (isi 1–2): mevcut `komurlesmis_kutuk[_top]` (`pulsar_dokular.py`), bu betik dokunmaz.
- sıcak (isi 3–4): `komurlesmis_kutuk_sicak[_top]` — daha çok ve daha parlak turuncu-sarı çatlak, kor tonlu gövde.
- çok sıcak (isi 5–6): `komurlesmis_kutuk_cok_sicak[_top]` — kor kızılı gövdede kalın beyaz-sarı parlak çatlaklar.
- `soyulmus_komurlesmis_kutuk[_top]`: kabuğu soyulmuş, pürüzsüz soğuk kömür (parlama yok).

Kullanım (worktree kökünden): python tools/assets/kutuk_dokulari.py
Çıktı: src/main/resources/assets/dogalafetler/textures/block/
"""
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from PIL import Image  # noqa: E402

from pulsar_dokular import BLOK_KLASORU, BOY, _sinirla  # noqa: E402


def _sonuk_renk(t):
    return (_sinirla(40 + 34 * t), _sinirla(36 + 30 * t), _sinirla(36 + 28 * t))


def _sicak_renk(t):
    return (_sinirla(170 + 85 * t), _sinirla(45 + 190 * t * t), _sinirla(10 + 90 * t ** 3))


def _cok_sicak_renk(t):
    return (255, _sinirla(110 + 145 * t), _sinirla(30 + 190 * t * t))


def _yan(tohum, taban, ton, catlak, ana, kalin, yatay, kul_benekleri=0):
    r = random.Random(tohum)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    lif = [r.randint(-5, 5) for _ in range(BOY)]
    for y in range(BOY):
        for x in range(BOY):
            v = taban + lif[x] + r.randint(-4, 4) + (6 if x % 4 == 0 else 0)
            px[x, y] = (_sinirla(v + ton[0]), _sinirla(v + ton[1]), _sinirla(v + ton[2]), 255)
    for _ in range(ana):
        x = r.randrange(BOY)
        y = r.randrange(BOY)
        boy = r.randint(7, 13)
        for k in range(boy):
            t = math.sin(math.pi * (k + 0.5) / boy)
            px[x % BOY, (y + k) % BOY] = (*catlak(t), 255)
            if t > 0.6 and r.random() < 0.5 + 0.5 * kalin:
                px[(x + 1) % BOY, (y + k) % BOY] = (*catlak(t * 0.5 + 0.2 * kalin), 255)
            if kalin > 0.7 and r.random() < 0.6:
                px[(x - 1) % BOY, (y + k) % BOY] = (*catlak(t * 0.35), 255)
            if r.random() < 0.18:
                x += r.choice((-1, 1))
    for _ in range(yatay):
        x, y = r.randrange(BOY), r.randrange(BOY)
        for k in range(r.randint(2, 4)):
            px[(x + k) % BOY, y] = (*catlak(0.35 + 0.4 * r.random()), 255)
    for _ in range(kul_benekleri):
        x, y = r.randrange(BOY), r.randrange(BOY)
        v = r.randint(62, 84)
        px[x, y] = (v, v - 2, v - 2, 255)
    return img


def _top(tohum, taban, ton, catlak, cekirdek, isik_carpani, isinli):
    r = random.Random(tohum)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    m = (BOY - 1) / 2
    for y in range(BOY):
        for x in range(BOY):
            d = math.hypot(x - m, y - m)
            if d > 7.2:
                v = taban - 8 + r.randint(-3, 3)
                px[x, y] = (_sinirla(v + ton[0] * 0.5), _sinirla(v + ton[1] * 0.5), _sinirla(v + ton[2] * 0.5), 255)
                continue
            halka = 0.5 + 0.5 * math.cos(d * 2.1 + r.uniform(-0.25, 0.25))
            v = taban + 14 * halka + r.randint(-4, 4)
            px[x, y] = (_sinirla(v + ton[0]), _sinirla(v + ton[1]), _sinirla(v + ton[2]), 255)
            sicak = max(0.0, 1.0 - d / cekirdek) if cekirdek > 0 else 0.0
            if sicak > 0:
                c = catlak(min(1.0, sicak * isik_carpani))
                px[x, y] = tuple(_sinirla(a * sicak + b * (1 - sicak)) for a, b in zip(c, px[x, y][:3])) + (255,)
            elif isinli and halka > 0.9 and r.random() < 0.45:
                px[x, y] = (*catlak(0.3 + 0.5 * r.random()), 255)
    for _ in range(6 if isinli else 4):
        aci = r.uniform(0, 2 * math.pi)
        for k in range(3, 8 if isinli else 6):
            x = int(round(m + math.cos(aci) * k))
            y = int(round(m + math.sin(aci) * k))
            px[x, y] = (*catlak(1.0 - (k - 3) / (7 if isinli else 5)), 255)
    return img


def sonuk():
    return _yan(0xC0A1, 31, (2, 0, -1), _sonuk_renk, ana=3, kalin=0.0, yatay=4, kul_benekleri=9)


def sonuk_top():
    return _top(0xC0A2, 28, (2, 0, 0), _sonuk_renk, 0.0, 1.0, False)


def sicak():
    return _yan(0xC0A1, 27, (16, 2, -2), _sicak_renk, ana=5, kalin=0.8, yatay=7)


def sicak_top():
    return _top(0xC0A2, 30, (14, 2, -2), _sicak_renk, 4.4, 1.3, True)


def cok_sicak():
    return _yan(0xC0A1, 36, (62, 2, -14), _cok_sicak_renk, ana=8, kalin=1.0, yatay=11)


def cok_sicak_top():
    return _top(0xC0A2, 38, (64, 4, -12), _cok_sicak_renk, 5.8, 1.5, True)


def soyulmus():
    r = random.Random(0xC0A4)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    lif = [r.randint(-4, 4) for _ in range(BOY)]
    for y in range(BOY):
        for x in range(BOY):
            v = 42 + lif[x] + r.randint(-3, 3)
            if x % 5 == 2:
                v -= 7
            px[x, y] = (_sinirla(v + 3), _sinirla(v), _sinirla(v - 2), 255)
    for _ in range(3):
        x, y = r.randrange(BOY), r.randrange(BOY)
        for k in range(r.randint(2, 4)):
            v = r.randint(18, 26)
            px[(x + k) % BOY, y] = (v + 2, v, v - 1, 255)
    return img


def soyulmus_top():
    r = random.Random(0xC0A5)
    img = Image.new("RGBA", (BOY, BOY))
    px = img.load()
    m = (BOY - 1) / 2
    for y in range(BOY):
        for x in range(BOY):
            d = math.hypot(x - m, y - m)
            if d > 7.2:
                v = 36 + r.randint(-3, 3)
            else:
                halka = 0.5 + 0.5 * math.cos(d * 1.9 + r.uniform(-0.2, 0.2))
                v = 46 + 16 * halka + r.randint(-3, 3)
                if d < 1.4:
                    v = 30 + r.randint(-3, 3)
            px[x, y] = (_sinirla(v + 4), _sinirla(v), _sinirla(v - 3), 255)
    return img


def main():
    BLOK_KLASORU.mkdir(parents=True, exist_ok=True)
    for ad, img in (("komurlesmis_kutuk_sonuk", sonuk()), ("komurlesmis_kutuk_sonuk_top", sonuk_top()),
                    ("komurlesmis_kutuk_sicak", sicak()), ("komurlesmis_kutuk_sicak_top", sicak_top()),
                    ("komurlesmis_kutuk_cok_sicak", cok_sicak()), ("komurlesmis_kutuk_cok_sicak_top", cok_sicak_top()),
                    ("soyulmus_komurlesmis_kutuk", soyulmus()), ("soyulmus_komurlesmis_kutuk_top", soyulmus_top())):
        yol = BLOK_KLASORU / f"{ad}.png"
        img.save(yol, optimize=True)
        print(yol)


if __name__ == "__main__":
    main()
