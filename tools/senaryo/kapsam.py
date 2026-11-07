#!/usr/bin/env python3
"""Değişen yola göre GameTest kapsamı (K-E51). Tek satır yazar: `TEST_FILTRE=<regex>`, `filtresiz` ya da `yok`.

Harita (sırayla; bir yol `filtresiz` ise sonuç `filtresiz`, yoksa filtrelerin birleşimi, hiçbiri yoksa `yok`):
  mixin (`/mixin/`, `*.mixins.json`), build.gradle, settings.gradle, gradle.properties, gradle/   → filtresiz
  <mod>/client/**                                                                                  → yok
  <mod>/core/**, <mod>/network/**, <mod>/DogalAfetler.java                                         → filtresiz
  <mod>/afet/<x>/**                                     → o klasördeki `*OyunTestleri` sınıfları (yoksa filtresiz)
  src/*/resources/assets/**, src/test/**, tools/**, docs/**, .vibe/**, kökteki *.md, LICENSE, .gitignore, gradlew* → yok
  diğer her yol (öteki paketler, src/*/resources/data, src/generated/**)                           → filtresiz (güvenli varsayılan)
<mod> = src/main/java/io/github/kadirberkpolat1/dogalafetler. Filtresiz koşu yalnız `main` birleştirmesinde ve bir kez.

Kullanım (proje ya da worktree kökünden):
  kapsam.py <taban>..<uç>                  `git diff --name-only` aralığı (üç noktalı da olur)
  kapsam.py --dosya <yol> [--dosya <yol>]  yol listesiyle (simülasyon)
  -v                                       her yolun sınıfı stderr'e
"""
import argparse
import subprocess
import sys
from pathlib import Path

W = Path(__file__).resolve().parents[2]
MOD = "src/main/java/io/github/kadirberkpolat1/dogalafetler/"
FILTRESIZ_DOSYA = {"build.gradle", "settings.gradle", "gradle.properties"}
YOK_KOK = {"LICENSE", ".gitignore", "gradlew", "gradlew.bat"}
YOK_ON_EK = ("tools/", "docs/", ".vibe/", "src/test/", "src/main/resources/assets/", "src/generated/resources/assets/")


def sinifla(yol):
    """("filtresiz" | "yok" | "filtre", sınıf listesi, gerekçe)."""
    if "/mixin/" in yol or yol.endswith(".mixins.json"):
        return "filtresiz", [], "mixin"
    if yol in FILTRESIZ_DOSYA or yol.startswith("gradle/"):
        return "filtresiz", [], "Gradle yapılandırması"
    if yol.startswith(MOD):
        ic = yol[len(MOD):]
        if ic.startswith("client/"):
            return "yok", [], "istemci kodu"
        if ic.startswith(("core/", "network/")) or ic == "DogalAfetler.java":
            return "filtresiz", [], "çekirdek/ağ/ana sınıf"
        parca = ic.split("/")
        if parca[0] == "afet" and len(parca) > 2:
            siniflar = sorted(p.stem for p in (W / MOD / "afet" / parca[1]).glob("*OyunTestleri.java"))
            if siniflar:
                return "filtre", siniflar, f"afet/{parca[1]}"
            return "filtresiz", [], f"afet/{parca[1]} GameTest sınıfı bulunamadı"
        return "filtresiz", [], "diğer paket (güvenli varsayılan)"
    if yol.startswith(YOK_ON_EK) or yol in YOK_KOK or ("/" not in yol and yol.endswith(".md")):
        return "yok", [], "GameTest girdisi değil"
    return "filtresiz", [], "tanınmayan yol (güvenli varsayılan)"


def kapsam(dosyalar, ayrinti=False):
    filtre, filtresiz = set(), False
    for yol in dosyalar:
        tur, siniflar, neden = sinifla(yol)
        if ayrinti:
            print(f"{tur:9} {yol} ({neden}{': ' + '|'.join(siniflar) if siniflar else ''})", file=sys.stderr)
        filtresiz |= tur == "filtresiz"
        filtre.update(siniflar)
    if filtresiz:
        return "filtresiz"
    return f"TEST_FILTRE={'|'.join(sorted(filtre))}" if filtre else "yok"


def main():
    ayr = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ayr.add_argument("aralik", nargs="?", help="<taban>..<uç>")
    ayr.add_argument("--dosya", action="append", default=[], help="değişen yol (tekrarlanabilir)")
    ayr.add_argument("-v", action="store_true", help="yol yol sınıf stderr'e")
    a = ayr.parse_args()
    if bool(a.aralik) == bool(a.dosya):
        ayr.error("ya <taban>..<uç> ya da --dosya ver")
    dosyalar = a.dosya
    if a.aralik:
        cikti = subprocess.run(["/usr/bin/git", "-C", str(W), "diff", "--name-only", a.aralik], capture_output=True,
                               text=True)
        if cikti.returncode:
            print(f"KAPSAM: git diff {a.aralik}: {cikti.stderr.strip()}", file=sys.stderr)
            return 2
        dosyalar = cikti.stdout.splitlines()
    print(kapsam(dosyalar, a.v))
    return 0


if __name__ == "__main__":
    sys.exit(main())
