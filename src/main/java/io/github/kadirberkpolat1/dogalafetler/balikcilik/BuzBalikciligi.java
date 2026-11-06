package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.Aktiflik;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.HareketsizlikSogumasi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.IsirmaSuresi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaApi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Buz deliğinde olta (madde 15). Vanilla {@link FishingHook} deliğin su sıvı durumunda yüzer ve balık tutar; bu sınıf
 * yalnız bekleme süresini değiştirir: olta delikteyken vanilla'nın {@code timeUntilLured} sayacı her tick bizim kalan
 * beklememize yazılır ({@link IsirmaSuresi}, Lure kısaltır), süre bitince yaklaşma evresi başlatılır; ısırma, çekme ve
 * loot vanilla yolundan gider (loot'u {@link BuzDeligiLootDegistirici} değiştirir). Normal buzda olta suya değmez, vanilla
 * zaten tutmaz. Alanları erişim dönüştürücüsü ya da mixin olmadan Forge'un SRG yansımasıyla yazılır (karar B-03).
 *
 * <p>Aynı zamanda madde 15 risk/denge: oltaşı delikteyken buz üstünde hareketsiz duran oyuncunun ortamına giderek
 * güçlenen bir soğuk kaynağı eklenir ({@link HareketsizlikSogumasi}, {@link SicaklikKancalari#ortamKaynagiEkle}).
 */
public final class BuzBalikciligi {
    private static final Field CEKME = alan("f_37090_");
    private static final Field YAKLASMA = alan("f_37091_");
    private static final Field ISIRMA = alan("f_37089_");
    private static final Field BALIK_ACISI = alan("f_37092_");
    private static final Field LURE = alan("f_37097_");
    /** Soğuk kaynağın yarıçapı (blok): tam güç oyuncunun kendisinde; bir blok ötedeki oyuncuya (1 − 1/1,5)² ≈ %11. */
    static final double KAYNAK_YARICAPI = 1.5;

    /** Bir oltanın bekleme durumu. */
    private static final class Olta {
        int bekleme;
        boolean basladi;
        boolean serbest;
    }

    /** Bir oyuncunun hareketsizlik sayacı (saniye). */
    private static final class Hareketsizlik {
        Vec3 sonKonum;
        int sayac;
        double sonGuc;
        long sonTick = -1000;
    }

    private static final Map<FishingHook, Olta> OLTALAR = new WeakHashMap<>();
    /** Bu tick'te çekilen deliğin konumu ve çeken (vanilla eşyayı aynı çağrıda, olaydan hemen sonra doğurur). */
    private static BlockPos cekilenDelik;
    private static long cekilenTick = -1;
    private static ServerPlayer cekilenOyuncu;
    private static final Map<UUID, Hareketsizlik> OYUNCULAR = new HashMap<>();

    private BuzBalikciligi() {
    }

    private static Field alan(String srg) {
        return ObfuscationReflectionHelper.findField(FishingHook.class, srg);
    }

    /** Balıkçılık bu buzda açık mı ({@link Aktiflik}; sütunun su üstü buz olması donma API'sinden). */
    public static boolean aktifMi(ServerLevel level, BlockPos konum) {
        DunyaDonemi d = DunyaDonemi.al(level);
        boolean kizilCuce = d.cozulmeDonemi() || d.gunesEvresi() == GunesEvresi.KIZIL_CUCE;
        boolean buz = DonmaApi.buzYuzeyiMi(level, konum);
        return Aktiflik.aktifMi(d.donmaIlerlemesi(), BalikcilikAyarlari.aktiflikEsigi(), kizilCuce,
                buz && kizilCuce ? DonmaApi.kisAgirligi(level, konum) : 0.0, buz);
    }

    /**
     * Olta bir buz deliğinin sütununda mı: deliğin içinde ya da ısırmada aşağı çekilip deliğin altındaki suya (en çok
     * iki blok) inmiş.
     */
    public static boolean delikteMi(FishingHook olta) {
        return delikKonumu(olta) != null;
    }

    /** Oltanın içinde ya da altında durduğu delik; yoksa {@code null}. */
    public static BlockPos delikKonumu(FishingHook olta) {
        BlockPos.MutableBlockPos p = olta.blockPosition().mutable();
        for (int i = 0; i <= 2; i++) {
            BlockState s = olta.level().getBlockState(p);
            if (s.is(BalikcilikKayitlari.BUZ_DELIGI.get())) {
                return p.immutable();
            }
            if (!s.getFluidState().is(FluidTags.WATER)) {
                return null;
            }
            p.move(0, 1, 0);
        }
        return null;
    }

    static void varlikKatildi(EntityJoinLevelEvent olay) {
        if (olay.getLevel().isClientSide) {
            return;
        }
        if (olay.getEntity() instanceof FishingHook olta) {
            OLTALAR.putIfAbsent(olta, new Olta());
        } else if (olay.getEntity() instanceof ItemEntity esya && cekilenDelik != null
                && olay.getLevel().getGameTime() == cekilenTick) {
            deliktenCikar(esya);
        }
    }

    /**
     * Vanilla çekilen eşyayı olta konumunda (deliğin suyunda) doğurur ve oyuncuya doğru fırlatır; deliğin komşuları buz
     * olduğu için eşya duvara çarpıp delikte kalırdı. Eşya deliğin üstüne alınır ve vanilla formülüyle yeniden fırlatılır.
     */
    private static void deliktenCikar(ItemEntity esya) {
        BlockPos d = cekilenDelik;
        if (Math.abs(esya.getX() - (d.getX() + 0.5)) > 1.0 || Math.abs(esya.getZ() - (d.getZ() + 0.5)) > 1.0
                || esya.getY() < d.getY() - 3 || esya.getY() > d.getY() + 1.5) {
            return;
        }
        double x = d.getX() + 0.5;
        double y = d.getY() + 1.05;
        double z = d.getZ() + 0.5;
        esya.setPos(x, y, z);
        if (cekilenOyuncu != null) {
            double dx = cekilenOyuncu.getX() - x;
            double dy = cekilenOyuncu.getY() - y;
            double dz = cekilenOyuncu.getZ() - z;
            esya.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        }
    }

    static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        cekilenDelik = null;
        cekilenOyuncu = null;
        if (OLTALAR.isEmpty()) {
            return;
        }
        MinecraftServer server = olay.getServer();
        boolean saniye = server.getTickCount() % 20 == 0;
        for (Iterator<Map.Entry<FishingHook, Olta>> it = OLTALAR.entrySet().iterator(); it.hasNext();) {
            Map.Entry<FishingHook, Olta> e = it.next();
            FishingHook olta = e.getKey();
            if (olta.isRemoved()) {
                it.remove();
                continue;
            }
            if (!delikteMi(olta)) {
                e.getValue().basladi = false;
                continue;
            }
            bekle(olta, e.getValue());
            if (saniye && olta.getPlayerOwner() instanceof ServerPlayer oyuncu) {
                sogut(oyuncu, server.getTickCount());
            }
        }
    }

    /** Bekleme durum makinesi; olta tick'inden sonra (sunucu tick'inin sonunda) çalışır. */
    private static void bekle(FishingHook olta, Olta d) {
        try {
            int isirma = ISIRMA.getInt(olta);
            int yaklasma = YAKLASMA.getInt(olta);
            if (isirma > 0 || yaklasma > 0) {
                d.serbest = true;
                return;
            }
            if (!d.basladi || d.serbest) {
                int lure = LURE.getInt(olta);
                d.bekleme = IsirmaSuresi.beklemeTick(IsirmaSuresi.toplamTick(olta.level().random.nextDouble(), lure,
                        BalikcilikAyarlari.isirma()));
                d.basladi = true;
                d.serbest = false;
            }
            d.bekleme--;
            if (d.bekleme > 0) {
                CEKME.setInt(olta, d.bekleme);
                return;
            }
            CEKME.setInt(olta, 0);
            YAKLASMA.setInt(olta, IsirmaSuresi.YAKLASMA_TICK);
            BALIK_ACISI.setFloat(olta, olta.level().random.nextFloat() * 360.0f);
            d.serbest = true;
        } catch (IllegalAccessException hata) {
            throw new IllegalStateException("FishingHook alanlarına erişilemedi", hata);
        }
    }

    /** Saniyede bir: oltaşı buz üstünde hareketsizse soğuk kaynağı (süre bir sonraki güncellemeye kadar). */
    private static void sogut(ServerPlayer oyuncu, long tick) {
        Hareketsizlik h = OYUNCULAR.computeIfAbsent(oyuncu.getUUID(), k -> new Hareketsizlik());
        Vec3 p = oyuncu.position();
        // Ara verip (olta delikte değilken) dönen balıkçının sayacı sıfırdan başlar.
        boolean surekli = tick - h.sonTick <= 40;
        h.sonTick = tick;
        boolean hareketsiz = surekli && h.sonKonum != null
                && HareketsizlikSogumasi.hareketsizMi(sq(p.x - h.sonKonum.x) + sq(p.z - h.sonKonum.z));
        h.sonKonum = p;
        h.sayac = HareketsizlikSogumasi.sonrakiSayac(h.sayac, hareketsiz && buzUstundeMi(oyuncu));
        h.sonGuc = HareketsizlikSogumasi.guc(h.sayac, BalikcilikAyarlari.sogutma());
        if (h.sonGuc < 0) {
            // Sıcaklık kaynakları gövde merkezinde (ayak + 1 blok) ölçer.
            SicaklikKancalari.ortamKaynagiEkle(oyuncu.serverLevel(), p.add(0, 1, 0), h.sonGuc, KAYNAK_YARICAPI, 21);
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Oyuncu buz (vanilla buz etiketi, donmanın ince buzu dahil) ya da buz deliği üstünde mi. */
    static boolean buzUstundeMi(Player oyuncu) {
        BlockState alt = oyuncu.level().getBlockState(oyuncu.getOnPos());
        return alt.is(BlockTags.ICE) || alt.is(BalikcilikKayitlari.BUZ_DELIGI.get());
    }

    /** Oyuncunun son hareketsizlik soğuması (°C, 0 ya da negatif; test ve bilgi için). */
    public static double sonSogutma(ServerPlayer oyuncu) {
        Hareketsizlik h = OYUNCULAR.get(oyuncu.getUUID());
        return h == null ? 0.0 : h.sonGuc;
    }

    /** Olta delikte bekliyorsa kalan bekleme (tick), değilse -1 (test için). */
    public static int kalanBekleme(FishingHook olta) {
        Olta d = OLTALAR.get(olta);
        return d == null || !d.basladi || d.serbest ? -1 : d.bekleme;
    }

    /** Vanilla {@code ItemFishedEvent} geçti: olta delikteyse {@link BalikTutulduEvent} yayınlanır. */
    static void balikTutuldu(ItemFishedEvent olay) {
        FishingHook olta = olay.getHookEntity();
        BlockPos delik = delikKonumu(olta);
        if (olay.getEntity() instanceof ServerPlayer oyuncu && delik != null) {
            cekilenDelik = delik;
            cekilenTick = olta.level().getGameTime();
            cekilenOyuncu = oyuncu;
            MinecraftForge.EVENT_BUS.post(new BalikTutulduEvent(oyuncu, olay.getDrops(), delik));
        }
    }

    static void sunucuDurdu(ServerStoppedEvent olay) {
        OLTALAR.clear();
        OYUNCULAR.clear();
        cekilenDelik = null;
        cekilenOyuncu = null;
    }
}
