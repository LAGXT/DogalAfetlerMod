#!/usr/bin/env bash
# Görsel betiği pc2'de (XFCE/X11) görünür istemciyle koşar: yalnız kare/vitrin koşuları; FPS/CPU ölçümü ana makinede kalır
# (pc2'nin `OLCUM` satırları `bilgi`, `olcum_tablosu.py`). Kullanım (proje ya da worktree kökünden):
#   tools/senaryo/pc2_istemci.sh <kanıt/> <görsel betik> [betik argümanları...]
#   tools/senaryo/pc2_istemci.sh .vibe/kanit/pc2-radar/ radar_gorsel.py .vibe/kanit/pc2-radar
#   tools/senaryo/pc2_istemci.sh /home/lagxt/LagOS/DogalAfetlerMod/.vibe/kanit/yiyecek/ yiyecek_gorsel.py
# <kanıt/> betiğin yazdığı klasör olmalı (argümansız betikler KÖK/.vibe/kanit/<ad>/'a yazar). Bu kipi bütün görsel betikler
# tanır (ortak ekran protokolü, `runclient_kilit.istemci_baslat` ve eşleri). İşi `uzak.sh` götürür (rsync, setsid, kesme,
# <kanıt/> geri çekme onundur). Bu betik yalnız: (1) betik var mı diye bakar; (2) runClient ~5–6 GiB olduğundan pc2'nin
# bütün yuvalarını tutar: yuva-2..yuva-N'yi (N = en az 2, `UZAK_YUVA`) kendisi flock'lar, yuva-1'i `uzak.sh`'ye `UZAK_YUVA=1`
# ile aldırır (K-E44 kilitleri, ~/.cache/uzak-pc2/);
# (3) Quick Play `test` dünyası pc2'de yoksa KÖK'ün run/saves/test'ini aynı yola kopyalar (`uzak.sh` run*/ eşitlemez);
# (4) yuva-1 alındıktan sonra (bütün yuvalar bizde, başka iş yok) pc2'nin boştaki Gradle daemon'larını `./gradlew --stop` ile
# durdurur: pc2'de daemon açık (~/.gradle/gradle.properties, boşta 15 dk), her biri ~0,5 GiB tutar, istemci koşusunda en az boş
# ~1,3 GiB. Uzak ortam: DOGALAFETLER_EKRAN=pc2, DISPLAY=$PC2_DISPLAY (vars. :0). Çıkış: `uzak.sh`'ninki (4 pc2 kapalı, 3 bekleme
# doldu), 2 kullanım hatası.
set -u
[ $# -ge 2 ] || { echo "kullanım: tools/senaryo/pc2_istemci.sh <kanıt/> <görsel betik> [argümanlar...]" >&2; exit 2; }
kanit=$1
betik=${2##*/}
shift 2
[ -x ./gradlew ] || { echo "PC2: proje ya da worktree kökünden çalıştır (./gradlew yok)" >&2; exit 2; }
[[ $kanit == */ ]] || kanit+=/
[ -f "tools/senaryo/$betik" ] || { echo "PC2: tools/senaryo/$betik yok" >&2; exit 2; }

makine=${UZAK_MAKINE:-pc2}
ssh_ac=(ssh -o BatchMode=yes -o ConnectTimeout=5 "$makine")
if ! "${ssh_ac[@]}" true 2>/dev/null; then
    echo "PC2: $makine'ye ulaşılamadı (kapalı ya da Windows açık); iş başlatılmadı, çıkış 4" >&2
    exit 4
fi

kilitler=$HOME/.cache/uzak-$makine
mkdir -p "$kilitler"
dk=${UZAK_BEKLE_DK:-60}
son=${UZAK_YUVA:-2}
[ "$son" -ge 2 ] || son=2
for ((n = 2; n <= son; n++)); do
    exec {fd}>"$kilitler/yuva-$n"
    if ! flock -n "$fd"; then
        echo "PC2: $makine yuva $n dolu, bekleniyor (en çok $dk dk)"
        flock -w $((dk * 60)) "$fd" || { echo "PC2: yuva $n $dk dk'da açılmadı, çıkış 3" >&2; exit 3; }
    fi
done

kok=$PWD
kok_ana=$kok
[ "$(basename -- "$(dirname -- "$kok")")" = .worktrees ] && kok_ana=$(dirname -- "$(dirname -- "$kok")")
dunya=$kok_ana/run/saves/test
if [ -d "$dunya" ] && ! "${ssh_ac[@]}" bash -s <<<"test -d $(printf %q "$dunya")"; then
    "${ssh_ac[@]}" bash -s <<<"mkdir -p $(printf %q "$dunya")" &&
        /usr/bin/rsync -a --exclude=session.lock -e "ssh -o BatchMode=yes" "$dunya/" "$makine:$dunya/" ||
        { echo "PC2: test dünyası $makine'ye kopyalanamadı" >&2; exit 4; }
    echo "PC2: test dünyası kopyalandı: $dunya"
fi

echo "PC2: $makine'nin $son yuvası bu istemci için (yuva-1 uzak.sh'de)"
# JAVA_HOME sabit: yerel kabuğun JAVA_HOME'u (ör. mise Java 21) pc2'de yok; o zaman `--stop` sessizce düşer, daemon'lar RAM'i tutar.
UZAK_YUVA=1 JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 exec tools/senaryo/uzak.sh -g "$kanit" env DOGALAFETLER_EKRAN=pc2 \
    DISPLAY="${PC2_DISPLAY:-:0}" JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 \
    sh -c './gradlew --stop -q </dev/null >/dev/null 2>&1; exec python3 "$@"' sh "tools/senaryo/$betik" "$@"
