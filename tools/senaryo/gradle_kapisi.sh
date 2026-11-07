#!/bin/sh
# Ağır süreç kapısı (GOREV §2, K-E51, K-E60). Kullanım (proje ya da worktree kökünden): tools/senaryo/gradle_kapisi.sh <log> <gradle argümanları...>
#  1. Test defteri (tools/senaryo/defter.py): görevlerden kimlik (build, test, gametest[:<TEST_FILTRE>]); aynı girdi hash'inde
#     `gecti` varsa "DEFTER: geçti @<hash> (...)", çıkış 0, Gradle başlamaz (DEFTER_ZORLA=1 koşturur). Koşu bitince sonuç yazılır.
#  2. Tek giriş kuyruk (K-E60): kuyruk işinin içinde değilse (SIRA_IS yok; SIRA_SERBEST=1 bilerek atlar) kendini
#     `sira.py ekle --bekle --makine oto` ile kuyruğa ekler ve bekler: işin çıktısı akar, çıkış kodu işinki, kesilirse iş
#     iptal. Ekransız görev ortak oto kuyruğuna (boş yuvası çok olan makine alır, eşitlikte pc2; K-E60 rev.), ekranlı (runData) ana
#     kuyruğuna. Beklemeden sıradaki işe geçmek: tools/senaryo/sira.py ekle -- tools/senaryo/gradle_kapisi.sh <log> <görevler>.
#  3. İşin içinde: görevlerin hepsi ekransızsa ve UZAK=0 değilse (pc2 işi) pc2'de koşar (uzak.sh, log geri gelir; K-E44),
#     değilse (ana makinenin işi UZAK=0 taşır) yerelde. pc2'de (UZAK_ICINDE) doğrudan Gradle.
# Log'un ilk satırı FILTRE=<regex> (kabul için geçersiz koşu) ya da FILTRE=yok (K-E20), ikincisi MAKINE=<ad> (K-E43).
# KAPI_SURE görevin süre sınırı (sn, vars. 1500; uzak.sh pc2'de 4500 verir). Gradle LC_ALL=C.UTF-8 ile başlar: tr_TR altında
# "BUİLD" yazılır (K-E13). --rerun-tasks yasak (K-E51): yeniden koşu gerekiyorsa DEFTER_ZORLA=1 ya da cleanTest.
log="$1"
shift
mkdir -p "$(dirname -- "$log")"
py=python3

gradle_kos() {
    if [ -n "$TEST_FILTRE" ]; then
        set -- "$@" "-Ptest_filtre=$TEST_FILTRE"
        echo "FILTRE=$TEST_FILTRE (kabul için geçersiz koşu)" > "$log"
    else
        echo "FILTRE=yok" > "$log"
    fi
    echo "MAKINE=$(cat /etc/hostname)" >> "$log"
    LC_ALL=C.UTF-8 JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 timeout "${KAPI_SURE:-1500}" ./gradlew "$@" --offline --console=plain \
        >> "$log" 2>&1
    kod=$?
    echo "çıkış=$kod"
    return $kod
}

if [ -n "${UZAK_ICINDE-}" ]; then
    gradle_kos "$@"
    exit
fi

kimlik=$($py tools/senaryo/defter.py kimlik -- "$@") || exit 2
if [ -n "$kimlik" ]; then
    girdi=$($py tools/senaryo/defter.py hash "$kimlik") || exit 2
    $py tools/senaryo/defter.py bak "$kimlik" --hash "$girdi" && exit 0
fi
if [ -z "${SIRA_IS-}" ] && [ "${SIRA_SERBEST-}" != 1 ]; then
    exec $py tools/senaryo/sira.py ekle --bekle --makine oto -- tools/senaryo/gradle_kapisi.sh "$log" "$@"
fi
if [ "${UZAK-1}" != 0 ] && tools/senaryo/uzak.sh --uzak-mi "$@"; then
    tools/senaryo/uzak.sh -g "$log" tools/senaryo/gradle_kapisi.sh "$log" "$@"
    kod=$?
else
    gradle_kos "$@"
    kod=$?
fi
if [ -n "$kimlik" ]; then
    $py tools/senaryo/defter.py yaz "$kimlik" $kod "$log" --hash "$girdi"
fi
exit $kod
