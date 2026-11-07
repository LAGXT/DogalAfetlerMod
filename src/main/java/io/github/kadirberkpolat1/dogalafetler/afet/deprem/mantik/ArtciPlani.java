package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Artçı sarsıntı planı (tasarım notu deprem §2 "Afet sonrası iz"; DS-14, AF-D1): ana sarsıntı bittikten sonra, her biri bir
 * öncekinin bitişinden {@code enAzAralik}–{@code enCokAralik} tick sonra başlayan {@code sayi} artçı. İlki ana sarsıntının
 * {@code siddet} kesri kadar güçlüdür, sonrakiler 0,8 kat zayıflar (Båth yasası: en büyük artçı ana şoktan belirgin küçüktür);
 * her biri 3–5 sn sürer. Artçı blok yıkmaz; yalnız sarsıntı, ses ve toz. {@link #olustur} zamanları AKTIF aşamasının
 * başından ölçekli tick'tir; AKTIF artçıları beklemez ({@link #aktifBitti}), kalan plan {@link #devret} ile afet yuvası
 * dışındaki artçı kuyruğuna geçer ve oradan sonra zamanlar devir anından ölçülür. Aynı tohum aynı planı verir. Saf Java'dır.
 */
public final class ArtciPlani {
    public static final int EN_KISA_SURE = 60;
    public static final int EN_UZUN_SURE = 100;
    /** Ana sarsıntı bittikten sonra yarık kazısının bitmesi için en çok bekleme (ölçekli tick); sonra AKTIF yine biter. */
    public static final int KAZI_PAYI = 600;
    private static final double ZAYIFLAMA = 0.8;

    /** Bir artçı: başlangıç ve bitiş (ölçekli tick), ana sarsıntıya göre genlik kesri. */
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

    /**
     * Deprem AKTIF aşaması bitti mi: ana sarsıntı bitti ve yarık kazısı tamam (ya da {@value #KAZI_PAYI} tick pay doldu).
     * Artçılar beklenmez: afet yuvasını tutmazlar (AF-D1).
     */
    public static boolean aktifBitti(long gecen, long anaBitis, boolean kazmaBitti) {
        return gecen >= anaBitis && (kazmaBitti || gecen >= anaBitis + KAZI_PAYI);
    }

    /**
     * AKTIF biterken planın kalanını kuyruğa devreder: zamanlar {@code devir} anından (AKTIF'in o anki geçen tick'i)
     * ölçülür. Kazı payı yüzünden başlangıcı geçmiş artçı devirde başlar; süreleri ve sıraları korunur, çakışmazlar.
     */
    public static List<Artci> devret(List<Artci> plan, long devir) {
        List<Artci> sonuc = new ArrayList<>(plan.size());
        long onceki = 0;
        for (Artci a : plan) {
            long bas = Math.max(a.baslangic() - devir, onceki);
            long bit = bas + a.sure();
            sonuc.add(new Artci(bas, bit, a.kesir()));
            onceki = bit;
        }
        return List.copyOf(sonuc);
    }

    /** Kuyruk bitti mi: son artçı da bitti (boş plan hemen biter). */
    public static boolean bitti(List<Artci> plan, long gecen) {
        return gecen >= son(plan, 0);
    }

    /**
     * {@code /dogalafet asama ileri} kuyrukta: süren artçı varsa onun bitişi, yoksa sıradaki artçının başlangıcı; artçı
     * kalmadıysa son artçının bitişi (kuyruk biter).
     */
    public static long sonrakiSinir(List<Artci> plan, long gecen) {
        for (Artci a : plan) {
            if (gecen < a.baslangic()) {
                return a.baslangic();
            }
            if (gecen < a.bitis()) {
                return a.bitis();
            }
        }
        return son(plan, gecen);
    }
}
