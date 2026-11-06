package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Yaşam döngüsünü uçtan uca sınamak için davranışı olmayan afet: bloklara ve varlıklara dokunmaz, AKTIF aşaması
 * {@code aktifSure} ölçekli tick sürer, etki alanı merkez çevresinde sabit yarıçaptır. Güneş patlaması türünde gerçek
 * afetin dönem etkisini taklit eder: AKTIF'e girerken güneş NORMAL ise BUYUYEN, BITIS'e girerken BUYUYEN ise SUPERNOVA olur
 * (madde 11 "tamamlanmamış süreç" sınanabilsin). GameTest'ler {@link AfetKaydi#geciciKaydet} ile kullanır;
 * geliştirme sunucusunda {@code -Ddogalafetler.oyunTestiAfeti=deprem,sinkhole} (Gradle: {@code -PoyunTestiAfeti=...})
 * verilirse o türler için kaydedilir. Üretim kurulumunda kaydedilmez.
 */
public final class OyunTestiAfeti extends Afet {
    public static final String OZELLIK = "dogalafetler.oyunTestiAfeti";
    public static final long VARSAYILAN_AKTIF_SURE = 6000;

    private final double yaricap;
    private final long aktifSure;
    private long aktifGecen;
    private int temizlemeSayisi;

    public OyunTestiAfeti(AfetTuru tur, double yaricap, long aktifSure) {
        super(tur);
        this.yaricap = yaricap;
        this.aktifSure = aktifSure;
    }

    /** {@value #OZELLIK} sistem özelliğindeki türler için kayıt yapar; özellik yoksa hiçbir şey yapmaz. */
    public static void ozelliktenKaydet() {
        String deger = System.getProperty(OZELLIK, "").trim();
        if (deger.isEmpty()) {
            return;
        }
        for (String parca : deger.toLowerCase(Locale.ROOT).split(",")) {
            AfetTuru.kimlikIle(parca).ifPresent(tur -> {
                AfetKaydi.kaydet(tur, () -> new OyunTestiAfeti(tur, 32, VARSAYILAN_AKTIF_SURE));
                DogalAfetler.LOGGER.warn("Oyun testi afeti {} için kaydedildi ({}); gerçek afet değildir", tur.kimlik(), OZELLIK);
            });
        }
    }

    public int temizlemeSayisi() {
        return temizlemeSayisi;
    }

    @Override
    public EtkiAlani etkiAlani() {
        if (tur().sunucuGeneli()) {
            return EtkiAlani.herYer();
        }
        BlockPos merkez = baglam().merkez();
        return EtkiAlani.daire(merkez.getX() + 0.5, merkez.getY(), merkez.getZ() + 0.5, yaricap);
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama == AfetAsamasi.AKTIF) {
            aktifGecen += adim;
        }
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (tur() != AfetTuru.GUNES_PATLAMASI) {
            return;
        }
        DunyaDonemi donem = DunyaDonemi.al(level);
        if (yeni == AfetAsamasi.AKTIF && donem.gunesEvresi() == GunesEvresi.NORMAL) {
            donem.gunesEvresiAyarla(GunesEvresi.BUYUYEN);
        } else if (yeni == AfetAsamasi.BITIS && donem.gunesEvresi() == GunesEvresi.BUYUYEN) {
            donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
        }
    }

    @Override
    protected boolean aktifBitti() {
        return aktifGecen >= aktifSure;
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, aktifSure - aktifGecen);
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        temizlemeSayisi++;
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        etiket.putLong("aktifGecen", aktifGecen);
        etiket.putInt("temizleme", temizlemeSayisi);
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        aktifGecen = etiket.getLong("aktifGecen");
        temizlemeSayisi = etiket.getInt("temizleme");
    }
}
