package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Spike komutları (yalnız deney; Faz 4 {@code /dogalafet start kasirga} ile değişir):
 *
 * <pre>
 * /dogalafet spike kasirga &lt;seviye 1-5&gt; [kalite dusuk|orta|yuksek|ultra]
 * /dogalafet spike kasirga dur
 * /dogalafet spike kasirga kalite &lt;kalite&gt;
 * /dogalafet spike kasirga hareket &lt;true|false&gt;
 * /dogalafet spike kasirga kamera &lt;mesafe&gt; [eğim]
 * </pre>
 *
 * Komutlar sohbete yazı göndermez (N6); olan sunucu log'una yazılır.
 */
public final class KasirgaSpikeKomutu {
    private static final SimpleCommandExceptionType KASIRGA_YOK = new SimpleCommandExceptionType(
            Component.literal("Kasırga yok"));
    private static final SimpleCommandExceptionType GECERSIZ_KALITE = new SimpleCommandExceptionType(
            Component.literal("Kalite: dusuk|orta|yuksek|ultra"));
    private static final double DOGUS_MESAFESI = 60;

    private KasirgaSpikeKomutu() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("spike").then(Commands.literal("kasirga")
                .then(Commands.literal("dur").executes(KasirgaSpikeKomutu::dur))
                .then(Commands.literal("kalite").then(kaliteArgumani().executes(KasirgaSpikeKomutu::kaliteDegistir)))
                .then(Commands.literal("hareket").then(Commands.argument("acik", BoolArgumentType.bool())
                        .executes(KasirgaSpikeKomutu::hareket)))
                .then(Commands.literal("kamera").then(Commands.argument("mesafe", DoubleArgumentType.doubleArg(5, 400))
                        .executes(b -> kamera(b, -12f))
                        .then(Commands.argument("egim", FloatArgumentType.floatArg(-90, 90))
                                .executes(b -> kamera(b, FloatArgumentType.getFloat(b, "egim"))))))
                .then(Commands.argument("seviye", IntegerArgumentType.integer(1, 5))
                        .executes(b -> baslat(b, -1))
                        .then(Commands.literal("kalite").then(kaliteArgumani()
                                .executes(b -> baslat(b, kalite(b))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> kaliteArgumani() {
        return Commands.argument("kalite", StringArgumentType.word()).suggests((b, oneri) ->
                SharedSuggestionProvider.suggest(List.of("dusuk", "orta", "yuksek", "ultra"), oneri));
    }

    private static int kalite(CommandContext<CommandSourceStack> b) throws CommandSyntaxException {
        String ad = StringArgumentType.getString(b, "kalite").toUpperCase(Locale.ROOT);
        try {
            return EfektKalitesi.valueOf(ad).ordinal();
        } catch (IllegalArgumentException hata) {
            throw GECERSIZ_KALITE.create();
        }
    }

    private static int baslat(CommandContext<CommandSourceStack> b, int kaliteSirasi) {
        CommandSourceStack kaynak = b.getSource();
        ServerLevel seviye = kaynak.getLevel();
        kasirgalariKaldir(kaynak);
        int sv = IntegerArgumentType.getInteger(b, "seviye");
        Vec3 konum = kaynak.getPosition();
        Vec2 bakis = kaynak.getRotation();
        double yaw = Math.toRadians(bakis.y);
        double bx = -Math.sin(yaw);
        double bz = Math.cos(yaw);
        double x = konum.x + bx * DOGUS_MESAFESI;
        double z = konum.z + bz * DOGUS_MESAFESI;
        double y = seviye.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        KasirgaVarligi kasirga = new KasirgaVarligi(KasirgaVarliklari.KASIRGA.get(), seviye);
        kasirga.setPos(x, y, z);
        // Oyuncuya doğru ilerler (tasarım notu §6: oyuncu çevresine doğru ±35° sapmalı yürüyüş).
        kasirga.kur(sv, kaliteSirasi, (float) Math.atan2(-bz, -bx));
        seviye.addFreshEntity(kasirga);
        DogalAfetler.LOGGER.info("[spike-kasirga] kasırga başladı: seviye={}, kalite={}, konum=({}, {}, {}), kimlik={}",
                sv, kaliteSirasi < 0 ? "istemci ayarı" : EfektKalitesi.values()[kaliteSirasi], Mth.floor(x),
                Mth.floor(y), Mth.floor(z), kasirga.getId());
        return 1;
    }

    private static int dur(CommandContext<CommandSourceStack> b) {
        int[] once = KasirgaSunucuIstatistik.say(b.getSource().getServer());
        int kaldirilan = kasirgalariKaldir(b.getSource());
        DogalAfetler.LOGGER.info("[spike-kasirga] dur: önce kasirga={}, ucan_blok={}; {} varlık kaldırıldı", once[0],
                once[1], kaldirilan);
        KasirgaSunucuIstatistik.sayimiZamanla();
        return 1;
    }

    private static int kasirgalariKaldir(CommandSourceStack kaynak) {
        List<Entity> silinecek = new ArrayList<>();
        for (ServerLevel seviye : kaynak.getServer().getAllLevels()) {
            for (Entity varlik : seviye.getAllEntities()) {
                if (varlik instanceof KasirgaVarligi || varlik instanceof UcanBlokVarligi) {
                    silinecek.add(varlik);
                }
            }
        }
        silinecek.forEach(Entity::discard);
        return silinecek.size();
    }

    private static List<KasirgaVarligi> kasirgalar(ServerLevel seviye) {
        List<KasirgaVarligi> liste = new ArrayList<>();
        for (Entity varlik : seviye.getAllEntities()) {
            if (varlik instanceof KasirgaVarligi kasirga && !kasirga.isRemoved()) {
                liste.add(kasirga);
            }
        }
        return liste;
    }

    private static int kaliteDegistir(CommandContext<CommandSourceStack> b) throws CommandSyntaxException {
        int sira = kalite(b);
        List<KasirgaVarligi> liste = kasirgalar(b.getSource().getLevel());
        liste.forEach(k -> k.kaliteSiralamasiniAyarla(sira));
        DogalAfetler.LOGGER.info("[spike-kasirga] kalite={} ({} kasırga)", EfektKalitesi.values()[sira], liste.size());
        return liste.size();
    }

    private static int hareket(CommandContext<CommandSourceStack> b) {
        boolean acik = BoolArgumentType.getBool(b, "acik");
        List<KasirgaVarligi> liste = kasirgalar(b.getSource().getLevel());
        liste.forEach(k -> k.hareketiAyarla(acik));
        DogalAfetler.LOGGER.info("[spike-kasirga] hareket={} ({} kasırga)", acik, liste.size());
        return liste.size();
    }

    /**
     * Oyuncuyu kasırganın ilerleme yönünde (kasırga kameraya doğru gelir) {@code mesafe} blok öteye, zeminin 3 blok
     * üstüne koyar ve kasırgaya baktırır (vitrin: tasarım notu §3).
     */
    private static int kamera(CommandContext<CommandSourceStack> b, float egim) throws CommandSyntaxException {
        ServerPlayer oyuncu = b.getSource().getPlayerOrException();
        ServerLevel seviye = oyuncu.serverLevel();
        KasirgaVarligi kasirga = kasirgalar(seviye).stream().findFirst().orElseThrow(KASIRGA_YOK::create);
        double mesafe = DoubleArgumentType.getDouble(b, "mesafe");
        double x = kasirga.getX() + Math.cos(kasirga.yon()) * mesafe;
        double z = kasirga.getZ() + Math.sin(kasirga.yon()) * mesafe;
        double y = seviye.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 3;
        float yaw = (float) (Mth.atan2(kasirga.getZ() - z, kasirga.getX() - x) * Mth.RAD_TO_DEG) - 90f;
        oyuncu.teleportTo(seviye, x, y, z, yaw, egim);
        DogalAfetler.LOGGER.info("[spike-kasirga] kamera: mesafe={}, eğim={}, konum=({}, {}, {}), yaw={}", mesafe, egim,
                String.format("%.1f", x), String.format("%.1f", y), String.format("%.1f", z),
                String.format("%.1f", yaw));
        return 1;
    }
}
