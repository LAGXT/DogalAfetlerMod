package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Güneş patlamasından sonraki dönem sürecinin davranışsız taklidi (yalnız geliştirme sunucusu ve senaryo, K-C41): güneş
 * SUPERNOVA ya da GUNESSIZ evresindeyken aktiftir; durdurulmamışsa evre ilerlemesini {@value #EVRE_SURESI} ölçekli tick'te
 * 1'e çıkarır ve evreyi bir sonrakine geçirir (SUPERNOVA → GUNESSIZ → KIZIL_CUCE). Gökyüzü, donma ya da su yoktur. Sahip
 * deseni (sözleşme §3a): her ilerlemeden önce {@link AfetYoneticisi#surecDurdu} sorulur; döngünün başladığı SUPERNOVA
 * {@code evreBaslangicSaati} kendi ek verisinde tutulur, değişince (yeni döngü) {@link AfetYoneticisi#donemSureciBasladi}
 * çağrılır. {@link OyunTestiAfeti} güneş patlaması türüyle özellikten kaydedilince birlikte kaydedilir.
 */
public final class OyunTestiSureci implements DonemSureci {
    public static final String KIMLIK = "oyuntesti_gunes";
    static final long EVRE_SURESI = 24_000;

    private OyunTestiSureci() {
    }

    static void kaydet() {
        OyunTestiSureci surec = new OyunTestiSureci();
        AfetYoneticisi.donemSureciKaydet(surec);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, surec::tick);
        DogalAfetler.LOGGER.warn("Oyun testi dönem süreci {} kaydedildi; gerçek süreç değildir", KIMLIK);
    }

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.GUNES_PATLAMASI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        GunesEvresi evre = DunyaDonemi.al(ustDunya).gunesEvresi();
        return evre == GunesEvresi.SUPERNOVA || evre == GunesEvresi.GUNESSIZ;
    }

    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        return sonraki(donem.gunesEvresi()).map(evre -> {
            donem.gunesEvresiAyarla(evre);
            return Component.literal(KIMLIK + " -> ").append(Component.translatable(evre.ceviriAnahtari()));
        });
    }

    private void tick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        ServerLevel ust = server.overworld();
        if (!aktifMi(ust)) {
            return;
        }
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        CompoundTag veri = donem.ekVeri(KIMLIK);
        if (donem.gunesEvresi() == GunesEvresi.SUPERNOVA && veri.getLong("dongu") != donem.evreBaslangicSaati()) {
            veri.putLong("dongu", donem.evreBaslangicSaati());
            donem.ekVeriDegisti(KIMLIK);
            yonetici.donemSureciBasladi(this);
        }
        int adim = yonetici.zamanAdimi();
        if (yonetici.surecDurdu(this) || adim <= 0) {
            return;
        }
        float ilerleme = donem.evreIlerlemesi() + adim / (float) EVRE_SURESI;
        if (ilerleme >= 1f) {
            sonraki(donem.gunesEvresi()).ifPresent(donem::gunesEvresiAyarla);
        } else {
            donem.evreIlerlemesiAyarla(ilerleme);
        }
    }

    private static Optional<GunesEvresi> sonraki(GunesEvresi evre) {
        return switch (evre) {
            case SUPERNOVA -> Optional.of(GunesEvresi.GUNESSIZ);
            case GUNESSIZ -> Optional.of(GunesEvresi.KIZIL_CUCE);
            default -> Optional.empty();
        };
    }
}
