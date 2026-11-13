package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Boyut;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.EkIsi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Hissedilen;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.KaynakTanimi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.KaynakYigilmasi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yagis;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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
        KaynakOlcumu kaynak = kaynaklar(level, oyuncu, ozet);
        return new OrtamSicakligi.Girdi(boyut(level), biyomSicakligi, kurak, level.getDayTime(), ayak.getY(), yuzey,
                gokyuzu, yagis(level, biyom, goz, gokyuzu), oyuncu.isInWater(), kaynak.toplam(),
                donem.gunesEvresi(), donem.evreIlerlemesi(), donem.donmaIlerlemesi(),
                SicaklikKancalari.kisAgirligi(level, ayak), kaynak.adet());
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

    /**
     * Göz hizasına düşen yağış. Gök ışığı camdan geçtiği için {@code gokyuzu} tek başına yetmez: vanilla
     * {@code isRainingAt} gibi hareket engelleyen en üst blok da gözün üstünde olmamalı (cam çatı altı kuru kalır).
     */
    private static Yagis yagis(ServerLevel level, Biome biyom, BlockPos goz, boolean gokyuzu) {
        if (!level.isRaining() || !gokyuzu || level.dimensionType().hasCeiling()
                || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, goz).getY() > goz.getY()) {
            return Yagis.YOK;
        }
        return switch (biyom.getPrecipitationAt(goz)) {
            case NONE -> Yagis.YOK;
            case SNOW -> Yagis.KAR;
            case RAIN -> level.isThundering() ? Yagis.FIRTINA : Yagis.YAGMUR;
        };
    }

    /**
     * Yakın kaynakların ölçümü: {@code toplam} sınırlanmamış °C toplamıdır, {@code adet} menüde gösterilen blok kaynağı sayısı.
     */
    private record KaynakOlcumu(double toplam, int adet) {
    }

    /**
     * Yakın blokların ve geçici kaynakların sınırlanmamış °C toplamı. Blok kaynakları yığılır ({@link KaynakYigilmasi}): bir
     * yönde birden fazla kaynak varsa o yönün toplamı sayıyla artar, zıt yöndeki kaynaklar yığılmayı kısmen götürür.
     * Geçici kaynaklar (yangın, meteor, olta soğuması) kendi yenilenme örtüşmeleri sahte yığılma yaratacağı için sayılmaz,
     * olduğu gibi eklenir.
     */
    private static KaynakOlcumu kaynaklar(ServerLevel level, ServerPlayer oyuncu, SicaklikAyarlari.Ozet ozet) {
        Map<Block, KaynakTanimi> tablo = tablo(ozet);
        Vec3 merkez = oyuncu.position().add(0, 1, 0);
        double gecici = SicaklikKancalari.geciciKaynaklar(level, merkez);
        if (tablo.isEmpty()) {
            return new KaynakOlcumu(gecici, 0);
        }
        int r = SicaklikAyarlari.kaynakTaramaYaricapi();
        BlockPos m = BlockPos.containing(merkez);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double isi = 0;
        double soguk = 0;
        double isiAgirligi = 0;
        double soguAgirligi = 0;
        int adet = 0;
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
                    double guc = k.guc();
                    double yaricap = k.yaricap();
                    if (durum.getBlock() instanceof DurumaBagliIsiKaynagi olcekleyen) {
                        double carpan = olcekleyen.gucCarpani(durum);
                        if (!(carpan > 0)) {
                            continue;
                        }
                        guc *= carpan;
                        yaricap *= olcekleyen.yaricapCarpani(durum);
                    }
                    double mesafe = Math.sqrt(merkez.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
                    double katki = OrtamSicakligi.kaynakKatkisi(guc, mesafe, yaricap);
                    if (katki > 0) {
                        isi += katki;
                        isiAgirligi += KaynakYigilmasi.agirlik(katki);
                    } else if (katki < 0) {
                        soguk += katki;
                        soguAgirligi += KaynakYigilmasi.agirlik(katki);
                    }
                    if (KaynakYigilmasi.sayilir(katki)) {
                        adet++;
                    }
                }
            }
        }
        KaynakYigilmasi.Sonuc yigilmis = KaynakYigilmasi.birlestir(isi, isiAgirligi, soguk, soguAgirligi,
                ozet.ekIsi().yigilma());
        return new KaynakOlcumu(gecici + yigilmis.toplam(), adet);
    }

    /**
     * Çevre ve ekipmandan gelen ek ısı kalemleri ({@link EkIsi}, {@link IsiKaynaklari}): yün blokları, lavın içi, yakın
     * oyuncular, yünlü/termostatlı zırh, Termostat Plakası ve kayıtlı ısıtıcı/soğutucu eşyalar (Serin Kolye, Soğutmalı
     * zırh). Sıfır olmayanlar döner; {@code OrtamSicakligi.Sonuc#ekle} ile mekan sonucuna eklenir. Eşya kalemlerinin kaynak
     * sayıları {@code adetler}'dedir (menü dökümü).
     *
     * @param mekanC ek kalemler eklenmeden önceki mekan sıcaklığı (termostat yalnız soğukta ısıtır; hedefe çeken soğutucu bu
     *               değerden serin hedefe çeker)
     */
    public static EkIsi.Olcum ekKatkilar(ServerPlayer oyuncu, EkIsi.Ayarlar a, double mekanC) {
        EnumMap<Katki, Double> k = new EnumMap<>(Katki.class);
        double yun = EkIsi.yunBloklari(yunBloklari(oyuncu, a), a);
        if (yun != 0) {
            k.put(Katki.YUN_BLOKLARI, yun);
        }
        double lav = EkIsi.lavIcinde(oyuncu.isInLava(), a);
        if (lav != 0) {
            k.put(Katki.LAV_ICINDE, lav);
        }
        double yakin = EkIsi.yakinOyuncular(yakinOyuncuSayisi(oyuncu, a), a);
        if (yakin != 0) {
            k.put(Katki.YAKIN_OYUNCULAR, yakin);
        }
        EnumMap<Katki, Integer> adetler = new EnumMap<>(Katki.class);
        IsiKaynaklari.olc(oyuncu, a, mekanC, yun + lav + yakin, k, adetler);
        return new EkIsi.Olcum(k, adetler);
    }

    /** {@code WOOL} etiketli blokların sınırlanmamış °C toplamı (oyuncunun göğsü merkez; yarıçap config'te). */
    private static double yunBloklari(ServerPlayer oyuncu, EkIsi.Ayarlar a) {
        if (!(a.yunBlokGucu() > 0 && a.yunBlokYaricap() > 0)) {
            return 0;
        }
        ServerLevel level = oyuncu.serverLevel();
        Vec3 merkez = oyuncu.position().add(0, 1, 0);
        BlockPos m = BlockPos.containing(merkez);
        int r = (int) Math.ceil(a.yunBlokYaricap());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double toplam = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    p.set(m.getX() + dx, m.getY() + dy, m.getZ() + dz);
                    if (!level.getBlockState(p).is(BlockTags.WOOL)) {
                        continue;
                    }
                    double mesafe = Math.sqrt(merkez.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
                    toplam += EkIsi.yunBlogu(mesafe, a);
                }
            }
        }
        return toplam;
    }

    /** Yakın yarıçap içindeki diğer canlı oyuncular (izleyici ve yaratıcı modundakiler sayılmaz). */
    private static int yakinOyuncuSayisi(ServerPlayer oyuncu, EkIsi.Ayarlar a) {
        if (!(a.yakinOyuncuBasina() > 0 && a.yakinOyuncuYaricap() > 0)) {
            return 0;
        }
        double r2 = a.yakinOyuncuYaricap() * a.yakinOyuncuYaricap();
        int sayi = 0;
        for (ServerPlayer diger : oyuncu.serverLevel().players()) {
            if (diger != oyuncu && diger.isAlive() && !diger.isSpectator() && !diger.isCreative()
                    && diger.distanceToSqr(oyuncu) <= r2) {
                sayi++;
            }
        }
        return sayi;
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
