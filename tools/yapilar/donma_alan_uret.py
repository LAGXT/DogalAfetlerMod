#!/usr/bin/env python3
"""Donma GameTest şablonlarını üretir (hepsi hava; sahneyi test kendisi kurar):

- dogalafetler:donma_alan (9x6x9): taş zemin, 3 derin göl, çimen, çamur, yaprak, bitki sahnesi.
- dogalafetler:donma_gol (32x5x32): chunk sınırını kesen yuvarlak göl (kıyı uzaklığının chunk'tan bağımsızlığı).

Kullanım (KÖK'ten): python3 tools/yapilar/donma_alan_uret.py
Çıktı: src/main/resources/data/dogalafetler/structures/<ad>.nbt (gzip'li NBT, DataVersion 3465 = 1.20.1).
"""
import gzip

from bos_yapi_uret import KOK, TAG_COMPOUND, TAG_END, ad, bilesik_listesi, metin, tamsayi, tamsayi_listesi

KLASOR = KOK / "src/main/resources/data/dogalafetler/structures"
SABLONLAR = {"donma_alan": (9, 6, 9), "donma_gol": (32, 5, 32)}


def uret(adi, boyut):
    bloklar = [
        tamsayi_listesi("pos", [x, y, z]) + tamsayi("state", 0)
        for y in range(boyut[1])
        for z in range(boyut[2])
        for x in range(boyut[0])
    ]
    govde = (
        tamsayi("DataVersion", 3465)
        + tamsayi_listesi("size", list(boyut))
        + bilesik_listesi("palette", [metin("Name", "minecraft:air")])
        + bilesik_listesi("blocks", bloklar)
        + bilesik_listesi("entities", [])
    )
    kok = bytes([TAG_COMPOUND]) + ad("") + govde + bytes([TAG_END])
    cikti = KLASOR / f"{adi}.nbt"
    cikti.parent.mkdir(parents=True, exist_ok=True)
    cikti.write_bytes(gzip.compress(kok, mtime=0))
    print(cikti)


def main():
    for adi, boyut in SABLONLAR.items():
        uret(adi, boyut)


if __name__ == "__main__":
    main()
