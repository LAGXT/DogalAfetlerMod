package io.github.kadirberkpolat1.dogalafetler.esya;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
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

    /** Radar ve sismograf paketlerinin sayısı (kanaldaki diğer alt sistemlerin paketleri sayılmaz). */
    private static int esyaPaketleri(List<byte[]> ham) {
        return radarlar(ham).size() + sismograflar(ham).size();
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

    /**
     * Çoklu afette radarın seçimi (K-E64, entegrasyon 7 `RadarSunucu` çözümü): görünen bölgesel afet küresel afetten önce
     * gelir (sonra başlasa da), küreseller arasında önce başlayan. Asit, etki alanı her yer olan taklittir (gerçek asidin hava ve
     * aşınması radar seçimini etkilemez; gerçek asidin "her yer" paketi `AsitOyunTestleri`'nde); güneş patlaması SIREN'de anomali.
     */
    @GameTest(template = "bos", batch = "esya_radar_coklu", timeoutTicks = 100)
    public static void cokluAfetteRadarBolgeseliOnceKureselleriBaslamaSirasiylaGosterir(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        yonetici.donem().gunesEvresiAyarla(GunesEvresi.NORMAL);
        BlockPos merkez = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer radarli = CekirdekOyunTestleri.sahteOyuncu(yardimci, merkez.getX() + 0.5, merkez.getY(),
                merkez.getZ() + 30.5);
        radarVer(radarli, false);
        try (AfetKaydi.GeciciKayit asit = AfetKaydi.geciciKaydet(AfetTuru.ASIT_YAGMURU,
                () -> new OyunTestiAfeti(AfetTuru.ASIT_YAGMURU, 16, 100) {
                    @Override
                    public EtkiAlani etkiAlani() {
                        return EtkiAlani.herYer();
                    }
                });
                AfetKaydi.GeciciKayit deprem = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                        () -> new OyunTestiAfeti(AfetTuru.DEPREM, 16, 100));
                AfetKaydi.GeciciKayit gunes = AfetKaydi.geciciKaydet(AfetTuru.GUNES_PATLAMASI,
                        () -> new OyunTestiAfeti(AfetTuru.GUNES_PATLAMASI, 16, 100))) {
            Afet a = cokluBaslat(yardimci, yonetici, AfetTuru.ASIT_YAGMURU, radarli, merkez);
            normalPencereyeTek(yardimci, a);
            RadarPaketi p = RadarSunucu.durum(radarli);
            yardimci.assertTrue(p.gorunum() == RadarGorunumu.AFET && p.tur() == AfetTuru.ASIT_YAGMURU && p.herYer()
                    && Float.isNaN(p.hareketYonu()), "yalnız asit: 'her yer' olmalı: " + p);

            Afet d = cokluBaslat(yardimci, yonetici, AfetTuru.DEPREM, radarli, merkez);
            normalPencereyeTek(yardimci, d);
            p = RadarSunucu.durum(radarli);
            yardimci.assertTrue(p.gorunum() == RadarGorunumu.AFET && p.tur() == AfetTuru.DEPREM && !p.herYer()
                    && p.merkezX() == d.baglam().merkez().getX() && p.merkezZ() == d.baglam().merkez().getZ(),
                    "asit + deprem: sonra başlayan bölgesel deprem gösterilmeli: " + p);
            yardimci.assertTrue(yonetici.durdur(AfetTuru.DEPREM), "deprem durmadı");
            p = RadarSunucu.durum(radarli);
            yardimci.assertTrue(p.tur() == AfetTuru.ASIT_YAGMURU && p.herYer(), "deprem durunca asit dönmeli: " + p);

            Afet g = cokluBaslat(yardimci, yonetici, AfetTuru.GUNES_PATLAMASI, radarli, merkez);
            yardimci.assertTrue(yonetici.asamaIleri(AfetTuru.GUNES_PATLAMASI) && g.asama() == AfetAsamasi.SIREN,
                    "güneş SIREN'e geçmedi");
            p = RadarSunucu.durum(radarli);
            yardimci.assertTrue(p.gorunum() == RadarGorunumu.AFET && p.tur() == AfetTuru.ASIT_YAGMURU && p.herYer(),
                    "asit önce başladı, güneş sonra: asit gösterilmeli: " + p);

            yonetici.durdur();
            yonetici.donem().gunesEvresiAyarla(GunesEvresi.NORMAL);
            g = cokluBaslat(yardimci, yonetici, AfetTuru.GUNES_PATLAMASI, radarli, merkez);
            yardimci.assertTrue(yonetici.asamaIleri(AfetTuru.GUNES_PATLAMASI) && g.asama() == AfetAsamasi.SIREN,
                    "güneş SIREN'e geçmedi (2)");
            a = cokluBaslat(yardimci, yonetici, AfetTuru.ASIT_YAGMURU, radarli, merkez);
            normalPencereyeTek(yardimci, a);
            p = RadarSunucu.durum(radarli);
            yardimci.assertTrue(p.gorunum() == RadarGorunumu.ANOMALI && p.tur() == AfetTuru.GUNES_PATLAMASI,
                    "güneş önce başladı, asit sonra: güneş anomalisi gösterilmeli: " + p);

            d = cokluBaslat(yardimci, yonetici, AfetTuru.DEPREM, radarli, merkez);
            normalPencereyeTek(yardimci, d);
            p = RadarSunucu.durum(radarli);
            yardimci.assertTrue(p.gorunum() == RadarGorunumu.AFET && p.tur() == AfetTuru.DEPREM,
                    "güneş + asit + deprem: bölgesel deprem gösterilmeli: " + p);
        } finally {
            yonetici.durdur();
            yonetici.donem().gunesEvresiAyarla(GunesEvresi.NORMAL);
            server.getPlayerList().remove(radarli);
        }
        yardimci.succeed();
    }

    private static Afet cokluBaslat(GameTestHelper yardimci, AfetYoneticisi yonetici, AfetTuru tur, ServerPlayer hedef,
            BlockPos merkez) {
        yardimci.assertTrue(yonetici.elleBaslat(tur, tur.seviyeli() ? 1 : null, hedef, yardimci.getLevel(), merkez)
                == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı: " + tur.kimlik());
        return yonetici.aktifAfet(tur).orElseThrow();
    }

    /** Bir afeti normal ön-uyarı penceresine ilerletir (afetin kendi tick'i; aşama ON_UYARI'da kalır). */
    private static void normalPencereyeTek(GameTestHelper yardimci, Afet afet) {
        afet.tick(yardimci.getLevel(), (int) afet.baglam().erkenBildirimSuresi());
        yardimci.assertTrue(afet.asama() == AfetAsamasi.ON_UYARI && afet.normalOnUyariBasladi(),
                "normal ON_UYARI penceresine geçilmedi: " + afet.tur().kimlik());
    }

    /**
     * Sismograf hotbar'da: deprem ON_UYARI normal penceresinde algılanır (yüzde, yaklaşık merkez; seviye yok), SIREN'de öncü
     * sarsıntı sürer, AKTIF'te ana sarsıntı (istemci "Ana sarsıntı" yazar).
     */
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
            yardimci.assertTrue(sahne.yonetici().asamaIleri() && afet.asama() == AfetAsamasi.SIREN, "SIREN'e geçmedi");
            guncelle(tasiyan);
            SismografPaketi siren = son(yardimci, sismograflar(gelenler(tasiyan)), "SIREN");
            yardimci.assertTrue(siren.algilandi() && siren.asama() == AfetAsamasi.SIREN && siren.oncu() > 0
                    && siren.afetNo() == p.afetNo(), "SIREN'de öncü sarsıntı paketi: " + siren);
            yardimci.assertTrue(sahne.yonetici().asamaIleri() && afet.asama() == AfetAsamasi.AKTIF, "AKTIF'e geçmedi");
            guncelle(tasiyan);
            SismografPaketi aktif = son(yardimci, sismograflar(gelenler(tasiyan)), "AKTIF");
            yardimci.assertTrue(aktif.algilandi() && aktif.asama() == AfetAsamasi.AKTIF && aktif.seviye() == 0,
                    "AKTIF'te ana sarsıntı paketi: " + aktif);
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
            yardimci.assertTrue(esyaPaketleri(gelenler(radarli)) == 0, "değişmeyen durum yeniden gönderildi");
            radarli.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            radarli.getInventory().setItem(8, ItemStack.EMPTY);
            guncelle(radarli);
            List<byte[]> birak = gelenler(radarli);
            yardimci.assertTrue(radarlar(birak).equals(List.of(RadarPaketi.YOK))
                    && sismograflar(birak).equals(List.of(SismografPaketi.YOK)), "bırakınca tek YOK beklendi");
            guncelle(radarli, esyasiz);
            yardimci.assertTrue(esyaPaketleri(gelenler(radarli)) == 0, "bıraktıktan sonra paket gitti");
            yardimci.assertTrue(esyaPaketleri(gelenler(esyasiz)) == 0, "eşyasız oyuncuya radar/sismograf paketi gitti");
        });
    }

    /**
     * Gelişmiş radarın son 5 afeti pakette: iki test afeti (kasırga S2, sinkhole S4) bitince en yenisi başta, yaşı 0 dk; aynı
     * anda normal radarlı oyuncunun paketinde geçmiş yok (madde 13a). Geçmiş dünya kaydıdır; testten önceki kayıtlar
     * sayıya katılır (en çok 5).
     */
    @GameTest(template = "bos", batch = "esya_radar_gecmis", timeoutTicks = 100)
    public static void gelismisRadarSonAfetleriPaketteGosterir(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci, AfetTuru.KASIRGA);
        sahne.koru(() -> {
            try (AfetKaydi.GeciciKayit sinkhole = AfetKaydi.geciciKaydet(AfetTuru.SINKHOLE,
                    () -> new OyunTestiAfeti(AfetTuru.SINKHOLE, 16, 100))) {
                ServerPlayer gelismis = sahne.oyuncu(0, 0);
                ServerPlayer normal = sahne.oyuncu(4, 0);
                radarVer(gelismis, true);
                radarVer(normal, false);
                int onceki = sahne.yonetici().donem().sonAfetler().size();
                for (Object[] afet : new Object[][] {{AfetTuru.KASIRGA, 2}, {AfetTuru.SINKHOLE, 4}}) {
                    yardimci.assertTrue(sahne.yonetici().elleBaslat((AfetTuru) afet[0], (Integer) afet[1], gelismis,
                            yardimci.getLevel(), sahne.merkez()) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                            "başlamadı: " + afet[0]);
                    yardimci.assertTrue(sahne.yonetici().durdur(), "durmadı: " + afet[0]);
                }
                guncelle(gelismis, normal);
                RadarPaketi g = son(yardimci, radarlar(gelenler(gelismis)), "gelişmiş radar");
                List<RadarPaketi.Gecmis> gecmis = g.gecmis();
                yardimci.assertTrue(g.gelismis() && g.gorunum() == RadarGorunumu.GIZLI
                        && gecmis.size() == Math.min(RadarPaketi.GECMIS_SINIRI, onceki + 2), "geçmiş boyu: " + gecmis);
                yardimci.assertTrue(gecmis.get(0).equals(new RadarPaketi.Gecmis(AfetTuru.SINKHOLE, 4, 0))
                        && gecmis.get(1).equals(new RadarPaketi.Gecmis(AfetTuru.KASIRGA, 2, 0)), "geçmiş sırası: " + gecmis);
                RadarPaketi n = son(yardimci, radarlar(gelenler(normal)), "normal radar");
                yardimci.assertTrue(!n.gelismis() && n.gecmis().isEmpty(), "normal radara geçmiş gitti: " + n);
            }
        });
    }

    /**
     * Tarif kimlikleri sabit (başarım işçisi); şekilli tarifler madde 13a-b'deki desenle dokuz hücrede birebir. Gelişmiş
     * tarifler {@code forge:not(forge:tag_empty)} koşuluyla yüklenir (R-17); {@code #dogalafetler:kozmik_cekirdek}'i meteor doldurur,
     * bu yüzden ikisi de yüklüdür ve meteorun Kozmik Çekirdeğiyle gerçekten craft edilir (boş etiket regresyondur).
     */
    @GameTest(template = "bos")
    public static void tariflerYukluVeDesenlerSartnameyleAyni(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        hucreler(yardimci, desen(yardimci, server, EsyaKayitlari.TARIF_HAVA_RADARI, EsyaKayitlari.HAVA_RADARI.get()),
                "Hava Radarı", Items.IRON_INGOT, Items.GLASS, Items.IRON_INGOT, Items.REDSTONE, Items.COMPASS, Items.REDSTONE,
                Items.IRON_INGOT, Items.COPPER_INGOT, Items.IRON_INGOT);
        hucreler(yardimci, desen(yardimci, server, EsyaKayitlari.TARIF_SISMOGRAF, EsyaKayitlari.SISMOGRAF.get()), "Sismograf",
                Items.IRON_INGOT, Items.PAPER, Items.IRON_INGOT, Items.STICK, Items.REDSTONE, Items.STICK, Items.STONE,
                Items.COPPER_INGOT, Items.STONE);
        Item cekirdek = MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get();
        yardimci.assertTrue(cekirdek.builtInRegistryHolder().is(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI),
                "#dogalafetler:kozmik_cekirdek Kozmik Çekirdeği içermiyor");
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(yardimci, 0.5, 2, 0.5);
        try {
            gelismisTarif(yardimci, oyuncu, EsyaKayitlari.TARIF_GELISMIS_HAVA_RADARI, EsyaKayitlari.HAVA_RADARI.get(),
                    EsyaKayitlari.GELISMIS_HAVA_RADARI.get(), cekirdek);
            gelismisTarif(yardimci, oyuncu, EsyaKayitlari.TARIF_HASSAS_SISMOGRAF, EsyaKayitlari.SISMOGRAF.get(),
                    EsyaKayitlari.HASSAS_SISMOGRAF.get(), cekirdek);
        } finally {
            server.getPlayerList().remove(oyuncu);
        }
        DogalAfetler.LOGGER.info("Eşya GameTest: gelişmiş tarifler yüklü, {} ile craft edildi", cekirdek);
        yardimci.succeed();
    }

    /** Temel iki tarif koşulsuz yüklü ve çalışma masasında (3x3 tezgah + {@code RecipeManager.getRecipeFor}) ürünü verir. */
    @GameTest(template = "bos", batch = "esya_tarif")
    public static void temelTariflerCalismaMasasindaUrunVerir(GameTestHelper yardimci) {
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(yardimci, 0.5, 2, 0.5);
        try {
            calismaMasasi(yardimci, oyuncu, EsyaKayitlari.TARIF_HAVA_RADARI, EsyaKayitlari.HAVA_RADARI.get(), Items.IRON_INGOT,
                    Items.GLASS, Items.IRON_INGOT, Items.REDSTONE, Items.COMPASS, Items.REDSTONE, Items.IRON_INGOT,
                    Items.COPPER_INGOT, Items.IRON_INGOT);
            calismaMasasi(yardimci, oyuncu, EsyaKayitlari.TARIF_SISMOGRAF, EsyaKayitlari.SISMOGRAF.get(), Items.IRON_INGOT,
                    Items.PAPER, Items.IRON_INGOT, Items.STICK, Items.REDSTONE, Items.STICK, Items.STONE, Items.COPPER_INGOT,
                    Items.STONE);
        } finally {
            yardimci.getLevel().getServer().getPlayerList().remove(oyuncu);
        }
        yardimci.succeed();
    }

    /**
     * JEI'siz oyuncu tarifi yalnız vanilla tarif kitabında görür: temel tarifler oyuncunun ilk tick'inde ({@code minecraft:tick})
     * ya da demir külçesi envantere girince kitaba girer. Gelişmişler başta kapalıdır, temel eşya envantere girince açılır.
     */
    @GameTest(template = "bos", batch = "esya_tarif")
    public static void tarifKitabiTemelleriOyunBasindaGelismisleriTemelEsyaylaAcar(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        List<ResourceLocation> temeller = List.of(EsyaKayitlari.TARIF_HAVA_RADARI, EsyaKayitlari.TARIF_SISMOGRAF);
        List<ResourceLocation> gelismisler = List.of(EsyaKayitlari.TARIF_GELISMIS_HAVA_RADARI,
                EsyaKayitlari.TARIF_HASSAS_SISMOGRAF);
        ServerPlayer yeni = CekirdekOyunTestleri.sahteOyuncu(yardimci, 0.5, 2, 0.5);
        ServerPlayer demirli = CekirdekOyunTestleri.sahteOyuncu(yardimci, 1.5, 2, 0.5);
        try {
            for (ResourceLocation tarif : temeller) {
                yardimci.assertTrue(!kitapta(yardimci, server, yeni, tarif), "tick'ten önce kitapta: " + tarif);
            }
            // ServerPlayer.doTick her tick'te bunu çağırır.
            CriteriaTriggers.TICK.trigger(yeni);
            for (ResourceLocation tarif : temeller) {
                yardimci.assertTrue(kitapta(yardimci, server, yeni, tarif), "ilk tick'te kitapta değil: " + tarif);
            }
            for (ResourceLocation tarif : gelismisler) {
                yardimci.assertTrue(!kitapta(yardimci, server, yeni, tarif), "temel eşyasız kitapta: " + tarif);
            }
            envantereKoy(demirli, Items.IRON_INGOT);
            for (ResourceLocation tarif : temeller) {
                yardimci.assertTrue(kitapta(yardimci, server, demirli, tarif), "demir külçesiyle kitapta değil: " + tarif);
            }
            envantereKoy(demirli, EsyaKayitlari.HAVA_RADARI.get(), EsyaKayitlari.SISMOGRAF.get());
            for (ResourceLocation tarif : gelismisler) {
                yardimci.assertTrue(kitapta(yardimci, server, demirli, tarif), "temel eşyayla kitapta değil: " + tarif);
            }
        } finally {
            server.getPlayerList().remove(yeni);
            server.getPlayerList().remove(demirli);
        }
        DogalAfetler.LOGGER.info("Eşya GameTest: temel tarifler ilk tick'te, gelişmişler temel eşyayla tarif kitabında");
        yardimci.succeed();
    }

    /**
     * Yüklü her {@code dogalafetler} tarifi bir tarif kitabı başarımının ({@code dogalafetler:recipes/**}) ödül listesinde
     * ({@code rewards.recipes}); yoksa tarif kitabında hiç görünmez. Özel tarifler ({@code isSpecial}, zırh ekleri) vanilla kitapta zaten görünmez, muaf. Meteor tarifleri
     * girdi meteor eşyası envantere girince açılır.
     */
    @GameTest(template = "bos", batch = "esya_tarif")
    public static void herTarifinKitapKilidiVarMeteorTarifleriGirdiyleAcilir(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        Set<ResourceLocation> kilitler = new HashSet<>();
        for (Advancement a : server.getAdvancements().getAllAdvancements()) {
            if (DogalAfetler.MOD_ID.equals(a.getId().getNamespace()) && a.getId().getPath().startsWith("recipes/")) {
                kilitler.addAll(Arrays.asList(a.getRewards().getRecipes()));
            }
        }
        List<String> kilitsiz = new ArrayList<>();
        List<String> ozel = new ArrayList<>();
        int sayi = 0;
        for (Recipe<?> tarif : server.getRecipeManager().getRecipes()) {
            if (!DogalAfetler.MOD_ID.equals(tarif.getId().getNamespace())) {
                continue;
            }
            if (tarif.isSpecial()) {
                ozel.add(tarif.getId().getPath());
            } else if (!kilitler.contains(tarif.getId())) {
                kilitsiz.add(tarif.getId().getPath());
            } else {
                sayi++;
            }
        }
        yardimci.assertTrue(kilitsiz.isEmpty(), "tarif kitabı başarımı olmayan tarifler: " + kilitsiz);
        yardimci.assertTrue(sayi > 0, "hiç dogalafetler tarifi yüklenmedi");
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(yardimci, 0.5, 2, 0.5);
        try {
            ResourceLocation kulce = DogalAfetler.konum("meteorit_kulcesi");
            ResourceLocation plaka = DogalAfetler.konum("termostat_plakasi");
            yardimci.assertTrue(!oyuncu.getRecipeBook().contains(kulce) && !oyuncu.getRecipeBook().contains(plaka),
                    "meteor tarifleri girdisiz kitapta");
            envantereKoy(oyuncu, MeteorEsyaKayitlari.METEORIT_PARCASI.get());
            yardimci.assertTrue(oyuncu.getRecipeBook().contains(kulce), "Meteorit Parçasıyla kitapta değil: " + kulce);
            envantereKoy(oyuncu, MeteorEsyaKayitlari.METEORIT_KULCESI.get());
            yardimci.assertTrue(oyuncu.getRecipeBook().contains(plaka), "Meteorit Külçesiyle kitapta değil: " + plaka);
        } finally {
            server.getPlayerList().remove(oyuncu);
        }
        DogalAfetler.LOGGER.info("Eşya GameTest: {} tarifin kitap kilidi var, özel (kitapsız) {}", sayi, ozel);
        yardimci.succeed();
    }

    private static void calismaMasasi(GameTestHelper yardimci, ServerPlayer oyuncu, ResourceLocation kimlik, Item sonuc,
            Item... dokuz) {
        ServerLevel level = yardimci.getLevel();
        TransientCraftingContainer tezgah = tezgah(oyuncu, dokuz);
        Optional<CraftingRecipe> eslesen = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
        yardimci.assertTrue(eslesen.isPresent() && eslesen.get().getId().equals(kimlik), "çalışma masası bulmadı: " + kimlik);
        yardimci.assertTrue(eslesen.get().assemble(tezgah, level.registryAccess()).is(sonuc), "ürün yanlış: " + kimlik);
    }

    /** Tarif kitabı başarımı ({@code recipes/tools/<tarif>}) tamam ve tarif oyuncunun kitabında. */
    private static boolean kitapta(GameTestHelper yardimci, MinecraftServer server, ServerPlayer oyuncu,
            ResourceLocation tarif) {
        Advancement kilit = server.getAdvancements().getAdvancement(tarif.withPrefix("recipes/tools/"));
        yardimci.assertTrue(kilit != null, "tarif kitabı başarımı yok: " + tarif);
        boolean acik = oyuncu.getAdvancements().getOrStartProgress(kilit).isDone();
        yardimci.assertTrue(acik == oyuncu.getRecipeBook().contains(tarif), "başarım ve tarif kitabı uyuşmuyor: " + tarif);
        return acik;
    }

    private static void envantereKoy(ServerPlayer oyuncu, Item... esyalar) {
        for (Item esya : esyalar) {
            oyuncu.getInventory().add(new ItemStack(esya));
        }
        oyuncu.inventoryMenu.broadcastChanges();
    }

    private static void hucreler(GameTestHelper yardimci, Ingredient[] desen, String ad, Item... beklenen) {
        for (int i = 0; i < 9; i++) {
            yardimci.assertTrue(desen[i].test(new ItemStack(beklenen[i])), ad + " hücre " + i + ": " + beklenen[i]);
            yardimci.assertTrue(!desen[i].test(new ItemStack(Items.DIRT)), ad + " hücre " + i + " toprak kabul ediyor");
        }
    }

    private static void gelismisTarif(GameTestHelper yardimci, ServerPlayer oyuncu, ResourceLocation kimlik, Item temel,
            Item sonuc, Item cekirdek) {
        ServerLevel level = yardimci.getLevel();
        Optional<? extends Recipe<?>> tarif = level.getRecipeManager().byKey(kimlik);
        yardimci.assertTrue(tarif.isPresent() && tarif.get().getIngredients().size() == 2, "şekilsiz tarif: " + kimlik);
        TransientCraftingContainer tezgah = tezgah(oyuncu, temel, cekirdek);
        Optional<CraftingRecipe> eslesen = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
        yardimci.assertTrue(eslesen.isPresent() && eslesen.get().assemble(tezgah, level.registryAccess()).is(sonuc),
                "Kozmik Çekirdekle craft edilmedi: " + kimlik);
    }

    private static TransientCraftingContainer tezgah(ServerPlayer oyuncu, Item... girdi) {
        TransientCraftingContainer tezgah = new TransientCraftingContainer(new CraftingMenu(0, oyuncu.getInventory()), 3, 3);
        for (int i = 0; i < girdi.length; i++) {
            tezgah.setItem(i, new ItemStack(girdi[i]));
        }
        return tezgah;
    }

    private static Ingredient[] desen(GameTestHelper yardimci, MinecraftServer server, ResourceLocation kimlik, Item sonuc) {
        Optional<? extends Recipe<?>> tarif = server.getRecipeManager().byKey(kimlik);
        yardimci.assertTrue(tarif.isPresent(), "tarif yok: " + kimlik);
        yardimci.assertTrue(tarif.get().getResultItem(server.registryAccess()).is(sonuc), "tarif sonucu: " + kimlik);
        List<Ingredient> malzemeler = tarif.get().getIngredients();
        yardimci.assertTrue(malzemeler.size() == 9, "3x3 değil: " + kimlik);
        return malzemeler.toArray(Ingredient[]::new);
    }
}
