#!/usr/bin/env python3
"""Afet Defteri eşya dokusu (journal paketi).

Tek 16x16 RGBA doku, elle yerleştirilmiş piksel sanatı; rastgelelik yok, çıktı bayt düzeyinde tekrarlanır:
  afet_defteri   bordo deri kapaklı, altın şeritli defter; kapakta sarı yıldırım, sağ kenarda krem sayfalar

Kullanım (proje kökünden):
    .venv/bin/python tools/assets/defter_dokulari.py              # dokuyu yazar
    .venv/bin/python tools/assets/defter_dokulari.py --onizleme   # ayrıca 16x büyütülmüş önizleme (yalnız inceleme)

Çıktı: src/main/resources/assets/dogalafetler/textures/item/afet_defteri.png (16x16, RGBA).
Değiştirmek için: aşağıdaki desen dizisinde her karakter bir pikseldir; harf → renk eşlemesi PALET'tedir.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/assets/dogalafetler/textures/item"

PALET = {
    ".": (0, 0, 0, 0),          # saydam
    "k": (28, 16, 10, 255),     # dış hat
    "r": (128, 44, 30, 255),    # kapak
    "R": (166, 70, 48, 255),    # kapak açık
    "d": (86, 28, 20, 255),     # kapak koyu (cilt)
    "g": (212, 160, 52, 255),   # altın
    "G": (255, 224, 112, 255),  # altın parlak (yıldırım)
    "w": (240, 228, 196, 255),  # sayfa
    "y": (200, 184, 144, 255),  # sayfa gölge
}

DESEN = [
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kddRRRRRRRRRwk.",
    ".kddgggggggggyk.",
    ".kddrrrrrGGrrwk.",
    ".kddrrrrGGrrrwk.",
    ".kddrrrGGGrrrwk.",
    ".kddrrrGGGGGrwk.",
    ".kddrrrrrGGrrwk.",
    ".kddrrrrGGrrrwk.",
    ".kddrrrGGrrrrwk.",
    ".kddgggggggggyk.",
    ".kddrrrrrrrrrwk.",
    ".kkkkkkkkkkkkkk.",
    "................",
    "................",
]


def olustur() -> Image.Image:
    goruntu = Image.new("RGBA", (16, 16))
    for y, satir in enumerate(DESEN):
        if len(satir) != 16:
            raise SystemExit(f"satır {y} 16 piksel değil: {len(satir)}")
        for x, harf in enumerate(satir):
            goruntu.putpixel((x, y), PALET[harf])
    if len(DESEN) != 16:
        raise SystemExit(f"desen 16 satır olmalı: {len(DESEN)}")
    return goruntu


def main() -> None:
    ayristirici = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ayristirici.add_argument("--onizleme", action="store_true", help="16x büyütülmüş önizleme de yaz")
    secenekler = ayristirici.parse_args()
    goruntu = olustur()
    CIKTI.mkdir(parents=True, exist_ok=True)
    goruntu.save(CIKTI / "afet_defteri.png")
    print(f"yazıldı: {CIKTI / 'afet_defteri.png'}")
    if secenekler.onizleme:
        onizleme = goruntu.resize((256, 256), Image.NEAREST)
        (KOK / "build").mkdir(parents=True, exist_ok=True)
        onizleme.save(KOK / "build" / "afet_defteri_onizleme.png")


if __name__ == "__main__":
    main()
