package io.github.kadirberkpolat1.dogalafetler.basarim;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetIzi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.BasarimTuru;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.KutupSayaci;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.BitisNedeni;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Başarımların oyun içi testleri (GOREV §6 #16): gerçek afet yaşam döngüsü ({@link OyunTestiAfeti}, geçici kayıt),
 * sahte oyuncular ve datagen'le üretilmiş advancement'lar. Her test kendi oyuncularını açar; tek afet yuvasını kullanan
 * testler kendi {@code batch}'indedir. Sahte oyuncular tick almadığı için saniyelik denetim doğrudan çağrılır. Toast
 * ve sohbet duyurusu vanilla'dır (JSON'da {@code show_toast}, {@code announce_to_chat}); burada yalnız kazanım,
 * görünürlük, ödül ve ünvan sınanır.
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
     * {@code oyuncular} merkezdeyken kasırga (OyunTestiAfeti, yarıçap 16) başlatır ve SIREN → AKTIF → BITIS → doğal son
     * ya da ({@code stop}) AKTIF'te durdurur. {@code araKontrol} AKTIF aşamada çağrılır.
     */
    private static void kasirga(GameTestHelper y, int seviye, ServerPlayer hedef, boolean stop, Runnable araKontrol) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                () -> new OyunTestiAfeti(AfetTuru.KASIRGA, YARICAP, 100))) {
            AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.KASIRGA, seviye, hedef, level,
                    y.absolutePos(BlockPos.ZERO));
            dogrula(y, sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı: " + sonuc);
            Afet afet = yonetici.aktifAfet().orElseThrow();
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.SIREN, "siren aşamasına geçmedi");
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.AKTIF, "aktif aşamaya geçmedi");
            araKontrol.run();
            if (stop) {
                yonetici.durdur();
            } else {
                yonetici.asamaIleri();
                yonetici.asamaIleri();
            }
            dogrula(y, yonetici.aktifAfet().isEmpty(), "afet sona ermedi");
        } finally {
            yonetici.durdur();
        }
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
            int yiyecek = 0;
            for (int i = 0; i < a.getInventory().getContainerSize(); i++) {
                ItemStack yigin = a.getInventory().getItem(i);
                if (yigin.isEdible()) {
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
            int beklenen = BasarimTuru.GOREV.varsayilanXp() * 2 + BasarimTuru.HEDEF.varsayilanXp()
                    + BasarimTuru.MEYDAN_OKUMA.varsayilanXp();
            dogrula(y, a.totalExperience >= beklenen, "XP (kök, İlk Rüzgâr, hedef, meydan okuma): "
                    + a.totalExperience);
        } finally {
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

    @GameTest(template = "bos")
    public static void radarUretimiRadarAltinda(GameTestHelper y) {
        ServerPlayer a = oyuncu(y, "radar-a", 0);
        ServerPlayer b = oyuncu(y, "radar-b", 2);
        try {
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(a, new ItemStack(Items.COMPASS),
                    new SimpleContainer(9)));
            dogrula(y, !kazandi(y, a, Basarimlar.RADAR_ALTINDA), "pusula üretimi sayılmaz");
            if (ForgeRegistries.ITEMS.containsKey(BasarimKimlikleri.HAVA_RADARI)) {
                ItemStack radar = new ItemStack(ForgeRegistries.ITEMS.getValue(BasarimKimlikleri.HAVA_RADARI));
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(a, radar, new SimpleContainer(9)));
            } else {
                // Radar eşyası kardeş dalda: olay dinleyicisinin çağırdığı yol kimlikle sınanır.
                BasarimOlaylari.esyaUretildi(a, BasarimKimlikleri.HAVA_RADARI);
            }
            dogrula(y, kazandi(y, a, Basarimlar.RADAR_ALTINDA), "Hava Radarı üretimi Radar Altında verir");
            dogrula(y, !kazandi(y, a, Basarimlar.YER_DINLEYICI), "radar Yer Dinleyici vermez");
            dogrula(y, !kazandi(y, b, Basarimlar.RADAR_ALTINDA), "diğer oyuncu almaz");
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

    @GameTest(template = "bos")
    public static void kosulluAfetBasarimlariIzdenGelir(GameTestHelper y) {
        ServerPlayer hasarli = oyuncu(y, "asit-a", 0);
        ServerPlayer kosullu = oyuncu(y, "iz-b", 2);
        try {
            long no = -8_000_000L;
            AfetIzi iz = new AfetIzi();
            iz.afeteBagla(no);
            iz.hasarAldi = true;
            BasarimVerisi.afetIziYaz(hasarli, iz);
            bitti(hasarli, AfetTuru.ASIT_YAGMURU, 2, no, BitisNedeni.DOGAL);
            dogrula(y, !kazandi(y, hasarli, Basarimlar.CATI_ALTI), "asit yağmurunda hasar alan Çatı Altı almaz");
            Set<Basarimlar> beklenen = new HashSet<>();
            AfetTuru[] turler = {AfetTuru.TSUNAMI, AfetTuru.SINKHOLE, AfetTuru.ORMAN_YANGINI};
            for (AfetTuru tur : turler) {
                AfetIzi k = new AfetIzi();
                k.afeteBagla(--no);
                k.suruklendi = tur == AfetTuru.TSUNAMI;
                k.cukuraDustu = tur == AfetTuru.SINKHOLE;
                k.yanginSaniye = tur == AfetTuru.ORMAN_YANGINI ? AfetIzi.YANGIN_SANIYESI : 0;
                BasarimVerisi.afetIziYaz(kosullu, k);
                bitti(kosullu, tur, 1, no, BitisNedeni.DOGAL);
            }
            beklenen.add(Basarimlar.DALGA_AVCISI);
            beklenen.add(Basarimlar.DELIKTEN_DONUS);
            beklenen.add(Basarimlar.YANMADAN_CIKTIM);
            for (Basarimlar b : beklenen) {
                dogrula(y, kazandi(y, kosullu, b), "izdeki koşul " + b.yol() + " vermeli");
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
}
