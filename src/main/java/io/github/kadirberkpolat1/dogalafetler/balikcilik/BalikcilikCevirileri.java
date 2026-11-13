package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Balıkçılığın dil anahtarları (datagen; {@code en_us} ve {@code tr_tr}). */
public final class BalikcilikCevirileri {
    private BalikcilikCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        y.ekle("block.dogalafetler.buz_deligi", "Ice Hole", "Buz Deliği");
        y.ekle("item.dogalafetler.buz_alabaligi", "Ice Trout", "Buz Alabalığı");
        y.ekle("item.dogalafetler.kutup_morinasi", "Polar Cod", "Kutup Morinası");
        y.ekle("item.dogalafetler.derin_soguk_somonu", "Deep Cold Salmon", "Derin Soğuk Somonu");
        y.ekle("item.dogalafetler.buz_kefali", "Ice Mullet", "Buz Kefali");
        y.ekle("item.dogalafetler.donmus_ringa", "Frozen Herring", "Donmuş Ringa");
        y.ekle("item.dogalafetler.pismis_buz_alabaligi", "Cooked Ice Trout", "Pişmiş Buz Alabalığı");
        y.ekle("item.dogalafetler.pismis_kutup_morinasi", "Cooked Polar Cod", "Pişmiş Kutup Morinası");
        y.ekle("item.dogalafetler.pismis_derin_soguk_somonu", "Cooked Deep Cold Salmon", "Pişmiş Derin Soğuk Somonu");
        y.ekle("item.dogalafetler.pismis_buz_kefali", "Cooked Ice Mullet", "Pişmiş Buz Kefali");
        y.ekle("item.dogalafetler.pismis_donmus_ringa", "Cooked Frozen Herring", "Pişmiş Donmuş Ringa");
        y.ekle("item.dogalafetler.buz_parcasi", "Ice Shard", "Buz Parçası");
        y.ekle(BuzBaligiEsyasi.Tur.CIG.ipucu(), "Raw: cools the body (small)", "Çiğ: vücudu serinletir (küçük)");
        y.ekle(BuzBaligiEsyasi.Tur.PISMIS.ipucu(), "Warms the body (medium)", "Vücudu ısıtır (orta)");
        y.ekle(BuzBaligiEsyasi.Tur.PISMIS_RINGA.ipucu(), "Warms the body strongly (large)", "Vücudu güçlü ısıtır (büyük)");
        y.ekle(BuzDeligiAcma.SINIR_MESAJI, "Too many ice holes are open in this area", "Bu bölgede çok fazla açık buz deliği var");
        y.ekle(BuzBaligiEsyasi.Tur.CIG_ORTA.ipucu(), "Raw: cools the body (medium)", "Çiğ: vücudu serinletir (orta)");
        y.ekle(BuzBaligiEsyasi.Tur.CIG_BUYUK.ipucu(), "Raw: cools the body strongly (large)",
                "Çiğ: vücudu güçlü serinletir (büyük)");
        y.ekle("item.dogalafetler.kutup_oltasi", "Polar Fishing Rod", "Kutup Oltası");
        y.ekle(KutupOltasi.ACIKLAMA, "The only rod that can catch the special polar fish",
                "Özel kutup balıklarını tutabilen tek olta");
        y.ekle(KutupOltasi.ACIKLAMA_2, "Bites twice as fast as a normal rod; works under ice holes and at open sea",
                "Normal oltadan 2 kat hızlı ısırır; buz deliğinde ve açık denizde çalışır");
        y.ekle(KutupOltasi.ACIKLAMA_3, "A yellow bobber under the ice means something rare is coming",
                "Buz altında şamandıra sarıysa nadir bir şey geliyor");
        y.ekle("item.dogalafetler.kristal_levrek", "Crystal Bass", "Kristal Levrek");
        y.ekle("item.dogalafetler.kutup_yilanbaligi", "Polar Eel", "Kutup Yılanbalığı");
        y.ekle("item.dogalafetler.aurora_baligi", "Aurora Fish", "Aurora Balığı");
        y.ekle("item.dogalafetler.pismis_kristal_levrek", "Cooked Crystal Bass", "Pişmiş Kristal Levrek");
        y.ekle("item.dogalafetler.pismis_kutup_yilanbaligi", "Cooked Polar Eel", "Pişmiş Kutup Yılanbalığı");
        y.ekle("item.dogalafetler.pismis_aurora_baligi", "Cooked Aurora Fish", "Pişmiş Aurora Balığı");
        y.ekle("item.dogalafetler.buzlu_inci", "Frosted Pearl", "Buzlu İnci");
        y.ekle("item.dogalafetler.tuz_kristali", "Salt Crystal", "Tuz Kristali");
        y.ekle("enchantment.dogalafetler.yineleme", "Duplicate", "Yineleme");
    }
}
