#!/bin/sh
# Ağır süreç kapısı (GOREV §2): çalışan Gradle sayısı < 2 ve boş RAM >= 4 GB olana kadar 60 sn'de bir bekler (en çok
# 20 dk), sonra verilen görevi JDK 17 ile çalıştırır. Kullanım: tools/senaryo/gradle_kapisi.sh <log> <gradle argümanları...>
log="$1"
shift
i=0
while [ $i -lt 20 ]; do
    sayi=$(pgrep -fc GradleWrapperMain)
    bos=$(awk '/MemAvailable/ {print int($2 / 1048576)}' /proc/meminfo)
    echo "kapı: gradle=$sayi bos=${bos}G"
    if [ "$sayi" -lt 2 ] && [ "$bos" -ge 4 ]; then
        break
    fi
    sleep 60
    i=$((i + 1))
done
JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9 timeout 1500 ./gradlew "$@" --offline --console=plain > "$log" 2>&1
kod=$?
echo "çıkış=$kod"
exit $kod
