package io.github.kadirberkpolat1.dogalafetler.rehber;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.BuzBalikciligi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BuzDeligiAcma;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaBloklari;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.rehber.mantik.KonusmaRitmi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikParcaciklari;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * Rehber cümlelerinin sunucu tarafı (K-E74). Üç tetikleyici, hepsi oyuncu başına bir kez ({@link RehberVerisi}):
 * <ul>
 * <li>{@link RehberMesaji#BALIK}: güneş evresi {@link GunesEvresi#GUNESSIZ} (buzul kıyameti, donma başlar) iken saniyede bir
 * taranır; üst dünyadaki her oyuncuya, sonradan girenlere de. Yaratıcı ve izleyici oyuncuya söylenmez (bayrak kalır).</li>
 * <li>{@link RehberMesaji#ALET} ve {@link RehberMesaji#EGIL}: oyuncu buz deliği açılabilecek bir buza ({@code BuzDeligiAcma}'nın
 * koşulları: buz, üstü boş, {@link BuzBalikciligi#aktifMi}) ayakta sağ tıklar. El boşsa ALET, kazma ya da balta varsa EGIL.
 * Eğilerek tıklama deliği {@code BuzDeligiAcma} açar; bu sınıf olayı iptal etmez ve deliğe dokunmaz.</li>
 * </ul>
 * Cümle söylenirken ağızdan ritimli buhar çıkar ({@link KonusmaRitmi}); başkaları buharı sunucudan alır (oyuncunun kendi
 * buharını istemci üretir, sicaklik deseni: kendisine parçacık gitmez).
 */
final class RehberOlaylari {
    /** Buharın gönderildiği en uzak izleyici (blok). */
    private static final double IZLEME_KARESI = 48.0 * 48.0;
    /** Her buhar parçasındaki bulutçuk sayısı. */
    private static final int BULUTCUK = 3;

    private record Konusma(RehberMesaji mesaj, long baslangic) {
    }

    private static final Map<UUID, Konusma> KONUSMALAR = new HashMap<>();

    private RehberOlaylari() {
    }

    static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        int tick = server.getTickCount();
        if (!KONUSMALAR.isEmpty()) {
            buharVer(server, tick);
        }
        if (tick % 20 == 0 && DunyaDonemi.al(server).gunesEvresi() == GunesEvresi.GUNESSIZ) {
            for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
                if (oyuncu.level().dimension() == Level.OVERWORLD) {
                    soyle(oyuncu, RehberMesaji.BALIK);
                }
            }
        }
    }

    /** Buz deliği açılabilecek buza ayakta sağ tık: el boşsa "alet", alet elindeyse "eğilmeliyim". */
    static void sagTik(PlayerInteractEvent.RightClickBlock olay) {
        // Aynı tık iki el için de gelir; yalnız ana el sayılır. İstemci tarafında oyuncu ServerPlayer değildir.
        if (olay.getHand() != InteractionHand.MAIN_HAND || !(olay.getEntity() instanceof ServerPlayer oyuncu)
                || oyuncu.isSecondaryUseActive() || oyuncu.isCreative() || oyuncu.isSpectator()) {
            return;
        }
        ServerLevel level = oyuncu.serverLevel();
        BlockPos konum = olay.getPos();
        BlockState buz = level.getBlockState(konum);
        if (!buz.is(Blocks.ICE) && !buz.is(DonmaBloklari.INCE_BUZ.get()) || !level.isEmptyBlock(konum.above())
                || !BuzBalikciligi.aktifMi(level, konum)) {
            return;
        }
        ItemStack ana = oyuncu.getMainHandItem();
        if (BuzDeligiAcma.aletMi(ana) || ana.isEmpty() && BuzDeligiAcma.aletMi(oyuncu.getOffhandItem())) {
            soyle(oyuncu, RehberMesaji.EGIL);
        } else if (ana.isEmpty()) {
            soyle(oyuncu, RehberMesaji.ALET);
        }
    }

    /** Ölüm ve End dönüşü: bayraklar yeni oyuncu nesnesine taşınır (ölümde de korunur). */
    static void kopyalandi(PlayerEvent.Clone olay) {
        if (olay.getOriginal() instanceof ServerPlayer eski && olay.getEntity() instanceof ServerPlayer yeni) {
            RehberVerisi.kopyala(eski, yeni);
        }
    }

    static void oyuncuCikti(PlayerEvent.PlayerLoggedOutEvent olay) {
        KONUSMALAR.remove(olay.getEntity().getUUID());
    }

    static void sunucuDurdu(ServerStoppedEvent olay) {
        KONUSMALAR.clear();
    }

    /**
     * Cümleyi bu oyuncuya bir kez söyletir: bayrak işaretlenir, paket gider, buhar başlar. Yaratıcı ve izleyici oyuncuya
     * ve daha önce söylenmiş cümleye hiçbir şey yapılmaz. Yeni cümle süren eskisinin yerine geçer.
     */
    private static void soyle(ServerPlayer oyuncu, RehberMesaji mesaj) {
        if (oyuncu.isCreative() || oyuncu.isSpectator() || !oyuncu.isAlive() || RehberVerisi.soylendi(oyuncu, mesaj)) {
            return;
        }
        RehberVerisi.isaretle(oyuncu, mesaj);
        AgKanali.oyuncuyaGonder(oyuncu, new RehberMesajPaketi(mesaj.anahtar()));
        KONUSMALAR.put(oyuncu.getUUID(), new Konusma(mesaj, oyuncu.server.getTickCount()));
    }

    /** Konuşan her oyuncunun ağzından ritimli buhar: yakındaki başkalarına gider, kendisine gitmez. */
    private static void buharVer(MinecraftServer server, int tick) {
        for (Iterator<Map.Entry<UUID, Konusma>> it = KONUSMALAR.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Konusma> e = it.next();
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(e.getKey());
            long gecen = tick - e.getValue().baslangic();
            if (oyuncu == null || !oyuncu.isAlive() || oyuncu.isSpectator() || gecen >= KonusmaRitmi.SURE_TICK) {
                it.remove();
                continue;
            }
            if (KonusmaRitmi.pufVar(e.getValue().mesaj().puflar(), (int) gecen)) {
                agizBuhari(oyuncu);
            }
        }
    }

    private static void agizBuhari(ServerPlayer oyuncu) {
        ServerLevel level = oyuncu.serverLevel();
        Vec3 bakis = oyuncu.getLookAngle();
        Vec3 agiz = oyuncu.getEyePosition().add(bakis.scale(0.35)).add(0, -0.12, 0);
        for (ServerPlayer izleyen : level.players()) {
            if (izleyen == oyuncu || izleyen.distanceToSqr(oyuncu) > IZLEME_KARESI) {
                continue;
            }
            for (int i = 0; i < BULUTCUK; i++) {
                Vec3 yon = bakis.add(oyuncu.getRandom().nextGaussian() * 0.18,
                        0.06 + oyuncu.getRandom().nextGaussian() * 0.08, oyuncu.getRandom().nextGaussian() * 0.18);
                level.sendParticles(izleyen, SicaklikParcaciklari.NEFES_BUHARI.get(), false, agiz.x, agiz.y, agiz.z, 0,
                        yon.x, yon.y, yon.z, 0.09);
            }
        }
    }
}
