package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.eventbus.api.IEventBus;

/** Yangın alt sisteminin tek kayıt noktası; {@code DogalAfetler} kurucusundan koşulsuz çağrılır (K-C33). */
public final class YanginSistemi {
    private YanginSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        YanginAyarlari.kaydet();
        YanginKayitlari.kaydet(modBus);
        AgKanali.kaydet(k -> k.sunucudanIstemciye("yangin/ates_durumu", AtesDurumuPaketi.class, AtesDurumuPaketi::yaz,
                AtesDurumuPaketi::oku, AtesDurumuPaketi::isle));
        KomutKaydi.ekle(YanginKomutlari.komut());
        DataGenerators.ceviriEkle(YanginSistemi::cevirileriYaz);
        DataGenerators.saglayiciEkle(YanginEtiketSaglayici::ekle);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.FIRE, YanginKayitlari.AFET_ATESI);
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle("block.dogalafetler.afet_atesi", "Disaster Fire", "Afet Ateşi");
        y.ekle(YanginKomutlari.TUTUSTUR, "Ignited %s fire cells", "%s yangın hücresi tutuşturuldu");
        y.ekle(YanginKomutlari.TUTUSTUR_SURUYOR, "Ignited %s fire cells; the rest of the area ignites over the next ticks",
                "%s yangın hücresi tutuşturuldu; alanın kalanı sonraki tick'lerde tutuşuyor");
        y.ekle(YanginKomutlari.SONDUR, "Extinguished %s fire cells", "%s yangın hücresi söndürüldü");
        y.ekle(YanginKomutlari.BILGI,
                "Burning cells: %s/%s, budget %s/%s per tick (TPS throttle ×%s, TPS %s), manager %s ms avg / %s ms max",
                "Yanan hücre: %s/%s, bütçe tick başına %s/%s (TPS kısması ×%s, TPS %s), yönetici %s ms ort / %s ms en çok");
    }
}
