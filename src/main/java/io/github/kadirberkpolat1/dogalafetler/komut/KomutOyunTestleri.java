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
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
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
        net.minecraft.nbt.CompoundTag ekVeri = donem.ekVeri("test");

        yardimci.assertTrue(calistir(yardimci, "dogalafet sifirla") == 1, "sifirla başarısız");
        yardimci.assertTrue(donem.ayVar(), "ay geri gelmedi");
        yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL && donem.evreIlerlemesi() == 0f, "güneş");
        yardimci.assertTrue(donem.donmaIlerlemesi() == 0f && !donem.cozulmeDonemi(), "donma");
        yardimci.assertTrue(donem.buharlasmaDerinligi() == 0f, "buharlaşma");
        yardimci.assertTrue(donem.deniz().equals(DenizSeviyesi.VARSAYILAN), "deniz");
        yardimci.assertTrue(donem.carpanlar().isEmpty(), "çarpanlar");
        yardimci.assertTrue(donem.gunOfseti() == 0, "gün ofseti");
        yardimci.assertTrue(donem.sonAfetler().isEmpty() && donem.sonHedef().isEmpty() && donem.sonSeviye() == 0,
                "geçmiş");
        yardimci.assertTrue(donem.genelBeklemeKalan() == 0, "bekleme");
        yardimci.assertTrue(donem.ekVeri("test").isEmpty(), "ek veri");
        yardimci.assertTrue(donem.ekVeri("test") == ekVeri && ekVeri.isEmpty(), "ek veri referansı sıfırlamada kopmamalı");
        yardimci.assertTrue(yonetici.zamanOlcegi().carpan() == 1.0, "hız 1'e dönmedi");
        yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "aktif afet kaldı");
        yardimci.succeed();
    }
}
