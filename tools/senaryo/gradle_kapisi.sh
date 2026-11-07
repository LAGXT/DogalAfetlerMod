#!/bin/sh
# Ağır süreç kapısı (GOREV §2): çalışan Gradle sayısı < 2 ve boş RAM >= 4 GB olana kadar 60 sn'de bir bekler (en çok
# 20 dk), sonra verilen görevi JDK 17 ile çalıştırır. Kapı 20 dk'da açılmazsa görev başlatılmaz: log'a "KAPI: açılmadı
# (20 dk)", çıkış 3 (K-E23; agir_surec_kapisi ile aynı). Kullanım: tools/senaryo/gradle_kapisi.sh <log> <gradle argümanları...>
# KAPI_PORT verilirse o portu dinleyen süreç kalmamasını da bekler (runServer; K-E13). KAPI_DK bekleme üst sınırı (dk, varsayılan 20).
# KAPI_SURE görevin süre sınırı (sn, varsayılan 1500; uzak.sh pc2'de 4500 verir, K-E43).
# TEST_FILTRE verilirse -Ptest_filtre=<regex> olarak geçer: runGameTestServer yalnız uyan testleri koşar (K-E20). Log'un ilk
# satırı FILTRE=<regex> (kabul için geçersiz koşu) ya da FILTRE=yok; kabul loglarında FILTRE=yok aranır (Gf4). İkinci satırı
# MAKINE=<ana bilgisayar adı>: koşunun hangi makinede geçtiği (K-E43).
# Gradle LC_ALL=C.UTF-8 ile başlar: tr_TR altında sonuç satırı "BUİLD" yazılır ve BUILD grep'leri kaçırır (K-E13).
log="$1"
shift
if [ -n "$TEST_FILTRE" ]; then
    set -- "$@" "-Ptest_filtre=$TEST_FILTRE"
    filtre_satiri="FILTRE=$TEST_FILTRE (kabul için geçersiz koşu)"
else
    filtre_satiri="FILTRE=yok"
fi
dk=${KAPI_DK:-20}
i=0
acik=0
while :; do
    sayi=$(pgrep -fc GradleWrapperMain)
    bos=$(awk '/MemAvailable/ {print int($2 / 1048576)}' /proc/meminfo)
    port_dolu=0
    if [ -n "$KAPI_PORT" ] && [ -n "$(ss -Hltn "sport = :$KAPI_PORT")" ]; then
        port_dolu=1
    fi
    echo "kapı: gradle=$sayi bos=${bos}G port=${KAPI_PORT:-yok} dolu=$port_dolu"
    if [ "$sayi" -lt 2 ] && [ "$bos" -ge 4 ] && [ "$port_dolu" -eq 0 ]; then
        acik=1
        break
    fi
    [ $i -ge $dk ] && break
    sleep 60
    i=$((i + 1))
done
if [ $acik -eq 0 ]; then
    echo "KAPI: açılmadı ($dk dk)" | tee "$log"
    echo "çıkış=3"
    exit 3
fi
echo "$filtre_satiri" > "$log"
echo "MAKINE=$(cat /etc/hostname)" >> "$log"
LC_ALL=C.UTF-8 JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 timeout "${KAPI_SURE:-1500}" ./gradlew "$@" --offline --console=plain >> "$log" 2>&1
kod=$?
echo "çıkış=$kod"
exit $kod
