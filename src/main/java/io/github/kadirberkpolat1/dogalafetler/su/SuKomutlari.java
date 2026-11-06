package io.github.kadirberkpolat1.dogalafetler.su;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Sanal su komutları (KOMUTLAR, N18): {@code /dogalafet denizseviyesi <değer> [kademeli]}, {@code /dogalafet aysonrasi atla}
 * ve doğrulama için {@code /dogalafet su bilgi|dalga|magara|cekilme|temizle|kiyi}. Yalnız OP (kök komut).
 */
public final class SuKomutlari {
    static final String ON = "commands.dogalafetler.su.";
    static final String DENIZ = ON + "deniz";
    static final String DENIZ_KADEMELI = ON + "deniz_kademeli";
    static final String ATLA = ON + "aysonrasi.";
    static final String BILGI = ON + "bilgi";
    static final String DALGA = ON + "dalga";
    static final String MAGARA = ON + "magara";
    static final String MAGARA_YOK = ON + "magara_yok";
    static final String CEKILME = ON + "cekilme";
    static final String TEMIZLE = ON + "temizle";
    static final String KIYI = ON + "kiyi";
    static final String KIYI_YOK = ON + "kiyi_yok";
    static final String ASAMA_BASLADI = ON + "asama.basladi";
    static final String ASAMA_HEDEF = ON + "asama.hedef";
    static final String ASAMA_GERI = ON + "asama.geri";
    static final String ASAMA_MAGARA = ON + "asama.magara";

    private SuKomutlari() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("denizseviyesi")
                .then(Commands.argument("deger", FloatArgumentType.floatArg(0f, DenizAlani.EN_COK_ARTIS))
                        .executes(k -> deniz(k, false))
                        .then(Commands.literal("kademeli").executes(k -> deniz(k, true)))));
        KomutKaydi.ekle(Commands.literal("aysonrasi").then(Commands.literal("atla").executes(SuKomutlari::atla)));
        KomutKaydi.ekle(Commands.literal("su")
                .then(Commands.literal("bilgi").executes(SuKomutlari::bilgi))
                .then(Commands.literal("dalga").executes(k -> dalga(k, 10))
                        .then(Commands.argument("yukseklik", IntegerArgumentType.integer(1, 30))
                                .executes(k -> dalga(k, IntegerArgumentType.getInteger(k, "yukseklik")))))
                .then(Commands.literal("magara").then(Commands.argument("konum", BlockPosArgument.blockPos())
                        .executes(k -> magara(k, 4000, -1))
                        .then(Commands.argument("hacim", IntegerArgumentType.integer(1, 20000))
                                .executes(k -> magara(k, IntegerArgumentType.getInteger(k, "hacim"), -1))
                                .then(Commands.argument("kalisSn", IntegerArgumentType.integer(0, 86400))
                                        .executes(k -> magara(k, IntegerArgumentType.getInteger(k, "hacim"),
                                                IntegerArgumentType.getInteger(k, "kalisSn")))))))
                .then(Commands.literal("cekilme").executes(k -> cekilme(k, 24, 3))
                        .then(Commands.argument("yaricap", IntegerArgumentType.integer(4, 96))
                                .then(Commands.argument("derinlik", IntegerArgumentType.integer(1, 12))
                                        .executes(k -> cekilme(k, IntegerArgumentType.getInteger(k, "yaricap"),
                                                IntegerArgumentType.getInteger(k, "derinlik"))))))
                .then(Commands.literal("temizle").executes(SuKomutlari::temizle))
                .then(Commands.literal("kiyi").executes(SuKomutlari::kiyi)));
        DataGenerators.ceviriEkle(SuKomutlari::cevirileriYaz);
    }

    static String sayi(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static void yaz(CommandContext<CommandSourceStack> k, Component c) {
        k.getSource().sendSuccess(() -> c, true);
    }

    private static int deniz(CommandContext<CommandSourceStack> k, boolean kademeli) {
        float hedef = DenizYukselmesiSureci.ORNEK.ayarla(k.getSource().getServer(), FloatArgumentType.getFloat(k, "deger"),
                kademeli);
        yaz(k, Component.translatable(kademeli ? DENIZ_KADEMELI : DENIZ, sayi(hedef)));
        return 1;
    }

    private static int atla(CommandContext<CommandSourceStack> k) {
        DenizYukselmesiSureci.AtlaSonucu s = DenizYukselmesiSureci.ORNEK.atla(k.getSource().getServer());
        Component c = Component.translatable(ATLA + s.name().toLowerCase(Locale.ROOT));
        if (s == DenizYukselmesiSureci.AtlaSonucu.BASLADI) {
            yaz(k, c);
            return 1;
        }
        k.getSource().sendFailure(c);
        return 0;
    }

    private static int bilgi(CommandContext<CommandSourceStack> k) {
        ServerLevel l = k.getSource().getLevel();
        SanalSu.Boyut b = SanalSu.boyut(l);
        SanalSuDunyasi d = b.dunya;
        Vec3 p = k.getSource().getPosition();
        yaz(k, Component.translatable(BILGI, l.dimension().location().toString(), sayi(d.denizArtisi()), d.dalgalar().size(),
                d.magaralar().size(), d.cekilmeler().size(), d.alan().parcaSayisi(), d.alan().sonGovdeSayisi(),
                sayi(d.alan().sonHesapNs() / 1e6), sayi(SanalSu.suSeviyesi(l, (int) Math.floor(p.x), (int) Math.floor(p.z)))));
        return 1;
    }

    /** Oyuncunun baktığı yönden 70 blok öteden ona doğru gelen test dalgası (Faz 4 tsunamisinin yerine). */
    private static int dalga(CommandContext<CommandSourceStack> k, int yukseklik) throws CommandSyntaxException {
        ServerPlayer p = k.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        Vec3 bak = p.getLookAngle();
        double boy = Math.hypot(bak.x, bak.z);
        double fx = boy < 1e-3 ? 1 : bak.x / boy;
        double fz = boy < 1e-3 ? 0 : bak.z / boy;
        // Taban gerçek su yüzeyinin altında kalırsa dalga gerçek suyun içinde görünmez: sütunun su/kara yüzeyi ile sınırla.
        double taban = Math.min(l.getSeaLevel(), l.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ())) - 1;
        int id = SanalSu.dalgaBaslat(l, new Dalga(p.getX() + fx * 70, p.getZ() + fz * 70, -fx, -fz, SanalSu.sunucuSaati, 0.3,
                yukseklik, 120, 40, taban, 140));
        yaz(k, Component.translatable(DALGA, id, yukseklik));
        return id;
    }

    private static int magara(CommandContext<CommandSourceStack> k, int hacim, int kalisSn) throws CommandSyntaxException {
        ServerLevel l = k.getSource().getLevel();
        BlockPos konum = BlockPosArgument.getLoadedBlockPos(k, "konum");
        long kalis = (kalisSn < 0 ? SuAyarlari.sunucu().magaraKalisSn.get() : kalisSn) * 20L;
        int id = SanalSu.magaraDoldur(l, konum, hacim, kalis);
        if (id < 0) {
            k.getSource().sendFailure(Component.translatable(MAGARA_YOK));
            return 0;
        }
        SanalSuDunyasi.Magara m = SanalSu.boyut(l).dunya.magaralar().get(id);
        yaz(k, Component.translatable(MAGARA, id, m.cep().hacim(), m.cep().ustY()));
        return id;
    }

    private static int cekilme(CommandContext<CommandSourceStack> k, int yaricap, int derinlik) {
        ServerLevel l = k.getSource().getLevel();
        Vec3 p = k.getSource().getPosition();
        int id = SanalSu.cekilmeBaslat(l, new Cekilme(p.x, p.z, yaricap, derinlik, (long) SanalSu.sunucuSaati, 300));
        yaz(k, Component.translatable(CEKILME, id, yaricap, derinlik));
        return id;
    }

    private static int temizle(CommandContext<CommandSourceStack> k) {
        ServerLevel l = k.getSource().getLevel();
        SanalSu.Boyut b = SanalSu.boyut(l);
        int dalga = b.dunya.dalgalar().size();
        int magara = b.dunya.magaralar().size();
        int cekilme = b.dunya.cekilmeler().size();
        b.dunya.temizle();
        b.magaraKutusu.clear();
        SanalSu.yayinla(l);
        yaz(k, Component.translatable(TEMIZLE, dalga, magara, cekilme));
        return 1;
    }

    /**
     * Vitrin için yüklü chunk'larda en iyi kumsalı bulur (deniz seviyesindeki kara, denize doğru en az 6 su sütunu, karaya
     * doğru deniz + 1..5 arası en az 8 alçak sütun, hepsi yaprak örtüsüz; puan = ikisinin toplamı, karası batıda olana +8) ve oyuncuyu izleyici olarak 16 blok açığa,
     * deniz + 7 yükseklikte kıyıya bakacak şekilde taşır (+4 yükselmeden sonra da kamera 3 blok suyun üstünde).
     */
    private static int kiyi(CommandContext<CommandSourceStack> k) throws CommandSyntaxException {
        ServerPlayer p = k.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        int deniz = l.getSeaLevel();
        int px = p.getBlockX();
        int pz = p.getBlockZ();
        int[][] yonler = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int enPuan = 0;
        int[] en = null;
        for (int x = px - 160; x <= px + 160; x += 2) {
            for (int z = pz - 160; z <= pz + 160; z += 2) {
                if (!kara(l, x, z, deniz, deniz + 1)) {
                    continue;
                }
                for (int[] d : yonler) {
                    // Karası batıda olan kıyı öncelikli: kamera batıya, gün batımında güneşe ve yansımasına bakar (vitrin).
                    int puan = kumsalPuani(l, x, z, d, deniz);
                    if (puan > 0 && d[0] < 0) {
                        puan += 8;
                    }
                    if (puan > enPuan) {
                        enPuan = puan;
                        en = new int[] {x, z, d[0], d[1]};
                    }
                }
            }
        }
        if (en == null) {
            k.getSource().sendFailure(Component.translatable(KIYI_YOK));
            return 0;
        }
        double x = en[0] + 0.5 - en[2] * 16;
        double z = en[1] + 0.5 - en[3] * 16;
        float yaw = (float) Math.toDegrees(Math.atan2(-en[2], en[3]));
        p.setGameMode(GameType.SPECTATOR);
        p.teleportTo(l, x, deniz + 7, z, yaw, 14f);
        yaz(k, Component.translatable(KIYI, en[0], deniz, en[1]));
        return 1;
    }

    /** Kumsal puanı (0: uygun değil). */
    private static int kumsalPuani(ServerLevel l, int x, int z, int[] d, int deniz) {
        int su = 0;
        while (su < 10 && su(l, x - d[0] * (su + 1), z - d[1] * (su + 1), deniz)) {
            su++;
        }
        if (su < 6) {
            return 0;
        }
        int kara = 0;
        for (int i = 1; i <= 12; i++) {
            if (kara(l, x + d[0] * i, z + d[1] * i, deniz + 1, deniz + 5)) {
                kara++;
            }
        }
        return kara < 8 ? 0 : su + kara;
    }

    /** Yalnız yüklü chunk'lar (arama chunk üretmez); yüklü değilse {@link Integer#MIN_VALUE}. */
    private static int yuzey(ServerLevel l, int x, int z) {
        if (!l.hasChunk(x >> 4, z >> 4)) {
            return Integer.MIN_VALUE;
        }
        return l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static boolean su(ServerLevel l, int x, int z, int deniz) {
        int y = yuzey(l, x, z);
        return y == deniz && acik(l, x, z, y) && l.getFluidState(new BlockPos(x, y - 1, z)).is(FluidTags.WATER);
    }

    private static boolean kara(ServerLevel l, int x, int z, int enAz, int enCok) {
        int y = yuzey(l, x, z);
        return y >= enAz && y <= enCok && acik(l, x, z, y) && l.getFluidState(new BlockPos(x, y - 1, z)).isEmpty();
    }

    /** Sütunun üstünde yaprak örtüsü yok: kamera ve oyuncu ağaç tacına takılmaz. */
    private static boolean acik(ServerLevel l, int x, int z, int y) {
        return l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) == y;
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle("donem_sureci.dogalafetler.deniz_yukselmesi", "sea level rise", "deniz yükselmesi");
        y.ekle("donem_sureci.dogalafetler.magara_suyu", "cave water", "mağara suyu");
        y.ekle(DENIZ, "Sea level set to +%s blocks", "Deniz seviyesi +%s blok yapıldı");
        y.ekle(DENIZ_KADEMELI, "Sea level is moving to +%s blocks", "Deniz seviyesi kademeli olarak +%s bloka gidiyor");
        y.ekle(ATLA + "basladi", "Waiting skipped: the sea is rising", "Bekleme atlandı: deniz yükseliyor");
        y.ekle(ATLA + "ay_var", "The moon is still there; no post-impact period", "Ay hâlâ yerinde; ay sonrası dönem yok");
        y.ekle(ATLA + "bitti", "Sea level rise is over for this world (evaporation)",
                "Bu dünyada deniz yükselmesi bitti (buharlaşma)");
        y.ekle(ATLA + "zaten", "The sea is already rising or has risen", "Deniz zaten yükseliyor ya da yükseldi");
        y.ekle(BILGI, "Virtual water (%s): sea +%s, waves %s, caves %s, drawdowns %s; field %s chunks, %s bodies, last %s ms; here: %s",
                "Sanal su (%s): deniz +%s, dalga %s, mağara %s, çekilme %s; alan %s chunk, %s kütle, son hesap %s ms; burada: %s");
        y.ekle(DALGA, "Test wave #%s started (height %s)", "Test dalgası #%s başladı (yükseklik %s)");
        y.ekle(MAGARA, "Cave water #%s: %s cells, surface y=%s", "Mağara suyu #%s: %s hücre, yüzey y=%s");
        y.ekle(MAGARA_YOK, "No cave space to fill here", "Burada dolacak mağara boşluğu yok");
        y.ekle(CEKILME, "Drawdown #%s: radius %s, depth %s", "Çekilme #%s: yarıçap %s, derinlik %s");
        y.ekle(TEMIZLE, "Virtual water cleared: %s waves, %s caves, %s drawdowns",
                "Sanal su temizlendi: %s dalga, %s mağara, %s çekilme");
        y.ekle(KIYI, "Beach at %s %s %s", "Kumsal: %s %s %s");
        y.ekle(KIYI_YOK, "No beach found nearby", "Yakında kumsal bulunamadı");
        y.ekle(ASAMA_BASLADI, "Sea level rise: waiting skipped, rising", "Deniz yükselmesi: bekleme atlandı, yükseliyor");
        y.ekle(ASAMA_HEDEF, "Sea level rise: reached +%s blocks", "Deniz yükselmesi: +%s bloka ulaştı");
        y.ekle(ASAMA_GERI, "Sea level rise: receding begins", "Deniz yükselmesi: geri çekilme başlıyor");
        y.ekle(ASAMA_MAGARA, "Cave water: %s pockets moved to the next stage", "Mağara suyu: %s cep bir sonraki aşamaya geçti");
    }
}
