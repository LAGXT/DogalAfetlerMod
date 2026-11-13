package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import net.minecraft.server.level.ServerPlayer;

/**
 * Başka alt sistemlerin kendi olaylarından başarım vermesi için tek giriş noktası. Anahtar, başarımın advancement yoludur
 * ({@link Basarimlar#yol()}); yalnız {@link Basarimlar#ozelMi()} olan başarımlar bu yolla verilir. Sunucu ana thread'inde
 * çağrılır. Aynı oyuncuya aynı anahtar tekrar verilirse sessizce yok sayılır (advancement zaten tamamdır, vanilla ölçüt
 * dinleyicisi kalmaz); tick'te çağrılması ucuzdur, yine de koşulu çağıran taraf denetler. Bilinmeyen anahtar programlama
 * hatasıdır: uyarı yazılır ve hiçbir şey verilmez.
 * <ul>
 * <li>{@link #DEVASA}: devasa kara deliğe 5 sn bakmak (gökyüzü).</li>
 * <li>{@link #ASTRONOM}: tüm gezegenlere en az 3 sn bakmak (gökyüzü; gizli başarım).</li>
 * <li>{@link #CAYIR_CAYIR}: kömürleşmiş kütük azami ısıya ulaşınca (kütük).</li>
 * <li>{@link #SIFON}: tsunaminin mağaraya akıttığı suyun yolunu blokla kapatmak (tsunami).</li>
 * <li>{@link #INCE_BUZ_KIRICI}: ince buz kırılınca (buharlaşma/ince buz).</li>
 * <li>{@link #YINELEME_IKIZ}: Yineleme büyülü olta ile katlanan tutuş (balıkçılık).</li>
 * <li>{@link #DEFTER_ILK_ACILIS}: Afet Defteri'ni ilk açış (defter).</li>
 * <li>{@link #KOLYE_SERIN}: Serin Kolye takmak (sıcaklık/soğutma).</li>
 * <li>Başarım diliminin kendi tetikleyicileri: {@link #GOKTEN_ZIRH}, {@link #SALLANAN_KULE}, {@link #YUKSEK_ZEMIN},
 * {@link #GOZUN_ICINDE}, {@link #CIFTE_BELA}.</li>
 * </ul>
 */
public final class OzelBasarim {
    public static final String DEVASA = "devasa";
    public static final String ASTRONOM = "astronom";
    public static final String CAYIR_CAYIR = "cayir_cayir";
    public static final String SIFON = "sifon";
    public static final String INCE_BUZ_KIRICI = "ince_buz_kirici";
    public static final String YINELEME_IKIZ = "yineleme_ikiz";
    public static final String DEFTER_ILK_ACILIS = "defter_ilk_acilis";
    public static final String KOLYE_SERIN = "kolye_serin";
    public static final String GOKTEN_ZIRH = "gokten_zirh";
    public static final String SALLANAN_KULE = "sallanan_kule";
    public static final String YUKSEK_ZEMIN = "yuksek_zemin";
    public static final String GOZUN_ICINDE = "gozun_icinde";
    public static final String CIFTE_BELA = "cifte_bela";

    private OzelBasarim() {
    }

    /** {@code anahtar}a karşılık gelen başarımı verir; zaten kazanılmışsa ya da oyuncu boşsa hiçbir şey yapmaz. */
    public static void ver(ServerPlayer oyuncu, String anahtar) {
        if (oyuncu == null) {
            return;
        }
        if (Basarimlar.ozelAnahtardan(anahtar).isEmpty()) {
            DogalAfetler.LOGGER.warn("[basarim] bilinmeyen özel başarım anahtarı: {}", anahtar);
            return;
        }
        BasarimOlcutleri.OZEL.tetikle(oyuncu, anahtar);
    }
}
