package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YorungeTest {
    @Test
    void hedefeGirisAcisiVeHizlaVarir() {
        for (double aci : new double[] {40, 55, 70, 90}) {
            for (double azimut = 0; azimut < 2 * Math.PI; azimut += 0.7) {
                Yorunge y = Yorunge.hesapla(100.5, 64, -20.5, aci, azimut, 140, 3.0);
                assertEquals(204.0, y.baslangicY(), 1e-9, "başlangıç hedefin yükseklik kadar üstünde");
                double hiz = Math.sqrt(y.hizX() * y.hizX() + y.hizY() * y.hizY() + y.hizZ() * y.hizZ());
                assertEquals(3.0, hiz, 1e-9, "hız büyüklüğü");
                double yatay = Math.hypot(y.hizX(), y.hizZ());
                assertEquals(aci, Math.toDegrees(Math.atan2(-y.hizY(), yatay)), 1e-6, "giriş açısı");
                double[] son = y.konum(y.tamSure());
                assertEquals(100.5, son[0], 1e-6);
                assertEquals(64.0, son[1], 1e-6);
                assertEquals(-20.5, son[2], 1e-6);
                assertTrue(y.sureTick() >= y.tamSure() && y.sureTick() < y.tamSure() + 1, "süre tavanı");
            }
        }
    }

    @Test
    void dikGiriseYatayUzaklikSifirSigGiriseUzak() {
        Yorunge dik = Yorunge.hesapla(0, 0, 0, 90, 1.0, 140, 3.0);
        assertEquals(0.0, Math.hypot(dik.baslangicX(), dik.baslangicZ()), 1e-6);
        Yorunge sig = Yorunge.hesapla(0, 0, 0, 40, 0.0, 140, 3.0);
        assertEquals(140 / Math.tan(Math.toRadians(40)), Math.hypot(sig.baslangicX(), sig.baslangicZ()), 1e-6);
        assertEquals(140 / Math.tan(Math.toRadians(40)), sig.baslangicX(), 1e-6, "azimut 0: +X yönünden gelir");
    }

    @Test
    void aciSinirlanir() {
        Yorunge y = Yorunge.hesapla(0, 0, 0, 5, 0, 100, 2.0);
        double yatay = Math.hypot(y.hizX(), y.hizZ());
        assertEquals(Yorunge.EN_KUCUK_ACI, Math.toDegrees(Math.atan2(-y.hizY(), yatay)), 1e-6);
    }
}
