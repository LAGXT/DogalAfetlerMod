package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/** Sütun düzeyinde kış ağırlığı ve ılık bölge ofseti (madde 9 ÇÖZÜLME). Koordinatlar blok cinsinden. */
public interface KisKaynagi {
    /** Kış ağırlığı [0, 1]: 0 ılık, 1 kalıcı kış. */
    double agirlik(int x, int z, int yuzeyY);

    /** Çözülme döneminde hedeften düşülen ılıklık ofseti (seviye puanı, ≥ 0). */
    double ofset(int x, int z);

    KisKaynagi YOK = new KisKaynagi() {
        @Override
        public double agirlik(int x, int z, int yuzeyY) {
            return 0.0;
        }

        @Override
        public double ofset(int x, int z) {
            return 0.0;
        }
    };
}
