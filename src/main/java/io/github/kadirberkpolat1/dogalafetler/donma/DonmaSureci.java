package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.PulsarYolu;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaIlerlemesi;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Dünya donması dönem süreci ({@code donma}, madde 9; çekirdek §3a): güneşsiz dönemde G {@code donma.donmaSuresi} boyunca
 * 100'e çıkar; kızıl cücede (çözülme dönemi açık, donma kalıcı değilse) {@code donma.cozulmeSuresi} boyunca tabana iner;
 * güneş normalken ve bağımsız çözülme açıksa 0'a iner. Pulsar zincirinde (pulsar-2) PULSAR ve KARADELİK_YUTMA donmayı
 * çözer, pulsar yolundaki kızıl cüce "donma kalıcı"yı yok sayar, eski güneş dönünce ({@link PulsarYolu#gunesDondu})
 * bağımsız çözülme kapalı olsa da 0'a iner. İlerleme {@code zamanAdimi()} ile birikir ({@code devamEt}'te iş
 * yok). Gamerule bağı güneş patlamasıdır; döngü kimliği yalnız süpernova başlangıç koşulunda karşılaştırılır
 * (K-C46). Ulaşılmış ilerleme durdurmada geri alınmaz. Ayrık aşaması yoktur: {@code asama ileri} bu süreci geçer
 * (varsayılan boş çıktı, K-D31); test aracı {@code /dogalafet donma <yüzde>}.
 */
final class DonmaSureci implements DonemSureci {
    static final String KIMLIK = "donma";
    private static final String DONGU = "surecDongusu";
    private static final float PAY = 1e-3f;

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Component ad() {
        return Component.translatable(DonmaCevirileri.SUREC_DONMA);
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.GUNES_PATLAMASI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        DunyaDonemi d = DunyaDonemi.al(ustDunya);
        float g = d.donmaIlerlemesi();
        return switch (d.gunesEvresi()) {
            case GUNESSIZ -> g < 100f - PAY;
            case KIZIL_CUCE -> d.cozulmeDonemi() && kaliciDegil(d) && g > DonmaAyarlari.cozulmeTabani() + PAY;
            case NORMAL -> (DonmaAyarlari.bagimsizCozulme() || PulsarYolu.gunesDondu(d)) && g > PAY;
            // Pulsar (gökyüzü-2) ve yutulması: donma durur ve geri çözülür; kalıcı kış ve "donma kalıcı" ayarı yok sayılır.
            case PULSAR, KARADELIK_YUTMA -> g > PAY;
            default -> false;
        };
    }

    /** Sahibinin sunucu tick'inden (END). */
    void tick(MinecraftServer server) {
        AfetYoneticisi y = AfetYoneticisi.al(server);
        DunyaDonemi d = y.donem();
        if (d.gunesEvresi() == GunesEvresi.SUPERNOVA) {
            CompoundTag veri = d.ekVeri(KisBolgesi.ALT_SISTEM);
            if (veri.getLong(DONGU) != d.evreBaslangicSaati()) {
                veri.putLong(DONGU, d.evreBaslangicSaati());
                d.ekVeriDegisti(KisBolgesi.ALT_SISTEM);
                y.donemSureciBasladi(this);
            }
        }
        if (d.gunesEvresi() == GunesEvresi.KIZIL_CUCE && !d.cozulmeDonemi() && kaliciDegil(d)) {
            // kalici doğuştan sonra kapatıldıysa çözülme dönemi doğuş olayını beklemeden açılır.
            d.cozulmeDonemiAyarla(true);
        }
        if (!aktifMi(server.overworld()) || y.surecDurdu(this)) {
            return;
        }
        int adim = y.zamanAdimi();
        if (adim <= 0) {
            return;
        }
        float g = d.donmaIlerlemesi();
        d.donmaIlerlemesiAyarla(switch (d.gunesEvresi()) {
            case GUNESSIZ -> DonmaIlerlemesi.donmaAdimi(g, adim, DonmaAyarlari.donmaSuresiGun());
            case KIZIL_CUCE -> DonmaIlerlemesi.cozulmeAdimi(g, adim, DonmaAyarlari.cozulmeSuresiGun(),
                    DonmaAyarlari.cozulmeTabani());
            default -> DonmaIlerlemesi.bagimsizCozulmeAdimi(g, adim, DonmaAyarlari.cozulmeSuresiGun());
        });
    }

    /** Kızıl cücede çözülme açılabilir mi: "donma kalıcı" kapalı ya da kızıl cüce pulsar yolunda (eski güneş dönecek). */
    static boolean kaliciDegil(DunyaDonemi d) {
        return !DonmaAyarlari.kalici() || PulsarYolu.donusYolu(d);
    }
}
