package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * Bir oyuncu buz deliğinden bir şey tuttu ({@code MinecraftForge.EVENT_BUS}, sunucu ana thread'i; madde 12 #20 "Buz
 * Balıkçısı", #21 "Kutup Ustası"). Vanilla {@code ItemFishedEvent} iptal edilmeden geçtiğinde, olta {@code buz_deligi}
 * bloğundayken bir kez yayınlanır. {@link #yakalananlar()} oyuncunun aldığı yığınlardır (balık, çöp ya da hazine);
 * başarımlar {@link #balikSayisi()}'nı saymalıdır.
 */
public final class BalikTutulduEvent extends Event {
    private final ServerPlayer oyuncu;
    private final List<ItemStack> yakalananlar;
    private final BlockPos delik;

    public BalikTutulduEvent(ServerPlayer oyuncu, List<ItemStack> yakalananlar, BlockPos delik) {
        this.oyuncu = oyuncu;
        this.yakalananlar = List.copyOf(yakalananlar);
        this.delik = delik.immutable();
    }

    public ServerPlayer oyuncu() {
        return oyuncu;
    }

    /** Yakalanan yığınlar (değiştirilemez kopya; yığınları değiştirmeyin). */
    public List<ItemStack> yakalananlar() {
        return yakalananlar;
    }

    /** Deliğin konumu. */
    public BlockPos delik() {
        return delik;
    }

    /** Yakalanan buz balığı sayısı ({@code #dogalafetler:cig_kutup_baligi}); çöp ve hazine sayılmaz. */
    public int balikSayisi() {
        int n = 0;
        for (ItemStack y : yakalananlar) {
            if (y.is(BalikcilikEtiketleri.CIG_KUTUP_BALIGI)) {
                n += y.getCount();
            }
        }
        return n;
    }
}
