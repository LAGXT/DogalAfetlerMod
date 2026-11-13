#!/usr/bin/env python3
"""Punchy uyumu: Kutup Oltası'nın birinci kişi fırlatma ve çekme animasyonları (K-E79).

Kullanım (kökünden): KOK/.venv/bin/python tools/assets/punchy_kutup_animasyon.py
Çıktı (deterministik): src/main/resources/assets/minecraft/punchy/animations/dogalafetler_kutup_oltasi.animation.json

Biçim Punchy'nin resource pack animasyon biçimidir (Bedrock .animation.json, bkz. Punchy wiki "Specific Animation & More Tuning System"): {"animations": {ad: {animation_length, bones: {kemik: {rotation|position: {zaman: [x, y, z]}}}, timeline}}}. Kemikler
vanilla klip ile aynıdır (right_arm, itemgrip_right, left_arm, itemgrip_left); sol el sağ elin yüzde sağ/sol aynasıdır
(rotasyonda y ve z işaret değiştirir, konumda x). Zaman ekseni vanilla ile aynı hızda oynar ("anim_speed_3.2").

Klipler (compat dosyası {assets/minecraft/punchy/compat/dogalafetler_kutup_oltasi.json} çağırır):
  kutup_oltasi_firlat  Fırlatma: kol geriye ve vanilla'dan yükseğe kalkar, kısa bir üşüme titremesi, sert bir kamçı ile
                       ileri fırlatır (olta ucu aşırı salınır), vanilla'nın fırlatılmış duruşuna oturur, sonra geri çekilir.
  kutup_oltasi_cek     Çekme: kol ve olta ucu geride büyük bir yay yapar, üşüyen küçük titremeler, sonra hızlı bir sert çekme.
"""
import json
from pathlib import Path

KOK = Path(__file__).resolve().parents[2]
CIKTI = KOK / "src/main/resources/assets/minecraft/punchy/animations/dogalafetler_kutup_oltasi.animation.json"


def anahtarlar(liste):
    """[(zaman, [x, y, z]), ...] -> {\"zaman\": [x, y, z]} (zaman biçimi: gereksiz sıfır yok)."""
    return {("%g" % t): [round(float(v), 4) for v in d] for t, d in liste}


def ayna_rot(liste):
    return [(t, [d[0], -d[1], -d[2]]) for t, d in liste]


def ayna_poz(liste):
    return [(t, [-d[0], d[1], d[2]]) for t, d in liste]


def klip(uzunluk, kol_rot, kol_poz, tut_rot, hiz=3.2):
    sag = {
        "right_arm": {"rotation": anahtarlar(kol_rot), "position": anahtarlar(kol_poz)},
        "itemgrip_right": {"rotation": anahtarlar(tut_rot), "position": anahtarlar([(0, [0, 0, 0]), (uzunluk, [0, 0, 0])])},
    }
    sol = {
        "left_arm": {"rotation": anahtarlar(ayna_rot(kol_rot)), "position": anahtarlar(ayna_poz(kol_poz))},
        "itemgrip_left": {"rotation": anahtarlar(ayna_rot(tut_rot)), "position": anahtarlar([(0, [0, 0, 0]), (uzunluk, [0, 0, 0])])},
    }
    return {
        "animation_length": uzunluk,
        "bones": {**sag, **sol},
        "timeline": {"0.0": "use_keyframes_v2;anim_speed_%g;" % hiz},
    }


def firlat():
    """Fırlatma. Vanilla'nın fırlatılmış son duruşu (kol x 32,6 / konum z -5,4 / tutuş x 13,28) korunur: sonraki hareketler
    (olta çekme, boşa dönüş) aynı yerden başlar."""
    kol_rot = [
        (0, [0, 0, 0]),
        (0.10, [-20, -2, 2.5]),
        (0.22, [-52, -5, 6.5]),
        (0.30, [-66, -6.5, 9]),       # geri kurma tepesi (vanilla -47)
        (0.34, [-64, -6, 9.8]),       # üşüme titremesi
        (0.38, [-67, -6.9, 8.2]),
        (0.42, [-65, -6.2, 9.6]),
        (0.46, [-66, -6.6, 8.8]),
        (0.50, [-40, -3, 5]),         # kopma
        (0.54, [-6, 2, 0.5]),
        (0.58, [24, 5, -1.8]),
        (0.62, [41, 6.3, -2.8]),      # aşırı salınım
        (0.70, [37, 5.9, -2.4]),
        (0.80, [33.5, 5.7, -2.1]),
        (1.50, [32.9, 5.68, -2.11]),
        (2.375, [32.5893, 5.6712, -2.1209]),
        (2.60, [20, 4, -2.5]),
        (3.00, [-32, -3.5, -3.8]),
        (3.30, [-38, -3.8, -3.8]),
        (3.90, [-16, -1.5, -1.7]),
        (4.40, [0, 0, 0]),
        (4.70, [1.2, 0.2, -0.1]),
        (5.10, [0, 0, 0]),
        (5.9167, [0, 0, 0]),
    ]
    kol_poz = [
        (0, [0, 0, 0]),
        (0.10, [0, -0.5, 1.2]),
        (0.22, [0, -1.2, 2.4]),
        (0.30, [0, -1.6, 3.0]),
        (0.46, [0, -1.5, 2.9]),
        (0.50, [0, -1.0, 0.5]),
        (0.54, [0, -0.6, -2.6]),
        (0.62, [0, -0.2, -6.2]),
        (0.70, [0, 0.15, -6.4]),
        (0.80, [0, 0.2, -5.8]),
        (1.1875, [0, 0, -5.4]),
        (2.375, [0, -1.3, -5.4]),
        (2.60, [0, -1.25, -4.8]),
        (3.1667, [0, -1.0, -0.8]),
        (4.40, [0, -0.2, 0]),
        (5.9167, [0, 0, 0]),
    ]
    tut_rot = [
        (0, [0, 0, 0]),
        (0.22, [-10, 0, 0]),
        (0.30, [-26, 0, -1]),         # olta ucu geride
        (0.46, [-24, 0.5, -1]),
        (0.50, [-5, 2, -1]),
        (0.58, [26, 9, -2.4]),
        (0.66, [35, 10, -2.5]),       # uç öne kamçı gibi fırlar
        (0.80, [18, 9.3, -2.3]),
        (0.95, [13.28, 9.28, -2.34]),
        (3.1667, [13.28, 9.28, -2.34]),
        (3.2778, [9.84, 6.87, -1.74]),
        (3.3889, [3.44, 2.41, -0.61]),
        (3.5, [0, 0, 0]),
        (5.9167, [0, 0, 0]),
    ]
    return klip(5.9167, kol_rot, kol_poz, tut_rot)


def cek():
    """Çekme (şamandıra ısırdığında): vanilla gibi kol kaldırılır ve bekler (burada üşüyen küçük titremelerle), sonra sert çekilir."""
    kol_rot = [
        (0, [0, 0, 0]),
        (0.12, [-28, -2.5, 4.5]),
        (0.25, [-58, -5, 8.6]),
        (0.60, [-57.5, -5, 8.8]),
        (0.90, [-56.5, -4.8, 8.0]),
        (1.20, [-57.5, -5, 8.8]),
        (1.50, [-56.5, -4.8, 8.2]),
        (1.80, [-57, -4.9, 8.5]),
        (2.0417, [-57, -4.9, 8.5]),
        (2.20, [-30, -2.5, 5]),
        (2.40, [4, 1.5, 0.5]),
        (2.55, [20, 3, -1.2]),
        (2.75, [22, 3.2, -1.5]),      # sert çekme tepesi (vanilla 15,9)
        (3.00, [12, 2, -0.5]),
        (3.30, [8, 1.5, 3]),
        (3.60, [6, 1, 4.5]),
        (4.20, [2.5, 0.4, 2]),
        (5, [0, 0, 0]),
    ]
    kol_poz = [
        (0, [0, 0, 0]),
        (0.15, [0, -1.0, 1.6]),
        (0.375, [0, -1.1, -1.2]),
        (2.0417, [0, -1.1, -1.8]),
        (2.12, [0, -1.5, -1.86]),
        (2.30, [0, -3.2, -2.0]),
        (2.50, [0, -5.0, -2.1]),
        (2.70, [0, -5.2, -2.1]),
        (3.00, [0, -4.4, -2.0]),
        (3.80, [0, -2.2, -2.0]),
        (4.60, [0, -0.4, -0.5]),
        (5, [0, 0, 0]),
    ]
    tut_rot = [
        (0, [0, 0, 0]),
        (0.375, [-20, -1.5, -2.3]),
        (2.0417, [-20, -1.5, -2.3]),
        (2.30, [-10, 0, 0]),
        (2.55, [16, 1.5, -1]),        # olta ucu sert çekmede öne baş eğer
        (2.80, [10, 1, -0.8]),
        (3.30, [-4, -0.5, -0.5]),
        (3.7917, [0, 0, 0]),
        (5, [0, 0, 0]),
    ]
    return klip(5, kol_rot, kol_poz, tut_rot)


def main():
    veri = {"format_version": "1.8.0", "animations": {"kutup_oltasi_firlat": firlat(), "kutup_oltasi_cek": cek()}}
    CIKTI.parent.mkdir(parents=True, exist_ok=True)
    CIKTI.write_text(json.dumps(veri, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(CIKTI.relative_to(KOK), len(veri["animations"]), "klip")


if __name__ == "__main__":
    main()
