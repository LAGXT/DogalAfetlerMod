package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.KureTarayici;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.OncelikSecici;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi.YagisDurumu;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Ortak yangın yöneticisi (madde 6): orman yangını ve meteor ateşleri aynı kuyruğu kullanır. Her sunucu tick'inde, boyut
 * başına en çok {@code tickButcesi × TpsOlcer.kismaCarpani()} hücre oyuncuya yakınlık (yaşlanmalı) sırasıyla işlenir;
 * yüklü olmayan chunk'taki hücre bekler. Blok hasarı kapalıyken ateş yalnız veri + görsel + hasar + ısıdır, blok değişmez.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class YanginYoneticisi {
    /** Bir hücre en sık bu aralıkla işlenir (yayılma denemesi, sönme, bitiş). */
    static final int ISLEME_ARALIGI = 10;
    private static final int YAYILMA_DENEMESI = 3;
    private static final double SICRAMA_OLASILIGI = 0.04;
    private static final double SICRAMA_SAPMASI = 20;
    private static final int OZET_ARALIGI = 10;
    private static final int ISI_ARALIGI = 20;
    private static final int BEKLEYEN_ARALIGI = 20;
    private static final int OZET_LOG_ARALIGI = 600;
    private static final int YANMIS_HAFIZA_TICK = 2400;
    private static final double OZET_MENZILI = 128;
    private static final double HUCRE_MENZILI = 64;
    private static final double ISI_MENZILI = 96;
    private static final int OZET_HUCRE_TAVANI = 48;
    private static final int BUHAR_TAVANI = 32;
    private static final double OYUNCUSUZ_MESAFE_KARE = 1.0e6;
    private static final int EN_BUYUK_ALAN_YARICAPI = 24;

    private static final Map<ResourceKey<Level>, YanginVerisi> VERILER = new ConcurrentHashMap<>();
    private static volatile DoubleSupplier kismaKaynagi = TpsOlcer::kismaCarpani;
    private static volatile BiPredicate<ServerLevel, BlockPos> yukluKaynagi = (level, konum) -> level.isLoaded(konum);
    private static volatile IntSupplier sonumKaynagi = () -> YanginAyarlari.afetSonrasiSonumSaniye() * 20;
    private static volatile boolean bekleyenDenetimi = true;

    private YanginYoneticisi() {
    }

    // ---------------------------------------------------------------- API (.vibe/kararlar/yangin.md §A)

    /**
     * {@code konum}'daki yakıtı tutuşturur; konum hava ise yanıcı komşuları tutuşur. Yanmaz etiketli, yanıcılığı 0 olan,
     * sanal su altındaki ya da yüklü olmayan blok tutuşmaz. Tutuşan hücre sayısını döndürür.
     */
    public static int tutustur(ServerLevel level, BlockPos konum, YanginKaynagi kaynak, double yogunluk) {
        Objects.requireNonNull(kaynak, "kaynak");
        YanginVerisi veri = veri(level);
        if (hucreEkle(level, veri, konum, kaynak, yogunluk)) {
            return 1;
        }
        if (!havaMi(level, konum)) {
            return 0;
        }
        int sayi = 0;
        for (Direction yon : Direction.values()) {
            if (hucreEkle(level, veri, konum.relative(yon), kaynak, yogunluk)) {
                sayi++;
            }
        }
        return sayi;
    }

    /**
     * Meteor krateri gibi bir alanın yanıcı yüzeyini (havaya değen blokları) {@code alanOrani(yogunluk)} olasılığıyla
     * tutuşturur. Tarama tick başına okuma bütçesiyle (hücre bütçesi × 64, TPS kısması dahil) yapılır: küçük alan hemen,
     * büyük alan sonraki tick'lerde biter (Y-21). Bu çağrıda tutuşan hücre sayısını döndürür.
     */
    public static int alanTutustur(ServerLevel level, BlockPos merkez, int yaricap, YanginKaynagi kaynak,
            double yogunluk) {
        Objects.requireNonNull(kaynak, "kaynak");
        YanginVerisi veri = veri(level);
        AlanIsi is = new AlanIsi(merkez.immutable(), Math.max(0, Math.min(EN_BUYUK_ALAN_YARICAPI, yaricap)), kaynak,
                yogunluk);
        veri.alanIsleri.add(is);
        alanIsleriniIsle(level, veri);
        return is.tutusan;
    }

    /** Bu boyutta süren (birkaç tick'e yayılmış) alan tutuşturma işi sayısı. */
    public static int bekleyenAlanSayisi(ServerLevel level) {
        return veri(level).alanIsleri.size();
    }

    /**
     * Bölgedeki ({@code null} = boyutun tümü) ateşi iz bırakmadan söndürür: yakıt yerinde kalır, ateş bloğu kalkar (chunk'ı
     * yüklü değilse yüklenince, Y-19). Bölgedeki süren alan tutuşturma işleri de durur.
     */
    public static int sondur(ServerLevel level, AABB bolge) {
        YanginVerisi veri = veri(level);
        veri.alanIsleri.removeIf(is -> bolge == null || bolge.contains(Vec3.atCenterOf(is.merkez)));
        return sondur(level, veri, h -> bolge == null || icinde(bolge, h.konum), "sondur");
    }

    public static int aktifSayi(ServerLevel level) {
        return veri(level).hucreler.size();
    }

    public static int aktifSayi(ServerLevel level, AABB bolge) {
        int sayi = 0;
        for (YanginHucresi h : veri(level).hucreler.values()) {
            if (icinde(bolge, h.konum)) {
                sayi++;
            }
        }
        return sayi;
    }

    /**
     * Afet bitti; o kaynağın süren alan tutuşturmaları durur ve yeni tutuşma olmaz (Y-05). {@code temiz=true}: ateşler
     * hemen söndürülür, ateş blokları kalkar, yanıcı bloklar korunur. {@code temiz=false}: kalan hücreler yayılmaz ve en
     * geç {@code afetSonrasiSonumSaniye} içinde yanıp biter (yakıt vanilla'daki gibi havaya döner; {@code afetBlokHasari=false}
     * iken blok değişmez). Vanilla ateşe devredilmez. Afet kaynakları için yönetici bunu {@link AfetBittiEvent}'ten kendisi
     * çağırır (Y-27); elle çağrmak idempotenttir.
     */
    public static void kaynakBitti(YanginKaynagi kaynak, boolean temiz) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            kaynakBitti(server, kaynak, temiz);
        }
    }

    public static void kaynakBitti(MinecraftServer server, YanginKaynagi kaynak, boolean temiz) {
        Objects.requireNonNull(kaynak, "kaynak");
        int sonum = Math.max(1, sonumKaynagi.getAsInt());
        for (ServerLevel level : server.getAllLevels()) {
            YanginVerisi veri = VERILER.get(level.dimension());
            if (veri == null) {
                continue;
            }
            veri.alanIsleri.removeIf(is -> is.kaynak.equals(kaynak));
            if (temiz) {
                int sonen = sondur(level, veri, h -> h.kaynak.equals(kaynak), "kaynakBitti(temiz)");
                if (sonen > 0) {
                    DogalAfetler.LOGGER.info("yangın kaynağı bitti (temiz): {} {} hücre söndürüldü ({})",
                            kaynak, sonen, level.dimension().location());
                }
                continue;
            }
            long simdi = level.getGameTime();
            int sayi = 0;
            for (YanginHucresi h : veri.hucreler.values()) {
                if (h.kaynak.equals(kaynak) && h.yayilabilir) {
                    h.yayilabilir = false;
                    h.bitis = Math.min(h.bitis, simdi + sonum / 2 + veri.rastgele.nextInt(sonum / 2 + 1));
                    sayi++;
                }
            }
            if (sayi > 0) {
                veri.setDirty();
                DogalAfetler.LOGGER.info("yangın kaynağı bitti: {} {} hücre yayılmadan en geç {} sn'de yanıp bitecek ({})",
                        kaynak, sayi, sonum / 20, level.dimension().location());
            }
        }
    }

    /** Şu anki tick bütçesi: config × TPS kısma çarpanı. */
    public static int etkinButce() {
        return YanginMantigi.butce(YanginAyarlari.tickButcesi(), kismaCarpani());
    }

    public static double kismaCarpani() {
        return kismaKaynagi.getAsDouble();
    }

    /** Yöneticinin bu boyuttaki tick süresi, son 100 çalışan tick: {ortalama ms, en çok ms}. */
    public static double[] sureOzeti(ServerLevel level) {
        return veri(level).sureOzeti();
    }

    /** Yanıcılık vanilla değerinden gelir ({@code getFlammability}); {@code dogalafetler:yanmaz} onu ezer (N12). */
    public static boolean yanabilir(BlockState durum, BlockGetter dunya, BlockPos konum) {
        return !durum.isAir() && !durum.is(YanginEtiketleri.YANMAZ)
                && durum.getFlammability(dunya, konum, Direction.UP) > 0;
    }

    // ---------------------------------------------------------------- test kancaları

    /** Testte TPS kademesini zorlar; önceki kaynağı döndürür. */
    static DoubleSupplier kismaKaynagiAyarla(DoubleSupplier kaynak) {
        DoubleSupplier onceki = kismaKaynagi;
        kismaKaynagi = Objects.requireNonNull(kaynak, "kaynak");
        return onceki;
    }

    /** Testte "chunk yüklü değil" durumunu taklit eder (ateş silme ve bekleyen silme yolu); önceki kaynağı döndürür. */
    static BiPredicate<ServerLevel, BlockPos> yukluKaynagiAyarla(BiPredicate<ServerLevel, BlockPos> kaynak) {
        BiPredicate<ServerLevel, BlockPos> onceki = yukluKaynagi;
        yukluKaynagi = Objects.requireNonNull(kaynak, "kaynak");
        return onceki;
    }

    /** Testte {@code kaynakBitti(temiz=false)} sönüm penceresini (tick) zorlar; önceki kaynağı döndürür. */
    static IntSupplier sonumKaynagiAyarla(IntSupplier kaynak) {
        IntSupplier onceki = sonumKaynagi;
        sonumKaynagi = Objects.requireNonNull(kaynak, "kaynak");
        return onceki;
    }

    /** Testte bekleyen silmenin 1 Hz denetimini açıp kapatır; {@code ChunkEvent.Load} yolu her zaman çalışır. */
    static void bekleyenDenetimiAyarla(boolean acik) {
        bekleyenDenetimi = acik;
    }

    static int sonButce(ServerLevel level) {
        return veri(level).sonButce;
    }

    static YanginVerisi veri(ServerLevel level) {
        return VERILER.computeIfAbsent(level.dimension(), k -> level.getDataStorage()
                .computeIfAbsent(YanginVerisi::yukle, YanginVerisi::new, YanginVerisi.AD));
    }

    // ---------------------------------------------------------------- olaylar

    @SubscribeEvent
    public static void dunyaYuklendi(LevelEvent.Load olay) {
        if (olay.getLevel() instanceof ServerLevel level) {
            veri(level);
        }
    }

    @SubscribeEvent
    public static void dunyaKapandi(LevelEvent.Unload olay) {
        if (olay.getLevel() instanceof ServerLevel level) {
            VERILER.remove(level.dimension());
        }
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        VERILER.clear();
    }

    /**
     * Afet bittiğinde o afetin ateşleri (kaynak = tür + {@code afetNo} = başlangıç saati) {@link YanginMantigi#temizBitis}
     * eşlemesiyle söndürülür ya da yanıp bitmeye bırakılır (Y-27). Faz 4 afetlerinin {@code kaynakBitti} çağırması gerekmez.
     */
    @SubscribeEvent
    public static void afetBitti(AfetBittiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            kaynakBitti(server, YanginKaynagi.afet(olay.tur(), olay.baslangicSaati()), YanginMantigi.temizBitis(olay.neden()));
        }
    }

    /** Chunk yüklendi: bekleyen ateş silmeleri bir sonraki tick'te denetlenir (olay işçi thread'inden de gelebilir). */
    @SubscribeEvent
    public static void chunkYuklendi(ChunkEvent.Load olay) {
        if (olay.getLevel() instanceof ServerLevel level) {
            YanginVerisi veri = VERILER.get(level.dimension());
            if (veri != null) {
                veri.chunkYuklendi = true;
            }
        }
    }

    @SubscribeEvent
    public static void dunyaTick(TickEvent.LevelTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !(olay.level instanceof ServerLevel level)) {
            return;
        }
        YanginVerisi veri = VERILER.get(level.dimension());
        if (veri == null) {
            return;
        }
        long baslangic = System.nanoTime();
        long simdi = level.getGameTime();
        if (simdi % 200 == 0) {
            veri.yanmislar.values().removeIf(bitis -> bitis <= simdi);
        }
        boolean calisti = false;
        if (!veri.bekleyenSilmeler.isEmpty()
                && (veri.chunkYuklendi || (bekleyenDenetimi && simdi % BEKLEYEN_ARALIGI == 0))) {
            veri.chunkYuklendi = false;
            bekleyenleriIsle(level, veri);
            calisti = true;
        }
        if (veri.hucreler.isEmpty() && veri.alanIsleri.isEmpty()) {
            if (!veri.paketAlanlar.isEmpty() && simdi % OZET_ARALIGI == 0) {
                ozetGonder(level, veri, simdi);
            }
            if (calisti) {
                veri.sureKaydet((System.nanoTime() - baslangic) / 1.0e6);
            }
            return;
        }
        int butce = butceyiGuncelle(level, veri);
        alanIsleriniIsle(level, veri);
        isle(level, veri, simdi, butce);
        if (simdi % OZET_ARALIGI == 0) {
            ozetGonder(level, veri, simdi);
        }
        veri.sureKaydet((System.nanoTime() - baslangic) / 1.0e6);
        if (simdi % OZET_LOG_ARALIGI == 0) {
            double[] sure = veri.sureOzeti();
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "yangın özeti: %s aktif %d, bütçe %d, alan işi %d, bekleyen silme %d, yönetici %.2f ms ort / %.2f ms en çok",
                    level.dimension().location(), veri.hucreler.size(), butce, veri.alanIsleri.size(),
                    veri.bekleyenSilmeler.size(), sure[0], sure[1]));
        }
    }

    // ---------------------------------------------------------------- işleme

    private static int butceyiGuncelle(ServerLevel level, YanginVerisi veri) {
        double carpan = kismaCarpani();
        int butce = YanginMantigi.butce(YanginAyarlari.tickButcesi(), carpan);
        if (butce != veri.sonButce && (veri.sonButce >= 0 || carpan < 1.0)) {
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "yangın kısma: %s bütçe %d -> %d hücre/tick (TPS çarpanı %.2f, TPS %.1f, aktif %d)",
                    level.dimension().location(), Math.max(0, veri.sonButce), butce, carpan, TpsOlcer.tps(),
                    veri.hucreler.size()));
        }
        veri.sonButce = butce;
        return butce;
    }

    private static void isle(ServerLevel level, YanginVerisi veri, long simdi, int butce) {
        List<ServerPlayer> oyuncular = level.players();
        veri.diziHazirla(veri.hucreler.size());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int m = 0;
        for (YanginHucresi h : veri.hucreler.values()) {
            long bekleyen = simdi - h.sonIslenme - ISLEME_ARALIGI;
            if (bekleyen < 0 || !level.isLoaded(p.set(h.konum))) {
                continue;
            }
            veri.puanlar[m] = YanginMantigi.oncelik(enYakinMesafeKare(oyuncular, p), bekleyen);
            veri.adaylar[m++] = h;
        }
        boolean hasar = Kurallar.blokHasariAcik(level.getGameRules());
        double[] ruzgar = YanginMantigi.gununRuzgari(simdi / 24000L);
        for (int i : OncelikSecici.enIyiler(veri.puanlar, m, butce)) {
            YanginHucresi h = veri.adaylar[i];
            if (veri.hucreler.get(h.konum) == h) {
                hucreyiIsle(level, veri, h, simdi, hasar, ruzgar);
            }
        }
        Arrays.fill(veri.adaylar, 0, m, null);
    }

    private static void hucreyiIsle(ServerLevel level, YanginVerisi veri, YanginHucresi h, long simdi, boolean hasar,
            double[] ruzgar) {
        h.sonIslenme = simdi;
        BlockPos konum = BlockPos.of(h.konum);
        BlockState durum = level.getBlockState(konum);
        if (!yanabilir(durum, level, konum) || YanginKancalari.sanalSu().suAltinda(level, konum)) {
            kaldir(level, veri, h);
            return;
        }
        if (h.atesVar && !atesYerinde(level, h)) {
            h.atesVar = false;
            kaldir(level, veri, h);
            return;
        }
        if (hasar && !h.atesVar) {
            atesYak(level, veri, h, konum);
        } else if (!hasar && h.atesVar) {
            atesSil(level, veri, h);
        }
        BlockPos ust = h.atesVar ? BlockPos.of(h.atesKonumu) : konum.above();
        YagisDurumu yagis = yagis(level, ust);
        RandomSource r = veri.rastgele;
        if (yagis != YagisDurumu.YOK && YanginAyarlari.yagmurdaSonme() && r.nextDouble()
                < YanginMantigi.yagmurSonmeOlasiligi(YanginAyarlari.yagmurSonmeOlasiligi(), h.yasOrani(simdi),
                        yagis == YagisDurumu.KORUNAKLI)) {
            if (veri.buharlar.size() < BUHAR_TAVANI) {
                veri.buharlar.add(ust.asLong());
            }
            kaldir(level, veri, h);
            return;
        }
        if (simdi >= h.bitis) {
            yakitiTuket(level, veri, h, konum, durum, hasar, simdi);
            return;
        }
        if (!hasar) {
            varlikHasari(level, ust);
        }
        if (h.yayilabilir && yagis == YagisDurumu.YOK) {
            yay(level, veri, h, konum, ruzgar);
        }
    }

    /** Kuru hücrenin yayılması; yağmurdaki (açık ya da korunaklı) hücre yaymaz, yalnız söner ya da yanar biter (Y-08). */
    private static void yay(ServerLevel level, YanginVerisi veri, YanginHucresi h, BlockPos konum, double[] ruzgar) {
        RandomSource r = veri.rastgele;
        int yaricap = YanginMantigi.kivilcimYaricapi(h.yogunluk);
        double hiz = YanginAyarlari.yayilmaHizCarpani() * YanginMantigi.yayilmaCarpani(h.yogunluk);
        for (int deneme = 0; deneme < YAYILMA_DENEMESI; deneme++) {
            int dx = r.nextInt(2 * yaricap + 1) - yaricap;
            int dy = r.nextInt(4) - 1;
            int dz = r.nextInt(2 * yaricap + 1) - yaricap;
            if (dx == 0 && dy == 0 && dz == 0) {
                continue;
            }
            BlockPos hedef = konum.offset(dx, dy, dz);
            if (veri.hucreler.containsKey(hedef.asLong()) || !level.isLoaded(hedef)) {
                continue;
            }
            BlockState durum = level.getBlockState(hedef);
            if (durum.isAir() || durum.is(YanginEtiketleri.YANMAZ)) {
                continue;
            }
            double nem = level.isRainingAt(hedef.above()) ? 1.0 : 0.0;
            double olasilik = YanginMantigi.tutusmaOlasiligi(durum.getFireSpreadSpeed(level, hedef, Direction.UP), hiz,
                    YanginMantigi.ruzgarKatsayisi(ruzgar[0], ruzgar[1], dx, dz), YanginMantigi.egimKatsayisi(dy),
                    YanginMantigi.nemKatsayisi(nem));
            if (r.nextDouble() < olasilik) {
                hucreEkle(level, veri, hedef, h.kaynak, h.yogunluk);
            }
        }
        int enUzak = YanginMantigi.sicramaMesafesi(h.yogunluk);
        if (enUzak >= 2 && r.nextDouble() < SICRAMA_OLASILIGI) {
            sicra(level, veri, h, konum, enUzak, hiz, ruzgar);
        }
    }

    /** Rüzgâr altına sıçrayan kıvılcım: iniş sütununun en üst bloğu yanıcıysa rüzgârsız olasılıkla tutuşur. */
    private static void sicra(ServerLevel level, YanginVerisi veri, YanginHucresi h, BlockPos konum, int enUzak, double hiz,
            double[] ruzgar) {
        RandomSource r = veri.rastgele;
        int[] kayma = YanginMantigi.sicramaKaymasi(ruzgar[0], ruzgar[1], (r.nextDouble() * 2 - 1) * SICRAMA_SAPMASI,
                2 + r.nextInt(enUzak - 1));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(konum.getX() + kayma[0], konum.getY() + 3,
                konum.getZ() + kayma[1]);
        if (!level.isLoaded(p)) {
            return;
        }
        for (int i = 0; i < 10; i++, p.move(Direction.DOWN)) {
            BlockState durum = level.getBlockState(p);
            if (durum.isAir() || durum.is(YanginKayitlari.AFET_ATESI.get())) {
                continue;
            }
            double nem = level.isRainingAt(p.above()) ? 1.0 : 0.0;
            if (!veri.hucreler.containsKey(p.asLong()) && r.nextDouble() < YanginMantigi.tutusmaOlasiligi(
                    durum.getFireSpreadSpeed(level, p, Direction.UP), hiz, 1, 1, YanginMantigi.nemKatsayisi(nem))) {
                hucreEkle(level, veri, p.immutable(), h.kaynak, h.yogunluk);
            }
            return;
        }
    }

    private static void yakitiTuket(ServerLevel level, YanginVerisi veri, YanginHucresi h, BlockPos konum,
            BlockState durum, boolean hasar, long simdi) {
        kaldir(level, veri, h);
        if (YanginMantigi.yanmaSonucu(hasar) == YanginMantigi.YanmaSonucu.HAVA) {
            durum.onCaughtFire(level, konum, Direction.UP, null);
            level.setBlock(konum, Blocks.AIR.defaultBlockState(), 3);
        } else {
            veri.yanmislar.put(h.konum, simdi + YANMIS_HAFIZA_TICK);
        }
    }

    private static void varlikHasari(ServerLevel level, BlockPos ust) {
        for (LivingEntity varlik : level.getEntitiesOfClass(LivingEntity.class, new AABB(ust))) {
            if (!varlik.fireImmune()) {
                varlik.setSecondsOnFire(3);
                varlik.hurt(level.damageSources().inFire(), 1.0F);
            }
        }
    }

    private static boolean hucreEkle(ServerLevel level, YanginVerisi veri, BlockPos konum, YanginKaynagi kaynak,
            double yogunluk) {
        long anahtar = konum.asLong();
        if (veri.hucreler.containsKey(anahtar) || veri.yanmislar.containsKey(anahtar) || !level.isLoaded(konum)) {
            return false;
        }
        BlockState durum = level.getBlockState(konum);
        if (!yanabilir(durum, level, konum) || YanginKancalari.sanalSu().suAltinda(level, konum)) {
            return false;
        }
        long simdi = level.getGameTime();
        YanginHucresi h = new YanginHucresi(anahtar, kaynak, (float) Math.max(0.0, Math.min(1.0, yogunluk)), simdi,
                simdi + YanginMantigi.yanmaSuresiTick(durum.getFlammability(level, konum, Direction.UP)));
        if (Kurallar.blokHasariAcik(level.getGameRules())) {
            atesYak(level, veri, h, konum);
        }
        veri.hucreler.put(anahtar, h);
        veri.setDirty();
        tavaniUygula(level, veri);
        return true;
    }

    private static void tavaniUygula(ServerLevel level, YanginVerisi veri) {
        int tasan = YanginMantigi.tasanSayisi(veri.hucreler.size(), YanginAyarlari.aktifTavan());
        Iterator<YanginHucresi> it = veri.hucreler.values().iterator();
        while (tasan-- > 0 && it.hasNext()) {
            atesSil(level, veri, it.next());
            it.remove();
        }
    }

    /**
     * Görünür ateşi yakıtın hava komşusuna koyar (önce üst, sonra yanlar, en son alt). Durum dayanağa göre hesaplanır
     * (zemin, yan ya da tavan alevi, Y-20); dayanağı olmayan konuma ateş konmaz.
     */
    private static void atesYak(ServerLevel level, YanginVerisi veri, YanginHucresi h, BlockPos konum) {
        AfetAtesiBlogu blok = YanginKayitlari.AFET_ATESI.get();
        for (Direction yon : ATES_YONLERI) {
            BlockPos kom = konum.relative(yon);
            if (!havaMi(level, kom)) {
                continue;
            }
            BlockState ates = blok.dayanagaGore(level, kom);
            if (!ates.canSurvive(level, kom)) {
                continue;
            }
            level.setBlock(kom, ates, 3);
            veri.bekleyenSilmeler.remove(kom.asLong());
            h.atesKonumu = kom.asLong();
            h.atesVar = true;
            return;
        }
    }

    private static final Direction[] ATES_YONLERI = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.DOWN};

    /** Hücrenin ateş bloğunu kaldırır; konum yüklü değilse bekleyen silmeye yazar (Y-19), chunk zorla yüklenmez. */
    private static void atesSil(ServerLevel level, YanginVerisi veri, YanginHucresi h) {
        if (!h.atesVar) {
            return;
        }
        h.atesVar = false;
        BlockPos p = BlockPos.of(h.atesKonumu);
        if (!yukluKaynagi.test(level, p)) {
            veri.bekleyenSilmeler.add(h.atesKonumu);
            veri.setDirty();
            return;
        }
        if (level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
            level.removeBlock(p, false);
        }
    }

    /** Yüklenmiş bekleyen silme konumlarındaki ateş bloklarını kaldırır. */
    static void bekleyenleriIsle(ServerLevel level, YanginVerisi veri) {
        int kaldirilan = 0;
        int cozulen = 0;
        LongIterator it = veri.bekleyenSilmeler.iterator();
        while (it.hasNext()) {
            BlockPos p = BlockPos.of(it.nextLong());
            if (!yukluKaynagi.test(level, p)) {
                continue;
            }
            if (level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
                level.removeBlock(p, false);
                kaldirilan++;
            }
            it.remove();
            cozulen++;
        }
        if (cozulen > 0) {
            veri.setDirty();
            DogalAfetler.LOGGER.info("yangın bekleyen silme: {} ateş bloğu kaldırıldı, {} konum zaten boş, {} bekliyor ({})",
                    kaldirilan, cozulen - kaldirilan, veri.bekleyenSilmeler.size(), level.dimension().location());
        }
    }

    /** Ateş bloğu yerinde mi; o konum yüklü değilse yerinde sayılır (chunk zorla yüklenmez). */
    private static boolean atesYerinde(ServerLevel level, YanginHucresi h) {
        BlockPos p = BlockPos.of(h.atesKonumu);
        return !level.isLoaded(p) || level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get());
    }

    private static boolean havaMi(ServerLevel level, BlockPos p) {
        return level.isLoaded(p) && level.getBlockState(p).isAir();
    }

    /**
     * Ateş konumundaki yağış (Y-08): konum ya da yatay komşusu yağmur görüyorsa açık (vanilla {@code isNearRain}); yağan
     * biyomda üstündeki katı örtü (yaprak hariç, {@code MOTION_BLOCKING_NO_LEAVES}) en çok 8 bloksa korunaklı; daha
     * derindeki ateş yağmurdan etkilenmez. Yüklü olmayan komşu okunmaz.
     */
    private static YagisDurumu yagis(ServerLevel level, BlockPos p) {
        if (!level.isRaining() || !level.isLoaded(p)) {
            return YagisDurumu.YOK;
        }
        boolean goruyor = level.isRainingAt(p);
        for (Direction yon : Direction.Plane.HORIZONTAL) {
            BlockPos kom = p.relative(yon);
            if (goruyor) {
                break;
            }
            goruyor = level.isLoaded(kom) && level.isRainingAt(kom);
        }
        boolean yagmurlu = level.getBiome(p).value().getPrecipitationAt(p) == Biome.Precipitation.RAIN;
        int ortu = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()) - p.getY();
        return YanginMantigi.yagisDurumu(true, goruyor, yagmurlu, ortu);
    }

    private static void kaldir(ServerLevel level, YanginVerisi veri, YanginHucresi h) {
        atesSil(level, veri, h);
        veri.hucreler.remove(h.konum);
        veri.setDirty();
    }

    /** {@code secici}'ye uyan hücreleri söndürür; yüklü olmayan ateş blokları bekleyen silmeye girer. */
    private static int sondur(ServerLevel level, YanginVerisi veri, Predicate<YanginHucresi> secici, String neden) {
        int bekleyenOnce = veri.bekleyenSilmeler.size();
        int sayi = 0;
        Iterator<YanginHucresi> it = veri.hucreler.values().iterator();
        while (it.hasNext()) {
            YanginHucresi h = it.next();
            if (secici.test(h)) {
                atesSil(level, veri, h);
                it.remove();
                sayi++;
            }
        }
        if (sayi > 0) {
            veri.setDirty();
        }
        int yeniBekleyen = veri.bekleyenSilmeler.size() - bekleyenOnce;
        if (yeniBekleyen > 0) {
            DogalAfetler.LOGGER.info("yangın {}: {} hücre söndü; {} ateş bloğu yüklü olmayan chunk'ta, chunk yüklenince "
                    + "kaldırılacak (bekleyen silme {}, {})", neden, sayi, yeniBekleyen, veri.bekleyenSilmeler.size(),
                    level.dimension().location());
        }
        return sayi;
    }

    // ---------------------------------------------------------------- alan tutuşturma (Y-21)

    /** Birkaç tick'e yayılabilen alan tutuşturma işi. */
    static final class AlanIsi {
        final BlockPos merkez;
        final YanginKaynagi kaynak;
        final double yogunluk;
        final KureTarayici tarayici;
        int tutusan;

        AlanIsi(BlockPos merkez, int yaricap, YanginKaynagi kaynak, double yogunluk) {
            this.merkez = merkez;
            this.kaynak = kaynak;
            this.yogunluk = yogunluk;
            this.tarayici = new KureTarayici(yaricap);
        }
    }

    /** Sıradaki alan işlerini bu tick'in kalan okuma bütçesiyle (hücre bütçesi × 64) ilerletir. */
    private static void alanIsleriniIsle(ServerLevel level, YanginVerisi veri) {
        long simdi = level.getGameTime();
        if (veri.alanButcesiTick != simdi) {
            veri.alanButcesiTick = simdi;
            veri.alanButcesi = YanginMantigi.alanOkumaButcesi(etkinButce());
        }
        BlockPos.MutableBlockPos kom = new BlockPos.MutableBlockPos();
        while (veri.alanButcesi > 0 && !veri.alanIsleri.isEmpty()) {
            AlanIsi is = veri.alanIsleri.peek();
            veri.alanButcesi -= is.tarayici.ilerle(veri.alanButcesi,
                    (dx, dy, dz) -> alanKonumu(level, veri, is, kom, dx, dy, dz));
            if (!is.tarayici.bitti()) {
                return;
            }
            veri.alanIsleri.poll();
        }
    }

    /** Alandaki bir konum: önce kendisi yanıcı mı (tek okuma), sonra havaya değen yüzü var mı. Harcanan okumayı döndürür. */
    private static int alanKonumu(ServerLevel level, YanginVerisi veri, AlanIsi is, BlockPos.MutableBlockPos kom, int dx,
            int dy, int dz) {
        if (veri.rastgele.nextDouble() >= YanginMantigi.alanOrani(is.yogunluk)) {
            return 1;
        }
        BlockPos p = is.merkez.offset(dx, dy, dz);
        if (!level.isLoaded(p) || !yanabilir(level.getBlockState(p), level, p)) {
            return 1;
        }
        int okuma = 1;
        for (Direction yon : Direction.values()) {
            okuma++;
            if (havaMi(level, kom.setWithOffset(p, yon))) {
                if (hucreEkle(level, veri, p, is.kaynak, is.yogunluk)) {
                    is.tutusan++;
                }
                break;
            }
        }
        return okuma;
    }

    // ---------------------------------------------------------------- ısı ve istemci özeti

    private static void ozetGonder(ServerLevel level, YanginVerisi veri, long simdi) {
        Map<Long, Toplayici> chunklar = new HashMap<>();
        for (YanginHucresi h : veri.hucreler.values()) {
            int x = BlockPos.getX(h.konum);
            int y = BlockPos.getY(h.konum);
            int z = BlockPos.getZ(h.konum);
            chunklar.computeIfAbsent(chunkAnahtari(x >> 4, z >> 4), k -> new Toplayici(x >> 4, z >> 4)).ekle(x, y, z);
        }
        if (simdi % ISI_ARALIGI == 0) {
            isiBildir(level, chunklar.values());
        }
        long[] buharlar = veri.buharlar.stream().mapToLong(Long::longValue).toArray();
        veri.buharlar.clear();
        for (ServerPlayer oyuncu : level.players()) {
            List<AtesDurumuPaketi.Ozet> ozetler = new ArrayList<>();
            for (Toplayici t : chunklar.values()) {
                double mesafeKare = yatayMesafeKare(oyuncu, t.ortX(), t.ortZ());
                if (mesafeKare <= OZET_MENZILI * OZET_MENZILI) {
                    ozetler.add(t.ozet(mesafeKare <= HUCRE_MENZILI * HUCRE_MENZILI));
                }
            }
            long[] yakinBuhar = Arrays.stream(buharlar).filter(b -> yatayMesafeKare(oyuncu,
                    BlockPos.getX(b), BlockPos.getZ(b)) <= HUCRE_MENZILI * HUCRE_MENZILI).toArray();
            boolean bos = ozetler.isEmpty() && yakinBuhar.length == 0;
            if (bos && !veri.paketAlanlar.remove(oyuncu.getUUID())) {
                continue;
            }
            if (!bos) {
                veri.paketAlanlar.add(oyuncu.getUUID());
            }
            AgKanali.oyuncuyaGonder(oyuncu, new AtesDurumuPaketi(List.copyOf(ozetler), yakinBuhar));
        }
        if (veri.hucreler.isEmpty()) {
            veri.paketAlanlar.clear();
        }
    }

    private static void isiBildir(ServerLevel level, Iterable<Toplayici> chunklar) {
        IsiKaynagiBildirici bildirici = YanginKancalari.isiKaynagi();
        if (bildirici == IsiKaynagiBildirici.BOS) {
            return;
        }
        for (Toplayici t : chunklar) {
            boolean yakin = false;
            for (ServerPlayer oyuncu : level.players()) {
                if (yatayMesafeKare(oyuncu, t.ortX(), t.ortZ()) <= ISI_MENZILI * ISI_MENZILI) {
                    yakin = true;
                    break;
                }
            }
            if (yakin) {
                double kok = Math.sqrt(t.sayi);
                bildirici.bildir(level, new Vec3(t.ortX(), t.ortY(), t.ortZ()), Math.min(18.0, 4.0 + 2.0 * kok),
                        6.0 + kok, ISI_ARALIGI * 2);
            }
        }
    }

    private static final class Toplayici {
        final int cx;
        final int cz;
        int sayi;
        double topX;
        double topY;
        double topZ;
        int ustY = Integer.MIN_VALUE;
        final int[] hucreler = new int[OZET_HUCRE_TAVANI];
        int hucreSayisi;

        Toplayici(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }

        void ekle(int x, int y, int z) {
            sayi++;
            topX += x + 0.5;
            topY += y + 0.5;
            topZ += z + 0.5;
            ustY = Math.max(ustY, y);
            if (hucreSayisi < hucreler.length) {
                hucreler[hucreSayisi++] = AtesDurumuPaketi.hucre(x, y, z);
            }
        }

        double ortX() {
            return topX / sayi;
        }

        double ortY() {
            return topY / sayi;
        }

        double ortZ() {
            return topZ / sayi;
        }

        AtesDurumuPaketi.Ozet ozet(boolean hucrelerle) {
            return new AtesDurumuPaketi.Ozet(cx, cz, sayi, (float) ortX(), (float) ortY(), (float) ortZ(), ustY,
                    hucrelerle ? Arrays.copyOf(hucreler, hucreSayisi) : new int[0]);
        }
    }

    // ---------------------------------------------------------------- yardımcılar

    private static long chunkAnahtari(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static double enYakinMesafeKare(List<ServerPlayer> oyuncular, BlockPos p) {
        double en = OYUNCUSUZ_MESAFE_KARE;
        for (ServerPlayer oyuncu : oyuncular) {
            en = Math.min(en, oyuncu.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
        }
        return en;
    }

    private static double yatayMesafeKare(ServerPlayer oyuncu, double x, double z) {
        double dx = oyuncu.getX() - x;
        double dz = oyuncu.getZ() - z;
        return dx * dx + dz * dz;
    }

    private static boolean icinde(AABB bolge, long konum) {
        return bolge.contains(BlockPos.getX(konum) + 0.5, BlockPos.getY(konum) + 0.5, BlockPos.getZ(konum) + 0.5);
    }
}
