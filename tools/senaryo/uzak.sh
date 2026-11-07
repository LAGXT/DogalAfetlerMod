#!/usr/bin/env bash
# Ekransız ağır işi ikinci makinede (pc2: FX-4100, 8 GB; docs/MAKINE.md) koşar, ana makine yalnız dosya eşitler (K-E43).
# Kullanım (proje ya da worktree kökünden): tools/senaryo/uzak.sh [-g <yol>]... <komut...>
#   tools/senaryo/uzak.sh -g <log> tools/senaryo/gradle_kapisi.sh <log> test
#   tools/senaryo/uzak.sh -g <kanıt>/ python3 tools/senaryo/deprem_sunucu.py --kanit <kanıt> --port 25651
# pc2'ye ulaşılamıyorsa çıkış 4: iş yerelde başlatılmaz. Ana makinedeki kilitle aynı anda tek uzak iş (8 GB); kilit
# UZAK_BEKLE_DK (vars. 60) dakikada alınamazsa çıkış 3. Geçerli klasör pc2'de aynı yola rsync'lenir (build/, .gradle/, run*/,
# logs/, .vibe/, .venv/, .worktrees/, .git hariç; hariçler pc2'de korunur), komut orada aynı klasörde koşar, çıktısı buraya
# akar. Bitince (hata ya da kesmede de) her -g yolu buraya geri çekilir; sonu / olan yol klasördür. Komutun çıkış kodu döner.
# Geçen ortam: TEST_FILTRE, KAPI_PORT, KAPI_DK, VIBE_OTURUM; KAPI_SURE verilmezse 4500 sn (FX-4100'ün derleme payı).
# UZAK_MAKINE ssh adı (vars. pc2).
set -u
makine=${UZAK_MAKINE:-pc2}
ssh_ac=(ssh -o BatchMode=yes -o ConnectTimeout=5 "$makine")

geri=()
while [ $# -gt 0 ]; do
    case $1 in
        -g)
            [ $# -ge 2 ] || { echo "UZAK: -g yol ister" >&2; exit 2; }
            yol=$(realpath -m -- "$2")
            [[ $2 == */ ]] && yol+=/
            geri+=("$yol")
            shift 2 ;;
        --) shift; break ;;
        *) break ;;
    esac
done
[ $# -gt 0 ] || { echo "kullanım: tools/senaryo/uzak.sh [-g <yol>]... <komut...>" >&2; exit 2; }
[ -x ./gradlew ] || { echo "UZAK: proje ya da worktree kökünden çalıştır (./gradlew yok)" >&2; exit 2; }
kok=$PWD

if ! "${ssh_ac[@]}" true 2>/dev/null; then
    echo "UZAK: $makine'ye ulaşılamadı (kapalı ya da Windows açık); iş başlatılmadı, çıkış 4" >&2
    exit 4
fi

mkdir -p "$HOME/.cache"
exec 9>"$HOME/.cache/uzak-$makine.kilit"
if ! flock -n 9; then
    dk=${UZAK_BEKLE_DK:-60}
    echo "UZAK: $makine'de başka bir uzak iş var, kilit bekleniyor (en çok $dk dk)"
    flock -w $((dk * 60)) 9 || { echo "UZAK: kilit $dk dk'da açılmadı, çıkış 3" >&2; exit 3; }
fi

klasorler=("$kok")
for y in "${geri[@]}"; do
    if [[ $y == */ ]]; then klasorler+=("$y"); else klasorler+=("$(dirname -- "$y")"); fi
done
"${ssh_ac[@]}" bash -s <<<"mkdir -p$(printf ' %q' "${klasorler[@]}")" || { echo "UZAK: $makine'de klasör açılamadı" >&2; exit 4; }
/usr/bin/rsync -a --delete -e "ssh -o BatchMode=yes" \
    --exclude=/build/ --exclude=/.gradle/ --exclude='/run*/' --exclude=/logs/ --exclude=/.vibe/ --exclude=/.venv/ \
    --exclude=/.worktrees/ --exclude=/.git --exclude=/cikti/ --exclude='/_eski-surum-*/' --exclude=__pycache__/ \
    "$kok/" "$makine:$kok/" || { echo "UZAK: rsync başarısız, iş başlatılmadı" >&2; exit 4; }

pid_dosyasi=/tmp/uzak-$$.pid
betik=$(
    printf 'cd %q || exit 125\n' "$kok"
    for v in TEST_FILTRE KAPI_PORT KAPI_DK VIBE_OTURUM; do
        if [ -n "${!v:-}" ]; then printf 'export %s=%q\n' "$v" "${!v}"; fi
    done
    printf 'export KAPI_SURE=%q\n' "${KAPI_SURE:-4500}"
    printf 'setsid'
    printf ' %q' "$@"
    printf ' </dev/null &\n'
    printf 'echo $! > %q\n' "$pid_dosyasi"
    printf 'wait $!; kod=$?; rm -f %q; exit $kod\n' "$pid_dosyasi"
)

echo "UZAK: $makine:$kok →$(printf ' %q' "$@")"
baslangic=$SECONDS
iptal=0
trap 'iptal=1' INT TERM HUP
"${ssh_ac[@]}" bash -s <<<"$betik" &
ssh_pid=$!
wait "$ssh_pid"
kod=$?
if [ "$iptal" = 1 ]; then
    echo "UZAK: kesildi, $makine'deki iş durduruluyor" >&2
    "${ssh_ac[@]}" bash -s <<<"kill -TERM -- -\$(cat $pid_dosyasi) 2>/dev/null; rm -f $pid_dosyasi"
    wait "$ssh_pid"
    kod=130
fi

for y in "${geri[@]}"; do
    if [[ $y == */ ]]; then hedef=$y; else hedef=$(dirname -- "$y")/; fi
    mkdir -p -- "$hedef"
    /usr/bin/rsync -a -e "ssh -o BatchMode=yes" "$makine:$y" "$hedef" || echo "UZAK: $y geri çekilemedi" >&2
done
echo "UZAK: çıkış=$kod süre=$((SECONDS - baslangic)) sn"
exit "$kod"
