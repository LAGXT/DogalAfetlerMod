package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Artçı sarsıntı planı (tasarım notu deprem §2 "Afet sonrası iz"; DS-14): ana sarsıntı bittikten sonra, her biri bir öncekinin
 * bitişinden {@code enAzAralik}–{@code enCokAralik} tick sonra başlayan {@code sayi} artçı. İlki ana sarsıntının
 * {@code siddet} kesri kadar güçlüdür, sonrakiler 0,8 kat zayıflar (Båth yasası: en büyük artçı ana şoktan belirgin küçüktür);
 * her biri 3–5 sn sürer. Artçı blok yıkmaz; yalnız sarsıntı, ses ve toz. Zamanlar AKTIF aşamasının başından ölçekli tick'tir.
 * Aynı tohum aynı planı verir. Saf Java'dır.
 */
public final class ArtciPlani {
    public static final int EN_KISA_SURE = 60;
    public static final int EN_UZUN_SURE = 100;
    private static final double ZAYIFLAMA = 0.8;

    /** Bir artçı: başlangıç ve bitiş (AKTIF'in başından ölçekli tick), ana sarsıntıya göre genlik kesri. */
    public record Artci(long baslangic, long bitis, double kesir) {
        public int sure() {
            return (int) (bitis - baslangic);
        }

        public boolean suruyor(long gecen) {
            return gecen >= baslangic && gecen < bitis;
        }
    }

    private ArtciPlani() {
    }

    /**
     * @param anaBitis ana sarsıntının bittiği an (= sarsıntı süresi)
     * @param sayi artçı sayısı (0 → boş plan)
     */
    public static List<Artci> olustur(long anaBitis, int sayi, int enAzAralik, int enCokAralik, double siddet, long tohum) {
        SplittableRandom r = new SplittableRandom(tohum ^ 0x5DEECE66DL);
        int enAz = Math.max(1, Math.min(enAzAralik, enCokAralik));
        int enCok = Math.max(enAz, Math.max(enAzAralik, enCokAralik));
        List<Artci> plan = new ArrayList<>();
        long onceki = anaBitis;
        double kesir = Math.max(0, siddet);
        for (int i = 0; i < sayi; i++) {
            long bas = onceki + enAz + r.nextInt(enCok - enAz + 1);
            long bit = bas + EN_KISA_SURE + r.nextInt(EN_UZUN_SURE - EN_KISA_SURE + 1);
            plan.add(new Artci(bas, bit, kesir));
            onceki = bit;
            kesir *= ZAYIFLAMA;
        }
        return List.copyOf(plan);
    }

    /** {@code gecen} anında süren artçının sırası (1'den), yoksa 0. */
    public static int suren(List<Artci> plan, long gecen) {
        for (int i = 0; i < plan.size(); i++) {
            if (plan.get(i).suruyor(gecen)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** Son artçının bitişi; plan boşsa {@code anaBitis}. */
    public static long son(List<Artci> plan, long anaBitis) {
        return plan.isEmpty() ? anaBitis : Math.max(anaBitis, plan.get(plan.size() - 1).bitis());
    }
}
