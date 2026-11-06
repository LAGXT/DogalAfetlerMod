package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisAlani;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisMaskesi;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;

/**
 * Kalıcı kış bölgesinin dünya bağı (madde 9 ÇÖZÜLME, K-D4, K0-24). Maske parametreleri (oran, dalga boyu, bant, Y geçişi,
 * ılık ofset) çözülme dönemi başlarken dünya dönemine ({@code ekVeri("donma").kis}) yazılır ve oradan okunur: config
 * sonradan değişse de kış bölgeleri kaymaz (spike B9); config yalnız henüz sabitlenmemiş döngü içindir. Biyom örneği
 * chunk ortasında deniz seviyesinin bir üstündeki gürültü biyomudur ({@code getUncachedNoiseBiome}): chunk yüklemez,
 * yalnız seed'e bağlıdır (yüklü olsun olmasın aynı sonuç). Soğuk = kar biyomu ({@code Tags.Biomes.IS_SNOWY}) ya da taban
 * sıcaklık &lt; 0,15. Ana thread'de kullanılır.
 */
public final class KisBolgesi implements KisAlani.ChunkOrnegi {
    static final String ALT_SISTEM = "donma";
    private static final String KIS = "kis";
    private static final int ONBELLEK_SINIRI = 200_000;

    private record Ornek(boolean soguk, float sicaklik) {
    }

    private static KisAlani alan;
    private static ServerLevel alanDunyasi;
    private static CompoundTag alanParametreleri;

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<Ornek> onbellek = new Long2ObjectOpenHashMap<>();

    private KisBolgesi(ServerLevel level) {
        this.level = level;
    }

    /** Üst dünyanın kış alanı (parametreler dünyaya sabitlenmişse onlarla). */
    public static KisAlani alan(ServerLevel level) {
        CompoundTag p = parametreler(DunyaDonemi.al(level));
        if (alan == null || alanDunyasi != level || !p.equals(alanParametreleri)) {
            KisMaskesi maske = new KisMaskesi(level.getSeed(), p.getDouble("oran"), p.getDouble("dalgaBoyu"),
                    p.getDouble("bant"));
            KisAlani.Ayar ayar = new KisAlani.Ayar(p.getInt("yAlt"), p.getInt("yUst"), p.getDouble("ofsetTaban"),
                    p.getDouble("ofsetKatsayi"), p.getDouble("ofsetEnCok"));
            alan = new KisAlani(maske, new KisBolgesi(level), ayar);
            alanDunyasi = level;
            alanParametreleri = p;
        }
        return alan;
    }

    /** Çözülme başlarken bir kez: config'in maske parametrelerini dünyaya yazar (zaten yazılıysa dokunmaz). */
    static void sabitle(DunyaDonemi donem) {
        CompoundTag ek = donem.ekVeri(ALT_SISTEM);
        if (ek.contains(KIS, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag p = configParametreleri();
        ek.put(KIS, p);
        donem.ekVeriDegisti(ALT_SISTEM);
        DogalAfetler.LOGGER.info("[donma] kalıcı kış parametreleri dünyaya sabitlendi: {}", p);
    }

    static boolean sabitMi(DunyaDonemi donem) {
        return donem.ekVeri(ALT_SISTEM).contains(KIS, Tag.TAG_COMPOUND);
    }

    private static CompoundTag parametreler(DunyaDonemi donem) {
        CompoundTag ek = donem.ekVeri(ALT_SISTEM);
        return ek.contains(KIS, Tag.TAG_COMPOUND) ? ek.getCompound(KIS) : configParametreleri();
    }

    private static CompoundTag configParametreleri() {
        KisAlani.Ayar a = DonmaAyarlari.kisAyar();
        CompoundTag p = new CompoundTag();
        p.putDouble("oran", DonmaAyarlari.kisOrani());
        p.putDouble("dalgaBoyu", DonmaAyarlari.kisDalgaBoyu());
        p.putDouble("bant", DonmaAyarlari.kisGecisBandi());
        p.putInt("yAlt", a.yAlt());
        p.putInt("yUst", Math.max(a.yAlt() + 1, a.yUst()));
        p.putDouble("ofsetTaban", a.ofsetSicaklikTabani());
        p.putDouble("ofsetKatsayi", a.ofsetKatsayisi());
        p.putDouble("ofsetEnCok", a.ofsetEnCok());
        return p;
    }

    static void sifirla() {
        alan = null;
        alanDunyasi = null;
        alanParametreleri = null;
    }

    private Ornek ornek(int cx, int cz) {
        long anahtar = ChunkPos.asLong(cx, cz);
        Ornek o = onbellek.get(anahtar);
        if (o == null) {
            int x = cx * 16 + 8;
            int z = cz * 16 + 8;
            int y = level.getSeaLevel() + 1;
            Holder<Biome> biyom = level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2);
            float sicaklik = biyom.value().getBaseTemperature();
            o = new Ornek(biyom.is(Tags.Biomes.IS_SNOWY) || sicaklik < 0.15f, sicaklik);
            if (onbellek.size() >= ONBELLEK_SINIRI) {
                onbellek.clear();
            }
            onbellek.put(anahtar, o);
        }
        return o;
    }

    @Override
    public boolean soguk(int cx, int cz) {
        return ornek(cx, cz).soguk();
    }

    @Override
    public double sicaklik(int cx, int cz) {
        return ornek(cx, cz).sicaklik();
    }
}
