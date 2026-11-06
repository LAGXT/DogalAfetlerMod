package io.github.kadirberkpolat1.dogalafetler.client.deprem;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.SarsintiZarfi;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.sinkhole.IstemciSinkhole;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Deprem ve sinkhole ekran sarsıntısı (madde 3: oyuncu başına; bölüm C "yakındakinde daha şiddetli"). Sunucudan ek paket
 * gelmez: {@code AfetDurumPaketi} (aşama/alt aşama değişiminde hemen, sonra 1 Hz) merkez, seviye ve afetin {@code ozel}
 * verisini (geçen süre, süre, genlik, yarıçap, yarık doğrultusu) taşır; aradaki tick'ler yerel saatle tahmin edilir.
 * Deprem: {@link SarsintiZarfi#deprem} zarfı × mesafe çarpanı, 8–14 Hz iki kanallı titreme + 1,2–2 Hz yuvarlanma, el ±2 piksel.
 * Sinkhole: çatlama/sarkmada 0,2° titreme, çekirdek çöküşünde 32 blok içinde tek sert sarsma ve aşağı kısa bakış çekimi.
 * Yarığın açık bölümünden ve oyuncunun çevresindeki zeminden toz ve çakıl sıçramaları (efekt kalitesiyle, 96 blok ötesi yok).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciSarsinti {
    private static final double TOZ_MENZILI = 96;
    private static final int TAHMIN_TAVANI = 40;

    private static long saat;
    private static int surum = -1;
    private static long afetNo;
    private static AfetAsamasi asama;
    private static int altAsama;
    private static int paketGecen;
    private static long paketSaati;
    private static long darbeSaati = Long.MIN_VALUE / 2;

    private IstemciSarsinti() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || mc.isPaused()) {
            return;
        }
        saat++;
        Optional<AfetDurumPaketi> durum = IstemciAfetDurumu.aktif();
        if (IstemciAfetDurumu.surum() != surum) {
            surum = IstemciAfetDurumu.surum();
            durum.ifPresent(IstemciSarsinti::paketGeldi);
        }
        if (mc.level != null && mc.player != null && durum.isPresent() && durum.get().tur() == AfetTuru.DEPREM
                && durum.get().asama() == AfetAsamasi.AKTIF) {
            toz(mc.level, mc.player, durum.get());
        }
    }

    private static void paketGeldi(AfetDurumPaketi p) {
        if (p.afetNo() != afetNo) {
            afetNo = p.afetNo();
            asama = null;
        }
        boolean cokus = p.tur() == AfetTuru.SINKHOLE && p.asama() == AfetAsamasi.AKTIF
                && p.altAsama() == SinkholeSeviyesi.COKUS;
        if (cokus && (asama != AfetAsamasi.AKTIF || altAsama != SinkholeSeviyesi.COKUS)) {
            darbeSaati = saat - Math.min(SarsintiZarfi.DARBE_TICK, p.ozel().getInt("g"));
            IstemciSinkhole.cokusTozu(p);
        }
        asama = p.asama();
        altAsama = p.altAsama();
        paketGecen = p.ozel().getInt("g");
        paketSaati = saat;
    }

    private static double gecen(float kismi) {
        return paketGecen + Math.min(TAHMIN_TAVANI, saat - paketSaati) + kismi;
    }

    /** Bu oyuncunun anlık sarsıntı genliği (derece) ve sinkhole bakış çekimi; {@code [genlik, cekim]}. */
    static double[] genlik(Player oyuncu, float kismi) {
        Optional<AfetDurumPaketi> durum = IstemciAfetDurumu.aktif();
        double carpan = DepremIstemciAyarlari.sarsintiCarpani();
        if (durum.isEmpty() || carpan <= 0 || durum.get().asama() != AfetAsamasi.AKTIF) {
            return new double[] {0, 0};
        }
        AfetDurumPaketi p = durum.get();
        double d = Math.hypot(oyuncu.getX() - p.merkez().getX() - 0.5, oyuncu.getZ() - p.merkez().getZ() - 0.5);
        if (p.tur() == AfetTuru.DEPREM) {
            double r = p.ozel().getFloat("r");
            double z = SarsintiZarfi.deprem(gecen(kismi), p.ozel().getInt("s"));
            return new double[] {p.ozel().getFloat("a") * z * SarsintiZarfi.mesafeCarpani(d, r, r + 64) * carpan, 0};
        }
        if (p.tur() == AfetTuru.SINKHOLE) {
            double r = p.ozel().getFloat("R");
            double taban = p.altAsama() <= SinkholeSeviyesi.SARKMA ? 0.2 * SarsintiZarfi.mesafeCarpani(d, r + 16, r + 48)
                    : 0;
            double darbe = d <= 32 ? SarsintiZarfi.darbe(saat - darbeSaati + kismi) : 0;
            return new double[] {(taban + p.ozel().getFloat("a") * darbe) * carpan, 1.5 * darbe * carpan};
        }
        return new double[] {0, 0};
    }

    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        Player oyuncu = Minecraft.getInstance().player;
        if (oyuncu == null) {
            return;
        }
        float kismi = (float) olay.getPartialTick();
        double[] g = genlik(oyuncu, kismi);
        if (g[0] <= 0.001 && g[1] <= 0.001) {
            return;
        }
        double t = (saat + kismi) / 20.0;
        olay.setYaw(olay.getYaw() + (float) (g[0] * SarsintiZarfi.titreme(t, 0)));
        olay.setPitch(olay.getPitch() + (float) (g[0] * 0.8 * SarsintiZarfi.titreme(t, 1) + g[1]));
        olay.setRoll(olay.getRoll() + (float) (g[0] * 0.5 * SarsintiZarfi.yuvarlanma(t)));
    }

    /** El eşyası ayrıca ±2 piksel sallanır (ana el olayında; yan el aynı pozu paylaşır). */
    @SubscribeEvent
    public static void el(RenderHandEvent olay) {
        Player oyuncu = Minecraft.getInstance().player;
        if (oyuncu == null || olay.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        double[] g = genlik(oyuncu, olay.getPartialTick());
        if (g[0] <= 0.001) {
            return;
        }
        double t = (saat + olay.getPartialTick()) / 20.0;
        double olcek = Math.min(1, g[0] / 2.0) * 0.008;
        olay.getPoseStack().translate(olcek * SarsintiZarfi.titreme(t, 1), olcek * SarsintiZarfi.titreme(t + 0.37, 0), 0);
    }

    static double kaliteCarpani() {
        return switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> 0.3;
            case ORTA -> 0.6;
            case YUKSEK -> 1.0;
            case ULTRA -> 1.6;
        };
    }

    private static void toz(ClientLevel level, Player oyuncu, AfetDurumPaketi p) {
        RandomSource r = level.random;
        double q = kaliteCarpani();
        double aci = p.ozel().getFloat("aci");
        double cephe = Math.min(p.ozel().getFloat("L"),
                p.ozel().getFloat("f") + Math.min(TAHMIN_TAVANI, saat - paketSaati) * 0.6);
        int n = adet(r, 6 * q);
        for (int i = 0; i < n && cephe > 0; i++) {
            double t = (r.nextDouble() * 2 - 1) * cephe;
            double yan = (r.nextDouble() - 0.5) * 3;
            double x = p.merkez().getX() + 0.5 + Math.cos(aci) * t - Math.sin(aci) * yan;
            double z = p.merkez().getZ() + 0.5 + Math.sin(aci) * t + Math.cos(aci) * yan;
            if (oyuncu.distanceToSqr(x, oyuncu.getY(), z) > TOZ_MENZILI * TOZ_MENZILI) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(x), (int) Math.floor(z));
            BlockState zemin = level.getBlockState(new BlockPos((int) Math.floor(x), y - 1, (int) Math.floor(z)));
            if (r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, (r.nextDouble() - 0.5) * 0.02, 0.03,
                        (r.nextDouble() - 0.5) * 0.02);
            } else if (!zemin.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, zemin), x, y + 0.5 + r.nextDouble() * 2,
                        z, 0, 0, 0);
            }
        }
        double[] g = genlik(oyuncu, 0);
        int sicrama = adet(r, 3 * q * Math.min(1, g[0]));
        for (int i = 0; i < sicrama; i++) {
            double x = oyuncu.getX() + (r.nextDouble() - 0.5) * 16;
            double z = oyuncu.getZ() + (r.nextDouble() - 0.5) * 16;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            BlockState zemin = level.getBlockState(new BlockPos((int) Math.floor(x), y - 1, (int) Math.floor(z)));
            if (!zemin.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, zemin), x, y + 0.05, z, 0, 0.12, 0);
            }
        }
    }

    static int adet(RandomSource r, double beklenen) {
        int tam = (int) beklenen;
        return tam + (r.nextDouble() < beklenen - tam ? 1 : 0);
    }
}
