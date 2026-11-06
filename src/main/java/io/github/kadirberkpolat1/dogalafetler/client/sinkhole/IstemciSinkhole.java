package io.github.kadirberkpolat1.dogalafetler.client.sinkhole;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sinkhole istemci efektleri (tasarım notu sinkhole §2 "Kamera", "Çöküşün görselliği"): çekirdek çökerken boşluktan yukarı
 * patlayan toz bulutu (efekt kalitesiyle; sunucunun gönderdiği blok parçacıklarına ek) ve çukura düşen oyuncuda 6° FOV
 * genişlemesi (deprem {@code sarsintiCarpani} ve vanilla FOV efekti ölçeğiyle çarpılır; 0'da kapalı). Sarsıntı {@code client.deprem.IstemciSarsinti}'dadır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciSinkhole {
    private IstemciSinkhole() {
    }

    /** Çekirdek çöküşü başladı (istemci ana thread'i, {@code IstemciSarsinti}'dan). */
    public static void cokusTozu(AfetDurumPaketi p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null
                || mc.player.distanceToSqr(p.merkez().getX(), mc.player.getY(), p.merkez().getZ()) > 128 * 128) {
            return;
        }
        RandomSource r = level.random;
        double yaricap = p.ozel().getFloat("R");
        double y = p.ozel().getInt("z") + 1;
        double q = switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> 0.3;
            case ORTA -> 0.6;
            case YUKSEK -> 1.0;
            case ULTRA -> 1.6;
        };
        int n = (int) ((20 + 6 * yaricap) * q);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = Math.sqrt(r.nextDouble()) * yaricap * 0.6;
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, p.merkez().getX() + 0.5 + Math.cos(a) * d,
                    y + r.nextDouble(), p.merkez().getZ() + 0.5 + Math.sin(a) * d, Math.cos(a) * 0.03,
                    0.08 + r.nextDouble() * 0.1, Math.sin(a) * 0.03);
        }
    }

    @SubscribeEvent
    public static void gorusAcisi(ComputeFovModifierEvent olay) {
        Player oyuncu = olay.getPlayer();
        var durum = IstemciAfetDurumu.aktif();
        if (durum.isEmpty() || durum.get().tur() != AfetTuru.SINKHOLE || durum.get().asama() != AfetAsamasi.AKTIF
                || durum.get().altAsama() < SinkholeSeviyesi.COKUS || oyuncu.fallDistance < 2) {
            return;
        }
        AfetDurumPaketi p = durum.get();
        double d = Math.hypot(oyuncu.getX() - p.merkez().getX() - 0.5, oyuncu.getZ() - p.merkez().getZ() - 0.5);
        if (d <= p.ozel().getFloat("R") + 0.5 && oyuncu.getY() < p.ozel().getInt("z")) {
            double olcek = DepremIstemciAyarlari.sarsintiCarpani() * Minecraft.getInstance().options.fovEffectScale().get();
            olay.setNewFovModifier(olay.getNewFovModifier() * (float) (1 + 6.0 / 70.0 * olcek));
        }
    }
}
