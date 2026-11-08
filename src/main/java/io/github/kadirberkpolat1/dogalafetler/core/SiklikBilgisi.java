package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonrakiAfetTahmini;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.kural.KuralSinirlari;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * {@code afetSikligi}'nın oyuncuya görünen etkisi: {@code /dogalafet durum} ve {@code /dogalafet siklik} satırı ile
 * {@code /gamerule afetSikligi} değişince OP'lere giden bildirim. Süreler gerçek oyun günü cinsindendir ({@code hiz}
 * çarpanı hesaba katılır).
 */
public final class SiklikBilgisi {
    private static final String ON = "commands.dogalafetler.siklik.";
    public static final String SONRAKI = ON + "sonraki";
    public static final String SONRAKI_AKTIF = ON + "sonraki_aktif";
    public static final String SONRAKI_YOK = ON + "sonraki_yok";
    public static final String SONRAKI_SIFIR = ON + "sonraki_sifir";
    public static final String SONRAKI_SISTEM_KAPALI = ON + "sonraki_sistem_kapali";
    public static final String DEGISTI = ON + "degisti";
    public static final String SINIR = ON + "sinir";

    private SiklikBilgisi() {
    }

    /**
     * Sonraki afet satırı. Doğal seçim hiç olamıyorsa (afet sistemi gamerule ile kapalı, sıklık %0) bunu söyler; aktif afet
     * varsa bittikten sonraki bekleme, yoksa tahmin ({@link SonrakiAfetTahmini}) ya da "uygun afet yok".
     */
    public static Component sonrakiSatiri(AfetYoneticisi yonetici) {
        if (!yonetici.server().getGameRules().getBoolean(Kurallar.DOGAL_AFETLER_ACIK)) {
            return Component.translatable(SONRAKI_SISTEM_KAPALI);
        }
        int yuzde = yuzde(yonetici);
        if (yuzde == 0) {
            return Component.translatable(SONRAKI_SIFIR);
        }
        String carpan = sayi(yuzde / 100.0);
        Optional<Afet> aktif = yonetici.aktifAfet();
        if (aktif.isPresent()) {
            Afet afet = aktif.get();
            ZamanlayiciAyarlari ayar = CekirdekAyarlari.al().zamanlayici();
            long bekleme = AfetZamanlayicisi.beklemeOlcekle(Math.round(ayar.genelBeklemeTick()
                    * ayar.siddetUzamasi(afet.tur(), afet.seviye())), yuzde / 100.0);
            return Component.translatable(SONRAKI_AKTIF, gun(yonetici, bekleme), yuzde, carpan);
        }
        Optional<SonrakiAfetTahmini> tahmin = yonetici.sonrakiAfetTahmini();
        if (tahmin.isEmpty()) {
            return Component.translatable(SONRAKI_YOK, yuzde);
        }
        SonrakiAfetTahmini t = tahmin.get();
        return Component.translatable(SONRAKI, gun(yonetici, t.ortalama()), gun(yonetici, t.enErken()),
                gun(yonetici, t.enGec()), yuzde, carpan);
    }

    /** Sıklık değişti bildirimi: yeni değer, gerekirse aralık uyarısı ve sonraki afet satırı. */
    public static List<Component> degisimSatirlari(AfetYoneticisi yonetici) {
        List<Component> satirlar = new ArrayList<>();
        int yuzde = yuzde(yonetici);
        satirlar.add(Component.translatable(DEGISTI, yuzde, sayi(yuzde / 100.0)));
        sinirUyarisi(yonetici).ifPresent(satirlar::add);
        satirlar.add(sonrakiSatiri(yonetici));
        return satirlar;
    }

    /** Gamerule 0–1000 dışındaysa (vanilla {@code /gamerule} sınır denetlemez) kullanılan değeri söyleyen satır. */
    public static Optional<Component> sinirUyarisi(AfetYoneticisi yonetici) {
        int ham = yonetici.server().getGameRules().getInt(Kurallar.AFET_SIKLIGI);
        int yuzde = KuralSinirlari.siklikYuzdesi(ham);
        return ham == yuzde ? Optional.empty() : Optional.of(Component.translatable(SINIR, ham, yuzde));
    }

    private static int yuzde(AfetYoneticisi yonetici) {
        return KuralSinirlari.siklikYuzdesi(yonetici.server().getGameRules().getInt(Kurallar.AFET_SIKLIGI));
    }

    /** Ölçekli afet saati tick'ini gerçek oyun günü olarak yazar. */
    private static String gun(AfetYoneticisi yonetici, long olcekliTick) {
        return sayi(yonetici.zamanOlcegi().gercekTick(olcekliTick) / (double) AfetZamanlayicisi.GUN_TICK);
    }

    private static String sayi(double deger) {
        return String.format(Locale.ROOT, "%.2f", deger);
    }

    public static void cevirileriYaz(CeviriYazici y) {
        y.ekle(SONRAKI, "Next disaster: ~%s days (range %s–%s days, 90%%); frequency %s%% (x%s)",
                "Sonraki afet: ~%s gün (aralık %s–%s gün, %%90); sıklık %%%s (x%s)");
        y.ekle(SONRAKI_AKTIF, "Next disaster: when the active one ends: ~%s days; frequency %s%% (x%s)",
                "Sonraki afet: aktif afet bitince: ~%s gün; sıklık %%%s (x%s)");
        y.ekle(SONRAKI_YOK, "Next disaster: none (frequency %s%%, no eligible disaster)",
                "Sonraki afet: yok (sıklık %%%s, uygun afet yok)");
        y.ekle(SONRAKI_SIFIR, "Next disaster: disasters are off (frequency 0%%)",
                "Sonraki afet: afetler kapalı (sıklık %%0)");
        y.ekle(SONRAKI_SISTEM_KAPALI, "Next disaster: the disaster system is off (dogalAfetlerAcik)",
                "Sonraki afet: afet sistemi kapalı (dogalAfetlerAcik)");
        y.ekle(DEGISTI, "Disaster frequency is now %s%% (x%s); new and running cooldowns scale with it.",
                "Afet sıklığı artık %%%s (x%s); yeni ve süren beklemeler buna göre ölçeklenir.");
        y.ekle(SINIR, "afetSikligi %s is outside 0–1000; %s%% is used.",
                "afetSikligi %s, 0–1000 aralığının dışında; %%%s kullanılır.");
    }
}
