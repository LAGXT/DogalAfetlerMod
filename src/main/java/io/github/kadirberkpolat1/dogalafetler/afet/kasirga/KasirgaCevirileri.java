package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Kasırganın dil anahtarları (datagen; {@code en_us}, {@code tr_tr}). Ekrana afet uyarısı yazılmaz (N6): yalnız varlık
 * adları, alt yazılar (vanilla Alt Yazı açıkken), ölüm iletileri ve OP komut çıktıları. */
final class KasirgaCevirileri {
    private KasirgaCevirileri() {
    }

    static void yaz(CeviriYazici y) {
        y.ekle("entity.dogalafetler.kasirga", "Tornado", "Kasırga");
        y.ekle("entity.dogalafetler.ucan_blok", "Flying Block", "Uçan Blok");
        y.ekle("death.attack.dogalafetler.kasirga", "%1$s was swept away by a tornado", "%1$s kasırgaya kapıldı");
        y.ekle("death.attack.dogalafetler.kasirga.player", "%1$s was swept away by a tornado while fighting %2$s",
                "%1$s, %2$s ile dövüşürken kasırgaya kapıldı");
        y.ekle(KasirgaKomutlari.BILGI, "Tornado: stage %s, phase %s (%s), level %s, centre %s, heading %s°, flying blocks %s/%s, "
                + "lifted %s; entities in world: tornado=%s, flying_block=%s",
                "Kasırga: aşama %s, evre %s (%s), seviye %s, merkez %s, yön %s°, uçan blok %s/%s, kaldırılan %s; "
                        + "dünyadaki varlıklar: kasirga=%s, ucan_blok=%s");
        y.ekle(KasirgaKomutlari.YOK, "No active tornado; entities in world: tornado=%s, flying_block=%s",
                "Aktif kasırga yok; dünyadaki varlıklar: kasirga=%s, ucan_blok=%s");
        KasirgaKayitlari.cevirileriYaz(y);
    }
}
