package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorSesleri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorVarligi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorSesMenzili;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Meteor sesleri (istemci): uçuş kükremesi yalnız meteor {@value MeteorSesMenzili#UCUS_MENZILI} blok yakınındayken çalar, çarpma
 * patlaması büyükte 128, küçükte 96 blok içinde. İkisi de vanilla doğrusal azalma yerine {@link MeteorSesMenzili} kazancıyla
 * (zayıflama kapalı, konum yön için) çalar; uçuş perdesi dinleyiciye yaklaşma hızından Doppler, ay parçaları %10 tiz. Her
 * türden aynı anda en çok {@value MeteorSesMenzili#AYNI_TUR_TAVANI} ses; uçuşta en yakınlar, çarpmada en uzak düşer.
 * Görsel otomasyonda ({@link GorselOtomasyon#AKTIF}) çalma/çalmama kararları log'a yazılır (K-E42: üretimde satır yok).
 */
final class MeteorSesi {
    private static final String ON = "METEOR_SES ";
    /** Patlama dosyasının uzunluğu (tick): bu süreden sonra tavanda yer tutmaz. */
    private static final int PATLAMA_TICK = 140;
    private static final String[] BANTLAR = {"küçük", "orta", "büyük"};

    private record Patlama(SimpleSoundInstance ses, double mesafe, long baslangic) {
    }

    /** İstemciye giren meteor, çalan uçuş sesi (yoksa null), sesi hiç çaldı mı ve dinleyiciye en yakın mesafesi. */
    private static final class Izlenen {
        final MeteorVarligi meteor;
        Ucus ucus;
        boolean caldi;
        double enYakin = Double.MAX_VALUE;

        Izlenen(MeteorVarligi meteor) {
            this.meteor = meteor;
        }
    }

    private static final List<Izlenen> METEORLAR = new ArrayList<>();
    private static final List<Patlama> PATLAMALAR = new ArrayList<>();
    private static long saat;

    private MeteorSesi() {
    }

    /** İstemci dünyasına meteor girdi ({@code EntityJoinLevelEvent}). */
    static void ekle(MeteorVarligi meteor) {
        METEORLAR.add(new Izlenen(meteor));
    }

    /** Her istemci tick'i: duyulan meteorlardan en yakın altısının uçuş sesi çalar, menzilden çıkanınki söner. */
    static void tick(Minecraft mc) {
        saat++;
        METEORLAR.removeIf(iz -> {
            if (!iz.meteor.isRemoved()) {
                return false;
            }
            if (iz.ucus != null) {
                iz.ucus.bitir();
            }
            if (!iz.caldi) {
                if (GorselOtomasyon.AKTIF) {
                    log(iz.enYakin <= MeteorSesMenzili.UCUS_MENZILI ? "uçuş çalmadı: meteor {} en yakın {}, tavan dolu"
                        : "uçuş çalmadı: meteor {} en yakın {} > menzil {}", iz.meteor.getId(), iz.enYakin,
                        MeteorSesMenzili.UCUS_MENZILI);
                }
            }
            return true;
        });
        if (METEORLAR.isEmpty()) {
            return;
        }
        Vec3 dinleyici = mc.player.getEyePosition();
        int n = METEORLAR.size();
        double[] mesafeler = new double[n];
        boolean[] caliyor = new boolean[n];
        for (int i = 0; i < n; i++) {
            Izlenen iz = METEORLAR.get(i);
            if (iz.ucus != null && iz.ucus.isStopped()) {
                iz.ucus = null;
            }
            mesafeler[i] = iz.meteor.position().distanceTo(dinleyici);
            iz.enYakin = Math.min(iz.enYakin, mesafeler[i]);
            caliyor[i] = iz.ucus != null;
        }
        boolean[] secildi = MeteorSesMenzili.ucusSecimi(mesafeler, caliyor, MeteorSesMenzili.AYNI_TUR_TAVANI);
        for (int i = 0; i < n; i++) {
            Izlenen iz = METEORLAR.get(i);
            if (secildi[i] && !caliyor[i]) {
                iz.ucus = new Ucus(iz.meteor);
                iz.caldi = true;
                mc.getSoundManager().play(iz.ucus);
                if (GorselOtomasyon.AKTIF) {
                    log("uçuş başladı: meteor {} mesafe {} (menzil {})", iz.meteor.getId(), mesafeler[i],
                        MeteorSesMenzili.UCUS_MENZILI);
                }
            } else if (!secildi[i] && caliyor[i]) {
                iz.ucus.bitir();
                iz.ucus = null;
                if (GorselOtomasyon.AKTIF) {
                    log("uçuş söndü: meteor {} mesafe {}", iz.meteor.getId(), mesafeler[i]);
                }
            }
        }
    }

    /** Çarpma paketi geldi: menzildeyse patlama mesafe kazancıyla çalar; tavan doluysa en uzaktaki patlama söner. */
    static void carpma(Vec3 nokta, double mesafe, double olcek, boolean ay) {
        double kazanc = MeteorSesMenzili.carpmaKazanci(mesafe, olcek);
        if (kazanc <= 0) {
            if (GorselOtomasyon.AKTIF) {
                log("çarpma çalmadı: {} ölçek {} mesafe {} > menzil {}", BANTLAR[MeteorOlcegi.bant(olcek)], olcek, mesafe,
                        MeteorSesMenzili.carpmaMenzili(olcek));
            }
            return;
        }
        SoundManager sesler = Minecraft.getInstance().getSoundManager();
        PATLAMALAR.removeIf(p -> saat - p.baslangic() >= PATLAMA_TICK);
        double[] calan = new double[PATLAMALAR.size()];
        for (int i = 0; i < calan.length; i++) {
            calan[i] = PATLAMALAR.get(i).mesafe();
        }
        int yer = MeteorSesMenzili.yer(calan, mesafe, MeteorSesMenzili.AYNI_TUR_TAVANI);
        if (yer == -2) {
            if (GorselOtomasyon.AKTIF) {
                log("çarpma çalmadı: {} ölçek {} mesafe {}, tavan dolu ({} daha yakın patlama)",
                        BANTLAR[MeteorOlcegi.bant(olcek)], olcek, mesafe, calan.length);
            }
            return;
        }
        if (yer >= 0) {
            sesler.stop(PATLAMALAR.remove(yer).ses());
        }
        SimpleSoundInstance ses = new SimpleSoundInstance(MeteorSesleri.PATLAMA, SoundSource.BLOCKS, (float) kazanc,
                (float) MeteorSesMenzili.carpmaPerdesi(olcek, ay), SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, nokta.x, nokta.y, nokta.z, false);
        sesler.play(ses);
        PATLAMALAR.add(new Patlama(ses, mesafe, saat));
        if (GorselOtomasyon.AKTIF) {
            log("çarpma çaldı: {} ölçek {} mesafe {} kazanç {}", BANTLAR[MeteorOlcegi.bant(olcek)], olcek, mesafe, kazanc);
        }
    }

    static void temizle() {
        for (Izlenen iz : METEORLAR) {
            if (iz.ucus != null) {
                iz.ucus.bitir();
            }
        }
        METEORLAR.clear();
        PATLAMALAR.clear();
    }

    /** Yalnız görsel otomasyonda; ondalıklı argümanlar burada biçimlenir (üretimde biçimleme yok). */
    private static void log(String metin, Object... arg) {
        if (!GorselOtomasyon.AKTIF) {
            return;
        }
        for (int i = 0; i < arg.length; i++) {
            if (arg[i] instanceof Double d) {
                arg[i] = String.format(Locale.ROOT, "%.1f", d);
            }
        }
        DogalAfetler.LOGGER.info(ON + metin, arg);
    }

    /** Bir meteorun uçuş döngüsü: meteoru izler, kazanç mesafeyle karesel, perde Doppler. */
    private static final class Ucus extends AbstractTickableSoundInstance {
        private final MeteorVarligi meteor;
        private int yas;

        Ucus(MeteorVarligi meteor) {
            super(MeteorSesleri.UCUS_OLAYI.get(), SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.meteor = meteor;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
            this.x = meteor.getX();
            this.y = meteor.getY();
            this.z = meteor.getZ();
        }

        void bitir() {
            stop();
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (meteor.isRemoved() || mc.player == null) {
                stop();
                return;
            }
            yas++;
            x = meteor.getX();
            y = meteor.getY();
            z = meteor.getZ();
            Vec3 r = mc.player.getEyePosition().subtract(meteor.position());
            double mesafe = r.length();
            Vec3 hiz = meteor.getDeltaMovement();
            double yaklasma = mesafe > 1e-3 ? hiz.dot(r) / mesafe : 0;
            pitch = (float) (MeteorSesMenzili.dopplerPerdesi(yaklasma) * (meteor.gorunum() == MeteorGorunumu.AY ? 1.1 : 1.0));
            volume = (float) (MeteorOlcegi.ara(meteor.olcek(), 0.75, 1.0, 0.6, 1.0) * MeteorSesMenzili.ucusKazanci(mesafe)
                    * Math.min(1.0, yas / 6.0));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
