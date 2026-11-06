package io.github.kadirberkpolat1.dogalafetler;

import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.donma.DisKancalar;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaApi;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** {@link Kancalar}'ın donma uçları: sanal suya giden üç kanca ve {@code kizilcuce bilgi} kış satırı. */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KancaOyunTestleri {
    private KancaOyunTestleri() {
    }

    @GameTest(template = "bos", batch = "kanca_donma")
    public static void donmaKancalariSanalSuyaVeGokyuzuneBagli(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        MinecraftServer server = level.getServer();
        DisKancalar kancalar = DisKancalar.al();
        y.assertTrue(kancalar != DisKancalar.BOS, "donma kancaları bağlı değil");

        CompoundTag su = DunyaDonemi.al(server).ekVeri("su");
        boolean vardi = su.contains("buharTaban");
        float onceki = su.getFloat("buharTaban");
        su.putFloat("buharTaban", 2.5f);
        double pay;
        try {
            pay = kancalar.buharlasmaPayi(server);
        } finally {
            if (vardi) {
                su.putFloat("buharTaban", onceki);
            } else {
                su.remove("buharTaban");
            }
        }
        y.assertTrue(Math.abs(pay - 2.5) < 1e-6, "buharlaşma payı sanal sudan gelmedi: " + pay);

        BlockPos p = y.absolutePos(BlockPos.ZERO);
        boolean beklenen = p.getY() < SanalSu.suSeviyesi(level, p.getX(), p.getZ());
        y.assertTrue(kancalar.sanalSuVarMi(level, p.getX(), p.getY(), p.getZ()) == beklenen,
                "sanalSuVarMi SanalSu.suSeviyesi ile aynı olmalı");
        kancalar.suYuzeyiDegisti(level, new ChunkPos(p));

        List<Component> cikti = new ArrayList<>();
        CommandSource yakalayici = new CommandSource() {
            @Override
            public void sendSystemMessage(Component mesaj) {
                cikti.add(mesaj);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack kaynak = new CommandSourceStack(yakalayici, Vec3.atCenterOf(p), Vec2.ZERO, level, 4, "kanca",
                Component.literal("kanca"), server, null);
        server.getCommands().performPrefixedCommand(kaynak, "dogalafet kizilcuce bilgi");
        for (Component satir : DonmaApi.kisBolgesiSatirlari(kaynak)) {
            y.assertTrue(cikti.contains(satir), "kizilcuce bilgi donma kış satırını içermiyor: " + satir.getString());
        }
        y.succeed();
    }
}
