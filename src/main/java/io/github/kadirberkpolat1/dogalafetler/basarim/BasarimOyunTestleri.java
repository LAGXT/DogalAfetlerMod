package io.github.kadirberkpolat1.dogalafetler.basarim;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetIzi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.BasarimTuru;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.KutupSayaci;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.BitisNedeni;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.Gokyuzu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Başarımların oyun içi testleri (GOREV §6 #16; 27 başarımın her biri en az bir testte kazanılır, tablo rapor §6 #16):
 * gerçek afet yaşam döngüsü ({@link OyunTestiAfeti}, geçici kayıt; sunucu geneli güneş ve ay dahil), sahte oyuncular ve
 * datagen'le üretilmiş advancement'lar. Faz 4 afetinin iç davranışına bağlı koşullar (#2, #4, #5, #6) afetin
 * yayınlayacağı {@link BasarimIpucuEvent} testten yayınlanarak sınanır. Her test kendi oyuncularını açar; tek afet
 * yuvasını ya da dünya dönemini kullanan testler kendi {@code batch}'indedir. Sahte oyuncular tick almadığı için
 * saniyelik denetim doğrudan çağrılır. Toast ve sohbet duyurusu vanilla'dır (JSON'da {@code show_toast}, {@code
 * announce_to_chat}); burada kazanım, görünürlük, ödül, ünvan ve duyurudaki ad sınanır.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BasarimOyunTestleri {
    private static final double YARICAP = 16;

    private BasarimOyunTestleri() {
    }

    private static ServerPlayer oyuncu(GameTestHelper y, String ad, double dx) {
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        GameProfile profil = new GameProfile(UUID.randomUUID(), ad);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, profil, m.getX() + 0.5 + dx, m.getY(), m.getZ() + 0.5);
        o.setGameMode(GameType.SURVIVAL);
        return o;
    }

    /** Göğü gören sahte oyuncu: sahnenin 40 blok üstünde (N16 gökyüzü ölçütleri). */
    private static ServerPlayer gokteOyuncu(GameTestHelper y, String ad, double dx) {
        ServerPlayer o = oyuncu(y, ad, dx);
        o.moveTo(o.getX(), o.getY() + 40, o.getZ());
        return o;
    }

    private static void cikar(GameTestHelper y, List<ServerPlayer> oyuncular) {
        for (ServerPlayer o : oyuncular) {
            y.getLevel().getServer().getPlayerList().remove(o);
        }
    }

    private static void dogrula(GameTestHelper y, boolean kosul, String mesaj) {
        if (!kosul) {
            y.fail(mesaj);
        }
    }

    private static Advancement advancement(GameTestHelper y, Basarimlar basarim) {
        Advancement a = y.getLevel().getServer().getAdvancements().getAdvancement(BasarimKimlikleri.basarim(basarim));
        if (a == null) {
            y.fail("advancement yüklenmedi (runData?): " + BasarimKimlikleri.basarim(basarim));
        }
        return a;
    }

    private static boolean kazandi(GameTestHelper y, ServerPlayer o, Basarimlar basarim) {
        return o.getAdvancements().getOrStartProgress(advancement(y, basarim)).isDone();
    }

    /** Vanilla görünürlük kuralı ({@code PlayerAdvancements} ile aynı değerlendirici). */
    private static boolean gorunur(GameTestHelper y, ServerPlayer o, Basarimlar basarim) {
        Advancement hedef = advancement(y, basarim);
        Map<Advancement, Boolean> sonuc = new HashMap<>();
        AdvancementVisibilityEvaluator.evaluateVisibility(hedef.getRoot(),
                a -> o.getAdvancements().getOrStartProgress(a).isDone(), sonuc::put);
        return Boolean.TRUE.equals(sonuc.get(hedef));
    }

    /**
     * {@code tur} sınama afetini (OyunTestiAfeti, yarıçap 16; sunucu geneli türde her yer) {@code hedef} merkezinde
     * başlatır: SIREN → AKTIF → {@code araKontrol} → BITIS → doğal son, ya da ({@code stop}) AKTIF'te durdurur.
     */
    private static void afet(GameTestHelper y, AfetTuru tur, int seviye, ServerPlayer hedef, boolean stop,
            Runnable araKontrol) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(tur, () -> new OyunTestiAfeti(tur, YARICAP, 100))) {
            AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(tur, seviye, hedef, level,
                    y.absolutePos(BlockPos.ZERO));
            dogrula(y, sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, tur.kimlik() + " başlamadı: " + sonuc);
            Afet afet = yonetici.aktifAfet().orElseThrow();
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.SIREN, "siren aşamasına geçmedi");
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.AKTIF, "aktif aşamaya geçmedi");
            araKontrol.run();
            if (stop) {
                yonetici.durdur();
            } else {
                for (int i = 0; i < 2 && yonetici.aktifAfet().isPresent(); i++) {
                    yonetici.asamaIleri();
                }
            }
            dogrula(y, yonetici.aktifAfet().isEmpty(), "afet sona ermedi");
        } finally {
            yonetici.durdur();
        }
    }

    private static void kasirga(GameTestHelper y, int seviye, ServerPlayer hedef, boolean stop, Runnable araKontrol) {
        afet(y, AfetTuru.KASIRGA, seviye, hedef, stop, araKontrol);
    }

    /** Süren afetin numarası ({@code AfetBittiEvent.baslangicSaati}). */
    private static long afetNo(GameTestHelper y) {
        return AfetYoneticisi.al(y.getLevel().getServer()).aktifAfet().orElseThrow().baglam().baslangicSaati();
    }

    private static void ipucu(ServerPlayer o, Ipucu ipucu) {
        MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(o, ipucu));
    }

    @GameTest(template = "bos", batch = "basarim_siren")
    public static void sirenKokBasariminiAcarSekmeGorunur(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "siren-a", 0);
        try {
            dogrula(y, !kazandi(y, a, Basarimlar.KOK) && !gorunur(y, a, Basarimlar.ILK_RUZGAR),
                    "başta kök açık olmamalı, sekme görünmemeli");
            kasirga(y, 1, a, true, () -> dogrula(y, kazandi(y, a, Basarimlar.KOK), "sirenle kök açılmalı"));
            dogrula(y, gorunur(y, a, Basarimlar.KOK) && gorunur(y, a, Basarimlar.ILK_RUZGAR)
                    && gorunur(y, a, Basarimlar.SUPERNOVA_TANIGI), "kök açılınca çocuklar ve torunlar görünür");
            dogrula(y, !gorunur(y, a, Basarimlar.BUZUL_CAGI), "üç kat aşağısı henüz görünmez (vanilla kuralı)");
            dogrula(y, !gorunur(y, a, Basarimlar.BIRLIKTE_HAYATTA) && !gorunur(y, a, Basarimlar.HEPSINI_ATLATTIM),
                    "gizliler açılana kadar görünmez");
        } finally {
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_kasirga")
    public static void dogalBitenKasirgaIlkRuzgariYalnizAlandakineVerir(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "ruzgar-a", 0);
        ServerPlayer b = oyuncu(y, "ruzgar-b", YARICAP + 8);
        try {
            kasirga(y, 1, a, false, () -> { });
            dogrula(y, kazandi(y, a, Basarimlar.ILK_RUZGAR), "alandaki oyuncu İlk Rüzgâr'ı almalı");
            dogrula(y, !kazandi(y, b, Basarimlar.ILK_RUZGAR), "alan dışındaki oyuncu almamalı (oyuncu başına)");
            dogrula(y, !kazandi(y, a, Basarimlar.FIRTINAYA_MEYDAN_OKU), "seviye 1 Fırtınaya Meydan Oku vermez");
            dogrula(y, BasarimVerisi.atlatilanlar(a).iceriyor(AfetTuru.KASIRGA), "kasırga atlatılanlara yazılmalı");
            dogrula(y, BasarimVerisi.atlatilanlar(b).boyut() == 0, "alan dışı oyuncunun kümesi boş");
            int beklenen = BasarimTuru.GOREV.varsayilanXp() * 2;
            dogrula(y, a.totalExperience >= beklenen, "XP ödülü (kök + İlk Rüzgâr): " + a.totalExperience);
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_stop")
    public static void stopIleBitenAfetSagCikmaSayilmaz(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "stop-a", 0);
        try {
            kasirga(y, 5, a, true, () -> { });
            dogrula(y, kazandi(y, a, Basarimlar.KOK), "siren duyuldu");
            dogrula(y, !kazandi(y, a, Basarimlar.ILK_RUZGAR) && !kazandi(y, a, Basarimlar.FELAKET_USTASI),
                    "stop ile biten afet (dogalBitti false) sayılmaz");
            dogrula(y, BasarimVerisi.atlatilanlar(a).boyut() == 0, "stop atlatılanlara yazılmaz");
        } finally {
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_seviye3")
    public static void seviyeUcFirtinayaMeydanOkuVeHedefOdulu(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "siddet-a", 0);
        try {
            kasirga(y, 3, a, false, () -> { });
            dogrula(y, kazandi(y, a, Basarimlar.FIRTINAYA_MEYDAN_OKU), "seviye 3 Fırtınaya Meydan Oku verir");
            dogrula(y, !kazandi(y, a, Basarimlar.FELAKET_USTASI), "seviye 3 Felaket Ustası vermez");
            // Yiyecek dalı birleşip runData unutulursa (tablo hâlâ yedek) kırmızıya döner (inceleme A5).
            Item beklenen = BasarimKimlikleri.esyaYaDaYedek(BasarimKimlikleri.SICAK_SALEP, Items.BAKED_POTATO);
            int yiyecek = 0;
            for (int i = 0; i < a.getInventory().getContainerSize(); i++) {
                ItemStack yigin = a.getInventory().getItem(i);
                if (yigin.isEdible()) {
                    dogrula(y, yigin.is(beklenen), "hedef ödülü " + beklenen + " olmalı: " + yigin);
                    yiyecek += yigin.getCount();
                }
            }
            dogrula(y, yiyecek >= 1 && yiyecek <= 2, "hedef ödülü 1-2 sıcak yiyecek: " + yiyecek);
            dogrula(y, BasarimVerisi.unvan(a).isEmpty(), "hedef ünvan vermez");
        } finally {
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_seviye5")
    public static void seviyeBesFelaketUstasiUnvanVeXp(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "usta-a", 0);
        String[] duyurudakiAd = new String[1];
        Consumer<AdvancementEvent.AdvancementEarnEvent> dinleyici = olay -> {
            if (olay.getEntity() == a
                    && olay.getAdvancement().getId().equals(BasarimKimlikleri.basarim(Basarimlar.FELAKET_USTASI))) {
                duyurudakiAd[0] = a.getDisplayName().getString();
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, AdvancementEvent.AdvancementEarnEvent.class,
                dinleyici);
        try {
            kasirga(y, 5, a, false, () -> { });
            dogrula(y, kazandi(y, a, Basarimlar.FELAKET_USTASI) && kazandi(y, a, Basarimlar.FIRTINAYA_MEYDAN_OKU),
                    "seviye 5 Felaket Ustası ve Fırtınaya Meydan Oku verir");
            dogrula(y, BasarimVerisi.unvan(a).orElse(null) == Unvan.FELAKET_USTASI, "meydan okuma ünvan verir");
            String unvan = Component.translatable(Unvan.FELAKET_USTASI.ceviriAnahtari()).getString();
            String sohbet = a.getDisplayName().getString();
            dogrula(y, sohbet.startsWith("[" + unvan + "] ") && sohbet.endsWith("usta-a"), "sohbet adı: " + sohbet);
            Component tab = a.getTabListDisplayName();
            dogrula(y, tab != null && tab.getString().startsWith("[" + unvan + "] "), "tab adı: " + tab);
            // Kazanım olayı sohbet duyurusundan hemen sonra gelir: o anki ad duyurudaki addır (A-32).
            dogrula(y, duyurudakiAd[0] != null && duyurudakiAd[0].startsWith("[" + unvan + "] "),
                    "meydan okumanın kendi duyurusu önekli ad taşımalı: " + duyurudakiAd[0]);
            int beklenen = BasarimTuru.GOREV.varsayilanXp() * 2 + BasarimTuru.HEDEF.varsayilanXp()
                    + BasarimTuru.MEYDAN_OKUMA.varsayilanXp();
            dogrula(y, a.totalExperience >= beklenen, "XP (kök, İlk Rüzgâr, hedef, meydan okuma): "
                    + a.totalExperience);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_birlikte")
    public static void ucOyuncuBirlikteHayattaGizliyidiAcilincaGorunur(GameTestHelper y) {
        List<ServerPlayer> oyuncular = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            oyuncular.add(oyuncu(y, "birlikte-" + i, i * 2.0));
        }
        try {
            kasirga(y, 2, oyuncular.get(0), false, () -> {
                for (ServerPlayer o : oyuncular) {
                    dogrula(y, kazandi(y, o, Basarimlar.KOK), "kök açık");
                    dogrula(y, !gorunur(y, o, Basarimlar.BIRLIKTE_HAYATTA), "gizli: açılmadan görünmez");
                }
            });
            for (ServerPlayer o : oyuncular) {
                dogrula(y, kazandi(y, o, Basarimlar.BIRLIKTE_HAYATTA), "3 sağ çıkan: Birlikte Hayatta");
                dogrula(y, gorunur(y, o, Basarimlar.BIRLIKTE_HAYATTA), "açılınca görünür");
            }
        } finally {
            cikar(y, oyuncular);
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_birlikte_iki")
    public static void ikiOyuncuBirlikteHayattaVermez(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "ikili-a", 0);
        ServerPlayer b = oyuncu(y, "ikili-b", 2);
        try {
            kasirga(y, 1, a, false, () -> { });
            dogrula(y, kazandi(y, a, Basarimlar.ILK_RUZGAR) && kazandi(y, b, Basarimlar.ILK_RUZGAR), "ikisi de sağ");
            dogrula(y, !kazandi(y, a, Basarimlar.BIRLIKTE_HAYATTA) && !kazandi(y, b, Basarimlar.BIRLIKTE_HAYATTA),
                    "2 oyuncu Birlikte Hayatta için yetmez");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_olen")
    public static void afetteOlenSagCikmaz(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "olen-a", 0);
        ServerPlayer b = oyuncu(y, "kalan-b", 2);
        try {
            kasirga(y, 1, b, false, a::kill);
            dogrula(y, !kazandi(y, a, Basarimlar.ILK_RUZGAR), "afet sürerken ölen sağ çıkmaz");
            dogrula(y, kazandi(y, b, Basarimlar.ILK_RUZGAR), "ölmeyen sağ çıkar");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_donma")
    public static void donmaAltmisBuzulCagi(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "buzul-a", 0);
        DunyaDonemi donem = DunyaDonemi.al(y.getLevel().getServer());
        float eski = donem.donmaIlerlemesi();
        try {
            donem.donmaIlerlemesiAyarla(40);
            dogrula(y, !kazandi(y, a, Basarimlar.BUZUL_CAGI), "%40 Buzul Çağı vermez");
            donem.donmaIlerlemesiAyarla(60);
            dogrula(y, kazandi(y, a, Basarimlar.BUZUL_CAGI), "%60 (%50'yi geçti) Buzul Çağı verir");
        } finally {
            donem.donmaIlerlemesiAyarla(eski);
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_kutup")
    public static void kutupSiginagiUcGunSifirlaYalnizAraSayaciSifirlar(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "kutup-a", 0);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        DunyaDonemi donem = yonetici.donem();
        try {
            yonetici.sifirla();
            donem.donmaIlerlemesiAyarla(100);
            BasarimVerisi.kutupYaz(a, new KutupSayaci(3 * KutupSayaci.GUN_TICK - 40, KutupSayaci.YOK));
            BasarimOlaylari.saniyelikDenetim(a);
            dogrula(y, !kazandi(y, a, Basarimlar.KUTUP_SIGINAGI), "3 günden az yetmez");
            BasarimVerisi.kutupYaz(a, new KutupSayaci(3 * KutupSayaci.GUN_TICK, KutupSayaci.YOK));
            BasarimOlaylari.saniyelikDenetim(a);
            dogrula(y, kazandi(y, a, Basarimlar.KUTUP_SIGINAGI), "donma %100'den beri 3 gün: Kutup Sığınağı");
            dogrula(y, BasarimVerisi.unvan(a).orElse(null) == Unvan.KUTUP_SAKINI, "meydan okuma ünvanı");
            AfetKumesi kume = new AfetKumesi();
            kume.ekle(AfetTuru.KASIRGA);
            BasarimVerisi.atlatilanlariYaz(a, kume);
            BasarimVerisi.balikEkle(a, 3);
            BasarimVerisi.kutupYaz(a, new KutupSayaci(KutupSayaci.GUN_TICK, KutupSayaci.YOK));
            yonetici.sifirla();
            dogrula(y, donem.donmaIlerlemesi() == 0, "sifirla donmayı sıfırlar");
            dogrula(y, BasarimVerisi.kutup(a).birikim() == 0, "K-E11: döneme bağlı ara sayaç sıfırlanır");
            dogrula(y, kazandi(y, a, Basarimlar.KUTUP_SIGINAGI) && kazandi(y, a, Basarimlar.BUZUL_CAGI),
                    "kazanılmış başarımlar kalır");
            dogrula(y, BasarimVerisi.unvan(a).orElse(null) == Unvan.KUTUP_SAKINI
                    && BasarimVerisi.atlatilanlar(a).iceriyor(AfetTuru.KASIRGA) && BasarimVerisi.balik(a) == 3,
                    "oyuncu verisindeki kalıcı ilerleme (ünvan, atlatılanlar, balık) kalır");
            BasarimVerisi.kutupYaz(a, new KutupSayaci(500, KutupSayaci.YOK));
            dogrula(y, BasarimVerisi.kutup(a).birikim() == 500, "yeni döngüde yeniden birikir");
        } finally {
            yonetici.sifirla();
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void besTuyKatmanliGiyimOyuncuBasina(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "tuy-a", 0);
        ServerPlayer b = oyuncu(y, "tuy-b", 2);
        try {
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FEATHER, 8));
            a.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_CHESTPLATE));
            b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FEATHER, 8));
            b.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_CHESTPLATE));
            for (int i = 0; i < 4; i++) {
                ForgeHooks.onItemRightClick(a, InteractionHand.MAIN_HAND);
            }
            ForgeHooks.onItemRightClick(b, InteractionHand.MAIN_HAND);
            int tuy = ZirhEkleri.tuySayisi(a.getOffhandItem());
            dogrula(y, tuy == 4, "4 tüy: " + tuy);
            dogrula(y, !kazandi(y, a, Basarimlar.KATMANLI_GIYIM), "4 tüy yetmez");
            ForgeHooks.onItemRightClick(a, InteractionHand.MAIN_HAND);
            dogrula(y, ZirhEkleri.tuySayisi(a.getOffhandItem()) == 5, "5 tüy");
            dogrula(y, kazandi(y, a, Basarimlar.KATMANLI_GIYIM), "5 tüy Katmanlı Giyim verir");
            dogrula(y, !kazandi(y, b, Basarimlar.KATMANLI_GIYIM), "diğer oyuncu (1 tüy) almaz");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void dortYunluParcaYunluKahraman(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "yun-a", 0);
        try {
            EquipmentSlot[] yuvalar = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            ItemStack[] zirhlar = {new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};
            for (int i = 0; i < 4; i++) {
                if (i < 3) {
                    ZirhEkleri.koy(zirhlar[i], ZirhEkleri.YUNLU);
                }
                a.setItemSlot(yuvalar[i], zirhlar[i]);
            }
            BasarimOlaylari.saniyelikDenetim(a);
            dogrula(y, !kazandi(y, a, Basarimlar.YUNLU_KAHRAMAN), "3 yünlü parça yetmez");
            ItemStack bot = a.getItemBySlot(EquipmentSlot.FEET);
            ZirhEkleri.koy(bot, ZirhEkleri.YUNLU);
            BasarimOlaylari.saniyelikDenetim(a);
            dogrula(y, kazandi(y, a, Basarimlar.YUNLU_KAHRAMAN), "4 yünlü parça Yünlü Kahraman verir");
        } finally {
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    /**
     * Eşya kayıtlıysa gerçek {@code ItemCraftedEvent}; eşya kardeş dalda (bu dalda kayıtsız) ise olay dinleyicisinin
     * çağırdığı kimlik yolu.
     */
    private static void uret(ServerPlayer o, ResourceLocation kimlik) {
        if (ForgeRegistries.ITEMS.containsKey(kimlik)) {
            ItemStack esya = new ItemStack(ForgeRegistries.ITEMS.getValue(kimlik));
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(o, esya, new SimpleContainer(9)));
        } else {
            BasarimOlaylari.esyaUretildi(o, kimlik);
        }
    }

    /** Yerden alma: kayıtlıysa gerçek {@code ItemPickupEvent}, değilse dinleyicinin kimlik yolu ({@link #uret}). */
    private static void topla(ServerPlayer o, ResourceLocation kimlik) {
        if (ForgeRegistries.ITEMS.containsKey(kimlik)) {
            ItemStack esya = new ItemStack(ForgeRegistries.ITEMS.getValue(kimlik));
            ItemEntity yerde = new ItemEntity(o.level(), o.getX(), o.getY(), o.getZ(), esya.copy());
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemPickupEvent(o, yerde, esya));
        } else {
            BasarimOlaylari.esyaToplandi(o, kimlik);
        }
    }

    @GameTest(template = "bos")
    public static void meteorEsyalariToplanincaGoktenGelenMisafirVeKozmikSans(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "meteor-a", 0);
        ServerPlayer b = oyuncu(y, "meteor-b", 2);
        try {
            ItemStack ates = new ItemStack(Items.FIRE_CHARGE);
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemPickupEvent(a,
                    new ItemEntity(a.level(), a.getX(), a.getY(), a.getZ(), ates.copy()), ates));
            dogrula(y, !kazandi(y, a, Basarimlar.GOKTEN_GELEN_MISAFIR), "başka eşya (ateş topu) sayılmaz");
            topla(a, BasarimKimlikleri.METEORIT_PARCASI);
            dogrula(y, kazandi(y, a, Basarimlar.GOKTEN_GELEN_MISAFIR), "Meteorit Parçası Gökten Gelen Misafir verir");
            dogrula(y, !kazandi(y, a, Basarimlar.KOZMIK_SANS), "parça Kozmik Şans vermez");
            dogrula(y, !kazandi(y, b, Basarimlar.GOKTEN_GELEN_MISAFIR), "diğer oyuncu almaz");
            topla(a, BasarimKimlikleri.KOZMIK_CEKIRDEK);
            dogrula(y, kazandi(y, a, Basarimlar.KOZMIK_SANS), "Kozmik Çekirdek Kozmik Şans verir");
            dogrula(y, BasarimVerisi.unvan(a).orElse(null) == Unvan.KOZMIK_KASIF, "meydan okuma ünvanı");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void radarUretimiRadarAltinda(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "radar-a", 0);
        ServerPlayer b = oyuncu(y, "radar-b", 2);
        try {
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(a, new ItemStack(Items.COMPASS),
                    new SimpleContainer(9)));
            dogrula(y, !kazandi(y, a, Basarimlar.RADAR_ALTINDA), "pusula üretimi sayılmaz");
            uret(a, BasarimKimlikleri.HAVA_RADARI);
            dogrula(y, kazandi(y, a, Basarimlar.RADAR_ALTINDA), "Hava Radarı üretimi Radar Altında verir");
            dogrula(y, !kazandi(y, a, Basarimlar.YER_DINLEYICI), "radar Yer Dinleyici vermez");
            dogrula(y, !kazandi(y, b, Basarimlar.RADAR_ALTINDA), "diğer oyuncu almaz");
            uret(b, BasarimKimlikleri.SISMOGRAF);
            dogrula(y, kazandi(y, b, Basarimlar.YER_DINLEYICI), "Sismograf üretimi Yer Dinleyici verir");
            dogrula(y, !kazandi(y, b, Basarimlar.RADAR_ALTINDA), "sismograf Radar Altında vermez");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void buzDeligiBaliklariBirikirKutupUstasiOyuncuBasina(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "balik-a", 0);
        ServerPlayer b = oyuncu(y, "balik-b", 2);
        try {
            BasarimOlaylari.buzDeligiBaligi(a, 0);
            dogrula(y, !kazandi(y, a, Basarimlar.BUZ_BALIKCISI), "balıksız çekme (çöp) sayılmaz");
            BasarimOlaylari.buzDeligiBaligi(a, 1);
            dogrula(y, kazandi(y, a, Basarimlar.BUZ_BALIKCISI), "ilk balık Buz Balıkçısı verir");
            for (int i = 0; i < 23; i++) {
                BasarimOlaylari.buzDeligiBaligi(a, 1);
            }
            dogrula(y, BasarimVerisi.balik(a) == 24 && !kazandi(y, a, Basarimlar.KUTUP_USTASI), "24 balık yetmez");
            BasarimOlaylari.buzDeligiBaligi(b, 3);
            BasarimOlaylari.buzDeligiBaligi(a, 1);
            dogrula(y, kazandi(y, a, Basarimlar.KUTUP_USTASI), "toplam 25 balık Kutup Ustası verir");
            dogrula(y, BasarimVerisi.balik(b) == 3 && kazandi(y, b, Basarimlar.BUZ_BALIKCISI)
                    && !kazandi(y, b, Basarimlar.KUTUP_USTASI), "diğer oyuncunun toplamı ayrı");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    /** Sentetik bitiş olayı (yalnız başarım dinleyicisine verilir; diğer alt sistemler görmez). */
    private static void bitti(ServerPlayer o, AfetTuru tur, int seviye, long no, BitisNedeni neden) {
        Set<UUID> kim = Set.of(o.getUUID());
        BasarimOlaylari.afetBitti(new AfetBittiEvent(tur, seviye, o.blockPosition(), Level.OVERWORLD, kim, kim, no,
                no + 1, neden));
    }

    @GameTest(template = "bos")
    public static void sekizTurdenSagCikanHepsiniAtlattim(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "hepsi-a", 0);
        try {
            long no = -9_000_000L;
            AfetTuru[] turler = AfetTuru.values();
            for (int i = 0; i < turler.length - 1; i++) {
                bitti(a, turler[i], turler[i].seviyeli() ? 1 : 0, no--, BitisNedeni.DOGAL);
            }
            AfetTuru son = turler[turler.length - 1];
            dogrula(y, !kazandi(y, a, Basarimlar.HEPSINI_ATLATTIM), "7 tür yetmez");
            bitti(a, son, son.seviyeli() ? 1 : 0, no--, BitisNedeni.KOMUT);
            dogrula(y, !kazandi(y, a, Basarimlar.HEPSINI_ATLATTIM) && !gorunur(y, a, Basarimlar.HEPSINI_ATLATTIM),
                    "stop ile biten 8. tür sayılmaz; gizli kalır");
            bitti(a, son, son.seviyeli() ? 1 : 0, no--, BitisNedeni.DOGAL);
            dogrula(y, kazandi(y, a, Basarimlar.HEPSINI_ATLATTIM), "sekiz tür Hepsini Atlattım verir");
            dogrula(y, gorunur(y, a, Basarimlar.HEPSINI_ATLATTIM), "açılınca görünür");
            dogrula(y, BasarimVerisi.unvan(a).orElse(null) == Unvan.HER_SEYI_ATLATAN, "meydan okuma ünvanı");
            dogrula(y, kazandi(y, a, Basarimlar.GUNESI_ATLATTIM) && kazandi(y, a, Basarimlar.AY_DUSTU)
                    && kazandi(y, a, Basarimlar.SALLANAN_ZEMIN), "tür başarımları");
            dogrula(y, kazandi(y, a, Basarimlar.CATI_ALTI), "hasar almadan atlatılan asit yağmuru Çatı Altı verir");
            dogrula(y, !kazandi(y, a, Basarimlar.DALGA_AVCISI) && !kazandi(y, a, Basarimlar.DELIKTEN_DONUS)
                    && !kazandi(y, a, Basarimlar.YANMADAN_CIKTIM), "ek koşulu (sürüklenme, çukur, 30 sn) olmayanlar");
            dogrula(y, !kazandi(y, a, Basarimlar.FIRTINAYA_MEYDAN_OKU), "seviye 1 ve seviyesiz afetler");
        } finally {
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    /** Olayı hiç gelmeyen (sentetik) afette yedek tahmin sayılır; hasar tahmini Çatı Altı'nı bozar (A-10, A-31). */
    @GameTest(template = "bos")
    public static void olaysizAfetteYedekTahminSayilir(GameTestHelper y) {
        ServerPlayer hasarli = oyuncu(y, "asit-a", 0);
        ServerPlayer kosullu = oyuncu(y, "iz-b", 2);
        try {
            long no = -8_000_000L;
            AfetIzi iz = new AfetIzi();
            iz.afeteBagla(no);
            iz.isaretle(Ipucu.HASAR_ALDI, false, 0);
            BasarimVerisi.afetIziYaz(hasarli, iz);
            bitti(hasarli, AfetTuru.ASIT_YAGMURU, 2, no, BitisNedeni.DOGAL);
            dogrula(y, !kazandi(y, hasarli, Basarimlar.CATI_ALTI), "asit yağmurunda hasar alan Çatı Altı almaz");
            Set<Basarimlar> beklenen = new HashSet<>();
            AfetTuru[] turler = {AfetTuru.TSUNAMI, AfetTuru.SINKHOLE, AfetTuru.ORMAN_YANGINI};
            for (AfetTuru tur : turler) {
                AfetIzi k = new AfetIzi();
                k.afeteBagla(--no);
                if (tur == AfetTuru.TSUNAMI) {
                    k.isaretle(Ipucu.SURUKLENDI, false, 0);
                } else if (tur == AfetTuru.SINKHOLE) {
                    k.isaretle(Ipucu.CUKURA_DUSTU, false, 0);
                } else {
                    for (int s = 0; s < AfetIzi.YANGIN_SANIYESI; s++) {
                        k.isaretle(Ipucu.YANGIN_ALANINDA, false, s);
                    }
                }
                BasarimVerisi.afetIziYaz(kosullu, k);
                bitti(kosullu, tur, 1, no, BitisNedeni.DOGAL);
            }
            beklenen.add(Basarimlar.DALGA_AVCISI);
            beklenen.add(Basarimlar.DELIKTEN_DONUS);
            beklenen.add(Basarimlar.YANMADAN_CIKTIM);
            for (Basarimlar b : beklenen) {
                dogrula(y, kazandi(y, kosullu, b), "izdeki yedek tahmin " + b.yol() + " vermeli");
            }
        } finally {
            cikar(y, List.of(hasarli, kosullu));
        }
        y.succeed();
    }

    private static int komut(MinecraftServer server, String komut) {
        return server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                komut);
    }

    @GameTest(template = "bos")
    public static void unvanKomutuVerirKaldirir(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "komut-a", 0);
        MinecraftServer server = y.getLevel().getServer();
        try {
            int sonuc = komut(server, "dogalafet unvan komut-a kutup_sakini");
            dogrula(y, sonuc == 1 && BasarimVerisi.unvan(a).orElse(null) == Unvan.KUTUP_SAKINI, "komut ünvan verir");
            String unvan = Component.translatable(Unvan.KUTUP_SAKINI.ceviriAnahtari()).getString();
            dogrula(y, a.getDisplayName().getString().startsWith("[" + unvan + "] "), "ad yenilendi");
            komut(server, "dogalafet unvan komut-a yok");
            dogrula(y, BasarimVerisi.unvan(a).isEmpty(), "yok ünvanı kaldırır");
            String ad = a.getDisplayName().getString();
            dogrula(y, ad.equals("komut-a"), "ad öneksiz: " + ad);
            int bilgi = komut(server, "dogalafet basarim bilgi");
            dogrula(y, bilgi == Basarimlar.values().length, "27 başarım yüklü: " + bilgi);
        } finally {
            cikar(y, List.of(a));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_gunes")
    public static void gunesPatlamasiGunesiAtlattimVeSupernovaTanigi(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        ServerPlayer a = gokteOyuncu(y, "gunes-a", 0);
        ServerPlayer b = gokteOyuncu(y, "gunes-b", 2);
        try {
            yonetici.sifirla();
            dogrula(y, Gokyuzu.gokyuzunuGoruyorMu(a), "sahne: a göğü görmeli");
            afet(y, AfetTuru.GUNES_PATLAMASI, 0, null, false, () -> {
                dogrula(y, donem.gunesEvresi() == GunesEvresi.BUYUYEN, "AKTIF: güneş büyüyor");
                BasarimOlaylari.saniyelikDenetim(a);
                dogrula(y, !kazandi(y, a, Basarimlar.SUPERNOVA_TANIGI), "büyüyen güneş süpernova değil");
                b.kill();
            });
            dogrula(y, donem.gunesEvresi() == GunesEvresi.SUPERNOVA, "doğal bitişte süpernova");
            dogrula(y, kazandi(y, a, Basarimlar.GUNESI_ATLATTIM),
                    "sunucu geneli afetten sağ çıkan Güneşi Atlattım alır");
            dogrula(y, kazandi(y, a, Basarimlar.SUPERNOVA_TANIGI), "süpernovaya geçişte göğü gören Süpernova Tanığı");
            dogrula(y, !kazandi(y, b, Basarimlar.GUNESI_ATLATTIM) && !kazandi(y, b, Basarimlar.SUPERNOVA_TANIGI),
                    "süreçte ölen ikisini de almaz");
            dogrula(y, !kazandi(y, a, Basarimlar.FIRTINAYA_MEYDAN_OKU), "seviyesiz afet seviye başarımı vermez");
        } finally {
            yonetici.sifirla();
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_ay")
    public static void ayCarpmasiAyDustuYildizliGeceYukselenSular(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        long oncekiSaat = ust.getDayTime();
        ServerPlayer a = gokteOyuncu(y, "ay-a", 0);
        ServerPlayer b = gokteOyuncu(y, "ay-b", 2);
        try {
            yonetici.sifirla();
            afet(y, AfetTuru.AY_CARPMASI, 0, null, false, () -> { });
            dogrula(y, kazandi(y, a, Basarimlar.AY_DUSTU) && kazandi(y, b, Basarimlar.AY_DUSTU),
                    "ay çarpmasından sağ çıkan herkes Ay Düştü alır");

            donem.ayVarAyarla(false);
            ust.setDayTime(18_000);
            dogrula(y, Gokyuzu.yildizliGeceMi(a), "sahne: ay yok, gece, göğü görüyor");
            for (int i = 0; i < 9; i++) {
                BasarimOlaylari.saniyelikDenetim(a);
            }
            ust.setDayTime(6_000);
            BasarimOlaylari.saniyelikDenetim(a);
            ust.setDayTime(18_000);
            for (int i = 0; i < 9; i++) {
                BasarimOlaylari.saniyelikDenetim(a);
            }
            dogrula(y, !kazandi(y, a, Basarimlar.YILDIZLI_GECE), "gündüzle kesilen 9 + 9 saniye yetmez");
            BasarimOlaylari.saniyelikDenetim(a);
            dogrula(y, kazandi(y, a, Basarimlar.YILDIZLI_GECE), "kesintisiz 10 saniye Yıldızlı Gece verir");

            long dongu = donem.saat() + 1;
            donem.denizAyarla(new DenizSeviyesi(4f, 1.5f, dongu, true));
            b.kill();
            dogrula(y, BasarimVerisi.denizOlum(b) == dongu, "yükselişte ölüm döngüye yazılır");
            dogrula(y, !kazandi(y, a, Basarimlar.YUKSELEN_SULAR), "yükseliş sürerken verilmez");
            donem.denizAyarla(new DenizSeviyesi(4f, 4f, dongu, true));
            dogrula(y, kazandi(y, a, Basarimlar.YUKSELEN_SULAR), "yükseliş tamamlanınca sağ olan Yükselen Sular alır");
            dogrula(y, !kazandi(y, b, Basarimlar.YUKSELEN_SULAR), "yükselişte ölen almaz");
        } finally {
            ust.setDayTime(oncekiSaat);
            yonetici.sifirla();
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    /**
     * Yeme: başlarken vücut {@code vucut}; bitince vanilla sırasıyla önce yiyeceğin ısınması (yiyecek dalının
     * {@code finishUsingItem}'ı gibi {@code VucutSicakligi.etkiEkle}), sonra {@code Finish} olayı.
     */
    private static void ye(ServerPlayer o, double vucut, ItemStack yiyecek, double anlik, int sure, double toplam) {
        VucutSicakligi.ayarla(o, vucut);
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(o, yiyecek, 32));
        if (sure > 0) {
            VucutSicakligi.etkiEkle(o, anlik, sure, toplam);
        }
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(o, yiyecek, 0, yiyecek.copy()));
    }

    @GameTest(template = "bos", batch = "basarim_corba")
    public static void corbaIcerIsinirYalnizDonarkenSicakYiyecekle(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.SICAKLIK_SISTEMI);
        boolean onceki = kural.get();
        kural.set(true, server);
        ServerPlayer soguk = oyuncu(y, "corba-soguk", 0);
        ServerPlayer donan = oyuncu(y, "corba-donan", 2);
        ServerPlayer serin = oyuncu(y, "corba-serin", 4);
        try {
            dogrula(y, SicaklikKapisi.acikMi(server), "sıcaklık sistemi açık olmalı");
            // −31 soğuk kademesi: kızarmış patates (vanilla pişmiş yiyeceğin küçük ısınması) ve nötre dönüş.
            ye(soguk, -31, new ItemStack(Items.BAKED_POTATO), 0.75, 600, 2.25);
            VucutSicakligi.ayarla(soguk, 0);
            BasarimOlaylari.saniyelikDenetim(soguk);
            dogrula(y, !kazandi(y, soguk, Basarimlar.CORBA_ICER_ISINIR),
                    "soğuk kademesinde yenen sayılmaz (donma değil)");
            // −80 donma kademesi: Ateşli Güveç ısınması (+8 anında, 180 sn'de +32).
            ye(donan, -80, new ItemStack(Items.MUSHROOM_STEW), 8, 180 * 20, 32);
            BasarimOlaylari.saniyelikDenetim(donan);
            dogrula(y, !kazandi(y, donan, Basarimlar.CORBA_ICER_ISINIR), "henüz nötr değil");
            VucutSicakligi.ayarla(donan, 0);
            BasarimOlaylari.saniyelikDenetim(donan);
            dogrula(y, kazandi(y, donan, Basarimlar.CORBA_ICER_ISINIR), "donarken sıcak yiyecek + etki sürerken nötr");
            // −80 donma kademesi ama ısıtmayan yiyecek.
            ye(serin, -80, new ItemStack(Items.MELON_SLICE), 0, 0, 0);
            VucutSicakligi.ayarla(serin, 0);
            BasarimOlaylari.saniyelikDenetim(serin);
            dogrula(y, !kazandi(y, serin, Basarimlar.CORBA_ICER_ISINIR), "sıcak olmayan yiyecek sayılmaz");
        } finally {
            kural.set(onceki, server);
            cikar(y, List.of(soguk, donan, serin));
        }
        y.succeed();
    }

    /**
     * Faz 4 iç davranışı (#2, #4, #5, #6): dört tür yayıncı olarak kayıtlı (Faz 4 işçisinin mod kurucusundaki satırı) ve
     * afetin {@link BasarimIpucuEvent}'i testten yayınlanır. Tek kaynak olaydır; aynı afetteki yedek tahminler (düşüş,
     * yangın yakınlığı, her hasar) sayılmaz (A-31, A-35).
     */
    @GameTest(template = "bos", batch = "basarim_ipucu")
    public static void ipucuOlayiTekKaynakDalgaCukurYanginAsit(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "ipucu-a", 0);
        ServerPlayer b = oyuncu(y, "ipucu-b", 2);
        ServerPlayer c = oyuncu(y, "ipucu-c", 4);
        try (BasarimIpucuEvent.GeciciYayinci tsunami =
                        BasarimIpucuEvent.geciciYayinci(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI);
                BasarimIpucuEvent.GeciciYayinci cukur =
                        BasarimIpucuEvent.geciciYayinci(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);
                BasarimIpucuEvent.GeciciYayinci yangin =
                        BasarimIpucuEvent.geciciYayinci(AfetTuru.ORMAN_YANGINI, Ipucu.YANGIN_ALANINDA);
                BasarimIpucuEvent.GeciciYayinci asit =
                        BasarimIpucuEvent.geciciYayinci(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI)) {
            afet(y, AfetTuru.TSUNAMI, 1, a, false, () -> ipucu(a, Ipucu.SURUKLENDI));
            dogrula(y, kazandi(y, a, Basarimlar.DALGA_AVCISI), "sürüklenme olayı Dalga Avcısı verir");
            dogrula(y, !kazandi(y, b, Basarimlar.DALGA_AVCISI), "olayı gelmeyen almaz");

            afet(y, AfetTuru.SINKHOLE, 1, a, false, () -> {
                ipucu(a, Ipucu.CUKURA_DUSTU);
                b.fallDistance = 10;
                BasarimOlaylari.tickDenetimi(b);
                b.fallDistance = 0;
            });
            dogrula(y, kazandi(y, a, Basarimlar.DELIKTEN_DONUS), "çukur olayı Delikten Dönüş verir");
            dogrula(y, !kazandi(y, b, Basarimlar.DELIKTEN_DONUS),
                    "olay gelen afette ağaçtan düşüş gibi yedek tahmin sayılmaz (inceleme A3)");

            afet(y, AfetTuru.ORMAN_YANGINI, 1, a, false, () -> {
                long no = afetNo(y);
                AfetIzi izA = BasarimVerisi.afetIzi(a);
                izA.afeteBagla(no);
                for (int s = 0; s < AfetIzi.YANGIN_SANIYESI - 1; s++) {
                    izA.isaretle(Ipucu.YANGIN_ALANINDA, true, -1_000 - s);
                }
                BasarimVerisi.afetIziYaz(a, izA);
                ipucu(a, Ipucu.YANGIN_ALANINDA);
                ipucu(a, Ipucu.YANGIN_ALANINDA);
                AfetIzi izB = BasarimVerisi.afetIzi(b);
                izB.afeteBagla(no);
                for (int s = 0; s < AfetIzi.YANGIN_SANIYESI; s++) {
                    izB.isaretle(Ipucu.YANGIN_ALANINDA, false, -1_000 - s);
                }
                BasarimVerisi.afetIziYaz(b, izB);
                dogrula(y, BasarimVerisi.afetIzi(a).yanginOlay == AfetIzi.YANGIN_SANIYESI,
                        "aynı saniyedeki ikinci olay sayılmaz: " + BasarimVerisi.afetIzi(a).yanginOlay);
            });
            dogrula(y, kazandi(y, a, Basarimlar.YANMADAN_CIKTIM), "30 olay saniyesi Yanmadan Çıktım verir");
            dogrula(y, !kazandi(y, b, Basarimlar.YANMADAN_CIKTIM), "olay gelen afette 30 yedek saniye sayılmaz");

            afet(y, AfetTuru.ASIT_YAGMURU, 1, a, false, () -> {
                ipucu(b, Ipucu.HASAR_ALDI);
                MinecraftForge.EVENT_BUS.post(new LivingDamageEvent(c, c.damageSources().generic(), 2f));
            });
            dogrula(y, kazandi(y, a, Basarimlar.CATI_ALTI), "hasar almayan Çatı Altı alır");
            dogrula(y, !kazandi(y, b, Basarimlar.CATI_ALTI), "asit hasarı olayı Çatı Altı'nı bozar");
            dogrula(y, kazandi(y, c, Basarimlar.CATI_ALTI),
                    "asit olay yayınlarken asitten olmayan hasar (zombi) bozmaz (inceleme A3)");
        } finally {
            cikar(y, List.of(a, b, c));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "basarim_yayinci")
    public static void olaysizAfetteYedekKayitliYayincidaYedekKapali(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "yayinci-a", 0);
        ServerPlayer b = oyuncu(y, "yayinci-b", 2);
        try {
            afet(y, AfetTuru.SINKHOLE, 1, a, false, () -> {
                a.fallDistance = 10;
                BasarimOlaylari.tickDenetimi(a);
                a.fallDistance = 0;
                ipucu(b, Ipucu.CUKURA_DUSTU);
                dogrula(y, BasarimVerisi.afetIzi(b).olayBitleri == 0,
                        "kayıtsız türden gelen olay yok sayılır ve uyarılır (inceleme Bs2, A-35)");
            });
            dogrula(y, kazandi(y, a, Basarimlar.DELIKTEN_DONUS), "olayı hiç gelmeyen afette yedek düşüş sayılır");
            try (BasarimIpucuEvent.GeciciYayinci kayit =
                    BasarimIpucuEvent.geciciYayinci(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU)) {
                afet(y, AfetTuru.SINKHOLE, 1, a, false, () -> {
                    b.fallDistance = 10;
                    BasarimOlaylari.tickDenetimi(b);
                    b.fallDistance = 0;
                    dogrula(y, BasarimVerisi.afetIzi(b).tahminBitleri == 0, "kayıtlı yayıncıda yedek iz yazmaz");
                });
            }
            dogrula(y, !kazandi(y, b, Basarimlar.DELIKTEN_DONUS), "kayıtlı yayıncı olay vermediyse kimse almaz");
        } finally {
            cikar(y, List.of(a, b));
        }
        y.succeed();
    }

    /** Kardeş dallar birleşip {@code runData} unutulursa kırmızıya döner (inceleme A5; A-06, A-07). */
    @GameTest(template = "bos")
    public static void ikonlarKayitliEsyalarlaGuncel(GameTestHelper y) {
        for (Basarimlar b : Basarimlar.values()) {
            if (b.ikonYedegi() == null) {
                continue;
            }
            Item yedek = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(b.ikonYedegi()));
            Item beklenen = BasarimKimlikleri.esyaYaDaYedek(ResourceLocation.tryParse(b.ikon()), yedek);
            Item ikon = advancement(y, b).getDisplay().getIcon().getItem();
            dogrula(y, ikon == beklenen, "runData eski: " + b.yol() + " ikonu " + ikon + ", beklenen " + beklenen);
        }
        y.succeed();
    }
}
