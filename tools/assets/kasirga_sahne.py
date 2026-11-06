#!/usr/bin/env python3
"""GameTest şablonu dogalafetler:kasirga_sahne (32x16x32 hava) yapı dosyasını üretir.

Kasırga oyun testleri sahnesini (taş taban, yaprak katmanı, domuz) bu kutunun içine kendisi kurar; şablon yerleştirilince
kutu havayla temizlenir. S2 kasırganın blok kaldırma alanı (çekim yarıçapı 15 × 0,6 = 9 blok) kutuya sığar.

Kullanım (worktree kökünden): python3 tools/assets/kasirga_sahne.py
Çıktı: src/main/resources/data/dogalafetler/structures/kasirga_sahne.nbt (gzip'li NBT, DataVersion 3465 = 1.20.1).
"""
import gzip
import struct
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/data/dogalafetler/structures/kasirga_sahne.nbt"
BOYUT = (32, 16, 32)

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
    bloklar = [tamsayi_listesi("pos", [x, y, z]) + tamsayi("state", 0)
               for y in range(BOYUT[1]) for z in range(BOYUT[2]) for x in range(BOYUT[0])]
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
