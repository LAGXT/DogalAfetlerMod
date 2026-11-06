package io.github.kadirberkpolat1.dogalafetler;

import io.github.kadirberkpolat1.dogalafetler.donma.DisKancalar;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaApi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.KizilCuceBilgiKatkisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import io.github.kadirberkpolat1.dogalafetler.su.DenizYukselmesiSureci;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKancalari;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Alt sistemler arasındaki kancaları bağlar (her kancanın sözleşmesi sahibinin {@code .vibe/kararlar/<parça>.md} §A'sında).
 * Mantık sahiplerdedir; burada yalnız bir satırlık adaptörler durur. Ortak koddur: istemci sınıfına dokunmaz.
 *
 * <p>Sanal su → sıcaklık ıslaklığı için bağ yoktur: sıcaklık {@code isInWater()} okur, sanal su mixin'i o alanı doldurur.
 */
final class Kancalar {
    private Kancalar() {
    }

    /** Mod kurucusunda, alt sistem kayıtlarından sonra bir kez. */
    static void bagla() {
        YanginKancalari.isiKaynagiAyarla(SicaklikKancalari::ortamKaynagiEkle);
        YanginKancalari.sanalSuAyarla((level, konum) -> {
            double yuzey = SanalSu.suSeviyesi(level, konum.getX(), konum.getZ());
            return !Double.isNaN(yuzey) && yuzey > konum.getY();
        });
        DisKancalar.ayarla(new DisKancalar() {
            @Override
            public void suYuzeyiDegisti(ServerLevel level, ChunkPos chunk) {
                SanalSu.chunkDegisti(level, chunk);
            }

            @Override
            public boolean sanalSuVarMi(ServerLevel level, int x, int y, int z) {
                return y < SanalSu.suSeviyesi(level, x, z);
            }

            @Override
            public double buharlasmaPayi(MinecraftServer server) {
                return DenizYukselmesiSureci.buharlasmaPayi(server);
            }
        });
        SicaklikKancalari.kisBolgesiSaglayiciAyarla(DonmaApi::kisAgirligi);
        KizilCuceBilgiKatkisi.ekle(DonmaApi::kisBolgesiSatirlari);
    }
}
