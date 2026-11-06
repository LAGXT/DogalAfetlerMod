package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=asit_orman} ile tek oyunculu dünyada
 * çalışır; üretimde etkisizdir. Yaratıcı kipte uçan kamera en yakın ormanın tepesinde: asit yağmuru S1 aktif karesi, sonra S5
 * ile ön-uyarı (elde Hava Radarı, arayüz açık), siren, aktif, vitrin (yoğun yağış, aşınan yapraklar) ve bitiş kareleri; asit
 * kamerası iki seviyede aynı yerde durur. Ardından alacakaranlıkta orman yangını S1 ve S5 aynı dizide, kamera yangın merkezinin
 * 34 blok rüzgâr üstünde, 10 blok yukarıda; orman S5 vitrininden sonra efekt kalitesi Düşük, Yüksek ve Ultra'ya zorlanır
 * (istemci config değeri, koşu sonunda geri konur), afet durunca aynı kamerada afetsiz taban ölçülür.
 * Ön-uyarı penceresi {@code hiz 20} ile kısaltılır, radar karesinden önce hız 1'e döner. Her ölçüm satırı TPS'i, her kare
 * FPS'i, parçacık sayısını ve kamerayı yazar. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır; kare istenen tick'te
 * değil sonraki çizilmiş karenin sonunda alınır (gizli workspace ~1 FPS).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class AsitOrmanOtomasyonu {
    private static final boolean AKTIF = "asit_orman".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "ASIT_ORMAN_OTOMASYON ";
    private static final double KAMERA_UZAKLIGI = 34;

    /** Bir adım her tick çağrılır ({@code t} adımın kaçıncı tick'i); {@code true} dönünce sıradakine geçilir. */
    private interface Adim {
        boolean calis(Minecraft mc, IntegratedServer s, int t);
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim;
    private static int sayac;
    private static String istenenKare;
    private static volatile boolean hazir;
    private static volatile boolean onUyari;
    private static EfektKalitesi ilkKalite;

    static {
        ADIMLAR.add(bekle(100));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::hazirla));
        ADIMLAR.add(bekle(240));
        ADIMLAR.add(arayuz(false));
        dizi(AfetTuru.ASIT_YAGMURU, "asit", 300, 300, 400, 60, false, s -> {
        });
        ADIMLAR.add(sunucu(s -> komut(s, "time set 13000")));
        dizi(AfetTuru.ORMAN_YANGINI, "orman", 400, 120, 400, 500, true, AsitOrmanOtomasyonu::ormanKamera);
        ADIMLAR.add(kalite(null));
    }

    private AsitOrmanOtomasyonu() {
    }

    /**
     * S1 aktif karesi, sonra S5'in ön-uyarı, siren, aktif, vitrin ve bitiş kareleri; {@code kademeler} ise vitrinden sonra
     * Düşük/Yüksek/Ultra kareleri ve durduktan sonra afetsiz taban.
     */
    private static void dizi(AfetTuru tur, String ad, int s1, int aktif, int vitrin, int bitis, boolean kademeler,
            Consumer<IntegratedServer> kamera) {
        ADIMLAR.add(sunucu(s -> baslat(s, tur, 1)));
        ADIMLAR.add(sunucu(kamera));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::ileri));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::ileri));
        ADIMLAR.add(bekle(s1));
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_s1")));
        ADIMLAR.add(kare(ad + "_s1"));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::dur));
        ADIMLAR.add(sunucu(s -> baslat(s, tur, 5)));
        ADIMLAR.add(sunucu(kamera));
        ADIMLAR.add(sunucu(s -> AfetYoneticisi.al(s).hizAyarla(20)));
        ADIMLAR.add(onUyariBekle());
        ADIMLAR.add(sunucu(s -> AfetYoneticisi.al(s).hizAyarla(1)));
        ADIMLAR.add(arayuz(true));
        ADIMLAR.add(bekle(60));
        ADIMLAR.add(kare(ad + "_on_uyari_radar"));
        ADIMLAR.add(arayuz(false));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::ileri));
        ADIMLAR.add(bekle(120));
        ADIMLAR.add(kare(ad + "_siren"));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::ileri));
        ADIMLAR.add(bekle(aktif));
        ADIMLAR.add(kare(ad + "_aktif"));
        ADIMLAR.add(bekle(vitrin));
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_vitrin")));
        ADIMLAR.add(kare(ad + "_vitrin"));
        if (kademeler) {
            for (EfektKalitesi k : new EfektKalitesi[] {EfektKalitesi.DUSUK, EfektKalitesi.YUKSEK, EfektKalitesi.ULTRA}) {
                String e = ad + "_s5_" + k.name().toLowerCase(Locale.ROOT);
                ADIMLAR.add(kalite(k));
                ADIMLAR.add(bekle(200));
                ADIMLAR.add(sunucu(s -> olcum(s, e)));
                ADIMLAR.add(kare(e));
            }
            ADIMLAR.add(kalite(null));
        }
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::ileri));
        ADIMLAR.add(bekle(bitis));
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_bitis")));
        ADIMLAR.add(kare(ad + "_bitis"));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::dur));
        if (kademeler) {
            ADIMLAR.add(bekle(200));
            ADIMLAR.add(sunucu(s -> olcum(s, ad + "_taban")));
            ADIMLAR.add(kare(ad + "_taban"));
        }
    }

    private static Adim bekle(int tick) {
        return (mc, s, t) -> t >= tick;
    }

    private static Adim arayuz(boolean acik) {
        return (mc, s, t) -> {
            mc.options.hideGui = !acik;
            return true;
        };
    }

    /** Efekt kalitesini istemci config değeriyle zorlar ({@code null}: koşu başındaki değer); eski parçacıklar silinir. */
    private static Adim kalite(EfektKalitesi kalite) {
        return (mc, s, t) -> {
            ForgeConfigSpec.EnumValue<EfektKalitesi> deger = kaliteDegeri();
            if (ilkKalite == null) {
                ilkKalite = deger.get();
            }
            deger.set(kalite == null ? ilkKalite : kalite);
            mc.particleEngine.setLevel(mc.level);
            DogalAfetler.LOGGER.info(ON + "kalite {} (parçacıklar silindi)", deger.get());
            return true;
        };
    }

    @SuppressWarnings("unchecked")
    private static ForgeConfigSpec.EnumValue<EfektKalitesi> kaliteDegeri() {
        ModConfig config = ConfigTracker.INSTANCE.fileMap().get(AyarKaydi.ISTEMCI_DOSYASI);
        return (ForgeConfigSpec.EnumValue<EfektKalitesi>) ((ForgeConfigSpec) config.getSpec()).getValues()
                .get("gorsel.efektKalitesi");
    }

    /** Sunucu thread'inde bir iş; bitince sıradaki adım. */
    private static Adim sunucu(Consumer<IntegratedServer> is) {
        return (mc, s, t) -> {
            if (t == 1) {
                hazir = false;
                s.execute(() -> {
                    is.accept(s);
                    hazir = true;
                });
                return false;
            }
            return hazir;
        };
    }

    /** Hızlandırılmış ön-uyarıda radarın göreceği normal pencere başlayana kadar (en çok 1200 tick). */
    private static Adim onUyariBekle() {
        return (mc, s, t) -> {
            if (t == 1) {
                onUyari = false;
            }
            if (t % 5 == 0) {
                s.execute(() -> onUyari = AfetYoneticisi.al(s).aktifAfet().map(Afet::normalOnUyariBasladi).orElse(true));
            }
            if (t > 1200) {
                DogalAfetler.LOGGER.warn(ON + "normal ön-uyarı 1200 tick'te başlamadı");
                return true;
            }
            return onUyari;
        };
    }

    /** Kareyi ister; sonraki çizilmiş karenin sonunda alınır, adımlar o zamana dek bekler. */
    private static Adim kare(String ad) {
        return (mc, s, t) -> {
            istenenKare = ad;
            return true;
        };
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (AKTIF) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (AKTIF) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "girdi: fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (AKTIF) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tekerlek {}", olay.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!AKTIF || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        IntegratedServer s = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || s == null || istenenKare != null) {
            return;
        }
        if (adim < ADIMLAR.size()) {
            if (ADIMLAR.get(adim).calis(mc, s, ++sayac)) {
                adim++;
                sayac = 0;
            }
        } else if (adim == ADIMLAR.size()) {
            adim++;
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void cizimSonu(TickEvent.RenderTickEvent olay) {
        if (!AKTIF || olay.phase != TickEvent.Phase.END || istenenKare == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String dosya = "ao_" + istenenKare + ".png";
        istenenKare = null;
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        DogalAfetler.LOGGER.info(ON + "kare {} (adım {}, FPS {}, parçacık {}, asit yoğunluğu {}, kalite {}, kamera {})", dosya,
                adim, mc.getFps(), mc.particleEngine.countParticles(),
                String.format(Locale.ROOT, "%.2f", IstemciAsitYagmuru.yogunluk()), IstemciAyarlari.efektKalitesi(),
                String.format(Locale.ROOT, "%.1f %.1f %.1f yaw %.0f pitch %.0f", mc.player.getX(), mc.player.getY(),
                        mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot()));
    }

    // ---------------------------------------------------------------- sunucu thread'i

    private static ServerPlayer oyuncu(IntegratedServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static void komut(IntegratedServer s, String komut) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), komut);
    }

    /** Kurallar, öğle, en yakın orman; yaratıcı kipte uçan kamera ormanın tepesinin 4 blok üstünde, elde Hava Radarı. */
    private static void hazirla(IntegratedServer s) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 5000", "weather clear"}) {
            komut(s, k);
        }
        ServerLevel level = s.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(BiomeTags.IS_FOREST), dogus, 6400, 32, 64);
        BlockPos m = bulunan == null ? dogus : bulunan.getFirst();
        ServerPlayer o = oyuncu(s);
        o.setGameMode(GameType.CREATIVE);
        o.getAbilities().flying = true;
        o.onUpdateAbilities();
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EsyaKayitlari.HAVA_RADARI.get()));
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, m.getX(), m.getZ());
        o.teleportTo(level, m.getX() + 0.5, ust + 4, m.getZ() + 0.5, -90F, 28F);
        DogalAfetler.LOGGER.info(ON + "orman {} ({}), kamera y={}", m.toShortString(), bulunan == null ? "bulunamadı"
                : bulunan.getSecond().unwrapKey().map(k -> k.location().toString()).orElse("?"), ust + 4);
    }

    private static void baslat(IntegratedServer s, AfetTuru tur, int seviye) {
        AfetYoneticisi y = AfetYoneticisi.al(s);
        y.durdur();
        ServerPlayer o = oyuncu(s);
        AfetYoneticisi.BaslatmaSonucu sonuc = y.elleBaslat(tur, seviye, o, o.serverLevel(), o.blockPosition());
        DogalAfetler.LOGGER.info(ON + "{} S{} başlatma: {} merkez {}", tur.kimlik(), seviye, sonuc,
                y.aktifAfet().map(a -> a.baglam().merkez().toShortString()).orElse("-"));
    }

    private static void ileri(IntegratedServer s) {
        AfetYoneticisi y = AfetYoneticisi.al(s);
        y.asamaIleri();
        DogalAfetler.LOGGER.info(ON + "aşama: {}", y.aktifAfet().map(a -> a.asama().kimlik()).orElse("bitti"));
    }

    private static void dur(IntegratedServer s) {
        AfetYoneticisi.al(s).durdur();
        DogalAfetler.LOGGER.info(ON + "durduruldu; kalan yanan hücre {}", YanginYoneticisi.aktifSayi(s.overworld()));
    }

    private static void olcum(IntegratedServer s, String ad) {
        DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "ölçüm %s: TPS %.1f (%.2f ms/tick), yanan hücre %d", ad,
                TpsOlcer.tps(), TpsOlcer.ortalamaMs(), YanginYoneticisi.aktifSayi(s.overworld())));
    }

    /** Orman yangınının merkezinin 34 blok rüzgâr üstünde, zeminin 10 blok üstünde, merkeze bakan kamera. */
    private static void ormanKamera(IntegratedServer s) {
        ServerLevel level = s.overworld();
        AfetYoneticisi.al(s).aktifAfet().ifPresent(afet -> {
            BlockPos m = afet.baglam().merkez();
            double[] r = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
            double g = Math.max(1e-6, Math.hypot(r[0], r[1]));
            double x = m.getX() + 0.5 - r[0] / g * KAMERA_UZAKLIGI;
            double z = m.getZ() + 0.5 - r[1] / g * KAMERA_UZAKLIGI;
            int zemin = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)), m.getY());
            double y = zemin + 10;
            double dx = m.getX() + 0.5 - x;
            double dz = m.getZ() + 0.5 - z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(y - m.getY() - 1, Math.hypot(dx, dz)));
            oyuncu(s).teleportTo(level, x, y, z, yaw, pitch);
            DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "orman kamerası %.0f %.0f %.0f yaw %.0f pitch %.0f", x, y,
                    z, yaw, pitch));
        });
    }
}
