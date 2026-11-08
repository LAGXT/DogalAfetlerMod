package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Hissedilen;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.IsiAlisverisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.KaynakTanimi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * SERVER config'in {@code sicaklik} bölümüne sıcaklık sisteminin katkısı (çekirdeğin {@code sicaklikSistemiModu}'nun yanına).
 * Saf mantık kayıtlarının ({@code Ayarlar}, {@code Esikler}) her bileşeni aynı adlı bir config anahtarıdır ve varsayılanı
 * kaydın {@code VARSAYILAN}'ından gelir: sayıların tek kaynağı saf koddur. Kayıtlar config yüklenince bir kez kurulur.
 */
public final class SicaklikAyarlari {
    private static AyarKaydi.Bolum<SicaklikAyarlari> bolum;
    private static volatile Ozet ozet;

    /** Config'ten kurulmuş saf ayarlar. */
    public record Ozet(IsiAlisverisi.Ayarlar isi, Yalitim.Ayarlar yalitim, Esikler esikler, OrtamSicakligi.Ayarlar ortam,
            Islaklik.Ayarlar islaklik, Hissedilen.Ayarlar hissedilen, List<KaynakTanimi> kaynaklar) {
    }

    private record Aralik(double min, double max, String aciklama) {
    }

    private final ForgeConfigSpec.IntValue guncellemeAraligiTick;
    private final ForgeConfigSpec.IntValue paketTazelemeTick;
    private final ForgeConfigSpec.IntValue tuyUstSiniri;
    private final ForgeConfigSpec.IntValue kaynakTaramaYaricapi;
    private final ForgeConfigSpec.DoubleValue hasarMiktari;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> kaynaklar;
    private final Map<String, ForgeConfigSpec.DoubleValue> isi;
    private final Map<String, ForgeConfigSpec.DoubleValue> yalitim;
    private final Map<String, ForgeConfigSpec.DoubleValue> esikler;
    private final Map<String, ForgeConfigSpec.DoubleValue> ortam;
    private final Map<String, ForgeConfigSpec.DoubleValue> islaklik;
    private final Map<String, ForgeConfigSpec.DoubleValue> hissedilen;

    private SicaklikAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Config göçünün sürümü (S3-09): dosya bundan eskiyse açılışta eski varsayılanında kalan sıcaklık değerleri "
                + "(eşikler, kaynak listesi) yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                .defineInRange("ayarSurumu", SicaklikAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
        guncellemeAraligiTick = k.comment("Vücut sıcaklığı güncelleme aralığı (tick; 20 = saniyede bir).")
                .defineInRange("guncellemeAraligiTick", 20, 5, 100);
        paketTazelemeTick = k.comment("Değer değişmese de HUD paketinin yeniden gönderildiği aralık (tick).")
                .defineInRange("paketTazelemeTick", 200, 20, 1200);
        tuyUstSiniri = k.comment("Bir zırh parçasına uygulanabilecek en çok Tüy Upgrade'i (N2).")
                .defineInRange("tuyUstSiniri", 30, 1, 1000);
        kaynakTaramaYaricapi = k.comment("Isı/soğuk kaynaklarının arandığı küpün yarıçapı (blok). Bu mesafeden uzak blok kaynakları",
                "kendi yarıçapları büyük olsa da sayılmaz (lav 6 blokta katkısı ~1 °C olduğu için varsayılan 5 yeter).")
                .defineInRange("kaynakTaramaYaricapi", 5, 1, 8);
        hasarMiktari = k.comment("Donma ve aşırı ısınma hasarının her vuruştaki miktarı (yarım kalp = 1).")
                .defineInRange("hasarMiktari", 1.0, 0.5, 20.0);
        kaynaklar = k.comment("Isı (+) ve soğuk (−) kaynakları: blok=güç:yarıçap. Güç kaynağın üzerindeki °C katkısıdır,",
                "mesafeyle (1 − d/r)² azalır. LIT özelliği olan bloklar (kamp ateşi, fırın) yalnız yanarken sayılır.",
                "Bilinmeyen blok kimliği sessizce atlanır (kömürleşmiş odunlar kısık ama sürekli ısı verir).")
                .defineListAllowEmpty(List.of("kaynaklar"), SicaklikAyarlari::varsayilanKaynaklar,
                        KaynakTanimi::gecerliMi);
        k.comment("Isı alışverişi (madde 7b, N4). Vücut sıcaklığı −100…+100 vücut birimidir, 0 nötr "
                + "(oyuncuya 32…42 °C görünür: °C = 37 + 0,05 · birim). Ortam terimi hissedilen sıcaklıktır.").push("isi");
        isi = tanimla(k, IsiAlisverisi.Ayarlar.VARSAYILAN, isiAciklamalari());
        k.pop();
        k.comment("Yalıtım: zırh, yün (N3), tüy (N2) ve termostat kancası (madde 13c, N4).").push("yalitim");
        yalitim = tanimla(k, Yalitim.Ayarlar.VARSAYILAN, yalitimAciklamalari());
        k.pop();
        k.comment("Vücut sıcaklığı eşikleri ve dört durum (S3-04): üşüme, donma, bunalma, yanma. Değerler vücut "
                + "birimidir (°C = 37 + 0,05 · birim).").push("esikler");
        esikler = tanimla(k, Esikler.VARSAYILAN, esikAciklamalari());
        k.pop();
        k.comment("Ortam (mekan) sıcaklığı (°C, madde 7a, 9, bölüm D ve G).").push("ortam");
        ortam = tanimla(k, OrtamSicakligi.Ayarlar.VARSAYILAN, ortamAciklamalari());
        k.pop();
        k.comment("Islaklık (S3-03): suda ve yağmurda dolar, kuru yerde mekan sıcaklığına göre kurur; ıslak oyuncu daha "
                + "hızlı üşür.").push("islaklik");
        islaklik = tanimla(k, Islaklik.Ayarlar.VARSAYILAN, islaklikAciklamalari());
        k.pop();
        k.comment("Hissedilen sıcaklık (S3-02, S3-05): mekan + ıslaklık, rüzgâr ve sol el düzeltmeleri. Vücut bununla "
                + "ısı alışverişi yapar.").push("hissedilen");
        hissedilen = tanimla(k, Hissedilen.Ayarlar.VARSAYILAN, hissedilenAciklamalari());
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("sicaklik", "Sıcaklık sistemi (madde 7)", SicaklikAyarlari::new);
        AyarKaydi.sunucuGocu(SicaklikAyarGocu.PLAN);
    }

    /** Mod veri yolunda: SERVER config yüklenince ya da değişince saf ayarlar yeniden kurulur. */
    static void configDegisti(ModConfigEvent olay) {
        if (AyarKaydi.SUNUCU_DOSYASI.equals(olay.getConfig().getFileName())) {
            ozet = null;
        }
    }

    public static Ozet al() {
        Ozet o = ozet;
        if (o == null) {
            SicaklikAyarlari a = bolum.get();
            List<KaynakTanimi> liste = new ArrayList<>();
            for (String satir : a.kaynaklar.get()) {
                liste.add(KaynakTanimi.coz(satir));
            }
            o = new Ozet(kurYaDa(IsiAlisverisi.Ayarlar.class, a.isi, IsiAlisverisi.Ayarlar.VARSAYILAN),
                    kurYaDa(Yalitim.Ayarlar.class, a.yalitim, Yalitim.Ayarlar.VARSAYILAN),
                    kurYaDa(Esikler.class, a.esikler, Esikler.VARSAYILAN),
                    kurYaDa(OrtamSicakligi.Ayarlar.class, a.ortam, OrtamSicakligi.Ayarlar.VARSAYILAN),
                    kurYaDa(Islaklik.Ayarlar.class, a.islaklik, Islaklik.Ayarlar.VARSAYILAN),
                    kurYaDa(Hissedilen.Ayarlar.class, a.hissedilen, Hissedilen.Ayarlar.VARSAYILAN), List.copyOf(liste));
            ozet = o;
        }
        return o;
    }

    /**
     * Bir bölümü kurar; değerleri geçersizse (ör. elle değiştirilmiş bir eşiğin göçle gelen komşusuyla sırasının bozulması)
     * yalnız o bölüm varsayılana döner, öteki bölümlerin elle girilmiş değerleri korunur.
     */
    private static <R extends Record> R kurYaDa(Class<R> tur, Map<String, ForgeConfigSpec.DoubleValue> degerler,
            R varsayilan) {
        try {
            return kur(tur, degerler);
        } catch (IllegalStateException hata) {
            DogalAfetler.LOGGER.error("Sıcaklık config'inin {} bölümü geçersiz, bu bölüm için varsayılanlar kullanılıyor: {}",
                    tur.getSimpleName(), hata.getMessage());
            return varsayilan;
        }
    }

    public static int guncellemeAraligiTick() {
        return bolum.get().guncellemeAraligiTick.get();
    }

    public static int paketTazelemeTick() {
        return bolum.get().paketTazelemeTick.get();
    }

    public static int tuyUstSiniri() {
        return bolum.get().tuyUstSiniri.get();
    }

    public static int kaynakTaramaYaricapi() {
        return bolum.get().kaynakTaramaYaricapi.get();
    }

    public static float hasarMiktari() {
        return bolum.get().hasarMiktari.get().floatValue();
    }

    static List<String> varsayilanKaynaklar() {
        return SicaklikAyarGocu.KAYNAKLAR;
    }

    private static Map<String, ForgeConfigSpec.DoubleValue> tanimla(ForgeConfigSpec.Builder k, Record varsayilan,
            Map<String, Aralik> araliklar) {
        Map<String, ForgeConfigSpec.DoubleValue> sonuc = new LinkedHashMap<>();
        for (RecordComponent bilesen : varsayilan.getClass().getRecordComponents()) {
            Aralik aralik = araliklar.get(bilesen.getName());
            if (aralik == null) {
                throw new IllegalStateException("Açıklamasız config anahtarı: " + bilesen.getName());
            }
            double deger = ((Number) oku(bilesen, varsayilan)).doubleValue();
            sonuc.put(bilesen.getName(), k.comment(aralik.aciklama())
                    .defineInRange(bilesen.getName(), deger, aralik.min(), aralik.max()));
        }
        return sonuc;
    }

    private static Object oku(RecordComponent bilesen, Record kayit) {
        try {
            return bilesen.getAccessor().invoke(kayit);
        } catch (ReflectiveOperationException hata) {
            throw new IllegalStateException("Kayıt bileşeni okunamadı: " + bilesen.getName(), hata);
        }
    }

    private static <R extends Record> R kur(Class<R> tur, Map<String, ForgeConfigSpec.DoubleValue> degerler) {
        RecordComponent[] bilesenler = tur.getRecordComponents();
        Class<?>[] turler = new Class<?>[bilesenler.length];
        Object[] argumanlar = new Object[bilesenler.length];
        for (int i = 0; i < bilesenler.length; i++) {
            turler[i] = bilesenler[i].getType();
            double d = degerler.get(bilesenler[i].getName()).get();
            argumanlar[i] = turler[i] == int.class ? (Object) (int) Math.round(d)
                    : turler[i] == long.class ? (Object) Math.round(d) : (Object) d;
        }
        try {
            Constructor<R> kurucu = tur.getDeclaredConstructor(turler);
            return kurucu.newInstance(argumanlar);
        } catch (ReflectiveOperationException hata) {
            throw new IllegalStateException("Sıcaklık ayarları kurulamadı (" + tur.getSimpleName() + "): "
                    + hata.getCause(), hata);
        }
    }

    private static Map<String, Aralik> isiAciklamalari() {
        Map<String, Aralik> m = new LinkedHashMap<>();
        m.put("esitlenmeHizi", new Aralik(0.0005, 0.1, "k (1/sn): çıplak, kuru oyuncunun vücut-ortam farkını kapatma oranı. "
                + "0,006 ≈ 167 sn zaman sabiti; donmaya ~3 dk."));
        m.put("konforSicakligi", new Aralik(-20, 40, "Çıplak, yürüyen oyuncunun nötr kaldığı ortam (°C)."));
        m.put("birimPerDerece", new Aralik(0.1, 10, "1 °C ortam farkının vücut birimi karşılığı."));
        m.put("metabolikIsi", new Aralik(0, 200, "U: konfor ortamında metabolik ısının vücut birimi karşılığı "
                + "(yalıtımın ısı tutma gücü buna orantılı)."));
        m.put("durmaEtkinligi", new Aralik(0.1, 3, "Dururken metabolik çarpan (1'in altı = durmak soğutur)."));
        m.put("yurumeEtkinligi", new Aralik(0.1, 3, "Yürürken metabolik çarpan."));
        m.put("kosmaEtkinligi", new Aralik(0.1, 3, "Koşarken metabolik çarpan (koşmak ısıtır)."));
        m.put("islakIletkenlik", new Aralik(1, 10, "Tam ıslakken (W = 1) ısı iletkenliği çarpanı; aradaki ıslaklıkta kurudan "
                + "(1) doğrusal."));
        m.put("suIletkenlik", new Aralik(1, 20, "Suyun içindeyken ısı iletkenliği çarpanı (soğuk su hızlı soğutur)."));
        m.put("asiriSogukEsigi", new Aralik(-100, 0, "Bu °C ve altında yün ve tüy donmayı yalnız yavaşlatır (madde 7e)."));
        return m;
    }

    private static Map<String, Aralik> yalitimAciklamalari() {
        Map<String, Aralik> m = new LinkedHashMap<>();
        m.put("zirhBasina", new Aralik(0, 2, "Giyilen her zırh parçasının yalıtımı."));
        m.put("yunBasina", new Aralik(0, 2, "Her yünlü parçanın toplam yalıtıma katkısı (sıcakta dezavantaj, N4)."));
        m.put("tuyBasina", new Aralik(0, 1, "Her Tüy Upgrade'inin yalıtımı."));
        m.put("yunSogumaOrani", new Aralik(0, 1, "Yünlü parça başına soğuma hızı kesintisi (N3: %25)."));
        m.put("asiriSogukYunVerimi", new Aralik(0, 1, "Aşırı soğukta yün kesintisinin kalan oranı "
                + "(1'in altında 4 yünlü parça da donmayı yalnız yavaşlatır)."));
        m.put("termostatCekimi", new Aralik(0, 5, "Termostatlı parça başına nötre çekme hızı (vücut birimi/sn)."));
        m.put("termostatIlimliSinir", new Aralik(0, 400, "Ortam eşdeğerinin (|E|) bu değerine kadar termostat tam çalışır."));
        m.put("termostatUcSinir", new Aralik(1, 500, "|E| bu değerden sonra termostat taban oranında çalışır."));
        m.put("termostatTaban", new Aralik(0.01, 1, "Uç ortamda termostatın kalan oranı (hiç kaybolmaz)."));
        return m;
    }

    private static Map<String, Aralik> esikAciklamalari() {
        Map<String, Aralik> m = new LinkedHashMap<>();
        m.put("notrBant", new Aralik(1, 50, "HUD'daki nötr bantın yarı genişliği (yalnız görünüm; 10 = 36,5–37,5 °C)."));
        m.put("soguk", new Aralik(-99, -1, "Üşüme: bu değer ve altında (−20 = 36,0 °C) hafif yavaşlama ve ağız buharı, hasarsız."));
        m.put("cokSoguk", new Aralik(-99, -1, "Donma: bu değer ve altında (−40 = 35,0 °C) buz kenarı ve titreme."));
        m.put("donmaHasari", new Aralik(-100, -1, "Bu değer ve altında (−70 = 33,5 °C) donma hasarı."));
        m.put("sicak", new Aralik(1, 99, "Bunalma: bu değer ve üstünde (+20 = 38,0 °C) terleme ve ek açlık, hasarsız."));
        m.put("cokSicak", new Aralik(1, 99, "Yanma: bu değer ve üstünde (+50 = 39,5 °C) sıcak hava, yalpa ve daha çok açlık."));
        m.put("yanmaHasari", new Aralik(1, 100, "Bu değer ve üstünde (+70 = 40,5 °C) aşırı ısınma hasarı."));
        m.put("enCokYavaslama", new Aralik(0, 0.9, "Ölçek ucundaki hareket hızı kesintisi (0,5 = %50)."));
        m.put("donmaAralikBasi", new Aralik(20, 1200, "Donma eşiğindeki hasar aralığı (tick)."));
        m.put("donmaAralikSonu", new Aralik(20, 1200, "−100'deki hasar aralığı (tick)."));
        m.put("yanmaAralikBasi", new Aralik(20, 1200, "Aşırı ısınma eşiğindeki hasar aralığı (tick)."));
        m.put("yanmaAralikSonu", new Aralik(20, 1200, "+100'deki hasar aralığı (tick)."));
        m.put("bunalmaAclik", new Aralik(0, 4, "Bunalmada saniyede ek açlık yorgunluğu (exhaustion; 4 = 1 açlık puanı)."));
        m.put("yanmaAclik", new Aralik(0, 4, "Yanmada saniyede ek açlık yorgunluğu (exhaustion; 4 = 1 açlık puanı)."));
        return m;
    }

    private static Map<String, Aralik> islaklikAciklamalari() {
        Map<String, Aralik> m = new LinkedHashMap<>();
        m.put("suDolum", new Aralik(0.001, 1, "Suyun içinde ya da kabarcık sütununda saniyede ıslanma (0,25 = 4 sn'de tam ıslak)."));
        m.put("yagmurDolum", new Aralik(0, 1, "Açık havada yağmur altında saniyede ıslanma (0,012 ≈ 83 sn; kar ıslatmaz)."));
        m.put("firtinaDolum", new Aralik(0, 1, "Açık havada fırtınada saniyede ıslanma."));
        m.put("kurumaTabani", new Aralik(0.0001, 1, "10 °C'de, ısı kaynağı yokken saniyede kuruma (1/150 ≈ 150 sn); "
                + "0 °C'de yarısı, 30 °C'de iki katı. Yanan oyuncu anında kurur."));
        m.put("kaynakHizlandirma", new Aralik(0, 2, "Yakındaki ısı kaynaklarının her °C'sinin kuruma hızına katı "
                + "(0,1: 10 °C kaynak iki kat hızlı kurutur)."));
        return m;
    }

    private static Map<String, Aralik> hissedilenAciklamalari() {
        Map<String, Aralik> m = new LinkedHashMap<>();
        m.put("islakSogutma", new Aralik(0, 30, "Tam ıslak oyuncunun hissedilen serinlemesi (°C; buharlaşma, sıcakta da)."));
        m.put("ruzgarYukseklik", new Aralik(0, 5, "Açık havada Y 64 üstünde blok başına rüzgâr (km/h)."));
        m.put("ruzgarYagmur", new Aralik(0, 100, "Açık havada yağışta ek rüzgâr (km/h)."));
        m.put("ruzgarFirtina", new Aralik(0, 100, "Açık havada fırtınada ek rüzgâr (km/h)."));
        m.put("ruzgarKosma", new Aralik(0, 100, "Koşarken ek rüzgâr (km/h). Rüzgâr soğuğu yalnız 10 °C altında ve "
                + "4,8 km/h üstünde işler (NWS formulü)."));
        m.put("buzKovasi", new Aralik(-50, 0, "Sol eldeki Buz Kovasının hissedilen düzeltmesi (°C)."));
        m.put("lavKovasi", new Aralik(0, 50, "Sol eldeki lav kovasının hissedilen düzeltmesi (°C)."));
        m.put("erimeEsigi", new Aralik(-30, 100, "Sol eldeki Buz Kovası bu mekan sıcaklığının (°C) üstünde erir; "
                + "saniyede (mekan − eşik)/10 birikir, 300'de su kovasına döner."));
        return m;
    }

    private static Map<String, Aralik> ortamAciklamalari() {
        Map<String, Aralik> m = new LinkedHashMap<>();
        m.put("biyomCarpani", new Aralik(0, 100, "Biyom sıcaklığı (vanilla ölçek) × bu = °C katkısı (ova 0,8 → 16)."));
        m.put("biyomOfseti", new Aralik(-50, 50, "Biyom katkısına eklenen sabit (°C)."));
        m.put("kurakOrtalamaDususu", new Aralik(0, 40, "Kurak (yağışsız) biyomun günlük ortalamasının düşüşü (°C)."));
        m.put("gunGeceGenligi", new Aralik(0, 40, "Gün-gece genliği (°C; öğleden sonra +, gece yarısı −)."));
        m.put("kurakGenlikCarpani", new Aralik(1, 10, "Kurak biyomda genlik çarpanı (çölde gündüz çok sıcak, gece çok soğuk)."));
        m.put("tepeSaati", new Aralik(0, 23999, "Gün eğrisinin tepe tick'i (6000 öğlen)."));
        m.put("yukseklikBaslangici", new Aralik(-64, 320, "Bu Y'nin üstünde yükseklik soğutur."));
        m.put("yukseklikKatsayisi", new Aralik(0, 1, "Blok başına °C düşüşü (gerçek 0,0065 °C/m × 10)."));
        m.put("yagmur", new Aralik(-30, 0, "Yağmur altında (°C)."));
        m.put("kar", new Aralik(-30, 0, "Kar yağışı altında (°C)."));
        m.put("firtina", new Aralik(-30, 0, "Fırtınada (°C)."));
        m.put("gunes", new Aralik(0, 30, "Öğlen güneş altında (°C; gölgede 0)."));
        m.put("yeraltiSicakligi", new Aralik(-30, 40, "Derin mağaranın sabit sıcaklığı (°C)."));
        m.put("yeraltiGecisDerinligi", new Aralik(1, 128, "Yüzeyin bu kadar altında sıcaklık tamamen yeraltı sabitidir (blok)."));
        m.put("kapaliAlanHedefi", new Aralik(-30, 40, "Kapalı alanın çekildiği dengeli sıcaklık (°C)."));
        m.put("kapaliAlanOrani", new Aralik(0, 1, "Kapalı alanda dengeli sıcaklığa çekilme oranı."));
        m.put("su", new Aralik(-40, 0, "Suyun içinde (°C; iletkenlik ayrıca artar)."));
        m.put("kaynakUstSiniri", new Aralik(0, 200, "Yakın ısı kaynaklarının toplam üst sınırı (°C)."));
        m.put("kaynakAltSiniri", new Aralik(-200, 0, "Yakın soğuk kaynaklarının toplam alt sınırı (°C)."));
        m.put("nether", new Aralik(-50, 150, "Nether sıcaklığı (°C)."));
        m.put("end", new Aralik(-100, 50, "End sıcaklığı (°C)."));
        m.put("gunesPatlamasiArtisi", new Aralik(0, 150, "Güneş patlamasının tepesinde açık havada artış (°C)."));
        m.put("golgeOrani", new Aralik(0, 1, "Güneş patlaması artışının gölgede kalan oranı."));
        m.put("yeraltiKorumasi", new Aralik(0, 1, "Tam derinlikte dünya dönemi etkisinin sönen oranı."));
        m.put("supernovaKalintisi", new Aralik(0, 100, "Süpernova başındaki artık ısı (°C; evre boyunca söner)."));
        m.put("donmaDususu", new Aralik(0, 150, "Donma ilerlemesi %100'deyken düşüş (°C; madde 9)."));
        m.put("kizilCuceOfseti", new Aralik(-60, 0, "Kızıl cüce döneminin ek ofseti (°C; bölüm D: belirgin şekilde serin)."));
        m.put("kizilCuceGunesCarpani", new Aralik(0, 1, "Kızıl cücede güneş ve gün-gece ısısının çarpanı (zayıf güneş)."));
        m.put("kisBolgesiDususu", new Aralik(0, 100, "Kızıl cücede kalıcı kış bölgesinin ek düşüşü (°C)."));
        return m;
    }
}
