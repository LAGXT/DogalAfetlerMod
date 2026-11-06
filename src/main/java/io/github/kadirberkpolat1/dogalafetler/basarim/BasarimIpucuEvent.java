package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.IpucuKaynaklari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Faz 4 afetlerinin başarımlara verdiği ipucu ({@code MinecraftForge.EVENT_BUS}, sunucu ana thread'i). Yalnız afetin
 * kendisinin bildiği "o afette ayrıca" koşullar içindir (madde 12 #2, #4, #5, #6); ipucu oyuncunun süren (AKTIF) afetteki
 * izine yazılır ve afet doğal bitince {@code afetten_sag_cikti} ölçütünün {@code kosul} alanına döner. Sözleşme
 * {@code .vibe/kararlar/basarim.md} §A.3: ipucunun ait olduğu afet türü onu yayınladığını mod kurucusunda
 * {@link #yayinciKaydet} ile bildirir; o türde başarımın yedek denetimi çalışmaz, tek kaynak bu olaydır (A-31). Kayıtsız
 * türden gelen olay yok sayılır ve tür + ipucu başına bir kez {@code WARN} yazılır (A-35).
 *
 * <pre>{@code
 * BasarimIpucuEvent.yayinciKaydet(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);            // mod kurucusunda
 * MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(oyuncu, Ipucu.CUKURA_DUSTU));  // oyuncu çukura düştüğünde
 * }</pre>
 */
public class BasarimIpucuEvent extends Event {
    /** Hangi afet türü hangi ipucunu kendisi yayınlıyor. */
    static final IpucuKaynaklari YAYINCILAR = new IpucuKaynaklari();

    private final ServerPlayer oyuncu;
    private final Ipucu ipucu;

    public BasarimIpucuEvent(ServerPlayer oyuncu, Ipucu ipucu) {
        this.oyuncu = Objects.requireNonNull(oyuncu, "oyuncu");
        this.ipucu = Objects.requireNonNull(ipucu, "ipucu");
    }

    public ServerPlayer oyuncu() {
        return oyuncu;
    }

    public Ipucu ipucu() {
        return ipucu;
    }

    /** Afet türü bu ipuçlarını kendisi yayınlar: başarımın yedek denetimi o türde kapanır. Mod kurucusunda bir kez. */
    public static void yayinciKaydet(AfetTuru tur, Ipucu... ipuclari) {
        for (Ipucu ipucu : ipuclari) {
            YAYINCILAR.kaydet(tur, ipucu);
        }
    }

    /** Yalnız GameTest: kaydı {@code try}-with-resources sonunda geri alır (önceden kayıtlıysa dokunmaz). */
    public static GeciciYayinci geciciYayinci(AfetTuru tur, Ipucu ipucu) {
        return new GeciciYayinci(tur, ipucu, YAYINCILAR.kaydet(tur, ipucu));
    }

    /** {@link #geciciYayinci} kaydı. */
    public record GeciciYayinci(AfetTuru tur, Ipucu ipucu, boolean yeni) implements AutoCloseable {
        @Override
        public void close() {
            if (yeni) {
                YAYINCILAR.kaldir(tur, ipucu);
            }
        }
    }

    /**
     * Yalnız GameTest ve otomasyon: türü {@code try}-with-resources boyunca bu ipucunun kayıtsız yayıncısı yapar (yedek
     * denetim yolu sınanabilsin), sonunda önceki kaydı geri koyar (K-E22). Faz 4 afeti mod kurucusunda {@link #yayinciKaydet}
     * yaptığı için "kayıtsız tür" örneği olarak gerçek türe güvenen test bununla sarılır. Üretim kodunda kullanılmaz.
     */
    public static GeciciKayitsiz geciciKayitsiz(AfetTuru tur, Ipucu ipucu) {
        return new GeciciKayitsiz(tur, ipucu, YAYINCILAR.kaldir(tur, ipucu));
    }

    /** {@link #geciciKayitsiz} tutamaçı. */
    public record GeciciKayitsiz(AfetTuru tur, Ipucu ipucu, boolean kayitliydi) implements AutoCloseable {
        @Override
        public void close() {
            if (kayitliydi) {
                YAYINCILAR.kaydet(tur, ipucu);
            }
        }
    }
}
