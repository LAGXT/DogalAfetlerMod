package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyari.SesDuzeyi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenZarfi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariAyarlari;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Konumsuz, döngülü siren sesi (S-02). {@link SoundSource#MASTER} ile çalar; düzey {@code zarf × sirenSes / 100} (Ana Ses'i
 * ses motoru uygular). Zarf ses örneğinden ayrı yaşar: ses motoru örneği kendiliğinden keserse {@link #devam()} aynı
 * zarfla yeni bir örnek verir (K0-27).
 *
 * <p>Kaynak dinleyiciye göreli (0,0,0) çalar (sağ-sol kayması yok). Vanilla alt yazısı konumu çalma anında
 * {@code getX/Y/Z}'den okur ve göreliliğe bakmaz; ilk tick'e kadar konum dinleyicinin dünya konumudur, böylece alt yazı
 * oku (S-06 B4) dünya başlangıcını göstermez. O tick'te düzey 0'dır (zarf sessiz başlar; devam örneği bir tick sessiz).
 */
final class SirenSesi extends AbstractTickableSoundInstance {
    private final SirenZarfi zarf;
    private final long afetNo;
    private final boolean devamOrnegi;
    private String bitisNedeni;
    private boolean ilkTick = true;

    SirenSesi(SirenZarfi zarf, long afetNo) {
        this(zarf, afetNo, "güvenlik tavanı (durdurma paketi gelmedi)", false);
    }

    private SirenSesi(SirenZarfi zarf, long afetNo, String bitisNedeni, boolean devamOrnegi) {
        super(SoundEvent.createVariableRangeEvent(DogalAfetler.konum(zarf.profil().sesAdi())), SoundSource.MASTER,
                SoundInstance.createUnseededRandom());
        this.zarf = zarf;
        this.afetNo = afetNo;
        this.bitisNedeni = bitisNedeni;
        this.devamOrnegi = devamOrnegi;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        Vec3 dinleyici = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        this.x = dinleyici.x;
        this.y = dinleyici.y;
        this.z = dinleyici.z;
        this.volume = 0f;
        this.pitch = zarf.perde();
    }

    /** Aynı zarfı (geçen tick, sönüm durumu, bitiş nedeni) sürdüren yeni örnek; açılış tekrarlanmaz. */
    SirenSesi devam() {
        return new SirenSesi(zarf, afetNo, bitisNedeni, true);
    }

    SirenZarfi zarf() {
        return zarf;
    }

    long afetNo() {
        return afetNo;
    }

    boolean devamOrnegi() {
        return devamOrnegi;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        // Ses motoru durdurulan örneği kanal kapanana kadar bir iki tick daha çağırır.
        if (isStopped()) {
            return;
        }
        if (ilkTick) {
            ilkTick = false;
            x = 0.0;
            y = 0.0;
            z = 0.0;
        }
        zarf.ilerle();
        volume = SesDuzeyi.siren(zarf.duzey(), UyariAyarlari.sirenSes().get());
        pitch = zarf.perde();
        int gecen = zarf.gecenTick();
        if (zarf.sonduruluyor()) {
            if (gecen % 4 == 0 || zarf.bitti()) {
                DogalAfetler.LOGGER.info("[siren] fade-out {}: tick={} zarf={} ses={} perde={}", zarf.profil().sesAdi(),
                        gecen, bicim(zarf.duzey()), bicim(volume), bicim(pitch));
            }
        } else if (gecen % 20 == 0) {
            DogalAfetler.LOGGER.info("[siren] çalıyor {}: tick={} kalan={} zarf={} ses={} sirenSes={}",
                    zarf.profil().sesAdi(), gecen, zarf.kalanTick(), bicim(zarf.duzey()), bicim(volume),
                    UyariAyarlari.sirenSes().get());
        }
        if (zarf.bitti()) {
            DogalAfetler.LOGGER.info("[siren] durdu {}: {} (geçen {} tick, kalan süre {} / toplam {} tick)",
                    zarf.profil().sesAdi(), bitisNedeni, gecen, zarf.kalanTick(), zarf.toplamTick());
            stop();
        }
    }

    /** {@code sure} tick'lik fade-out; uygulandıysa bitiş nedeni yazılır. */
    boolean sondur(int sure, String neden) {
        if (zarf.sondur(sure)) {
            bitisNedeni = neden;
            return true;
        }
        return false;
    }

    static String bicim(float deger) {
        return String.format(Locale.ROOT, "%.4f", deger);
    }
}
