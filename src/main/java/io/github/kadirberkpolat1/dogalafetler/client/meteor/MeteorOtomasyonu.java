package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.Termostat;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorBoyutu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorOdulleri;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=meteor} ile tek oyunculu dünyada çalışır;
 * üretimde etkisizdir. Gece, en yakın ormanın 48 blok batısında ağaçların üstünde seyirci kamera doğuya bakar; sağdan
 * gelen kademeli ay (mavi-beyaz) ve güneş meteorlarının izleri, çarpma anı, krater ve ateş karelenir; sonra yakın kamerayla
 * parıldayan Meteorit Parçası ve Kozmik Çekirdek, en sonda Kozmik Çekirdek ve Termostatlı zırh ipuçları. Klavye, fare ve
 * tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class MeteorOtomasyonu {
    private static final boolean AKTIF = "meteor".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "METEOR_OTOMASYON ";
    private static final int UZAKLIK = 48;
    private static final int KAMERA_YUKSEKLIGI = 18;

    /** Hedef ofseti (kameradan bakış yönünde x, sağa z), boyut, görünüm, açı, gelış yönü, başlangıç tick'i. */
    private record Atis(int dx, int dz, MeteorBoyutu boyut, MeteorGorunumu gorunum, double aci, double azimut, int tick) {
    }

    private static final List<Atis> ATISLAR = List.of(
            new Atis(0, 0, MeteorBoyutu.BUYUK, MeteorGorunumu.AY, 55, Math.PI / 2 - 0.35, 0),
            new Atis(10, -14, MeteorBoyutu.KUCUK, MeteorGorunumu.AY, 50, Math.PI / 2 - 0.15, 5),
            new Atis(-6, 12, MeteorBoyutu.KUCUK, MeteorGorunumu.GUNES, 60, Math.PI / 2 - 0.5, 9),
            new Atis(16, 8, MeteorBoyutu.BUYUK, MeteorGorunumu.AY, 48, Math.PI / 2 - 0.25, 14));

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static int ilkCarpma = -1;
    private static int carpmaBaslangic;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos odak;

    private MeteorOtomasyonu() {
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
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        sayac++;
        switch (adim) {
            case -1 -> {
                if (sayac >= 100) {
                    DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}", mc.player.getGameProfile().getName());
                    server.execute(() -> hazirla(server));
                    sonraki();
                }
            }
            case 0 -> {
                if (sunucuHazir && sayac >= 240) {
                    mc.options.hideGui = true;
                    carpmaBaslangic = IstemciMeteor.carpmaSayisi();
                    sonraki();
                }
            }
            case 1 -> vitrin(mc, server);
            case 2 -> {
                if (sayac == 1) {
                    server.execute(() -> oduller(server));
                } else if (sayac == 80) {
                    kare(mc, "odul_yakin");
                } else if (sayac == 96) {
                    kare(mc, "odul_yakin_b");
                    sonraki();
                }
            }
            case 3 -> {
                if (sayac == 1) {
                    mc.setScreen(new IpucuEkrani());
                } else if (sayac == 30) {
                    kare(mc, "ipuclari");
                    mc.setScreen(null);
                    server.execute(() -> {
                        int sonen = YanginYoneticisi.sondur(server.overworld(), null);
                        DogalAfetler.LOGGER.info(ON + "bitti; {} yangın hücresi söndürüldü", sonen);
                    });
                    sonraki();
                }
            }
            case 4 -> {
                if (sayac >= 60) {
                    DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
                    adim = 5;
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }

    /** Meteorlar: atar, uçuşta iki kare, ilk çarpmada parlama karesi, sonra krater ve ateş kareleri. */
    private static void vitrin(Minecraft mc, IntegratedServer server) {
        for (Atis a : ATISLAR) {
            if (sayac == 1 + a.tick()) {
                server.execute(() -> at(server, a));
            }
        }
        if (sayac == 30) {
            kare(mc, "iz_1");
        } else if (sayac == 40) {
            kare(mc, "iz_2");
        }
        if (ilkCarpma < 0 && IstemciMeteor.carpmaSayisi() > carpmaBaslangic) {
            ilkCarpma = sayac;
            DogalAfetler.LOGGER.info(ON + "ilk çarpma paketi tick {}", sayac);
        }
        if (ilkCarpma > 0 && sayac == ilkCarpma + 1) {
            kare(mc, "carpma");
        } else if (ilkCarpma > 0 && sayac == ilkCarpma + 6) {
            kare(mc, "carpma_b");
        } else if (ilkCarpma > 0 && sayac == ilkCarpma + 80) {
            kare(mc, "krater_ates");
            server.execute(() -> DogalAfetler.LOGGER.info(ON + "krater karesi: yanan {} hücre, bekleyen krater {}",
                    YanginYoneticisi.aktifSayi(server.overworld()), MeteorAPI.bekleyenKraterSayisi(server.overworld())));
        } else if (ilkCarpma > 0 && sayac == ilkCarpma + 100) {
            server.execute(() -> yakinKamera(server));
        } else if (ilkCarpma > 0 && sayac == ilkCarpma + 180) {
            kare(mc, "krater_yakin");
            sonraki();
        } else if (ilkCarpma < 0 && sayac > 400) {
            DogalAfetler.LOGGER.warn(ON + "400 tick'te çarpma paketi gelmedi");
            sonraki();
        }
    }

    private static void sonraki() {
        adim++;
        sayac = 0;
    }

    /** Sunucu thread'inde: kurallar, gece, en yakın orman, seyirci kamera. */
    private static void hazirla(IntegratedServer server) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 14500", "weather clear"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), k);
        }
        ServerLevel level = server.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.FOREST)
                || h.is(Biomes.BIRCH_FOREST) || h.is(Biomes.FLOWER_FOREST) || h.is(Biomes.DARK_FOREST), dogus, 6400, 32, 64);
        BlockPos merkez = bulunan == null ? dogus : bulunan.getFirst();
        odak = merkez;
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.setGameMode(GameType.SPECTATOR);
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, merkez.getX(), merkez.getZ());
        oyuncu.teleportTo(level, merkez.getX() - UZAKLIK + 0.5, ust + KAMERA_YUKSEKLIGI, merkez.getZ() + 0.5, -90F, 4F);
        DogalAfetler.LOGGER.info(ON + "orman {} ({}), kamera y={}", merkez.toShortString(),
                bulunan == null ? "bulunamadı (doğuş)" : bulunan.getSecond().unwrapKey().map(k -> k.location().toString())
                        .orElse("?"), ust + KAMERA_YUKSEKLIGI);
        sunucuHazir = true;
    }

    private static void at(IntegratedServer server, Atis a) {
        ServerLevel level = server.overworld();
        BlockPos hedef = odak.offset(a.dx(), 0, a.dz());
        boolean var = MeteorAPI.dusur(level, hedef, a.boyut(), a.gorunum(), YanginKaynagi.KOMUT, true, a.aci(), a.azimut())
                .isPresent();
        DogalAfetler.LOGGER.info(ON + "meteor {} {} → {}: varlık {}", a.boyut(), a.gorunum(), hedef.toShortString(), var);
    }

    /** İlk krateri yukarıdan yakından gören kamera. */
    private static void yakinKamera(IntegratedServer server) {
        ServerLevel level = server.overworld();
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, odak.getX(), odak.getZ());
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, odak.getX() - 13.5, ust + 11, odak.getZ() + 0.5, -90F, 38F);
    }

    /** Sunucu thread'inde: kamera yakınında taş platform, sıcak Meteorit Parçası ve Kozmik Çekirdek. */
    private static void oduller(IntegratedServer server) {
        ServerLevel level = server.overworld();
        BlockPos taban = odak.offset(-UZAKLIK, 0, -20);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, taban.getX(), taban.getZ()) + 6;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(taban.getX() + dx, y, taban.getZ() + dz),
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                for (int k = 1; k <= 4; k++) {
                    level.setBlockAndUpdate(new BlockPos(taban.getX() + dx, y + k, taban.getZ() + dz),
                            Blocks.AIR.defaultBlockState());
                }
            }
        }
        MeteorOdulleri.birak(level, new Vec3(taban.getX() + 0.0, y + 1.1, taban.getZ() + 0.5),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
        MeteorOdulleri.birak(level, new Vec3(taban.getX() + 1.6, y + 1.1, taban.getZ() + 0.5),
                new ItemStack(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()), 0);
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, taban.getX() + 0.8, y + 2.6, taban.getZ() - 2.0, 0F, 42F);
        DogalAfetler.LOGGER.info(ON + "ödüller {} y={}", taban.toShortString(), y + 1);
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "meteor_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, parçacık {}, çarpma paketi {})", dosya, mc.getFps(),
                mc.particleEngine.countParticles(), IstemciMeteor.carpmaSayisi());
    }

    /** Kozmik Çekirdek (büyü parlamalı simge + ipucu) ve Termostatlı elmas göğüslüğün ipucu; dünya arkada görünür. */
    private static final class IpucuEkrani extends Screen {
        private final ItemStack cekirdek = new ItemStack(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get());
        private final ItemStack parca = new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get());
        private final ItemStack plaka = new ItemStack(MeteorEsyaKayitlari.TERMOSTAT_PLAKASI.get());
        private final ItemStack zirh = new ItemStack(Items.DIAMOND_CHESTPLATE);

        IpucuEkrani() {
            super(Component.literal("meteor"));
            zirh.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
            ZirhEkleri.koy(zirh, Termostat.TERMOSTATLI);
            ZirhEkleri.koy(zirh, ZirhEkleri.YUNLU);
            ZirhEkleri.tuyAyarla(zirh, 8);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics g, int fareX, int fareY, float kismi) {
            g.fill(0, 0, width, height, 0x60000000);
            simge(g, cekirdek, 24, 24);
            simge(g, parca, 24, 100);
            simge(g, plaka, width / 2 + 10, 24);
            simge(g, zirh, width / 2 + 10, 100);
            g.renderTooltip(font, cekirdek, 96, 40);
            g.renderTooltip(font, parca, 96, 116);
            g.renderTooltip(font, plaka, width / 2 + 82, 40);
            g.renderTooltip(font, zirh, width / 2 + 82, 116);
        }

        private static void simge(GuiGraphics g, ItemStack yigin, int x, int y) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(4, 4, 1);
            g.renderItem(yigin, 0, 0);
            g.pose().popPose();
        }
    }
}
