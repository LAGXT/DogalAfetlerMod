#!/bin/sh
# K-E numarası sayacı (K-E51): sonraki karar numarasını atomik verir (flock), örn. "K-E52". Sayaç KÖK/.vibe/karar-sayaci
# son verilen numarayı tutar (ana kopya; worktree'ler de oraya bakar, iki oturum aynı numarayı alamaz). Dosya yoksa
# docs/KARARLAR.md'deki en büyük K-E numarasından devam eder. Kullanım: tools/senaryo/karar_no.sh
set -eu
w=$(cd "$(dirname "$0")/../.." && pwd)
case $w in
    */.worktrees/*) kok=${w%/.worktrees/*} ;;
    *) kok=$w ;;
esac
sayac=$kok/.vibe/karar-sayaci
mkdir -p "$kok/.vibe"
exec 9>>"$kok/.vibe/.karar-sayaci.kilit"
flock -w 30 9
son=$(cat "$sayac" 2>/dev/null || true)
if [ -z "$son" ]; then
    son=$(cat "$kok/docs/KARARLAR.md" "$w/docs/KARARLAR.md" 2>/dev/null | grep -o 'K-E[0-9][0-9]*' | sed 's/K-E//' | sort -n |
        tail -n 1)
fi
yeni=$((${son:-0} + 1))
printf '%s\n' "$yeni" > "$sayac.yeni"
mv "$sayac.yeni" "$sayac"
echo "K-E$yeni"
