#!/usr/bin/env python3
"""Spike yardımcısı: bu worktree'nin Minecraft penceresini açılır açılmaz ajan workspace'ine sessizce taşır.

Kullanım (runClient'tan hemen önce, ayrı bir süreç olarak):
    timeout 320 python3 tools/spike/gokyuzu_pencere_tasi.py 98

Hyprland 0.57 bu makinede Lua yapılandırma yöneticisiyle çalışıyor: `hyprctl dispatch movetoworkspacesilent 98,...` eski
sözdizimi hata verir (çıktı yakalanırsa sessizce başarısız olur, pencere sahibinin ekranında kalır). Lua ifadesinde
`follow = false` zorunludur; yoksa sahibinin görünümü hedef workspace'e kayar. Yalnız çalışma klasörü bu worktree'nin
`run/` klasörü olan pencere taşınır; başka işçilerin aynı sınıflı Minecraft pencerelerine dokunulmaz.
"""
import json
import os
import subprocess
import sys
import time

KOK = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RUN = os.path.join(KOK, "run")

imza = os.listdir("/run/user/1000/hypr/")[0]
env = dict(os.environ, HYPRLAND_INSTANCE_SIGNATURE=imza)
hedef = sys.argv[1] if len(sys.argv) > 1 else "98"
bitis = time.time() + 300
tasinan = set()
while time.time() < bitis:
    try:
        istemciler = json.loads(subprocess.run(["hyprctl", "clients", "-j"], env=env, capture_output=True,
                                               text=True).stdout)
    except (json.JSONDecodeError, OSError):
        istemciler = []
    for c in istemciler:
        ad = (c.get("class", "") + " " + c.get("title", "")).lower()
        if "minecraft" not in ad or c["address"] in tasinan:
            continue
        try:
            cwd = os.readlink(f"/proc/{c.get('pid')}/cwd")
        except OSError:
            continue
        if os.path.realpath(cwd) != os.path.realpath(RUN):
            continue
        tasinan.add(c["address"])
        ifade = 'hl.dsp.window.move({ workspace = "%s", window = "address:%s", follow = false })' % (hedef, c["address"])
        sonuc = subprocess.run(["hyprctl", "dispatch", ifade], env=env, capture_output=True, text=True)
        print(f"{time.strftime('%H:%M:%S')} taşındı: {c.get('class')!r} ws {c.get('workspace', {}).get('name')} -> {hedef} "
              f"pid={c.get('pid')} sonuç={sonuc.stdout.strip()} {sonuc.stderr.strip()}", flush=True)
    time.sleep(0.15)
