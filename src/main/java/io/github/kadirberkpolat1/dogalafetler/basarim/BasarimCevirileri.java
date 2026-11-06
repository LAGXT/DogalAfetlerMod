package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Başarım adları, açıklamaları, ünvanlar ve komut çıktıları (madde 12: her başarımın Türkçe adı ve açıklaması). */
final class BasarimCevirileri {
    private BasarimCevirileri() {
    }

    static void yaz(CeviriYazici yazici) {
        for (Basarimlar basarim : Basarimlar.values()) {
            yazici.ekle(basarim.adAnahtari(), basarim.ingilizceAd(), basarim.turkceAd());
            yazici.ekle(basarim.aciklamaAnahtari(), basarim.ingilizceAciklama(), basarim.turkceAciklama());
        }
        for (Unvan unvan : Unvan.values()) {
            yazici.ekle(unvan.ceviriAnahtari(), unvan.ingilizceAd(), unvan.turkceAd());
        }
        yazici.ekle("commands.dogalafetler.unvan.mevcut", "Title of %s: %s", "%s ünvanı: %s");
        yazici.ekle("commands.dogalafetler.unvan.yok", "%s has no title", "%s oyuncusunun ünvanı yok");
        yazici.ekle("commands.dogalafetler.unvan.ayarlandi", "Title of %s set to %s", "%s oyuncusunun ünvanı %s oldu");
        yazici.ekle("commands.dogalafetler.unvan.kaldirildi", "Title of %s removed",
                "%s oyuncusunun ünvanı kaldırıldı");
        yazici.ekle("commands.dogalafetler.unvan.bilinmeyen", "Unknown title: %s (one of: %s)",
                "Bilinmeyen ünvan: %s (seçenekler: %s)");
        yazici.ekle("commands.dogalafetler.unvan.gizli", "Titles are hidden (basarim.unvanGoster = false)",
                "Ünvanlar gizli (basarim.unvanGoster = false)");
        yazici.ekle("commands.dogalafetler.basarim.yuklu", "Natural Disasters tab: %s/%s advancements loaded",
                "Doğal Afetler sekmesi: %s/%s başarım yüklendi");
        yazici.ekle("commands.dogalafetler.basarim.oyuncu",
                "%s: %s/%s earned; survived %s/%s disaster types (%s); ice-hole fish %s; polar days %s; title %s",
                "%s: %s/%s kazanıldı; %s/%s afet türünden sağ çıktı (%s); buz deliği balığı %s; kutup günü %s; "
                        + "ünvan %s");
        yazici.ekle("commands.dogalafetler.basarim.unvan_yok", "none", "yok");
    }
}
