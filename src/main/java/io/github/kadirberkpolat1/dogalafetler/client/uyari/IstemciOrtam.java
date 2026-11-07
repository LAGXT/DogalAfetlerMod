package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.Bastirma;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamKarisimi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSonumu;
import io.github.kadirberkpolat1.dogalafetler.uyari.SesDuzeyi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenNobeti;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamPaketi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * İstemcideki ortam katmanları ({@code uyari.OrtamSesi}'nin karşılığı). Her tick katmanın düzeyi = hacim eğrisi × mesafe
 * sönümü × {@code ortamTavan} × siren bastırması (-3 dB) × kapanış fade'i; en yüksek düzeyli katmanlar kalite tavanına
 * kadar çalar (ses.md §4, §5). Ses motorunun kestiği katman (boyut değişimi) aynı zaman çizelgesiyle yeniden çalar.
 * Başka istemci paketleri katmanı yalnız {@link #katman} ile okur (ör. deprem artçı sarsıntısı, AF-D7).
 */
public final class IstemciOrtam {
    static final IstemciOrtam ORNEK = new IstemciOrtam();

    private final Map<String, Katman> katmanlar = new LinkedHashMap<>();
    private final Bastirma bastirma = new Bastirma();

    /** Salt okuma: katmanın son paketi, başından beri geçen istemci tick'i ve kapanış fade'i (1 sürüyor, 0 söndü). */
    public record KatmanDurumu(OrtamPaketi paket, long gecen, float fade) {
    }

    private IstemciOrtam() {
    }

    /** Katman istemcide varsa (sönüyor olsa da) durumu; istemci ana thread'inde. */
    public static Optional<KatmanDurumu> katman(String kimlik) {
        Katman k = ORNEK.katmanlar.get(kimlik);
        return k == null ? Optional.empty() : Optional.of(new KatmanDurumu(k.paket, k.gecen, k.fade()));
    }

    private static final class Katman {
        private OrtamPaketi paket;
        private long gecen;
        private int fadeSure;
        private int fadeGecen = -1;
        private float duzey;
        private Ornek ornek;
        private final SirenNobeti nobet = new SirenNobeti();

        private Katman(OrtamPaketi paket) {
            this.paket = paket;
            this.gecen = paket.gecenTick();
        }

        private boolean sonuyor() {
            return fadeGecen >= 0;
        }

        private float fade() {
            return sonuyor() ? Math.max(0f, 1f - fadeGecen / (float) fadeSure) : 1f;
        }
    }

    /** Katmanın ses örneği; düzeyi ve konumu her tick katmandan okur. */
    private static final class Ornek extends AbstractTickableSoundInstance {
        private final Katman katman;

        private Ornek(Katman katman) {
            super(SoundEvent.createVariableRangeEvent(katman.paket.ses()), katman.paket.kategori(),
                    SoundInstance.createUnseededRandom());
            this.katman = katman;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = katman.paket.boyut() == null;
            konum();
            this.volume = katman.duzey;
        }

        private void konum() {
            OrtamPaketi p = katman.paket;
            x = p.boyut() == null ? 0.0 : p.x();
            y = p.boyut() == null ? 0.0 : p.y();
            z = p.boyut() == null ? 0.0 : p.z();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (!isStopped()) {
                konum();
                volume = katman.duzey;
            }
        }

        private void bitir() {
            stop();
        }
    }

    void paket(OrtamPaketi paket) {
        Katman eski = katmanlar.get(paket.kimlik());
        boolean ayni = eski != null && !eski.sonuyor() && eski.paket.baslangic() == paket.baslangic();
        if (ayni) {
            eski.paket = paket;
            return;
        }
        if (eski != null) {
            sesiKes(eski);
        }
        katmanlar.put(paket.kimlik(), new Katman(paket));
        DogalAfetler.LOGGER.info("[ortam] katman alındı: {} ({}, {}) geçen {} tick", paket.kimlik(), paket.ses(),
                paket.boyut() == null ? "global" : "menzil " + paket.menzil(), paket.gecenTick());
    }

    void durdur(OrtamDurdurPaketi paket) {
        Katman katman = katmanlar.get(paket.kimlik());
        if (katman != null && !katman.sonuyor()) {
            katman.fadeSure = paket.fadeTick();
            katman.fadeGecen = 0;
            DogalAfetler.LOGGER.info("[ortam] katman sönüyor: {} ({} tick)", paket.kimlik(), paket.fadeTick());
        }
    }

    void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || katmanlar.isEmpty()) {
            if (mc.level != null && !mc.isPaused()) {
                bastirma.ilerle(SirenCalar.ORNEK.caliyor());
            }
            return;
        }
        float bastir = bastirma.ilerle(SirenCalar.ORNEK.caliyor());
        Vec3 kamera = mc.gameRenderer.getMainCamera().getPosition();
        String boyut = mc.level.dimension().location().toString();
        int tavan = UyariAyarlari.ortamTavan();
        List<OrtamKarisimi.Aday> adaylar = new ArrayList<>();
        SoundManager sesler = mc.getSoundManager();
        boolean anaSesKapali = mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0f;
        for (Iterator<Katman> it = katmanlar.values().iterator(); it.hasNext(); ) {
            Katman k = it.next();
            k.gecen++;
            if (k.sonuyor() && ++k.fadeGecen >= k.fadeSure) {
                sesiKes(k);
                it.remove();
                DogalAfetler.LOGGER.info("[ortam] katman bitti: {}", k.paket.kimlik());
                continue;
            }
            OrtamPaketi p = k.paket;
            float sonum = p.boyut() == null ? 1f : boyut.equals(p.boyut().toString())
                    ? OrtamSonumu.carpan(kamera.distanceTo(new Vec3(p.x(), p.y(), p.z())), p.menzil()) : 0f;
            k.duzey = SesDuzeyi.ortam(p.egri().deger(k.gecen), sonum, tavan, bastir) * k.fade();
            adaylar.add(new OrtamKarisimi.Aday(p.kimlik(), p.ses().toString(), k.duzey));
        }
        Set<String> secilen = OrtamKarisimi.secilenler(adaylar,
                OrtamKarisimi.tavan(IstemciAyarlari.efektKalitesi()), OrtamKarisimi.TUR_TAVANI);
        for (Katman k : katmanlar.values()) {
            boolean calsin = secilen.contains(k.paket.kimlik());
            if (!calsin) {
                sesiKes(k);
            } else if (k.ornek == null) {
                k.ornek = new Ornek(k);
                sesler.play(k.ornek);
            } else if (k.nobet.yenidenCalinmali(k.ornek.isStopped(), false, sesler.isActive(k.ornek), anaSesKapali)) {
                k.ornek = new Ornek(k);
                sesler.play(k.ornek);
                DogalAfetler.LOGGER.info("[ortam] katman yeniden çalındı (ses motoru kesti): {}", k.paket.kimlik());
            }
        }
    }

    /** Çıkışta bütün katmanlar fade beklemeden kalkar. */
    void temizle() {
        katmanlar.values().forEach(IstemciOrtam::sesiKes);
        katmanlar.clear();
    }

    private static void sesiKes(Katman k) {
        if (k.ornek != null) {
            k.ornek.bitir();
            Minecraft.getInstance().getSoundManager().stop(k.ornek);
            k.ornek = null;
        }
    }

    /** Otomasyon ve log için: katmanın o anki düzeyi (yoksa -1) ve ses motorunda çalıp çalmadığı. */
    float duzey(String kimlik) {
        Katman k = katmanlar.get(kimlik);
        return k == null ? -1f : k.duzey;
    }

    boolean caliyor(String kimlik) {
        Katman k = katmanlar.get(kimlik);
        return k != null && k.ornek != null && Minecraft.getInstance().getSoundManager().isActive(k.ornek);
    }

    boolean var(String kimlik) {
        return katmanlar.containsKey(kimlik);
    }
}
