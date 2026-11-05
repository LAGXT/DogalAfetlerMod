package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Donma spike'ının kayıt noktası: capability, olay dinleyicileri, veri paketi yükleyicisi ve komutlar. */
public final class DonmaSpike {
    private DonmaSpike() {
    }

    public static void kaydet(IEventBus modBus) {
        modBus.addListener(ChunkDonmaSeviyesi::kaydet);
        IEventBus forge = MinecraftForge.EVENT_BUS;
        forge.addGenericListener(LevelChunk.class, ChunkDonmaSeviyesi::ekle);
        forge.addListener((AddReloadListenerEvent olay) -> olay.addListener(new DonmaTablosuYukleyici()));
        forge.addListener(DonmaZamanlayici::sunucuBasliyor);
        forge.addListener(DonmaZamanlayici::chunkYuklendi);
        forge.addListener(DonmaZamanlayici::chunkBosaltildi);
        forge.addListener(DonmaZamanlayici::tick);
        forge.addListener(DonmaZamanlayici::sunucuTick);
        KomutKaydi.ekle(DonmaKomutlari.olustur());
    }
}
