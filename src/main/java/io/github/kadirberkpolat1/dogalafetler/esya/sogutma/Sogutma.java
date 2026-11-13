package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.IsiKaynaklari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Soğutucu eşyaların tek kayıt noktası ({@code Balikcilik.kaydet} çağırır): Soğutmalı zırh eki ({@code sogutmali}, Termostatlı'nın
 * aynası), eşyalar, config ve datagen katkıları.
 *
 * <p>Fizik: Termostat vucudu nötre çeker; soğutma ise ortamı serin bir hedefe (config {@code sogutma.hedefC}) çeker. Serin
 * Kolye (envanterde) ve Soğutmalı zırh parçaları {@link IsiKaynaklari} üzerinden "hedefe çeken soğutucu" olarak kaydedilir:
 * her ölçümde (sicaklık güncellemesinde) oyuncunun ekipmanından hesaplanır, ortam hedefin üstündeyse fark kadar (en çok
 * ekipman kapasitesi, birden fazla soğutucu taşınırsa yığılır) soğutur, hedefin altındaki ortama dokunmaz. Sonuç termometre
 * dökümünde "soğutucu eşyalar" satırı (kaynak sayısıyla) olarak görünür.
 *
 * <p>Kök neden (K-E74): eski yöntem saniyede bir oyuncunun üzerine 1,5 blok yarıçaplı, 21 tick'lik geçici kaynak bırakıyordu;
 * oyuncu yürürken ölçüm anında kaynağın yarıçapından çıkıyor, etki geçici kaynakların genel toplamına karışıp ayrı bir
 * satır olarak da görünmüyordu.
 */
public final class Sogutma {
    public static final ZirhEkleri.Ek SOGUTMALI = new ZirhEkleri.Ek("sogutmali", "tooltip.dogalafetler.sogutmali");
    public static final String PLAKA_ACIKLAMASI = "tooltip.dogalafetler.sogutma_plakasi";

    private Sogutma() {
    }

    /** Mod kurucusunda bir kez. */
    public static void kaydet(IEventBus modBus) {
        SogutmaAyarlari.kaydet();
        ZirhEkleri.kaydet(SOGUTMALI);
        SogutmaKayitlari.kaydet(modBus);
        // Ayarlar her ölçümde okunur (config sonradan yüklenir ve değişebilir).
        IsiKaynaklari.hedefliSogutucuHedefiAyarla(SogutmaAyarlari::hedefC);
        IsiKaynaklari.hedefliSogutucuEkiKaydet(SOGUTMALI, SogutmaAyarlari::plakaParcaBasinaC);
        IsiKaynaklari.hedefliSogutucuEsyaKaydet(SogutmaKayitlari.SERIN_KOLYE::get, SogutmaAyarlari::kolyeC);
        MinecraftForge.EVENT_BUS.addListener(Sogutma::oyuncuTick);
        DataGenerators.ceviriEkle(Sogutma::cevirileriYaz);
        // Lambda: sağlayıcı sınıfı yalnız datagen'de yüklenir (K-E9).
        DataGenerators.saglayiciEkle(olay -> SogutmaVerileri.ekle(olay));
    }

    /** Serin Kolye envanterdeyse (2 sn'de bir denetim) {@code kolye_serin} başarımı; tekrar verme sessizce yok sayılır. */
    private static void oyuncuTick(TickEvent.PlayerTickEvent olay) {
        if (olay.phase == TickEvent.Phase.END && olay.player instanceof ServerPlayer oyuncu
                && oyuncu.tickCount % 40 == 0
                && oyuncu.getInventory().hasAnyOf(Set.of(SogutmaKayitlari.SERIN_KOLYE.get()))) {
            OzelBasarim.ver(oyuncu, OzelBasarim.KOLYE_SERIN);
        }
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle(SOGUTMALI.ceviriAnahtari(), "Cooling", "Soğutmalı");
        y.ekle(PLAKA_ACIKLAMASI, "Craft with an armor piece: pulls hot surroundings toward a cool target",
                "Bir zırh parçasıyla birleştir: sıcak ortamı serin hedef değere doğru çeker");
        y.ekle(SerinKolye.ACIKLAMA, "Carry it in your inventory: cools hot surroundings",
                "Envanterde taşı: sıcak ortamı serinletir");
        y.ekle(SogukIksiri.ACIKLAMA, "Cools the body strongly for a while", "Vücudu bir süre güçlü serinletir");
        y.ekle("item.dogalafetler.sogutma_parcasi", "Cooling Fragment", "Soğutma Parçası");
        y.ekle("item.dogalafetler.sogutma_kulcesi", "Cooling Ingot", "Soğutma Külçesi");
        y.ekle("item.dogalafetler.sogutma_plakasi", "Cooling Plate", "Soğutma Plakası");
        y.ekle("item.dogalafetler.serin_kolye", "Cool Necklace", "Serin Kolye");
        y.ekle("item.dogalafetler.soguk_iksiri", "Cold Potion", "Soğuk İksiri");
    }
}
