package io.github.kadirberkpolat1.dogalafetler.uyum;

import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.fml.ModList;

/**
 * İsteğe bağlı mod uyumlarının ortak girişi (K-E74). Uyum modları yüklü değilse hiçbir uyum sınıfı yüklenmez ve mod aynı çalışır:
 * <ul>
 * <li>JEI ({@code client.uyum.JeiUyumu}): tarifi olmayan eşyaların üstüne "nasıl ve nerede elde edilir" bilgi sayfası.</li>
 * <li>Jade ({@code client.uyum.JadeUyumu}): kömürleşmiş kütük ısısı, kül katı ve (açılan) ortam sıcaklığı/afet satırı.</li>
 * <li>Curios ({@link CuriosKopru}, {@link CuriosVerileri}): Serin Kolye kolye yuvasına girer ve oradayken de serinletir.</li>
 * <li>Punchy ({@code client.uyum.PunchyUyumu}, {@code client.uyum.mixin}): deprem sarsıntısı birinci kişi kola ve başkasının
 *     gövdesine de işler.</li>
 * </ul>
 * JEI ve Jade eklentileri API jar'ı olmadan, {@code src/uyumApi} imza taslaklarına karşı derlenir (bkz. build.gradle).
 * Bu sınıf ortak koddur: istemci sınıfına dokunmaz, yalnız datagen ve yüklü-mod sorgusu verir.
 */
public final class Uyum {
    public static final String JEI = "jei";
    public static final String JADE = "jade";
    public static final String CURIOS = "curios";
    public static final String PUNCHY = "punchy";

    private Uyum() {
    }

    /** Mod kurucusunda bir kez: uyum çevirileri ve Curios veri paketi datagen'e eklenir. */
    public static void kaydet() {
        DataGenerators.ceviriEkle(EsyaIpuclari::cevirileriYaz);
        DataGenerators.ceviriEkle(UyumCevirileri::yaz);
        DataGenerators.saglayiciEkle(CuriosVerileri::ekle);
    }

    /** {@code modKimligi} yüklü mü (mod listesi henüz yoksa, örn. datagen başlangıcında, hayır). */
    public static boolean yuklu(String modKimligi) {
        ModList liste = ModList.get();
        return liste != null && liste.isLoaded(modKimligi);
    }
}
