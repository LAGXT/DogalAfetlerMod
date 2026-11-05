#!/usr/bin/env python3
"""GameTest şablonu dogalafetler:donma_alan (9x6x9 hava) yapı dosyasını üretir.

Donma GameTest'i sahneyi (taş zemin, 3 derin göl, çimen, çamur, yaprak, bitki) testte helper.setBlock ile kurar;
şablon yalnız temizlenen ve karşılaştırılan hacmi verir.

Kullanım (KÖK'ten): python3 tools/yapilar/donma_alan_uret.py
Çıktı: src/main/resources/data/dogalafetler/structures/donma_alan.nbt (gzip'li NBT, DataVersion 3465 = 1.20.1).
"""
import gzip
from pathlib import Path

from bos_yapi_uret import KOK, TAG_COMPOUND, TAG_END, ad, bilesik_listesi, metin, tamsayi, tamsayi_listesi

CIKTI = KOK / "src/main/resources/data/dogalafetler/structures/donma_alan.nbt"
BOYUT = (9, 6, 9)


def main():
    bloklar = [
        tamsayi_listesi("pos", [x, y, z]) + tamsayi("state", 0)
        for y in range(BOYUT[1])
        for z in range(BOYUT[2])
        for x in range(BOYUT[0])
    ]
    govde = (
        tamsayi("DataVersion", 3465)
        + tamsayi_listesi("size", list(BOYUT))
        + bilesik_listesi("palette", [metin("Name", "minecraft:air")])
        + bilesik_listesi("blocks", bloklar)
        + bilesik_listesi("entities", [])
    )
    kok = bytes([TAG_COMPOUND]) + ad("") + govde + bytes([TAG_END])
    CIKTI.parent.mkdir(parents=True, exist_ok=True)
    CIKTI.write_bytes(gzip.compress(kok, mtime=0))
    print(CIKTI)


if __name__ == "__main__":
    main()
