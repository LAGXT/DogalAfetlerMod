#!/usr/bin/env bash
# Ekransız ağır işi ikinci makinede (pc2: FX-4100, 8 GB; docs/MAKINE.md) koşar, ana makine yalnız dosya eşitler (K-E43, K-E44).
# Kullanım (proje ya da worktree kökünden): tools/senaryo/uzak.sh [-g <yol>]... <komut...>
#   tools/senaryo/uzak.sh -g <log> tools/senaryo/gradle_kapisi.sh <log> test
#   tools/senaryo/uzak.sh -g <kanıt>/ python3 tools/senaryo/deprem_sunucu.py --kanit <kanıt> --port 25651
# Elle çağırmak gerekmez: ./gradlew (ekransız görevler), gradle_kapisi.sh ve *_sunucu.py kendileri buraya döner (K-E44).
# Tek giriş (K-E60): kuyruk işinin dışındaki çağrı (SIRA_IS ve UZAK_ICINDE yok, SIRA_SERBEST=1 değil) kendini pc2 işi olarak
# kuyruğa ekler ve bekler (`sira.py ekle --bekle`); --uzak-mi sorgusu kuyruğa girmez.
#   uzak.sh --gradle <proje> <gradle argümanları>  ./gradlew kancası: görevlerin hepsi ekransızsa pc2'de, değilse yerelde
#   uzak.sh --uzak-mi <gradle argümanları>         çıkış 0: görevlerin hepsi ekransız (pc2'ye gider)
# pc2'ye ulaşılamıyorsa çıkış 4: iş yerelde başlatılmaz. pc2'de aynı anda UZAK_YUVA (vars. 2) iş ve aynı klasörden tek iş
# (ana makinedeki ~/.cache/uzak-<makine>/ flock'ları); UZAK_BEKLE_DK (vars. 60) dakikada yer açılmazsa çıkış 3. Geçerli klasör pc2'de
# aynı yola rsync'lenir (build/, .gradle/, run*/, logs/, .vibe/, .venv/, .worktrees/, .git hariç; hariçler pc2'de korunur); işten
# önce pc2'nin son açılışından eski çökme artıkları (sıfır kilit, 0 baytlık jars-9 sınıfı) yanlarına .bozuk-<zaman> adıyla
# taşınır. Komut orada aynı klasörde koşar, çıktısı buraya akar. Bitince (hata ya da kesmede de) her -g yolu buraya geri
# çekilir; sonu / olan yol klasördür. -g yolu klasörün dışında ya da .vibe/, build/, logs/, run*/ altında olmalı (eşitlenen
# kaynak geri ezilmez).
# Geçen ortam: TEST_FILTRE, KAPI_PORT, KAPI_DK, VIBE_OTURUM, JAVA_HOME, LC_ALL, GRADLE_OPTS; KAPI_SURE vars. 4500 sn,
# KAPI_RAM_MB vars. 2500 (pc2'de iki iş yan yana sığsın diye; K-E44). UZAK_MAKINE ssh adı (vars. pc2). UZAK_YUVA_TUTULU=1:
# yuva kilitlerini çağıran tutuyor (kuyruğun istemci işi, K-E51), yuva beklenmez; klasör kilidi yine alınır.
set -u
makine=${UZAK_MAKINE:-pc2}
ssh_ac=(ssh -o BatchMode=yes -o ConnectTimeout=5 "$makine")

# Ekransız görevler: pc2'de koşar. Başka her görev (runClient, runData, runServer, wrapper, tasks...) yerelde kalır.
gradle_uzak_mi() {
    local atla=0 gorev=0 g
    for g in "$@"; do
        if [ $atla = 1 ]; then atla=0; continue; fi
        case $g in
            -x|--exclude-task|-p|--project-dir|-I|--init-script|-g|--gradle-user-home|-c|--settings-file|-b|--build-file|\
            --include-build|-P|--project-prop|-D|--system-prop) atla=1 ;;
            -*) ;;
            *)
                gorev=1
                case ${g##*:} in
                    test|cleanTest|build|assemble|check|clean|compileJava|compileTestJava|classes|testClasses|jar|reobfJar|\
                    runGameTestServer) ;;
                    *) return 1 ;;
                esac ;;
        esac
    done
    [ $gorev = 1 ]
}

case ${1:-} in
    --uzak-mi)
        shift
        gradle_uzak_mi "$@"
        exit ;;
esac

if [ -z "${SIRA_IS-}${UZAK_ICINDE-}" ] && [ "${SIRA_SERBEST-}" != 1 ]; then
    exec python3 "$(dirname -- "$0")/sira.py" ekle --bekle --makine oto -- "$0" "$@"
fi

case ${1:-} in
    --gradle)
        dizin=$2
        shift 2
        if ! gradle_uzak_mi "$@"; then
            UZAK_ICINDE=1 exec "$dizin/gradlew" "$@"
        fi
        cd "$dizin" || exit 2
        set -- -g "$PWD/build/reports/" -g "$PWD/build/test-results/" -g "$PWD/build/libs/" ./gradlew "$@" ;;
esac

[ -x ./gradlew ] || { echo "UZAK: proje ya da worktree kökünden çalıştır (./gradlew yok)" >&2; exit 2; }
kok=$PWD

geri=()
while [ $# -gt 0 ]; do
    case $1 in
        -g)
            [ $# -ge 2 ] || { echo "UZAK: -g yol ister" >&2; exit 2; }
            yol=$(realpath -m -- "$2")
            case $yol/ in
                "$kok"/.vibe/*|"$kok"/build/*|"$kok"/logs/*|"$kok"/run*/*) ;;
                "$kok"/*) echo "UZAK: -g $2 eşitlenen kaynağın içinde; .vibe/, build/, logs/, run*/ altı ya da klasör dışı olmalı" >&2
                           exit 2 ;;
            esac
            [[ $2 == */ ]] && yol+=/
            geri+=("$yol")
            shift 2 ;;
        --) shift; break ;;
        *) break ;;
    esac
done
[ $# -gt 0 ] || { echo "kullanım: tools/senaryo/uzak.sh [-g <yol>]... <komut...>" >&2; exit 2; }

if ! "${ssh_ac[@]}" true 2>/dev/null; then
    echo "UZAK: $makine'ye ulaşılamadı (kapalı ya da Windows açık); iş başlatılmadı, çıkış 4 (UZAK=0 yerelde zorlar)" >&2
    exit 4
fi

kilitler=$HOME/.cache/uzak-$makine
mkdir -p "$kilitler"
dk=${UZAK_BEKLE_DK:-60}
bitis=$((SECONDS + dk * 60))
exec 8>"$kilitler/klasor-$(printf %s "$kok" | md5sum | cut -c1-12)"
if ! flock -n 8; then
    echo "UZAK: bu klasörden $makine'de başka bir iş var, bekleniyor (en çok $dk dk)"
    flock -w $((dk * 60)) 8 || { echo "UZAK: klasör kilidi $dk dk'da açılmadı, çıkış 3" >&2; exit 3; }
fi
yuva_sayisi=${UZAK_YUVA:-2}
yuva=0
# K-E51: kuyruğun pc2 istemci işi (sira.py ekle --makine pc2 --istemci) yuva kilitlerinin hepsini kendisi tutar.
[ "${UZAK_YUVA_TUTULU:-}" = 1 ] && yuva=kuyruk
while [ $yuva = 0 ]; do
    for ((n = 1; n <= yuva_sayisi; n++)); do
        exec 9>"$kilitler/yuva-$n"
        if flock -n 9; then yuva=$n; break; fi
        exec 9>&-
    done
    [ $yuva != 0 ] && break
    [ $SECONDS -ge $bitis ] && { echo "UZAK: $makine'de $dk dk'da yuva açılmadı, çıkış 3" >&2; exit 3; }
    [ -n "${bekleme_yazildi:-}" ] || echo "UZAK: $makine'nin $yuva_sayisi yuvası dolu, bekleniyor (en çok $dk dk)"
    bekleme_yazildi=1
    sleep 5
done

klasorler=("$kok")
for y in "${geri[@]}"; do
    if [[ $y == */ ]]; then klasorler+=("$y"); else klasorler+=("$(dirname -- "$y")"); fi
done
"${ssh_ac[@]}" bash -s <<<"mkdir -p$(printf ' %q' "${klasorler[@]}")" || { echo "UZAK: $makine'de klasör açılamadı" >&2; exit 4; }
/usr/bin/rsync -a --delete -e "ssh -o BatchMode=yes" \
    --exclude=/build/ --exclude=/.gradle/ --exclude='/run*/' --exclude=/logs/ --exclude=/.vibe/ --exclude=/.venv/ \
    --exclude=/.worktrees/ --exclude=/.git --exclude=/cikti/ --exclude='/_eski-surum-*/' --exclude=__pycache__/ \
    "$kok/" "$makine:$kok/" || { echo "UZAK: rsync başarısız, iş başlatılmadı" >&2; exit 4; }

# pc2 kirli kapanırsa (güç/donma) ext4'ün gecikmeli ayırması o an yazılan Gradle dosyalarını sıfır dolu ya da 0 bayt bırakır:
# ilk baytı 0 olan kilit Gradle'ı "Unexpected lock protocol found in lock file. Expected 3, found 0" ile başlamadan düşürür,
# jars-9'da 0 baytlık derlenmiş betik sınıfı kalır. pc2'nin son açılışından eski artıklar silinmez, yanına .bozuk-<zaman> adıyla
# taşınır: klasörün kendi .gradle/ önbellek klasörü (klasör kilidi bizde) ve içerik adresli jars-9 girdisi; ~/.gradle'ın
# kalanı (başka işlerle ortak) yalnız raporlanır.
artik_temizle=$(cat <<'SON'
acilis=$(( $(date +%s) - $(cut -d. -f1 /proc/uptime) ))
damga=bozuk-$(date +%Y%m%d%H%M%S)
sifir_kilit() { [ -s "$1" ] && [ "$(od -An -tx1 -N1 -- "$1" | tr -d ' ')" = 00 ]; }
find .gradle -mindepth 2 -name '*.lock' -type f ! -path '*.bozuk-*' ! -newermt "@$acilis" 2>/dev/null | while read -r f; do
    sifir_kilit "$f" || continue
    d=$(dirname -- "$f")
    mv -n -- "$d" "$d.$damga" 2>/dev/null && echo "UZAK: çökme artığı kilit $PWD/$f; klasörü $d.$damga" >&2
done
j=$HOME/.gradle/caches/jars-9
find "$j" -mindepth 2 -maxdepth 3 -type f \( \( -name '*.class' -o -name '*.jar' \) -size 0 -o -name '*.lock' \) \
    ! -path '*.bozuk-*' ! -newermt "@$acilis" 2>/dev/null | while read -r f; do
        case $f in *.lock) sifir_kilit "$f" || continue ;; esac
        g=${f#"$j"/}
        echo "$j/${g%%/*}"
    done | sort -u | while read -r d; do
        mv -n -- "$d" "$d.$damga" 2>/dev/null && echo "UZAK: çökme artığı jars-9 girdisi $d → $d.$damga" >&2
    done
find "$HOME/.gradle/caches" "$HOME/.gradle/daemon" -maxdepth 3 -name '*.lock' -type f ! -path "$j/*/*" \
    ! -path '*.bozuk-*' ! -newermt "@$acilis" 2>/dev/null | while read -r f; do
        sifir_kilit "$f" && echo "UZAK: UYARI çökme artığı kilit $f (ortak önbellek, elle yanına .bozuk-<tarih> adıyla taşı)" >&2
    done
true
SON
)

pid_dosyasi=/tmp/uzak-$$-$yuva.pid
betik=$(
    printf 'cd %q || exit 125\n' "$kok"
    printf '%s\n' "$artik_temizle"
    for v in TEST_FILTRE KAPI_PORT KAPI_DK VIBE_OTURUM JAVA_HOME LC_ALL GRADLE_OPTS; do
        if [ -n "${!v:-}" ]; then printf 'export %s=%q\n' "$v" "${!v}"; fi
    done
    printf 'export KAPI_SURE=%q KAPI_RAM_MB=%q UZAK_ICINDE=1\n' "${KAPI_SURE:-4500}" "${KAPI_RAM_MB:-2500}"
    printf 'setsid'
    printf ' %q' "$@"
    printf ' </dev/null &\n'
    printf 'echo $! > %q\n' "$pid_dosyasi"
    printf 'wait $!; kod=$?; rm -f %q; exit $kod\n' "$pid_dosyasi"
)

echo "UZAK: $makine yuva $yuva/$yuva_sayisi:$kok →$(printf ' %q' "$@")"
baslangic=$SECONDS
iptal=0
trap 'iptal=1' INT TERM HUP
"${ssh_ac[@]}" bash -s <<<"$betik" &
ssh_pid=$!
wait "$ssh_pid"
kod=$?
if [ "$iptal" = 1 ]; then
    echo "UZAK: kesildi, $makine'deki iş durduruluyor" >&2
    # Oturumun hepsi: `timeout` (gradle_kapisi.sh) kendi süreç grubunu açar, grup sinyali Gradle'a ulaşmaz (K-E51).
    "${ssh_ac[@]}" bash -s <<<"pkill -TERM -s \$(cat $pid_dosyasi) 2>/dev/null; rm -f $pid_dosyasi"
    wait "$ssh_pid"
    kod=130
fi

for y in "${geri[@]}"; do
    if [[ $y == */ ]]; then hedef=$y; else hedef=$(dirname -- "$y")/; fi
    mkdir -p -- "$hedef"
    /usr/bin/rsync -a --ignore-missing-args -e "ssh -o BatchMode=yes" "$makine:$y" "$hedef" || echo "UZAK: $y geri çekilemedi" >&2
done
echo "UZAK: çıkış=$kod süre=$((SECONDS - baslangic)) sn"
exit "$kod"
