package io.github.kadirberkpolat1.dogalafetler.journal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.stats.Stats;

/**
 * Defterin "kat edilen mesafe" grupları: vanilla {@code *_ONE_CM} istatistiklerinin toplamı (santimetre). Düşme
 * ({@code FALL_ONE_CM}) yolculuk sayılmaz. İstatistikler vanilla'nın kendi sayacındadır ({@code ServerStatsCounter}); defter
 * ayrıca saymaz, açılışta okur.
 */
public enum MesafeGrubu {
    /** Yürüme, koşma, eğilerek yürüme ve tırmanma. */
    KARA(Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM, Stats.CROUCH_ONE_CM, Stats.CLIMB_ONE_CM),
    /** Yüzme, su altında ve su üstünde yürüme. */
    SU(Stats.SWIM_ONE_CM, Stats.WALK_UNDER_WATER_ONE_CM, Stats.WALK_ON_WATER_ONE_CM),
    /** Uçuş ve elytra. */
    HAVA(Stats.FLY_ONE_CM, Stats.AVIATE_ONE_CM),
    /** Maden arabası, tekne ve binekler. */
    TASIT(Stats.MINECART_ONE_CM, Stats.BOAT_ONE_CM, Stats.PIG_ONE_CM, Stats.HORSE_ONE_CM, Stats.STRIDER_ONE_CM);

    private final ResourceLocation[] istatistikler;

    MesafeGrubu(ResourceLocation... istatistikler) {
        this.istatistikler = istatistikler;
    }

    /** Grubun toplam mesafesi (cm); int sayaçların toplamı taşmasın diye long. */
    public long santimetre(StatsCounter sayac) {
        long toplam = 0;
        for (ResourceLocation kimlik : istatistikler) {
            Stat<ResourceLocation> istatistik = Stats.CUSTOM.get(kimlik);
            toplam += Math.max(0, sayac.getValue(istatistik));
        }
        return toplam;
    }
}
