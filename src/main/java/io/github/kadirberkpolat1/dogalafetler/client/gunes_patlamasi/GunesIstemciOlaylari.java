package io.github.kadirberkpolat1.dogalafetler.client.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiSesleri;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.GunesGorselEgrileri;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Güneş patlamasının istemci etkileri (tasarım §2, §4, §5; N13, N15). Tek durum kaynağı çekirdeğin {@code DonemPaketi}'dir
 * (evre, ilerleme, evre başlangıç saati): yeni paket yok, sonradan giren oyuncu aynı kareyi görür. Gökyüzü için
 * yalnız Forge'un {@code AFTER_SKY} aşaması, ekran örtüleri için {@code AFTER_WEATHER} kullanılır (mixin yok;
 * Gökyüzü'nün çizicisine dokunulmaz). Süpernova ve kızıl cüce sesleri evre saniyesinden istemcide bir kez çalar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GunesIstemciOlaylari {
    private static long sesDongusu = Long.MIN_VALUE;
    private static boolean basincCaldi;
    private static boolean kristalCaldi;
    private static boolean akorCaldi;

    private GunesIstemciOlaylari() {
    }

    @SubscribeEvent
    public static void asama(RenderLevelStageEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }
        float pt = olay.getPartialTick();
        DonemPaketi d = IstemciDonem.durum();
        if (olay.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            if (d.gunesEvresi() == GunesEvresi.BUYUYEN) {
                float guc = GunesGorselEgrileri.aurora(d.evreIlerlemesi(), 1f - gunIsigi(level, pt), level.getRainLevel(pt));
                if (guc > 0.01f) {
                    GunesGokCizici.aurora(olay.getPoseStack().last().pose(), guc, level.getGameTime() + pt, kalite());
                }
            }
        } else if (olay.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            GunesGokCizici.ekran(olay.getProjectionMatrix(), ortuler(mc, level, d, pt), level.getGameTime() + pt, kalite());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || mc.level == null) {
            return;
        }
        DonemPaketi d = IstemciDonem.durum();
        KavrulmaRengi.denetle(mc, d);
        if (mc.isPaused()) {
            return;
        }
        if (d.evreBaslangicSaati() != sesDongusu) {
            sesDongusu = d.evreBaslangicSaati();
            basincCaldi = false;
            kristalCaldi = false;
            akorCaldi = false;
        }
        float s = evreSaniyesi(d, 0f);
        float cokme = GokyuzuEgrileri.SN_COKME_SN;
        if (d.gunesEvresi() == GunesEvresi.SUPERNOVA) {
            if (!basincCaldi && s >= cokme && s < cokme + 3f) {
                basincCaldi = true;
                cal(mc, GunesPatlamasiSesleri.SUPERNOVA_BASINC);
            }
            if (!kristalCaldi && s >= cokme + 6f && s < cokme + 16f) {
                kristalCaldi = true;
                cal(mc, GunesPatlamasiSesleri.SUPERNOVA_KRISTAL);
            }
        } else if (d.gunesEvresi() == GunesEvresi.KIZIL_CUCE && !akorCaldi && s < 6f && d.evreIlerlemesi() < 1f) {
            akorCaldi = true;
            cal(mc, GunesPatlamasiSesleri.KIZIL_CUCE_AKOR);
        }
    }

    /** Ekran örtüleri: süpernova beyaz parlaması, ısı dalgalanması, kızıl cüce şafak tonu (her biri 0–1). */
    static GunesGokCizici.Ortuler ortuler(Minecraft mc, ClientLevel level, DonemPaketi d, float pt) {
        boolean gok = gokuGoruyor(mc, level);
        return switch (d.gunesEvresi()) {
            case SUPERNOVA -> new GunesGokCizici.Ortuler(0.85f * GokyuzuEgrileri.supernovaFlas(evreSaniyesi(d, pt))
                    * (gok ? 1f : 0.35f), 0f, 0f);
            case BUYUYEN -> new GunesGokCizici.Ortuler(0f,
                    GunesGorselEgrileri.isiDalgasi(d.evreIlerlemesi(), gunIsigi(level, pt), gok), 0f);
            case KIZIL_CUCE -> new GunesGokCizici.Ortuler(0f, 0f, GunesGorselEgrileri.safakTonu(d.evreIlerlemesi()));
            // Pulsar (gökyüzü-2): ısı dalgalanması güneşe değil ısınmaya bağlı, gece de sürer.
            case PULSAR -> new GunesGokCizici.Ortuler(0f, gok ? PulsarEgrileri.yogunluk(d.evreIlerlemesi())
                    * (0.6f + 0.4f * gunIsigi(level, pt)) : 0f, 0f);
            default -> GunesGokCizici.Ortuler.YOK;
        };
    }

    /** Evrenin başından beri geçen ölçekli saniye (gökyüzü G9 ile aynı saat). */
    static float evreSaniyesi(DonemPaketi d, float pt) {
        return (float) Math.max(0.0, (IstemciDonem.saat(pt) - d.evreBaslangicSaati()) / 20.0);
    }

    /** Vanilla gök karartma formülü: gündüz 1, gece 0. */
    static float gunIsigi(ClientLevel level, float pt) {
        return Mth.clamp(Mth.cos(level.getTimeOfDay(pt) * Mth.TWO_PI) * 2f + 0.5f, 0f, 1f);
    }

    private static boolean gokuGoruyor(Minecraft mc, ClientLevel level) {
        return level.canSeeSky(BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition()));
    }

    private static EfektKalitesi kalite() {
        return IstemciAyarlari.efektKalitesi();
    }

    private static void cal(Minecraft mc, ResourceLocation ses) {
        mc.getSoundManager().play(new SimpleSoundInstance(ses, SoundSource.AMBIENT, 1f, 1f,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        DogalAfetler.LOGGER.info("[gunes] ses çalındı: {}", ses);
    }
}
