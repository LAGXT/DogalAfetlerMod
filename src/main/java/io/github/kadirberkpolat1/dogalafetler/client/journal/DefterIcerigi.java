package io.github.kadirberkpolat1.dogalafetler.client.journal;

import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.AltBaslik;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Baslik;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Bosluk;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Deger;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Esya;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Metin;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.SayfaSonu;
import io.github.kadirberkpolat1.dogalafetler.client.journal.DefterSatiri.Satir;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekCevirileri;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterCevirileri;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi.EsyaKaydi;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi.SonAfet;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi.TurKaydi;
import io.github.kadirberkpolat1.dogalafetler.journal.MesafeGrubu;
import io.github.kadirberkpolat1.dogalafetler.journal.mantik.SayfaDuzeni;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Defterin dört sekmesinin içeriğini satır listesi olarak kurar (Genel, Afetler, Balık/Eşya, Rehber). Paket tek
 * seferde gelir; metinler sütun genişliğine bir kez kırılır. Sayılar ve dil anahtarları {@link DefterCevirileri}'ndedir.
 */
final class DefterIcerigi {
    static final int GENEL = 0;
    static final int AFETLER = 1;
    static final int ESYALAR = 2;
    static final int REHBER = 3;
    static final int SEKME_SAYISI = 4;

    private static final String ON = DefterCevirileri.ON;

    private DefterIcerigi() {
    }

    static List<Satir> sekme(int sekme, DefterPaketi d, Font font, int genislik) {
        Kurucu k = new Kurucu(font, genislik);
        switch (sekme) {
            case GENEL -> genel(k, d);
            case AFETLER -> afetler(k, d);
            case ESYALAR -> esyalar(k, d);
            default -> rehber(k, d);
        }
        return k.satirlar;
    }

    // ---------------------------------------------------------------- Genel

    private static void genel(Kurucu k, DefterPaketi d) {
        k.grupAc();
        k.baslik(t("genel.yasam"));
        k.deger(t("genel.gun_simdi"), gun(d.yasamGunu()));
        k.deger(t("genel.gun_en"), gun(d.enUzunGun()));
        k.deger(t("genel.dunya"), sayi(d.dunyaGunu()));
        k.deger(t("genel.olum"), sayi(d.olum()));
        k.bosluk(4);

        k.grupAc();
        k.baslik(t("genel.mesafe"));
        long toplam = 0;
        for (long cm : d.mesafeCm()) {
            toplam += cm;
        }
        k.deger(t("mesafe.toplam"), mesafe(toplam));
        k.deger(t("mesafe.kara"), mesafe(d.mesafeCm()[MesafeGrubu.KARA.ordinal()]));
        k.deger(t("mesafe.su"), mesafe(d.mesafeCm()[MesafeGrubu.SU.ordinal()]));
        k.deger(t("mesafe.hava"), mesafe(d.mesafeCm()[MesafeGrubu.HAVA.ordinal()]));
        k.deger(t("mesafe.tasit"), mesafe(d.mesafeCm()[MesafeGrubu.TASIT.ordinal()]));

        int atlatilan = 0;
        int afetOlum = 0;
        for (TurKaydi tur : d.turler()) {
            atlatilan += tur.atlatilan();
            afetOlum += tur.olum();
        }
        k.grupAc();
        k.baslik(t("genel.afetler"));
        k.deger(t("afet.atlatilan"), sayi(atlatilan));
        if (d.enYuksekSeviye() > 0) {
            k.deger(t("afet.en_yuksek"), Component.literal(d.enYuksekSeviye() + " · ")
                    .append(Component.translatable(CekirdekCevirileri.seviyeAnahtari(d.enYuksekSeviye()))));
            k.deger(t("afet.en_yuksek_tur"), turAdi(d.enYuksekTur()));
        } else {
            k.deger(t("afet.en_yuksek"), t("yok"));
            k.deger(t("afet.en_yuksek_tur"), t("yok"));
        }
        k.deger(t("afet.olum"), sayi(afetOlum));
        k.deger(t("afet.dalga"), sayi(d.tsunamiDalga()));
        k.bosluk(4);

        k.grupAc();
        k.baslik(t("genel.sicaklik"));
        k.deger(t("sicaklik.soguk"), derece(d.enSogukC()));
        k.deger(t("sicaklik.sicak"), derece(d.enSicakC()));
        k.bosluk(4);

        k.grupAc();
        k.baslik(t("genel.meteor"));
        k.deger(t("meteor.parca"), sayi(d.meteoritParca()));
        k.deger(t("meteor.cekirdek"), sayi(d.kozmikCekirdek()));
        k.deger(t("buz.delik"), sayi(d.buzDeligi()));
    }

    // ---------------------------------------------------------------- Afetler

    private static void afetler(Kurucu k, DefterPaketi d) {
        AfetTuru[] turler = AfetTuru.values();
        for (int i = 0; i < turler.length && i < d.turler().size(); i++) {
            TurKaydi kayit = d.turler().get(i);
            AfetTuru tur = turler[i];
            k.grupAc();
            k.baslik(Component.translatable(tur.ceviriAnahtari()));
            k.deger(t("afet.atlatilan"), sayi(kayit.atlatilan()));
            k.deger(t("afet.karsilasilan"), sayi(kayit.karsilasilan()));
            k.deger(t("afet.olum"), sayi(kayit.olum()));
            if (tur.seviyeli()) {
                k.bosluk(4);
                k.altBaslik(t("afetler.seviye"));
                for (int seviye = AfetTuru.EN_DUSUK_SEVIYE; seviye <= AfetTuru.EN_YUKSEK_SEVIYE; seviye++) {
                    k.deger(seviyeEtiketi(seviye), sayi(kayit.seviye()[seviye - 1]));
                }
            }
            k.bosluk(4);
            k.altBaslik(t("afetler.ilk"));
            if (kayit.ilkGun() < 0) {
                k.metin(t("afetler.ilk_yok"), DefterSatiri.SOLUK);
            } else {
                k.deger(t("afetler.ilk_gun"), sayi(kayit.ilkGun()));
                k.deger(t("afetler.ilk_tarih"), Component.literal(tarih(kayit.ilkTarih())));
            }
            k.sayfaSonu();
        }
        k.grupAc();
        k.baslik(t("afetler.son"));
        if (d.son().isEmpty()) {
            k.metin(t("afetler.son_bos"), DefterSatiri.SOLUK);
        }
        for (SonAfet son : d.son()) {
            Component ad = turAdi(son.tur());
            if (son.seviye() > 0) {
                ad = Component.translatable(ON + "afetler.son_seviyeli", ad, Integer.toString(son.seviye()));
            }
            k.deger(ad, Component.translatable(ON + "gun.no", Long.toString(son.gun())));
        }
    }

    // ---------------------------------------------------------------- Balık/Eşya

    private static void esyalar(Kurucu k, DefterPaketi d) {
        k.grupAc();
        k.baslik(t("esyalar.baslik"));
        k.deger(t("esyalar.olta"), sayi(d.olta()));
        k.deger(t("esyalar.buz_tutus"), sayi(d.buzTutus()));
        k.deger(t("esyalar.buz_balik"), sayi(d.buzBalik()));
        k.deger(t("esyalar.balik"), sayi(d.balikToplam()));
        k.deger(t("esyalar.esya"), sayi(d.esyaToplam()));
        k.bosluk(4);
        if (d.esyalar().isEmpty()) {
            k.grupAc();
            k.metin(t("esyalar.bos"), DefterSatiri.SOLUK);
            return;
        }
        liste(k, d, true, t("esyalar.balik_liste"));
        liste(k, d, false, t("esyalar.esya_liste"));
    }

    /** Başlık ilk satırla birlikte kalır; liste sayfalara serbestçe bölünür. */
    private static void liste(Kurucu k, DefterPaketi d, boolean balik, Component baslik) {
        boolean ilk = true;
        for (EsyaKaydi kayit : d.esyalar()) {
            if (kayit.balik() != balik) {
                continue;
            }
            ResourceLocation kimlik = ResourceLocation.tryParse(kayit.kimlik());
            Item esya = kimlik == null ? null : ForgeRegistries.ITEMS.getValue(kimlik);
            if (esya == null || esya == Items.AIR) {
                continue;
            }
            ItemStack yigin = new ItemStack(esya);
            if (ilk) {
                k.grupAc();
                k.baslik(baslik);
                k.esya(yigin, kayit.adet());
                k.grupBirak();
                ilk = false;
            } else {
                k.esya(yigin, kayit.adet());
            }
        }
        if (!ilk) {
            k.bosluk(4);
        }
    }

    // ---------------------------------------------------------------- Rehber

    private static void rehber(Kurucu k, DefterPaketi d) {
        k.grupAc();
        k.baslik(t("rehber.seviyeler"));
        for (int seviye = AfetTuru.EN_DUSUK_SEVIYE; seviye <= AfetTuru.EN_YUKSEK_SEVIYE; seviye++) {
            if (seviye > AfetTuru.EN_DUSUK_SEVIYE) {
                k.grupAc();
            }
            k.altBaslik(seviyeEtiketi(seviye));
            k.metin(Component.translatable(DefterCevirileri.SEVIYE_ON + seviye), DefterSatiri.METIN);
            k.bosluk(3);
        }
        k.grupAc();
        k.metin(t("rehber.seviye_not"), DefterSatiri.SOLUK);
        k.sayfaSonu();

        AfetTuru[] turler = AfetTuru.values();
        for (int i = 0; i < turler.length; i++) {
            k.grupAc();
            if (i == 0) {
                k.baslik(t("rehber.ne_yapmali"));
            }
            k.altBaslik(Component.translatable(turler[i].ceviriAnahtari()));
            k.metin(Component.translatable(DefterCevirileri.IPUCU_ON + turler[i].kimlik()), DefterSatiri.METIN);
            k.bosluk(4);
        }
        k.sayfaSonu();

        float[] e = d.esikC();
        k.grupAc();
        k.baslik(t("rehber.sicaklik"));
        k.metin(t("rehber.sicaklik_giris"), DefterSatiri.METIN);
        k.bosluk(4);
        k.grupAc();
        k.metin(Component.translatable(ON + "rehber.sicaklik_usume", c(e[2])), DefterSatiri.METIN);
        k.bosluk(2);
        k.metin(Component.translatable(ON + "rehber.sicaklik_donma", c(e[1]), c(e[0])), DefterSatiri.METIN);
        k.bosluk(2);
        k.metin(Component.translatable(ON + "rehber.sicaklik_bunalma", c(e[3])), DefterSatiri.METIN);
        k.bosluk(2);
        k.metin(Component.translatable(ON + "rehber.sicaklik_yanma", c(e[4]), c(e[5])), DefterSatiri.METIN);
        k.bosluk(4);
        for (int i = 1; i <= 3; i++) {
            k.grupAc();
            k.metin(t("rehber.sicaklik_ipucu" + i), DefterSatiri.SOLUK);
            k.bosluk(4);
        }
    }

    // ---------------------------------------------------------------- biçimleme

    private static MutableComponent t(String anahtar) {
        return Component.translatable(ON + anahtar);
    }

    private static Component sayi(long n) {
        return Component.literal(Long.toString(n));
    }

    private static Component gun(long n) {
        return n == 1 ? t("gun.bir") : Component.translatable(ON + "gun", Long.toString(n));
    }

    /** 1 km'nin altında metre, üstünde bir ondalıklı kilometre. */
    private static Component mesafe(long santimetre) {
        long metre = santimetre / 100;
        if (metre < 1000) {
            return Component.translatable(ON + "mesafe.m", Long.toString(metre));
        }
        return Component.translatable(ON + "mesafe.km", String.format(Locale.ROOT, "%.1f", metre / 1000.0));
    }

    private static Component derece(float santigrat) {
        return Float.isNaN(santigrat) ? t("yok") : Component.translatable(ON + "derece", c(santigrat));
    }

    private static String c(float santigrat) {
        return String.format(Locale.ROOT, "%.1f", santigrat);
    }

    private static Component turAdi(int sira) {
        AfetTuru[] turler = AfetTuru.values();
        return sira >= 0 && sira < turler.length ? Component.translatable(turler[sira].ceviriAnahtari()) : t("yok");
    }

    private static Component seviyeEtiketi(int seviye) {
        return Component.literal(seviye + " · ").append(Component.translatable(CekirdekCevirileri.seviyeAnahtari(seviye)));
    }

    private static String tarih(long milisaniye) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(milisaniye), ZoneId.systemDefault()).toString();
    }

    /** Satır listesi kurucusu: geçerli grubu ve sütun genişliğini tutar. */
    private static final class Kurucu {
        final List<Satir> satirlar = new ArrayList<>();
        private final Font font;
        private final int genislik;
        private int grup = -1;
        private int sonrakiGrup;

        Kurucu(Font font, int genislik) {
            this.font = font;
            this.genislik = genislik;
        }

        /** Sonraki satırlar yeni bir bölünmez grup oluşturur. */
        void grupAc() {
            grup = sonrakiGrup++;
        }

        /** Sonraki satırlar gruplanmaz (sayfalara serbestçe bölünür). */
        void grupBirak() {
            grup = SayfaDuzeni.GRUPSUZ;
        }

        void baslik(Component metin) {
            satirlar.add(new Baslik(metin, grup));
        }

        void altBaslik(Component metin) {
            satirlar.add(new AltBaslik(metin, grup));
        }

        void deger(Component etiket, Component deger) {
            satirlar.add(new Deger(etiket, deger, grup));
        }

        void metin(Component metin, int renk) {
            for (FormattedCharSequence satir : font.split(metin, genislik)) {
                satirlar.add(new Metin(satir, renk, grup));
            }
        }

        void esya(ItemStack yigin, int adet) {
            satirlar.add(new Esya(yigin, yigin.getHoverName(), adet, grup));
        }

        void bosluk(int yukseklik) {
            satirlar.add(new Bosluk(yukseklik, grup));
        }

        void sayfaSonu() {
            satirlar.add(new SayfaSonu());
        }
    }
}
