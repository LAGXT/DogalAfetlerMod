package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Etki;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.EtkiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dogalafet yiyecek bilgi [oyuncu]} (yalnız OP): madde 14 tablosunun config'le kurulmuş etkileri ve vanilla listesi;
 * oyuncu verilirse azalan verim penceresi süren türler.
 */
public final class YiyecekKomutlari {
    static final String K = "commands.dogalafetler.yiyecek.";

    private YiyecekKomutlari() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("yiyecek")
                .then(Commands.literal("bilgi")
                        .executes(c -> tablo(c.getSource()))
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .executes(YiyecekKomutlari::oyuncu))));
    }

    private static int tablo(CommandSourceStack kaynak) {
        EtkiAyarlari a = YiyecekAyarlari.etki();
        satir(kaynak, Component.translatable(K + "bilgi.baslik", sayi(a.anlikOran() * 100),
                a.azalanVerimPenceresiSaniye(), sayi(a.azalanVerimOrani())));
        for (YiyecekTablosu.Kayit kayit : YiyecekTablosu.KAYITLAR) {
            YiyecekTanimi t = kayit.tanim();
            Etki e = Etki.hesapla(t, a, 1.0, false);
            satir(kaynak, Component.translatable(K + "bilgi.satir",
                    Component.translatable(YiyecekKayitlari.esya(kayit.ad()).getDescriptionId()),
                    Component.translatable(YiyecekKayitlari.efekt(t.yon()).getDescriptionId()),
                    Component.translatable(buyuklukAnahtari(t)), sayi(e.toplam()), t.sureSaniye()));
        }
        satir(kaynak, Component.translatable(K + "bilgi.vanilla",
                Component.translatable(K + (YiyecekAyarlari.vanillaIsinma() ? "acik" : "kapali")),
                YiyecekAyarlari.vanillaTanimlari().size(), sayi(a.cokKucuk())));
        return YiyecekTablosu.KAYITLAR.size();
    }

    private static int oyuncu(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer o = EntityArgument.getPlayer(c, "oyuncu");
        Map<String, Long> pencereler = new TreeMap<>(YiyecekAPI.surenPencereler(o));
        String liste = pencereler.entrySet().stream()
                .map(g -> g.getKey() + " " + (g.getValue() + 19) / 20 + "s")
                .collect(Collectors.joining(", "));
        Component deger = pencereler.isEmpty() ? Component.translatable(K + "yok") : Component.literal(liste);
        satir(c.getSource(), Component.translatable(K + "bilgi.oyuncu", o.getDisplayName(), deger));
        return pencereler.size();
    }

    static String buyuklukAnahtari(YiyecekTanimi t) {
        return "yiyecek.dogalafetler.buyukluk." + t.buyukluk().anahtar();
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.2f", d).replaceAll("\\.?0+$", "");
    }

    private static void satir(CommandSourceStack kaynak, Component metin) {
        kaynak.sendSuccess(() -> metin, false);
    }
}
