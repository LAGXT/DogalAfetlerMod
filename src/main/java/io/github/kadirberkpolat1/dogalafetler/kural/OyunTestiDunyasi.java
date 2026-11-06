package io.github.kadirberkpolat1.dogalafetler.kural;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * GameTest sunucusunun dünya kuralları (K0-31). Forge'un GameTest sunucusu dünyayı {@code server.properties}'ten kurar ve
 * vanilla GameTest kurallarını uygulamaz; mob doğması, hava ve gün döngüsü, rastgele tick ve ateş yayılması testlerin
 * sahnesini bozar. Bunlar yalnız {@link GameTestServer} örneğinde, açılışta (testler başlamadan) kapatılır. Düz dünya
 * ayarlarını {@code build.gradle}'daki {@code runGameTestServer} görevi yazar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class OyunTestiDunyasi {
    private OyunTestiDunyasi() {
    }

    @SubscribeEvent
    public static void sunucuBasladi(ServerStartedEvent olay) {
        MinecraftServer server = olay.getServer();
        if (!(server instanceof GameTestServer)) {
            return;
        }
        GameRules kurallar = server.getGameRules();
        kurallar.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        kurallar.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        kurallar.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        kurallar.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        kurallar.getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        DogalAfetler.LOGGER.info("GameTest dünyası '{}' (düz: {}) kuralları ayarlandı: {}={}, {}={}, {}={}, {}={}, {}={}",
                server.getWorldData().getLevelName(), server.getWorldData().isFlatWorld(),
                GameRules.RULE_DOMOBSPAWNING.getId(), kurallar.getBoolean(GameRules.RULE_DOMOBSPAWNING),
                GameRules.RULE_WEATHER_CYCLE.getId(), kurallar.getBoolean(GameRules.RULE_WEATHER_CYCLE),
                GameRules.RULE_DAYLIGHT.getId(), kurallar.getBoolean(GameRules.RULE_DAYLIGHT),
                GameRules.RULE_RANDOMTICKING.getId(), kurallar.getInt(GameRules.RULE_RANDOMTICKING),
                GameRules.RULE_DOFIRETICK.getId(), kurallar.getBoolean(GameRules.RULE_DOFIRETICK));
    }
}
