package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.IsiAlisverisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SureliEtki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** {@code /dogalafet sicaklik set <oyuncu> <değer>} ve {@code /dogalafet sicaklik bilgi <oyuncu>} (KOMUTLAR; yalnız OP). */
public final class SicaklikKomutlari {
    private static final String K = "commands.dogalafetler.sicaklik.";

    private SicaklikKomutlari() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("sicaklik")
                .then(Commands.literal("set")
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .then(Commands.argument("deger", DoubleArgumentType.doubleArg(-IsiAlisverisi.OLCEK,
                                        IsiAlisverisi.OLCEK))
                                        .executes(SicaklikKomutlari::ayarla))))
                .then(Commands.literal("bilgi")
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .executes(SicaklikKomutlari::bilgi))));
    }

    private static int ayarla(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer oyuncu = EntityArgument.getPlayer(c, "oyuncu");
        double deger = DoubleArgumentType.getDouble(c, "deger");
        VucutSicakligi.ayarla(oyuncu, deger);
        Esikler e = SicaklikAyarlari.al().esikler();
        double yeni = VucutSicakligi.al(oyuncu);
        c.getSource().sendSuccess(() -> Component.translatable(K + "set", oyuncu.getDisplayName(), sayi(yeni),
                kademe(e, yeni)), true);
        if (!SicaklikKapisi.acikMi(c.getSource().getServer())) {
            c.getSource().sendSuccess(() -> Component.translatable(K + "kapali",
                    SicaklikKapisi.ozet(c.getSource().getServer())), false);
        }
        return (int) Math.round(yeni);
    }

    private static int bilgi(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer oyuncu = EntityArgument.getPlayer(c, "oyuncu");
        CommandSourceStack kaynak = c.getSource();
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        SicaklikYoneticisi.Analiz a = SicaklikYoneticisi.analiz(oyuncu);
        double vucut = VucutSicakligi.al(oyuncu);
        Esikler e = ozet.esikler();
        boolean acik = SicaklikKapisi.acikMi(kaynak.getServer());
        satir(kaynak, Component.translatable(K + "bilgi.baslik", oyuncu.getDisplayName()));
        satir(kaynak, Component.translatable(K + "bilgi.kapi",
                Component.translatable(K + (acik ? "acik" : "kapali_kisa")), SicaklikKapisi.ozet(kaynak.getServer())));
        satir(kaynak, Component.translatable(K + "bilgi.vucut", sayi(vucut), kademe(e, vucut),
                String.format(Locale.ROOT, "%+.3f", a.egilim()), a.girdi().etkinlik().name(),
                a.girdi().islaklik().name()));
        MutableComponent kalemler = Component.empty();
        boolean ilk = true;
        for (Katki k : Katki.values()) {
            double d = a.ortam().katki(k);
            if (Math.abs(d) < 0.05) {
                continue;
            }
            if (!ilk) {
                kalemler.append(", ");
            }
            ilk = false;
            kalemler.append(Component.translatable("sicaklik.dogalafetler.katki." + k.anahtar()))
                    .append(String.format(Locale.ROOT, " %+.1f", d));
        }
        satir(kaynak, Component.translatable(K + "bilgi.ortam", sayi(a.ortam().toplam()), sayi(a.ortamBirim()),
                kalemler));
        Yalitim.Girdi y = a.girdi().yalitim();
        satir(kaynak, Component.translatable(K + "bilgi.yalitim",
                String.format(Locale.ROOT, "%.2f", Yalitim.toplam(y, ozet.yalitim())), y.zirhParcasi(),
                y.yunluParca(), y.toplamTuy(), y.termostatParcasi(),
                String.format(Locale.ROOT, "%.2f", Yalitim.yunSogumaCarpani(y.yunluParca(),
                        a.girdi().ortam() <= ozet.isi().asiriSogukEsigi(), ozet.yalitim()))));
        List<SureliEtki> etkiler = VucutSicakligi.etkiler(oyuncu);
        double kalan = etkiler.stream().mapToDouble(SureliEtki::kalan).sum();
        satir(kaynak, Component.translatable(K + "bilgi.etkiler", etkiler.size(), sayi(kalan)));
        return (int) Math.round(vucut);
    }

    private static void satir(CommandSourceStack kaynak, Component metin) {
        kaynak.sendSuccess(() -> metin, false);
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static Component kademe(Esikler e, double vucut) {
        return Component.translatable("sicaklik.dogalafetler.kademe." + e.kademe(vucut).name().toLowerCase(Locale.ROOT));
    }
}
