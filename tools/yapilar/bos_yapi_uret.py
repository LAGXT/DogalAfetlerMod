#!/usr/bin/env python3
"""GameTest şablonu dogalafetler:bos (1x1x1, tek hava bloğu) yapı dosyasını üretir.

Kullanım (KÖK'ten): python3 tools/yapilar/bos_yapi_uret.py
Çıktı: src/main/resources/data/dogalafetler/structures/bos.nbt (gzip'li NBT, DataVersion 3465 = 1.20.1).
Yalnız standart kütüphane kullanır.
"""
import gzip
import struct
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/data/dogalafetler/structures/bos.nbt"

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def ad(metin):
    veri = metin.encode("utf-8")
    return struct.pack(">H", len(veri)) + veri


def tamsayi(isim, deger):
    return bytes([TAG_INT]) + ad(isim) + struct.pack(">i", deger)


def metin(isim, deger):
    return bytes([TAG_STRING]) + ad(isim) + ad(deger)


def tamsayi_listesi(isim, degerler):
    return bytes([TAG_LIST]) + ad(isim) + bytes([TAG_INT]) + struct.pack(">i", len(degerler)) + b"".join(
        struct.pack(">i", d) for d in degerler)


def bilesik_listesi(isim, govdeler):
    tur = TAG_COMPOUND if govdeler else TAG_END
    return bytes([TAG_LIST]) + ad(isim) + bytes([tur]) + struct.pack(">i", len(govdeler)) + b"".join(
        g + bytes([TAG_END]) for g in govdeler)


def main():
    govde = (
        tamsayi("DataVersion", 3465)
        + tamsayi_listesi("size", [1, 1, 1])
        + bilesik_listesi("palette", [metin("Name", "minecraft:air")])
        + bilesik_listesi("blocks", [tamsayi_listesi("pos", [0, 0, 0]) + tamsayi("state", 0)])
        + bilesik_listesi("entities", [])
    )
    kok = bytes([TAG_COMPOUND]) + ad("") + govde + bytes([TAG_END])
    CIKTI.parent.mkdir(parents=True, exist_ok=True)
    # mtime=0: aynı girdi her zaman aynı baytları üretir.
    CIKTI.write_bytes(gzip.compress(kok, mtime=0))
    print(CIKTI)


if __name__ == "__main__":
    main()
