package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunTestiAfetSecimi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Yaşam döngüsünü uçtan uca sınamak için davranışı olmayan afet: bloklara ve varlıklara dokunmaz, AKTIF aşaması
 * {@code aktifSure} ölçekli tick sürer, etki alanı merkez çevresinde sabit yarıçaptır. Güneş patlaması türünde gerçek
 * afetin dönem etkisini taklit eder: AKTIF'e girerken güneş NORMAL ise BUYUYEN, BITIS'e girerken BUYUYEN ise SUPERNOVA olur
 * (madde 11 "tamamlanmamış süreç" sınanabilsin). GameTest'ler {@link AfetKaydi#geciciKaydet} ile kullanır (hata sınaması
 * için alt sınıflanır); geliştirme sunucusunda {@code -Ddogalafetler.oyunTestiAfeti=deprem,sinkhole} (Gradle:
 * {@code -PoyunTestiAfeti=...}) verilirse o türler için kaydedilir, güneş patlaması türüyle birlikte
 * {@link OyunTestiSureci} de kaydedilir. Gerçek afeti kayıtlı tür uyarıyla atlanır (K-E22); {@code !} önekli tür
 * ({@code !deprem}) gerçek afetin yerine geçer (K-E23: çekirdek senaryosu gerçek afetlerden bağımsız koşar). Üretim
 * kurulumunda kaydedilmez.
 */
public class OyunTestiAfeti extends Afet {
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

    /**
     * {@value #OZELLIK} sistem özelliğindeki kayıtsız türler için kayıt yapar; özellik yoksa hiçbir şey yapmaz. Gerçek afet
     * kayıtlarından sonra çağrılır (K-E22): gerçek afeti kayıtlı tür {@code WARN} ile atlanır, {@code !} önekliyse gerçek
     * afetin yerine geçer (K-E23).
     */
    public static void ozelliktenKaydet() {
        OyunTestiAfetSecimi.Secim secim = OyunTestiAfetSecimi.sec(System.getProperty(OZELLIK), AfetKaydi::kayitliMi);
        for (AfetTuru tur : secim.atlanan()) {
            DogalAfetler.LOGGER.warn("Oyun testi afeti {} atlandı ({}): gerçek afet kayıtlı (K-E22)", tur.kimlik(), OZELLIK);
        }
        for (AfetTuru tur : secim.ezilecek()) {
            AfetKaydi.oyunTestiEz(tur, () -> new OyunTestiAfeti(tur, 32, VARSAYILAN_AKTIF_SURE));
            DogalAfetler.LOGGER.warn("Oyun testi afeti {} gerçek afetin yerine kaydedildi ({}, K-E23); gerçek afet değildir",
                    tur.kimlik(), OZELLIK);
        }
        for (AfetTuru tur : secim.kaydedilecek()) {
            AfetKaydi.oyunTestiKaydet(tur, () -> new OyunTestiAfeti(tur, 32, VARSAYILAN_AKTIF_SURE));
            DogalAfetler.LOGGER.warn("Oyun testi afeti {} için kaydedildi ({}); gerçek afet değildir", tur.kimlik(), OZELLIK);
        }
        if (secim.kaydedilecek().contains(AfetTuru.GUNES_PATLAMASI) || secim.ezilecek().contains(AfetTuru.GUNES_PATLAMASI)) {
            OyunTestiSureci.kaydet();
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

    /** Taklit gün saati penceresini beklemez: senaryo betikleri yaşam döngüsünü saatten bağımsız sınar (gökyüzü-2 istek 26). */
    @Override
    protected boolean patlamaPenceresiniBekle(ServerLevel level) {
        return false;
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
