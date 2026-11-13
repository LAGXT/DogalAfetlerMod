package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.SarsintiZarfi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * K0-19 rev. K-E67 (kullanıcı isteği: "afet aktifleştikten sonra ortaya çıkma süresini 30 saniyeye düşür, hepsini"):
 * afetin seçilmesinden (ya da komutla başlatılmasından) oyuncunun onu görmesine kadar geçen süre = erken bildirim + ön-uyarı
 * penceresi (sunucu geneli türde sirenin fazlası kadar kısa) + siren + türe özel AKTIF sonrası görünme gecikmesi. Varsayılan
 * değerlerle bütün türlerde ortalama 25–35 sn, en kötü durum 35 sn'yi aşmaz. Doğal güneş patlaması ve ay çarpmasının gün
 * saati penceresi seçimde uygulanır (ön-uyarıda bekleme yok); görünme anı pencerede kalır. Süreler ölçekli tick'tir (hız
 * 1'de gerçek tick).
 */
class GorunmeSuresiTest {
    private static final int SN = 20;
    private static final double EN_AZ_ORTALAMA_SN = 25;
    private static final double EN_COK_ORTALAMA_SN = 35;
    private static final double EN_KOTU_SN = 35;

    private record Sure(double ortalamaTick, long enCokTick, double onUyariOrtalama, long onUyariEnCok, int siren,
            long gorunme) {
        double ortalamaSn() {
            return ortalamaTick / SN;
        }

        double enCokSn() {
            return enCokTick / (double) SN;
        }
    }

    /** AKTIF'in başından afetin görünmesine kadar geçen tick (gerçek sabitlerden). */
    private static long gorunmeGecikmesi(AfetTuru tur) {
        return switch (tur) {
            // Huni AKTIF'in ilk tick'inde DOGUS evresinde iner (toz girdabı → huni); duvar bulutu sirende zaten döner.
            case KASIRGA -> 0;
            // Su AKTIF'te çekilmeye başlar; dalga dalgaGecikmeSn sonra açıkta doğar.
            case TSUNAMI -> Math.round(TsunamiAyarlari.VARSAYILAN_DALGA_GECIKME_SN * SN);
            // Öncü titreme (%20) AKTIF'in ilk tick'inde başlar.
            case DEPREM -> 0;
            // AKTIF başı meteor yağmurudur; güneş sirende zaten büyür.
            case GUNES_PATLAMASI -> 0;
            // Yağmur AKTIF'in ilk tick'inde çiser; çise eşiğinin üstüne giriş rampasında çıkar.
            case ASIT_YAGMURU -> asitGorunmeTick();
            // Çatlama (titreme ve çatlaklar) AKTIF'in ilk alt aşamasıdır.
            case SINKHOLE -> 0;
            // Tutuşma AKTIF'e geçiş anındadır (OrmanYangini.asamaDegisti).
            case ORMAN_YANGINI -> 0;
            // AKTIF başı = çarpma: parlama ve patlama o anda.
            case AY_CARPMASI -> 0;
            // Sel suyu AKTIF'in ilk saniyesinde kalkmaya başlar (sinüs eğrisi: su çizgisi hemen belirginleşir).
            case SEL -> selGorunmeTick();
        };
    }

    /** En geç görünen seviyenin su çizgisi {@code GORUNUR_ARTIS} bloğa kalkma süresi (yukarı yuvarlanır). */
    private static long selGorunmeTick() {
        long en = 0;
        for (int s = 1; s <= 5; s++) {
            en = Math.max(en, (long) Math.ceil(SelKurallari.Tablo.VARSAYILAN.seviye(s).suyu(0).gorunmeGecikmesi()));
        }
        return en;
    }

    private static long asitGorunmeTick() {
        int sure = AsitKurallari.Tablo.VARSAYILAN.seviye(1).sureTick();
        long t = 0;
        while (AsitKurallari.yogunluk(t, sure) < AsitKurallari.ETKI_ESIGI) {
            t++;
        }
        return t;
    }

    private static Sure sure(AfetTuru tur) {
        int siren = SirenTuru.sureTick(tur);
        long fazla = siren - SirenTuru.STANDART_SURE_TICK;
        double onUyari = OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM
                + OnUyariPenceresi.ortalama(OnUyariPenceresi.VARSAYILAN_EN_AZ, OnUyariPenceresi.VARSAYILAN_EN_COK, fazla);
        long onUyariEnCok = OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM
                + OnUyariPenceresi.enCok(OnUyariPenceresi.VARSAYILAN_EN_AZ, OnUyariPenceresi.VARSAYILAN_EN_COK, fazla);
        long gorunme = gorunmeGecikmesi(tur);
        return new Sure(onUyari + siren + gorunme, onUyariEnCok + siren + gorunme, onUyari, onUyariEnCok, siren, gorunme);
    }

    @Test
    void butunTurlerdeSecimdenGorunmeyeOtuzSaniye() {
        Map<AfetTuru, Sure> tablo = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            tablo.put(tur, sure(tur));
        }
        assertEquals(AfetTuru.values().length, tablo.size(), "her tür kapsanmalı");
        assertEquals(9, tablo.size(), "9 afet türü");
        System.out.println("K-E67 seçimden görünmeye (sn): tür | ortalama | en çok | ön-uyarı ort/en çok | siren | görünme");
        for (Map.Entry<AfetTuru, Sure> e : tablo.entrySet()) {
            Sure s = e.getValue();
            System.out.println(String.format(Locale.ROOT, "%-16s | %5.1f | %5.1f | %5.1f / %5.1f | %4.1f | %3.1f",
                    e.getKey().kimlik(), s.ortalamaSn(), s.enCokSn(), s.onUyariOrtalama() / SN,
                    s.onUyariEnCok() / (double) SN, s.siren() / (double) SN, s.gorunme() / (double) SN));
        }
        for (Map.Entry<AfetTuru, Sure> e : tablo.entrySet()) {
            Sure s = e.getValue();
            String ad = e.getKey().kimlik();
            assertTrue(s.ortalamaSn() >= EN_AZ_ORTALAMA_SN && s.ortalamaSn() <= EN_COK_ORTALAMA_SN,
                    ad + " ortalama " + s.ortalamaSn() + " sn");
            assertTrue(s.enCokSn() <= EN_KOTU_SN, ad + " en kötü durum " + s.enCokSn() + " sn");
        }
    }

    /** Sıfır sayılan görünme gecikmelerinin dayanağı gerçek eğrilerden: AKTIF'in ilk anında görünür bir şey var. */
    @Test
    void sifirGecikmelerAktifinIlkAnindaGorunur() {
        assertEquals(KasirgaEvresi.DOGUS, KasirgaEvresi.aktifte(0, 1).evre(), "kasırga AKTIF'e doğuşla girer");
        assertTrue(SarsintiZarfi.deprem(0, 100) > 0, "deprem öncü titremesi ilk tick'te");
        assertEquals(0, SinkholeSeviyesi.CATLAMA, "sinkhole AKTIF'e çatlamayla girer");
        assertTrue(SinkholeSeviyesi.varsayilan(1).sure(SinkholeSeviyesi.CATLAMA) > 0);
        assertTrue(AyEgrileri.parlama(0f) > 0 && AyEgrileri.patlamaGorunur(0f), "ay çarpması parlaması çarpma anında");
        assertTrue(AsitKurallari.yogunluk(1, AsitKurallari.Tablo.VARSAYILAN.seviye(1).sureTick()) > 0,
                "asit yağmuru ilk tick'te çiser");
        assertTrue(asitGorunmeTick() <= 2 * SN, "asit çise eşiğini 2 sn içinde aşar: " + asitGorunmeTick());
    }

    /** Kırmızı karşılaştırma: K-E67'den önceki varsayılanlar (ön-uyarı 600 + [900, 1800], uzun siren) eşiği aşıyordu. */
    @Test
    void eskiVarsayilanlarEsigiAsiyordu() {
        int eskiUzunSiren = 60 * SN;
        for (AfetTuru tur : AfetTuru.values()) {
            int eskiSiren = tur.sunucuGeneli() ? eskiUzunSiren : SirenTuru.STANDART_SURE_TICK;
            double eski = 600 + OnUyariPenceresi.ortalama(900, 1800, 0) + eskiSiren + gorunmeGecikmesi(tur);
            assertTrue(eski / SN > EN_COK_ORTALAMA_SN, tur.kimlik() + " eski ortalama " + eski / SN + " sn");
        }
    }

    /**
     * Z2: doğal güneş patlaması ve ay çarpması yalnız görünme payı da pencereye sığıyorken seçilir; seçilebilir her anda
     * seçimden görünmeye en çok süre sonra da pencerededir ve bekleme olmadığından süre komutla başlatmayla aynıdır (≤ 35 sn).
     */
    @Test
    void dogalGunesVeAySecimdenGorunmeyeOtuzSaniyePencerede() {
        for (AfetTuru tur : new AfetTuru[] {AfetTuru.GUNES_PATLAMASI, AfetTuru.AY_CARPMASI}) {
            Sure s = sure(tur);
            long pay = GunSaatiPenceresi.gorunmePayi(tur, OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM,
                    OnUyariPenceresi.VARSAYILAN_EN_AZ, OnUyariPenceresi.VARSAYILAN_EN_COK);
            assertTrue(pay >= s.enCokTick(), tur.kimlik() + ": pay " + pay + " < en çok " + s.enCokTick());
            GunSaatiPenceresi pencere = GunSaatiPenceresi.dogalPatlama(tur).orElseThrow();
            int secilebilir = 0;
            for (long g = 0; g < GunSaatiPenceresi.GUN_TICK; g += 10) {
                if (GunSaatiPenceresi.dogalSecimCarpani(tur, g, pay, true) > 0) {
                    secilebilir++;
                    assertTrue(pencere.icinde(g + s.enCokTick()), tur.kimlik() + " görünme pencere dışı: " + g);
                }
            }
            assertTrue(secilebilir > 0, tur.kimlik() + " hiç seçilemiyor");
            assertTrue(s.ortalamaSn() >= EN_AZ_ORTALAMA_SN && s.ortalamaSn() <= EN_COK_ORTALAMA_SN && s.enCokSn() <= EN_KOTU_SN,
                    tur.kimlik() + " doğal: " + s.ortalamaSn() + " / " + s.enCokSn());
        }
    }

    /** Z3: tsunami dalgası kıyıya seçimden ortalama ≤ 40 sn'de vurur (doğuş 2 sn + kıyıya 8 sn; eski ~49 sn). */
    @Test
    void tsunamiKiyiyaSecimdenKirkSaniyede() {
        Sure s = sure(AfetTuru.TSUNAMI);
        for (int seviye = 1; seviye <= 5; seviye++) {
            TsunamiSeviyesi sv = TsunamiSeviyesi.al(seviye);
            double kiyiya = sv.aciklik() / sv.hizTick();
            System.out.println(String.format(Locale.ROOT, "tsunami S%d kıyı vuruşu seçimden ortalama %.1f sn, en çok %.1f sn",
                    seviye, (s.ortalamaTick() + kiyiya) / SN, (s.enCokTick() + kiyiya) / SN));
            assertTrue((s.ortalamaTick() + kiyiya) / SN <= 40, "S" + seviye + " ortalama");
            assertTrue((s.enCokTick() + kiyiya) / SN <= 45, "S" + seviye + " en çok");
            assertTrue(kiyiya >= TsunamiAyarlari.VARSAYILAN_CEKILME_SN * SN - TsunamiAyarlari.VARSAYILAN_DALGA_GECIKME_SN * SN,
                    "çekilme dalga vurmadan biter: S" + seviye);
        }
    }
}
