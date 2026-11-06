package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyari.Uyari;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariAyarlari;
import java.util.List;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/**
 * "Müzik ve Sesler" ekranına eklenen "Sirenler" kaydırıcısı (N5, S-01). Vanilla dokuz kategori kaydırıcısını ikili
 * satırlara dizer ve "Konuşma" satırının sağı boş kalır; kaydırıcı oraya girer, bulunamazsa (başka bir mod ekranı
 * değiştirdiyse) listenin sonuna tam genişlikte eklenir. Değer bellekte anında değişir (çalan siren her tick okur),
 * dosyaya ekran kapanırken yazılır.
 */
final class SirenKaydiricisi {
    private static OptionInstance<Integer> sonEklenen;

    private SirenKaydiricisi() {
    }

    /** Son açılan ses ekranına eklenen kaydırıcı (otomasyon onu fareyle oynatmadan sürer). */
    static OptionInstance<Integer> sonEklenen() {
        return sonEklenen;
    }

    private static OptionInstance<Integer> olustur() {
        return new OptionInstance<>(Uyari.KAYDIRICI,
                OptionInstance.cachedConstantTooltip(Component.translatable(Uyari.KAYDIRICI_ACIKLAMA)),
                (baslik, deger) -> deger == 0
                        ? Options.genericValueLabel(baslik, CommonComponents.OPTION_OFF)
                        : Component.translatable("options.percent_value", baslik, deger),
                new OptionInstance.IntRange(0, 100),
                UyariAyarlari.sirenSes().get(),
                deger -> UyariAyarlari.sirenSes().set(deger));
    }

    /** Kaydırıcıyı listeye ekler; nereye eklendiğini döndürür (log için). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static String ekle(Options ayarlar, OptionsList liste) {
        OptionInstance<Integer> siren = olustur();
        sonEklenen = siren;
        OptionInstance<Double> konusma = ayarlar.getSoundSourceOptionInstance(SoundSource.VOICE);
        AbstractWidget konusmaDugmesi = liste.findOption(konusma);
        // OptionsList.Entry protected olduğu için satırlar türü anılmadan yer değiştirir.
        List satirlar = liste.children();
        for (int i = 0; i < satirlar.size(); i++) {
            List<?> ogeler = ((ContainerEventHandler) satirlar.get(i)).children();
            if (konusmaDugmesi != null && ogeler.size() == 1 && ogeler.get(0) == konusmaDugmesi) {
                liste.addSmall(konusma, siren);
                satirlar.set(i, satirlar.remove(satirlar.size() - 1));
                return "Konuşma satırının sağı (satır " + i + ")";
            }
        }
        liste.addBig(siren);
        DogalAfetler.LOGGER.warn("[siren] Konuşma satırı bulunamadı; Sirenler kaydırıcısı listenin sonuna eklendi");
        return "listenin sonu";
    }
}
