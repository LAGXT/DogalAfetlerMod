package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaApi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Sıcaklığa giren kancaların ({@code Kancalar}) oyun içi sınaması: yangın ısısı ve donmanın kış ağırlığı. */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SicaklikKancaOyunTestleri {
    private SicaklikKancaOyunTestleri() {
    }

    /** Yanan hücrenin yakınında oyuncu varken yangın yöneticisi sıcaklığa geçici ısı kaynağı bildirir (bölüm G). */
    @GameTest(template = "yangin_sahne", batch = "kanca_yangin_isi", timeoutTicks = 200)
    public static void yanginIsisiSicaklikKaynaginaGider(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos kutuk = new BlockPos(4, 1, 4);
        y.setBlock(kutuk, Blocks.OAK_LOG);
        BlockPos mutlak = y.absolutePos(kutuk);
        Vec3 nokta = Vec3.atCenterOf(mutlak);
        double once = SicaklikKancalari.geciciKaynaklar(level, nokta);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, nokta.x, mutlak.getY(), nokta.z + 3);
        y.assertTrue(YanginYoneticisi.tutustur(level, mutlak, YanginKaynagi.KOMUT, 0.1) > 0, "kütük tutuşmalı");
        y.succeedWhen(() -> {
            double simdi = SicaklikKancalari.geciciKaynaklar(level, nokta);
            y.assertTrue(simdi > once, "yangın ısısı sıcaklık kaynağına gelmedi: " + once + " -> " + simdi);
            y.assertTrue(Math.abs(SicaklikKancalari.kisAgirligi(level, mutlak)
                    - Math.max(0, Math.min(1, DonmaApi.kisAgirligi(level, mutlak)))) < 1e-9,
                    "sıcaklığın kış ağırlığı donmanınkiyle aynı olmalı");
            YanginYoneticisi.sondur(level, new AABB(mutlak).inflate(4));
            level.getServer().getPlayerList().remove(oyuncu);
        });
    }
}
