package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Bir seçim turu (madde 3, 4, N11, N14): zamanlayıcı türü seçer; bölgesel tür hedef adaylarının sırasıyla
 * ({@link HedefSecici}) denenir, sunucu geneli tür hedefsiz denenir. Hiçbir hedefte başlayamayan tür {@code ertelemeTick}
 * ertelenir ve beklemede olmayan türlerden ağırlıkla sıradaki seçilir. Hedef adayı yoksa (ayarlı boyutlarda oyuncu yok)
 * yalnız sunucu geneli türler seçilebilir. Sunucudaki yönetici ve testlerdeki simülasyon aynı yolu kullanır.
 */
public final class SecimTuru {
    /** Türü hedefte (sunucu geneli türde {@code null}) başlatmayı dener; başladıysa {@code true}. */
    @FunctionalInterface
    public interface Baslatici<T> {
        boolean dene(AfetTuru tur, T hedef);
    }

    /** Turun sonucu: başlayan tür ({@code null}: başlamadı), hedefi ve bu turda ertelenen türler. */
    public record Sonuc<T>(AfetTuru tur, T hedef, Set<AfetTuru> ertelenenler) {
        public boolean basladi() {
            return tur != null;
        }
    }

    private SecimTuru() {
    }

    public static <T> Sonuc<T> isle(ZamanlayiciAyarlari ayarlar, AfetZamanlayicisi.Girdi girdi,
            List<HedefSecici.Aday<T>> adaylar, T sonHedef, Baslatici<T> baslatici, RandomGenerator rastgele) {
        Set<AfetTuru> uygun = EnumSet.noneOf(AfetTuru.class);
        uygun.addAll(girdi.uygunTurler());
        if (adaylar.isEmpty()) {
            uygun.removeIf(tur -> !tur.sunucuGeneli());
        }
        Set<AfetTuru> ertelenen = EnumSet.noneOf(AfetTuru.class);
        Optional<AfetTuru> secim = AfetZamanlayicisi.sec(ayarlar, girdi.ile(uygun, girdi.zorla()), rastgele);
        while (secim.isPresent()) {
            AfetTuru tur = secim.get();
            if (tur.sunucuGeneli()) {
                if (baslatici.dene(tur, null)) {
                    return new Sonuc<>(tur, null, Collections.unmodifiableSet(ertelenen));
                }
            } else {
                for (T hedef : HedefSecici.sirala(adaylar, sonHedef, rastgele)) {
                    if (baslatici.dene(tur, hedef)) {
                        return new Sonuc<>(tur, hedef, Collections.unmodifiableSet(ertelenen));
                    }
                }
            }
            girdi.bekleme().ertele(tur, girdi.saat(), ayarlar.ertelemeTick());
            ertelenen.add(tur);
            uygun.removeIf(aday -> girdi.bekleme().turBeklemede(aday, girdi.saat()));
            secim = AfetZamanlayicisi.sec(ayarlar, girdi.ile(uygun, true), rastgele);
        }
        return new Sonuc<>(null, null, Collections.unmodifiableSet(ertelenen));
    }
}
