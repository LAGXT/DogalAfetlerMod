package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Afet fabrikalarının kayıt sistemi. Afet işçileri mod kurucusunda kendi türlerini kaydeder:
 *
 * <pre>{@code
 * AfetKaydi.kaydet(AfetTuru.KASIRGA, Kasirga::new);
 * }</pre>
 *
 * Kayıtlı olmayan tür rastgele seçilmez ve {@code /dogalafet start} "henüz kayıtlı değil" der. Bir türü iki kez kaydetmek
 * hatadır. Fabrika her çağrıda yeni bir {@link Afet} üretmelidir.
 */
public final class AfetKaydi {
    /** Geçici kaydı geri alan tutamaç (yalnız GameTest'ler). */
    public interface GeciciKayit extends AutoCloseable {
        @Override
        void close();
    }

    private static final Map<AfetTuru, Supplier<? extends Afet>> FABRIKALAR = new EnumMap<>(AfetTuru.class);
    /** {@link OyunTestiAfeti#ozelliktenKaydet} ile kaydedilen türler (yalnız hata iletisi için). */
    private static final Set<AfetTuru> OYUN_TESTI = EnumSet.noneOf(AfetTuru.class);

    private AfetKaydi() {
    }

    public static synchronized void kaydet(AfetTuru tur, Supplier<? extends Afet> fabrika) {
        Objects.requireNonNull(tur, "tur");
        Objects.requireNonNull(fabrika, "fabrika");
        if (FABRIKALAR.containsKey(tur)) {
            throw new IllegalStateException(tur.kimlik() + " zaten kayıtlı" + (OYUN_TESTI.contains(tur)
                    ? " (oyun testi afeti olarak: gerçek afet kaydı DogalAfetler kurucusunda OyunTestiAfeti.ozelliktenKaydet'ten"
                            + " önce olmalı, K-E22)"
                    : ""));
        }
        FABRIKALAR.put(tur, fabrika);
    }

    /** Yalnız {@link OyunTestiAfeti#ozelliktenKaydet}: türü oyun testi afeti olarak kaydeder (tür kayıtsız olmalı). */
    static synchronized void oyunTestiKaydet(AfetTuru tur, Supplier<? extends Afet> fabrika) {
        kaydet(tur, fabrika);
        OYUN_TESTI.add(tur);
    }

    /**
     * Yalnız {@link OyunTestiAfeti#ozelliktenKaydet} ({@code !tür}, K-E23): kayıtlı gerçek afetin fabrikasını oyun testi afetiyle
     * değiştirir; geliştirme sunucusunun ömrü boyunca geri alınmaz.
     */
    static synchronized void oyunTestiEz(AfetTuru tur, Supplier<? extends Afet> fabrika) {
        if (!FABRIKALAR.containsKey(tur)) {
            throw new IllegalStateException(tur.kimlik() + " kayıtlı değil; ezilecek gerçek afet yok (K-E23)");
        }
        FABRIKALAR.put(tur, Objects.requireNonNull(fabrika, "fabrika"));
        OYUN_TESTI.add(tur);
    }

    public static synchronized boolean kayitliMi(AfetTuru tur) {
        return FABRIKALAR.containsKey(tur);
    }

    public static synchronized Set<AfetTuru> kayitliTurler() {
        return FABRIKALAR.isEmpty() ? EnumSet.noneOf(AfetTuru.class) : EnumSet.copyOf(FABRIKALAR.keySet());
    }

    /** Yeni bir afet örneği; tür kayıtlı değilse boş. */
    public static Optional<Afet> olustur(AfetTuru tur) {
        Supplier<? extends Afet> fabrika;
        synchronized (AfetKaydi.class) {
            fabrika = FABRIKALAR.get(tur);
        }
        if (fabrika == null) {
            return Optional.empty();
        }
        Afet afet = Objects.requireNonNull(fabrika.get(), () -> tur.kimlik() + " fabrikası null döndü");
        if (afet.tur() != tur) {
            throw new IllegalStateException(tur.kimlik() + " fabrikası " + afet.tur().kimlik() + " üretti");
        }
        return Optional.of(afet);
    }

    /**
     * Yalnız GameTest'ler için: türü geçici olarak verilen fabrikaya bağlar; {@link GeciciKayit#close()} önceki kaydı
     * (ya da kayıtsız hâli) geri getirir.
     */
    public static synchronized GeciciKayit geciciKaydet(AfetTuru tur, Supplier<? extends Afet> fabrika) {
        Supplier<? extends Afet> onceki = FABRIKALAR.put(tur, Objects.requireNonNull(fabrika, "fabrika"));
        return () -> {
            synchronized (AfetKaydi.class) {
                if (onceki == null) {
                    FABRIKALAR.remove(tur);
                } else {
                    FABRIKALAR.put(tur, onceki);
                }
            }
        };
    }
}
