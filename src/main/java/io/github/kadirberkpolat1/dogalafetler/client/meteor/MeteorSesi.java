package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorVarligi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Meteorun uçuş sesi: vanilla rüzgâr ıslığı döngüsü ({@code item.elytra.flying}) meteoru izler; Doppler için perde yaklaşırken
 * 1,2, uzaklaşırken 0,8 (tasarım §4); ay parçaları %10 tiz. Menzil hacimle (64 / 96 blok).
 */
final class MeteorSesi extends AbstractTickableSoundInstance {
    private final MeteorVarligi meteor;
    private int yas;

    MeteorSesi(MeteorVarligi meteor) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
        this.meteor = meteor;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.x = meteor.getX();
        this.y = meteor.getY();
        this.z = meteor.getZ();
    }

    @Override
    public void tick() {
        if (meteor.isRemoved()) {
            stop();
            return;
        }
        yas++;
        x = meteor.getX();
        y = meteor.getY();
        z = meteor.getZ();
        Player oyuncu = Minecraft.getInstance().player;
        double yaklasma = 0;
        Vec3 hiz = meteor.getDeltaMovement();
        if (oyuncu != null && hiz.lengthSqr() > 1e-8) {
            Vec3 r = oyuncu.getEyePosition().subtract(meteor.position());
            if (r.lengthSqr() > 1e-6) {
                yaklasma = hiz.normalize().dot(r.normalize());
            }
        }
        pitch = (float) ((1.0 + 0.2 * yaklasma) * (meteor.gorunum() == MeteorGorunumu.AY ? 1.1 : 1.0));
        volume = (meteor.boyut().buyuk() ? 6.0F : 4.0F) * Math.min(1.0F, yas / 10.0F);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
