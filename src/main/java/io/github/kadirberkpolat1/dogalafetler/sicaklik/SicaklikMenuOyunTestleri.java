package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekKayitlari;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Termometre menüsünün oyun içi testleri (S3-08): sunucunun menüye yolladığı döküm, oyuncunun menüde göreceği değerleri
 * taşır; kapı kapalıyken istek yanıtsız kalır. Sahte oyuncular tick almaz; istek {@link SicaklikDokumIstegi#yanitla} ile
 * ağdaki işleyicinin yolundan karşılanır.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SicaklikMenuOyunTestleri {
    private static final String BATCH = "sicaklik_menu";

    private SicaklikMenuOyunTestleri() {
    }

    private static ServerPlayer oyuncu(GameTestHelper y, String ad, double dx) {
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), ad), m.getX() + 0.5 + dx,
                m.getY(), m.getZ() + 0.5);
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

    private static GameRules.BooleanValue kuraliAc(MinecraftServer server) {
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.SICAKLIK_SISTEMI);
        kural.set(true, server);
        return kural;
    }

    /** Islaklık ve kuruma yağmura bağlı; bu testler açık havada ölçer. */
    private static void havaAcik(GameTestHelper y) {
        y.getLevel().setWeatherParameters(6000, 0, false, false);
    }

    private static SicaklikDokumPaketi dokum(GameTestHelper y, ServerPlayer o) {
        SicaklikDokumPaketi p = SicaklikDokumIstegi.yanitla(o);
        if (p == null) {
            y.fail(o.getGameProfile().getName() + ": kapı açıkken döküm isteği yanıtlanmadı");
        }
        return p;
    }

    /** Menünün iki sütunu toplanabilir: mekan kalemleri mekanı, hissedilen düzeltmeleri farka eşittir. */
    private static void dokumTutarli(GameTestHelper y, SicaklikDokumPaketi p, String ad) {
        double mekan = 0;
        double duzeltme = 0;
        for (Katki k : Katki.values()) {
            float d = p.katki(k);
            if (Float.isNaN(d)) {
                continue;
            }
            if (k.hissedilen()) {
                duzeltme += d;
            } else {
                mekan += d;
            }
        }
        dogrula(y, Math.abs(mekan - p.mekan()) < 0.01, ad + ": mekan kalemleri " + mekan + " ≠ mekan " + p.mekan());
        dogrula(y, Math.abs(p.mekan() + duzeltme - p.hissedilen()) < 0.01,
                ad + ": mekan + düzeltmeler " + (p.mekan() + duzeltme) + " ≠ hissedilen " + p.hissedilen());
    }

    /** S3-05: sol eldeki Buz Kovası serinletir, lav kovası ısıtır; ana eldeki kova dökümde kalem açmaz. */
    @GameTest(template = "bos", batch = BATCH)
    public static void solEldekiKovaDokumdeSolElKalemiOlur(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        havaAcik(y);
        ServerPlayer buzlu = oyuncu(y, "menu-buz", 0);
        ServerPlayer lavli = oyuncu(y, "menu-lav", 0.3);
        ServerPlayer anaElde = oyuncu(y, "menu-anael", 0.6);
        try {
            buzlu.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(YiyecekKayitlari.BUZ_KOVASI.get()));
            lavli.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LAVA_BUCKET));
            anaElde.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(YiyecekKayitlari.BUZ_KOVASI.get()));
            SicaklikDokumPaketi b = dokum(y, buzlu);
            SicaklikDokumPaketi l = dokum(y, lavli);
            SicaklikDokumPaketi a = dokum(y, anaElde);
            dogrula(y, b.solEl() == SolEl.BUZ_KOVASI, "sol el Buz Kovası görünmeli: " + b.solEl());
            dogrula(y, b.katki(Katki.SOL_EL) < 0, "Buz Kovası kalemi eksi olmalı: " + b.katki(Katki.SOL_EL));
            dogrula(y, b.hissedilen() < b.mekan(), "Buz Kovasıyla hissedilen mekandan soğuk: " + b.hissedilen());
            dogrula(y, l.solEl() == SolEl.LAV_KOVASI, "sol el lav kovası görünmeli: " + l.solEl());
            dogrula(y, l.katki(Katki.SOL_EL) > 0, "lav kovası kalemi artı olmalı: " + l.katki(Katki.SOL_EL));
            dogrula(y, a.solEl() == SolEl.YOK && Float.isNaN(a.katki(Katki.SOL_EL)),
                    "ana eldeki kova kalem açmamalı: " + a.solEl() + " " + a.katki(Katki.SOL_EL));
            dokumTutarli(y, b, "buzlu");
            dokumTutarli(y, l, "lavlı");
            y.succeed();
        } finally {
            cikar(y, List.of(buzlu, lavli, anaElde));
        }
    }

    /** Lav kaynağının yanında mekan katkılarında "Kaynaklar" artı görünür ve mekan o kadar ısınır. */
    @GameTest(template = "bos", batch = BATCH)
    public static void lavYanindaKaynaklarKalemiArtiOlur(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        havaAcik(y);
        BlockPos lav = new BlockPos(2, 1, 0);
        ServerPlayer once = oyuncu(y, "menu-lavsiz", 0);
        ServerPlayer sonra = oyuncu(y, "menu-lavli", 0);
        try {
            SicaklikDokumPaketi soguk = dokum(y, once);
            y.setBlock(lav, Blocks.LAVA);
            SicaklikDokumPaketi sicak = dokum(y, sonra);
            y.setBlock(lav, Blocks.AIR);
            dogrula(y, Float.isNaN(soguk.katki(Katki.KAYNAKLAR)), "lavsız yerde kaynak kalemi olmamalı: "
                    + soguk.katki(Katki.KAYNAKLAR));
            float kaynak = sicak.katki(Katki.KAYNAKLAR);
            dogrula(y, kaynak > 1, "lav yanında Kaynaklar artı olmalı: " + kaynak);
            dogrula(y, Math.abs(sicak.mekan() - soguk.mekan() - kaynak) < 0.01,
                    "mekan kaynak kadar ısınmalı: " + soguk.mekan() + " → " + sicak.mekan() + " (kaynak " + kaynak + ")");
            dokumTutarli(y, sicak, "lav");
            y.succeed();
        } finally {
            y.setBlock(lav, Blocks.AIR);
            cikar(y, List.of(once, sonra));
        }
    }

    /** S3-03: ıslak oyuncu menüde ıslaklık yüzdesini, eksi ıslaklık kalemini ve kalan kuruma süresini görür. */
    @GameTest(template = "bos", batch = BATCH)
    public static void islakOyuncuKurumaSuresiVeIslaklikKalemiGorur(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        havaAcik(y);
        ServerPlayer islak = oyuncu(y, "menu-islak", 0);
        ServerPlayer kuru = oyuncu(y, "menu-kuru", 0.5);
        try {
            VucutSicakligi.islaklikAyarla(islak, 0.6);
            VucutSicakligi.islaklikAyarla(kuru, 0);
            SicaklikDokumPaketi i = dokum(y, islak);
            SicaklikDokumPaketi k = dokum(y, kuru);
            dogrula(y, Math.abs(i.islaklik() - 0.6f) < 1e-4, "ıslaklık %60 görünmeli: " + i.islaklik());
            dogrula(y, i.katki(Katki.ISLAKLIK) < 0, "ıslaklık kalemi eksi olmalı: " + i.katki(Katki.ISLAKLIK));
            dogrula(y, i.kurumaSaniye() > 0, "açık havada kuruma süresi artı olmalı: " + i.kurumaSaniye());
            dogrula(y, i.hissedilen() < k.hissedilen(), "ıslak oyuncu daha soğuk hissetmeli: " + i.hissedilen() + " / "
                    + k.hissedilen());
            dogrula(y, k.kurumaSaniye() == -1 && Float.isNaN(k.katki(Katki.ISLAKLIK)),
                    "kuru oyuncuda kuruma ve ıslaklık kalemi olmamalı: " + k.kurumaSaniye() + " " + k.katki(Katki.ISLAKLIK));
            dokumTutarli(y, i, "ıslak");
            y.succeed();
        } finally {
            cikar(y, List.of(islak, kuru));
        }
    }

    /** Menünün vücut sayısı ve eğilim oku: üşümüş vücut ısınma, ateşlenmiş vücut soğuma yönünde görünür. */
    @GameTest(template = "bos", batch = BATCH)
    public static void vucutVeEgilimOyuncununDurumunuGosterir(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        havaAcik(y);
        ServerPlayer usuyen = oyuncu(y, "menu-usuyen", 0);
        ServerPlayer bunalan = oyuncu(y, "menu-bunalan", 0.5);
        try {
            VucutSicakligi.yaz(usuyen, -80);
            VucutSicakligi.yaz(bunalan, 80);
            SicaklikDokumPaketi u = dokum(y, usuyen);
            SicaklikDokumPaketi b = dokum(y, bunalan);
            dogrula(y, u.vucut() == -80f && b.vucut() == 80f, "vücut değerleri: " + u.vucut() + " / " + b.vucut());
            dogrula(y, u.egilim() > 0, "33 °C'deki vücut ılık mekanda ısınmalı: " + u.egilim());
            dogrula(y, b.egilim() < 0, "41 °C'deki vücut ılık mekanda soğumalı: " + b.egilim());
            y.succeed();
        } finally {
            cikar(y, List.of(usuyen, bunalan));
        }
    }

    /**
     * Kapı kapalıyken istek yanıtsız kalır (ağa hiçbir şey gitmez); açıkken yanıtlanır, aynı oyuncunun 10 tick içindeki
     * ikinci isteği düşer, 10 tick sonra yeniden yanıtlanır.
     */
    @GameTest(template = "bos", batch = BATCH, timeoutTicks = 100)
    public static void kapiKapaliykenIstekYanitsizAcikkenSinirli(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = kuraliAc(server);
        ServerPlayer o = oyuncu(y, "menu-kapi", 0);
        try {
            kural.set(false, server);
            ozelPaketSayisi(o);
            SicaklikDokumPaketi kapali = SicaklikDokumIstegi.yanitla(o);
            int gidenKapali = ozelPaketSayisi(o);
            kural.set(true, server);
            dogrula(y, kapali == null && gidenKapali == 0,
                    "kapı kapalıyken yanıt gitmemeli: " + kapali + ", " + gidenKapali + " paket");
            ozelPaketSayisi(o);
            SicaklikDokumPaketi acik = SicaklikDokumIstegi.yanitla(o);
            dogrula(y, acik != null && ozelPaketSayisi(o) == 1, "kapı açıkken tek döküm paketi gitmeli");
            dogrula(y, SicaklikDokumIstegi.yanitla(o) == null && ozelPaketSayisi(o) == 0,
                    "10 tick dolmadan ikinci istek yanıtlanmamalı");
        } catch (RuntimeException hata) {
            kural.set(true, server);
            cikar(y, List.of(o));
            throw hata;
        }
        y.runAfterDelay(SicaklikDokumIstegi.EN_KISA_ARALIK, () -> {
            try {
                dogrula(y, SicaklikDokumIstegi.yanitla(o) != null, "10 tick sonra istek yeniden yanıtlanmalı");
                y.succeed();
            } finally {
                kural.set(true, server);
                cikar(y, List.of(o));
            }
        });
    }

    /** Sahte oyuncunun kanalına giden mod kanalı paketlerinin sayısı; giden kuyruğu boşaltır. */
    private static int ozelPaketSayisi(ServerPlayer o) {
        EmbeddedChannel kanal = (EmbeddedChannel) o.connection.connection.channel();
        int n = 0;
        for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
            if (mesaj instanceof ClientboundCustomPayloadPacket p && p.getIdentifier().equals(DogalAfetler.konum("ana"))) {
                n++;
            }
        }
        return n;
    }
}
