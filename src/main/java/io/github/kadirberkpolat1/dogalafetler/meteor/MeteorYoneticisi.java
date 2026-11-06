package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Boyut başına meteor durumu (sunucu ana thread'i): uçan meteorlar (tavan), krater işleri (ortak blok bütçesi × TPS
 * kısması), yağmurlar, magma astarının soğuması ve sıcak ödül parçaları. Kaydedilmez (M-05): yeniden açılışta uçan
 * meteor ve yağmur yoktur, kazısı süren krater yarım kalır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class MeteorYoneticisi {
    private static final int SICAK_ARALIGI = 5;

    record Soguma(BlockPos konum, long zaman, BlockState yeni) {
    }

    static final class Durum {
        final List<MeteorVarligi> meteorlar = new ArrayList<>();
        final ArrayDeque<KraterIsi> kraterler = new ArrayDeque<>();
        final List<MeteorYagmuru> yagmurlar = new ArrayList<>();
        final List<Soguma> sogumalar = new ArrayList<>();
        final List<ItemEntity> sicaklar = new ArrayList<>();
    }

    private static final Map<ResourceKey<Level>, Durum> DURUMLAR = new HashMap<>();
    private static int sonrakiYagmur = 1;

    private MeteorYoneticisi() {
    }

    private static Durum durum(ServerLevel level) {
        return DURUMLAR.computeIfAbsent(level.dimension(), k -> new Durum());
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
        durum(level).kraterler.add(is);
    }

    static int bekleyenKraterSayisi(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.kraterler.size();
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
        durum(level).sogumalar.add(new Soguma(konum.immutable(), zaman, yeni));
    }

    static void sicakEkle(ServerLevel level, ItemEntity e) {
        durum(level).sicaklar.add(e);
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
        int butce = Math.max(1, (int) Math.round(MeteorAyarlari.kraterBlokButcesi() * TpsOlcer.kismaCarpani()));
        while (butce > 0 && !d.kraterler.isEmpty()) {
            KraterIsi is = d.kraterler.peek();
            butce -= Math.max(1, is.ilerlet(level, butce));
            if (is.bitti()) {
                d.kraterler.poll();
            }
        }
        if (!d.sogumalar.isEmpty()) {
            long simdi = level.getGameTime();
            for (Iterator<Soguma> it = d.sogumalar.iterator(); it.hasNext();) {
                Soguma s = it.next();
                if (simdi < s.zaman()) {
                    continue;
                }
                it.remove();
                if (level.isLoaded(s.konum()) && level.getBlockState(s.konum()).is(Blocks.MAGMA_BLOCK)) {
                    level.setBlockAndUpdate(s.konum(), s.yeni());
                }
            }
        }
        if (!d.sicaklar.isEmpty() && level.getGameTime() % SICAK_ARALIGI == 0) {
            MeteorOdulleri.sicaklariIsle(level, d.sicaklar);
        }
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
     * astarı hemen soğumuş kayaya döner. Açılmış kraterler (kalıcı iz) ve yerdeki ödüller kalır.
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
            for (Soguma s : d.sogumalar) {
                if (level.isLoaded(s.konum()) && level.getBlockState(s.konum()).is(Blocks.MAGMA_BLOCK)) {
                    level.setBlockAndUpdate(s.konum(), s.yeni());
                    soguma++;
                }
            }
            d.sogumalar.clear();
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
