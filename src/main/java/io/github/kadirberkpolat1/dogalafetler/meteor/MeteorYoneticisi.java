package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Boyut başına meteor durumu (sunucu ana thread'i): uçan meteorlar (tavan), krater işleri (ortak blok bütçesi × TPS
 * kısması), yağmurlar, magma astarının soğuması ve sıcak ödül parçaları. Krater işleri ve soğumalar {@link MeteorVerisi}'nde
 * kalıcıdır ve chunk yüklü değilken bekler (M-25); uçan meteor ve yağmur kaydedilmez (M-05). Sıcak parça takibi
 * eşyanın NBT'sinden kurulur: dünyaya giren (yeniden açılış, chunk yüklenmesi dahil) etiketli eşya listeye alınır (M-26).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class MeteorYoneticisi {
    private static final int SICAK_ARALIGI = 5;
    private static final int SOGUMA_ARALIGI = 10;

    static final class Durum {
        final MeteorVerisi veri;
        final List<MeteorVarligi> meteorlar = new ArrayList<>();
        final List<MeteorYagmuru> yagmurlar = new ArrayList<>();
        final List<ItemEntity> sicaklar = new ArrayList<>();

        Durum(MeteorVerisi veri) {
            this.veri = veri;
        }
    }

    private static final Map<ResourceKey<Level>, Durum> DURUMLAR = new HashMap<>();
    private static int sonrakiYagmur = 1;
    private static BiPredicate<ServerLevel, BlockPos> yukluKaynagi = ServerLevel::isLoaded;

    private MeteorYoneticisi() {
    }

    private static Durum durum(ServerLevel level) {
        return DURUMLAR.computeIfAbsent(level.dimension(), k -> new Durum(level.getDataStorage()
                .computeIfAbsent(MeteorVerisi::yukle, MeteorVerisi::new, MeteorVerisi.AD)));
    }

    /** Krater ve soğuma yazabilir mi (chunk yüklü). */
    static boolean yuklu(ServerLevel level, BlockPos konum) {
        return yukluKaynagi.test(level, konum);
    }

    /** Test: "chunk yüklü değil" durumunu taklit eder; öncekini döndürür (geri koyun). */
    static BiPredicate<ServerLevel, BlockPos> yukluKaynagiAyarla(BiPredicate<ServerLevel, BlockPos> kaynak) {
        BiPredicate<ServerLevel, BlockPos> onceki = yukluKaynagi;
        yukluKaynagi = kaynak;
        return onceki;
    }

    static MeteorVerisi veri(ServerLevel level) {
        return durum(level).veri;
    }

    static void meteorEkle(ServerLevel level, MeteorVarligi m) {
        durum(level).meteorlar.add(m);
    }

    static int aktifMeteorSayisi(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        if (d == null) {
            return 0;
        }
        d.meteorlar.removeIf(MeteorVarligi::isRemoved);
        return d.meteorlar.size();
    }

    static void kraterEkle(ServerLevel level, KraterIsi is) {
        MeteorVerisi veri = durum(level).veri;
        veri.kraterler.add(is);
        veri.setDirty();
    }

    static int bekleyenKraterSayisi(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.veri.kraterler.size();
    }

    /** Merkezi {@code alan} içindeki bekleyen krater işleri (GameTest: kalıcı dünyada yalnız kendi sahnesi, K-C24). */
    static int bekleyenKraterSayisi(ServerLevel level, AABB alan) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0
                : (int) d.veri.kraterler.stream().filter(k -> alan.contains(Vec3.atCenterOf(k.merkez()))).count();
    }

    /** Merkezi {@code alan} içindeki krater işlerini atar (GameTest sahne hazırlığı: önceki koşudan kalan iş). */
    static void kraterleriUnut(ServerLevel level, AABB alan) {
        MeteorVerisi veri = durum(level).veri;
        if (veri.kraterler.removeIf(k -> alan.contains(Vec3.atCenterOf(k.merkez())))) {
            veri.setDirty();
        }
    }

    /** Bekleyen krater işlerinden sıradaki hücresinin chunk'ı yüklü olmadığı için duranlar. */
    static int bekleyenChunkKraterSayisi(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : (int) d.veri.kraterler.stream().filter(KraterIsi::bekliyor).count();
    }

    static int yagmurEkle(ServerLevel level, IntFunction<MeteorYagmuru> kurucu) {
        MeteorYagmuru y = kurucu.apply(sonrakiYagmur++);
        durum(level).yagmurlar.add(y);
        return y.kimlik;
    }

    static boolean yagmurDurdur(ServerLevel level, int kimlik) {
        Durum d = DURUMLAR.get(level.dimension());
        return d != null && d.yagmurlar.removeIf(y -> y.kimlik == kimlik);
    }

    static int yagmurSayisi(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.yagmurlar.size();
    }

    /** Kaynağın (null: hepsinin) yağmurlarını durdurur; {@code ucanlariAt} ise uçan meteorlarını da kaldırır. */
    static int durdur(MinecraftServer server, YanginKaynagi kaynak, boolean ucanlariAt) {
        int sayi = 0;
        for (Map.Entry<ResourceKey<Level>, Durum> g : DURUMLAR.entrySet()) {
            Durum d = g.getValue();
            for (Iterator<MeteorYagmuru> it = d.yagmurlar.iterator(); it.hasNext();) {
                MeteorYagmuru y = it.next();
                if (kaynak == null || y.kaynak.equals(kaynak)) {
                    it.remove();
                    sayi++;
                }
            }
            if (ucanlariAt) {
                for (MeteorVarligi m : List.copyOf(d.meteorlar)) {
                    if (!m.isRemoved() && (kaynak == null || kaynak.equals(m.kaynak()))) {
                        m.discard();
                    }
                }
                d.meteorlar.removeIf(MeteorVarligi::isRemoved);
            }
        }
        return sayi;
    }

    static void sogumaEkle(ServerLevel level, BlockPos konum, long zaman, BlockState yeni) {
        MeteorVerisi veri = durum(level).veri;
        veri.sogumalar.add(new MeteorVerisi.Soguma(konum.immutable(), zaman, yeni));
        veri.setDirty();
    }

    static int bekleyenSogumaSayisi(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.veri.sogumalar.size();
    }

    /** Sıcak parça dünyaya girdi (bırakılış, yeniden açılış ya da chunk yüklenmesi): NBT'den takibe alınır ya da soğutulur. */
    @SubscribeEvent
    public static void varlikGirdi(EntityJoinLevelEvent olay) {
        if (!(olay.getLevel() instanceof ServerLevel level) || !(olay.getEntity() instanceof ItemEntity e)) {
            return;
        }
        long bitis = MeteorOdulleri.sicakBitis(e.getItem());
        if (bitis == OdulZamani.SOGUK) {
            return;
        }
        if (!OdulZamani.sicakMi(level.getGameTime(), bitis)) {
            ItemStack kopya = e.getItem().copy();
            MeteorOdulleri.sogut(kopya);
            e.setItem(kopya);
            return;
        }
        List<ItemEntity> sicaklar = durum(level).sicaklar;
        if (!sicaklar.contains(e)) {
            sicaklar.add(e);
        }
    }

    static boolean sicakTakipte(ServerLevel level, ItemEntity e) {
        Durum d = DURUMLAR.get(level.dimension());
        return d != null && d.sicaklar.contains(e);
    }

    /** Test: yeniden açılışı taklit eder (bellekteki sıcak parça listesi boş). */
    static void sicakTakibiniUnut(ServerLevel level) {
        durum(level).sicaklar.clear();
    }

    /** Kayıtlı krater işleri olan boyut açılınca hemen yüklenir (işler beklemeden sürsün). */
    @SubscribeEvent
    public static void dunyaYuklendi(LevelEvent.Load olay) {
        if (olay.getLevel() instanceof ServerLevel level) {
            MeteorVerisi veri = durum(level).veri;
            if (!veri.kraterler.isEmpty() || !veri.sogumalar.isEmpty()) {
                DogalAfetler.LOGGER.info("meteor kaydı ({}): {} bekleyen krater işi, {} magma soğuması",
                        level.dimension().location(), veri.kraterler.size(), veri.sogumalar.size());
            }
        }
    }

    @SubscribeEvent
    public static void dunyaTick(TickEvent.LevelTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !(olay.level instanceof ServerLevel level)) {
            return;
        }
        Durum d = DURUMLAR.get(level.dimension());
        if (d == null) {
            return;
        }
        d.yagmurlar.removeIf(y -> !y.tick(level));
        kraterleriIlerlet(level, d.veri);
        if (!d.veri.sogumalar.isEmpty() && level.getGameTime() % SOGUMA_ARALIGI == 0) {
            sogumalariIsle(level, d.veri, false);
        }
        if (!d.sicaklar.isEmpty()) {
            MeteorOdulleri.sicaklariIsle(level, d.sicaklar, level.getGameTime() % SICAK_ARALIGI == 0);
        }
    }

    /**
     * Krater işleri sırayla, ortak bütçeyle. Bekleyen (chunk'ı yüklü olmayan) iş sırayı tıkamaz. İndeksli döngü: bir işin
     * bitiş olayı yeni krater ekleyebilir.
     */
    private static void kraterleriIlerlet(ServerLevel level, MeteorVerisi veri) {
        int butce = Math.max(1, (int) Math.round(MeteorAyarlari.kraterBlokButcesi() * TpsOlcer.kismaCarpani()));
        int i = 0;
        while (butce > 0 && i < veri.kraterler.size()) {
            KraterIsi is = veri.kraterler.get(i);
            int kullanilan = is.ilerlet(level, butce);
            butce -= kullanilan;
            if (kullanilan > 0) {
                veri.setDirty();
            }
            if (is.bitti()) {
                veri.kraterler.remove(i);
                veri.setDirty();
            } else {
                i++;
            }
        }
    }

    /** Zamanı gelmiş (ya da {@code hepsi}) soğumalar: chunk yüklüyse magma kayaya döner, değilse kayıt bekler. */
    private static int sogumalariIsle(ServerLevel level, MeteorVerisi veri, boolean hepsi) {
        long simdi = level.getGameTime();
        int soguyan = 0;
        for (Iterator<MeteorVerisi.Soguma> it = veri.sogumalar.iterator(); it.hasNext();) {
            MeteorVerisi.Soguma s = it.next();
            if (!hepsi && simdi < s.zaman() || !yuklu(level, s.konum())) {
                continue;
            }
            it.remove();
            veri.setDirty();
            if (level.getBlockState(s.konum()).is(Blocks.MAGMA_BLOCK)) {
                level.setBlockAndUpdate(s.konum(), s.yeni());
                soguyan++;
            }
        }
        return soguyan;
    }

    /**
     * Afet bittiğinde o afetin (kaynak = tür + başlangıç saati) yağmurları durur; temiz bitişte ({@code KOMUT}, {@code
     * IPTAL}) uçan meteorları da kalkar, doğal bitişte düşmeyi tamamlar.
     */
    @SubscribeEvent
    public static void afetBitti(AfetBittiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        YanginKaynagi kaynak = YanginKaynagi.afet(olay.tur(), olay.baslangicSaati());
        int sayi = durdur(server, kaynak, YanginMantigi.temizBitis(olay.neden()));
        if (sayi > 0) {
            DogalAfetler.LOGGER.info("meteor: {} afeti bitti ({}), {} yağmur durdu", olay.tur(), olay.neden(), sayi);
        }
    }

    /**
     * {@code /dogalafet sifirla} (K-E11): bütün yağmurlar durur, uçan meteorlar kalkar, sıcak ödül parçaları soğur ve magma
     * astarı hemen soğumuş kayaya döner (yüklü olmayan chunk'takiler yüklenince). Açılmış ve süren kraterler (kalıcı iz)
     * ve yerdeki ödüller kalır.
     */
    @SubscribeEvent
    public static void donemDegisti(DonemDegistiEvent olay) {
        if (olay.alan() != DonemAlani.SIFIRLAMA) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        int yagmur = durdur(server, null, true);
        int sicak = 0;
        int soguma = 0;
        for (ServerLevel level : server.getAllLevels()) {
            Durum d = DURUMLAR.get(level.dimension());
            if (d == null) {
                continue;
            }
            for (ItemEntity e : d.sicaklar) {
                if (e.isAlive() && MeteorOdulleri.sicakBitis(e.getItem()) != OdulZamani.SOGUK) {
                    ItemStack kopya = e.getItem().copy();
                    MeteorOdulleri.sogut(kopya);
                    e.setItem(kopya);
                    sicak++;
                }
            }
            d.sicaklar.clear();
            soguma += sogumalariIsle(level, d.veri, true);
            d.veri.sogumalar.replaceAll(s -> new MeteorVerisi.Soguma(s.konum(), 0L, s.yeni()));
        }
        DogalAfetler.LOGGER.info("meteor sıfırlama: {} yağmur durdu, uçan meteorlar kalktı, {} sıcak parça ve {} magma soğudu",
                yagmur, sicak, soguma);
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        DURUMLAR.clear();
        sonrakiYagmur = 1;
    }
}
