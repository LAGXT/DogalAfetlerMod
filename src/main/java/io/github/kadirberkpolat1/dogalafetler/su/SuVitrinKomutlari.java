package io.github.kadirberkpolat1.dogalafetler.su;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Geliştirme komutları (SU-31): {@code /dogalafet su vitrin [kamera iskele|sualti|tepe] | vitrin dalga <yükseklik>} ve
 * {@code /dogalafet su ay var|yok}. Vitrin gerçek blok koyar, ay komutu Faz 4 ay çarpmasını atlar; ikisi de yalnız
 * {@link SuKomutlari#gelistirmeKomutlari()} doğruyken kaydedilir. Bu sınıf başka yerden anılmaz: üretimde ne o ne
 * {@code SuVitrin} yüklenir.
 */
final class SuVitrinKomutlari {
    private SuVitrinKomutlari() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("su")
                .then(Commands.literal("vitrin").executes(SuVitrinKomutlari::vitrin)
                        .then(Commands.literal("kamera")
                                .then(Commands.literal("iskele").executes(k -> vitrinKamera(k, SuVitrin.Kamera.ISKELE)))
                                .then(Commands.literal("sualti").executes(k -> vitrinKamera(k, SuVitrin.Kamera.SUALTI)))
                                .then(Commands.literal("tepe").executes(k -> vitrinKamera(k, SuVitrin.Kamera.TEPE))))
                        .then(Commands.literal("dalga").then(Commands.argument("yukseklik", IntegerArgumentType.integer(1, 30))
                                .executes(k -> vitrinDalga(k, IntegerArgumentType.getInteger(k, "yukseklik"))))))
                .then(Commands.literal("ay")
                        .then(Commands.literal("var").executes(k -> ay(k, true)))
                        .then(Commands.literal("yok").executes(k -> ay(k, false)))));
    }

    /** Vitrin sahnesi (SU-29): kumsala iskele ve kıyı köyü kurar, kamera iskelenin ucunda. */
    static int vitrin(CommandContext<CommandSourceStack> k) throws CommandSyntaxException {
        ServerPlayer p = k.getSource().getPlayerOrException();
        SuVitrin.Sahne s = SuVitrin.kur(p.serverLevel(), p.getBlockX(), p.getBlockZ());
        if (s == null) {
            k.getSource().sendFailure(Component.translatable(SuKomutlari.KIYI_YOK));
            return 0;
        }
        SuVitrin.kamera(p, SuVitrin.Kamera.ISKELE);
        SuKomutlari.yaz(k, Component.translatable(SuKomutlari.VITRIN, s.x(), s.deniz(), s.z()));
        return 1;
    }

    private static int vitrinKamera(CommandContext<CommandSourceStack> k, SuVitrin.Kamera ne) throws CommandSyntaxException {
        if (!SuVitrin.kamera(k.getSource().getPlayerOrException(), ne)) {
            k.getSource().sendFailure(Component.translatable(SuKomutlari.VITRIN_YOK));
            return 0;
        }
        SuKomutlari.yaz(k, Component.translatable(SuKomutlari.VITRIN_KAMERA, ne.name().toLowerCase(Locale.ROOT)));
        return 1;
    }

    private static int vitrinDalga(CommandContext<CommandSourceStack> k, int yukseklik) {
        int id = SuVitrin.dalga(k.getSource().getLevel(), yukseklik);
        if (id < 0) {
            k.getSource().sendFailure(Component.translatable(SuKomutlari.VITRIN_YOK));
            return 0;
        }
        SuKomutlari.yaz(k, Component.translatable(SuKomutlari.VITRIN_DALGA, id, yukseklik));
        return id;
    }

    /** Test: ayı kaldırır/geri koyar ({@code DunyaDonemi.ayVarAyarla}); ay yok olunca deniz süreci beklemeye girer. */
    private static int ay(CommandContext<CommandSourceStack> k, boolean var) {
        DunyaDonemi.al(k.getSource().getServer()).ayVarAyarla(var);
        SuKomutlari.yaz(k, Component.translatable(SuKomutlari.AY, var ? "+" : "-"));
        return 1;
    }
}
