package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Genel ve afet başına bekleme (madde 4, 10). Bitiş anları ölçekli afet saatiyle tutulur; bekleme afet bittiğinde başlar.
 * Kısa bir bekleme (ör. N14 ertelemesi) uzun olanı silmez. Süreler {@code afetSikligi}'na bölünür; sıklık değişince süren
 * beklemelerin kalanı {@link #siklikDegisti} ile yeni sıklığa ölçeklenir.
 */
public final class BeklemeDurumu {
    private long genelBitis;
    private final EnumMap<AfetTuru, Long> turBitis = new EnumMap<>(AfetTuru.class);

    public boolean genelBeklemede(long saat) {
        return saat < genelBitis;
    }

    public boolean turBeklemede(AfetTuru tur, long saat) {
        Long bitis = turBitis.get(tur);
        return bitis != null && saat < bitis;
    }

    public long genelKalan(long saat) {
        return Math.max(0, genelBitis - saat);
    }

    public long turKalan(AfetTuru tur, long saat) {
        Long bitis = turBitis.get(tur);
        return bitis == null ? 0 : Math.max(0, bitis - saat);
    }

    /** Afet bitti: genel ve tür beklemesi şiddete göre uzayarak, {@code afetSikligi}'na bölünerek başlar. */
    public void afetBitti(AfetTuru tur, int seviye, long saat, ZamanlayiciAyarlari ayarlar, double siklik) {
        double uzama = ayarlar.siddetUzamasi(tur, seviye);
        long genel = AfetZamanlayicisi.beklemeOlcekle(Math.round(ayarlar.genelBeklemeTick() * uzama), siklik);
        long kendi = AfetZamanlayicisi.beklemeOlcekle(Math.round(ayarlar.turBekleme(tur) * uzama), siklik);
        genelBitis = Math.max(genelBitis, saat + genel);
        turBitis.merge(tur, saat + kendi, Math::max);
    }

    /**
     * {@code afetSikligi} {@code eski}'den {@code yeni}'ye geçti: süren her beklemenin kalanı {@code eski / yeni} ile çarpılır
     * (on kat sıklık → kalan onda biri). Sıklıklardan biri 0 ise (seçim yok) dokunulmaz. Değişen bir bekleme varsa true.
     */
    public boolean siklikDegisti(long saat, double eski, double yeni) {
        if (!(eski > 0) || !(yeni > 0) || Double.isInfinite(eski) || Double.isInfinite(yeni) || eski == yeni) {
            return false;
        }
        double oran = eski / yeni;
        boolean degisti = false;
        long genel = genelKalan(saat);
        if (genel > 0) {
            genelBitis = saat + Math.round(genel * oran);
            degisti = true;
        }
        for (Map.Entry<AfetTuru, Long> girdi : turBitis.entrySet()) {
            long kalan = girdi.getValue() - saat;
            if (kalan > 0) {
                girdi.setValue(saat + Math.round(kalan * oran));
                degisti = true;
            }
        }
        return degisti;
    }

    /** Türü bir süre seçilemez yapar (N14: uygun yer yok). */
    public void ertele(AfetTuru tur, long saat, long sure) {
        turBitis.merge(tur, saat + Math.max(0, sure), Math::max);
    }

    public long genelBitis() {
        return genelBitis;
    }

    public Map<AfetTuru, Long> turBitisleri() {
        return Collections.unmodifiableMap(new EnumMap<>(turBitis));
    }

    /** Kayıttan yükler. */
    public void yukle(long genel, Map<AfetTuru, Long> turler) {
        genelBitis = genel;
        turBitis.clear();
        turBitis.putAll(turler);
    }

    public void sifirla() {
        genelBitis = 0;
        turBitis.clear();
    }
}
