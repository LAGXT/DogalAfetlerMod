package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/**
 * Çekirdeğin dil dosyası girdileri (runData ile {@code en_us} ve {@code tr_tr} üretilir): afet adları
 * ({@code afet.dogalafetler.<ad>}), seviye adları, aşama adları ve güneş evresi (dönem) adları.
 */
public final class CekirdekCevirileri {
    public static final String MOD_ADI = "mod.dogalafetler.ad";

    private static final String[][] SEVIYELER = {
        {"Unleveled", "Seviyesiz"},
        {"Light", "Hafif"},
        {"Moderate", "Orta"},
        {"Severe", "Şiddetli"},
        {"Destructive", "Yıkıcı"},
        {"Catastrophic", "Felaket"},
    };

    private CekirdekCevirileri() {
    }

    /** Seviye adı anahtarı: {@code seviye.dogalafetler.<0-5>} (0 = seviyesiz). */
    public static String seviyeAnahtari(int seviye) {
        return "seviye.dogalafetler." + Math.max(0, Math.min(AfetTuru.EN_YUKSEK_SEVIYE, seviye));
    }

    public static void yaz(CeviriYazici yazici) {
        yazici.ekle(MOD_ADI, "Natural Disasters", "Doğal Afetler");
        for (AfetTuru tur : AfetTuru.values()) {
            yazici.ekle(tur.ceviriAnahtari(), tur.ingilizceAd(), tur.turkceAd());
        }
        for (int seviye = 0; seviye < SEVIYELER.length; seviye++) {
            yazici.ekle(seviyeAnahtari(seviye), SEVIYELER[seviye][0], SEVIYELER[seviye][1]);
        }
        yazici.ekle(AfetAsamasi.ON_UYARI.ceviriAnahtari(), "Pre-warning", "Ön-uyarı");
        yazici.ekle(AfetAsamasi.SIREN.ceviriAnahtari(), "Siren", "Siren");
        yazici.ekle(AfetAsamasi.AKTIF.ceviriAnahtari(), "Active", "Aktif");
        yazici.ekle(AfetAsamasi.BITIS.ceviriAnahtari(), "Ending", "Bitiş");
        yazici.ekle(GunesEvresi.NORMAL.ceviriAnahtari(), "Normal sun", "Normal güneş");
        yazici.ekle(GunesEvresi.BUYUYEN.ceviriAnahtari(), "Swelling sun", "Büyüyen güneş");
        yazici.ekle(GunesEvresi.SUPERNOVA.ceviriAnahtari(), "Supernova", "Süpernova");
        yazici.ekle(GunesEvresi.GUNESSIZ.ceviriAnahtari(), "Sunless era", "Güneşsiz dönem");
        yazici.ekle(GunesEvresi.KIZIL_CUCE.ceviriAnahtari(), "Red dwarf era", "Kızıl cüce dönemi");
        yazici.ekle(GunesEvresi.PULSAR.ceviriAnahtari(), "Pulsar era", "Pulsar dönemi");
    }
}
