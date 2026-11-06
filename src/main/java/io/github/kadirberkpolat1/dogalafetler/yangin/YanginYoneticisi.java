package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.OncelikSecici;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
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

    /** Meteor krateri gibi bir alanın yanıcı yüzeyini (havaya değen blokları) {@code yogunluk} olasılığıyla tutuşturur. */
    public static int alanTutustur(ServerLevel level, BlockPos merkez, int yaricap, YanginKaynagi kaynak,
            double yogunluk) {
        Objects.requireNonNull(kaynak, "kaynak");
        int r = Math.max(0, Math.min(EN_BUYUK_ALAN_YARICAPI, yaricap));
        YanginVerisi veri = veri(level);
        BlockPos.MutableBlockPos kom = new BlockPos.MutableBlockPos();
        int sayi = 0;
        for (BlockPos p : BlockPos.betweenClosed(merkez.offset(-r, -r, -r), merkez.offset(r, r, r))) {
            if (p.distSqr(merkez) > (double) r * r + 0.5 || !level.isLoaded(p)
                    || veri.rastgele.nextDouble() >= YanginMantigi.alanOrani(yogunluk)) {
                continue;
            }
            boolean yuzey = false;
            for (Direction yon : Direction.values()) {
                if (havaMi(level, kom.setWithOffset(p, yon))) {
                    yuzey = true;
                    break;
                }
            }
            if (yuzey && hucreEkle(level, veri, p.immutable(), kaynak, yogunluk)) {
                sayi++;
            }
        }
        return sayi;
    }

    /** Bölgedeki ({@code null} = boyutun tümü) ateşi iz bırakmadan söndürür: yakıt yerinde kalır, ateş bloğu kalkar. */
    public static int sondur(ServerLevel level, AABB bolge) {
        YanginVerisi veri = veri(level);
        int sayi = 0;
        Iterator<YanginHucresi> it = veri.hucreler.values().iterator();
        while (it.hasNext()) {
            YanginHucresi h = it.next();
            if (bolge == null || icinde(bolge, h.konum)) {
                atesSil(level, h);
                it.remove();
                sayi++;
            }
        }
        if (sayi > 0) {
            veri.setDirty();
        }
        return sayi;
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
     * Afet bitti: o kaynağın ateşleri artık yayılmaz ve en geç {@code afetSonrasiSonumSaniye} içinde (rastgele, yarısından
     * sonra) söner. Vanilla'ya devredilmez: vanilla ateş bütçe ve tavan dışında yayılırdı (Y-05).
     */
    public static void kaynakBitti(YanginKaynagi kaynak) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            kaynakBitti(server, kaynak);
        }
    }

    public static void kaynakBitti(MinecraftServer server, YanginKaynagi kaynak) {
        int sonum = Math.max(1, YanginAyarlari.afetSonrasiSonumSaniye() * 20);
        for (ServerLevel level : server.getAllLevels()) {
            YanginVerisi veri = VERILER.get(level.dimension());
            if (veri == null) {
                continue;
            }
            long simdi = level.getGameTime();
            for (YanginHucresi h : veri.hucreler.values()) {
                if (h.kaynak.equals(kaynak)) {
                    h.yayilabilir = false;
                    h.bitis = Math.min(h.bitis, simdi + sonum / 2 + veri.rastgele.nextInt(sonum / 2 + 1));
                    veri.setDirty();
                }
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

    @SubscribeEvent
    public static void dunyaTick(TickEvent.LevelTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !(olay.level instanceof ServerLevel level)) {
            return;
        }
        YanginVerisi veri = VERILER.get(level.dimension());
        if (veri == null) {
            return;
        }
        long simdi = level.getGameTime();
        if (simdi % 200 == 0) {
            veri.yanmislar.values().removeIf(bitis -> bitis <= simdi);
        }
        if (veri.hucreler.isEmpty()) {
            if (!veri.paketAlanlar.isEmpty() && simdi % OZET_ARALIGI == 0) {
                ozetGonder(level, veri, simdi);
            }
            return;
        }
        int butce = butceyiGuncelle(level, veri);
        isle(level, veri, simdi, butce);
        if (simdi % OZET_ARALIGI == 0) {
            ozetGonder(level, veri, simdi);
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
        java.util.Arrays.fill(veri.adaylar, 0, m, null);
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
            atesYak(level, h, konum);
        } else if (!hasar && h.atesVar) {
            atesSil(level, h);
        }
        BlockPos ust = h.atesVar ? BlockPos.of(h.atesKonumu) : konum.above();
        boolean yagmur = level.isRainingAt(ust);
        RandomSource r = veri.rastgele;
        if (yagmur && YanginAyarlari.yagmurdaSonme() && r.nextDouble() < YanginMantigi.yagmurSonmeOlasiligi(
                YanginAyarlari.yagmurSonmeOlasiligi(), h.yasOrani(simdi))) {
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
        if (h.yayilabilir) {
            yay(level, veri, h, konum, yagmur, ruzgar);
        }
    }

    private static void yay(ServerLevel level, YanginVerisi veri, YanginHucresi h, BlockPos konum, boolean yagmur,
            double[] ruzgar) {
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
            double nem = level.isRainingAt(hedef.above()) ? 1.0 : yagmur ? 0.5 : 0.0;
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
            atesYak(level, h, konum);
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
            atesSil(level, it.next());
            it.remove();
        }
    }

    /** Görünür ateşi yakıtın hava komşusuna koyar (önce üst, sonra yanlar, en son alt). */
    private static void atesYak(ServerLevel level, YanginHucresi h, BlockPos konum) {
        for (Direction yon : ATES_YONLERI) {
            BlockPos kom = konum.relative(yon);
            if (havaMi(level, kom)) {
                level.setBlock(kom, YanginKayitlari.AFET_ATESI.get().defaultBlockState(), 3);
                h.atesKonumu = kom.asLong();
                h.atesVar = true;
                return;
            }
        }
    }

    private static final Direction[] ATES_YONLERI = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.DOWN};

    private static void atesSil(ServerLevel level, YanginHucresi h) {
        if (!h.atesVar) {
            return;
        }
        h.atesVar = false;
        BlockPos p = BlockPos.of(h.atesKonumu);
        if (level.isLoaded(p) && level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
            level.removeBlock(p, false);
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

    private static void kaldir(ServerLevel level, YanginVerisi veri, YanginHucresi h) {
        atesSil(level, h);
        veri.hucreler.remove(h.konum);
        veri.setDirty();
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
            long[] yakinBuhar = java.util.Arrays.stream(buharlar).filter(b -> yatayMesafeKare(oyuncu,
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
                    hucrelerle ? java.util.Arrays.copyOf(hucreler, hucreSayisi) : new int[0]);
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
