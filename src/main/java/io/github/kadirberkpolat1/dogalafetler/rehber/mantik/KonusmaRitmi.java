package io.github.kadirberkpolat1.dogalafetler.rehber.mantik;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Konuşma ritmi ve yazının solma eğrisi (saf Java). Cümle kelimelerine bölünür; her kelimenin başında ağızdan bir buhar
 * parçası çıkar, 6 harften uzun kelimelerde her 6 harfte bir daha (uzun kelime tek nefesle bitmez). Harf başına
 * {@value #HARF_TICK} tick, kelimeler arası {@value #BOSLUK_TICK} tick, ilk kelimeden önce {@value #BASLANGIC_TICK} tick (yazı
 * belirirken ağız açılır). Sunucu (başkalarının gördüğü buhar) ve istemci (kendi buharı ve yazı) aynı metinden aynı ritmi
 * hesaplar; metin dilden bağımsız olsun diye ritim her zaman Türkçe cümleden çıkarılır.
 */
public final class KonusmaRitmi {
    /** Yazının ekranda kaldığı toplam süre (tick; giriş ve çıkış solması dahil). */
    public static final int SURE_TICK = 100;
    /** Yazının belirme süresi (tick). */
    public static final int GIRIS_TICK = 10;
    /** Yazının sönme süresi (tick). */
    public static final int CIKIS_TICK = 20;
    static final double HARF_TICK = 1.5;
    static final int BOSLUK_TICK = 4;
    static final int BASLANGIC_TICK = 6;
    static final int UZUN_KELIME = 6;

    private KonusmaRitmi() {
    }

    /**
     * Buhar parçalarının çıktığı tick'ler (artan, tekrarsız; {@link #SURE_TICK} altında). Boş metinde boş dizi.
     */
    public static int[] puflar(String metin) {
        List<Integer> tickler = new ArrayList<>();
        double t = BASLANGIC_TICK;
        for (String kelime : metin.trim().split("\\s+")) {
            if (kelime.isEmpty()) {
                continue;
            }
            for (int harf = 0; harf < kelime.length(); harf += UZUN_KELIME) {
                int tick = (int) Math.round(t + harf * HARF_TICK);
                if (tick < SURE_TICK && (tickler.isEmpty() || tickler.get(tickler.size() - 1) < tick)) {
                    tickler.add(tick);
                }
            }
            t += kelime.length() * HARF_TICK + BOSLUK_TICK;
        }
        return tickler.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Bu tick'te buhar çıkar mı ({@code puflar} artan sıralıdır). */
    public static boolean pufVar(int[] puflar, int tick) {
        return Arrays.binarySearch(puflar, tick) >= 0;
    }

    /** Yazının opaklığı 0–1: {@value #GIRIS_TICK} tick'te belirir, son {@value #CIKIS_TICK} tick'te söner; süre dışında 0. */
    public static float opaklik(float gecen, int sure) {
        if (gecen < 0f || gecen >= sure) {
            return 0f;
        }
        float giris = Math.min(1f, gecen / GIRIS_TICK);
        float cikis = Math.min(1f, (sure - gecen) / CIKIS_TICK);
        return Math.min(giris, cikis);
    }
}
