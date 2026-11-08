package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SureliEtki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dogalafet sicaklik set <oyuncu> <°C>} (32–42 °C, {@link VucutOlcegi}), {@code /dogalafet sicaklik bilgi <oyuncu>} ve
 * {@code /dogalafet sicaklik islaklik <oyuncu> <0–100>} (KOMUTLAR; yalnız OP).
 */
public final class SicaklikKomutlari {
    private static final String K = "commands.dogalafetler.sicaklik.";

    private SicaklikKomutlari() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("sicaklik")
                .then(Commands.literal("set")
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .then(Commands.argument("deger", DoubleArgumentType.doubleArg(VucutOlcegi.EN_DUSUK_C,
                                        VucutOlcegi.EN_YUKSEK_C))
                                        .executes(SicaklikKomutlari::ayarla))))
                .then(Commands.literal("bilgi")
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .executes(SicaklikKomutlari::bilgi)))
                .then(Commands.literal("islaklik")
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .then(Commands.argument("yuzde", DoubleArgumentType.doubleArg(0, 100))
                                        .executes(SicaklikKomutlari::islaklik)))));
    }

    private static int ayarla(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer oyuncu = EntityArgument.getPlayer(c, "oyuncu");
        double santigrat = DoubleArgumentType.getDouble(c, "deger");
        VucutSicakligi.ayarla(oyuncu, VucutOlcegi.birim(santigrat));
        Esikler e = SicaklikAyarlari.al().esikler();
        double yeni = VucutSicakligi.al(oyuncu);
        c.getSource().sendSuccess(() -> Component.translatable(K + "set", oyuncu.getDisplayName(),
                VucutOlcegi.metin(VucutOlcegi.santigrat(yeni)), durum(e, yeni)), true);
        kapaliysaUyar(c.getSource());
        return (int) Math.round(VucutOlcegi.santigrat(yeni));
    }

    private static int islaklik(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer oyuncu = EntityArgument.getPlayer(c, "oyuncu");
        double yuzde = DoubleArgumentType.getDouble(c, "yuzde");
        VucutSicakligi.islaklikAyarla(oyuncu, yuzde / 100.0);
        double yeni = VucutSicakligi.islaklik(oyuncu);
        c.getSource().sendSuccess(() -> Component.translatable(K + "islaklik", oyuncu.getDisplayName(), yuzde(yeni)),
                true);
        kapaliysaUyar(c.getSource());
        return (int) Math.round(yeni * 100);
    }

    private static void kapaliysaUyar(CommandSourceStack kaynak) {
        if (!SicaklikKapisi.acikMi(kaynak.getServer())) {
            kaynak.sendSuccess(() -> Component.translatable(K + "kapali", SicaklikKapisi.ozet(kaynak.getServer())),
                    false);
        }
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
        satir(kaynak, Component.translatable(K + "bilgi.vucut", VucutOlcegi.metin(VucutOlcegi.santigrat(vucut)),
                durum(e, vucut), String.format(Locale.ROOT, "%+.3f", a.egilim() * VucutOlcegi.DERECE_PER_BIRIM),
                a.girdi().etkinlik().name(), sayi(vucut)));
        satir(kaynak, Component.translatable(K + "bilgi.ortam", sayi(a.mekan().toplam()), kalemler(a, false)));
        satir(kaynak, Component.translatable(K + "bilgi.hissedilen", sayi(a.hissedilen().toplam()),
                sayi(a.ortamBirim()), kalemler(a, true), sayi(a.hissedilen().ruzgarKmh())));
        double w = a.girdi().islaklik();
        double kalan = Islaklik.kalanSaniye(w, a.kurumaHizi());
        satir(kaynak, Component.translatable(K + "bilgi.islaklik", yuzde(w), a.girdi().suda()
                ? Component.translatable(K + "bilgi.suda")
                : kalan < 0 ? Component.translatable(K + "bilgi.islaniyor")
                        : Component.translatable(K + "bilgi.kuruma", (int) Math.ceil(kalan))));
        Yalitim.Girdi y = a.girdi().yalitim();
        satir(kaynak, Component.translatable(K + "bilgi.yalitim",
                String.format(Locale.ROOT, "%.2f", a.yalitim()), y.zirhParcasi(),
                y.yunluParca(), y.toplamTuy(), y.termostatParcasi(),
                String.format(Locale.ROOT, "%.2f", Yalitim.yunSogumaCarpani(y.yunluParca(),
                        a.girdi().ortam() <= ozet.isi().asiriSogukEsigi(), ozet.yalitim()))));
        List<SureliEtki> etkiler = VucutSicakligi.etkiler(oyuncu);
        double kalanEtki = etkiler.stream().mapToDouble(SureliEtki::kalan).sum();
        satir(kaynak, Component.translatable(K + "bilgi.etkiler", etkiler.size(),
                String.format(Locale.ROOT, "%+.2f", kalanEtki * VucutOlcegi.DERECE_PER_BIRIM)));
        return (int) Math.round(VucutOlcegi.santigrat(vucut));
    }

    /** Sıfır olmayan mekan ({@code hissedilen} false) ya da hissedilen kalemleri, döküm sırasıyla. */
    private static MutableComponent kalemler(SicaklikYoneticisi.Analiz a, boolean hissedilen) {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (Katki k : Katki.values()) {
            if (k.hissedilen() != hissedilen) {
                continue;
            }
            double d = hissedilen ? a.hissedilen().duzeltme(k) : a.mekan().katki(k);
            if (Math.abs(d) < 0.05) {
                continue;
            }
            if (!ilk) {
                sonuc.append(", ");
            }
            ilk = false;
            sonuc.append(Component.translatable("sicaklik.dogalafetler.katki." + k.anahtar()))
                    .append(String.format(Locale.ROOT, " %+.1f", d));
        }
        if (ilk) {
            sonuc.append("—");
        }
        return sonuc;
    }

    private static void satir(CommandSourceStack kaynak, Component metin) {
        kaynak.sendSuccess(() -> metin, false);
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static String yuzde(double w) {
        return String.format(Locale.ROOT, "%%%.0f", w * 100);
    }

    private static Component durum(Esikler e, double vucut) {
        return Component.translatable("sicaklik.dogalafetler.durum." + e.kademe(vucut).durum().anahtar());
    }
}
