package io.github.kadirberkpolat1.dogalafetler.afet.sel;

import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelKurallari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.DurumaBagliIsiKaynagi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.KaynakTanimi;
import io.github.kadirberkpolat1.dogalafetler.su.IsiBolgesi;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Sel suyunu buharlaştıran ısı kaynaklarını (lav, ateş, kamp ateşi, yanan fırın, meşale…) oyuncuların çevresindeki yüklü chunk'larda
 * arar. Kaynak tablosu sıcaklık sisteminin config satırlarıdır (blok=güç:yarıçap, güç &gt; 0): tek bir ısı tablosu vardır. Tarama
 * ucuzdur: chunk bölümlerinin palet özetine ({@code maybeHas}) bakılır, yalnız ısı kaynağı bloku içeren, yüzeye yakın (8 blok altından
 * 40 blok üstüne) bölümler blok blok gezilir; tick başına en çok {@code taramaButcesiChunk} chunk işlenir ve bir tur döndükten
 * sonra baştan başlanır (yeni konulan ya da sönen kaynak birkaç saniyede yansır). Komşu bloklar 2×2×2'lik hücrelerde birleştirilir
 * (lav gölü yüzlerce blok değil birkaç bölge verir); chunk başına ve toplamda sınır vardır. Sunucu thread'inde çalışır.
 */
final class SelIsiTarayici {
    /** Bir chunk'tan en çok bu kadar bölge alınır (en genişler). */
    static final int CHUNK_TAVANI = 24;
    /** Pakete giren toplam bölge tavanı. */
    static final int TOPLAM_TAVAN = 384;
    /** İsı kaynağı yüzeyin (en üst blok) en çok bu kadar altında ve üstünde aranır: mağara ve yer altı lavı sel suyuna karışmaz. */
    private static final int YUZEY_ALTI = 8;
    private static final int YUZEY_USTU = 40;
    private static final int KUME_YENILEME = 40;

    private static SicaklikAyarlari.Ozet tabloOzeti;
    private static Map<Block, KaynakTanimi> tablo = Map.of();

    private final Map<Long, List<IsiBolgesi>> chunkBolgeleri = new HashMap<>();
    private final ArrayDeque<Long> sira = new ArrayDeque<>();
    private List<IsiBolgesi> birlesik = List.of();
    /** Son küme kuruluşu (oyun tick'i); ilk tick'te hemen kurulsun diye bir yenileme aralığı geride başlar. */
    private long sonKume = -KUME_YENILEME;
    private boolean kirli;

    /** Son taramadan birleştirilmiş bölgeler (değişmez). */
    List<IsiBolgesi> bolgeler() {
        return birlesik;
    }

    /** Her tick çağrılır; bölge listesi değiştiyse {@code true}. */
    boolean tick(ServerLevel level) {
        long simdi = level.getGameTime();
        if (sira.isEmpty() && simdi - sonKume >= KUME_YENILEME) {
            sonKume = simdi;
            kumeKur(level);
        }
        int butce = SelAyarlari.taramaButcesiChunk();
        for (int i = 0; i < butce && !sira.isEmpty(); i++) {
            long anahtar = sira.poll();
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(anahtar), ChunkPos.getZ(anahtar));
            List<IsiBolgesi> yeni = chunk == null ? List.of() : tara(level, chunk);
            List<IsiBolgesi> eski = yeni.isEmpty() ? chunkBolgeleri.remove(anahtar) : chunkBolgeleri.put(anahtar, yeni);
            if (!yeni.equals(eski == null ? List.of() : eski)) {
                kirli = true;
            }
        }
        if (!kirli || !sira.isEmpty()) {
            return false;
        }
        kirli = false;
        List<IsiBolgesi> yeni = new ArrayList<>();
        for (List<IsiBolgesi> l : chunkBolgeleri.values()) {
            yeni.addAll(l);
        }
        if (yeni.size() > TOPLAM_TAVAN) {
            yeni.sort((a, b) -> Float.compare(b.yaricap(), a.yaricap()));
            yeni = new ArrayList<>(yeni.subList(0, TOPLAM_TAVAN));
        }
        boolean degisti = !new HashSet<>(yeni).equals(new HashSet<>(birlesik)) || yeni.size() != birlesik.size();
        birlesik = List.copyOf(yeni);
        return degisti;
    }

    /** Taranacak chunk kümesi: oyuncuların çevresindeki yüklü chunk'lar, yakından uzağa; dışarıda kalanların bölgeleri silinir. */
    private void kumeKur(ServerLevel level) {
        int r = SelAyarlari.taramaYaricapiChunk();
        Set<Long> kume = new java.util.LinkedHashSet<>();
        for (ServerPlayer p : level.players()) {
            int pcx = p.getBlockX() >> 4;
            int pcz = p.getBlockZ() >> 4;
            for (int halka = 0; halka <= r; halka++) {
                for (int dx = -halka; dx <= halka; dx++) {
                    for (int dz = -halka; dz <= halka; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) == halka) {
                            kume.add(ChunkPos.asLong(pcx + dx, pcz + dz));
                        }
                    }
                }
            }
        }
        if (chunkBolgeleri.keySet().removeIf(k -> !kume.contains(k))) {
            kirli = true;
        }
        sira.addAll(kume);
    }

    private static List<IsiBolgesi> tara(ServerLevel level, LevelChunk chunk) {
        Map<Block, KaynakTanimi> t = tablo();
        if (t.isEmpty()) {
            return List.of();
        }
        int enAlcak = Integer.MAX_VALUE;
        int enYuksek = Integer.MIN_VALUE;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                enAlcak = Math.min(enAlcak, h);
                enYuksek = Math.max(enYuksek, h);
            }
        }
        int yAlt = Math.max(level.getMinBuildHeight(), enAlcak - YUZEY_ALTI);
        int yUst = Math.min(level.getMaxBuildHeight() - 1, enYuksek + YUZEY_USTU);
        int ilk = chunk.getSectionIndex(yAlt);
        int son = chunk.getSectionIndex(yUst);
        Map<Long, IsiBolgesi> hucreler = null;
        for (int i = ilk; i <= son; i++) {
            LevelChunkSection bolum = chunk.getSection(i);
            if (bolum.hasOnlyAir() || !bolum.maybeHas(durum -> t.containsKey(durum.getBlock()))) {
                continue;
            }
            int bolumY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int ly = 0; ly < 16; ly++) {
                int y = bolumY + ly;
                if (y < yAlt || y > yUst) {
                    continue;
                }
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        double yaricap = yaricap(t, bolum.getBlockState(lx, ly, lz));
                        if (yaricap <= 0) {
                            continue;
                        }
                        int yuzey = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                        if (y < yuzey - YUZEY_ALTI || y > yuzey + YUZEY_USTU) {
                            continue;
                        }
                        if (hucreler == null) {
                            hucreler = new LinkedHashMap<>();
                        }
                        int x = chunk.getPos().getMinBlockX() + lx;
                        int z = chunk.getPos().getMinBlockZ() + lz;
                        long hucre = BlockPos.asLong(x >> 1, y >> 1, z >> 1);
                        IsiBolgesi var = hucreler.get(hucre);
                        if (var == null || var.yaricap() < yaricap) {
                            hucreler.put(hucre, new IsiBolgesi(x, y, z, (float) yaricap));
                        }
                    }
                }
            }
        }
        if (hucreler == null) {
            return List.of();
        }
        List<IsiBolgesi> sonuc = new ArrayList<>(hucreler.values());
        if (sonuc.size() > CHUNK_TAVANI) {
            sonuc.sort((a, b) -> Float.compare(b.yaricap(), a.yaricap()));
            sonuc = new ArrayList<>(sonuc.subList(0, CHUNK_TAVANI));
        }
        return List.copyOf(sonuc);
    }

    /** Blok durumunun buharlaştırma yarıçapı (blok); ısı kaynağı değilse, sönükse ya da zayıfsa 0. */
    static double yaricap(Map<Block, KaynakTanimi> t, BlockState durum) {
        KaynakTanimi k = t.get(durum.getBlock());
        if (k == null || durum.hasProperty(BlockStateProperties.LIT) && !durum.getValue(BlockStateProperties.LIT)) {
            return 0;
        }
        double guc = k.guc();
        double yaricap = k.yaricap();
        if (durum.getBlock() instanceof DurumaBagliIsiKaynagi olcekleyen) {
            double carpan = olcekleyen.gucCarpani(durum);
            if (!(carpan > 0)) {
                return 0;
            }
            guc *= carpan;
            yaricap *= olcekleyen.yaricapCarpani(durum);
        }
        return SelKurallari.buharlasmaYaricapi(guc, yaricap, SelAyarlari.isiEnAzGuc(), SelAyarlari.isiYaricapCarpani());
    }

    /** Isıtıcı (güç &gt; 0) kaynak satırlarının blok tablosu; config değişince yeniden kurulur. */
    private static synchronized Map<Block, KaynakTanimi> tablo() {
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        if (tabloOzeti != ozet) {
            Map<Block, KaynakTanimi> yeni = new IdentityHashMap<>();
            for (KaynakTanimi k : ozet.kaynaklar()) {
                ResourceLocation ad = ResourceLocation.tryParse(k.blok());
                if (k.guc() > 0 && ad != null && ForgeRegistries.BLOCKS.containsKey(ad)) {
                    yeni.put(ForgeRegistries.BLOCKS.getValue(ad), k);
                }
            }
            tablo = yeni;
            tabloOzeti = ozet;
        }
        return tablo;
    }
}
