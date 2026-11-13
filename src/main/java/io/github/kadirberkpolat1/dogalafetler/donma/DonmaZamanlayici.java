package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaTablosuYukleyici.Tablo;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.BuharlasmaHedefi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.ChunkOnceligi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaEsikleri;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaHedefi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.EsikKumesi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisKaynagi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.YetismeButcesi;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

/**
 * Chunk'ları güncel global ilerlemeye (G), döneme ve buharlaşma derinliğine yetiştiren kuyruk (madde 9 YENİ YÜKLENEN
 * CHUNK'LAR, ÖNCELİK; madde 3 TPS; N9; K-D2).
 *
 * <ul>
 * <li>{@code ChunkEvent.Load} (ana thread) chunk'ı yüklü kümeye ve bekleyenlere ekler; {@code Unload} çıkarır. G, dönem,
 * etkin buharlaşma derinliği ya da sıfırlama döngüsü değişince bütün yüklü chunk'lar yeniden kuyruğa girer.</li>
 * <li>Bir iş chunk'ın G'sini en çok {@code seviyeAdimi} ilerletir, dönemi ve buharlaşmayı günceller; 256 sütun sabit
 * karışık sırayla. Süren adımın bitişi capability'ye yazılır (yarım adım); chunk hedefe ulaşana kadar sıradakine
 * geçilmez. Bekleyenler en yakın oyuncuya uzaklığa göre sıralı (20 tick'te bir yeniden).</li>
 * <li>Bütçe {@link YetismeButcesi}: tick başına en çok {@code tickBasinaMaksBlok · TpsOlcer.kismaCarpani()} blok, bir
 * sütun ancak en kötü durumunu karşılayan birim kaldıysa işlenir; ayrıca süre tavanı (aşım sonraki tick'ten düşülür).</li>
 * <li>Yalnız bilet seviyesi en az {@code BLOCK_TICKING} olan ve 8 komşusu yüklü chunk'lar işlenir ({@link #islenebilir}):
 * kenar sütunundaki şekil güncellemesi komşuyu senkron yüklemez; simülasyon mesafesi dışında ama görüş mesafesindeki
 * chunk'lar da donar (D5). {@code afetBlokHasari} kapalıyken kuyruk durur; açılınca kaldığı yerden sürer.</li>
 * <li>Buharlaşma sürerken ({@code etkin derinlik > 0}) oyuncunun {@value #TARAMA_YARICAP} chunk çevresindeki yüklü chunk'lar
 * {@value #TARAMA_ARALIGI} tick'te bir yeniden denetlenir: derinlik değişmese de sonradan gelen su (dalga, akış, oyuncu,
 * geri dolan çekilme) kurur; efekt çıkan su gerçekten azalır. Aynı tick bütçesi ve süre tavanı geçerlidir.</li>
 * <li>{@code /dogalafet sifirla} sonrası (yeni döngü) chunk bitlerini korur ve bizim kar/buzumuzu 0'a çözer (D4).</li>
 * </ul>
 */
public final class DonmaZamanlayici {
    /** Bir blok değişiminin bütçe birimi (sütun taraması 1). */
    public static final int BLOK_MALIYETI = 8;
    private static final int[] SUTUN_SIRASI = sutunSirasi();

    private static final LongOpenHashSet YUKLU = new LongOpenHashSet();
    private static final LongLinkedOpenHashSet BEKLEYEN = new LongLinkedOpenHashSet();
    private static final Long2ObjectOpenHashMap<ChunkIsi> ISLER = new Long2ObjectOpenHashMap<>();
    private static long[] sira = new long[0];
    private static int siraBasi;
    private static boolean siraKirli = true;
    @Nullable
    private static LongSet kapsam;
    private static EsikKumesi esikKumesi;
    private static DonmaEsikleri esikKumesiEsikleri;
    private static Tablo esikKumesiTablosu;

    private static int sonG = -1;
    private static boolean sonCozulme;
    private static int sonBuhar = -1;
    private static long sonDongu = Long.MIN_VALUE;
    private static long oncekiAsimNs;
    private static volatile boolean kuyrukBos = true;
    /** Buharlaşma yeniden denetim aralığı (tick) ve oyuncudan en uzak denetlenen chunk (Chebyshev). */
    static final int TARAMA_ARALIGI = 200;
    static final int TARAMA_YARICAP = 12;
    /** Derinlik değişmese de buharlaşma için yeniden işlenecek chunk'lar ({@link #BEKLEYEN}'in alt kümesi). */
    private static final LongOpenHashSet TARA = new LongOpenHashSet();
    /** Dönem verisinde ({@code donma}) buharlaşma başlarkenki sanal deniz yükselmesi; döngü bitince silinir. */
    private static final String SANAL_BASI = "buharSanalBasi";
    /** Derinlik 0 iken her tick izlenen sanal deniz yükselmesi; {@code NaN}: izlenmedi (sunucu yeni açıldı). */
    private static float sonSanal = Float.NaN;

    private static long tamamlananAdim;
    private static long hedefeUlasan;
    private static long toplamDegisen;
    private static int sonTickDegisen;
    private static int enCokTickDegisen;
    private static int sonBlokSiniri = 256;
    private static double sonKisma = 1.0;
    private static long ozetAdim;
    private static long ozetDegisen;
    private static long ozetSutun;
    private static long ozetNs;
    private static long ozetEnUzunNs;
    private static int ozetSureSiniri;
    /**
     * Isınma: istatistik sıfırlandıktan sonraki ilk {@value #ISINMA_TICK} iş yapan (sütun işleyen) işçi tick'i (soğuk JIT,
     * sınıf yükleme, ilk BFS'ler). Boş tick'ler (bekleyen chunk'lar hedefte ya da işlenemez) pencereyi doldurmaz ve en uzun
     * tick'e girmez (inceleme-3 D3-1).
     */
    private static final int ISINMA_TICK = 100;
    private static int isciTick;
    private static long isinmaEnUzunNs;
    private static long enUzunNs;

    private DonmaZamanlayici() {
    }

    private static final class ChunkIsi {
        final DonmaIsleyici.Gecis gecis;
        final int hedefG;
        final long baslangicTick;
        final KisKaynagi kis;
        /** Adım bitince chunk'a yazılacak yön (0: sütunlar karışık ya da bilinmiyor). */
        final int sonrakiYon;
        final DonmaIsleyici.Sayac sayac = new DonmaIsleyici.Sayac();
        int[] kiyi;
        boolean bfsYeni;
        int sutun;

        ChunkIsi(DonmaIsleyici.Gecis gecis, int hedefG, long baslangicTick, KisKaynagi kis, int sonrakiYon) {
            this.gecis = gecis;
            this.hedefG = hedefG;
            this.baslangicTick = baslangicTick;
            this.kis = kis;
            this.sonrakiYon = sonrakiYon;
        }
    }

    static String donemAdi(boolean cozulme) {
        return cozulme ? "çözülme" : "donma";
    }

    /** {@code loadLevel}'dan önce: spawn chunk'larının yükleme olayları bu temizlikten sonra gelir. */
    static void sunucuBasliyor(ServerAboutToStartEvent olay) {
        YUKLU.clear();
        BEKLEYEN.clear();
        ISLER.clear();
        TARA.clear();
        sira = new long[0];
        siraKirli = true;
        kapsam = null;
        sonG = -1;
        sonBuhar = -1;
        sonDongu = Long.MIN_VALUE;
        sonSanal = Float.NaN;
        oncekiAsimNs = 0;
        esikKumesi = null;
        istatistikSifirla();
        KisBolgesi.sifirla();
    }

    /** Eşik kümesi açılışta kurulur (config ve veri paketi yüklü); ilk işçi tick'inin süresine girmez. */
    static void sunucuBasladi(ServerStartedEvent olay) {
        esikKumesi(DonmaAyarlari.esikler(), DonmaTablosuYukleyici.etkin());
    }

    private static boolean ustDunya(Object level) {
        return level instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD;
    }

    static void chunkYuklendi(ChunkEvent.Load olay) {
        if (ustDunya(olay.getLevel()) && olay.getChunk() instanceof LevelChunk chunk) {
            long anahtar = chunk.getPos().toLong();
            YUKLU.add(anahtar);
            BEKLEYEN.add(anahtar);
            siraKirli = true;
        }
    }

    static void chunkBosaltildi(ChunkEvent.Unload olay) {
        if (ustDunya(olay.getLevel()) && olay.getChunk() instanceof LevelChunk chunk) {
            long anahtar = chunk.getPos().toLong();
            YUKLU.remove(anahtar);
            BEKLEYEN.remove(anahtar);
            ISLER.remove(anahtar);
            TARA.remove(anahtar);
        }
    }

    /** Veri paketi tablosu değişti: eşik kümesi yeniden kurulur, yüklü chunk'lar denetlenir. */
    static void tabloDegisti() {
        esikKumesi = null;
        BEKLEYEN.addAll(YUKLU);
        siraKirli = true;
    }

    static void hepsiniKuyrugaAl() {
        BEKLEYEN.addAll(YUKLU);
        siraKirli = true;
    }

    /** Yalnız GameTest: işlenecek chunk'ları sınırlar ({@code null} tümü); diğer testlerin ve spawn'ın chunk'larına dokunulmaz. */
    public static void kapsamAyarla(@Nullable LongSet chunklar) {
        kapsam = chunklar == null ? null : new LongOpenHashSet(chunklar);
        siraKirli = true;
    }

    /**
     * Yalnız GameTest sahne kurulumu: chunk verisini {@link ChunkDonmaVerisi#donguyeGec} ile sıfırlar ve chunk'ın yarım işini
     * atar (K-E14). İş eski verinin adımına göre kurulmuştur (ör. önceki testin genel G'sinde, kapsam dışında kalıp yarım
     * duran spawn chunk'ı); sürdürülürse bitişinde {@code sonG}'yi yeni sahnenin blokları işlenmeden eski hedefe yazar.
     */
    public static void chunkVerisiniSifirla(LevelChunk chunk, long dongu, int g, boolean cozulmeDonemi) {
        ChunkDonmaVerisi.al(chunk).donguyeGec(dongu, g, cozulmeDonemi);
        chunk.setUnsaved(true);
        long anahtar = chunk.getPos().toLong();
        ChunkIsi eski = ISLER.remove(anahtar);
        if (eski != null) {
            DogalAfetler.LOGGER.info("[donma] test sahnesi: chunk {},{} yarım işi atıldı (G {} -> {}, {}/256 sütun)",
                    chunk.getPos().x, chunk.getPos().z, eski.gecis.eskiG(), eski.gecis.yeniG(), eski.sutun);
        }
        if (YUKLU.contains(anahtar)) {
            BEKLEYEN.add(anahtar);
        }
        siraKirli = true;
    }

    public static void istatistikSifirla() {
        tamamlananAdim = hedefeUlasan = toplamDegisen = 0;
        sonTickDegisen = enCokTickDegisen = 0;
        ozetAdim = ozetDegisen = ozetSutun = ozetNs = ozetEnUzunNs = 0;
        ozetSureSiniri = 0;
        isciTick = 0;
        isinmaEnUzunNs = enUzunNs = 0;
    }

    static void tick(TickEvent.LevelTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !ustDunya(olay.level)) {
            return;
        }
        ServerLevel level = (ServerLevel) olay.level;
        MinecraftServer server = level.getServer();
        DunyaDonemi donem = DunyaDonemi.al(server);
        int g = hedefG(donem);
        boolean cozulme = donem.cozulmeDonemi();
        int buhar = etkinBuhar(server, donem);
        long dongu = dongu(donem);
        if (g != sonG || cozulme != sonCozulme || buhar != sonBuhar || dongu != sonDongu) {
            sonG = g;
            sonCozulme = cozulme;
            sonBuhar = buhar;
            sonDongu = dongu;
            hepsiniKuyrugaAl();
        }
        int tick = server.getTickCount();
        if (buhar > 0 && tick % TARAMA_ARALIGI == 0) {
            taramaIste(level);
        }
        if (tick % 20 == 0) {
            siraKirli = true;
        }
        sonTickDegisen = 0;
        boolean calisti = false;
        if (!BEKLEYEN.isEmpty() && Kurallar.blokHasariAcik(level.getGameRules())) {
            calisti = isle(level, tick, g, cozulme, buhar, dongu);
        }
        kuyrukBos = !calisti;
        if (tick % 100 == 0) {
            ozet(donem);
        }
    }

    /**
     * Chunk işlenebilir mi: bilet seviyesi blok tick'i (oyuncunun görüş mesafesi ya da spawn) ve 8 komşusu tam yüklü.
     * {@code isPositionTicking} simülasyon mesafesini de istediği için görünen dış halkayı hiç işlemiyordu; ChunkHolder'ın
     * ticking future'ı erişilemez (protected). Future tamamlanmadan yapılan değişiklik istemciye giden ilk chunk paketinde
     * yer alır.
     */
    static boolean islenebilir(ServerLevel level, LevelChunk chunk, int cx, int cz) {
        FullChunkStatus durum = chunk.getFullStatus();
        if (durum == null || !durum.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
            return false;
        }
        ServerChunkCache kaynak = level.getChunkSource();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dz != 0) && kaynak.getChunkNow(cx + dx, cz + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    static int hedefG(DunyaDonemi donem) {
        return Math.max(0, Math.min(100, (int) Math.floor(donem.donmaIlerlemesi() + 1e-4)));
    }

    static int etkinBuhar(MinecraftServer server, DunyaDonemi donem) {
        return BuharlasmaHedefi.etkinDerinlik(donem.buharlasmaDerinligi(), buharPayi(server, donem));
    }

    /**
     * Gerçek suyun buharlaşmadan önce sanal deniz yükselmesine bırakacağı pay (N10). Pay yalnız buharlaşma başlarken sanal
     * yükselme varsa geçerlidir: {@code DenizYukselmesiSureci}'nin taban değeri bir kez yazılıp kalır; sanal yükselme çoktan
     * tükenmişken (ikinci güneş patlaması, pulsar) eski taban gerçek suyu hiç ya da geç kurutuyordu. Başlangıçtaki yükselme
     * derinlik 0 iken her tick izlenir ve döngü başında dönem verisine yazılır (eski kayıtta izleme yoksa kanca değeri).
     */
    private static double buharPayi(MinecraftServer server, DunyaDonemi donem) {
        CompoundTag ek = donem.ekVeri(KisBolgesi.ALT_SISTEM);
        if (donem.buharlasmaDerinligi() <= 0f) {
            sonSanal = donem.deniz().mevcutArtis();
            if (ek.contains(SANAL_BASI)) {
                ek.remove(SANAL_BASI);
                donem.ekVeriDegisti(KisBolgesi.ALT_SISTEM);
            }
            return 0.0;
        }
        if (!ek.contains(SANAL_BASI)) {
            ek.putFloat(SANAL_BASI, Float.isNaN(sonSanal) ? (float) DisKancalar.al().buharlasmaPayi(server) : sonSanal);
            donem.ekVeriDegisti(KisBolgesi.ALT_SISTEM);
        }
        return ek.getFloat(SANAL_BASI) > 0f ? DisKancalar.al().buharlasmaPayi(server) : 0.0;
    }

    /** Buharlaşma sürerken oyuncuya yakın yüklü chunk'ları yeniden denetime alır (kuyruk bütçesiyle işlenir). */
    private static void taramaIste(ServerLevel level) {
        List<ServerPlayer> oyuncular = level.players();
        if (oyuncular.isEmpty()) {
            return;
        }
        int[] px = new int[oyuncular.size()];
        int[] pz = new int[oyuncular.size()];
        for (int i = 0; i < px.length; i++) {
            ChunkPos p = oyuncular.get(i).chunkPosition();
            px[i] = p.x;
            pz[i] = p.z;
        }
        LongIterator it = YUKLU.iterator();
        while (it.hasNext()) {
            long anahtar = it.nextLong();
            int cx = ChunkPos.getX(anahtar);
            int cz = ChunkPos.getZ(anahtar);
            for (int i = 0; i < px.length; i++) {
                if (Math.abs(cx - px[i]) <= TARAMA_YARICAP && Math.abs(cz - pz[i]) <= TARAMA_YARICAP) {
                    TARA.add(anahtar);
                    BEKLEYEN.add(anahtar);
                    siraKirli = true;
                    break;
                }
            }
        }
    }

    static long dongu(DunyaDonemi donem) {
        return donem.ekVeri(KisBolgesi.ALT_SISTEM).getLong("dongu");
    }

    private static EsikKumesi esikKumesi(DonmaEsikleri e, Tablo tablo) {
        if (esikKumesi == null || esikKumesiEsikleri != e || esikKumesiTablosu != tablo) {
            esikKumesi = EsikKumesi.kur(e, tablo.esikDonma(), tablo.esikCozulme());
            esikKumesiEsikleri = e;
            esikKumesiTablosu = tablo;
        }
        return esikKumesi;
    }

    /**
     * Bir tick'lik iş; işlenebilir (ticking) chunk'ta sütun işlendiyse {@code true}. Süre tavanı (D2-2) sıra yenilemeyi,
     * chunk işi kurulumunu (kış alanı örnekleri) ve kıyı BFS'ini de kapsar: süre her yeni iş kurulmadan ve her sütundan
     * (BFS ancak bir sütunun içinde başlar) önce denetlenir; tek tick tavanı en çok bir kurulum ya da bir sütun + BFS kadar
     * aşar. BFS'in birimi sütundan önce ayrılır ({@link YetismeButcesi#sutunRezervi}).
     */
    private static boolean isle(ServerLevel level, long tick, int g, boolean cozulme, int buhar, long dongu) {
        long basla = System.nanoTime();
        if (siraKirli) {
            siraYenile(level);
        }
        YetismeButcesi butce = DonmaAyarlari.butce();
        double kisma = TpsOlcer.kismaCarpani();
        sonKisma = kisma;
        sonBlokSiniri = butce.blokSiniri(kisma);
        int birim = butce.birim(kisma);
        int kalan = birim;
        DonmaEsikleri e = DonmaAyarlari.esikler();
        int temelDegisim = Math.max(e.maksBuzKalinligi() + 1, 2);
        int enAzRezerv = Math.min(birim, butce.rezerv(temelDegisim));
        long sinir = basla + butce.sureSiniriNs(oncekiAsimNs);
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        EsikKumesi esikler = esikKumesi(e, tablo);
        int adim = DonmaAyarlari.seviyeAdimi();
        BlockPos.MutableBlockPos konum = new BlockPos.MutableBlockPos();
        int bakilan = 0;
        boolean sureDoldu = false;
        while (!sureDoldu && butce.sutunSigar(kalan, enAzRezerv) && siraBasi < sira.length) {
            long anahtar = sira[siraBasi];
            if (!BEKLEYEN.contains(anahtar) || kapsam != null && !kapsam.contains(anahtar)) {
                siraBasi++;
                continue;
            }
            int cx = ChunkPos.getX(anahtar);
            int cz = ChunkPos.getZ(anahtar);
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            ChunkDonmaVerisi durum = chunk == null ? null : ChunkDonmaVerisi.al(chunk);
            if (durum == null) {
                BEKLEYEN.remove(anahtar);
                ISLER.remove(anahtar);
                siraBasi++;
                continue;
            }
            if (!islenebilir(level, chunk, cx, cz)) {
                siraBasi++;
                continue;
            }
            ChunkIsi is = ISLER.get(anahtar);
            if (is == null) {
                if (durum.dongu() != dongu) {
                    durum.donguyeAktar(dongu);
                    chunk.setUnsaved(true);
                }
                if (durum.sonG() == g && durum.cozulme() == cozulme && durum.sonBuhar() == buhar && !durum.yarimVar()
                        && !TARA.contains(anahtar)) {
                    BEKLEYEN.remove(anahtar);
                    siraBasi++;
                    continue;
                }
                if (!butce.sureVar(System.nanoTime(), sinir)) {
                    ozetSureSiniri++;
                    break;
                }
                boolean yarim = durum.yarimVar();
                int eskiG = yarim ? durum.yarimG() : durum.sonG();
                boolean eskiCozulme = yarim ? durum.yarimCozulme() : durum.cozulme();
                int bitisG = DonmaHedefi.siradakiSeviye(eskiG, g, adim);
                boolean buharFarki = durum.sonBuhar() != buhar;
                boolean tara = TARA.remove(anahtar);
                boolean buharIsle = buharFarki || tara;
                boolean atlama = !buharIsle
                        && DonmaHedefi.atlamaSerbest(durum.sonYon(), eskiG, eskiCozulme, bitisG, cozulme, yarim);
                int sonrakiYon = yarim || buharFarki ? 0
                        : eskiG == bitisG && eskiCozulme == cozulme ? durum.sonYon()
                                : DonmaHedefi.adimYonu(eskiG, eskiCozulme, bitisG, cozulme);
                DonmaIsleyici.Gecis gecis = new DonmaIsleyici.Gecis(eskiG, eskiCozulme, bitisG, cozulme, buhar, buharIsle,
                        atlama);
                KisKaynagi kis = eskiCozulme || cozulme ? KisBolgesi.alan(level).chunk(cx, cz) : KisKaynagi.YOK;
                is = new ChunkIsi(gecis, g, tick, kis, sonrakiYon);
                ISLER.put(anahtar, is);
                durum.yarimAdim(bitisG, cozulme);
            }
            ChunkIsi suren = is;
            ChunkPos p = chunk.getPos();
            DonmaIsleyici.Kiyi kiyi = (x, z) -> {
                if (suren.kiyi == null) {
                    suren.kiyi = DonmaIsleyici.kiyiUzakliklari(level, p.getMinBlockX(), p.getMinBlockZ(), 16, 16,
                            DonmaIsleyici.SINIRSIZ, tablo, e);
                    suren.bfsYeni = true;
                }
                return suren.kiyi[ChunkDonmaVerisi.indeks(x, z)];
            };
            int rezervTemel = temelDegisim + Math.max(is.gecis.buhar(), durum.sonBuhar());
            while (is.sutun < 256 && butce.sutunSigar(kalan, Math.min(birim, butce.sutunRezervi(rezervTemel, is.kiyi == null)))) {
                if (!butce.sureVar(System.nanoTime(), sinir)) {
                    ozetSureSiniri++;
                    sureDoldu = true;
                    break;
                }
                int i = SUTUN_SIRASI[is.sutun++];
                int once = is.sayac.degisen;
                DonmaIsleyici.sutunuIsle(level, chunk, durum, p.getMinBlockX() + (i & 15), p.getMinBlockZ() + (i >> 4),
                        DonmaIsleyici.SINIRSIZ, is.gecis, is.kis, kiyi, tablo, e, esikler, konum, is.sayac);
                int degisen = is.sayac.degisen - once;
                kalan -= butce.sutunMaliyeti(degisen);
                sonTickDegisen += degisen;
                if (is.bfsYeni) {
                    kalan -= YetismeButcesi.BFS_MALIYETI;
                    is.bfsYeni = false;
                }
                bakilan++;
            }
            if (is.sutun < 256) {
                break;
            }
            bitir(level, chunk, durum, anahtar, is);
        }
        long gecen = System.nanoTime() - basla;
        oncekiAsimNs = Math.max(0L, gecen - butce.tavanNs());
        enCokTickDegisen = Math.max(enCokTickDegisen, sonTickDegisen);
        ozetSutun += bakilan;
        ozetNs += gecen;
        ozetEnUzunNs = Math.max(ozetEnUzunNs, gecen);
        if (bakilan > 0) {
            if (++isciTick <= ISINMA_TICK) {
                isinmaEnUzunNs = Math.max(isinmaEnUzunNs, gecen);
            } else {
                enUzunNs = Math.max(enUzunNs, gecen);
            }
        }
        return bakilan > 0;
    }

    private static void bitir(ServerLevel level, LevelChunk chunk, ChunkDonmaVerisi durum, long anahtar, ChunkIsi is) {
        durum.durum(is.gecis.yeniG(), is.gecis.yeniCozulme());
        durum.sonBuhar(is.gecis.buhar());
        durum.sonYon(is.sonrakiYon);
        chunk.setUnsaved(true);
        ISLER.remove(anahtar);
        tamamlananAdim++;
        ozetAdim++;
        toplamDegisen += is.sayac.degisen;
        ozetDegisen += is.sayac.degisen;
        if (is.gecis.yeniG() == is.hedefG) {
            hedefeUlasan++;
        }
        if (is.sayac.suDegisti) {
            DisKancalar.al().suYuzeyiDegisti(level, chunk.getPos());
        }
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

    private static void ozet(DunyaDonemi donem) {
        if (ozetAdim == 0 && ozetSutun == 0) {
            return;
        }
        DogalAfetler.LOGGER.info(String.format(Locale.ROOT, "[donma] özet: G=%.2f %s buhar=%.2f | yüklü=%d bekleyen=%d süren=%d"
                        + " | son 100 tick: %d adım, %d blok, %d sütun, işçi ort %.2f ms/tick en çok %.2f ms, süre sınırı %d kez"
                        + " | bütçe %d blok/tick (kısma %.2f, TPS %.1f), tick başına en çok %d blok | toplam %d adım, %d blok",
                donem.donmaIlerlemesi(), donemAdi(donem.cozulmeDonemi()), donem.buharlasmaDerinligi(), YUKLU.size(),
                BEKLEYEN.size(), ISLER.size(), ozetAdim, ozetDegisen, ozetSutun, ozetNs / 100.0 / 1e6,
                ozetEnUzunNs / 1e6, ozetSureSiniri, sonBlokSiniri, sonKisma, TpsOlcer.tps(), enCokTickDegisen,
                tamamlananAdim, toplamDegisen));
        ozetAdim = ozetDegisen = ozetSutun = ozetNs = ozetEnUzunNs = 0;
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

    /** Kuyrukta iş yok mu (her tick yazılır; tek oyunculu otomasyon istemci thread'inden okur). */
    public static boolean kuyrukBos() {
        return kuyrukBos;
    }

    static int yukluSayisi() {
        return YUKLU.size();
    }

    static int bekleyenSayisi() {
        return BEKLEYEN.size();
    }

    static int surenIsSayisi() {
        return ISLER.size();
    }

    static long tamamlananAdim() {
        return tamamlananAdim;
    }

    static long toplamDegisen() {
        return toplamDegisen;
    }

    public static int sonTickDegisen() {
        return sonTickDegisen;
    }

    public static int enCokTickDegisen() {
        return enCokTickDegisen;
    }

    /** Isınmadan sonraki en uzun işçi tick'i (ns). */
    static long enUzunNs() {
        return enUzunNs;
    }

    /** Isınma penceresindeki (ilk {@value #ISINMA_TICK} işçi tick'i) en uzun tick (ns). */
    static long isinmaEnUzunNs() {
        return isinmaEnUzunNs;
    }

    static int sonBlokSiniri() {
        return sonBlokSiniri;
    }

    static double sonKisma() {
        return sonKisma;
    }

    static long hedefeUlasan() {
        return hedefeUlasan;
    }
}
