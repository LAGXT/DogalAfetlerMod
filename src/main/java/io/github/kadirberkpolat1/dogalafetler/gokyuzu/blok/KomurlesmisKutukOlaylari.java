package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KutukIsisi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

/** Kömürleşmiş kütük oyun olayları (Forge veri yolu, yalnız sunucu etkili). */
public final class KomurlesmisKutukOlaylari {
    private KomurlesmisKutukOlaylari() {
    }

    /** Üzerine fırlatılan kar topu kütüğü söndürür (kademe 0); top her zamanki gibi parçalanır. */
    public static void karTopuCarpti(ProjectileImpactEvent olay) {
        if (!(olay.getProjectile() instanceof Snowball top) || !(olay.getRayTraceResult() instanceof BlockHitResult vurus)) {
            return;
        }
        Level level = top.level();
        if (level.isClientSide) {
            return;
        }
        BlockPos konum = vurus.getBlockPos();
        BlockState durum = level.getBlockState(konum);
        if (durum.is(PulsarKayitlari.KOMURLESMIS_KUTUK.get())) {
            KomurlesmisKutukBlogu.sondur(level, konum, durum);
        }
    }

    /**
     * Lav kovası dolumu kütüğü azami ısıya ({@link KutukIsisi#MAKS}) ulaştırdığında dolumu yapan oyuncuya "çayır çayır"
     * başarımını verir (tekrar verilirse {@link OzelBasarim#ver} sessizce yok sayar). Yalnız sunucuda çağrılır.
     */
    public static void isiYukseldi(Player oyuncu, int yeniIsi) {
        if (yeniIsi >= KutukIsisi.MAKS && oyuncu instanceof ServerPlayer sunucuOyuncu) {
            OzelBasarim.ver(sunucuOyuncu, "cayir_cayir");
        }
    }
}
