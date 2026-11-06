package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Diğer alt sistemlerin ortam sıcaklığına girdi verdiği kancalar (yalnız sunucu ana thread'i):
 * <ul>
 * <li>{@link #ortamKaynagiEkle} — geçici ısı/soğuk kaynağı: yangın yöneticisi (bölüm G: "yangına yakın bölgelerde
 * ortam sıcaklığı yükselsin"), meteor kraterleri, afetler. Kaynak süre bitince kendiliğinden düşer; uzun süren olay
 * kaynağı aralıklarla yeniler.</li>
 * <li>{@link #kisBolgesiSaglayiciAyarla} — donma sisteminin kalıcı kış bölgesi ağırlığı (madde 9, kızıl cüce dönemi).</li>
 * </ul>
 */
public final class SicaklikKancalari {
    /** Boyut başına en çok geçici kaynak (eskisi düşer). */
    public static final int EN_COK_KAYNAK = 512;

    /** Bir noktanın kalıcı kış bölgesi ağırlığı (0–1). */
    @FunctionalInterface
    public interface KisBolgesiSaglayici {
        double agirlik(ServerLevel level, BlockPos konum);
    }

    private record Kaynak(Vec3 konum, double guc, double yaricap, long bitis) {
    }

    private static final Map<ResourceKey<Level>, List<Kaynak>> KAYNAKLAR = new HashMap<>();
    private static volatile KisBolgesiSaglayici kisBolgesi = (level, konum) -> 0.0;

    private SicaklikKancalari() {
    }

    /**
     * Geçici bir kaynak ekler.
     *
     * @param guc      kaynağın üstündeki °C katkısı (negatif = soğuk); mesafeyle {@code (1 − d/r)²} azalır
     * @param yaricap  blok (en çok 64)
     * @param sureTick oyun tick'i
     */
    public static void ortamKaynagiEkle(ServerLevel level, Vec3 konum, double guc, double yaricap, int sureTick) {
        Objects.requireNonNull(konum, "konum");
        if (sureTick <= 0 || yaricap <= 0 || guc == 0 || !Double.isFinite(guc)) {
            return;
        }
        List<Kaynak> liste = KAYNAKLAR.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        if (liste.size() >= EN_COK_KAYNAK) {
            liste.remove(0);
        }
        liste.add(new Kaynak(konum, guc, Math.min(64, yaricap), level.getGameTime() + sureTick));
    }

    /** {@code konum}'daki geçici kaynakların sınırlanmamış °C toplamı; süresi bitenler silinir. */
    static double geciciKaynaklar(ServerLevel level, Vec3 konum) {
        List<Kaynak> liste = KAYNAKLAR.get(level.dimension());
        if (liste == null || liste.isEmpty()) {
            return 0;
        }
        long simdi = level.getGameTime();
        double toplam = 0;
        for (Iterator<Kaynak> it = liste.iterator(); it.hasNext();) {
            Kaynak k = it.next();
            if (k.bitis() <= simdi) {
                it.remove();
                continue;
            }
            toplam += OrtamSicakligi.kaynakKatkisi(k.guc(), k.konum().distanceTo(konum), k.yaricap());
        }
        return toplam;
    }

    public static void kisBolgesiSaglayiciAyarla(KisBolgesiSaglayici saglayici) {
        kisBolgesi = Objects.requireNonNull(saglayici, "saglayici");
    }

    static double kisAgirligi(ServerLevel level, BlockPos konum) {
        double w = kisBolgesi.agirlik(level, konum);
        return Double.isFinite(w) ? Math.max(0, Math.min(1, w)) : 0;
    }

    /** Sunucu kapanınca geçici kaynaklar unutulur (kalıcı değildir). */
    static void temizle() {
        KAYNAKLAR.clear();
    }
}
