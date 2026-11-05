#!/usr/bin/env python3
"""runServer'ın ürettiği sabit seed'li dünyayı istemcinin Quick Play test dünyasına çevirir.

Kullanım (KÖK'ten): .venv/bin/python tools/dunya/test_dunyasi_hazirla.py

- run-server/test (server.properties: level-name=test, level-seed=20261105) → run/saves/test kopyalanır
  (session.lock hariç; hedef varsa önce silinir).
- level.dat: LevelName="test" ve allowCommands=1 (tek oyunculuda /dogalafet için hileler açık).
- run/options.txt: pauseOnLostFocus:false (pencere odak kaybedince dünya durmasın).
Sunucu kapalıyken çalıştırılır.
"""
import shutil
import sys
from pathlib import Path

import nbtlib

KOK = Path(__file__).resolve().parents[2]
KAYNAK = KOK / "run-server" / "test"
HEDEF = KOK / "run" / "saves" / "test"
SECENEKLER = KOK / "run" / "options.txt"


def secenegi_yaz():
    SECENEKLER.parent.mkdir(parents=True, exist_ok=True)
    satirlar = SECENEKLER.read_text(encoding="utf-8").splitlines() if SECENEKLER.exists() else []
    satirlar = [s for s in satirlar if not s.startswith("pauseOnLostFocus:")]
    satirlar.append("pauseOnLostFocus:false")
    SECENEKLER.write_text("\n".join(satirlar) + "\n", encoding="utf-8")


def main():
    if not (KAYNAK / "level.dat").exists():
        sys.exit(f"{KAYNAK}/level.dat yok: önce ./gradlew runServer ile dünyayı üret")
    if HEDEF.exists():
        shutil.rmtree(HEDEF)
    shutil.copytree(KAYNAK, HEDEF, ignore=shutil.ignore_patterns("session.lock"))

    level = nbtlib.load(HEDEF / "level.dat")
    veri = level["Data"]
    veri["LevelName"] = nbtlib.String("test")
    veri["allowCommands"] = nbtlib.Byte(1)
    level.save()

    secenegi_yaz()
    kontrol = nbtlib.load(HEDEF / "level.dat")["Data"]
    print(f"{HEDEF}: LevelName={kontrol['LevelName']} allowCommands={int(kontrol['allowCommands'])} "
          f"seed={int(kontrol['WorldGenSettings']['seed'])}")


if __name__ == "__main__":
    main()
