package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.DenizSeviyesiAlani;
import io.github.kadirberkpolat1.dogalafetler.su.MagaraZamani;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** İstemci tarafı (spike): alanın yerel hesabı, çekilmede chunk yeniden derleme, buhar parçacığı, su altı sisi. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuIstemci {
    private static Cekilme sonCekilme;
    private static int sayac;
    private static int sonHesap;

    private SuIstemci() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null) {
            return;
        }
        SanalSuDunyasi d = SanalSu.ISTEMCI;
        long t = l.getGameTime();
        Cekilme c = d.cekilme();
        if (c != sonCekilme) {
            kirlet(mc, sonCekilme);
            kirlet(mc, c);
            sonCekilme = c;
        }
        if (d.alanGerekli() && ++sayac % 10 == 0) {
            alanGuncelle(mc, l, d.alan());
        }
        d.temizlikYap(t);
        RandomSource r = l.random;
        for (SanalSuDunyasi.Magara m : d.magaralar()) {
            if (m.zaman().asama(t) != MagaraZamani.Asama.BUHARLASIYOR) {
                continue;
            }
            long[] s = m.cep().sutunAnahtarlari();
            double seviye = m.zaman().seviye(t);
            for (int i = 0; i < 8; i++) {
                long k = s[r.nextInt(s.length)];
                int x = (int) (k >> 32);
                int z = (int) k;
                if (m.cep().icerir(x, (int) Math.floor(seviye - 0.5), z)) {
                    l.addParticle(ParticleTypes.CLOUD, x + r.nextDouble(), seviye, z + r.nextDouble(), 0, 0.03, 0);
                }
            }
        }
    }

    private static void alanGuncelle(Minecraft mc, ClientLevel l, DenizSeviyesiAlani alan) {
        int r = Math.min(mc.options.getEffectiveRenderDistance(), 12) + 1;
        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;
        Set<Long> istenen = new HashSet<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                istenen.add(DenizSeviyesiAlani.anahtar(pcx + dx, pcz + dz));
                alan.iste(pcx + dx, pcz + dz);
            }
        }
        for (long k : alan.istenenler()) {
            if (!istenen.contains(k)) {
                alan.birak(k);
            }
        }
        long t0 = System.nanoTime();
        int okunan = alan.isle(SanalSu.okuyucu(l), 64);
        if (okunan > 0) {
            DogalAfetler.LOGGER.info("SPIKE-SU alan(istemci) okunan={} okuma={} ms parca={} bekleyen={}", okunan,
                    String.format(Locale.ROOT, "%.2f", (System.nanoTime() - t0) / 1e6), alan.parcaSayisi(),
                    alan.bekleyen());
        }
        if (alan.hesapSayisi() != sonHesap) {
            sonHesap = alan.hesapSayisi();
            DogalAfetler.LOGGER.info("SPIKE-SU alan(istemci) BFS#{} sutun={} sure={} ms (arka plan)", sonHesap,
                    alan.sonSutunSayisi(), String.format(Locale.ROOT, "%.2f", alan.sonHesapNs() / 1e6));
        }
    }

    /** İstemcide sunucu blok olayı yok; chunk (yeniden) yüklenince o chunk tekrar okunur. */
    @SubscribeEvent
    public static void chunkYuklendi(ChunkEvent.Load e) {
        if (e.getLevel().isClientSide() && SanalSu.ISTEMCI.alanGerekli()) {
            SanalSu.ISTEMCI.alan().gecersizKil(e.getChunk().getPos().x, e.getChunk().getPos().z);
        }
    }

    private static void kirlet(Minecraft mc, Cekilme c) {
        if (c == null) {
            return;
        }
        int x0 = (int) Math.floor(c.x() - c.yaricap());
        int z0 = (int) Math.floor(c.z() - c.yaricap());
        int x1 = (int) Math.ceil(c.x() + c.yaricap());
        int z1 = (int) Math.ceil(c.z() + c.yaricap());
        long t0 = System.nanoTime();
        mc.levelRenderer.setBlocksDirty(x0, 40, z0, x1, 70, z1);
        int bolum = ((x1 >> 4) - (x0 >> 4) + 3) * ((z1 >> 4) - (z0 >> 4) + 3) * ((70 >> 4) - (40 >> 4) + 3);
        DogalAfetler.LOGGER.info("SPIKE-SU cekilme yeniden derleme: bolum~{} isaretleme={} ms", bolum,
                String.format(Locale.ROOT, "%.3f", (System.nanoTime() - t0) / 1e6));
        SuRenderer.pencereBasla("cekilme_derleme", 3000);
    }

    static boolean kameraSuda(Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        SanalSuDunyasi d = SanalSu.dunya(mc.level);
        if (d == null || cam.getFluidInCamera() != FogType.NONE) {
            return false;
        }
        Vec3 p = cam.getPosition();
        return !Double.isNaN(d.ustu(p.x, p.y, p.z, mc.level.getGameTime()));
    }

    @SubscribeEvent
    public static void sis(ViewportEvent.RenderFog e) {
        if (kameraSuda(e.getCamera())) {
            e.setNearPlaneDistance(-8f);
            e.setFarPlaneDistance(40f);
            e.setFogShape(FogShape.SPHERE);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor e) {
        if (kameraSuda(e.getCamera())) {
            int c = Minecraft.getInstance().level.getBiome(e.getCamera().getBlockPosition()).value().getWaterFogColor();
            e.setRed((c >> 16 & 255) / 255f);
            e.setGreen((c >> 8 & 255) / 255f);
            e.setBlue((c & 255) / 255f);
        }
    }
}
