#!/usr/bin/env python3
"""Buz altı balıkçılık dokularını üretir (16x16 PNG, prosedürel, sabit tohumlu; Mojang dokusu kopyalanmaz).

Blok (src/main/resources/assets/dogalafetler/textures/block/):
- buz_deligi_kenar.png: deliğin buz çerçevesi; açık mavi, ~%85 opak buz, beyaz çatlak ağı ve kırağı noktaları.
- buz_deligi_buzlanma_{1,2,3}.png: deliğin yeniden donma zarı; ortadaki 10x10 açıklıkta (piksel 3..12) kenarlardan içeri
  büyüyen yarı saydam beyaz-mavi buz iğneleri. Aşama 1 kenar şeridi, 2 yarısı, 3 neredeyse kapalı.

Eşya (src/main/resources/assets/dogalafetler/textures/item/):
- buz_alabaligi, kutup_morinasi, derin_soguk_somonu, buz_kefali, donmus_ringa (+ pismis_ önekli beşi): çapraz duran balık
  (kuyruk sol altta, baş sağ üstte, vanilla balıklarıyla aynı duruş). Türler gövde biçimi, sırt/karın rengi ve desenle
  ayrılır: alabalık pembe yan şerit + koyu benekler; morina zeytin-kahve alacalı; somon koyu lacivert sırt + gümüş yan,
  daha iri; kefal gri ve boyuna koyu çizgiler; ringa parlak mavi + beyaz kırağı kenarı ve parıltı. Pişmişler aynı biçimde,
  kızarmış kahve-altın tonlu ve ızgara izli; pişmiş ringa daha altın.
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

# Tür: sırt, karın, en geniş yarı genişlik, desen.
TURLER = {
    "buz_alabaligi": dict(sirt=(66, 108, 122), karin=(222, 232, 236), genislik=2.7, desen="alabalik"),
    "kutup_morinasi": dict(sirt=(116, 112, 84), karin=(226, 220, 198), genislik=2.9, desen="morina"),
    "derin_soguk_somonu": dict(sirt=(36, 56, 96), karin=(232, 238, 244), genislik=3.3, desen="somon"),
    "buz_kefali": dict(sirt=(92, 102, 114), karin=(212, 218, 224), genislik=2.6, desen="kefal"),
    "donmus_ringa": dict(sirt=(58, 112, 166), karin=(234, 244, 252), genislik=2.4, desen="ringa"),
}
T0 = (2.6, 13.2)
H0 = (13.1, 2.9)


def eksen(x, y):
    """Piksel merkezinin balık çerçevesi: u kuyruktan (0) başa (1), v sırt (+) / karın (-) yönünde piksel."""
    dx, dy = H0[0] - T0[0], H0[1] - T0[1]
    boy = math.hypot(dx, dy)
    ex, ey = dx / boy, dy / boy
    px_, py_ = x + 0.5 - T0[0], y + 0.5 - T0[1]
    u = (px_ * ex + py_ * ey) / boy
    v = px_ * ey - py_ * ex  # sol üst taraf (sırt) pozitif
    return u, v


def govde_yari(u, g):
    if u < 0.24 or u > 1.0:
        return 0.0
    t = (u - 0.24) / 0.76
    return g * math.sin(math.pi * min(1.0, t * 0.96 + 0.04)) ** 0.75


def kuyruk(u, v):
    if u < 0.0 or u > 0.28:
        return False
    yay = (0.28 - u) * 9.5
    return abs(v) <= yay + 0.6 and not (u < 0.12 and abs(v) < (0.12 - u) * 7)


def balik(ad, pismis):
    tur = TURLER[ad]
    r = random.Random(sum(map(ord, ad)) * (7 if pismis else 3))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    maske = [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            u, v = eksen(x, y)
            w = govde_yari(u, tur["genislik"])
            ic = abs(v) <= w or kuyruk(u, v)
            # sırt yüzgeci
            if 0.45 < u < 0.7 and w < v <= w + 1.1 and (x + y) % 2 == 0:
                ic = True
            if not ic:
                continue
            maske[y][x] = True
            t = max(0.0, min(1.0, (v + tur["genislik"]) / (2 * tur["genislik"])))
            renk = karistir(tur["karin"], tur["sirt"], t ** 1.2)
            d = tur["desen"]
            if d == "alabalik":
                if abs(v) < 0.55 and u > 0.3:
                    renk = karistir(renk, (232, 132, 146), 0.75)
                if v > 0.9 and r.random() < 0.28:
                    renk = karistir(renk, (34, 44, 46), 0.8)
            elif d == "morina":
                renk = karistir(renk, (88, 80, 58), r.random() * 0.35 if v > -0.5 else 0)
                if abs(v + 0.2) < 0.35 and u > 0.3:
                    renk = karistir(renk, (236, 230, 204), 0.6)
            elif d == "somon":
                if -0.6 < v < 0.8:
                    renk = karistir(renk, (182, 196, 212), 0.55)
                if v > 1.2 and r.random() < 0.15:
                    renk = karistir(renk, (20, 30, 52), 0.7)
            elif d == "kefal":
                if v > -1.0 and round(v * 1.6) % 2 == 0:
                    renk = karistir(renk, (54, 60, 70), 0.55)
            elif d == "ringa":
                if -0.4 < v < 1.0:
                    renk = karistir(renk, (196, 224, 242), 0.45)
            if kuyruk(u, v) and u < 0.26:
                renk = karistir(renk, tur["sirt"], 0.5)
            if pismis:
                hedef = (176, 112, 52) if d == "ringa" else (150, 92, 46)
                renk = karistir(renk, hedef, 0.62)
                renk = karistir(renk, (214, 160, 92), max(0.0, (0.5 - abs(v) / 4)) * 0.4)
                # ızgara izleri: eksene dik çizgiler
                if u > 0.32 and int(u * 15) % 3 == 0:
                    renk = karistir(renk, (70, 40, 20), 0.55)
                if u < 0.24:
                    renk = karistir(renk, (88, 54, 28), 0.5)
            n = r.randint(-6, 6)
            px[x, y] = (*sinirla((renk[0] + n, renk[1] + n, renk[2] + n)), 255)
    # koyu dış çizgi
    kenar_renk = (44, 30, 20) if pismis else (24, 36, 52)
    for y in range(16):
        for x in range(16):
            if maske[y][x]:
                continue
            komsu = any(0 <= x + a < 16 and 0 <= y + b < 16 and maske[y + b][x + a]
                        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if komsu:
                px[x, y] = (*kenar_renk, 255)
    # göz
    for y in range(16):
        for x in range(16):
            u, v = eksen(x, y)
            if 0.84 < u < 0.92 and 0.15 < v < 1.25 and maske[y][x]:
                px[x, y] = (12, 12, 16, 255) if not pismis else (238, 232, 214, 255)
                break
    # ringa: kırağı kenarı ve parıltı (çiğde)
    if tur["desen"] == "ringa" and not pismis:
        for y in range(16):
            for x in range(16):
                if px[x, y][3] and px[x, y][:3] == kenar_renk and (x * 3 + y) % 4 == 0:
                    px[x, y] = (236, 246, 255, 255)
        for x, y in ((7, 7), (9, 5), (5, 9)):
            if maske[y][x]:
                px[x, y] = (255, 255, 255, 255)
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


if __name__ == "__main__":
    main()
