#!/usr/bin/env python3
"""Spike yardımcısı (K0-21 rev. ekran protokolü): bu worktree'nin Minecraft penceresini ajan workspace'ine taşır ve oyun
boyunca sahibinin klavye odağını korur.

Kullanım (runClient'tan hemen önce, ayrı bir süreç olarak; Minecraft kapanınca kendisi biter):
    timeout 1800 python3 tools/spike/gokyuzu_pencere_tasi.py 98

1. Başlarken sahibinin aktif penceresi kaydedilir.
2. Pencere yalnız pid ile seçilir: `/proc/<pid>/cwd` bu worktree'nin `run/` klasörü olmalı.
3. Taşıma hypruse'un `movetoworkspacesilent` karşılığıyla yapılır (hypruse/hyprctl.py `_lua_movetoworkspacesilent`):
   Lua `hl.dsp.window.move{..., follow = false}`; bu Hyprland eski `movetoworkspacesilent` dizgesini kabul etmez.
   Yanıt `ok` değilse başarısız sayılır ve yeniden denenir.
4. Taşımadan hemen sonra ve her 30 sn'de aktif pencere denetlenir; bizim Minecraft'ımızsa odak 1. adımdaki pencereye
   geri verilir (hypruse `_lua_focuswindow` karşılığı) ve yazılır.
"""
import json
import os
import subprocess
import sys
import time

KOK = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RUN = os.path.realpath(os.path.join(KOK, "run"))
ENV = dict(os.environ, HYPRLAND_INSTANCE_SIGNATURE=os.listdir("/run/user/1000/hypr/")[0])
HEDEF = sys.argv[1] if len(sys.argv) > 1 else "98"
SURE = float(sys.argv[2]) if len(sys.argv) > 2 else 1700


def yaz(metin):
    print(f"{time.strftime('%H:%M:%S')} {metin}", flush=True)


def hyprctl_json(*args):
    try:
        return json.loads(subprocess.run(["hyprctl", *args, "-j"], env=ENV, capture_output=True, text=True).stdout)
    except (json.JSONDecodeError, OSError):
        return None


def lua(ifade):
    sonuc = subprocess.run(["hyprctl", "dispatch", ifade], env=ENV, capture_output=True, text=True)
    return (sonuc.stdout + sonuc.stderr).strip()


def benim_pencerem():
    for c in hyprctl_json("clients") or []:
        try:
            if os.path.realpath(os.readlink(f"/proc/{c.get('pid')}/cwd")) == RUN:
                return c
        except OSError:
            continue
    return None


aktif = hyprctl_json("activewindow") or {}
sahibin = aktif.get("address")
yaz(f"sahibin aktif penceresi: {sahibin} ({aktif.get('class')!r})")

bitis = time.time() + SURE
tasindi = None
goruldu = False
son_denetim = 0.0
while time.time() < bitis:
    c = benim_pencerem()
    if c is None:
        if goruldu:
            yaz("Minecraft penceresi kapandı, çıkılıyor")
            break
        time.sleep(0.15)
        continue
    goruldu = True
    adres = c["address"]
    if tasindi != adres or str(c.get("workspace", {}).get("name")) != HEDEF:
        yanit = lua('hl.dsp.window.move({ workspace = "%s", window = "address:%s", follow = false })' % (HEDEF, adres))
        if yanit == "ok":
            tasindi = adres
            yaz(f"taşındı: pid={c.get('pid')} ws {c.get('workspace', {}).get('name')} -> {HEDEF}")
            son_denetim = 0.0
        else:
            yaz(f"TAŞIMA BAŞARISIZ (yanıt: {yanit!r}), yeniden denenecek")
            time.sleep(0.5)
            continue
    if time.time() - son_denetim >= 30:
        son_denetim = time.time()
        simdi = (hyprctl_json("activewindow") or {}).get("address")
        if simdi == adres and sahibin and sahibin != adres:
            yanit = lua('hl.dsp.focus({ window = "address:%s" })' % sahibin)
            yaz(f"odak bizim Minecraft'taydı, sahibine geri verildi ({sahibin}): {yanit!r}")
        else:
            yaz(f"odak denetimi: aktif {simdi} (bizim değil)")
    time.sleep(0.15)
