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
    }
}
