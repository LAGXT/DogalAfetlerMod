package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaTablosuYukleyici.Tablo;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/**
 * Chunk seviyelerini hedefe yetiştiren kuyruk (madde 9 YENİ YÜKLENEN CHUNK'LAR, ÖNCELİK; madde 3 TPS).
 *
 * <ul>
 * <li>{@code ChunkEvent.Load} (ana thread) chunk'ı yüklü kümeye ve bekleyenlere ekler; {@code Unload} çıkarır.</li>
 * <li>Her tick bekleyenler en yakın oyuncuya uzaklığa göre sıralı (20 tick'te bir yeniden) işlenir. Sıradaki chunk'ın
 * seviyesi hedefe en çok {@code seviyeAdimi} kadar yaklaşır: 256 sütun yeni seviyeye getirilir, bitince seviye
 * capability'ye yazılır. Geçiş tick'ler arasında bölünür; bir sütun 1, bir blok değişimi 8 birim tutar.</li>
 * <li>Bütçe TPS 18'in altında yarıya, 15'in altında çeyreğe iner; ayrıca bir tick'te en çok {@code tickMaksMs}.</li>
 * <li>Yalnız {@code BLOCK_TICKING} chunk'lar işlenir: daha düşük durumdaki chunk'ta {@code setBlock} istemcilere
 * gitmez ({@code Level.markAndNotifyBlock}, {@code ChunkHolder.blockChanged}).</li>
 * <li>{@code afetBlokHasari} kapalıyken kuyruk durur, seviyeler değişmez; açılınca kaldığı yerden sürer.</li>
 * </ul>
 */
public final class DonmaZamanlayici {
    private static final int[] SUTUN_SIRASI = sutunSirasi();
    private static final int BFS_MALIYETI = 64;

    private static final LongOpenHashSet YUKLU = new LongOpenHashSet();
    private static final LongLinkedOpenHashSet BEKLEYEN = new LongLinkedOpenHashSet();
    private static final Long2ObjectOpenHashMap<ChunkIsi> ISLER = new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet IZLENEN = new LongOpenHashSet();
    private static long[] sira = new long[0];
    private static int siraBasi;
    private static boolean siraKirli = true;

    private static KisMaskesi maske;
    private static long maskeSeed;

    static volatile int yapayYukMs;

    static long tamamlananAdim;
    static long hedefeUlasan;
    static long toplamDegisen;
    static long toplamSutun;
    static int sonButce = DonmaAyarlari.temelButce;
    static int sonBolen = 1;
    static double sonTps = 20.0;
    private static long ozetAdim;
    private static long ozetUlasan;
    private static long ozetDegisen;
    private static long ozetSutun;
    private static long ozetNs;
    private static long ozetEnUzunNs;
    private static int ozetSureSiniri;

    private DonmaZamanlayici() {
    }

    private static final class ChunkIsi {
        final int baslangic;
        final int bitis;
        final int hedef;
        final boolean cozulme;
        final long baslangicTick;
        int[] kiyi;
        int sutun;
        int degisen;

        ChunkIsi(int baslangic, int bitis, int hedef, boolean cozulme, long baslangicTick) {
            this.baslangic = baslangic;
            this.bitis = bitis;
            this.hedef = hedef;
            this.cozulme = cozulme;
            this.baslangicTick = baslangicTick;
        }
    }

    /** {@code loadLevel}'dan önce gelir; {@code ServerStartingEvent} spawn chunk'ları yüklendikten sonra gelir ve kümeyi silerdi. */
    static void sunucuBasliyor(ServerAboutToStartEvent olay) {
        YUKLU.clear();
        BEKLEYEN.clear();
        ISLER.clear();
        IZLENEN.clear();
        sira = new long[0];
        siraKirli = true;
        maske = null;
        yapayYukMs = 0;
        tamamlananAdim = hedefeUlasan = toplamDegisen = toplamSutun = 0;
    }

    private static boolean ustDunya(Object level) {
        return level instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD;
    }

    static void chunkYuklendi(ChunkEvent.Load olay) {
        if (!ustDunya(olay.getLevel()) || !(olay.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        long anahtar = chunk.getPos().toLong();
        YUKLU.add(anahtar);
        BEKLEYEN.add(anahtar);
        siraKirli = true;
        if (IZLENEN.contains(anahtar)) {
            DogalAfetler.LOGGER.info("[donma] izlenen chunk {} yüklendi (yeni={}): kayıtlı seviye {}", chunk.getPos(),
                    olay.isNewChunk(), ChunkDonmaSeviyesi.oku(chunk));
        }
    }

    static void chunkBosaltildi(ChunkEvent.Unload olay) {
        if (!ustDunya(olay.getLevel()) || !(olay.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        long anahtar = chunk.getPos().toLong();
        YUKLU.remove(anahtar);
        BEKLEYEN.remove(anahtar);
        ChunkIsi yarim = ISLER.remove(anahtar);
        if (IZLENEN.contains(anahtar)) {
            DogalAfetler.LOGGER.info("[donma] izlenen chunk {} boşaltıldı: seviye {} chunk NBT'sine yazılacak{}",
                    chunk.getPos(), ChunkDonmaSeviyesi.oku(chunk),
                    yarim == null ? "" : " (yarım geçiş " + yarim.baslangic + "→" + yarim.bitis + " bırakıldı)");
        }
    }

    /** Global ilerleme ya da dönem değişti: bütün yüklü chunk'lar yeniden hedefe bakılır. */
    static void hepsiniKuyrugaAl() {
        BEKLEYEN.addAll(YUKLU);
        ISLER.clear();
        siraKirli = true;
    }

    static void izle(long anahtar) {
        IZLENEN.add(anahtar);
    }

    static int bekleyenSayisi() {
        return BEKLEYEN.size();
    }

    static int yukluSayisi() {
        return YUKLU.size();
    }

    static int surenIsSayisi() {
        return ISLER.size();
    }

    static KisMaskesi maske(ServerLevel level) {
        long seed = level.getSeed();
        if (maske == null || maskeSeed != seed) {
            maske = new KisMaskesi(seed, DonmaAyarlari.kisOrani, DonmaAyarlari.kisDalgaBoyu, DonmaAyarlari.kisBant);
            maskeSeed = seed;
        }
        return maske;
    }

    static boolean sogukBiyom(ServerLevel level, LevelChunk chunk) {
        ChunkPos p = chunk.getPos();
        int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8);
        Holder<Biome> biyom = level.getBiome(new BlockPos(p.getMiddleBlockX(), y, p.getMiddleBlockZ()));
        return biyom.is(Tags.Biomes.IS_COLD_OVERWORLD) || biyom.value().getBaseTemperature() < 0.15f;
    }

    static double kisAgirligi(ServerLevel level, LevelChunk chunk) {
        ChunkPos p = chunk.getPos();
        return DonmaHedefi.kisAgirligi(maske(level).agirlik(p.x, p.z), sogukBiyom(level, chunk));
    }

    static int hedef(ServerLevel level, LevelChunk chunk, DonmaVerisi veri) {
        return DonmaHedefi.hedef(veri.ilerleme(), veri.cozulme(), veri.cozulme() ? kisAgirligi(level, chunk) : 0.0);
    }

    /** START: {@code tickServer} süre ölçümüne girer; END ise {@code tickTimes} yazıldıktan sonra gelir ve TPS'e yansımazdı. */
    static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase == TickEvent.Phase.START && yapayYukMs > 0) {
            try {
                Thread.sleep(yapayYukMs);
            } catch (InterruptedException hata) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static void tick(TickEvent.LevelTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !ustDunya(olay.level)) {
            return;
        }
        ServerLevel level = (ServerLevel) olay.level;
        MinecraftServer sunucu = level.getServer();
        int tick = sunucu.getTickCount();
        if (tick % 20 == 0) {
            sonTps = TpsKismasi.tps(sunucu.tickTimes);
            int bolen = TpsKismasi.bolen(sonTps, DonmaAyarlari.tpsEsigi, DonmaAyarlari.tpsKritikEsigi);
            if (bolen != sonBolen) {
                DogalAfetler.LOGGER.info("[donma] TPS {} ({} ms/tick): bütçe 1/{} → {} blok/tick",
                        String.format("%.1f", sonTps), String.format("%.1f", sunucu.getAverageTickTime()), bolen,
                        Math.max(1, DonmaAyarlari.temelButce / bolen));
            }
            sonBolen = bolen;
            siraKirli = true;
        }
        sonButce = Math.max(1, DonmaAyarlari.temelButce / sonBolen);
        if (!BEKLEYEN.isEmpty() && Kurallar.blokHasariAcik(level.getGameRules())) {
            isle(level, tick);
        }
        if (tick % 100 == 0) {
            ozet(level, sunucu);
        }
    }

    private static void isle(ServerLevel level, long tick) {
        if (siraKirli) {
            siraYenile(level);
        }
        long basla = System.nanoTime();
        long sinir = basla + DonmaAyarlari.tickMaksMs * 1_000_000L;
        int kalan = sonButce * DonmaAyarlari.BLOK_MALIYETI;
        DonmaVerisi veri = DonmaVerisi.al(level.getServer());
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        DonmaEsikleri e = DonmaAyarlari.esikler;
        BlockPos.MutableBlockPos konum = new BlockPos.MutableBlockPos();
        int bakilan = 0;
        while (kalan > 0 && siraBasi < sira.length) {
            if (System.nanoTime() >= sinir) {
                ozetSureSiniri++;
                break;
            }
            long anahtar = sira[siraBasi];
            if (!BEKLEYEN.contains(anahtar)) {
                siraBasi++;
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(anahtar), ChunkPos.getZ(anahtar));
            if (chunk == null) {
                BEKLEYEN.remove(anahtar);
                ISLER.remove(anahtar);
                siraBasi++;
                continue;
            }
            FullChunkStatus durum = chunk.getFullStatus();
            if (durum == null || !durum.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
                siraBasi++;
                continue;
            }
            ChunkIsi is = ISLER.get(anahtar);
            if (is == null) {
                int mevcut = ChunkDonmaSeviyesi.oku(chunk);
                int hedef = hedef(level, chunk, veri);
                if (mevcut == hedef) {
                    BEKLEYEN.remove(anahtar);
                    siraBasi++;
                    continue;
                }
                is = new ChunkIsi(mevcut, DonmaHedefi.siradakiSeviye(mevcut, hedef, DonmaAyarlari.seviyeAdimi), hedef,
                        veri.cozulme(), tick);
                ISLER.put(anahtar, is);
            }
            if (is.kiyi == null) {
                ChunkPos p = chunk.getPos();
                is.kiyi = DonmaIsleyici.kiyiUzakliklari(level, p.getMinBlockX(), p.getMinBlockZ(), 16, 16,
                        DonmaIsleyici.SINIRSIZ, tablo, e);
                kalan -= BFS_MALIYETI;
            }
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            while (is.sutun < 256 && kalan > 0) {
                int i = SUTUN_SIRASI[is.sutun++];
                int degisen = DonmaIsleyici.sutunuIsle(level, chunk, minX + (i & 15), minZ + (i >> 4),
                        DonmaIsleyici.SINIRSIZ, is.bitis, is.cozulme, is.kiyi[i], tablo, e, konum);
                is.degisen += degisen;
                kalan -= 1 + degisen * DonmaAyarlari.BLOK_MALIYETI;
                bakilan++;
            }
            if (is.sutun >= 256) {
                ChunkDonmaSeviyesi.yaz(chunk, is.bitis);
                ISLER.remove(anahtar);
                tamamlananAdim++;
                ozetAdim++;
                toplamDegisen += is.degisen;
                ozetDegisen += is.degisen;
                if (is.bitis == is.hedef) {
                    hedefeUlasan++;
                    ozetUlasan++;
                }
                if (IZLENEN.contains(anahtar)) {
                    DogalAfetler.LOGGER.info("[donma] izlenen chunk {} seviye {} → {} (hedef {}): {} blok değişti, {} tick sürdü",
                            chunk.getPos(), is.baslangic, is.bitis, is.hedef, is.degisen, tick - is.baslangicTick);
                }
            }
        }
        long gecen = System.nanoTime() - basla;
        toplamSutun += bakilan;
        ozetSutun += bakilan;
        ozetNs += gecen;
        ozetEnUzunNs = Math.max(ozetEnUzunNs, gecen);
    }

    private static void siraYenile(ServerLevel level) {
        List<ServerPlayer> oyuncular = level.players();
        int[] px = new int[oyuncular.size()];
        int[] pz = new int[oyuncular.size()];
        for (int i = 0; i < px.length; i++) {
            ChunkPos p = oyuncular.get(i).chunkPosition();
            px[i] = p.x;
            pz[i] = p.z;
        }
        sira = ChunkOnceligi.sirala(BEKLEYEN.toLongArray(), px, pz);
        siraBasi = 0;
        siraKirli = false;
    }

    private static void ozet(ServerLevel level, MinecraftServer sunucu) {
        if (ozetAdim == 0 && ozetSutun == 0) {
            return;
        }
        DonmaVerisi veri = DonmaVerisi.al(sunucu);
        DogalAfetler.LOGGER.info("[donma] özet: G={} çözülme={} yüklü={} bekleyen={} süren={} | son 100 tick: {} adım, {} chunk"
                        + " hedefte, {} blok, {} sütun, işçi ort {} ms/tick en çok {} ms, süre sınırı {} kez | bütçe {} blok/tick"
                        + " (1/{}) TPS {} ({} ms/tick) | toplam {} adım, {} blok",
                veri.ilerleme(), veri.cozulme(), YUKLU.size(), BEKLEYEN.size(), ISLER.size(), ozetAdim, ozetUlasan,
                ozetDegisen, ozetSutun, String.format("%.2f", ozetNs / 100.0 / 1e6), String.format("%.2f", ozetEnUzunNs / 1e6),
                ozetSureSiniri, sonButce, sonBolen, String.format("%.1f", sonTps),
                String.format("%.1f", sunucu.getAverageTickTime()), tamamlananAdim, toplamDegisen);
        ozetAdim = ozetUlasan = ozetDegisen = ozetSutun = ozetNs = ozetEnUzunNs = 0;
        ozetSureSiniri = 0;
    }

    private static int[] sutunSirasi() {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }
        Random r = new Random(0x5D0E1L);
        for (int i = 255; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
        }
        return s;
    }
}
