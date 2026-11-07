#!/usr/bin/env python3
"""Görsel otomasyonların ortak `OLCUM` satırlarından tek FPS tablosu (TSV; K-E29 (b), 5-matris #10).

Kullanım: python3 tools/senaryo/olcum_tablosu.py [-o tablo.tsv] [--desen 'latest-*.log,runClient-*.log'] <dizin|log> ...

Dizinler özyinelemeli taranır (varsayılan desenler `latest-*.log` ve `runClient-*.log`, virgülle ayrılır; tsunami istemci
log'unu yalnız `runClient-*.log` olarak saklar), dosyalar doğrudan okunur. Aynı klasörde aynı koşunun iki kopyası (`latest-<n>.log`
+ `runClient-<n>.log`) tek sayılır: (klasör, otomasyon, sahne, duvar_ms, cpu_ms, mspt, kare) aynı olan satırın ilki kalır. Satır biçimi (`client.core.GorselOtomasyon`):
`OLCUM <otomasyon> sahne=<ad> kademe=<Düşük|Orta|Yüksek|Ultra> duvar_ms=.. cpu_ms=.. mspt=.. gradle=.. yabanci=.. kare=..
taban=<ad|->`.

Tek FPS kuralı (K-E29 (b)) taban sahnesi verilmiş satırlara uygulanır. Taban, aynı log'da aynı otomasyonun o adlı satırıdır
(birden çoksa satır numarası en yakın olanı):
- taban duvar_ms ≥ 10 → `oran`: duvar_ms ≤ 2 × taban;
- taban duvar_ms < 10 → `mutlak`: cpu_ms ≤ 33,3 ve mspt < 50 ve yabanci ≤ 1.
Sonuç: `gecti` / `kaldi`; tabanı bulunamayan `taban_yok`; kare sayısı 0 olan `olcum_yok`; tabansız satır `bilgi`.
Log ya da aynı klasördeki başka bir `*.log` `MAKINE=LAGXT2` (pc2, `pc2_istemci.sh`) taşıyorsa kural uygulanmaz: satırlar `bilgi`,
kural sütunu `pc2` (yavaş CPU ve RV620 ile FPS/CPU ölçümü geçersiz; ölçüm ana makinede).
`tetik` sütunu duvar saati 30 FPS'in altındaki satırda `fps<30` yazar (K-E29 (b) görünür koşu tetikleyicisi; kural sonucunu değiştirmez).
Log başına `KARE-KAPISI` özeti (alınan / geçersiz kare, A-34) ve kural sayımları stderr'e yazılır.

Çıkış: 0 en az bir `OLCUM` satırı yazıldı, 1 hiç satır yok, 2 kullanım hatası.
"""
import argparse
import contextlib
import math
import re
import sys
from pathlib import Path

OLCUM = re.compile(r"\bOLCUM (\S+) (.*)$")
ALAN = re.compile(r"(\w+)=(\S+)")
KAPI = re.compile(r"\bKARE-KAPISI \S+ \S+ kapi=(acik|GECERSIZ)\b")
MAKINE = re.compile(r"\bMAKINE=(\S+)")
OLCUMSUZ_MAKINELER = ("LAGXT2",)
SUTUNLAR = ("log", "satir", "otomasyon", "sahne", "kademe", "duvar_ms", "fps", "cpu_ms", "mspt", "gradle", "yabanci",
            "kare", "taban", "taban_ms", "kural", "sonuc", "tetik")
TETIK_FPS = 30.0
ORAN_ESIGI_MS = 10.0
CPU_TAVANI_MS = 33.3
MSPT_TAVANI = 50.0
YABANCI_TAVANI = 1


def sayi(metin):
    try:
        return float(metin)
    except (TypeError, ValueError):
        return math.nan


def oku(log):
    """Bir log'un OLCUM satırları (alan sözlükleri) ve kare kapısı sayımları."""
    satirlar = []
    kapi = {"acik": 0, "GECERSIZ": 0}
    for no, satir in enumerate(log.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        k = KAPI.search(satir)
        if k:
            kapi[k.group(1)] += 1
            continue
        m = OLCUM.search(satir)
        if not m:
            continue
        alan = dict(ALAN.findall(m.group(2)))
        if "duvar_ms" not in alan or "cpu_ms" not in alan:
            continue
        alan.update(log=str(log), satir=no, otomasyon=m.group(1))
        satirlar.append(alan)
    return satirlar, kapi


def kural(satir, tabanlar):
    """(taban_ms, kural, sonuç) üçlüsü."""
    taban_adi = satir.get("taban", "-")
    if int(sayi(satir.get("kare", "0")) or 0) == 0 or math.isnan(sayi(satir["duvar_ms"])):
        return "", "-", "olcum_yok"
    if taban_adi == "-":
        return "", "-", "bilgi"
    adaylar = [t for t in tabanlar if t["otomasyon"] == satir["otomasyon"] and t["sahne"] == taban_adi
               and int(sayi(t.get("kare", "0")) or 0) > 0]
    if not adaylar:
        return "", "-", "taban_yok"
    taban = min(adaylar, key=lambda t: abs(t["satir"] - satir["satir"]))
    taban_ms = sayi(taban["duvar_ms"])
    if taban_ms >= ORAN_ESIGI_MS:
        gecti = sayi(satir["duvar_ms"]) <= 2 * taban_ms
        return taban["duvar_ms"], "oran", "gecti" if gecti else "kaldi"
    gecti = (sayi(satir["cpu_ms"]) <= CPU_TAVANI_MS and sayi(satir.get("mspt")) < MSPT_TAVANI
             and sayi(satir.get("yabanci")) <= YABANCI_TAVANI)
    return taban["duvar_ms"], "mutlak", "gecti" if gecti else "kaldi"


def makine(log):
    """Log'un ya da klasöründeki başka bir `*.log`'un ilk `MAKINE=<ad>` değeri; yoksa boş."""
    for yol in [log, *sorted(log.parent.glob("*.log"))]:
        with contextlib.suppress(OSError):
            bulunan = MAKINE.search(yol.read_text(encoding="utf-8", errors="replace"))
            if bulunan:
                return bulunan.group(1)
    return ""


def loglar(yollar, desen):
    bulunan = []
    for yol in yollar:
        p = Path(yol)
        if p.is_dir():
            for d in desen.split(","):
                bulunan.extend(sorted(p.rglob(d.strip())))
        elif p.is_file():
            bulunan.append(p)
        else:
            print(f"yok: {p}", file=sys.stderr)
    return list(dict.fromkeys(bulunan))


VARSAYILAN_DESEN = "latest-*.log,runClient-*.log"


def tablo(yollar, desen=VARSAYILAN_DESEN):
    """TSV satırları (başlık dahil) ve stderr özet satırları."""
    cikti = ["	".join(SUTUNLAR)]
    ozet = []
    gorulen = set()
    for log in loglar(yollar, desen):
        satirlar, kapi = oku(log)
        anahtarlar = {(str(log.parent), s["otomasyon"], s.get("sahne"), s["duvar_ms"], s["cpu_ms"], s.get("mspt"), s.get("kare"))
                      for s in satirlar}
        if satirlar and anahtarlar <= gorulen:
            ozet.append(f"{log}: aynı koşunun kopyası, atlandı")
            continue
        gorulen |= anahtarlar
        sonuclar = {}
        olcumsuz = makine(log) in OLCUMSUZ_MAKINELER
        for s in satirlar:
            taban_ms, ad, sonuc = kural(s, satirlar)
            if olcumsuz and sonuc != "olcum_yok":
                ad, sonuc = "pc2", "bilgi"
            sonuclar[sonuc] = sonuclar.get(sonuc, 0) + 1
            duvar = sayi(s["duvar_ms"])
            fps = f"{1000 / duvar:.1f}" if duvar > 0 else ""
            tetik = "fps<30" if duvar > 0 and 1000 / duvar < TETIK_FPS else ""
            deger = {**s, "fps": fps, "taban_ms": taban_ms, "kural": ad, "sonuc": sonuc, "tetik": tetik}
            cikti.append("	".join(str(deger.get(c, "")) for c in SUTUNLAR))
        if satirlar or any(kapi.values()):
            ozet.append(f"{log}: {len(satirlar)} OLCUM "
                        + " ".join(f"{k}={v}" for k, v in sorted(sonuclar.items()))
                        + f"; kare kapısı açık={kapi['acik']} geçersiz={kapi['GECERSIZ']}")
    return cikti, ozet


def main(argv=None):
    ap = argparse.ArgumentParser(description="OLCUM satırlarından tek FPS tablosu (TSV, K-E29 (b)).")
    ap.add_argument("yollar", nargs="+", help="dizin (özyinelemeli) ya da log dosyası")
    ap.add_argument("-o", "--cikti", help="TSV dosyası (yoksa stdout)")
    ap.add_argument("--desen", default=VARSAYILAN_DESEN, help="dizinlerde aranan dosya desenleri (virgülle)")
    a = ap.parse_args(argv)
    cikti, ozet = tablo(a.yollar, a.desen)
    metin = "\n".join(cikti) + "\n"
    if a.cikti:
        Path(a.cikti).parent.mkdir(parents=True, exist_ok=True)
        Path(a.cikti).write_text(metin, encoding="utf-8")
    else:
        sys.stdout.write(metin)
    for s in ozet:
        print(s, file=sys.stderr)
    print(f"toplam {len(cikti) - 1} OLCUM satırı" + (f" → {a.cikti}" if a.cikti else ""), file=sys.stderr)
    return 0 if len(cikti) > 1 else 1


if __name__ == "__main__":
    sys.exit(main())
