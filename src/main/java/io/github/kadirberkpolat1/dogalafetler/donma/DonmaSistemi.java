package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.donma.veri.DonmaVeriSaglayicilari;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Donma sisteminin kayıt noktası (madde 9, N9, N10): config, bloklar, chunk capability'si, veri paketi yükleyicisi, kuyruk,
 * dönem süreçleri ({@code donma}, {@code buharlasma}), komutlar ve datagen katkıları. Dünya dönemi olaylarını dinler:
 * kızıl cüce doğunca (pulsar yolunda "donma kalıcı" açık olsa da) çözülme dönemi açılır ve kış parametreleri dünyaya
 * sabitlenir, eski güneş dönünce NORMAL'de çözülme sürer; süpernova yeni donma döngüsünü başlatır; güneş
 * büyümesi ya da pulsar (yutulması dahil) bitince buharlaşma derinliği 0'a iner (sular geri gelir, N9); {@code sifirla}
 * yeni bir veri döngüsü açar ve bizim kar/buzumuz kuyrukla 0'a çözülür (D4). Yalnız {@code DogalAfetler} kurucusundan çağrılır.
 */
public final class DonmaSistemi {
    private static DonmaSureci donmaSureci;
    private static BuharlasmaSureci buharlasmaSureci;

    private DonmaSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        DonmaAyarlari.kaydet();
        DonmaBloklari.kaydet(modBus);
        DonmaParcaciklari.kaydet(modBus);
        modBus.addListener(ChunkDonmaVerisi::kaydet);
        IEventBus forge = MinecraftForge.EVENT_BUS;
        forge.addGenericListener(LevelChunk.class, ChunkDonmaVerisi::ekle);
        forge.addListener((AddReloadListenerEvent olay) -> olay.addListener(new DonmaTablosuYukleyici()));
        forge.addListener(DonmaZamanlayici::sunucuBasliyor);
        forge.addListener(DonmaZamanlayici::sunucuBasladi);
        forge.addListener(DonmaZamanlayici::chunkYuklendi);
        forge.addListener(DonmaZamanlayici::chunkBosaltildi);
        forge.addListener(DonmaZamanlayici::tick);
        forge.addListener(DonmaSistemi::sunucuTick);
        forge.addListener(DonmaSistemi::donemDegisti);
        donmaSureci = new DonmaSureci();
        buharlasmaSureci = new BuharlasmaSureci();
        AfetYoneticisi.donemSureciKaydet(donmaSureci);
        AfetYoneticisi.donemSureciKaydet(buharlasmaSureci);
        KomutKaydi.ekle(baglam -> DonmaKomutlari.donma());
        KomutKaydi.ekle(baglam -> DonmaKomutlari.buharlasma());
        DataGenerators.ceviriEkle(DonmaCevirileri::yaz);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.DIRT, DonmaBloklari.KIRAGILI_CIMEN);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.ANIMALS_SPAWNABLE_ON, DonmaBloklari.KIRAGILI_CIMEN);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.MINEABLE_WITH_SHOVEL, DonmaBloklari.KIRAGILI_CIMEN);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.ICE, DonmaBloklari.INCE_BUZ);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.MINEABLE_WITH_PICKAXE, DonmaBloklari.INCE_BUZ);
        // Lambda: sağlayıcı sınıfı (istemci model üreticilerine bağlı) yalnız datagen'de yüklenir.
        DataGenerators.saglayiciEkle(olay -> DonmaVeriSaglayicilari.ekle(olay));
    }

    private static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase == TickEvent.Phase.END) {
            buharlasmaSureci.tick(olay.getServer());
            donmaSureci.tick(olay.getServer());
        }
    }

    private static void donemDegisti(DonemDegistiEvent olay) {
        DunyaDonemi d = olay.donem();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || DunyaDonemi.al(server) != d) {
            return;
        }
        switch (olay.alan()) {
            case GUNES_EVRESI -> {
                GunesEvresi evre = d.gunesEvresi();
                if (evre == GunesEvresi.SUPERNOVA || evre == GunesEvresi.PULSAR || evre == GunesEvresi.KARADELIK_YUTMA) {
                    d.cozulmeDonemiAyarla(false);
                } else if (evre == GunesEvresi.KIZIL_CUCE && DonmaSureci.kaliciDegil(d)) {
                    d.cozulmeDonemiAyarla(true);
                }
                // Yutma 90 sn sürer: buharlaşan sular pulsar yolundaki güneşsiz dönemde geri gelir.
                if (evre != GunesEvresi.BUYUYEN && evre != GunesEvresi.PULSAR && evre != GunesEvresi.KARADELIK_YUTMA
                        && d.buharlasmaDerinligi() > 0f) {
                    d.buharlasmaDerinligiAyarla(0f);
                }
            }
            case COZULME -> {
                if (d.cozulmeDonemi()) {
                    KisBolgesi.sabitle(d);
                }
            }
            case SIFIRLAMA -> {
                d.ekVeri(KisBolgesi.ALT_SISTEM).putLong("dongu", System.currentTimeMillis());
                d.ekVeriDegisti(KisBolgesi.ALT_SISTEM);
                KisBolgesi.sifirla();
                DogalAfetler.LOGGER.info("[donma] dünya dönemi sıfırlandı: yeni veri döngüsü, bizim kar ve buz kuyrukla 0'a çözülür");
            }
            default -> {
            }
        }
    }

    /** {@code donma bilgi} için sürecin durumu (sürüyor / durduruldu (neden) / sürmüyor). */
    static Component surecDurumu(AfetYoneticisi y, ServerLevel ust) {
        Optional<DonemSureci.DurdurmaNedeni> neden = y.durdurmaNedeni(donmaSureci);
        return !donmaSureci.aktifMi(ust) ? Component.translatable(DonmaCevirileri.SUREC_YOK)
                : neden.isPresent() ? Component.translatable(DonmaCevirileri.SUREC_DURDU,
                        neden.get().name().toLowerCase(java.util.Locale.ROOT))
                        : Component.translatable(DonmaCevirileri.SUREC_SURUYOR);
    }
}
