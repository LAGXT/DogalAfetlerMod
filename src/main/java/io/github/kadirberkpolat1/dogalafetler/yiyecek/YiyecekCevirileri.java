package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Yiyecek dil anahtarları (datagen): eşya ve efekt adları, Buz Kovası, tooltip, kademe adları, komut çıktıları. */
public final class YiyecekCevirileri {
    private YiyecekCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        esya(y, "sicak_salep", "Hot Salep", "Sıcak Salep");
        esya(y, "sicak_cikolata", "Hot Chocolate", "Sıcak Çikolata");
        esya(y, "sicak_tavuk_corbasi", "Hot Chicken Soup", "Sıcak Tavuk Çorbası");
        esya(y, "atesli_guvec", "Fiery Stew", "Ateşli Güveç");
        esya(y, "isitici_cay", "Warming Tea", "Isıtıcı Çay");
        esya(y, "ayran", "Ayran", "Ayran");
        esya(y, "buzlu_karpuz", "Iced Melon Slice", "Buzlu Karpuz Dilimi");
        esya(y, "meyve_serbeti", "Fruit Sherbet", "Meyve Şerbeti");
        esya(y, "kutup_baligi_corbasi", "Polar Fish Soup", "Kutup Balığı Çorbası");
        esya(y, "maras_dondurmasi", "Maraş Ice Cream", "Maraş Dondurması");
        esya(y, "soguk_elma_suyu", "Cold Apple Juice", "Soğuk Elma Suyu");
        esya(y, "kar_helvasi", "Snow Halva", "Kar Helvası");
        esya(y, "soguk_komposto", "Chilled Compote", "Soğuk Komposto");
        esya(y, "parlak_sorbe", "Glowing Sorbet", "Parıltılı Sorbe");
        esya(y, "buzlu_kakao", "Iced Cocoa", "Buzlu Kakao");
        esya(y, "tarhana_corbasi", "Tarhana Soup", "Tarhana Çorbası");
        esya(y, "kuzu_haslama", "Lamb Broth", "Kuzu Haşlama");
        esya(y, "kabak_corbasi", "Pumpkin Soup", "Bal Kabağı Çorbası");
        esya(y, "sucuklu_yumurta", "Sucuk and Eggs", "Sucuklu Yumurta");
        esya(y, "firinda_elma", "Baked Apple", "Fırında Elma Tatlısı");
        esya(y, "elma_cayi", "Apple Tea", "Elma Çayı");
        esya(y, "buz_kovasi", "Ice Bucket", "Buz Kovası");
        y.ekle("tooltip.dogalafetler.buz_kovasi", "Cools you when held in the off hand; melts in the heat",
                "Sol elde tutunca serinletir; sıcakta erir");
        y.ekle("tooltip.dogalafetler.buz_kovasi.erime", "Melted: %1$s%%", "Erime: %%%1$s");

        y.ekle("effect.dogalafetler.icten_isinma", "Inner Warmth", "İçten Isınma");
        y.ekle("effect.dogalafetler.serinlik", "Coolness", "Serinlik");

        y.ekle("tooltip.dogalafetler.yiyecek.etki", "%1$s: %2$s, %3$s s", "%1$s: %2$s, %3$s sn");
        y.ekle("tooltip.dogalafetler.yiyecek.yan_etki", "Overheats you fast in hot weather",
                "Sıcakta hızla aşırı ısıtır");
        y.ekle("yiyecek.dogalafetler.buyukluk.cok_kucuk", "very mild", "çok hafif");
        y.ekle("yiyecek.dogalafetler.buyukluk.kucuk", "mild", "hafif");
        y.ekle("yiyecek.dogalafetler.buyukluk.orta", "medium", "orta");
        y.ekle("yiyecek.dogalafetler.buyukluk.buyuk", "strong", "büyük");
        y.ekle("yiyecek.dogalafetler.buyukluk.cok_buyuk", "very strong", "çok büyük");

        String k = YiyecekKomutlari.K;
        y.ekle(k + "bilgi.baslik", "Temperature foods (instant share %1$s%%, same food again within %2$s s: x%3$s):",
                "Sıcaklık yiyecekleri (anında pay %%%1$s, aynı yiyecek %2$s sn içinde tekrar: x%3$s):");
        y.ekle(k + "bilgi.satir", "%1$s: %2$s (%3$s), %4$s units over %5$s s",
                "%1$s: %2$s (%3$s), %4$s birim, %5$s sn");
        y.ekle(k + "bilgi.vanilla", "Vanilla foods: warming %1$s (%2$s foods), cooling %3$s (%4$s foods)",
                "Vanilla yiyecekler: ısınma %1$s (%2$s yiyecek), serinlik %3$s (%4$s yiyecek)");
        y.ekle(k + "bilgi.oyuncu", "Diminishing returns active for %1$s: %2$s", "%1$s azalan verim penceresi: %2$s");
        y.ekle(k + "acik", "on", "açık");
        y.ekle(k + "kapali", "off", "kapalı");
        y.ekle(k + "yok", "none", "yok");
    }

    private static void esya(CeviriYazici y, String ad, String ingilizce, String turkce) {
        y.ekle("item.dogalafetler." + ad, ingilizce, turkce);
    }
}
