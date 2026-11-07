package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorSesleri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorVarligi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorSesMenzili;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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

    private record Patlama(SimpleSoundInstance ses, double mesafe, long baslangic) {
    }

    private static final Map<MeteorVarligi, Ucus> UCUSLAR = new HashMap<>();
    private static final Set<Integer> GORULEN = new HashSet<>();
    private static final List<Patlama> PATLAMALAR = new ArrayList<>();
    private static long saat;

    private MeteorSesi() {
    }

    /** Her istemci tick'i: duyulan meteorlardan en yakın altısının uçuş sesi çalar, menzilden çıkanınki söner. */
    static void tick(Minecraft mc) {
        saat++;
        UCUSLAR.entrySet().removeIf(g -> g.getValue().isStopped() || g.getKey().isRemoved());
        List<MeteorVarligi> meteorlar = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof MeteorVarligi m && !m.isRemoved()) {
                meteorlar.add(m);
            }
        }
        Vec3 dinleyici = mc.player.getEyePosition();
        double[] mesafeler = new double[meteorlar.size()];
        boolean[] caliyor = new boolean[meteorlar.size()];
        for (int i = 0; i < mesafeler.length; i++) {
            mesafeler[i] = meteorlar.get(i).position().distanceTo(dinleyici);
            caliyor[i] = UCUSLAR.containsKey(meteorlar.get(i));
        }
        boolean[] secildi = MeteorSesMenzili.ucusSecimi(mesafeler, caliyor, MeteorSesMenzili.AYNI_TUR_TAVANI);
        Set<Integer> simdiki = new HashSet<>();
        for (int i = 0; i < mesafeler.length; i++) {
            MeteorVarligi m = meteorlar.get(i);
            simdiki.add(m.getId());
            boolean ilk = GORULEN.add(m.getId());
            if (secildi[i] && !caliyor[i]) {
                Ucus u = new Ucus(m);
                UCUSLAR.put(m, u);
                mc.getSoundManager().play(u);
                log("uçuş başladı: meteor {} mesafe {} (menzil {})", m.getId(), sayi(mesafeler[i]),
                        sayi(MeteorSesMenzili.UCUS_MENZILI));
            } else if (!secildi[i] && caliyor[i]) {
                UCUSLAR.remove(m).bitir();
                log("uçuş söndü: meteor {} mesafe {}", m.getId(), sayi(mesafeler[i]));
            } else if (ilk && !secildi[i]) {
                log("uçuş çalmadı: meteor {} mesafe {} > menzil {}", m.getId(), sayi(mesafeler[i]),
                        sayi(MeteorSesMenzili.UCUS_MENZILI));
            }
        }
        GORULEN.retainAll(simdiki);
    }

    /** Çarpma paketi geldi: menzildeyse patlama mesafe kazancıyla çalar; tavan doluysa en uzaktaki patlama söner. */
    static void carpma(Vec3 nokta, double mesafe, boolean buyuk, boolean ay) {
        double kazanc = MeteorSesMenzili.carpmaKazanci(mesafe, buyuk);
        String boy = buyuk ? "büyük" : "küçük";
        if (kazanc <= 0) {
            log("çarpma çalmadı: {} mesafe {} > menzil {}", boy, sayi(mesafe), sayi(MeteorSesMenzili.carpmaMenzili(buyuk)));
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
            log("çarpma çalmadı: {} mesafe {}, tavan dolu ({} daha yakın patlama)", boy, sayi(mesafe), calan.length);
            return;
        }
        if (yer >= 0) {
            sesler.stop(PATLAMALAR.remove(yer).ses());
        }
        SimpleSoundInstance ses = new SimpleSoundInstance(MeteorSesleri.PATLAMA, SoundSource.BLOCKS, (float) kazanc,
                (float) MeteorSesMenzili.carpmaPerdesi(buyuk, ay), SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, nokta.x, nokta.y, nokta.z, false);
        sesler.play(ses);
        PATLAMALAR.add(new Patlama(ses, mesafe, saat));
        log("çarpma çaldı: {} mesafe {} kazanç {}", boy, sayi(mesafe), sayi(kazanc));
    }

    static void temizle() {
        for (Iterator<Ucus> it = UCUSLAR.values().iterator(); it.hasNext();) {
            it.next().bitir();
            it.remove();
        }
        GORULEN.clear();
        PATLAMALAR.clear();
    }

    private static void log(String metin, Object... arg) {
        if (GorselOtomasyon.AKTIF) {
            DogalAfetler.LOGGER.info(ON + metin, arg);
        }
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
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
            volume = (float) ((meteor.boyut().buyuk() ? 1.0 : 0.75) * MeteorSesMenzili.ucusKazanci(mesafe)
                    * Math.min(1.0, yas / 6.0));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
