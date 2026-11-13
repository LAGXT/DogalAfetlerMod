package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Gezegen bakma ilerlemesinin bit maskesi (K-E84: sunucu bunu oyuncunun kalıcı verisinde saklar). */
class BakmaKurallariTest {
    @Test
    void sekizGezegeninHepsiEklenincePlutonDahilTamdir() {
        assertEquals(BakmaKurallari.GEZEGEN_SAYISI, GezegenYerlesimi.SAYI, "bakılacak gezegen sayısı yerleşimdekiyle aynı");
        int maske = 0;
        for (int i = 0; i < BakmaKurallari.GEZEGEN_SAYISI; i++) {
            assertFalse(BakmaKurallari.tamMi(maske), "eksikken tam değil: " + i);
            maske = BakmaKurallari.maskeyeEkle(maske, i);
            assertTrue(BakmaKurallari.maskedeVar(maske, i));
        }
        assertTrue(BakmaKurallari.tamMi(maske));
        assertEquals(BakmaKurallari.tamMaske(), maske);
    }

    @Test
    void tekrarVeGecersizNumaraMaskeyiDegistirmez() {
        int maske = BakmaKurallari.maskeyeEkle(0, 3);
        assertEquals(maske, BakmaKurallari.maskeyeEkle(maske, 3));
        assertEquals(maske, BakmaKurallari.maskeyeEkle(maske, -1));
        assertEquals(maske, BakmaKurallari.maskeyeEkle(maske, BakmaKurallari.GEZEGEN_SAYISI));
        assertFalse(BakmaKurallari.maskedeVar(maske, 99));
    }
}
