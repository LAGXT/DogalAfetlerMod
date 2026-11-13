package io.github.kadirberkpolat1.dogalafetler.journal;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikTutulduEvent;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Afet Defteri'nin istatistik toplayıcıları (sunucu ana thread'i, oyuncu başına). Yeni kanca eklemez, mevcutları dinler:
 * {@link AfetBittiEvent} (karşılaşılan ve atlatılan afetler), {@link BalikTutulduEvent} (buz deliği çekmeleri),
 * {@link ItemFishedEvent} (tutulan eşyalar), {@link LivingDeathEvent} (yaşam ve afet ölümleri), eşya alma (meteor
 * parçaları) ve saniyede bir vücut sıcaklığı örneği. Mesafe, ölüm ve olta tutuş sayıları vanilla istatistiklerinden
 * açılışta okunur; sürekli yayın ve ek tick işi yoktur (sıcaklık örneği oyuncu başına 20 tick'te bir NBT okuması).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class DefterOlaylari {
    /** Vücut sıcaklığı örnekleme aralığı (tick). */
    static final int SICAKLIK_ARALIGI = 20;
    /** Afetin etki alanı kenarından bu kadar blok dışındaki ölüm de afet ölümü sayılır. */
    static final double OLUM_PAYI = 8.0;

    private DefterOlaylari() {
    }

    /**
     * AKTIF'e girmiş, geri alınmamış, doğal ya da komutla biten afet ({@link AfetBittiEvent#basarimSayilir()}: başarım
     * sayımıyla aynı kural). Etkilenen çevrim içi her oyuncuya karşılaşma; sağ çıkanlara atlatma yazılır.
     */
    @SubscribeEvent
    public static void afetBitti(AfetBittiEvent olay) {
        if (!olay.basarimSayilir()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Set<UUID> hepsi = new HashSet<>(olay.etkilenenler());
        hepsi.addAll(olay.sagCikanlar());
        if (hepsi.isEmpty()) {
            return;
        }
        long gun = AfetYoneticisi.al(server).oyunGunu();
        long tarih = System.currentTimeMillis();
        for (UUID kimlik : hepsi) {
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
            if (oyuncu != null) {
                DefterVerisi.afetBitti(oyuncu, olay.tur(), olay.seviye(), olay.sagCikanlar().contains(kimlik), gun, tarih);
            }
        }
    }

    /** Buz deliğinden çekme (balikçılığın her çekmede bir kez yayınladığı olay): çekme ve buz balığı sayısı. */
    @SubscribeEvent
    public static void balikTutuldu(BalikTutulduEvent olay) {
        DefterVerisi.buzTutusu(olay.oyuncu(), olay.balikSayisi());
    }

    /**
     * Olta ile tutulan her yığın (buz deliği dahil, bir çekme bir kez): eşya listesi. Düşük öncelik: başka bir dinleyici
     * olayı iptal ettiyse sayılmaz.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void oltaTutuldu(ItemFishedEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            DefterVerisi.esyalarTutuldu(oyuncu, olay.getDrops());
        }
    }

    /** Ölüm: yeni yaşam başlar; süren bir afetin etki alanındaysa o afet türüne ölüm yazılır (tür başına bir kez). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void oldu(LivingDeathEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        AfetYoneticisi yonetici = AfetYoneticisi.al(oyuncu.server);
        Set<AfetTuru> yazilan = new HashSet<>();
        for (Afet afet : yonetici.aktifAfetler()) {
            if (afet.asama() == AfetAsamasi.AKTIF && !yazilan.contains(afet.tur()) && alanda(yonetici, oyuncu, afet)) {
                yazilan.add(afet.tur());
                DefterVerisi.afetteOldu(oyuncu, afet.tur());
            }
        }
        DefterVerisi.oldu(oyuncu, yonetici.oyunGunu());
    }

    private static boolean alanda(AfetYoneticisi yonetici, ServerPlayer oyuncu, Afet afet) {
        if (!afet.tur().sunucuGeneli() && !oyuncu.level().dimension().equals(afet.baglam().boyut())) {
            return false;
        }
        Optional<EtkiAlani> alan = yonetici.etkiAlani(afet);
        return alan.isPresent() && alan.get().iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), OLUM_PAYI);
    }

    /**
     * Meteorit Parçası / Kozmik Çekirdek toplama: yalnız yerden alınan, bir oyuncunun fırlatmadığı eşya (meteorun ödülü);
     * atıp yeniden alarak sayacı şişirmek mümkün olmasın.
     */
    @SubscribeEvent
    public static void esyaAlindi(PlayerEvent.ItemPickupEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        ItemStack yigin = olay.getStack();
        boolean parca = yigin.is(MeteorEsyaKayitlari.METEORIT_PARCASI.get());
        if (!parca && !yigin.is(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get())) {
            return;
        }
        ItemEntity kaynak = olay.getOriginalEntity();
        if (kaynak.getOwner() instanceof Player || yigin.getCount() <= 0) {
            return;
        }
        DefterVerisi.meteorToplandi(oyuncu, !parca, yigin.getCount());
    }

    /** Saniyede bir (oyuncu başına kaydırılmış) vücut sıcaklığı örneği: yalnız sıcaklık sistemi çalışırken. */
    @SubscribeEvent
    public static void oyuncuTick(TickEvent.PlayerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || olay.side != LogicalSide.SERVER
                || !(olay.player instanceof ServerPlayer oyuncu)
                || (oyuncu.tickCount + oyuncu.getId()) % SICAKLIK_ARALIGI != 0) {
            return;
        }
        sicaklikOrnegi(oyuncu);
    }

    /** GameTest sahte oyuncuları tick almadığı için doğrudan çağrılabilir. */
    static void sicaklikOrnegi(ServerPlayer oyuncu) {
        if (oyuncu.isAlive() && !oyuncu.isSpectator() && SicaklikKapisi.acikMi(oyuncu.server)) {
            DefterVerisi.sicaklikOrnegi(oyuncu, VucutOlcegi.santigrat(VucutSicakligi.al(oyuncu)));
        }
    }

    /** Ölümde ve boyut dönüşünde veri yeni oyuncu nesnesine taşınır (kalıcı: ölümde silinmez). */
    @SubscribeEvent
    public static void kopyalandi(PlayerEvent.Clone olay) {
        if (olay.getOriginal() instanceof ServerPlayer eski && olay.getEntity() instanceof ServerPlayer yeni) {
            DefterVerisi.kopyala(eski, yeni);
        }
    }
}
