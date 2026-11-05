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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/**
 * Chunk'ları güncel global ilerlemeye (G) ve döneme yetiştiren kuyruk (madde 9 YENİ YÜKLENEN CHUNK'LAR, ÖNCELİK; madde 3 TPS).
 *
 * <ul>
 * <li>{@code ChunkEvent.Load} (ana thread) chunk'ı yüklü kümeye ve bekleyenlere ekler; {@code Unload} çıkarır.</li>
 * <li>Chunk capability'si son işlenen G'yi ve dönemi tutar. Bir iş G'yi en çok {@code seviyeAdimi} kadar ilerletir ve
 * dönemi günceller: 256 sütun eski (G, dönem) hedefinden yeni hedefe götürülür (sütun hedefi kış ağırlığına bağlı),
 * bitince capability yazılır. Bekleyenler en yakın oyuncuya uzaklığa göre sıralı (20 tick'te bir yeniden).</li>
 * <li>Bütçe birimli (sütun 1, blok değişimi 8, kıyı BFS 64); TPS 18'in altında yarıya, 15'in altında çeyreğe iner; ayrıca
 * bir tick'te en çok {@code tickMaksMs} (16 sütunda bir bakılır).</li>
 * <li>Yalnız {@code ServerChunkCache.isPositionTicking} chunk'lar işlenir: ticking future'ı bitmiş chunk'ın 1 chunk
 * çevresi FULL'dür; aksi halde kenar sütundaki {@code setBlock}'un komşu şekil güncellemesi komşu chunk'ı ana thread'de
 * senkron yükletir ve değişim istemciye de gitmez.</li>
 * <li>{@code afetBlokHasari} kapalıyken kuyruk durur; açılınca kaldığı yerden sürer.</li>
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

    static volatile int yapayYukMs;

    static long tamamlananAdim;
    static long hedefeUlasan;
    static long toplamDegisen;
    static long toplamSutun;
    static int sonButce = DonmaAyarlari.temelButce;
    static int sonBolen = 1;
    static double sonTps = 20.0;
    static double sonMs;
    private static long ozetAdim;
    private static long ozetUlasan;
    private static long ozetDegisen;
    private static long ozetSutun;
    private static long ozetNs;
    private static long ozetEnUzunNs;
    private static int ozetSureSiniri;
    private static int ozetErken;

    private DonmaZamanlayici() {
    }

    private static final class ChunkIsi {
        final int baslangicG;
        final boolean oncekiCozulme;
        final int bitisG;
        final boolean cozulme;
        final int hedefG;
        final long baslangicTick;
        KisKaynagi kis;
        int[] kiyi;
        int sutun;
        int degisen;

        ChunkIsi(int baslangicG, boolean oncekiCozulme, int bitisG, boolean cozulme, int hedefG, long baslangicTick) {
            this.baslangicG = baslangicG;
            this.oncekiCozulme = oncekiCozulme;
            this.bitisG = bitisG;
            this.cozulme = cozulme;
            this.hedefG = hedefG;
            this.baslangicTick = baslangicTick;
        }
    }

    static String donemAdi(boolean cozulme) {
        return cozulme ? "çözülme" : "donma";
    }

    /** {@code loadLevel}'dan önce gelir; {@code ServerStartingEvent} spawn chunk'ları yüklendikten sonra gelir ve kümeyi silerdi. */
    static void sunucuBasliyor(ServerAboutToStartEvent olay) {
        YUKLU.clear();
        BEKLEYEN.clear();
        ISLER.clear();
        IZLENEN.clear();
        sira = new long[0];
        siraKirli = true;
        yapayYukMs = 0;
        tamamlananAdim = hedefeUlasan = toplamDegisen = toplamSutun = 0;
        DunyaKisi.sifirla();
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
            ChunkDonmaSeviyesi d = ChunkDonmaSeviyesi.al(chunk);
            DogalAfetler.LOGGER.info("[donma] izlenen chunk {} yüklendi (yeni={}): kayıtlı seviye {}, dönem {}", chunk.getPos(),
                    olay.isNewChunk(), d == null ? 0 : d.sonG(), donemAdi(d != null && d.cozulme()));
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
                    yarim == null ? "" : " (yarım geçiş " + yarim.baslangicG + "→" + yarim.bitisG + " bırakıldı)");
        }
    }

    /**
     * Global ilerleme ya da dönem değişti: bütün yüklü chunk'lar yeniden kuyruğa girer. Süren adımlar bitirilir; yarım
     * bırakılan adımda capability eski kalır ve dokunulmuş sütunlar ileride kalırdı.
     */
    static void hepsiniKuyrugaAl() {
        BEKLEYEN.addAll(YUKLU);
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
            sonMs = TpsKismasi.ortalamaMs(sunucu.tickTimes);
            sonTps = TpsKismasi.tps(sunucu.tickTimes);
            int bolen = TpsKismasi.bolen(sonTps, DonmaAyarlari.tpsEsigi, DonmaAyarlari.tpsKritikEsigi);
            if (bolen != sonBolen) {
                DogalAfetler.LOGGER.info("[donma] TPS {} ({} ms/tick): bütçe 1/{} → {} blok/tick",
                        String.format("%.1f", sonTps), String.format("%.1f", sonMs), bolen,
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
            ozet(sunucu);
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
            int cx = ChunkPos.getX(anahtar);
            int cz = ChunkPos.getZ(anahtar);
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            ChunkDonmaSeviyesi durum = chunk == null ? null : ChunkDonmaSeviyesi.al(chunk);
            if (durum == null) {
                BEKLEYEN.remove(anahtar);
                ISLER.remove(anahtar);
                siraBasi++;
                continue;
            }
            if (!level.getChunkSource().isPositionTicking(anahtar)) {
                FullChunkStatus fs = chunk.getFullStatus();
                if (fs != null && fs.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
                    ozetErken++;
                }
                siraBasi++;
                continue;
            }
            ChunkIsi is = ISLER.get(anahtar);
            if (is == null) {
                if (durum.sonG() == veri.ilerleme() && durum.cozulme() == veri.cozulme()) {
                    BEKLEYEN.remove(anahtar);
                    siraBasi++;
                    continue;
                }
                is = new ChunkIsi(durum.sonG(), durum.cozulme(),
                        DonmaHedefi.siradakiSeviye(durum.sonG(), veri.ilerleme(), DonmaAyarlari.seviyeAdimi), veri.cozulme(),
                        veri.ilerleme(), tick);
                ISLER.put(anahtar, is);
            }
            if (is.kiyi == null) {
                ChunkPos p = chunk.getPos();
                is.kiyi = DonmaIsleyici.kiyiUzakliklari(level, p.getMinBlockX(), p.getMinBlockZ(), 16, 16,
                        DonmaIsleyici.SINIRSIZ, tablo, e);
                is.kis = is.oncekiCozulme || is.cozulme ? DunyaKisi.alan(level).chunk(cx, cz) : KisKaynagi.YOK;
                kalan -= BFS_MALIYETI;
            }
            int minX = cx * 16;
            int minZ = cz * 16;
            while (is.sutun < 256 && kalan > 0 && ((is.sutun & 15) != 0 || System.nanoTime() < sinir)) {
                int i = SUTUN_SIRASI[is.sutun++];
                int degisen = DonmaIsleyici.sutunuIsle(level, chunk, durum, minX + (i & 15), minZ + (i >> 4),
                        DonmaIsleyici.SINIRSIZ, is.baslangicG, is.oncekiCozulme, is.bitisG, is.cozulme, is.kis, is.kiyi[i],
                        tablo, e, konum);
                is.degisen += degisen;
                kalan -= 1 + degisen * DonmaAyarlari.BLOK_MALIYETI;
                bakilan++;
            }
            if (is.sutun >= 256) {
                durum.durum(is.bitisG, is.cozulme);
                chunk.setUnsaved(true);
                ISLER.remove(anahtar);
                tamamlananAdim++;
                ozetAdim++;
                toplamDegisen += is.degisen;
                ozetDegisen += is.degisen;
                if (is.bitisG == is.hedefG) {
                    hedefeUlasan++;
                    ozetUlasan++;
                }
                if (IZLENEN.contains(anahtar)) {
                    DogalAfetler.LOGGER.info("[donma] izlenen chunk {} seviye {} → {} (hedef {}){}: {} blok değişti, {} tick sürdü",
                            chunk.getPos(), is.baslangicG, is.bitisG, is.hedefG,
                            is.oncekiCozulme == is.cozulme ? ""
                                    : ", dönem " + donemAdi(is.oncekiCozulme) + " → " + donemAdi(is.cozulme),
                            is.degisen, tick - is.baslangicTick);
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

    private static void ozet(MinecraftServer sunucu) {
        if (ozetAdim == 0 && ozetSutun == 0 && ozetErken == 0) {
            return;
        }
        DonmaVerisi veri = DonmaVerisi.al(sunucu);
        DogalAfetler.LOGGER.info("[donma] özet: G={} çözülme={} yüklü={} bekleyen={} süren={} | son 100 tick: {} adım, {} chunk"
                        + " hedefte, {} blok, {} sütun, işçi ort {} ms/tick en çok {} ms, süre sınırı {} kez, ticket'ı BLOCK_TICKING"
                        + " ama ticking olmayan için atlama {} | bütçe {} blok/tick (1/{}) TPS {} ({} ms/tick) | toplam {} adım, {} blok",
                veri.ilerleme(), veri.cozulme(), YUKLU.size(), BEKLEYEN.size(), ISLER.size(), ozetAdim, ozetUlasan,
                ozetDegisen, ozetSutun, String.format("%.2f", ozetNs / 100.0 / 1e6), String.format("%.2f", ozetEnUzunNs / 1e6),
                ozetSureSiniri, ozetErken, sonButce, sonBolen, String.format("%.1f", sonTps), String.format("%.1f", sonMs),
                tamamlananAdim, toplamDegisen);
        ozetAdim = ozetUlasan = ozetDegisen = ozetSutun = ozetNs = ozetEnUzunNs = 0;
        ozetSureSiniri = 0;
        ozetErken = 0;
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
