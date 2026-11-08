package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Boyut;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Hissedilen;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.KaynakTanimi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yagis;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Oyuncunun çevresinden {@link OrtamSicakligi.Girdi} kurar (yalnız sunucu). Aynı girdinin gökyüzü, yağış, Y ve kaynak
 * bilgisi hissedilen sıcaklığın ({@link #hissedilenGirdisi}) ve ıslaklığın ({@link #islaklikKaynagi}) da girdisidir: çevre
 * güncelleme başına bir kez ölçülür.
 */
public final class OrtamOlcer {
    private static SicaklikAyarlari.Ozet kaynakOzeti;
    private static Map<Block, KaynakTanimi> kaynakTablosu = Map.of();

    private OrtamOlcer() {
    }

    public static OrtamSicakligi.Sonuc olc(ServerPlayer oyuncu, SicaklikAyarlari.Ozet ozet) {
        return OrtamSicakligi.hesapla(girdi(oyuncu, ozet), ozet.ortam());
    }

    public static OrtamSicakligi.Girdi girdi(ServerPlayer oyuncu, SicaklikAyarlari.Ozet ozet) {
        ServerLevel level = oyuncu.serverLevel();
        BlockPos ayak = oyuncu.blockPosition();
        Vec3 gozV = oyuncu.getEyePosition();
        BlockPos goz = BlockPos.containing(gozV);
        Biome biyom = level.getBiome(ayak).value();
        float biyomSicakligi = biyom.getBaseTemperature();
        boolean kurak = !biyom.hasPrecipitation() && biyomSicakligi > 1.0f;
        int yuzey = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ayak.getX(), ayak.getZ());
        boolean gokyuzu = level.canSeeSky(goz);
        DunyaDonemi donem = DunyaDonemi.al(level);
        return new OrtamSicakligi.Girdi(boyut(level), biyomSicakligi, kurak, level.getDayTime(), ayak.getY(), yuzey,
                gokyuzu, yagis(level, biyom, goz, gokyuzu), oyuncu.isInWater(), kaynaklar(level, oyuncu, ozet),
                donem.gunesEvresi(), donem.evreIlerlemesi(), donem.donmaIlerlemesi(),
                SicaklikKancalari.kisAgirligi(level, ayak));
    }

    /**
     * Islatma kaynağı (S3-03): suda ya da kabarcık sütununda SU; açık havada yağmur (RAIN yağışı, kar ıslatmaz) YAGMUR,
     * fırtınada FIRTINA; diğer her yer (çatı altı dahil) KURU. Yağış {@code girdi}'den gelir (göz hizasında, gökyüzü açıkken).
     */
    public static Islaklik.Kaynak islaklikKaynagi(ServerPlayer oyuncu, OrtamSicakligi.Girdi girdi) {
        if (oyuncu.isInWaterOrBubble()) {
            return Islaklik.Kaynak.SU;
        }
        return switch (girdi.yagis()) {
            case YAGMUR -> Islaklik.Kaynak.YAGMUR;
            case FIRTINA -> Islaklik.Kaynak.FIRTINA;
            case YOK, KAR -> Islaklik.Kaynak.KURU;
        };
    }

    /** Hissedilen sıcaklığın girdisi: mekan °C, ıslaklık, gökyüzü/yağış/Y (rüzgâr), koşma ve sol el. */
    public static Hissedilen.Girdi hissedilenGirdisi(ServerPlayer oyuncu, OrtamSicakligi.Girdi girdi, double mekanC,
            double islaklik, SolEl solEl) {
        return new Hissedilen.Girdi(mekanC, islaklik, girdi.gokyuzu() && !oyuncu.serverLevel().dimensionType().hasCeiling(),
                girdi.y(), girdi.yagis(), oyuncu.isSprinting(), solEl);
    }

    static Boyut boyut(ServerLevel level) {
        if (level.dimension() == Level.NETHER || level.dimensionType().ultraWarm()) {
            return Boyut.NETHER;
        }
        if (level.dimension() == Level.END) {
            return Boyut.END;
        }
        return Boyut.NORMAL;
    }

    private static Yagis yagis(ServerLevel level, Biome biyom, BlockPos goz, boolean gokyuzu) {
        if (!level.isRaining() || !gokyuzu || level.dimensionType().hasCeiling()) {
            return Yagis.YOK;
        }
        return switch (biyom.getPrecipitationAt(goz)) {
            case NONE -> Yagis.YOK;
            case SNOW -> Yagis.KAR;
            case RAIN -> level.isThundering() ? Yagis.FIRTINA : Yagis.YAGMUR;
        };
    }

    /** Yakın blokların ve geçici kaynakların sınırlanmamış °C toplamı. */
    private static double kaynaklar(ServerLevel level, ServerPlayer oyuncu, SicaklikAyarlari.Ozet ozet) {
        Map<Block, KaynakTanimi> tablo = tablo(ozet);
        Vec3 merkez = oyuncu.position().add(0, 1, 0);
        double toplam = SicaklikKancalari.geciciKaynaklar(level, merkez);
        if (tablo.isEmpty()) {
            return toplam;
        }
        int r = SicaklikAyarlari.kaynakTaramaYaricapi();
        BlockPos m = BlockPos.containing(merkez);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    p.set(m.getX() + dx, m.getY() + dy, m.getZ() + dz);
                    BlockState durum = level.getBlockState(p);
                    if (durum.isAir()) {
                        continue;
                    }
                    KaynakTanimi k = tablo.get(durum.getBlock());
                    if (k == null || (durum.hasProperty(BlockStateProperties.LIT)
                            && !durum.getValue(BlockStateProperties.LIT))) {
                        continue;
                    }
                    double mesafe = Math.sqrt(merkez.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
                    toplam += OrtamSicakligi.kaynakKatkisi(k.guc(), mesafe, k.yaricap());
                }
            }
        }
        return toplam;
    }

    private static synchronized Map<Block, KaynakTanimi> tablo(SicaklikAyarlari.Ozet ozet) {
        if (kaynakOzeti != ozet) {
            Map<Block, KaynakTanimi> yeni = new IdentityHashMap<>();
            for (KaynakTanimi k : ozet.kaynaklar()) {
                ResourceLocation ad = ResourceLocation.tryParse(k.blok());
                if (ad != null && ForgeRegistries.BLOCKS.containsKey(ad)) {
                    yeni.put(ForgeRegistries.BLOCKS.getValue(ad), k);
                }
            }
            kaynakTablosu = yeni;
            kaynakOzeti = ozet;
        }
        return kaynakTablosu;
    }
}
