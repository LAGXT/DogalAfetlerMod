package io.github.kadirberkpolat1.dogalafetler.afet.sel;

import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelTetikleyicileri;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * SERVER {@code sel} bölümü: seviye tablosu (yükselme, süreler), doğal seçim çarpanları (yağmur, fırtına, tsunami, ay çarpması) ve
 * ısı kaynaklarının sel suyunu buharlaştırması. Seviye listeleri 1–5 sırasıyla beş değerdir; varsayılanlar
 * {@link SelKurallari.Tablo#VARSAYILAN}'dan gelir.
 */
public final class SelAyarlari {
    private static AyarKaydi.Bolum<SelAyarlari> bolum;

    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> artisBlok;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> yukselmeSaniye;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> zirveSaniye;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> inmeSaniye;
    private final ForgeConfigSpec.DoubleValue bogulmaCarpani;
    private final ForgeConfigSpec.DoubleValue yagmurCarpani;
    private final ForgeConfigSpec.DoubleValue firtinaCarpani;
    private final ForgeConfigSpec.DoubleValue tsunamiSururkenCarpani;
    private final ForgeConfigSpec.DoubleValue tsunamiSonrasiCarpani;
    private final ForgeConfigSpec.IntValue tsunamiSonrasiSabitDakika;
    private final ForgeConfigSpec.IntValue tsunamiSonrasiSonumDakika;
    private final ForgeConfigSpec.DoubleValue ayCarpmasiCarpani;
    private final ForgeConfigSpec.IntValue ayCarpmasiSabitDakika;
    private final ForgeConfigSpec.IntValue ayCarpmasiSonumDakika;
    private final ForgeConfigSpec.BooleanValue isiBuharlastirir;
    private final ForgeConfigSpec.DoubleValue isiEnAzGuc;
    private final ForgeConfigSpec.DoubleValue isiYaricapCarpani;
    private final ForgeConfigSpec.IntValue taramaYaricapiChunk;
    private final ForgeConfigSpec.IntValue taramaButcesiChunk;

    private SelAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Config göçünün sürümü (K-E61): dosya bundan eskiyse açılışta eski varsayılanında kalan sel yükselme ve süre listeleri "
                + "yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                .defineInRange("ayarSurumu", SelAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
        SelKurallari.Tablo v = SelKurallari.Tablo.VARSAYILAN;
        artisBlok = DepremAyarlari.ondaliklar(k, "artisBlok",
                "Selin deniz ve göl yüzeyini yükselttiği en çok blok (S1…S5; en çok 16; ay çarpması sonrası kalıcı yükselmeyle toplanır)",
                v.artisBlok().toArray(Double[]::new));
        yukselmeSaniye = DepremAyarlari.tamsayilar(k, "yukselmeSaniye", "Sel suyunun tepeye çıkış süresi (saniye), S1…S5",
                v.yukselmeSaniye().toArray(Integer[]::new));
        zirveSaniye = DepremAyarlari.tamsayilar(k, "zirveSaniye", "Sel suyunun tepede kalış süresi (saniye), S1…S5",
                v.zirveSaniye().toArray(Integer[]::new));
        inmeSaniye = DepremAyarlari.tamsayilar(k, "inmeSaniye", "Sel suyunun geri çekilme süresi (saniye), S1…S5",
                v.inmeSaniye().toArray(Integer[]::new));
        bogulmaCarpani = k.comment("Sel suyunda boğulma, vanilla suya göre bu kadar kat hızlı (2–3 arası önerilir; 1 = vanilla).")
                .defineInRange("bogulmaCarpani", SelKurallari.BOGULMA_CARPANI, 1.0, 10.0);
        SelTetikleyicileri.Anlik a = SelTetikleyicileri.Anlik.VARSAYILAN;
        k.comment("Doğal seçimde selin olasılığını artıran etkenler (1 = etkisiz). Çarpanlar birbiriyle çarpılır;",
                "hava durumundan yalnız en güçlüsü sayılır.").push("tetikleyiciler");
        yagmurCarpani = k.comment("Üst dünyada yağmur yağarken.").defineInRange("yagmurCarpani", a.yagmur(), 1.0, 100.0);
        firtinaCarpani = k.comment("Üst dünyada fırtına (gök gürültülü yağmur) varken.")
                .defineInRange("firtinaCarpani", a.firtina(), 1.0, 100.0);
        tsunamiSururkenCarpani = k.comment("Bir tsunami sürerken (boş afet yuvası varsa).")
                .defineInRange("tsunamiSururkenCarpani", a.tsunamiSururken(), 1.0, 100.0);
        tsunamiSonrasiCarpani = k.comment("Bir tsunami doğal bitince geçici çarpan (tepe).")
                .defineInRange("tsunamiSonrasiCarpani", 6.0, 1.0, 100.0);
        tsunamiSonrasiSabitDakika = k.comment("Tsunami sonrası çarpanın tepede kaldığı süre (ölçekli dakika).")
                .defineInRange("tsunamiSonrasiSabitDakika", 4, 0, 1_000);
        tsunamiSonrasiSonumDakika = k.comment("Tsunami sonrası çarpanın 1'e indiği süre (ölçekli dakika).")
                .defineInRange("tsunamiSonrasiSonumDakika", 10, 0, 1_000);
        ayCarpmasiCarpani = k.comment("Ay çarpmasından sonra geçici çarpan (tepe).")
                .defineInRange("ayCarpmasiCarpani", 10.0, 1.0, 100.0);
        ayCarpmasiSabitDakika = k.comment("Ay çarpması sonrası çarpanın tepede kaldığı süre (ölçekli dakika).")
                .defineInRange("ayCarpmasiSabitDakika", 15, 0, 10_000);
        ayCarpmasiSonumDakika = k.comment("Ay çarpması sonrası çarpanın 1'e indiği süre (ölçekli dakika).")
                .defineInRange("ayCarpmasiSonumDakika", 30, 0, 10_000);
        k.pop();
        k.comment("Isı yayan bloklar (lav, ateş, kamp ateşi, yanan fırın, meşale…; kaynak tablosu sıcaklık bölümündeki",
                "sıcak kaynaklardır) sel suyunu çevrelerinde buharlaştırır: kaynak satırındaki yarıçap × çarpan içinde sel yoktur.")
                .push("buharlasma");
        isiBuharlastirir = k.comment("false: ısı kaynakları sel suyunu buharlaştırmaz.").define("acik", true);
        isiEnAzGuc = k.comment("Buharlaştırmak için kaynak satırındaki en az güç (°C).")
                .defineInRange("enAzGuc", 3.0, 0.0, 100.0);
        isiYaricapCarpani = k.comment("Kaynak satırındaki yarıçapın çarpanı (blok).").defineInRange("yaricapCarpani", 1.0, 0.25, 2.0);
        taramaYaricapiChunk = k.comment("Oyuncunun çevresinde ısı kaynağı aranan yarıçap (chunk).")
                .defineInRange("taramaYaricapiChunk", 6, 1, 16);
        taramaButcesiChunk = k.comment("Tick başına taranan en çok chunk (ısı kaynağı araması).")
                .defineInRange("taramaButcesiChunk", 3, 1, 32);
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("sel", "Sel (taşkın): deniz ve göllerin taşması", SelAyarlari::new);
        AyarKaydi.sunucuGocu(SelAyarGocu.PLAN);
    }

    /** Seviye tablosu; beş öğeli olmayan liste varsayılana düşer. */
    public static SelKurallari.Tablo tablo() {
        SelAyarlari a = bolum.get();
        SelKurallari.Tablo v = SelKurallari.Tablo.VARSAYILAN;
        return new SelKurallari.Tablo(liste(a.artisBlok.get(), v.artisBlok()), liste(a.yukselmeSaniye.get(), v.yukselmeSaniye()),
                liste(a.zirveSaniye.get(), v.zirveSaniye()), liste(a.inmeSaniye.get(), v.inmeSaniye()));
    }

    public static SelKurallari.Seviye seviye(int seviye) {
        return tablo().seviye(seviye);
    }

    /** Sel suyunda boğulma hızının vanilla suya oranı (K-E81). */
    public static double bogulmaCarpani() {
        return bolum.get().bogulmaCarpani.get();
    }

    public static SelTetikleyicileri.Anlik anlikTetikleyiciler() {
        SelAyarlari a = bolum.get();
        return new SelTetikleyicileri.Anlik(a.yagmurCarpani.get(), a.firtinaCarpani.get(), a.tsunamiSururkenCarpani.get());
    }

    /** Tsunami doğal bitince eklenecek geçici çarpan (tick). */
    public static SelTetikleyicileri.Gecici tsunamiSonrasi() {
        SelAyarlari a = bolum.get();
        return new SelTetikleyicileri.Gecici(a.tsunamiSonrasiCarpani.get(), dakikaTick(a.tsunamiSonrasiSabitDakika.get()),
                dakikaTick(a.tsunamiSonrasiSonumDakika.get()));
    }

    /** Ay çarpması anında eklenecek geçici çarpan (tick). */
    public static SelTetikleyicileri.Gecici ayCarpmasiSonrasi() {
        SelAyarlari a = bolum.get();
        return new SelTetikleyicileri.Gecici(a.ayCarpmasiCarpani.get(), dakikaTick(a.ayCarpmasiSabitDakika.get()),
                dakikaTick(a.ayCarpmasiSonumDakika.get()));
    }

    public static boolean isiBuharlastirir() {
        return bolum.get().isiBuharlastirir.get();
    }

    public static double isiEnAzGuc() {
        return bolum.get().isiEnAzGuc.get();
    }

    public static double isiYaricapCarpani() {
        return bolum.get().isiYaricapCarpani.get();
    }

    public static int taramaYaricapiChunk() {
        return bolum.get().taramaYaricapiChunk.get();
    }

    public static int taramaButcesiChunk() {
        return bolum.get().taramaButcesiChunk.get();
    }

    private static long dakikaTick(int dakika) {
        return dakika * 60L * SelKurallari.SANIYE_TICK;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> liste(List<? extends Number> ham, List<T> varsayilan) {
        if (ham == null || ham.size() != varsayilan.size()) {
            return varsayilan;
        }
        Object ilk = varsayilan.get(0);
        List<T> sonuc = new java.util.ArrayList<>(ham.size());
        for (Number n : ham) {
            sonuc.add((T) (ilk instanceof Double ? (Object) n.doubleValue() : (Object) n.intValue()));
        }
        return sonuc;
    }
}
