package io.github.kadirberkpolat1.dogalafetler.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/**
 * Uyum katmanının dil anahtarları (Jade satırları ve Jade yapılandırma başlıkları). Anahtarlar ortak koddadır, böylece datagen
 * istemci sınıfı yüklemeden yazar; JEI metinleri {@link EsyaIpuclari}'ndadır.
 */
public final class UyumCevirileri {
    /** Jade'in eklenti kategorisi başlığı: {@code config.jade.plugin_<ad alanı>}. */
    private static final String JADE_KATEGORI = "config.jade.plugin_" + DogalAfetler.MOD_ID;

    /** Kömürleşmiş kütük: "ısı: kademe/azami (durum)". */
    public static final String JADE_KUTUK = "jade.dogalafetler.kutuk_isi";
    /** Durum ekleri: {@code .sonuk}, {@code .ilik}, {@code .sicak}, {@code .cok_sicak}. */
    public static final String JADE_KUTUK_DURUM = "jade.dogalafetler.kutuk_isi.";
    public static final String JADE_KUTUK_IPUCU = "jade.dogalafetler.kutuk_isi.ipucu";
    public static final String JADE_KUL = "jade.dogalafetler.kul_kati";
    public static final String JADE_HISSEDILEN = "jade.dogalafetler.hissedilen";
    public static final String JADE_AFET = "jade.dogalafetler.afet";

    private UyumCevirileri() {
    }

    static void yaz(CeviriYazici y) {
        y.ekle(JADE_KATEGORI, "Natural Disasters", "Doğal Afetler");
        y.ekle(JADE_KATEGORI + ".kutuk_isi", "Charred Log heat", "Kömürleşmiş Kütük ısısı");
        y.ekle(JADE_KATEGORI + ".kul_kati", "Ash Layer depth", "Kül Katmanı yüksekliği");
        y.ekle(JADE_KATEGORI + ".ortam", "Felt temperature and active disaster", "Hissedilen sıcaklık ve aktif afet");

        y.ekle(JADE_KUTUK, "Heat: %s/%s (%s)", "İsı: %s/%s (%s)");
        y.ekle(JADE_KUTUK_DURUM + "sonuk", "cold", "sönük");
        y.ekle(JADE_KUTUK_DURUM + "ilik", "warm", "ılık");
        y.ekle(JADE_KUTUK_DURUM + "sicak", "hot", "sıcak");
        y.ekle(JADE_KUTUK_DURUM + "cok_sicak", "very hot", "çok sıcak");
        y.ekle(JADE_KUTUK_IPUCU, "Lava bucket: hotter, snowball: puts it out", "Lav kovası: daha sıcak, kar topu: söndürür");
        y.ekle(JADE_KUL, "Ash: %s/%s layers", "Kül: %s/%s kat");
        y.ekle(JADE_HISSEDILEN, "Felt temperature: %s °C", "Hissedilen sıcaklık: %s °C");
        y.ekle(JADE_AFET, "Active disaster: %s (level %s)", "Aktif afet: %s (seviye %s)");
    }
}
