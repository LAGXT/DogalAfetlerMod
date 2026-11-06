#!/bin/sh
# Ağır süreç kapısı (GOREV §2): çalışan Gradle sayısı < 2 ve boş RAM >= 4 GB olana kadar 60 sn'de bir bekler (en çok
# 20 dk), sonra verilen görevi JDK 17 ile çalıştırır. Kullanım: tools/senaryo/gradle_kapisi.sh <log> <gradle argümanları...>
# KAPI_PORT verilirse o portu dinleyen süreç kalmamasını da bekler (runServer; K-E13).
log="$1"
shift
i=0
while [ $i -lt 20 ]; do
    sayi=$(pgrep -fc GradleWrapperMain)
    bos=$(awk '/MemAvailable/ {print int($2 / 1048576)}' /proc/meminfo)
    port_dolu=0
    if [ -n "$KAPI_PORT" ] && [ -n "$(ss -Hltn "sport = :$KAPI_PORT")" ]; then
        port_dolu=1
    fi
    echo "kapı: gradle=$sayi bos=${bos}G port=${KAPI_PORT:-yok} dolu=$port_dolu"
    if [ "$sayi" -lt 2 ] && [ "$bos" -ge 4 ] && [ "$port_dolu" -eq 0 ]; then
        break
    fi
    sleep 60
    i=$((i + 1))
done
JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 timeout 1500 ./gradlew "$@" --offline --console=plain > "$log" 2>&1
kod=$?
echo "çıkış=$kod"
exit $kod
