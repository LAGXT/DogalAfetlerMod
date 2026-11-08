package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunGunu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SeviyeDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** {@code /dogalafet} kökünün kaydını, yetkisini ve çekirdek alt komutlarını doğrular. */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KomutOyunTestleri {
    private KomutOyunTestleri() {
    }

    private static CommandSourceStack opKaynagi(MinecraftServer server) {
        return server.createCommandSourceStack().withSuppressedOutput().withPermission(DogalAfetKomutu.YETKI_SEVIYESI);
    }

    private static int calistir(GameTestHelper yardimci, String komut) {
        MinecraftServer server = yardimci.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut, opKaynagi(server));
        } catch (CommandSyntaxException hata) {
            yardimci.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return -1;
        }
    }

    @GameTest(template = "bos")
    public static void kokKomutuOpIleCalisir(GameTestHelper yardimci) {
        CommandDispatcher<CommandSourceStack> dagitici = yardimci.getLevel().getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> kok = dagitici.getRoot().getChild(DogalAfetKomutu.KOK);
        yardimci.assertTrue(kok != null, "/dogalafet kayıtlı değil");
        for (String ad : DogalAfetKomutu.CEKIRDEK_ALT_KOMUTLARI) {
            yardimci.assertTrue(kok.getChild(ad) != null, "/dogalafet " + ad + " kayıtlı değil");
        }

        CommandSourceStack kaynak = yardimci.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        yardimci.assertTrue(!kok.canUse(kaynak.withPermission(DogalAfetKomutu.YETKI_SEVIYESI - 1)),
                "OP olmayan /dogalafet kullanabiliyor");
        yardimci.assertTrue(kok.canUse(kaynak.withPermission(DogalAfetKomutu.YETKI_SEVIYESI)),
                "OP /dogalafet kullanamıyor");

        int sonuc = calistir(yardimci, DogalAfetKomutu.KOK + " " + DogalAfetKomutu.DURUM);
        yardimci.assertTrue(sonuc == 1, "/dogalafet durum sonucu 1 değil: " + sonuc);
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "cekirdek_komut_start")
    public static void startIkinciAfetiReddeder(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(yardimci.getLevel().getServer());
        yonetici.durdur();
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 8, 100))) {
            yardimci.assertTrue(calistir(yardimci, "dogalafet start deprem 2") == 1, "ilk start başarısız");
            Afet afet = yonetici.aktifAfet().orElse(null);
            yardimci.assertTrue(afet != null && afet.tur() == AfetTuru.DEPREM && afet.seviye() == 2,
                    "deprem seviye 2 aktif değil");
            yardimci.assertTrue(calistir(yardimci, "dogalafet start deprem 1") == 0, "ikinci afet reddedilmedi");
            yardimci.assertTrue(yonetici.aktifAfet().orElse(null) == afet, "ikinci start aktif afeti değiştirdi");
            yardimci.assertTrue(calistir(yardimci, "dogalafet asama ileri") == 1, "asama ileri başarısız");
            yardimci.assertTrue(calistir(yardimci, "dogalafet stop") == 1, "stop başarısız");
            yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "stop afeti durdurmadı");
            yardimci.assertTrue(calistir(yardimci, "dogalafet stop") == 0, "afet yokken stop 0 dönmeli");
        } finally {
            yonetici.durdur();
        }
        if (!AfetKaydi.kayitliMi(AfetTuru.TSUNAMI)) {
            yardimci.assertTrue(calistir(yardimci, "dogalafet start tsunami") == 0, "kayıtlı olmayan afet başlatıldı");
            yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "kayıtlı olmayan afet aktif oldu");
        }
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "cekirdek_komut_sifirla")
    public static void sifirlaDonemiIlkHalineDondurur(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = DunyaDonemi.al(server);
        donem.ayVarAyarla(false);
        donem.gunesEvresiAyarla(GunesEvresi.KIZIL_CUCE);
        donem.evreIlerlemesiAyarla(0.5f);
        donem.donmaIlerlemesiAyarla(80f);
        donem.cozulmeDonemiAyarla(true);
        donem.buharlasmaDerinligiAyarla(3f);
        donem.denizAyarla(new DenizSeviyesi(4f, 2f, 100L, false));
        donem.carpanEkle(AfetTuru.TSUNAMI, 3.5, 1_000, 1_000, SonumTipi.DOGRUSAL);
        donem.gunOfsetiAyarla(90);
        donem.ekVeri("test").putInt("deger", 1);
        yardimci.assertTrue(calistir(yardimci, "dogalafet hiz 5") == 1, "hiz başarısız");
        yardimci.assertTrue(calistir(yardimci, "dogalafet gun 42") == 1, "gun başarısız");
        yardimci.assertTrue(yonetici.oyunGunu() == 42, "gun 42 ayarlanmadı: " + yonetici.oyunGunu());
        server.overworld().setDayTime(server.overworld().getDayTime() + 72_000);
        yardimci.assertTrue(yonetici.oyunGunu() == 42, "dünya saati ilerletilince oyun günü değişmemeli: " + yonetici.oyunGunu());
        server.overworld().setDayTime(1_000);
        yardimci.assertTrue(yonetici.oyunGunu() == 42, "/time set day gibi geri alınca oyun günü değişmemeli");
        long uykuOnce = donem.uykuBirikimi();
        long gunSaati = server.overworld().getDayTime();
        ForgeEventFactory.onSleepFinished(server.overworld(), gunSaati + 2 * 24_000L, gunSaati);
        yardimci.assertTrue(donem.uykuBirikimi() == uykuOnce + 48_000, "uyku birikmedi: " + donem.uykuBirikimi());
        yardimci.assertTrue(yonetici.oyunGunu() == 44, "iki günlük uyku oyun gününü 2 ilerletmeli: " + yonetici.oyunGunu());
        net.minecraft.nbt.CompoundTag ekVeri = donem.ekVeri("test");

        yardimci.assertTrue(calistir(yardimci, "dogalafet sifirla") == 1, "sifirla başarısız");
        yardimci.assertTrue(donem.ayVar(), "ay geri gelmedi");
        yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL && donem.evreIlerlemesi() == 0f, "güneş");
        yardimci.assertTrue(donem.donmaIlerlemesi() == 0f && !donem.cozulmeDonemi(), "donma");
        yardimci.assertTrue(donem.buharlasmaDerinligi() == 0f, "buharlaşma");
        yardimci.assertTrue(donem.deniz().equals(DenizSeviyesi.VARSAYILAN), "deniz");
        yardimci.assertTrue(donem.carpanlar().isEmpty(), "çarpanlar");
        yardimci.assertTrue(donem.gunOfseti() == 0, "gün ofseti");
        yardimci.assertTrue(donem.uykuBirikimi() == uykuOnce + 48_000, "sifirla uyku birikimini korumalı: " + donem.uykuBirikimi());
        yardimci.assertTrue(yonetici.oyunGunu() == OyunGunu.gun(server.overworld().getGameTime(), donem.uykuBirikimi(), 0),
                "sifirla sonrası gün = (oyun zamanı + uyku) günü: " + yonetici.oyunGunu());
        yardimci.assertTrue(donem.sonAfetler().isEmpty() && donem.sonHedef().isEmpty() && donem.sonSeviye() == 0,
                "geçmiş");
        yardimci.assertTrue(donem.genelBeklemeKalan() == 0, "bekleme");
        yardimci.assertTrue(donem.ekVeri("test").isEmpty(), "ek veri");
        yardimci.assertTrue(donem.ekVeri("test") == ekVeri && ekVeri.isEmpty(), "ek veri referansı sıfırlamada kopmamalı");
        yardimci.assertTrue(yonetici.zamanOlcegi().carpan() == 1.0, "hız 1'e dönmedi");
        yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "aktif afet kaldı");
        yardimci.succeed();
    }

    /**
     * Kullanıcı şikayeti (K-E67): "{@code /dogalafet gun} ile günü artırsak bile seviye 4-5 %0 gösteriyor". Komutla başlatılan
     * seviye 5 afet bitince sonraki doğal seçimin S4/S5 olasılığı sıfırlanmaz (önceki doğal afet sayılmaz); {@code gun}
     * çıktısı olasılık yazar; {@code afetMaksSiddet} üstündeki komut seviyesi uygulanır ve çıktıda belirtilir; {@code durum}
     * %0 olan seviyenin nedenini yazar.
     */
    @GameTest(template = "bos", batch = "cekirdek_komut_seviye")
    public static void elleSeviyeBesSonrakiOlasiliklariSifirlamaz(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        GameRules.IntegerValue maks = server.getGameRules().getRule(Kurallar.AFET_MAKS_SIDDET);
        int oncekiMaks = maks.get();
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 100))) {
            yonetici.sifirla();
            maks.set(5, server);
            yardimci.assertTrue(calistir(yardimci, "dogalafet gun 100") == 1, "gun 100 başarısız");
            yardimci.assertTrue(calistir(yardimci, "dogalafet start kasirga 5") == 1, "start kasirga 5 başarısız");
            yardimci.assertTrue(yonetici.aktifAfet(AfetTuru.KASIRGA).map(Afet::seviye).orElse(0) == 5, "seviye 5 değil");
            yardimci.assertTrue(calistir(yardimci, "dogalafet stop") == 1, "stop başarısız");
            yardimci.assertTrue(yonetici.donem().sonSeviye() == 0, "komutla başlayan seviye son seviyeye yazılmamalı");
            SeviyeDurumu durum = yonetici.seviyeDurumu(100);
            yardimci.assertTrue(durum.olasilik(4) > 0 && durum.olasilik(5) > 0,
                    "elle S5'ten sonra gün 100'de S4/S5 > 0 olmalı: " + durum.olasilik(4) + " " + durum.olasilik(5));

            List<Component> gun = toplaVeCalistir(yardimci, "dogalafet gun 100");
            Component olasiliklar = arguman(gun, CekirdekKomutlari.GUN_AYARLANDI, 1);
            yardimci.assertTrue(olasiliklar != null, "gun çıktısı yok: " + gun);
            String metin = olasiliklar.getString();
            yardimci.assertTrue(metin.contains("S4 ") && !metin.contains("S4 0.0%") && !metin.contains("S5 0.0%"),
                    "gun 100 çıktısında S4/S5 %0: " + metin);

            maks.set(3, server);
            List<Component> start = toplaVeCalistir(yardimci, "dogalafet start kasirga 5");
            yardimci.assertTrue(yonetici.aktifAfet(AfetTuru.KASIRGA).map(Afet::seviye).orElse(0) == 5,
                    "komut afetMaksSiddet'i aşabilmeli");
            yardimci.assertTrue(arguman(start, CekirdekKomutlari.START_MAKS_ASILDI, 0) != null,
                    "maks aşımı çıktıda belirtilmeli: " + start);
            List<Component> durumSatirlari = toplaVeCalistir(yardimci, "dogalafet durum");
            Component seviyeler = arguman(durumSatirlari, CekirdekKomutlari.DURUM_SEVIYELER, 2);
            yardimci.assertTrue(seviyeler != null && anahtarVar(seviyeler, CekirdekKomutlari.SEVIYE_NEDEN_MAKS),
                    "durum %0 seviyenin nedenini (afetMaksSiddet) yazmalı: " + seviyeler);
        } finally {
            yonetici.durdur();
            maks.set(oncekiMaks, server);
            yonetici.sifirla();
        }
        yardimci.succeed();
    }

    /** Komutu çıktısı toplanan OP kaynağıyla çalıştırır; başarı ve hata satırlarını döner. */
    private static List<Component> toplaVeCalistir(GameTestHelper yardimci, String komut) {
        MinecraftServer server = yardimci.getLevel().getServer();
        List<Component> satirlar = new ArrayList<>();
        CommandSource toplayici = new CommandSource() {
            @Override
            public void sendSystemMessage(Component mesaj) {
                satirlar.add(mesaj);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        try {
            server.getCommands().getDispatcher().execute(komut, server.createCommandSourceStack().withSource(toplayici)
                    .withPermission(DogalAfetKomutu.YETKI_SEVIYESI));
        } catch (CommandSyntaxException hata) {
            yardimci.fail("/" + komut + " çalışmadı: " + hata.getMessage());
        }
        return satirlar;
    }

    /** {@code anahtar} çeviri anahtarlı satırın {@code sira}. argümanı (Component değilse metne çevrilir); yoksa null. */
    private static Component arguman(List<Component> satirlar, String anahtar, int sira) {
        for (Component satir : satirlar) {
            if (satir.getContents() instanceof TranslatableContents t && t.getKey().equals(anahtar)) {
                Object[] arg = t.getArgs();
                if (sira >= arg.length) {
                    return Component.empty();
                }
                return arg[sira] instanceof Component c ? c : Component.literal(String.valueOf(arg[sira]));
            }
        }
        return null;
    }

    /** Bileşende (kardeşleri ve çeviri argümanları dahil) {@code anahtar} çevirisi geçiyor mu. */
    private static boolean anahtarVar(Component bilesen, String anahtar) {
        if (bilesen.getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals(anahtar)) {
                return true;
            }
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component c && anahtarVar(c, anahtar)) {
                    return true;
                }
            }
        }
        for (Component kardes : bilesen.getSiblings()) {
            if (anahtarVar(kardes, anahtar)) {
                return true;
            }
        }
        return false;
    }
}
