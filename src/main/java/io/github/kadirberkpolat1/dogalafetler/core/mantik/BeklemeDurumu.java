package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Genel ve afet başına bekleme (madde 4, 10). Bitiş anları ölçekli afet saatiyle tutulur; bekleme afet bittiğinde başlar.
 * Kısa bir bekleme (ör. N14 ertelemesi) uzun olanı silmez.
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

    /** Afet bitti: genel ve tür beklemesi şiddete göre uzayarak başlar. */
    public void afetBitti(AfetTuru tur, int seviye, long saat, ZamanlayiciAyarlari ayarlar) {
        double uzama = ayarlar.siddetUzamasi(tur, seviye);
        genelBitis = Math.max(genelBitis, saat + Math.round(ayarlar.genelBeklemeTick() * uzama));
        turBitis.merge(tur, saat + Math.round(ayarlar.turBekleme(tur) * uzama), Math::max);
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
