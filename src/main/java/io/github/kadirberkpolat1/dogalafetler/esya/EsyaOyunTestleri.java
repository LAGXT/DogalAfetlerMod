package io.github.kadirberkpolat1.dogalafetler.esya;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarSunucu;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografSunucu;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkDirection;

/**
 * Radar ve sismografın oyun içi testleri (madde 3, 4, 13a, 13b, N7): paketler yalnız eşyayı tutan sahte oyuncuya gider
 * ve gömülü kanalından okunur (çekirdek §13). Afet türleri henüz kayıtlı olmadığı için {@link OyunTestiAfeti} geçici
 * kaydedilir. ON_UYARI'nın normal penceresine afetin kendi {@code tick}'iyle erken bölüm kadar ilerlenir (aşama değişmez).
 * Tek afet yuvasını kullandıkları için her biri kendi {@code batch}'indedir.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EsyaOyunTestleri {
    private EsyaOyunTestleri() {
    }

    private static ICustomPacket<?> ozel(Object mesaj) {
        return (ICustomPacket<?>) AgKanali.KANAL.toVanillaPacket(mesaj, NetworkDirection.PLAY_TO_CLIENT);
    }

    /** Sahte oyuncunun kanalına giden mod paketleri (ham, eskiden yeniye); kuyruk boşalır. */
    private static List<byte[]> gelenler(ServerPlayer oyuncu) {
        EmbeddedChannel kanal = (EmbeddedChannel) oyuncu.connection.connection.channel();
        String ad = ozel(RadarPaketi.YOK).getName().toString();
        List<byte[]> sonuc = new ArrayList<>();
        for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
            if (mesaj instanceof ICustomPacket<?> paket && paket.getName().toString().equals(ad)) {
                sonuc.add(ByteBufUtil.getBytes(paket.getInternalData()));
            }
        }
        return sonuc;
    }

    private static <M> List<M> coz(List<byte[]> ham, Object ornek, Function<FriendlyByteBuf, M> okuyucu) {
        byte kimlik = ByteBufUtil.getBytes(ozel(ornek).getInternalData())[0];
        List<M> sonuc = new ArrayList<>();
        for (byte[] bayt : ham) {
            if (bayt.length > 0 && bayt[0] == kimlik) {
                sonuc.add(okuyucu.apply(new FriendlyByteBuf(Unpooled.wrappedBuffer(Arrays.copyOfRange(bayt, 1,
                        bayt.length)))));
            }
        }
        return sonuc;
    }

    private static List<RadarPaketi> radarlar(List<byte[]> ham) {
        return coz(ham, RadarPaketi.YOK, RadarPaketi::oku);
    }

    private static List<SismografPaketi> sismograflar(List<byte[]> ham) {
        return coz(ham, SismografPaketi.YOK, SismografPaketi::oku);
    }

    private static <T> T son(GameTestHelper yardimci, List<T> liste, String ne) {
        yardimci.assertTrue(!liste.isEmpty(), ne + ": paket gelmedi");
        return liste.get(liste.size() - 1);
    }

    private record Sahne(GameTestHelper yardimci, MinecraftServer server, AfetYoneticisi yonetici, AfetTuru tur,
            BlockPos merkez, AfetKaydi.GeciciKayit kayit, List<ServerPlayer> oyuncular) {
        static Sahne kur(GameTestHelper yardimci, AfetTuru tur) {
            ServerLevel level = yardimci.getLevel();
            AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
            yonetici.durdur();
            if (tur == AfetTuru.GUNES_PATLAMASI) {
                yonetici.donem().gunesEvresiAyarla(GunesEvresi.NORMAL);
            }
            AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(tur, () -> new OyunTestiAfeti(tur, 16, 100));
            return new Sahne(yardimci, level.getServer(), yonetici, tur, yardimci.absolutePos(BlockPos.ZERO), kayit,
                    new ArrayList<>());
        }

        ServerPlayer oyuncu(double dx, double dz) {
            ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(yardimci, merkez.getX() + 0.5 + dx, merkez.getY(),
                    merkez.getZ() + 0.5 + dz);
            oyuncular.add(oyuncu);
            return oyuncu;
        }

        Afet baslat(ServerPlayer hedef) {
            yardimci.assertTrue(yonetici.elleBaslat(tur, tur.seviyeli() ? 1 : null, hedef, yardimci.getLevel(), merkez)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı: " + tur.kimlik());
            Afet afet = yonetici.aktifAfet().orElseThrow();
            yardimci.assertTrue(afet.asama() == AfetAsamasi.ON_UYARI && !afet.normalOnUyariBasladi()
                    && afet.baglam().erkenBildirimSuresi() > 0, "afet ON_UYARI'nın erken bölümünde başlamalı");
            return afet;
        }

        /** Normal ön-uyarı penceresine ilerler (afetin kendi tick'i; aşama ON_UYARI'da kalır). */
        void normalPencereye(Afet afet) {
            afet.tick(yardimci.getLevel(), (int) afet.baglam().erkenBildirimSuresi());
            yardimci.assertTrue(afet.asama() == AfetAsamasi.ON_UYARI && afet.normalOnUyariBasladi(),
                    "normal ON_UYARI penceresine geçilmedi");
        }

        void temizle() {
            yonetici.durdur();
            if (tur == AfetTuru.GUNES_PATLAMASI) {
                yonetici.donem().gunesEvresiAyarla(GunesEvresi.NORMAL);
            }
            kayit.close();
            oyuncular.forEach(o -> server.getPlayerList().remove(o));
        }

        void koru(Runnable is) {
            try {
                is.run();
            } catch (RuntimeException hata) {
                temizle();
                throw hata;
            }
            temizle();
            yardimci.succeed();
        }
    }

    private static void radarVer(ServerPlayer oyuncu, boolean gelismis) {
        oyuncu.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gelismis ? EsyaKayitlari.GELISMIS_HAVA_RADARI.get()
                : EsyaKayitlari.HAVA_RADARI.get()));
    }

    private static void guncelle(ServerPlayer... oyuncular) {
        for (ServerPlayer o : oyuncular) {
            RadarSunucu.guncelle(o);
            SismografSunucu.guncelle(o);
        }
    }

    /**
     * Radar elde: deprem ON_UYARI'nın erken bölümünde "sakin", normal pencere başlayınca afet (ad, merkez, süre; normal radarda
     * seviye yok). Yanındaki eşyasız oyuncuya hiç radar ya da sismograf paketi gitmez (madde 4: eşyası olmayan için bir şey
     * değişmez).
     */
    @GameTest(template = "bos", batch = "esya_radar_onuyari", timeoutTicks = 100)
    public static void radarOnUyaridaNormalPenceredenSonraGosterirEsyasizaPaketYok(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.DEPREM);
        sahne.koru(() -> {
            ServerPlayer hedef = sahne.oyuncu(0, 0);
            ServerPlayer radarli = sahne.oyuncu(30, 0);
            ServerPlayer esyasiz = sahne.oyuncu(30, 4);
            radarVer(radarli, false);
            Afet afet = sahne.baslat(hedef);
            BlockPos merkez = afet.baglam().merkez();
            guncelle(radarli, esyasiz);
            RadarPaketi erken = son(yardimci, radarlar(gelenler(radarli)), "erken bölüm");
            yardimci.assertTrue(erken.elde() && erken.gorunum() == RadarGorunumu.GIZLI && erken.menzil() == 300,
                    "erken bölümde radar sakin olmalı: " + erken);
            sahne.normalPencereye(afet);
            guncelle(radarli, esyasiz);
            RadarPaketi p = son(yardimci, radarlar(gelenler(radarli)), "normal pencere");
            yardimci.assertTrue(p.elde() && !p.gelismis() && p.gorunum() == RadarGorunumu.AFET && p.tur() == AfetTuru.DEPREM
                    && p.asama() == AfetAsamasi.ON_UYARI && p.merkezX() == merkez.getX() && p.merkezZ() == merkez.getZ()
                    && p.kalanSn() > 0 && p.seviye() == 0 && Float.isNaN(p.hareketYonu()) && p.gecmis().isEmpty(),
                    "normal radar paketi: " + p);
            List<byte[]> esyasizGelen = gelenler(esyasiz);
            yardimci.assertTrue(radarlar(esyasizGelen).isEmpty() && sismograflar(esyasizGelen).isEmpty(),
                    "eşyasız oyuncuya radar/sismograf paketi gitti");
            yardimci.assertTrue(sismograflar(gelenler(radarli)).isEmpty(), "sismografsız oyuncuya sismograf paketi gitti");
        });
    }

    /** Merkez 400 blok ötede: normal radar (300) göstermez, Gelişmiş radar (800) gösterir ve seviyeyi ekler. */
    @GameTest(template = "bos", batch = "esya_radar_menzil", timeoutTicks = 100)
    public static void radarMenzilDisindaGostermezGelismisGosterir(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.DEPREM);
        sahne.koru(() -> {
            ServerPlayer hedef = sahne.oyuncu(400, 0);
            ServerPlayer normal = sahne.oyuncu(0, 0);
            ServerPlayer gelismis = sahne.oyuncu(0, 3);
            radarVer(normal, false);
            radarVer(gelismis, true);
            Afet afet = sahne.baslat(hedef);
            sahne.normalPencereye(afet);
            guncelle(normal, gelismis);
            RadarPaketi n = son(yardimci, radarlar(gelenler(normal)), "normal radar");
            yardimci.assertTrue(n.elde() && n.gorunum() == RadarGorunumu.GIZLI, "menzil dışı gösterildi: " + n);
            RadarPaketi g = son(yardimci, radarlar(gelenler(gelismis)), "gelişmiş radar");
            yardimci.assertTrue(g.elde() && g.gelismis() && g.menzil() == 800 && g.gorunum() == RadarGorunumu.AFET
                    && g.seviye() == afet.seviye() && g.seviye() == 1, "gelişmiş radar paketi: " + g);
        });
    }

    /** Güneş patlaması ON_UYARI'da radarda yok (normal pencerede bile); SIREN'de gökyüzü anomalisi + geri sayım (N7). */
    @GameTest(template = "bos", batch = "esya_radar_global", timeoutTicks = 100)
    public static void globalAfetOnUyaridaYokSirendeAnomali(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.GUNES_PATLAMASI);
        sahne.koru(() -> {
            ServerPlayer radarli = sahne.oyuncu(0, 0);
            radarVer(radarli, true);
            Afet afet = sahne.baslat(radarli);
            sahne.normalPencereye(afet);
            guncelle(radarli);
            RadarPaketi on = son(yardimci, radarlar(gelenler(radarli)), "ON_UYARI");
            yardimci.assertTrue(on.gorunum() == RadarGorunumu.GIZLI, "global afet ON_UYARI'da gösterildi: " + on);
            yardimci.assertTrue(sahne.yonetici().asamaIleri() && afet.asama() == AfetAsamasi.SIREN, "SIREN'e geçmedi");
            guncelle(radarli);
            RadarPaketi s = son(yardimci, radarlar(gelenler(radarli)), "SIREN");
            yardimci.assertTrue(s.gorunum() == RadarGorunumu.ANOMALI && s.tur() == AfetTuru.GUNES_PATLAMASI
                    && s.asama() == AfetAsamasi.SIREN && s.kalanSn() > 0 && s.kalanSn() <= 60, "anomali paketi: " + s);
        });
    }

    /** Sismograf hotbar'da: deprem ON_UYARI normal penceresinde algılanır (yüzde, yaklaşık merkez; seviye yok). */
    @GameTest(template = "bos", batch = "esya_sismograf_deprem", timeoutTicks = 100)
    public static void sismografEnvanterdeDepremOnUyarisindaAlgilar(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.DEPREM);
        sahne.koru(() -> {
            ServerPlayer hedef = sahne.oyuncu(0, 0);
            ServerPlayer tasiyan = sahne.oyuncu(20, 0);
            tasiyan.getInventory().setItem(4, new ItemStack(EsyaKayitlari.SISMOGRAF.get()));
            Afet afet = sahne.baslat(hedef);
            guncelle(tasiyan);
            SismografPaketi erken = son(yardimci, sismograflar(gelenler(tasiyan)), "erken bölüm");
            yardimci.assertTrue(erken.tasiyor() && !erken.algilandi(), "normal sismograf erken bölümde algıladı: " + erken);
            sahne.normalPencereye(afet);
            guncelle(tasiyan);
            SismografPaketi p = son(yardimci, sismograflar(gelenler(tasiyan)), "normal pencere");
            BlockPos merkez = afet.baglam().merkez();
            double sapma = Math.hypot(p.tahminX() - merkez.getX(), p.tahminZ() - merkez.getZ());
            yardimci.assertTrue(p.tasiyor() && !p.hassas() && p.algilandi() && p.tur() == AfetTuru.DEPREM && p.yuzde() >= 1
                    && p.yuzde() <= 100 && p.seviye() == 0 && p.oncu() > 0 && sapma <= SismografAyarlari.merkezHatasi(false) + 2,
                    "sismograf paketi: " + p + " sapma " + sapma);
            yardimci.assertTrue(radarlar(gelenler(tasiyan)).isEmpty(), "radarsız oyuncuya radar paketi gitti");
        });
    }

    /** Kasırga sismik değildir: sismograf algılamaz (yalnız "sakin"). */
    @GameTest(template = "bos", batch = "esya_sismograf_kasirga", timeoutTicks = 100)
    public static void sismografKasirgayiAlgilamaz(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.KASIRGA);
        sahne.koru(() -> {
            ServerPlayer hedef = sahne.oyuncu(0, 0);
            ServerPlayer tasiyan = sahne.oyuncu(10, 0);
            tasiyan.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(EsyaKayitlari.HASSAS_SISMOGRAF.get()));
            Afet afet = sahne.baslat(hedef);
            sahne.normalPencereye(afet);
            guncelle(tasiyan);
            sahne.yonetici().asamaIleri();
            guncelle(tasiyan);
            List<SismografPaketi> gelen = sismograflar(gelenler(tasiyan));
            yardimci.assertTrue(!gelen.isEmpty() && gelen.stream().noneMatch(SismografPaketi::algilandi),
                    "kasırgada algılama: " + gelen);
        });
    }

    /**
     * Hassas Sismograf (yan elde) ON_UYARI'nın başından algılar ve seviyeyi verir; aynı anda hotbar'ında normal sismograf
     * taşıyan oyuncu henüz algılamaz.
     */
    @GameTest(template = "bos", batch = "esya_sismograf_hassas", timeoutTicks = 100)
    public static void hassasSismografErkenAlgilarSeviyeyiVerir(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.SINKHOLE);
        sahne.koru(() -> {
            ServerPlayer hedef = sahne.oyuncu(0, 0);
            ServerPlayer hassas = sahne.oyuncu(12, 0);
            ServerPlayer normal = sahne.oyuncu(12, 4);
            hassas.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(EsyaKayitlari.HASSAS_SISMOGRAF.get()));
            normal.getInventory().setItem(0, new ItemStack(EsyaKayitlari.SISMOGRAF.get()));
            Afet afet = sahne.baslat(hedef);
            guncelle(hassas, normal);
            SismografPaketi h = son(yardimci, sismograflar(gelenler(hassas)), "hassas");
            yardimci.assertTrue(h.hassas() && h.algilandi() && h.tur() == AfetTuru.SINKHOLE && h.seviye() == afet.seviye()
                    && h.asama() == AfetAsamasi.ON_UYARI, "hassas sismograf erken algılamadı: " + h);
            SismografPaketi n = son(yardimci, sismograflar(gelenler(normal)), "normal");
            yardimci.assertTrue(!n.hassas() && n.tasiyor() && !n.algilandi(), "normal sismograf erken algıladı: " + n);
        });
    }

    /** Radar bırakılınca tek {@code YOK}, sonra hiç paket; radar hiç tutulmadıysa hiç paket yok. */
    @GameTest(template = "bos", batch = "esya_radar_birak", timeoutTicks = 100)
    public static void radarBirakilincaTekYokSonraSessiz(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.DEPREM);
        sahne.koru(() -> {
            ServerPlayer radarli = sahne.oyuncu(0, 0);
            ServerPlayer esyasiz = sahne.oyuncu(3, 0);
            radarVer(radarli, false);
            radarli.getInventory().setItem(8, new ItemStack(EsyaKayitlari.SISMOGRAF.get()));
            guncelle(radarli, esyasiz);
            List<byte[]> ilk = gelenler(radarli);
            yardimci.assertTrue(radarlar(ilk).size() == 1 && radarlar(ilk).get(0).elde()
                    && sismograflar(ilk).size() == 1 && sismograflar(ilk).get(0).tasiyor(), "ilk durum paketleri");
            guncelle(radarli);
            yardimci.assertTrue(gelenler(radarli).isEmpty(), "değişmeyen durum yeniden gönderildi");
            radarli.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            radarli.getInventory().setItem(8, ItemStack.EMPTY);
            guncelle(radarli);
            List<byte[]> birak = gelenler(radarli);
            yardimci.assertTrue(radarlar(birak).equals(List.of(RadarPaketi.YOK))
                    && sismograflar(birak).equals(List.of(SismografPaketi.YOK)), "bırakınca tek YOK beklendi");
            guncelle(radarli, esyasiz);
            yardimci.assertTrue(gelenler(radarli).isEmpty() && gelenler(esyasiz).isEmpty(), "eşyasız oyuncuya paket gitti");
        });
    }

    /** Tarif kimlikleri sabit (başarım işçisi); şekilli tarifler madde 13a-b'deki desenle birebir. */
    @GameTest(template = "bos")
    public static void tariflerYukluVeDesenlerSartnameyleAyni(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        Ingredient[] radar = desen(yardimci, server, EsyaKayitlari.TARIF_HAVA_RADARI, EsyaKayitlari.HAVA_RADARI.get().asItem());
        yardimci.assertTrue(radar[0].test(new ItemStack(Items.IRON_INGOT)) && radar[1].test(new ItemStack(Items.GLASS))
                && radar[3].test(new ItemStack(Items.REDSTONE)) && radar[4].test(new ItemStack(Items.COMPASS))
                && radar[7].test(new ItemStack(Items.COPPER_INGOT)) && radar[8].test(new ItemStack(Items.IRON_INGOT)),
                "Hava Radarı deseni");
        Ingredient[] sismograf = desen(yardimci, server, EsyaKayitlari.TARIF_SISMOGRAF, EsyaKayitlari.SISMOGRAF.get().asItem());
        yardimci.assertTrue(sismograf[1].test(new ItemStack(Items.PAPER)) && sismograf[3].test(new ItemStack(Items.STICK))
                && sismograf[4].test(new ItemStack(Items.REDSTONE)) && sismograf[6].test(new ItemStack(Items.STONE))
                && sismograf[7].test(new ItemStack(Items.COPPER_INGOT)), "Sismograf deseni");
        for (ResourceLocation kimlik : List.of(EsyaKayitlari.TARIF_GELISMIS_HAVA_RADARI, EsyaKayitlari.TARIF_HASSAS_SISMOGRAF)) {
            Optional<? extends Recipe<?>> tarif = server.getRecipeManager().byKey(kimlik);
            yardimci.assertTrue(tarif.isPresent() && tarif.get().getIngredients().size() == 2, "şekilsiz tarif: " + kimlik);
        }
        yardimci.succeed();
    }

    private static Ingredient[] desen(GameTestHelper yardimci, MinecraftServer server, ResourceLocation kimlik,
            net.minecraft.world.item.Item sonuc) {
        Optional<? extends Recipe<?>> tarif = server.getRecipeManager().byKey(kimlik);
        yardimci.assertTrue(tarif.isPresent(), "tarif yok: " + kimlik);
        yardimci.assertTrue(tarif.get().getResultItem(server.registryAccess()).is(sonuc), "tarif sonucu: " + kimlik);
        List<Ingredient> malzemeler = tarif.get().getIngredients();
        yardimci.assertTrue(malzemeler.size() == 9, "3x3 değil: " + kimlik);
        return malzemeler.toArray(Ingredient[]::new);
    }
}
