package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Öncü sarsıntının şiddet eğrisi ve darbe zamanlaması (madde 13b). Şiddet 0–1: seviye ve ön-uyarı ilerlemesiyle artar
 * (başta seyrek ve zayıf, siren yaklaştıkça sık ve belirgin). Sarsıntı sürekli değil darbelerle gelir: iki darbe arası
 * 12 sn'den 3 sn'ye iner; her darbe 2 sn süren yumuşak bir zarftır. Ekran titremesi ve ses bu zarfla çalışır.
 */
public final class OncuSarsinti {
    public static final int DARBE_SURESI = 40;
    public static final int EN_UZUN_ARALIK = 240;
    public static final int EN_KISA_ARALIK = 60;

    private OncuSarsinti() {
    }

    /** Seviye 1 → en çok 0,48; seviye 5 → en çok 1. İlerleme 0'da tavanın beşte biri. */
    public static double siddet(double ilerleme, int seviye) {
        double i = Math.max(0, Math.min(1, ilerleme));
        int s = Math.max(1, Math.min(5, seviye));
        return (0.35 + 0.13 * s) * (0.2 + 0.8 * Math.pow(i, 1.5));
    }

    /** İki darbenin başlangıçları arasındaki tick. */
    public static int darbeAraligi(double siddet) {
        double s = Math.max(0, Math.min(1, siddet));
        return (int) Math.round(EN_UZUN_ARALIK + (EN_KISA_ARALIK - EN_UZUN_ARALIK) * s);
    }

    /** Darbenin başından beri geçen tick'teki zarf 0–1 ({@code sin²}; uçlarda 0). */
    public static double zarf(int darbedeTick) {
        if (darbedeTick < 0 || darbedeTick >= DARBE_SURESI) {
            return 0;
        }
        double s = Math.sin(Math.PI * darbedeTick / DARBE_SURESI);
        return s * s;
    }

    /** Öncü sarsıntı sesinin seviyesi: kısık (0,08–0,30), konuşmayı bastırmaz (madde 5). */
    public static float sesSeviyesi(double siddet) {
        return (float) (0.08 + 0.22 * Math.max(0, Math.min(1, siddet)));
    }

    /** Kamera titremesinin en büyük açısı (derece): çok hafif, tam şiddette ve çarpan 1'de 0,6°. */
    public static double titremeAcisi(double siddet, double zarf, double carpan) {
        return 0.6 * Math.max(0, Math.min(1, siddet)) * Math.max(0, Math.min(1, zarf)) * Math.max(0, carpan);
    }
}
