package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Bir oyuncunun azalan verim geçmişi (madde 14 "spam engeli"): her türün son yenme zamanı (oyun tick'i). Aynı tür son
 * yemeden itibaren pencere (vars. 60 sn) içinde tekrar yenirse etki config oranıyla (vars. 0,5) çarpılır; pencere son
 * yemeden sayılır, böylece sürekli spam sürekli yarım etki verir. Farklı türler birbirinden bağımsızdır. Saat geri
 * gitmişse (ör. dünya kopyası) ceza verilmez.
 */
public final class YemeGecmisi {
    private final Map<String, Long> son = new TreeMap<>();

    /** Bu yemenin verim çarpanını döndürür ve zamanı kaydeder. */
    public double ye(String tur, long simdiTick, EtkiAyarlari ayarlar) {
        double carpan = carpan(tur, simdiTick, ayarlar);
        son.put(Objects.requireNonNull(tur, "tur"), simdiTick);
        return carpan;
    }

    /** Şimdi yenirse verilecek çarpan (kayıt değişmez). */
    public double carpan(String tur, long simdiTick, EtkiAyarlari ayarlar) {
        return kalanTick(tur, simdiTick, ayarlar) > 0 ? ayarlar.azalanVerimOrani() : 1.0;
    }

    /** Türün azalan verim penceresinin kalanı (tick); pencere dışındaysa 0. */
    public long kalanTick(String tur, long simdiTick, EtkiAyarlari ayarlar) {
        Long zaman = son.get(tur);
        if (zaman == null) {
            return 0;
        }
        long gecen = simdiTick - zaman;
        long pencere = ayarlar.azalanVerimPenceresiTick();
        return gecen >= 0 && gecen < pencere ? pencere - gecen : 0;
    }

    /** Penceresi dolmuş kayıtları atar (oyuncu verisi küçük kalsın). */
    public void buda(long simdiTick, EtkiAyarlari ayarlar) {
        son.keySet().removeIf(tur -> kalanTick(tur, simdiTick, ayarlar) == 0);
    }

    /** Tür → son yenme tick'i (kopya, ada göre sıralı). */
    public Map<String, Long> kayitlar() {
        return new TreeMap<>(son);
    }

    public static YemeGecmisi kayitlardan(Map<String, Long> kayitlar) {
        YemeGecmisi g = new YemeGecmisi();
        g.son.putAll(kayitlar);
        return g;
    }
}
