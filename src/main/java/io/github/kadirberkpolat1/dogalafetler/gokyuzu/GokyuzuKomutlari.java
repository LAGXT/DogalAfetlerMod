package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * {@code /dogalafet kizilcuce baslat|bilgi} ve {@code /dogalafet pulsar baslat} (KOMUTLAR, N18; pulsar gökyüzü-2; pulsar
 * zincirinin kalan süresi {@code bilgi}'de ve {@code /dogalafet durum}'da, pulsar-2). Geliştirme
 * ortamında (üretim jar'ında yok) görsel kanıt için {@code /dogalafet gokyuzutest evre <evre> [ilerleme] | ay <var|yok> |
 * donma <yüzde>} de kaydedilir. Çıktılar yalnız komutu yazana gider (N6); başarılı komut 1, reddedilen 0 döner.
 */
public final class GokyuzuKomutlari {
    private static final String ON = "commands.dogalafetler.kizilcuce.";
    static final String BASLADI = ON + "basladi";
    static final String BASLADI_DURMUS = ON + "basladi_durmus";
    static final String ZATEN = ON + "zaten";
    static final String AFET_SURUYOR = ON + "afet_suruyor";
    static final String BASLIK = ON + "bilgi.baslik";
    static final String EVRE = ON + "bilgi.evre";
    static final String AY = ON + "bilgi.ay";
    static final String SUREC = ON + "bilgi.surec";
    static final String SUREC_AKTIF = ON + "bilgi.surec.aktif";
    static final String SUREC_KOMUT = ON + "bilgi.surec.komut";
    static final String SUREC_KURAL = ON + "bilgi.surec.kural";
    static final String SUREC_YOK = ON + "bilgi.surec.yok";
    static final String KALAN_SUPERNOVA = ON + "bilgi.kalan.supernova";
    static final String KALAN_DONMA = ON + "bilgi.kalan.donma";
    static final String KALAN_GECIKME = ON + "bilgi.kalan.gecikme";
    static final String KALAN_GECIKME_DONGUSUZ = ON + "bilgi.kalan.gecikme_dongusuz";
    static final String KALAN_SAFAK = ON + "bilgi.kalan.safak";
    static final String KALAN_DOGUS = ON + "bilgi.kalan.dogus";
    static final String KALAN_TAMAM = ON + "bilgi.kalan.tamam";
    static final String KALAN_BASLAMADI = ON + "bilgi.kalan.baslamadi";
    static final String DONMA = ON + "bilgi.donma";
    static final String KIS_YOK = ON + "bilgi.kis_yok";
    static final String GUN = ON + "gun";
    static final String VAR = ON + "var";
    static final String YOK = ON + "yok";
    static final String EVET = ON + "evet";
    static final String HAYIR = ON + "hayir";
    static final String ASAMA_EVRE = "commands.dogalafetler.gokyuzu.asama";
    static final String ASAMA_DOGUS_TAMAM = "commands.dogalafetler.gokyuzu.dogus_tamam";
    static final String ASAMA_PULSAR_TAMAM = "commands.dogalafetler.gokyuzu.pulsar_tamam";
    static final String PULSAR_BASLADI = "commands.dogalafetler.pulsar.basladi";
    static final String PULSAR_ZATEN = "commands.dogalafetler.pulsar.zaten";
    static final String KALAN_PULSAR = ON + "bilgi.kalan.pulsar";
    static final String KALAN_KARADELIK = ON + "bilgi.kalan.karadelik";
    static final String KALAN_BUZUL = ON + "bilgi.kalan.buzul";
    static final String KALAN_BUZUL_DONGUSUZ = ON + "bilgi.kalan.buzul_dongusuz";
    static final String KALAN_BUZUL_SAFAK = ON + "bilgi.kalan.buzul_safak";
    static final String KALAN_DONUS_DOGUS = ON + "bilgi.kalan.donus_dogus";
    static final String KALAN_GUNES_DONDU = ON + "bilgi.kalan.gunes_dondu";
    static final String ASAMA_ESKI_GUNES = "commands.dogalafetler.gokyuzu.eski_gunes";
    static final String DURUM_PULSAR = "commands.dogalafetler.pulsar.durum";

    private static final List<KizilCuceBilgiKatkisi> KATKILAR = new CopyOnWriteArrayList<>();

    private GokyuzuKomutlari() {
    }

    static void katkiEkle(KizilCuceBilgiKatkisi katki) {
        KATKILAR.add(java.util.Objects.requireNonNull(katki, "katki"));
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("kizilcuce")
                .then(Commands.literal("baslat").executes(GokyuzuKomutlari::baslat))
                .then(Commands.literal("bilgi").executes(GokyuzuKomutlari::bilgi)));
        KomutKaydi.ekle(Commands.literal("pulsar")
                .then(Commands.literal("baslat").executes(GokyuzuKomutlari::pulsarBaslat)));
        if (!FMLEnvironment.production) {
            KomutKaydi.ekle(Commands.literal("gokyuzutest")
                    .then(Commands.literal("evre").then(Commands.argument("evre", StringArgumentType.word())
                            .suggests((b, k) -> SharedSuggestionProvider.suggest(
                                    Arrays.stream(GunesEvresi.values()).map(GunesEvresi::kimlik), k))
                            .executes(b -> testEvre(b, -1f))
                            .then(Commands.argument("ilerleme", FloatArgumentType.floatArg(0f, 1f))
                                    .executes(b -> testEvre(b, FloatArgumentType.getFloat(b, "ilerleme"))))))
                    .then(Commands.literal("ay")
                            .then(Commands.literal("var").executes(b -> testAy(b, true)))
                            .then(Commands.literal("yok").executes(b -> testAy(b, false))))
                    .then(Commands.literal("donma").then(Commands.argument("yuzde", FloatArgumentType.floatArg(0f, 100f))
                            .executes(b -> {
                                DunyaDonemi.al(b.getSource().getServer())
                                        .donmaIlerlemesiAyarla(FloatArgumentType.getFloat(b, "yuzde"));
                                return yaz(b.getSource(), Component.literal("[test] donma = "
                                        + FloatArgumentType.getFloat(b, "yuzde")));
                            })))
                    .then(Commands.literal("yildiz")
                            .then(Commands.literal("kizil_cuce").executes(b -> testYildiz(b, GunesEvresi.KIZIL_CUCE)))
                            .then(Commands.literal("pulsar").executes(b -> testYildiz(b, GunesEvresi.PULSAR)))
                            .then(Commands.literal("secim").executes(b -> testYildiz(b, null))))
                    .then(Commands.literal("pulsarmerkez")
                            .then(Commands.literal("dogus").executes(b -> testMerkez(b, true)))
                            .then(Commands.literal("kaldir").executes(b -> testMerkez(b, false)))
                            .then(Commands.literal("bilgi").executes(b -> yaz(b.getSource(),
                                    Component.literal("[test] " + KavurmaKuyrugu.ozet(b.getSource().getServer().overworld())))))));
        }
    }

    /** Geliştirme: zincirin doğal doğuşunu sabitler ({@code null} = olasılıkla seçim). */
    private static int testYildiz(CommandContext<CommandSourceStack> b, GunesEvresi yildiz) {
        GunesZinciriSureci.zorlananYildiz = yildiz;
        return yaz(b.getSource(), Component.literal("[test] yeni yıldız = " + (yildiz == null ? "secim" : yildiz.kimlik())));
    }

    /** Geliştirme: oyuncusuz sunucuda (TPS senaryosu) pulsarın kavurma ve yangın merkezi dünya doğuş noktası olur. */
    private static int testMerkez(CommandContext<CommandSourceStack> b, boolean dogus) {
        if (dogus) {
            KavurmaKuyrugu.merkezKaynagiAyarla(l -> List.of(l.getSharedSpawnPos()));
        } else {
            KavurmaKuyrugu.merkezKaynagiAyarla(null);
        }
        return yaz(b.getSource(), Component.literal("[test] pulsar merkezi = " + (dogus ? "dogus" : "oyuncular")));
    }

    static int baslat(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        AfetYoneticisi yonetici = AfetYoneticisi.al(kaynak.getServer());
        DunyaDonemi donem = yonetici.donem();
        if (donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE) {
            kaynak.sendFailure(Component.translatable(ZATEN));
            return 0;
        }
        if (yonetici.aktifAfet().filter(a -> a.tur() == AfetTuru.GUNES_PATLAMASI).isPresent()) {
            kaynak.sendFailure(Component.translatable(AFET_SURUYOR));
            return 0;
        }
        GunesZinciriSureci.dogusBaslat(donem, "komut");
        if (yonetici.surecDurdu(GunesZinciriSureci.al())) {
            donem.evreIlerlemesiAyarla(1f);
            return yaz(kaynak, Component.translatable(BASLADI_DURMUS));
        }
        return yaz(kaynak, Component.translatable(BASLADI));
    }

    /** Pulsarı şimdi doğurur (olasılık, donma ve şafak beklenmez); zaten pulsarsa ya da güneş patlaması sürüyorsa 0. */
    static int pulsarBaslat(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        AfetYoneticisi yonetici = AfetYoneticisi.al(kaynak.getServer());
        DunyaDonemi donem = yonetici.donem();
        if (donem.gunesEvresi() == GunesEvresi.PULSAR) {
            kaynak.sendFailure(Component.translatable(PULSAR_ZATEN));
            return 0;
        }
        if (yonetici.aktifAfet().filter(a -> a.tur() == AfetTuru.GUNES_PATLAMASI).isPresent()) {
            kaynak.sendFailure(Component.translatable(AFET_SURUYOR));
            return 0;
        }
        GunesZinciriSureci.pulsarBaslat(donem, "komut");
        return yaz(kaynak, Component.translatable(PULSAR_BASLADI));
    }

    static int bilgi(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        MinecraftServer server = kaynak.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = server.overworld();
        GunesEvresi evre = donem.gunesEvresi();
        List<Component> satirlar = new ArrayList<>();
        satirlar.add(Component.translatable(BASLIK));
        satirlar.add(Component.translatable(EVRE, Component.translatable(evre.ceviriAnahtari()),
                yuzde(donem.evreIlerlemesi() * 100f)));
        satirlar.add(Component.translatable(AY, Component.translatable(donem.ayVar() ? VAR : YOK)));
        satirlar.add(Component.translatable(SUREC, surecDurumu(yonetici, ust)));
        satirlar.add(kalan(donem, ust));
        satirlar.add(Component.translatable(DONMA, yuzde(donem.donmaIlerlemesi()),
                Component.translatable(donem.cozulmeDonemi() ? EVET : HAYIR)));
        if (KATKILAR.isEmpty()) {
            satirlar.add(Component.translatable(KIS_YOK));
        }
        for (KizilCuceBilgiKatkisi katki : KATKILAR) {
            satirlar.addAll(katki.satirlar(kaynak));
        }
        for (Component satir : satirlar) {
            kaynak.sendSuccess(() -> satir, false);
        }
        return 1;
    }

    private static Component surecDurumu(AfetYoneticisi yonetici, ServerLevel ust) {
        GunesZinciriSureci surec = GunesZinciriSureci.al();
        if (!surec.aktifMi(ust)) {
            return Component.translatable(SUREC_YOK);
        }
        Optional<DonemSureci.DurdurmaNedeni> neden = yonetici.durdurmaNedeni(surec);
        return Component.translatable(neden.map(n -> n == DonemSureci.DurdurmaNedeni.KOMUT ? SUREC_KOMUT : SUREC_KURAL)
                .orElse(SUREC_AKTIF));
    }

    private static Component kalan(DunyaDonemi donem, ServerLevel ust) {
        Optional<Component> pulsar = PulsarSureci.kalanMetni(ust, donem);
        if (pulsar.isPresent()) {
            return pulsar.get();
        }
        GunesZinciri.Ayarlar ayar = GokyuzuAyarlari.zincir();
        float p = donem.evreIlerlemesi();
        return switch (donem.gunesEvresi()) {
            case SUPERNOVA -> Component.translatable(KALAN_SUPERNOVA, gun((1f - p) * ayar.supernovaTick()));
            case GUNESSIZ -> {
                long kapi = GunesZinciriSureci.kapiSayaci(donem);
                if (donem.donmaIlerlemesi() < GunesZinciri.DONMA_TAM - 1e-3f) {
                    yield Component.translatable(KALAN_DONMA, yuzde(donem.donmaIlerlemesi()));
                }
                boolean dongu = ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
                if (!GunesZinciri.kapiAcik(kapi, ayar)) {
                    yield Component.translatable(dongu ? KALAN_GECIKME : KALAN_GECIKME_DONGUSUZ,
                            gun(ayar.gecikmeTick() - kapi));
                }
                yield Component.translatable(KALAN_SAFAK, GunesZinciri.safagaKalan(ust.getDayTime()));
            }
            case KIZIL_CUCE -> p < 1f ? Component.translatable(KALAN_DOGUS, yuzde(p * 100f))
                    : Component.translatable(KALAN_TAMAM);
            default -> Component.translatable(KALAN_BASLAMADI);
        };
    }

    private static int testEvre(CommandContext<CommandSourceStack> b, float ilerleme) {
        GunesEvresi evre = GunesEvresi.kimlikIle(StringArgumentType.getString(b, "evre"));
        DunyaDonemi donem = DunyaDonemi.al(b.getSource().getServer());
        donem.gunesEvresiAyarla(evre);
        if (ilerleme >= 0f) {
            donem.evreIlerlemesiAyarla(ilerleme);
        }
        return yaz(b.getSource(), Component.literal("[test] güneş = " + evre.kimlik() + " ilerleme = "
                + donem.evreIlerlemesi()));
    }

    private static int testAy(CommandContext<CommandSourceStack> b, boolean var) {
        DunyaDonemi.al(b.getSource().getServer()).ayVarAyarla(var);
        return yaz(b.getSource(), Component.literal("[test] ay = " + var));
    }

    private static int yaz(CommandSourceStack kaynak, Component metin) {
        kaynak.sendSuccess(() -> metin, false);
        return 1;
    }

    static String yuzde(float deger) {
        return String.format(Locale.ROOT, "%.1f", deger);
    }

    static Component gun(double tick) {
        return Component.translatable(GUN, String.format(Locale.ROOT, "%.2f", Math.max(0.0, tick) / GunesZinciri.GUN_TICK));
    }

    public static void cevirileriYaz(CeviriYazici y) {
        y.ekle(GunesZinciriSureci.AD_ANAHTARI, "Sun chain (supernova, sunless era, red dwarf)",
                "Güneş zinciri (süpernova, güneşsiz dönem, kızıl cüce)");
        y.ekle(BASLADI, "The red dwarf is rising.", "Kızıl cüce doğuyor.");
        y.ekle(BASLADI_DURMUS, "The red dwarf has risen (sun chain is stopped, dawn skipped).",
                "Kızıl cüce doğdu (güneş zinciri durdurulmuş, doğuş atlandı).");
        y.ekle(ZATEN, "The red dwarf era has already begun.", "Kızıl cüce dönemi zaten başladı.");
        y.ekle(AFET_SURUYOR, "A solar flare is active; use /dogalafet stop first.",
                "Güneş patlaması sürüyor; önce /dogalafet stop.");
        y.ekle(BASLIK, "Sky era", "Gökyüzü dönemi");
        y.ekle(EVRE, "Sun: %s (progress %s%%)", "Güneş: %s (ilerleme %%%s)");
        y.ekle(AY, "Moon: %s", "Ay: %s");
        y.ekle(SUREC, "Sun chain process: %s", "Güneş zinciri süreci: %s");
        y.ekle(SUREC_AKTIF, "running", "sürüyor");
        y.ekle(SUREC_KOMUT, "stopped (stop)", "durduruldu (stop)");
        y.ekle(SUREC_KURAL, "stopped (rule)", "durduruldu (kural)");
        y.ekle(SUREC_YOK, "not running", "sürmüyor");
        y.ekle(KALAN_SUPERNOVA, "Supernova fades into the sunless era in %s.", "Süpernova %s sonra sönüp güneşsiz dönem başlar.");
        y.ekle(KALAN_DONMA, "Red dwarf: waiting for freeze to reach 100%% (now %s%%).",
                "Kızıl cüce: donmanın %%100 olması bekleniyor (şu an %%%s).");
        y.ekle(KALAN_GECIKME, "Red dwarf rises %s from now, at the first dawn after that.",
                "Kızıl cüce %s sonra, ardından gelen ilk şafakta doğar.");
        y.ekle(KALAN_GECIKME_DONGUSUZ, "Red dwarf rises %s from now (daylight cycle is off: dawn is not awaited).",
                "Kızıl cüce %s sonra doğar (gün döngüsü kapalı: şafak beklenmez).");
        y.ekle(KALAN_SAFAK, "Red dwarf rises at the next dawn (%s ticks of daytime).",
                "Kızıl cüce ilk şafakta doğar (gün saatinde %s tick).");
        y.ekle(KALAN_DOGUS, "Red dwarf dawn in progress: %s%%", "Kızıl cüce doğuşu sürüyor: %%%s");
        y.ekle(KALAN_TAMAM, "Red dwarf era established.", "Kızıl cüce dönemi kuruldu.");
        y.ekle(KALAN_BASLAMADI, "The sun chain has not started (it begins with the supernova).",
                "Güneş zinciri başlamadı (süpernovayla başlar).");
        y.ekle(DONMA, "Freeze progress: %s%%, thaw era: %s", "Donma ilerlemesi: %%%s, çözülme dönemi: %s");
        y.ekle(KIS_YOK, "Permanent winter zone: freeze system not loaded.", "Kalıcı kış bölgesi: donma sistemi yüklü değil.");
        y.ekle(GUN, "%s days", "%s gün");
        y.ekle(VAR, "present", "var");
        y.ekle(YOK, "gone", "yok");
        y.ekle(EVET, "yes", "evet");
        y.ekle(HAYIR, "no", "hayır");
        y.ekle(ASAMA_EVRE, "%s: %s", "%s: %s");
        y.ekle(ASAMA_DOGUS_TAMAM, "%s: red dwarf dawn completed", "%s: kızıl cüce doğuşu tamamlandı");
        y.ekle(ASAMA_PULSAR_TAMAM, "%s: pulsar at full strength", "%s: pulsar tam güçte");
        y.ekle(PULSAR_BASLADI, "A pulsar is born: the world begins to burn.", "Pulsar doğdu: dünya kavrulmaya başlıyor.");
        y.ekle(PULSAR_ZATEN, "The pulsar era has already begun.", "Pulsar dönemi zaten başladı.");
        y.ekle(KALAN_PULSAR, "Pulsar era: heating %s%%; a black hole swallows the pulsar in %s.",
                "Pulsar dönemi: ısınma %%%s; kara delik pulsarı %s sonra yutar.");
        y.ekle(KALAN_KARADELIK, "The black hole is swallowing the pulsar: %s s left.",
                "Kara delik pulsarı yutuyor: %s sn kaldı.");
        y.ekle(KALAN_BUZUL, "Ice age: a red dwarf rises %s from now, at the first dawn after that.",
                "Buzul kıyamet: kızıl cüce %s sonra, ardından gelen ilk şafakta doğar.");
        y.ekle(KALAN_BUZUL_DONGUSUZ, "Ice age: a red dwarf rises %s from now (daylight cycle is off: dawn is not awaited).",
                "Buzul kıyamet: kızıl cüce %s sonra doğar (gün döngüsü kapalı: şafak beklenmez).");
        y.ekle(KALAN_BUZUL_SAFAK, "Ice age ends: the red dwarf rises at the next dawn (%s ticks of daytime).",
                "Buzul kıyamet bitiyor: kızıl cüce ilk şafakta doğar (gün saatinde %s tick).");
        y.ekle(KALAN_DONUS_DOGUS, "Red dwarf dawn %s%% (%s left); then the old sun returns.",
                "Kızıl cüce doğuşu %%%s (%s kaldı); ardından eski güneş geri gelir.");
        y.ekle(KALAN_GUNES_DONDU, "The old sun has returned; the ice is thawing (freeze %s%%).",
                "Eski güneş geri döndü; buzlar çözülüyor (donma %%%s).");
        y.ekle(ASAMA_ESKI_GUNES, "%s: the old sun has returned", "%s: eski güneş geri döndü");
        y.ekle(DURUM_PULSAR, "Pulsar chain: %s; %s", "Pulsar zinciri: %s; %s");
        y.ekle(PulsarSureci.AD_ANAHTARI, "Pulsar chain (scorching world, black hole, ice age, old sun)",
                "Pulsar zinciri (kavrulan dünya, kara delik, buzul kıyamet, eski güneş)");
    }
}
