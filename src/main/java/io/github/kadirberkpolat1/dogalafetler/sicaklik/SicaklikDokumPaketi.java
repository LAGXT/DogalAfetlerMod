package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code sicaklik/dokum}: termometre menüsünün ayrıntılı dökümü (S3-08, sözleşme §A6). Yalnız menü açıkken, istemcinin
 * {@link SicaklikDokumIstegi}'ne yanıt olarak o oyuncuya gider. Kalemler {@link Katki} sırasıyla dizindedir; olmayan kalem
 * {@code NaN}. ~110 bayt.
 *
 * @param mekan         mekan sıcaklığı (°C)
 * @param hissedilen    hissedilen sıcaklık (°C)
 * @param vucut         vücut sıcaklığı (vücut birimi; °C'ye {@code VucutOlcegi} çevirir)
 * @param egilim        vücudun değişim hızı (birim/sn)
 * @param islaklik      ıslaklık W (0–1)
 * @param kurumaSaniye  tamamen kurumaya kalan süre (sn); −1 kurumuyor (ıslanıyor ya da zaten kuru)
 * @param yalitim       toplam yalıtım I (ısı alışverişi {@code 1 + I} kat yavaşlar)
 * @param katkilar      {@code Katki.values().length} uzunluğunda kalem °C'leri
 */
public record SicaklikDokumPaketi(float mekan, float hissedilen, float vucut, float egilim, float islaklik,
        float kurumaSaniye, float ruzgarKmh, float yalitim, SolEl solEl, float[] katkilar) {
    public static final String AD = "sicaklik/dokum";
    private static final int KALEM = Katki.values().length;

    /** Sunucunun analizinden (durumu değiştirmez) menünün gösterdiği değerler. */
    public static SicaklikDokumPaketi analizden(SicaklikYoneticisi.Analiz a, double vucut) {
        float[] k = new float[KALEM];
        Arrays.fill(k, Float.NaN);
        doldur(k, a.mekan().katkilar());
        doldur(k, a.hissedilen().duzeltmeler());
        double w = a.girdi().islaklik();
        double kuruma = w > 0 && a.kurumaHizi() > 0 ? w / a.kurumaHizi() : -1;
        return new SicaklikDokumPaketi((float) a.mekan().toplam(), (float) a.hissedilen().toplam(), (float) vucut,
                (float) a.egilim(), (float) w, (float) kuruma, (float) a.hissedilen().ruzgarKmh(), (float) a.yalitim(),
                a.hissedilen().solEl(), k);
    }

    private static void doldur(float[] k, Map<Katki, Double> kalemler) {
        for (Map.Entry<Katki, Double> e : kalemler.entrySet()) {
            k[e.getKey().ordinal()] = e.getValue().floatValue();
        }
    }

    /** Kalemin °C katkısı; kalem yoksa {@code NaN}. */
    public float katki(Katki k) {
        return k.ordinal() < katkilar.length ? katkilar[k.ordinal()] : Float.NaN;
    }

    static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye(AD, SicaklikDokumPaketi.class, SicaklikDokumPaketi::yaz,
                SicaklikDokumPaketi::oku, SicaklikDokumPaketi::isle));
    }

    public void yaz(FriendlyByteBuf t) {
        t.writeFloat(mekan);
        t.writeFloat(hissedilen);
        t.writeFloat(vucut);
        t.writeFloat(egilim);
        t.writeFloat(islaklik);
        t.writeFloat(kurumaSaniye);
        t.writeFloat(ruzgarKmh);
        t.writeFloat(yalitim);
        t.writeByte(solEl.ordinal());
        t.writeVarInt(katkilar.length);
        for (float f : katkilar) {
            t.writeFloat(f);
        }
    }

    public static SicaklikDokumPaketi oku(FriendlyByteBuf t) {
        float mekan = t.readFloat();
        float hissedilen = t.readFloat();
        float vucut = t.readFloat();
        float egilim = t.readFloat();
        float islaklik = t.readFloat();
        float kuruma = t.readFloat();
        float ruzgar = t.readFloat();
        float yalitim = t.readFloat();
        SolEl solEl = SolEl.sirayla(t.readByte());
        int n = t.readVarInt();
        float[] k = new float[KALEM];
        Arrays.fill(k, Float.NaN);
        for (int i = 0; i < n; i++) {
            float f = t.readFloat();
            if (i < KALEM) {
                k[i] = f;
            }
        }
        return new SicaklikDokumPaketi(mekan, hissedilen, vucut, egilim, islaklik, kuruma, ruzgar, yalitim, solEl, k);
    }

    /** İstemci ana thread'inde: {@code client.sicaklik.IstemciDokum}'e N15 yoluyla (K-C14). */
    public static void isle(SicaklikDokumPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<SicaklikDokumPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.sicaklik.IstemciDokum::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
