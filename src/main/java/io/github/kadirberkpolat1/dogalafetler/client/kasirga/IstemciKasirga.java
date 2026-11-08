package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaKaliteButcesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * İstemcideki kasırga varlıkları ve her kasırganın yalnız görsel enkaz kopyaları (spike SK-03). Her gerçek
 * {@link UcanBlokVarligi} istemciye varınca (blok durumu senkron olunca) kalite bütçesi kadar kopya üretilir; kopyaların ağ
 * maliyeti yoktur. Yalnız render/istemci thread'i yazar ve okur.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciKasirga {
    /** Yalnız görsel enkaz: kasırga merkezine göre sarmal yörünge, sonra dışa savrulma ve düşüş. */
    record GorselEnkaz(BlockState durum, double dogum, double yaricap, double aci, double dikeyHiz, double tepe,
            float olcek, float eksenX, float eksenY, float eksenZ, float donusHizi, double disaHiz, double yukariHiz) {
    }

    static final Map<Integer, KasirgaVarligi> KASIRGALAR = new HashMap<>();
    static final Map<Integer, List<GorselEnkaz>> ENKAZ = new HashMap<>();
    private static final Random RASTGELE = new Random(20261105);
    /** Görsel otomasyonun kalite karşılaştırması için (null: istemcinin {@code efektKalitesi} ayarı). */
    static EfektKalitesi zorlananKalite;
    /** Görsel otomasyonun kasırgasız FPS ölçümü için (simülasyon sürer, bütün kasırga çizimi atlanır). */
    static boolean gizli;
    /**
     * Görsel otomasyonun K-17 kontrol karesi için (K-E49): yalnız huni, toz/etek, duvar bulutu ve kasırga sisi atlanır; uçan
     * bloklar ve görsel enkaz kopyaları çizilir, fark maskesi yalnız toz katmanını tutar.
     */
    static boolean yalnizTozGizli;

    private IstemciKasirga() {
    }

    static EfektKalitesi kalite() {
        EfektKalitesi z = zorlananKalite;
        return z != null ? z : IstemciAyarlari.efektKalitesi();
    }

    /** Kasırganın istemci zamanı (sn). */
    static double zaman(Entity varlik, float kismi) {
        return (varlik.tickCount + kismi) / 20.0;
    }

    public static int kasirgaSayisi() {
        return KASIRGALAR.size();
    }

    public static int gorselEnkazSayisi() {
        int toplam = 0;
        for (List<GorselEnkaz> liste : ENKAZ.values()) {
            toplam += liste.size();
        }
        return toplam;
    }

    @SubscribeEvent
    public static void varlikGirdi(EntityJoinLevelEvent olay) {
        if (olay.getLevel().isClientSide && olay.getEntity() instanceof KasirgaVarligi kasirga) {
            KASIRGALAR.put(kasirga.getId(), kasirga);
        }
    }

    @SubscribeEvent
    public static void varlikCikti(EntityLeaveLevelEvent olay) {
        if (olay.getLevel().isClientSide && olay.getEntity() instanceof KasirgaVarligi kasirga) {
            KASIRGALAR.remove(kasirga.getId());
            ENKAZ.remove(kasirga.getId());
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        KASIRGALAR.clear();
        ENKAZ.clear();
    }

    /** {@link UcanBlokVarligi} verisi istemciye ulaşınca (ortak kodun kancası üzerinden) çağrılır. */
    static void ucanBlokGeldi(UcanBlokVarligi blok) {
        KasirgaVarligi kasirga = KASIRGALAR.get(blok.kasirgaKimligi());
        if (kasirga == null) {
            return;
        }
        Vec3 kamera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double mesafe = Math.hypot(kamera.x - kasirga.getX(), kamera.z - kasirga.getZ());
        int sayi = KasirgaKaliteButcesi.icin(kalite()).gorselEnkazSayisi(mesafe, KasirgaAyarlari.lod());
        int tavan = KasirgaAyarlari.gorselKopyaTavani();
        KasirgaSeviyesi s = kasirga.seviyeBilgisi();
        List<GorselEnkaz> liste = ENKAZ.computeIfAbsent(kasirga.getId(), k -> new ArrayList<>());
        double simdi = zaman(kasirga, 0);
        for (int i = 0; i < sayi && liste.size() < tavan; i++) {
            float ex = RASTGELE.nextFloat() - 0.5f;
            float ey = RASTGELE.nextFloat() - 0.5f;
            float ez = RASTGELE.nextFloat() - 0.5f;
            float uzunluk = (float) Math.sqrt(ex * ex + ey * ey + ez * ez) + 1e-4f;
            liste.add(new GorselEnkaz(blok.blok(), simdi - RASTGELE.nextDouble() * 1.5,
                    s.tabanYaricapi() * (0.5 + RASTGELE.nextDouble() * 0.9), RASTGELE.nextDouble() * Math.PI * 2,
                    3 + RASTGELE.nextDouble() * 4, s.ucusYuksekligi(RASTGELE.nextDouble()),
                    0.25f + RASTGELE.nextFloat() * 0.25f, ex / uzunluk, ey / uzunluk, ez / uzunluk,
                    90 + RASTGELE.nextFloat() * 360, 2 + RASTGELE.nextDouble() * 4, 2 + RASTGELE.nextDouble() * 5));
        }
    }
}
