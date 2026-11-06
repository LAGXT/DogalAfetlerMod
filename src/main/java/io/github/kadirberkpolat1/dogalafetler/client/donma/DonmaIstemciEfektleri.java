package io.github.kadirberkpolat1.dogalafetler.client.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaAyarlari;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaEsikleri;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dünya donmasının istemci efektleri (madde 9, tasarım notu §2 ve §5; yazı yok). Kaynak yalnız {@link IstemciDonem}
 * ({@code DonemPaketi}: donma ilerlemesi, çözülme dönemi, buharlaşma derinliği):
 * <ul>
 * <li>Ayaz: ilerlemeyle artan, süzülen buz kristalleri (göğü gören yerlerde).</li>
 * <li>Kar yağışı: ilerleme eşiği aşılınca yağmur istemcide kar olur. Biyom sıcaklığına ve sunucu havasına dokunulmaz;
 * yağmur sürdükçe yerel dünyanın yağmur düzeyi 0'a çekilir (vanilla yağmur çizilmez) ve yerine iri, yavaş kar taneleri
 * düşer; eşiğin altına inince yağmur düzeyi geri verilir.</li>
 * <li>Buhar: güneş büyürken (buharlaşma) ve donarken açık kalan su yüzeyinden yükselen deniz dumanu.</li>
 * <li>Nefes buharı: donmuş dünyada oyuncuların ağzından 2 sn'de bir.</li>
 * <li>Çözülme damlası: kızıl cüce çözülmesinde yapraklardan süzülen damlalar.</li>
 * </ul>
 * Yoğunluk efekt kalitesi (Düşük 0,3 … Ultra 1,5) × CLIENT {@code donma.parcacikYogunlugu}; yalnız oyuncu çevresinde.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class DonmaIstemciEfektleri {
    private static boolean yagmuruBastiriyoruz;
    private static float karSiddeti;
    private static int sayac;

    private DonmaIstemciEfektleri() {
    }

    private static double kaliteCarpani() {
        EfektKalitesi k = IstemciAyarlari.efektKalitesi();
        double c = switch (k) {
            case DUSUK -> 0.3;
            case ORTA -> 0.6;
            case YUKSEK -> 1.0;
            case ULTRA -> 1.5;
        };
        return c * DonmaAyarlari.parcacikYogunlugu();
    }

    private static int karYagisiEsigi() {
        try {
            return DonmaAyarlari.karYagisiEsigi();
        } catch (IllegalStateException yuklenmedi) {
            return DonmaEsikleri.VARSAYILAN_KAR_YAGISI;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer oyuncu = mc.player;
        if (level == null || oyuncu == null || mc.isPaused()) {
            yagmuruBastiriyoruz = false;
            karSiddeti = 0f;
            return;
        }
        DonemPaketi durum = IstemciDonem.durum();
        boolean ustDunya = level.dimension() == Level.OVERWORLD;
        float g = ustDunya ? durum.donmaIlerlemesi() : 0f;
        yagis(level, g >= karYagisiEsigi());
        if (!ustDunya) {
            return;
        }
        double c = kaliteCarpani();
        RandomSource r = level.random;
        sayac++;
        if (g > 0f) {
            ayaz(level, oyuncu, r, g, c);
            if (sayac % 40 == 0) {
                nefes(level, oyuncu, mc, r);
            }
        }
        if (karSiddeti > 0f) {
            kar(level, oyuncu, r, c);
        }
        float buhar = durum.buharlasmaDerinligi();
        if (buhar > 0.01f || g >= 30f && g < 99f) {
            buhar(level, oyuncu, r, c * (buhar > 0.01f ? 1.0 : 0.5));
        }
        if (durum.cozulmeDonemi() && g < 99f) {
            damla(level, oyuncu, r, c);
        }
    }

    /** Yağmuru istemcide kara çevirir; biyom ve sunucu havası değişmez. */
    private static void yagis(ClientLevel level, boolean karModu) {
        boolean yagiyor = level.getLevelData().isRaining();
        if (karModu && yagiyor) {
            yagmuruBastiriyoruz = true;
            level.setRainLevel(0f);
            karSiddeti = Math.min(1f, karSiddeti + 0.01f);
        } else {
            if (yagmuruBastiriyoruz) {
                yagmuruBastiriyoruz = false;
                if (yagiyor) {
                    level.setRainLevel(1f);
                }
            }
            karSiddeti = Math.max(0f, karSiddeti - 0.01f);
        }
    }

    private static int sayi(RandomSource r, double ortalama) {
        int n = (int) ortalama;
        return n + (r.nextDouble() < ortalama - n ? 1 : 0);
    }

    private static void ayaz(ClientLevel level, LocalPlayer o, RandomSource r, float g, double c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = sayi(r, g / 100.0 * 5.0 * c); i > 0; i--) {
            double x = o.getX() + (r.nextDouble() - 0.5) * 24;
            double y = o.getY() + r.nextDouble() * 8 - 1;
            double z = o.getZ() + (r.nextDouble() - 0.5) * 24;
            if (level.canSeeSky(p.set(x, y, z))) {
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (r.nextDouble() - 0.5) * 0.02, -0.01 - r.nextDouble() * 0.02,
                        (r.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    private static void kar(ClientLevel level, LocalPlayer o, RandomSource r, double c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = sayi(r, 28.0 * c * karSiddeti); i > 0; i--) {
            double x = o.getX() + (r.nextDouble() - 0.5) * 32;
            double z = o.getZ() + (r.nextDouble() - 0.5) * 32;
            double y = o.getY() + 6 + r.nextDouble() * 10;
            int zemin = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (y > zemin && level.canSeeSky(p.set(x, Math.max(y, zemin), z))) {
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.03 + (r.nextDouble() - 0.5) * 0.03, -0.12 - r.nextDouble() * 0.06,
                        (r.nextDouble() - 0.5) * 0.03);
            }
        }
    }

    private static void buhar(ClientLevel level, LocalPlayer o, RandomSource r, double c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = sayi(r, 6.0 * c); i > 0; i--) {
            int x = (int) Math.floor(o.getX() + (r.nextDouble() - 0.5) * 32);
            int z = (int) Math.floor(o.getZ() + (r.nextDouble() - 0.5) * 32);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            BlockState s = level.getBlockState(p.set(x, y, z));
            if (s.is(Blocks.WATER) && s.getFluidState().isSource() && level.getBlockState(p.set(x, y + 1, z)).isAir()) {
                level.addParticle(ParticleTypes.CLOUD, x + r.nextDouble(), y + 1.05, z + r.nextDouble(),
                        (r.nextDouble() - 0.5) * 0.01, 0.015 + r.nextDouble() * 0.02, (r.nextDouble() - 0.5) * 0.01);
            }
        }
    }

    private static void nefes(ClientLevel level, LocalPlayer kendi, Minecraft mc, RandomSource r) {
        boolean herkes = IstemciAyarlari.efektKalitesi() != EfektKalitesi.DUSUK;
        for (Player p : level.players()) {
            if (p != kendi && (!herkes || p.distanceToSqr(kendi) > 32 * 32) || p == kendi && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }
            Vec3 bak = p.getLookAngle();
            Vec3 agiz = p.getEyePosition().add(bak.scale(0.35)).add(0, -0.12, 0);
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.CLOUD, agiz.x, agiz.y, agiz.z, bak.x * 0.03 + (r.nextDouble() - 0.5) * 0.01,
                        0.008, bak.z * 0.03 + (r.nextDouble() - 0.5) * 0.01);
            }
        }
    }

    private static void damla(ClientLevel level, LocalPlayer o, RandomSource r, double c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = sayi(r, 4.0 * c); i > 0; i--) {
            int x = (int) Math.floor(o.getX() + (r.nextDouble() - 0.5) * 24);
            int z = (int) Math.floor(o.getZ() + (r.nextDouble() - 0.5) * 24);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            if (level.getBlockState(p.set(x, y, z)).is(BlockTags.LEAVES) && level.getBlockState(p.set(x, y - 1, z)).isAir()
                    && r.nextFloat() < 0.5f) {
                level.addParticle(ParticleTypes.DRIPPING_WATER, x + r.nextDouble(), y - 0.05, z + r.nextDouble(), 0, 0, 0);
            }
        }
    }
}
