#!/bin/sh
# Ağır süreç kapısı (GOREV §2, K-E51). Kullanım (proje ya da worktree kökünden): tools/senaryo/gradle_kapisi.sh <log> <gradle argümanları...>
# Ana makinedeki çağrı:
#  1. Test defteri (tools/senaryo/defter.py): görevlerden kimlik (build, test, gametest[:<TEST_FILTRE>]); aynı girdi hash'inde
#     `gecti` varsa "DEFTER: geçti @<hash> (...)", çıkış 0, Gradle başlamaz (DEFTER_ZORLA=1 koşturur). Koşu bitince sonuç yazılır.
#  2. Görevlerin hepsi ekransızsa (uzak.sh --uzak-mi) iş pc2'de koşar, log geri gelir (K-E44); değilse ya da UZAK=0 ise yerelde.
#  3. Kapı doluysa (sira.py dolu-mu: pc2'nin iki yuvası ya da bu klasörün pc2 işi; yerelde Gradle >= 2, boş RAM < KAPI_RAM_MB
#     (vars. 4096) ya da KAPI_PORT dinleniyor; ya da o makinenin kuyruğunda bekleyen iş) beklemez: iş `sira.py ekle` ile kuyruğa
#     girer (ekransız iş ortak `oto` kuyruğuna: pc2 öncelikli, taşanı ana makine UZAK=0 ile alır, K-E53), log'a ve çıktıya
#     "SIRA: ... kuyrukta <id>", çıkış 0 (sonuç `tools/senaryo/sira.py bekle <id>`, pano, defter).
#     KAPI_BLOKLA=1 eski davranış: kapı 60 sn'de bir, en çok KAPI_DK (vars. 20) dk beklenir; açılmazsa "KAPI: açılmadı", çıkış 3
#     (K-E23). Kuyruğun koştuğu iş (SIRA_ICINDE) ve defterli bir betik içinden çağrı (DEFTER_ICINDE) da kuyruğa atmaz, yerelde
#     böyle bekler; pc2'de (UZAK_ICINDE) kapı uzak.sh yuvasıdır, ayrıca beklenmez.
# Log'un ilk satırı FILTRE=<regex> (kabul için geçersiz koşu) ya da FILTRE=yok (K-E20), ikincisi MAKINE=<ad> (K-E43).
# KAPI_PORT verilirse o portu dinleyen süreç kalmaması da beklenir (runServer; K-E13). KAPI_SURE görevin süre sınırı (sn,
# vars. 1500; uzak.sh pc2'de 4500 verir). Gradle LC_ALL=C.UTF-8 ile başlar: tr_TR altında "BUİLD" yazılır (K-E13).
# --rerun-tasks yasak (K-E51): yeniden koşu gerekiyorsa DEFTER_ZORLA=1 ya da cleanTest.
log="$1"
shift
mkdir -p "$(dirname -- "$log")"
py=python3

kapi_bekle() {
    dk=${KAPI_DK:-20}
    i=0
    while :; do
        sayi=$(pgrep -fc GradleWrapperMain)
        bos=$(awk '/MemAvailable/ {print int($2 / 1024)}' /proc/meminfo)
        port_dolu=0
        if [ -n "$KAPI_PORT" ] && [ -n "$(ss -Hltn "sport = :$KAPI_PORT")" ]; then
            port_dolu=1
        fi
        echo "kapı: gradle=$sayi bos=${bos}M port=${KAPI_PORT:-yok} dolu=$port_dolu"
        if [ "$sayi" -lt 2 ] && [ "$bos" -ge "${KAPI_RAM_MB:-4096}" ] && [ "$port_dolu" -eq 0 ]; then
            return 0
        fi
        [ $i -ge "$dk" ] && break
        sleep 60
        i=$((i + 1))
    done
    echo "KAPI: açılmadı ($dk dk)" | tee "$log"
    echo "çıkış=3"
    return 3
}

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
if [ "${UZAK-1}" != 0 ] && tools/senaryo/uzak.sh --uzak-mi "$@"; then
    makine=pc2
else
    makine=ana
fi
blokla=${KAPI_BLOKLA-0}
[ -n "${SIRA_ICINDE-}${DEFTER_ICINDE-}" ] && blokla=1
kuyruk=$makine
[ $makine = pc2 ] && kuyruk=oto
if [ "$blokla" != 1 ] && neden=$($py tools/senaryo/sira.py dolu-mu --makine $makine); then
    is=$($py tools/senaryo/sira.py ekle --makine $kuyruk -- tools/senaryo/gradle_kapisi.sh "$log" "$@") || exit 2
    echo "SIRA: kapı dolu ($neden); kuyrukta $is — sonuç: tools/senaryo/sira.py bekle $is" | tee "$log"
    exit 0
fi
if [ $makine = pc2 ]; then
    tools/senaryo/uzak.sh -g "$log" tools/senaryo/gradle_kapisi.sh "$log" "$@"
    kod=$?
else
    if [ "$blokla" = 1 ]; then
        kapi_bekle || exit 3
    fi
    gradle_kos "$@"
    kod=$?
fi
if [ -n "$kimlik" ]; then
    $py tools/senaryo/defter.py yaz "$kimlik" $kod "$log" --hash "$girdi"
fi
exit $kod
