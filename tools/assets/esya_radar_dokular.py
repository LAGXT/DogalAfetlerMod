#!/usr/bin/env python3
"""Hava Radarı ve Sismograf eşya dokuları (SARTNAME madde 13a-b, N19).

Dört 16x16 RGBA doku, elle yerleştirilmiş piksel sanatı; rastgelelik yok, çıktı bayt düzeyinde tekrarlanır:
  hava_radari            yeşil CRT ekranlı, demir gövdeli el cihazı; tarama çizgisi, kırmızı afet noktası, bakır tuşlar
  gelismis_hava_radari   aynı cihaz mor-mavi çerçeveli, camgöbeği ekran, mor uçlu anten ve Kozmik Çekirdek taşı
  sismograf              taş kaide üzerinde kâğıt sarılı tambur; tüy kalem kâğıda sismik iz çiziyor
  hassas_sismograf       aynı düzen mor-mavi tambur kapakları, mor mürekkep ve Kozmik Çekirdek taşı

Kullanım (worktree kökünden):
    ../../.venv/bin/python tools/assets/esya_radar_dokular.py            # dokuları yazar
    ../../.venv/bin/python tools/assets/esya_radar_dokular.py --onizleme # ayrıca 16x büyütülmüş önizleme (yalnız inceleme)

Çıktı: src/main/resources/assets/dogalafetler/textures/item/<ad>.png (16x16, RGBA).
Değiştirmek için: aşağıdaki desen dizilerinde her karakter bir pikseldir; harf → renk eşlemesi PALET'tedir.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/assets/dogalafetler/textures/item"

# Ortak palet. "." saydam.
PALET = {
    ".": (0, 0, 0, 0),
    "k": (28, 30, 34, 255),      # dış hat
    "d": (62, 66, 74, 255),      # demir koyu
    "m": (104, 110, 120, 255),   # demir orta
    "a": (150, 156, 166, 255),   # demir açık
    "p": (206, 212, 220, 255),   # demir parlama
    "e": (8, 34, 16, 255),       # ekran zemini
    "g": (30, 96, 46, 255),      # ekran halkası
    "G": (92, 236, 120, 255),    # tarama çizgisi
    "h": (176, 255, 190, 255),   # tarama ucu
    "r": (232, 64, 42, 255),     # afet noktası
    "c": (192, 104, 60, 255),    # bakır
    "C": (236, 156, 104, 255),   # bakır parlama
    "n": (120, 62, 36, 255),     # bakır koyu
    # Gelişmiş vurgular (mor-mavi)
    "v": (86, 60, 170, 255),     # mor koyu
    "V": (122, 99, 224, 255),    # mor
    "b": (79, 163, 255, 255),    # mavi
    "B": (170, 214, 255, 255),   # mavi parlama
    "q": (6, 30, 40, 255),       # camgöbeği ekran zemini
    "Q": (64, 220, 230, 255),    # camgöbeği tarama
    "z": (210, 160, 255, 255),   # çekirdek parıltısı
    # Sismograf
    "s": (88, 88, 92, 255),      # taş koyu
    "S": (128, 128, 132, 255),   # taş
    "t": (164, 164, 168, 255),   # taş açık
    "w": (236, 228, 204, 255),   # kâğıt
    "W": (250, 246, 232, 255),   # kâğıt parlama
    "y": (196, 184, 152, 255),   # kâğıt gölge
    "i": (28, 44, 110, 255),     # mürekkep
    "I": (96, 52, 168, 255),     # mor mürekkep
    "f": (244, 244, 240, 255),   # tüy
    "F": (196, 198, 204, 255),   # tüy gölge
    "o": (60, 52, 46, 255),      # tüy sapı / kalem ucu
}

DESENLER: dict[str, list[str]] = {
    "hava_radari": [
        "............kr..",
        "............ka..",
        "............ka..",
        "..kkkkkkkkkkkk..",
        ".kpppaaaaaaaamk.",
        ".kakkkkkkkkkkdk.",
        ".kakeeggggeekdk.",
        ".kakegeeGhgekdk.",
        ".kakgeeGeeegkdk.",
        ".kakgereeeegkdk.",
        ".kakegeeeegekdk.",
        ".kakeeggggeekdk.",
        ".kakkkkkkkkkkdk.",
        ".kamCcmmmCcmmdk.",
        ".kdnnnddddnnndk.",
        "..kkkkkkkkkkkk..",
    ],
    "gelismis_hava_radari": [
        "............kz..",
        "............kV..",
        "............kV..",
        "..kkkkkkkkkkkk..",
        ".kBBBbbbbbbbbVk.",
        ".kbkkkkkkkkkkvk.",
        ".kbkqqbbbbqqkvk.",
        ".kbkqbqqQBbqkvk.",
        ".kbkbqqQqqqbkvk.",
        ".kbkbqrqqqqbkvk.",
        ".kbkqbqqqqbqkvk.",
        ".kbkqqbbbbqqkvk.",
        ".kbkkkkkkkkkkvk.",
        ".kbmCcmzVmCcmvk.",
        ".kvvvvvvvvvvvvk.",
        "..kkkkkkkkkkkk..",
    ],
    "sismograf": [
        "............ffo.",
        "...........fFo..",
        "..........fFo...",
        ".........fFo....",
        "........fFo.....",
        ".......fFo......",
        ".kkkkkkkokkkkkk.",
        ".kaWWWWWiWWWWak.",
        ".kawwiwwiwwiwak.",
        ".kaiiwiiwiiwiak.",
        ".kmwwwwwwwwwwmk.",
        ".kdyyyyyyyyyydk.",
        "..kkkkkSSkkkkk..",
        "..kttttttttttk..",
        ".kSSScCCcSSSSSk.",
        ".kkkkkkkkkkkkkk.",
    ],
    "hassas_sismograf": [
        "............ffo.",
        "...........fFo..",
        "..........fFo...",
        ".........fFo....",
        "........fFo.....",
        ".......fFo......",
        ".kkkkkkkokkkkkk.",
        ".kbWWWWWIWWWWVk.",
        ".kbwwIwwIwwIwVk.",
        ".kbIIwIIwIIwIVk.",
        ".kVwwwwwwwwwwvk.",
        ".kvyyyyyyyyyyvk.",
        "..kkkkkSSkkkkk..",
        "..kttttttttttk..",
        ".kSSScCzVcSSSSk.",
        ".kkkkkkkkkkkkkk.",
    ],
}


def doku(desen: list[str]) -> Image.Image:
    if len(desen) != 16 or any(len(satir) != 16 for satir in desen):
        raise ValueError("desen 16x16 olmalı")
    resim = Image.new("RGBA", (16, 16))
    for y, satir in enumerate(desen):
        for x, harf in enumerate(satir):
            resim.putpixel((x, y), PALET[harf])
    return resim


def main() -> int:
    ayristirici = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ayristirici.add_argument("--onizleme", type=Path, nargs="?", const=Path("/tmp/esya_radar_onizleme.png"),
                             help="16x büyütülmüş yan yana önizleme yolu")
    secenek = ayristirici.parse_args()
    CIKTI.mkdir(parents=True, exist_ok=True)
    resimler = []
    for ad, desen in DESENLER.items():
        resim = doku(desen)
        resim.save(CIKTI / f"{ad}.png", optimize=False)
        resimler.append(resim)
        print(f"{ad}.png")
    if secenek.onizleme:
        tuval = Image.new("RGBA", (16 * 16 * len(resimler) + 16 * (len(resimler) - 1), 256), (54, 58, 66, 255))
        for i, resim in enumerate(resimler):
            tuval.paste(resim.resize((256, 256), Image.NEAREST), (i * 272, 0), resim.resize((256, 256), Image.NEAREST))
        tuval.save(secenek.onizleme)
        print(f"önizleme: {secenek.onizleme}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
