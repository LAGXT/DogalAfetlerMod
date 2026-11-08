package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/**
 * S3-08 / §A6: HUD'un ({@code sicaklik/durum}) ve termometre menüsünün ({@code sicaklik/dokum}, {@code sicaklik/dokum_iste})
 * paketleri sunucunun yazdığı değerleri istemciye bozmadan taşır: kodla → çöz aynı değer, tampon tam tükenir.
 */
class SicaklikPaketleriTest {
    private static FriendlyByteBuf tampon() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    @Test
    void durumPaketiHerKademeVeSolElIleAynenGeriGelir() {
        Esikler e = Esikler.VARSAYILAN;
        float[] vucutlar = {-100f, -71.5f, -40f, -12.25f, 0f, 19.75f, 32f, 55.5f, 99f};
        for (float vucut : vucutlar) {
            for (SolEl solEl : SolEl.values()) {
                SicaklikDurumPaketi p = SicaklikDurumPaketi.acik(vucut, vucut * 0.5f + 3.25f, -17.5f, -23.75f,
                        -0.125f, 0.625f, solEl, e);
                FriendlyByteBuf t = tampon();
                p.yaz(t);
                SicaklikDurumPaketi geri = SicaklikDurumPaketi.oku(t);
                assertEquals(p, geri, "vücut " + vucut + " sol el " + solEl);
                assertEquals(0, t.readableBytes(), "durum paketi fazla bayt bırakmamalı");
            }
        }
        for (Kademe k : Kademe.values()) {
            SicaklikDurumPaketi p = new SicaklikDurumPaketi(true, 1f, 2f, 3f, 4f, 5f, k, 0.5f, SolEl.LAV_KOVASI, 6f,
                    -20f, -40f, -70f, 20f, 50f, 70f);
            FriendlyByteBuf t = tampon();
            p.yaz(t);
            assertEquals(p, SicaklikDurumPaketi.oku(t), "kademe " + k);
        }
        FriendlyByteBuf t = tampon();
        SicaklikDurumPaketi.KAPALI.yaz(t);
        SicaklikDurumPaketi kapali = SicaklikDurumPaketi.oku(t);
        assertEquals(SicaklikDurumPaketi.KAPALI, kapali);
        assertTrue(!kapali.acik(), "kapalı paket istemcide HUD'u gizler");
    }

    @Test
    void dokumPaketiHerKalemiYerindeTasir() {
        float[] kalemler = new float[Katki.values().length];
        Arrays.fill(kalemler, Float.NaN);
        for (Katki k : Katki.values()) {
            if (k.ordinal() % 3 != 1) {
                kalemler[k.ordinal()] = (k.ordinal() - 6) * 1.25f;
            }
        }
        for (SolEl solEl : SolEl.values()) {
            SicaklikDokumPaketi p = new SicaklikDokumPaketi(-12.5f, -19.25f, -44f, -0.75f, 0.83f, 61.5f, 27f, 0.4f,
                    solEl, kalemler);
            FriendlyByteBuf t = tampon();
            p.yaz(t);
            SicaklikDokumPaketi geri = SicaklikDokumPaketi.oku(t);
            assertEquals(0, t.readableBytes(), "döküm paketi fazla bayt bırakmamalı");
            assertEquals(p.mekan(), geri.mekan());
            assertEquals(p.hissedilen(), geri.hissedilen());
            assertEquals(p.vucut(), geri.vucut());
            assertEquals(p.egilim(), geri.egilim());
            assertEquals(p.islaklik(), geri.islaklik());
            assertEquals(p.kurumaSaniye(), geri.kurumaSaniye());
            assertEquals(p.ruzgarKmh(), geri.ruzgarKmh());
            assertEquals(p.yalitim(), geri.yalitim());
            assertEquals(solEl, geri.solEl());
            assertArrayEquals(kalemler, geri.katkilar(), "olmayan kalem NaN kalır, olan yerinde");
            for (Katki k : Katki.values()) {
                assertEquals(Float.floatToIntBits(p.katki(k)), Float.floatToIntBits(geri.katki(k)), "kalem " + k);
            }
        }
    }

    @Test
    void dokumPaketiFarkliKalemSayisiniBozmadanOkur() {
        int kalem = Katki.values().length;
        FriendlyByteBuf t = tampon();
        for (int i = 0; i < 8; i++) {
            t.writeFloat(i + 0.5f);
        }
        t.writeByte(SolEl.BUZ_KOVASI.ordinal());
        t.writeVarInt(kalem + 2);
        for (int i = 0; i < kalem + 2; i++) {
            t.writeFloat(i);
        }
        SicaklikDokumPaketi fazla = SicaklikDokumPaketi.oku(t);
        assertEquals(0, t.readableBytes(), "fazla kalemler okunup atılır");
        assertEquals(kalem, fazla.katkilar().length);
        assertEquals(kalem - 1f, fazla.katki(Katki.values()[kalem - 1]));
        assertEquals(SolEl.BUZ_KOVASI, fazla.solEl());

        FriendlyByteBuf az = tampon();
        new SicaklikDokumPaketi(1f, 2f, 3f, 4f, 0.5f, -1f, 5f, 0f, SolEl.YOK, new float[] {7f, 8f}).yaz(az);
        SicaklikDokumPaketi eksik = SicaklikDokumPaketi.oku(az);
        assertEquals(7f, eksik.katki(Katki.values()[0]));
        assertEquals(8f, eksik.katki(Katki.values()[1]));
        assertTrue(Float.isNaN(eksik.katki(Katki.SOL_EL)), "gönderilmeyen kalem NaN (menüde gösterilmez)");
    }

    @Test
    void dokumIstegiBosGovdeyleGidipGelir() {
        FriendlyByteBuf t = tampon();
        new SicaklikDokumIstegi().yaz(t);
        assertEquals(0, t.readableBytes(), "istek gövdesiz");
        assertEquals(new SicaklikDokumIstegi(), SicaklikDokumIstegi.oku(t));
    }
}
