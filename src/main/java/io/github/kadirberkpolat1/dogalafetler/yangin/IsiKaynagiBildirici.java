package io.github.kadirberkpolat1.dogalafetler.yangin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Yangının sıcaklık sistemine geçici ısı kaynağı bildirdiği kanca (madde 7, bölüm G). İmzası
 * {@code SicaklikKancalari.ortamKaynagiEkle(ServerLevel, Vec3, double, double, int)} ile aynıdır; entegratör birleştirmede
 * {@code YanginKancalari.isiKaynagiAyarla(SicaklikKancalari::ortamKaynagiEkle)} ile bağlar.
 */
@FunctionalInterface
public interface IsiKaynagiBildirici {
    IsiKaynagiBildirici BOS = (level, konum, guc, yaricap, sureTick) -> {
    };

    void bildir(ServerLevel level, Vec3 konum, double guc, double yaricap, int sureTick);
}
