package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ortak sanal suyun dış API'si (madde 8; sözleşme {@code .vibe/kararlar/sanal-su.md} §A). Sunucu boyut başına bir
 * {@link SanalSuDunyasi} tutar (otorite); istemci kendi seviyesinin kopyasını {@link #istemci} alanına koyar. Gerçek blok
 * hiç değişmez. Zaman ölçekli afet saatidir ({@code DunyaDonemi.saat()}).
 *
 * <p><b>İş parçacığı:</b> sorgular paylaşılan geçici nesneler kullanır; yalnız seviyenin sahibi iş parçacığından (sunucu
 * thread'i / istemci thread'i) çağrılır. Değiştiren API'ler yalnız sunucu thread'inde.
 */
public final class SanalSu {
    /** Bir boyutun sunucu durumu: su, tazeleme planı ve sabitlenmiş (oyuncusuz hesaplanan) chunk'lar. */
    static final class Boyut {
        final SanalSuDunyasi dunya;
        final TazelemePlani plan = new TazelemePlani(2, 20, 16);
        final Set<Long> sabit = new HashSet<>();
        /** Mağara kimliği -> cep kutusu {x0, y0, z0, x1, y1, z1} ve başlangıç hash'i (harita bütünlüğü kanıtı). */
        final Map<Integer, long[]> magaraKutusu = new HashMap<>();
        /** Deniz alanının tick başına okuma bütçesi (her 10 tick'te yeniden hesaplanır). */
        int tickButcesi = 1;
        /** Bu oyun saatine kadar deniz alanı okunmaz (yalnız test, {@link #alanOkumasiDuraklat}). */
        long okumaDuraklatBitis = Long.MIN_VALUE;
        /** Dalga kimliği -> afet numarası ({@link #dalgaBaslat(ServerLevel, Dalga, long)}; kalıcı). */
        final Map<Integer, Long> dalgaAfetNo = new HashMap<>();

        Boyut(ServerLevel l) {
            dunya = yeniDunya(l.getSeaLevel(), sunucuYurutucu());
            dunya.canliZemin(canliZemin(l));
        }
    }

    static final Map<ResourceKey<Level>, Boyut> BOYUTLAR = new HashMap<>();
    /** Afet numarası yok: {@link #dalgaBaslat(ServerLevel, Dalga)} tekillik denetlemez. */
    public static final long AFET_YOK = Long.MIN_VALUE;
    /** İstemci seviyesinin kopyası ({@code client.su} doldurur); yoksa {@code null}. Ortak kod istemci sınıfı görmez. */
    public static volatile SanalSuDunyasi istemci;
    /** İstemcide tahmin edilen ölçekli afet saati ({@code client.su} bağlar). */
    public static volatile DoubleSupplier istemciSaati = () -> 0;
    /** Sunucunun bu tick'teki ölçekli afet saati ({@link SanalSuSunucu} her tick başında yazar). */
    static volatile long sunucuSaati;
    private static int sonKimlik;
    private static Executor sunucuYurutucu;
    private static Executor istemciYurutucu;

    private SanalSu() {
    }

    /** Mod kurucusundan: ağ paketi, komutlar, dönem süreçleri, çeviriler. */
    public static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye("su/durum", SuPaketi.class, SuPaketi::yaz, SuPaketi::oku, SuPaketi::isle));
        SuKomutlari.kaydet();
        DenizYukselmesiSureci.kaydet();
        MagaraSureci.kaydet();
    }

    /** Yeni bir su dünyası; {@code denizSeviyesi} bu yüzey ve altındaki su kütlelerini deniz sayar. */
    public static SanalSuDunyasi yeniDunya(int denizSeviyesi, Executor yurutucu) {
        SanalSuDunyasi d = new SanalSuDunyasi(new DenizAlani(DenizAlani.EN_COK_ARTIS, 1, denizSeviyesi));
        d.alan().yurutucu(yurutucu);
        return d;
    }

    private static synchronized Executor sunucuYurutucu() {
        if (sunucuYurutucu == null) {
            sunucuYurutucu = yurutucu("dogalafetler-su-alan-sunucu");
        }
        return sunucuYurutucu;
    }

    /** İstemci deniz alanının arka plan hesap iş parçacığı. */
    public static synchronized Executor istemciYurutucu() {
        if (istemciYurutucu == null) {
            istemciYurutucu = yurutucu("dogalafetler-su-alan-istemci");
        }
        return istemciYurutucu;
    }

    private static Executor yurutucu(String ad) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, ad);
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    static Boyut boyut(ServerLevel l) {
        return BOYUTLAR.computeIfAbsent(l.dimension(), k -> new Boyut(l));
    }

    /** Seviyenin şimdiki saati (sunucuda afet saati, istemcide tahmini). */
    public static double saat(Level l) {
        return l.isClientSide() ? istemciSaati.getAsDouble() : sunucuSaati;
    }

    /** Sunucunun bu tick'teki ölçekli afet saati (seviye erişimi olmayan sunucu kodu için). */
    public static long suankiSunucuSaati() {
        return sunucuSaati;
    }

    /** Seviyenin sanal suyu; hiç su yoksa {@code null}. */
    public static SanalSuDunyasi dunya(Level l) {
        if (l == null) {
            return null;
        }
        SanalSuDunyasi d;
        if (l.isClientSide()) {
            d = istemci;
        } else {
            Boyut b = BOYUTLAR.get(l.dimension());
            d = b == null ? null : b.dunya;
        }
        return d != null && d.aktif() ? d : null;
    }

    // ---------------------------------------------------------------- ıslaklık ve sorgu (sıcaklık, render, fizik)

    /** Varlığın kutusunun <b>sanal</b> suda kalan payı (0..1); gerçek suyu saymaz. */
    public static double batiklikOrani(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        if (d == null) {
            return 0;
        }
        AABB b = e.getBoundingBox();
        return d.batiklik(e.getX(), e.getZ(), b.minY, b.maxY, saat(e.level()));
    }

    /** Varlık sanal suya değiyor mu (ıslaklık). Gerçek su için vanilla {@code isInWater()}: mixin onu da sanal suyla doldurur. */
    public static boolean icindeMi(Entity e) {
        return batiklikOrani(e) > 0;
    }

    /** Sütundaki en üst sanal su yüzeyi (mutlak Y); sanal su yoksa NaN. */
    public static double suSeviyesi(Level l, int x, int z) {
        SanalSuDunyasi d = dunya(l);
        return d == null ? Double.NaN : d.enUstYuzey(x, z, saat(l));
    }

    // ---------------------------------------------------------------- dış sistemlerin bildirimi (K0-30)

    /** Blok değiştiren dış sistem (donma su↔buz, yıkım) bildirir: chunk deniz alanında yeniden okunur. Sunucu thread'i. */
    public static void chunkDegisti(Level l, ChunkPos p) {
        if (!l.isClientSide()) {
            Boyut b = BOYUTLAR.get(l.dimension());
            if (b != null) {
                b.dunya.alan().gecersizKil(p.x, p.z);
            }
        }
    }

    /**
     * Test (SU-36): deniz alanının okumasını ve tazelenmesini {@code tick} oyun tick'i dondurur; alanın bildiği zemin bu sürede
     * bayat kalır (kodla değişen bloğu görmez). Süre dolunca okuma kendiliğinden sürer.
     */
    static void alanOkumasiDuraklat(ServerLevel l, int tick) {
        boyut(l).okumaDuraklatBitis = l.getGameTime() + tick;
    }


    /** Oyuncu olmasa da deniz alanı hesaplanacak chunk aralığı (tsunami hedefi, GameTest). */
    public static void bolgeSabitle(ServerLevel l, ChunkPos a, ChunkPos b) {
        Set<Long> s = boyut(l).sabit;
        for (int x = Math.min(a.x, b.x); x <= Math.max(a.x, b.x); x++) {
            for (int z = Math.min(a.z, b.z); z <= Math.max(a.z, b.z); z++) {
                s.add(DenizAlani.anahtar(x, z));
            }
        }
    }

    public static void bolgeBirak(ServerLevel l) {
        boyut(l).sabit.clear();
    }

    // ---------------------------------------------------------------- dalga (Faz 4 tsunami)

    /** Dalgayı başlatır ve boyuttaki oyunculara gönderir; kimliği döner. */
    public static int dalgaBaslat(ServerLevel l, Dalga d) {
        return dalgaBaslat(l, d, AFET_YOK);
    }

    /**
     * Afete bağlı dalga: aynı {@code afetNo} ile süren bir dalga varsa yeni dalga eklenmez, onun kimliği döner (çift dalga
     * olmaz; yeniden başlatmada dalga zaten kayıttan geri gelir, §A2). {@code afetNo} afetin kendi kalıcı numarasıdır.
     */
    public static int dalgaBaslat(ServerLevel l, Dalga d, long afetNo) {
        if (afetNo != AFET_YOK) {
            int var = dalgaAfetBul(afetNo);
            if (var > 0) {
                return var;
            }
        }
        int id = ++sonKimlik;
        Boyut b = boyut(l);
        b.dunya.dalgaEkle(id, d);
        if (afetNo != AFET_YOK) {
            b.dalgaAfetNo.put(id, afetNo);
        }
        yayinla(l);
        return id;
    }

    /** {@code afetNo}'ya bağlı süren dalganın kimliği, yoksa -1 (bitmiş dalgaların kaydı silinir). */
    static int dalgaAfetBul(long afetNo) {
        int sonuc = -1;
        for (Boyut b : BOYUTLAR.values()) {
            b.dalgaAfetNo.keySet().removeIf(id -> !b.dunya.dalgalar().containsKey(id));
            for (Map.Entry<Integer, Long> e : b.dalgaAfetNo.entrySet()) {
                if (e.getValue() == afetNo) {
                    sonuc = e.getKey();
                }
            }
        }
        return sonuc;
    }

    /** Dalga henüz sürüyor mu (kimlik kayıttan geri gelmiş olabilir). */
    public static boolean dalgaVarMi(ServerLevel l, int id) {
        Boyut b = BOYUTLAR.get(l.dimension());
        return b != null && b.dunya.dalgalar().containsKey(id);
    }

    /** Dalganın parametrelerini değiştirir (ör. kıyıda yavaşlama); yoksa {@code false}. */
    public static boolean dalgaGuncelle(ServerLevel l, int id, Dalga d) {
        Boyut b = boyut(l);
        if (!b.dunya.dalgalar().containsKey(id)) {
            return false;
        }
        b.dunya.dalgaEkle(id, d);
        yayinla(l);
        return true;
    }

    public static boolean dalgaBitir(ServerLevel l, int id) {
        boolean vardi = boyut(l).dunya.dalgaKaldir(id);
        if (vardi) {
            yayinla(l);
        }
        return vardi;
    }

    /** Tsunami öncesi çekilme (görsel; istemci gerçek suyun üst katmanlarını gizler). */
    public static int cekilmeBaslat(ServerLevel l, Cekilme c) {
        int id = ++sonKimlik;
        boyut(l).dunya.cekilmeAyarla(id, c);
        yayinla(l);
        return id;
    }

    public static boolean cekilmeBitir(ServerLevel l, int id) {
        boolean vardi = boyut(l).dunya.cekilmeKaldir(id);
        if (vardi) {
            yayinla(l);
        }
        return vardi;
    }

    // ---------------------------------------------------------------- sel (taşkın) ve ısı bölgeleri

    /**
     * Sel suyunu başlatır ve boyuttaki oyunculara gönderir; kimliği döner. Yükselme {@link SelSuyu#baslangic()} saatinden itibaren
     * parametrik hesaplanır; bitince {@code SanalSuSunucu} kendiliğinden kaldırır.
     */
    public static int selBaslat(ServerLevel l, SelSuyu s) {
        int id = ++sonKimlik;
        boyut(l).dunya.selEkle(id, s);
        yayinla(l);
        return id;
    }

    public static boolean selBitir(ServerLevel l, int id) {
        Boyut b = BOYUTLAR.get(l.dimension());
        boolean vardi = b != null && b.dunya.selKaldir(id);
        if (vardi) {
            yayinla(l);
        }
        return vardi;
    }

    /** Sel henüz sürüyor mu (kimlik kaydından geri gelmiş olabilir). */
    public static boolean selVarMi(ServerLevel l, int id) {
        Boyut b = BOYUTLAR.get(l.dimension());
        return b != null && b.dunya.seller().containsKey(id);
    }

    /**
     * Sel suyunu buharlaştıran ısı kaynağı bölgelerini ayarlar; liste değişmediyse paket gönderilmez. Değiştiyse {@code true}.
     */
    public static boolean isiBolgeleriAyarla(ServerLevel l, List<IsiBolgesi> yeni) {
        Boyut b = BOYUTLAR.get(l.dimension());
        if (b == null || Arrays.equals(b.dunya.isiDizisi(), yeni.toArray(new IsiBolgesi[0]))) {
            return false;
        }
        b.dunya.isiBolgeleriAyarla(yeni);
        yayinla(l);
        return true;
    }

    // ---------------------------------------------------------------- mağara suyu (bölüm B)

    /**
     * {@code merkez}'den mağaraya en çok {@code hacim} hücre sanal su doldurur (önce en alçak, düz yüzey; K0-29); su
     * {@code kalisTick} ölçekli tick kalır, sonra kademeli kurur ve iz bırakmaz. Merkez boş bir mağara hücresi değilse ya da
     * hiç dolmazsa -1. Kimlik {@link #magaraBitir} için.
     */
    public static int magaraDoldur(ServerLevel l, BlockPos merkez, int hacim, long kalisTick) {
        SuAyarlari.Sunucu a = SuAyarlari.sunucu();
        MagaraCebi cep = MagaraDolumu.doldur(magaraHucresi(l), merkez.getX(), merkez.getY(), merkez.getZ(),
                Math.max(1, Math.min(hacim, a.magaraMaksHacim.get())), a.magaraMaksKesif.get());
        if (cep.bos()) {
            return -1;
        }
        MagaraZamani zaman = new MagaraZamani(sunucuSaati, cep.minY(), cep.ustY(), a.magaraDolmaHizi.get() / 20.0,
                Math.max(0, kalisTick), a.kurumaHizi());
        int id = ++sonKimlik;
        Boyut b = boyut(l);
        long[] kutu = cepKutusu(cep);
        kutu[6] = bolgeHash(l, (int) kutu[0], (int) kutu[1], (int) kutu[2], (int) kutu[3], (int) kutu[4], (int) kutu[5]);
        b.magaraKutusu.put(id, kutu);
        b.dunya.magaraEkle(id, cep, zaman);
        yayinla(l);
        return id;
    }

    /** Mağarayı hemen kaldırır (dünya zaten değişmemiştir). */
    public static boolean magaraBitir(ServerLevel l, int id) {
        Boyut b = boyut(l);
        b.magaraKutusu.remove(id);
        boolean vardi = b.dunya.magaraKaldir(id);
        if (vardi) {
            yayinla(l);
        }
        return vardi;
    }

    /** Cebin kutusu (+2 blok) {x0, y0, z0, x1, y1, z1, (hash yeri)}. */
    static long[] cepKutusu(MagaraCebi cep) {
        int x0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (long k : cep.sutunAnahtarlari()) {
            x0 = Math.min(x0, (int) (k >> 32));
            x1 = Math.max(x1, (int) (k >> 32));
            z0 = Math.min(z0, (int) k);
            z1 = Math.max(z1, (int) k);
        }
        return new long[] {x0 - 2, cep.minY() - 2, z0 - 2, x1 + 2, cep.ustY() + 2, z1 + 2, 0};
    }

    /** Boyuttaki bütün oyunculara tam durum; parametreler tick sonunda bir kez döneme yazılır (kalıcılık, SU-24, SU-34). */
    public static void yayinla(ServerLevel l) {
        Boyut b = BOYUTLAR.get(l.dimension());
        if (b != null) {
            AgKanali.boyuttakilereGonder(l.dimension(), SuPaketi.durumdan(l.dimension(), b.dunya));
        }
        SuKalicilik.kirlet();
    }

    /** Kayıttan kurulan kimlik: yeni kimlikler onun üstünden verilir. */
    static int sonKimlik() {
        return sonKimlik;
    }

    static void kimlikGordu(int id) {
        sonKimlik = Math.max(sonKimlik, id);
    }

    // ---------------------------------------------------------------- mixin kancaları (K0-25)

    /** Ayak hizasından sanal su yüzeyine yükseklik; suda değilse 0. */
    public static double mixinSuYuksekligi(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        if (d == null) {
            return 0;
        }
        AABB b = e.getBoundingBox();
        return d.suYuksekligi(e.getX(), e.getZ(), b.minY, b.maxY, saat(e.level()));
    }

    public static boolean gozSudaMi(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        return d != null && !Double.isNaN(d.ustu(e.getX(), e.getEyeY() - 0.11111111, e.getZ(), saat(e.level())));
    }

    /** Varlığın gözü sel suyunun içinde mi (boğulma hızlanır, K-E81); sunucu thread'i. */
    public static boolean gozSelSuyundaMi(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        return d != null && d.selSuyundaMi(e.getX(), e.getEyeY() - 0.11111111, e.getZ(), saat(e.level()));
    }

    /**
     * Dalga sürüklemesi (deterministik, iki tarafta aynı): {@code cikti} = yatay ivme (x, z); etki eden dalga ya da
     * {@code null}. Varlığın dalgaya bakan tarafında üç sıra katı blok varsa (su gölgesi: duvar, bina) kuvvet %40 azalır.
     */
    public static Dalga itmeHesapla(Entity e, double[] cikti) {
        SanalSuDunyasi d = dunya(e.level());
        if (d == null || d.dalgalar().isEmpty()) {
            cikti[0] = 0;
            cikti[1] = 0;
            return null;
        }
        AABB b = e.getBoundingBox();
        Dalga dalga = d.itme(e.getX(), e.getZ(), b.minY, b.maxY, saat(e.level()), cikti);
        if (dalga != null && suGolgesi(e.level(), e.position(), dalga)) {
            cikti[0] *= 0.6;
            cikti[1] *= 0.6;
        }
        return dalga;
    }

    private static boolean suGolgesi(Level l, Vec3 p, Dalga d) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int sira = 0;
        for (int i = 1; i <= 3; i++) {
            m.set(p.x - d.yonX() * i, p.y + 0.5, p.z - d.yonZ() * i);
            if (l.getBlockState(m).isCollisionShapeFullBlock(l, m)) {
                sira++;
            }
        }
        return sira >= 3;
    }

    /**
     * Sürüklemeyi uygular: oyuncuyu kendi istemcisi (hareketin sahibi), diğer varlıkları (mob, eşya) sunucu iter; dalganın
     * içinde oyuncu girdisi kısmen kaybolur (yatay hız sönümü). Mixin her tick çağırır.
     */
    public static void itmeUygula(Entity e) {
        boolean sahibi = e.level().isClientSide() ? e instanceof Player p && p.isLocalPlayer() : !(e instanceof Player);
        if (!sahibi) {
            return;
        }
        double[] it = new double[2];
        if (itmeHesapla(e, it) != null) {
            Vec3 v = e.getDeltaMovement();
            double sonum = e instanceof Player ? 0.88 : 1.0;
            e.setDeltaMovement(v.x * sonum + it[0], v.y, v.z * sonum + it[1]);
        }
    }

    // ---------------------------------------------------------------- dünya okuma

    /**
     * Bloğun mağara suyu için özü: hava; geçirgen (çarpışmasız ya da vanilla'da su tutabilen {@code WATERLOGGED}); katı.
     * Gerçek sıvı içeren hücre katı sayılır (su zaten orada).
     */
    public static MagaraDolumu.BlokOzu blokOzu(BlockGetter g, BlockPos p, BlockState s) {
        if (s.isAir()) {
            return MagaraDolumu.BlokOzu.HAVA;
        }
        if (!s.getFluidState().isEmpty()) {
            return MagaraDolumu.BlokOzu.KATI;
        }
        if (s.getCollisionShape(g, p).isEmpty() || s.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return MagaraDolumu.BlokOzu.GECIRGEN;
        }
        return MagaraDolumu.BlokOzu.KATI;
    }

    /**
     * Sütunun şu anki zemini doğrudan dünyadan (SU-27, SU-36): yaprak hariç hareket engelleyen yüzey (sıvı dahil) ve
     * üstü, yükseklik haritasından (blok değişince hemen güncel); chunk yüklü değilse bilinmiyor. Sunucu ve istemci aynı kural.
     */
    public static SanalSuDunyasi.ZeminOkuyucu canliZemin(Level l) {
        return (x, z) -> l.hasChunk(x >> 4, z >> 4) ? l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                : Integer.MIN_VALUE;
    }

    /** Mağara dolumu hücresi: yüklü olmayan chunk ve sütunun yaprak hariç yüzeyinin hizası ve üstü açık (K0-29). */
    public static MagaraDolumu.Hucre magaraHucresi(Level l) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        return (x, y, z) -> {
            m.set(x, y, z);
            if (!l.isLoaded(m)) {
                return MagaraDolumu.Tur.ACIK;
            }
            return MagaraDolumu.sinifla(true, blokOzu(l, m, l.getBlockState(m)), y,
                    l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        };
    }

    /**
     * Deniz alanı sütun okuyucusu (sunucu ve istemci aynı kod): zemin = yaprak ve hava atlanmış hareket engelleyen yüzeyin
     * bir üstü; durgun (kaynak) su yüzeyi su sütunudur, akan su kara sayılır (SU-10 B3).
     */
    public static DenizAlani.SutunOkuyucu okuyucu(Level level) {
        return (cx, cz, zemin, su, taban) -> {
            LevelChunk c = level.getChunkSource().getChunkNow(cx, cz);
            if (c == null) {
                return false;
            }
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int alt = level.getMinBuildHeight();
            for (int i = 0; i < 256; i++) {
                int lx = i & 15;
                int lz = i >> 4;
                int y = c.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
                BlockState s = c.getBlockState(p.set(lx, y, lz));
                while (y > alt && (s.is(BlockTags.LEAVES) || s.isAir())) {
                    y--;
                    s = c.getBlockState(p.set(lx, y, lz));
                }
                boolean w = s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource();
                zemin[i] = y + 1;
                su[i] = w;
                if (w) {
                    int t = y;
                    while (t > alt && t > y - 64
                            && c.getBlockState(p.set(lx, t - 1, lz)).getFluidState().is(FluidTags.WATER)) {
                        t--;
                    }
                    taban[i] = t;
                } else {
                    taban[i] = y + 1;
                }
            }
            return true;
        };
    }

    // ---------------------------------------------------------------- harita bütünlüğü yardımcıları (test, §6 #15)

    /** Kutudaki blok durum kimliklerinin FNV-1a özeti (sıvı seviyesi dahil). */
    public static long bolgeHash(Level l, int x0, int y0, int z0, int x1, int y1, int z1) {
        long h = 0xcbf29ce484222325L;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    h ^= Block.getId(l.getBlockState(p.set(x, y, z)));
                    h *= 0x100000001b3L;
                }
            }
        }
        return h;
    }

    /** Kutuda koşulu sağlayan blok sayısı (ör. gerçek su blokları). */
    public static int bolgeSayim(Level l, BlockPos a, BlockPos b, Predicate<BlockState> kosul) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (kosul.test(l.getBlockState(p))) {
                n++;
            }
        }
        return n;
    }
}
